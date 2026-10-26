package com.cwjn.skada.util;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.network.SkadaNetwork;
import com.cwjn.skada.network.server_to_client.S2CPlayCriticalFailSound;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
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
   * Gets the total damage for a given element, including weapon spread, elemental base damage,
   * and elemental affinity.
   *
   * <p>The current formula matches the element info UI:
   * {@code (baseDamage + spreadDamage) * (1 + affinity)}.
   *
   * @param p the player whose combat stats should be evaluated
   * @param e the element to evaluate
   * @return the total damage for the specified element, or {@code 0.0} if the player or element is null
   */
  public static double getElementalDamageForElement(Player p, Element e) {
    if (p == null || e == null) {
      return 0.0;
    }

    double weaponDamage = UtilData.getPlayerAttackWeaponInfo(p).getSpread().getDamageFromElementRatio(
      p.getAttributeValue(Attributes.ATTACK_DAMAGE),
      e);
    double baseDamage = p.getAttributeValue(e.baseDamage());
    double affinityMultiplier = p.getAttributeValue(e.affinity()) + 1.0D;
    return (baseDamage + weaponDamage) * affinityMultiplier;
  }

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

  private static final double PRECISION_CONSISTENCY_MIN = 0.30;
  private static final double PRECISION_CONSISTENCY_MAX = 0.98;
  private static final double PRECISION_CONSISTENCY_PARITY = 0.65;
  private static final double PRECISION_CONSISTENCY_RATIO_CAP = 2.0;
  private static final double PROJECTILE_ACCURACY_PRECISION_SCALE = 5.0;
  private static final double PROJECTILE_ACCURACY_MAX = 0.98;
  private static final double PROJECTILE_INACCURACY_MAX = 15.0;
  // ---- Angular velocity derivation constants (torque → α → ω chain) ----

  /** Torque applied per unit of player strength (N·m per strength point).
   *  Calibrated so that at strength 50.0 the effective torque ≈ 0.003 N·m. */
  public static final double TORQUE_PER_STRENGTH = 6.0e-5;

  /** Default swing arc for rotational attacks (radians).
   *  ~143° (2.5 rad) represents a typical full-body swing arc. */
  public static final double SWING_ARC_DEFAULT = 2.5;

  /** Reference moment of inertia for skill compensation normalisation (kg·m²).
   *  At this inertia the skill-compensation factor is 1.0 regardless of the
   *  exponent, so the calibration anchor never drifts.
   *  ~0.00015 kg·m² ≈ typical longsword. */
  public static final double REFERENCE_INERTIA = 0.00015;

  /** Default inertia-compensation exponent (wielder skill).
   *  0.5 = pure Newtonian physics (no compensation),
   *  0.4 = HEMA-trained fighter (current calibrated value),
   *  lower = more skilled (heavy weapons penalised less). */
  public static final double SKILL_EXPONENT_DEFAULT = 0.4;

  /** Upper clamp on angular velocity to prevent absurd values. */
  public static final double ANGULAR_VELOCITY_MAX = 200.0;

  /**
   * Converts a raw precision score into a consistency fraction assuming a target with zero
   * armour toughness.
   *
   * <p>Zero toughness always yields {@code PRECISION_CONSISTENCY_MAX = 0.98}; there is nothing
   * left to degrade the attack's reliability. Delegates to
   * {@link #precisionScoreToConsistency(double, double)} with {@code targetToughness = 0.0}.
   *
   * @param rawPrecision the generated precision score for this attack
   * @return the consistency fraction, always {@code 0.98} for this overload
   */
  public static double precisionScoreToConsistency(double rawPrecision) {
    return precisionScoreToConsistency(rawPrecision, 0.0);
  }

  /**
   * Converts a raw precision score into a consistency fraction in the range
   * {@code [0.30, 0.98]}. Consistency describes how tightly damage rolls cluster around
   * their expected value: {@code 0.30} means heavy variance and {@code 0.98} means nearly
   * deterministic damage.
   *
   * <p>What this method does
   * <ul>
   *   <li>Compares offence to defence with the ratio {@code r = precision / toughness}.</li>
   *   <li>Maps the ratio through a piecewise-linear ramp: {@code 0.30 + 0.35 * r} up to {@code r = 1}, then {@code 0.65 + 0.33 * (r - 1)} up to {@code r = 2}.</li>
   *   <li>Returns the {@code 0.98} cap for every ratio at or beyond {@code 2}.</li>
   *   <li>Returns the {@code 0.98} cap whenever the target has no toughness.</li>
   * </ul>
   *
   * <p>Layman's terms
   * <ul>
   *   <li>Precision and toughness are compared directly: each point of toughness demands one point of precision to reach {@code 0.65} consistency and two points to reach the cap.</li>
   *   <li>The {@code 0.30} floor means even a zero-precision attack keeps some reliability, though it leans heavily on the 50% damage floor in the variance roll.</li>
   *   <li>Once precision reaches twice the target's toughness, any extra precision is wasted.</li>
   * </ul>
   *
   * <p>Where the result is used
   * <ul>
   *   <li>{@code DamageMath.getDamageFromPrecisionNormalDistribution} uses {@code (1 - consistency)} as the standard deviation of the damage roll relative to full damage, so {@code 0.65} consistency means damage typically swings by about 35%.</li>
   *   <li>At {@code 0.30} consistency the standard deviation is {@code 0.70 * damage} and roughly 47% of rolls land on the 50% damage floor; expected damage is about {@code 0.64 * damage}.</li>
   *   <li>Ranged spread is intentionally separate and toughness-free: see {@link #precisionScoreToProjectileInaccuracy(double)}.</li>
   * </ul>
   *
   * <p>Important math breakpoints with the current constants
   * <ul>
   *   <li>{@code toughness = 0} -> consistency = {@code 0.98} for any precision.</li>
   *   <li>{@code precision = 0} (toughness > 0) -> consistency = {@code 0.30} (the floor).</li>
   *   <li>{@code precision = 0.5 * toughness} -> consistency = {@code 0.475}.</li>
   *   <li>{@code precision = toughness} -> consistency = {@code 0.65}.</li>
   *   <li>{@code precision = 1.5 * toughness} -> consistency = {@code 0.815}.</li>
   *   <li>{@code precision = 2 * toughness} -> consistency = {@code 0.98} (the cap, reached exactly at twice the toughness).</li>
   *   <li>{@code precision > 2 * toughness} -> consistency stays {@code 0.98}; excess precision is wasted.</li>
   * </ul>
   *
    * <p>Examples at the shared generated precision maximum ({@code 50} for strike, slash, and thrust)
    * <ul>
    *   <li>Against toughness {@code 12}: any attack type at precision {@code 50} -> {@code 0.98}.</li>
    *   <li>Against toughness {@code 20}: any attack type at precision {@code 50} -> {@code 0.98}.</li>
   * </ul>
   *
   * @param rawPrecision the generated precision score for this attack; values at or below {@code 0} yield the {@code 0.30} floor
   * @param targetToughness the target's armour toughness (vanilla {@code ARMOR_TOUGHNESS}); values at or below {@code 0} yield the {@code 0.98} cap
   * @return the consistency fraction, always between {@code 0.30} and {@code 0.98}
   */
  public static double precisionScoreToConsistency(double rawPrecision, double targetToughness) {
    if (targetToughness <= 0.0) {
      return PRECISION_CONSISTENCY_MAX;
    }
    double safePrecision = Math.max(0.0, rawPrecision);
    double ratio = safePrecision / targetToughness;
    double consistency;
    if (ratio <= 1.0) {
      double belowParitySlope = PRECISION_CONSISTENCY_PARITY - PRECISION_CONSISTENCY_MIN;
      consistency = PRECISION_CONSISTENCY_MIN + belowParitySlope * ratio;
    } else {
      double aboveParitySlope = (PRECISION_CONSISTENCY_MAX - PRECISION_CONSISTENCY_PARITY)
        / (PRECISION_CONSISTENCY_RATIO_CAP - 1.0);
      consistency = PRECISION_CONSISTENCY_PARITY + aboveParitySlope * (ratio - 1.0);
    }
    return Math.max(PRECISION_CONSISTENCY_MIN, Math.min(PRECISION_CONSISTENCY_MAX, consistency));
  }

  /**
   * Converts a raw precision score into projectile inaccuracy in degrees.
   *
   * <p>Ranged accuracy is intentionally independent of the melee consistency curve and of the
   * target's toughness: precision only controls how tightly a weapon's projectiles group.
   *
   * <p>What this method does
   * <ul>
   *   <li>Computes an accuracy share {@code precision / (precision + 5.0)} and caps it at {@code 0.98}.</li>
   *   <li>Returns {@code PROJECTILE_INACCURACY_MAX * (1 - accuracy)} degrees of spread.</li>
   * </ul>
   *
   * <p>Important math breakpoints with the current constants
   * <ul>
   *   <li>{@code precision = 0} -> 15.0 degrees (maximum spread).</li>
   *   <li>{@code precision = 5} (one scale) -> 7.5 degrees (half spread).</li>
   *   <li>{@code precision = 8.5} (vanilla bow) -> about 5.56 degrees.</li>
   *   <li>{@code precision = 11} (vanilla crossbow) -> about 4.69 degrees.</li>
   *   <li>As precision grows very large, inaccuracy approaches the {@code 0.3} degree floor.</li>
   * </ul>
   *
   * @param rawPrecision the generated precision score of the ranged weapon; values at or below {@code 0} yield maximum spread
   * @return the projectile inaccuracy in degrees, always between {@code 0.3} and {@code 15.0}
   */
  public static double precisionScoreToProjectileInaccuracy(double rawPrecision) {
    double safePrecision = Math.max(0.0, rawPrecision);
    double accuracy = safePrecision / (safePrecision + PROJECTILE_ACCURACY_PRECISION_SCALE);
    accuracy = Math.min(PROJECTILE_ACCURACY_MAX, accuracy);
    return PROJECTILE_INACCURACY_MAX * (1.0 - accuracy);
  }

  /**
   * Converts the ranged attack-type damage stat into bonus projectile velocity.
   * This intentionally affects launch physics rather than direct hit damage.
   */
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
    if (severity != CriticalFailSeverity.NONE) {
      SkadaNetwork.serverToPlayer(new S2CPlayCriticalFailSound(severity), player);
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

}
