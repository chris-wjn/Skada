package com.cwjn.skada.util;

import com.cwjn.skada.CommonConfig;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Utility class for combat-related functions, such as damage calculations, attack type and element handling, and other combat mechanics.
 * This class is not clientsided, as it is also used for server-side operations.
 */
public abstract class UtilCombat {

  private static final double SLASH_LETHALITY_MULTIPLIER_MAX_BONUS = 0.55;
  private static final double SLASH_LETHALITY_MULTIPLIER_SCALE = 40.0;
  private static final double THRUST_LETHALITY_HEALTH_SHARE_CAP = 0.14;
  private static final double THRUST_LETHALITY_HEALTH_SHARE_SCALE = 28.0;
  private static final double STRIKE_LETHALITY_PENETRATION_CAP = 0.60;
  private static final double STRIKE_LETHALITY_ARMOUR_SCALE = 2.5;
  private static final double STRIKE_LETHALITY_BASELINE_SCALE = 18.0;

  /**
   * Converts slash lethality into a bounded damage multiplier.
   *
   * <p>What this method does
   * <ul>
   *   <li>Takes a raw lethality value and turns it into a multiplicative damage bonus.</li>
   *   <li>Uses an exponential diminishing-returns curve so the first points of lethality matter
   *       the most and later points add progressively less value.</li>
   *   <li>Returns {@code 1.0} when lethality is zero or negative, so the method never lowers damage.</li>
   * </ul>
   *
   * <p>Why this shape is used
   * <ul>
   *   <li>Slash attacks should feel immediately rewarding without creating runaway scaling.</li>
   *   <li>A multiplier is easy to compose with the rest of the damage pipeline.</li>
   *   <li>The cap keeps slash lethality useful at high values without letting it dominate every other stat.</li>
   * </ul>
   *
   * <p>Important math breakpoints with the current constants
   * <ul>
   *   <li>Cap: {@code 1.0 + SLASH_LETHALITY_MULTIPLIER_MAX_BONUS = 1.55}.</li>
   *   <li>{@code lethality = 0} -> multiplier = {@code 1.0}.</li>
   *   <li>{@code lethality = 40} (one scale) -> multiplier is about {@code 1.35}; roughly 63% of the cap bonus is reached.</li>
   *   <li>{@code lethality = 80} (two scales) -> multiplier is about {@code 1.48}; roughly 86% of the cap bonus is reached.</li>
   *   <li>{@code lethality = 120} (three scales) -> multiplier is about {@code 1.52}; roughly 95% of the cap bonus is reached.</li>
   * </ul>
   *
   * @param lethality the lethality value of the attack
   * @param armour the target's armour value; retained for API compatibility and not used here
   * @param targetHP the target's current health; retained for API compatibility and not used here
   * @return the bonus damage multiplier, always at least {@code 1.0}
   */
  public static double percentBonusDamage(double lethality, double armour, double targetHP) {
    if (lethality <= 0.0) return 1.0;
    double bonusShare = SLASH_LETHALITY_MULTIPLIER_MAX_BONUS
      * (1.0 - Math.exp(-lethality / SLASH_LETHALITY_MULTIPLIER_SCALE));
    return 1.0 + bonusShare;
  }

