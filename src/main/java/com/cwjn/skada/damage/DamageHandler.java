package com.cwjn.skada.damage;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.Skada;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.ElementSpreadInstance;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.event.custom.PostMitigationEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.MinecraftForge;

import java.util.Optional;

import static com.cwjn.skada.data.SkadaData.DEBUG_ENABLED;
import static com.cwjn.skada.util.UtilColour.*;

/** Shared damage calculation invoked by Skada's damage-pipeline mixins. */
public class DamageHandler {

  /**
   * Float.MAX_VALUE literal for compatibility with vanilla damage resistance stat tracking.
   * Used to check for overflow in damage resistance calculations (copied from vanilla LivingEntity).
   */
  private static final float VANILLA_FLOAT_MAX_VALUE = 3.4028235E37F;

  /**
   * Calculates damage after Forge's hurt listeners and before absorption hearts.
   * An uncancelled zero input can still carry the attacker's base elemental damage.
   *
   * @return the mitigated damage, or zero when Skada cancels the hit
   */
  public static float calculateDamage(LivingEntity target, DamageSource incomingSource, float incomingAmount) {
    if (target.getServer() != null) target.getServer().getProfiler().push("SkadaDamageHandler");
    try {
      SkadaDamageSource source;
      boolean isProjectile = false;
      if (DEBUG_ENABLED) Skada.LOGGER.debug(CONSOLE_UNDERLINE + "Damage calculation for entity: {}, initial damage: {}, source: {}" + CONSOLE_RESET, target.getName().getString(), incomingAmount, incomingSource.getMsgId());
      double amount = incomingAmount;
      if (incomingSource instanceof SkadaDamageSource) {
        if (DEBUG_ENABLED) Skada.LOGGER.debug("Damage source is a SkadaDamageSource.");
        source = (SkadaDamageSource) incomingSource;
      } else if (incomingSource.getDirectEntity() instanceof Projectile projectile) {
        Optional<AccessProjectileData> projData = resolveProjectileData(projectile);
        if (projData.isPresent()) {
          source = new SkadaDamageSource(incomingSource, projData.get().getDamageInfo());
          isProjectile = true;
          if (DEBUG_ENABLED) Skada.LOGGER.debug("Damage source is a projectile with damage info: {}", projData.get().getDamageInfo());
        } else {
          if (DEBUG_ENABLED) Skada.LOGGER.debug("Projectile has no Skada damage snapshot, treating as environmental.");
          source = SkadaDamageSource.environmental(incomingSource);
        }
      } else {
        if (DEBUG_ENABLED) Skada.LOGGER.debug("Damage source is not a SkadaDamageSource, creating environmental source.");
        source = SkadaDamageSource.environmental(incomingSource);
      }
      DamageInfo info = source.getInfo();
      ElementSpreadInstance spread = info.elementSpreadInstance().copy();
      double armour = target.getAttributeValue(Attributes.ARMOR);
      if (DEBUG_ENABLED) Skada.LOGGER.debug("Damage info: {}, Element spread: {}, Armour: {}", info, spread, armour);

      if (source.is(SkadaDamageTypeTags.CANCELLED_BY_ARMOUR)) {
        if (armour != 0) {
          if (DEBUG_ENABLED) Skada.LOGGER.debug("Damage is cancelled by armour.");
          return 0.0F;
        }
      }

      if (!info.isEnvironmental()) {

        if (CommonConfig.ENABLE_PRECISION.get() && CommonConfig.ENABLE_PRECISION_FOR_MELEE.get() && !isProjectile) {
          if (DEBUG_ENABLED) Skada.LOGGER.debug("Pre-precision damage: {}", amount);
          amount = DamageMath.getDamageFromPrecisionNormalDistribution(
            info.precision(),
            target.getAttributeValue(Attributes.ARMOR_TOUGHNESS),
            amount,
            target.getRandom());
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
              ((ServerPlayer) source.getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, (int) Math.round(f2 * 10.0F));
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

      //ELEMENTAL WEAKNESSES, AFFINITIES, AND PHYSICAL RESISTANCE
      double effectiveArmour = armour;
      spread.transform(amount);
      if (source.getEntity() instanceof LivingEntity attacker) {
        // Base elemental damage exists independently of the weapon's spread.
        for (Element element : SkadaData.REGISTRY_ELEMENT.get().getValues()) {
          if (attacker.getAttributeValue(element.baseDamage()) > 0.0) {
            spread.getElements().putIfAbsent(element, 0.0F);
          }
        }
      }
      if (DEBUG_ENABLED) Skada.LOGGER.debug("Post-transform element values: {}", spread.getElements());
      for (Element element : spread.getElements().keySet()) {
        double elementResistance = element.usesArmourAsResistance() ? effectiveArmour : target.getAttributeValue(element.resist());
        if (source.getEntity() instanceof LivingEntity le) {
          double baseDamage = le.getAttributeValue(element.baseDamage());
          double affinityMultiplier = 1 + le.getAttributeValue(element.affinity());
          spread.applyFunctionToElement(element, x -> x + baseDamage);
          spread.applyFunctionToElement(element, x -> x * affinityMultiplier);
          if (DEBUG_ENABLED) Skada.LOGGER.debug("Element: {}, Base damage: {}, Affinity: {}, Pre-resistance damage: {}, Target Resistance: {}",
                  element.name(),
                  baseDamage,
                  affinityMultiplier,
                  spread.getElements().get(element),
                  elementResistance);
        } else if (DEBUG_ENABLED) {
          Skada.LOGGER.debug("Element: {}, Pre-resistance damage: {}, Target Resistance: {}",
                  element.name(),
                  spread.getElements().get(element),
                  elementResistance);
        }
        if (element.usesArmourAsResistance()) {
          spread.applyFunctionToElement(element, x -> DamageMath.getDamageAfterArmourReduction(x, effectiveArmour));
        } else {
          spread.applyFunctionToElement(element, x -> DamageMath.getDamageAfterElementalResistance(x, elementResistance));
        }
        if (DEBUG_ENABLED) Skada.LOGGER.debug("Element: {}, Final damage: {}", element.name(), spread.getElements().get(element));
      }
      PostMitigationEvent evt = new PostMitigationEvent(target, spread.getElements());
      MinecraftForge.EVENT_BUS.post(evt);
      return evt.getTotalDamage();
    } finally {
      if (target.getServer() != null) target.getServer().getProfiler().pop();
    }
  }

  /**
   * Resolves projectile data from an entity. Returns empty if the entity does not implement
   * {@link AccessProjectileData} or if the projectile never received a Skada combat snapshot.
   * Package-private to allow unit testing without Forge bootstrap.
   */
  static Optional<AccessProjectileData> resolveProjectileData(Entity projectile) {
    if (projectile instanceof AccessProjectileData data && data.hasDamageInfo()) return Optional.of(data);
    return Optional.empty();
  }

}
