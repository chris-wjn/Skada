package com.cwjn.skada.damage;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.Skada;
import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.ElementSpreadInstance;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.event.custom.PostMitigationEvent;
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
import static com.cwjn.skada.util.ConsoleColour.*;

@Mod.EventBusSubscriber
public class DamageHandler {

  /**
   * Minimum damage multiplier for precision system.
   * Damage cannot be reduced below this fraction of the base damage value.
   */
  private static final double MIN_DAMAGE_MULTIPLIER = 0.5;

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
    if (DEBUG_ENABLED) Skada.LOGGER.debug(UNDERLINE + "Damage event for entity: {}, initial damage: {}, source: {}" + RESET, target.getName().getString(), event.getAmount(), event.getSource().getMsgId());
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
        amount = getDamageFromPrecisionNormalDistribution(info.precision(), amount, event.getEntity().getRandom());
        if (DEBUG_ENABLED) Skada.LOGGER.debug("Post-precision damage: {}", amount);
      }

      if (CommonConfig.ENABLE_LETHALITY.get()) {
        double lethality = info.lethality();
        double toughness = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        if (DEBUG_ENABLED) Skada.LOGGER.debug("Lethality: {}, Toughness: {}", lethality, toughness);

        if (DEBUG_ENABLED) Skada.LOGGER.debug("Pre-lethality damage: {}", amount);
        switch (info.attackType().type().getOperation()) {
          case SUM_WITH_DAMAGE -> amount += info.attackType().type().apply(lethality, toughness, target.getMaxHealth());
          case SUM_WITH_ARMOUR -> armour += info.attackType().type().apply(lethality, toughness, target.getMaxHealth());
          case MULTIPLY_WITH_DAMAGE -> amount *= info.attackType().type().apply(lethality, toughness, target.getMaxHealth());
          case MULTIPLY_WITH_ARMOUR -> armour *= info.attackType().type().apply(lethality, toughness, target.getMaxHealth());
        }
        if (DEBUG_ENABLED) Skada.LOGGER.debug("Post-lethality damage: {}", amount);
      }

      //ATTACK TYPE WEAKNESSES
      AttackType attackType = info.attackType();
      if (DEBUG_ENABLED) Skada.LOGGER.debug("Pre-attack type damage: {}", amount);
      amount = getDamageAfterAttackTypeReduction(amount, target.getAttributeValue(attackType.resistAttribute()));
      if (DEBUG_ENABLED) Skada.LOGGER.debug("Post-attack type damage: {}", amount);

    }

    //ARMOUR FORMULA
    if (DEBUG_ENABLED) Skada.LOGGER.debug("Pre-armour damage: {}", amount);
    amount = getDamageAfterArmourReduction(amount, armour);
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
      spread.applyFunctionToElement(element, x -> getDamageAfterElementalResistance(x, target.getAttributeValue(element.resist())));
      if (DEBUG_ENABLED) Skada.LOGGER.debug("Element: {}, Final damage: {}", element.name(), spread.getElements().get(element));
    }
    PostMitigationEvent evt = new PostMitigationEvent(target, spread.getElements());
    MinecraftForge.EVENT_BUS.post(evt);
    event.setAmount(evt.getTotalDamage());
    if (target.getServer() != null) target.getServer().getProfiler().pop();
  }

  /**
   * Armour reduction formula, using a logistic curve to convert armour points to percentage resistance.
   *  <pre>
   *    if x >= 0, y = 100 / (1 + e^((-x/10) + 2))
   *    if x < 0, y = -100 / (1 + e^((x/10) + 2))
   *  </pre>
   * @param damage The damage to be reduced.
   * @param armour The armour value, can be negative.
   * @return The damage after armour reduction.
   */
  private static double getDamageAfterArmourReduction(double damage, double armour) {
    //If the target has no armour, the attack does full damage.
    if (armour == 0) return damage;
    //Convert armour points to a percentage resistance. If armour is negative, the formula is inverted.
    double resistance = armour < 0 ? -100 / (1 + Math.exp((armour / 10) + 2)) : 100 / (1 + Math.exp((-armour / 10) + 2));
    //If the resistance is >= 100% (somehow), do no damage, otherwise, return the damage after reduction.
    return resistance >= 100 ? 0 : damage * (1 - resistance / 100);
  }

  /**
   * Uses a radical formula to reduce damage based on elemental resistance.
   *  <pre>
   *      if x >= 0, y = √(x/4)
   *      if x < 0, y = -√(-x/4)
   *  </pre>
   * @param damage The damage to be reduced.
   * @param resistance The resistance value, can be negative.
   * @return The damage after elemental resistance reduction.
   */
  private static double getDamageAfterElementalResistance(double damage, double resistance) {
    return damage * (1 - (resistance > 0 ? Math.sqrt(resistance / 4) : -Math.sqrt(-resistance / 4)));
  }

  /**
   * Get a percentage of damage reduced based on the resistance value, using the damage class formula.
   * The result of this function should be multiplied with the value to be reduced.
   *  <pre>
   *    y = 100 / (100 + x)
   *  </pre>
   * @param damage The damage to be reduced.
   * @param resistance The resistance value, can be negative.
   * @return The damage after reduction.
   */
  private static double getDamageAfterAttackTypeReduction(double damage, double resistance) {
    return damage * (100/(100+resistance));
  }

  /**
   * Calculates damage based on precision using a normal distribution. Higher precision results
   * in lower standard deviation, leading to more consistent damage.
   *
   * @param precision The precision value (0.0 to 1.0).
   * @param damage The initial damage value.
   * @param random The random source for generating normal distribution values.
   * @return The damage after applying precision adjustments.
   */
  private static double getDamageFromPrecisionNormalDistribution(double precision, double damage, RandomSource random) {
    // Higher precision means lower standard deviation (more consistent damage)
    double standardDeviation = (1.0 - precision) * damage;

    // Generate a random number from a normal distribution
    double z = random.nextGaussian();

    // Since the mean is the original damage value, any damage value beyond the mean should
    // be treated as a negative number. We'll take the absolute value of z to ensure
    // we're always reducing the damage.
    double normalDistributionModifier = Math.abs(z) * standardDeviation;

    // Calculate final damage using the normal distribution
    // Clamp the result between MIN_DAMAGE_MULTIPLIER and the original damage value
    return Math.max(damage * MIN_DAMAGE_MULTIPLIER, damage - normalDistributionModifier);
  }

  /*
   * This method increases the element spread based on the attacker's finesse and the defender's mobility. Uses
   * a roll system, with 4 tiers.
   */
  private static int finesseMobilityFormula(ElementSpreadInstance spread, double finesse, double mobility, double secondaryStat, double agility, RandomSource random) {
    int difference = Math.min((int) ((finesse * secondaryStat * 0.1) - (mobility * agility * 0.1)), 300);
    if (difference == 0) return 0;
    if (difference > 0) {
      int extraHits = 0;
      while (true) {
        double rate = 0.5 - extraHits * 0.25;
        if (extraHits == 1) rate = 0.33;
        if (difference < 100) {
          if (rate * difference * 0.01 > random.nextDouble()) {
            extraHits++;
          }
          break;
        } else {
          difference -= 100;
          if (rate > random.nextDouble()) {
            extraHits++;
          } else {
            break;
          }
        }
      }
      return extraHits;
    } else {
      difference *= -1;
      int tier = 0;
      double damageReduc = 0.0;
      while (true) {
        double rate = 0.5 - tier * 0.25;
        if (tier == 1) rate = 0.33;
        if (difference < 100) {
          if (rate * difference * 0.01 > random.nextDouble()) {
            damageReduc += 16.5;
          }
          break;
        } else {
          difference -= 100;
          if (rate > random.nextDouble()) {
            damageReduc += 16.5;
            tier++;
          } else {
            break;
          }
        }
      }
      double finalDamageReduc = damageReduc;
      spread.applyFunctionToAll(d -> d * (1 - finalDamageReduc * 0.01));
    }
    return 0;
  }

}