  /**
   * Converts thrust lethality into a percentage of current health.
   *
   * <p>What this method does
   * <ul>
   *   <li>Transforms lethality into a share of the target's current health.</li>
   *   <li>Multiplies that share by {@code targetHP}, producing an absolute damage amount.</li>
   *   <li>Uses the same diminishing-returns pattern as the slash curve, but expresses the output as a health fraction instead of a multiplier.</li>
   * </ul>
   *
   * <p>Why this shape is used
   * <ul>
   *   <li>Thrust attacks are meant to be counter hp-stackers.</li>
   *   <li>A capped current-HP damage means it isn't as strong vs low max-hp targets.</li>
   *   <li>Because the result scales from current health, the same lethality remains relevant at different enemy sizes without becoming a pure fixed-damage nuke.</li>
   * </ul>
   *
   * <p>Important math breakpoints with the current constants
   * <ul>
   *   <li>Cap: {@code THRUST_LETHALITY_HEALTH_SHARE_CAP = 0.14}; the method can remove at most 14% of current health.</li>
   *   <li>{@code lethality = 0} or {@code targetHP <= 0} -> damage = {@code 0}.</li>
   *   <li>{@code lethality = 28} (one scale) -> about 8.9% of current HP.</li>
   *   <li>{@code lethality = 56} (two scales) -> about 12.1% of current HP.</li>
   *   <li>{@code lethality = 84} (three scales) -> about 13.3% of current HP.</li>
   * </ul>
   *
   * @param lethality the lethality value of the attack
   * @param armour the target's armour value; retained for API compatibility and not used here
   * @param targetHP the target's current health
   * @return the damage amount to add to the current damage total
   */
  public static double percentHealthDamage(double lethality, double armour, double targetHP) {
    if (lethality <= 0.0 || targetHP <= 0.0) return 0.0;
    double healthShare = THRUST_LETHALITY_HEALTH_SHARE_CAP
      * (1.0 - Math.exp(-lethality / THRUST_LETHALITY_HEALTH_SHARE_SCALE));
    return healthShare * targetHP;
  }

  /**
   * Converts strike lethality into an armour multiplier.
   *
   * <p>What this method does
   * <ul>
   *   <li>Computes a bounded penetration share from lethality only.</li>
   *   <li>Returns a multiplier in the range {@code [1 - cap, 1.0]} that is applied to the target's armour.</li>
   *   <li>Higher lethality means more armour is bypassed, but the bypass amount never exceeds the configured cap.</li>
   * </ul>
   *
   * <p>Why this shape is used
   * <ul>
   *   <li>Strike attacks should reward lethality by weakening armour rather than directly multiplying raw damage.</li>
   *   <li>Armour should not make strike lethality less effective; otherwise armour becomes a universal counter to two different offensive axes at once.</li>
   *   <li>The baseline term prevents small lethality values from producing outsized penetration.</li>
   * </ul>
   *
   * <p>Important math breakpoints with the current constants
   * <ul>
   *   <li>Cap: {@code STRIKE_LETHALITY_PENETRATION_CAP = 0.60}, so the returned multiplier never goes below {@code 0.40}.</li>
   *   <li>{@code lethality = 0} -> multiplier = {@code 1.0}.</li>
   *   <li>{@code lethality = 18} (one baseline scale) -> penetration is about half the cap, so the multiplier is about {@code 0.70}.</li>
   *   <li>{@code lethality = 36} -> penetration is about 67% of the cap, so the multiplier is about {@code 0.60}.</li>
   *   <li>{@code lethality = 72} -> penetration is about 80% of the cap, so the multiplier is about {@code 0.52}.</li>
   *   <li>As {@code lethality} grows very large, penetration approaches the cap and the multiplier approaches {@code 0.40}.</li>
   * </ul>
   *
   * @param lethality the lethality value of the attack
   * @param armour the target's current armour; retained for API compatibility and not used here
   * @param targetHP the target's current health; retained for API compatibility and not used here
   * @return a multiplier to apply to the target's armour, always between {@code 0.40} and {@code 1.0}
   */
  public static double percentReduc(double lethality, double armour, double targetHP) {
    if (lethality <= 0.0) return 1.0;
    double penetrationShare = STRIKE_LETHALITY_PENETRATION_CAP
      * lethality / (lethality + STRIKE_LETHALITY_BASELINE_SCALE);
    return Math.max(1.0 - STRIKE_LETHALITY_PENETRATION_CAP, 1.0 - penetrationShare);
  }

