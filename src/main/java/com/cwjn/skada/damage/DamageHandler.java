package com.cwjn.skada.damage;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.Skada;
import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.ElementSpreadInstance;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.event.custom.PostMitigationEvent;
import com.cwjn.skada.util.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.cwjn.skada.data.SkadaData.DEBUG_ENABLED;
import static com.cwjn.skada.util.UtilColour.*;

@Mod.EventBusSubscriber
public class DamageHandler {

  /**
   * Float.MAX_VALUE literal for compatibility with vanilla damage resistance stat tracking.
   * Used to check for overflow in damage resistance calculations (copied from vanilla LivingEntity).
   */
  private static final float VANILLA_FLOAT_MAX_VALUE = 3.4028235E37F;

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void doDamageCalculation(LivingHurtEvent event) {
    SkadaDamageSource source;
    LivingEntity target = event.getEntity();
    if (target.getServer() != null) target.getServer().getProfiler().push("SkadaDamageHandler");
    boolean isProjectile = false;
    if (DEBUG_ENABLED) Skada.LOGGER.debug(CONSOLE_UNDERLINE + "Damage event for entity: {}, initial damage: {}, source: {}" + CONSOLE_RESET, target.getName().getString(), event.getAmount(), event.getSource().getMsgId());
    double amount = event.getAmount();
    if (event.getSource() instanceof SkadaDamageSource) {
      if (DEBUG_ENABLED) Skada.LOGGER.debug("Damage source is a SkadaDamageSource.");
      source = (SkadaDamageSource) event.getSource();
    } else if (event.getSource().getDirectEntity() instanceof Projectile projectile) {
      AccessProjectileData proj = (AccessProjectileData) projectile;
      source = new SkadaDamageSource(event.getSource(), proj.getDamageInfo());
      isProjectile = true;
      if (DEBUG_ENABLED) Skada.LOGGER.debug("Damage source is a projectile with damage info: {}", proj.getDamageInfo());
    } else {
      if (DEBUG_ENABLED) Skada.LOGGER.debug("Damage source is not a SkadaDamageSource, creating environmental source.");
      source = SkadaDamageSource.environmental(event.getSource());
    }
    DamageInfo info = source.getInfo();
    ElementSpreadInstance spread = info.elementSpreadInstance();
    double armour = target.getAttributeValue(Attributes.ARMOR);
    if (DEBUG_ENABLED) Skada.LOGGER.debug("Damage info: {}, Element spread: {}, Armour: {}", info, spread, armour);

    if (source.is(SkadaDamageTypeTags.CANCELLED_BY_ARMOUR)) {
      if (armour != 0) {
        if (DEBUG_ENABLED) Skada.LOGGER.debug("Damage is cancelled by armour, cancelling event.");
        event.setCanceled(true);
        return;
      }
    }

    if (!info.isEnvironmental()) {

      if (CommonConfig.ENABLE_PRECISION.get() && CommonConfig.ENABLE_PRECISION_FOR_MELEE.get() && !isProjectile) {
        if (DEBUG_ENABLED) Skada.LOGGER.debug("Pre-precision damage: {}", amount);
        amount = DamageMath.getDamageFromPrecisionNormalDistribution(
          info.precision(),
          target.getAttributeValue(Attributes.ARMOR_TOUGHNESS),
          amount,
          event.getEntity().getRandom());
        if (DEBUG_ENABLED) Skada.LOGGER.debug("Post-precision damage: {}", amount);
      }

      if (CommonConfig.ENABLE_LETHALITY.get()) {
        double lethality = info.lethality();
        double toughness = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        double healthContext = target.getHealth();

        if (DEBUG_ENABLED) Skada.LOGGER.debug("Lethality: {}, Toughness: {}", lethality, toughness);

        if (DEBUG_ENABLED) Skada.LOGGER.debug("Pre-lethality damage: {}", amount);
        switch (info.attackType().type().getOperation()) {
          case SUM_WITH_DAMAGE -> amount += info.attackType().type().apply(lethality, armour, healthContext);
          case SUM_WITH_ARMOUR -> armour += info.attackType().type().apply(lethality, armour, healthContext);
          case MULTIPLY_WITH_DAMAGE -> amount *= info.attackType().type().apply(lethality, armour, healthContext);
          case MULTIPLY_WITH_ARMOUR -> armour *= info.attackType().type().apply(lethality, armour, healthContext);
        }
        if (DEBUG_ENABLED) Skada.LOGGER.debug("Post-lethality damage: {}", amount);
      }

      //ATTACK TYPE WEAKNESSES
      AttackType attackType = info.attackType();
      if (DEBUG_ENABLED) Skada.LOGGER.debug("Pre-attack type damage: {}", amount);
      amount = DamageMath.getDamageAfterAttackTypeReduction(amount, target.getAttributeValue(attackType.resistAttribute()));
      if (DEBUG_ENABLED) Skada.LOGGER.debug("Post-attack type damage: {}", amount);

    }

    //ARMOUR FORMULA
    if (DEBUG_ENABLED) Skada.LOGGER.debug("Pre-armour damage: {}", amount);
    amount = DamageMath.getDamageAfterArmourReduction(amount, armour);
    if (DEBUG_ENABLED) Skada.LOGGER.debug("Post-armour damage: {}", amount);


    //Enchantment and potion effects, copied from LivingEntity#getDamageAfterMagicAbsorb
    if (!source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
      if (target.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
        int i = (target.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
        int j = 25 - i;
        double f = amount * (float) j;
        double f1 = amount;
        amount = Math.max(f / 25.0F, 0.0F);
        double f2 = f1 - amount;
        if (f2 > 0.0F && f2 < VANILLA_FLOAT_MAX_VALUE) {
          if (target instanceof ServerPlayer) {
            ((ServerPlayer) target).awardStat(Stats.DAMAGE_RESISTED, (int) Math.round(f2 * 10.0F));
          } else if (source.getEntity() instanceof ServerPlayer) {
            ((ServerPlayer) source.getEntity()).awardStat(Stats.DAMAGE_RESISTED, (int) Math.round(f2 * 10.0F));
          }
        }
        if (DEBUG_ENABLED) Skada.LOGGER.debug("Post-resistance damage: {}", amount);
      }
      if (!source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
        int k = EnchantmentHelper.getDamageProtection(target.getArmorSlots(), source);
        if (k > 0) {
          amount = CombatRules.getDamageAfterMagicAbsorb((float) amount, (float) k);
        }
        if (DEBUG_ENABLED) Skada.LOGGER.debug("Post-enchantment damage: {}", amount);
      }
    }

    //ELEMENTAL WEAKNESSES AND AFFINITIES
    spread.transform(amount);
    for (Element element : spread.getElements().keySet()) {
      if (source.getEntity() instanceof LivingEntity le) {
        if (DEBUG_ENABLED) Skada.LOGGER.debug("Element: {}, Base damage: {}, Affinity: {}, Pre-resistance damage: {}, Target Resistance: {}",
                element.name(),
                le.getAttributeValue(element.baseDamage()),
                (1 + le.getAttributeValue(element.affinity())),
                spread.getElements().get(element),
                target.getAttributeValue(element.resist()));
        spread.applyFunctionToElement(element, x -> x + le.getAttributeValue(element.baseDamage()));
        spread.applyFunctionToElement(element, x -> x * (1 + le.getAttributeValue(element.affinity())));
      }
      spread.applyFunctionToElement(element, x -> DamageMath.getDamageAfterElementalResistance(x, target.getAttributeValue(element.resist())));
      if (DEBUG_ENABLED) Skada.LOGGER.debug("Element: {}, Final damage: {}", element.name(), spread.getElements().get(element));
    }
    PostMitigationEvent evt = new PostMitigationEvent(target, spread.getElements());
    MinecraftForge.EVENT_BUS.post(evt);
    event.setAmount(evt.getTotalDamage());
    if (target.getServer() != null) target.getServer().getProfiler().pop();
  }

}