  private static final double PRECISION_SCORE_OFFSET = 5.0;
  private static final double PRECISION_TOUGHNESS_WEIGHT = 0.5;
  private static final double PRECISION_CONSISTENCY_MIN = 0.0;
  private static final double PRECISION_CONSISTENCY_MAX = 0.98;
  private static final double PROJECTILE_INACCURACY_MAX = 15.0;
  public static final double ANGULAR_VELOCITY_BASE = 10.0;
  public static final double ANGULAR_VELOCITY_REFERENCE_INERTIA = 0.00015;
  public static final double ANGULAR_VELOCITY_INERTIA_EXPONENT = 0.4;
  public static final double ANGULAR_VELOCITY_STRENGTH_REFERENCE = 50.0;
  public static final double ANGULAR_VELOCITY_MAX = 200.0;

  public static double precisionScoreToConsistency(double rawPrecision) {
    return precisionScoreToConsistency(rawPrecision, 0.0);
  }

  public static double precisionScoreToConsistency(double rawPrecision, double targetToughness) {
    double safePrecision = Math.max(0.0, rawPrecision);
    double safeToughness = Math.max(0.0, targetToughness);
    double denominator = safePrecision + PRECISION_SCORE_OFFSET + PRECISION_TOUGHNESS_WEIGHT * safeToughness;
    if (denominator <= 0.0) {
      return PRECISION_CONSISTENCY_MIN;
    }
    double consistency = safePrecision / denominator;
    return Math.max(PRECISION_CONSISTENCY_MIN, Math.min(PRECISION_CONSISTENCY_MAX, consistency));
  }

  public static double precisionScoreToProjectileInaccuracy(double rawPrecision) {
    return PROJECTILE_INACCURACY_MAX * (1.0 - precisionScoreToConsistency(rawPrecision));
  }

  public static double projectileVelocityWithDamageBonus(double baseVelocity, double damageBonus) {
    return Math.max(0.0, baseVelocity + damageBonus);
  }

  public enum CriticalFailSeverity {
    NONE,
    EDGE_DAMAGE,
    DEFORMATION,
    CATASTROPHIC
  }

  private static final String CRITICAL_FAIL_EDGE_DAMAGE_TAG = "skadaCriticalFailEdgeDamage";
  private static final String CRITICAL_FAIL_DEFORMATION_TAG = "skadaCriticalFailDeformation";
  private static final double EDGE_DAMAGE_DURABILITY_LOSS = 0.01;
  private static final double BASE_DEFORMATION_SEVERITY_SHARE = 0.18;
  private static final double BASE_CATASTROPHIC_SEVERITY_SHARE = 0.02;
  private static final double EDGE_DAMAGE_TO_DEFORMATION_WEIGHT = 0.03;
  private static final double EDGE_DAMAGE_TO_CATASTROPHIC_WEIGHT = 0.01;
  private static final double DEFORMATION_TO_DEFORMATION_WEIGHT = 0.05;
  private static final double DEFORMATION_TO_CATASTROPHIC_WEIGHT = 0.08;
  private static final double FAIL_CHANCE_TO_DEFORMATION_WEIGHT = 0.80;
  private static final double FAIL_CHANCE_TO_CATASTROPHIC_WEIGHT = 0.35;

  /*
   * Roll for a critical fail and apply a severity tier.
   * Edge damage is the common light outcome, deformation is the moderate outcome,
   * and catastrophic failure breaks the item outright.
   */
  @SuppressWarnings("null")
  public static void rollCriticalFail(ItemStack item, double chance, RandomSource random, ServerPlayer player) {
    if (!CommonConfig.ENABLE_CRITICAL_FAIL.get()) return;
    if (player.getAbilities().instabuild) return;
    CriticalFailSeverity severity = sampleCriticalFailSeverity(item, chance, random);
    if (severity == CriticalFailSeverity.NONE) {
      return;
    }
    applyCriticalFail(item, severity, player);
  }

  public static CriticalFailSeverity sampleCriticalFailSeverity(ItemStack item, double chance, RandomSource random) {
    int edgeDamage = criticalFailEdgeDamageCount(item);
    int deformation = criticalFailDeformationCount(item);
    return criticalFailSeverity(chance, random.nextDouble(), random.nextDouble(), edgeDamage, deformation);
  }

  static CriticalFailSeverity criticalFailSeverity(double chance, double triggerRoll, double severityRoll, int edgeDamage, int deformation) {
    if (triggerRoll >= chance) {
      return CriticalFailSeverity.NONE;
    }

    double catastrophicShare = Mth.clamp(
      BASE_CATASTROPHIC_SEVERITY_SHARE
        + EDGE_DAMAGE_TO_CATASTROPHIC_WEIGHT * Math.max(0, edgeDamage)
        + DEFORMATION_TO_CATASTROPHIC_WEIGHT * Math.max(0, deformation)
        + FAIL_CHANCE_TO_CATASTROPHIC_WEIGHT * Math.max(0.0, chance),
      0.02,
      0.35);
    double deformationShare = Mth.clamp(
      BASE_DEFORMATION_SEVERITY_SHARE
        + EDGE_DAMAGE_TO_DEFORMATION_WEIGHT * Math.max(0, edgeDamage)
        + DEFORMATION_TO_DEFORMATION_WEIGHT * Math.max(0, deformation)
        + FAIL_CHANCE_TO_DEFORMATION_WEIGHT * Math.max(0.0, chance),
      0.18,
      0.78 - catastrophicShare);

    if (severityRoll < catastrophicShare) {
      return CriticalFailSeverity.CATASTROPHIC;
    }
    if (severityRoll < catastrophicShare + deformationShare) {
      return CriticalFailSeverity.DEFORMATION;
    }
    return CriticalFailSeverity.EDGE_DAMAGE;
  }

  private static void applyCriticalFail(ItemStack item, CriticalFailSeverity severity, ServerPlayer player) {
    CompoundTag tag = item.getOrCreateTag();
    switch (severity) {
      case EDGE_DAMAGE -> {
        tag.putInt(CRITICAL_FAIL_EDGE_DAMAGE_TAG, criticalFailEdgeDamageCount(item) + 1);
        hurtItemByFraction(item, EDGE_DAMAGE_DURABILITY_LOSS, player);
      }
      case DEFORMATION -> {
        tag.putInt(CRITICAL_FAIL_DEFORMATION_TAG, criticalFailDeformationCount(item) + 1);
        hurtItemByFraction(item, CommonConfig.CRITICAL_FAIL_DURABILITY_LOSS.get(), player);
      }
      case CATASTROPHIC -> item.hurtAndBreak(
        item.getDamageValue(),
        player,
        p -> p.broadcastBreakEvent(player.getUsedItemHand()));
      case NONE -> {
      }
    }
  }

  private static void hurtItemByFraction(ItemStack item, double durabilityLossFraction, ServerPlayer player) {
    int amount = criticalFailDurabilityLoss(item.getMaxDamage(), item.getDamageValue(), durabilityLossFraction);
    item.hurtAndBreak(amount, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
  }

  static int criticalFailDurabilityLoss(int maxDamage, int remainingDurability, double durabilityLossFraction) {
    if (maxDamage <= 0 || remainingDurability <= 0) {
      return 0;
    }
    int scaledLoss = (int) Math.ceil(maxDamage * Math.max(0.0, durabilityLossFraction));
    return Math.max(1, Math.min(remainingDurability, scaledLoss));
  }

  static int criticalFailEdgeDamageCount(ItemStack item) {
    return item.getOrCreateTag().getInt(CRITICAL_FAIL_EDGE_DAMAGE_TAG);
  }

  static int criticalFailDeformationCount(ItemStack item) {
    return item.getOrCreateTag().getInt(CRITICAL_FAIL_DEFORMATION_TAG);
  }

  public static double tridentProjectileVelocity(double baseVelocity, double damageBonus) {
    return projectileVelocityWithDamageBonus(baseVelocity, damageBonus);
  }

}