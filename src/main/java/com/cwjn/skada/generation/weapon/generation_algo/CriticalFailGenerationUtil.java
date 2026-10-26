package com.cwjn.skada.generation.weapon.generation_algo;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackDeliverySnapshot;
import com.cwjn.skada.generation.weapon.WeaponAssembly;
import com.cwjn.skada.generation.weapon.generation_algo.context.AssemblyPhysicsSnapshot;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextFactory;
import com.cwjn.skada.generation.weapon.generation_algo.context.MaterialResponseSnapshot;
import com.cwjn.skada.generation.weapon.generation_algo.context.ContactSnapshotSlash;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextSlash;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextStrike;
import com.cwjn.skada.generation.weapon.generation_algo.context.ContactSnapshotStrike;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextThrust;
import com.cwjn.skada.generation.weapon.generation_algo.context.ContactSnapshotThrust;
import com.cwjn.skada.util.Util;
import net.minecraft.util.Mth;

public abstract class CriticalFailGenerationUtil {

  private static final double COMMITMENT_BASE = 0.84;
  private static final double COMMITMENT_BALANCE_MISMATCH_WEIGHT = 0.45;
  private static final double COMMITMENT_OVERCOMMITMENT_WEIGHT = 0.70;
  private static final double COMMITMENT_INERTIA_WEIGHT = 0.55;
  private static final double COMMITMENT_MODIFIER_MIN = 0.72;
  private static final double COMMITMENT_MODIFIER_MAX = 1.95;
  private static final double SLASH_BASE_CHANCE = 0.0045;
  private static final double SLASH_FAIL_CHANCE_MIN = 0.0001;
  private static final double SLASH_FAIL_CHANCE_MAX = 0.05;
  private static final double THRUST_BASE_CHANCE = 0.0035;
  private static final double THRUST_FAIL_CHANCE_MIN = 0.0001;
  private static final double THRUST_FAIL_CHANCE_MAX = 0.05;
  private static final double THRUST_LINEAR_MOTION_RISK = 0.78;
  private static final double THRUST_ROTATIONAL_MOTION_RISK = 1.35;
  private static final double STRIKE_BASE_CHANCE = 0.0015;
  private static final double STRIKE_FAIL_CHANCE_MIN = 0.00005;
  private static final double STRIKE_FAIL_CHANCE_MAX = 0.03;

  private static final double RELIABILITY_BASE = 0.74;
  private static final double RELIABILITY_BALANCE_MISMATCH_WEIGHT = 0.80;
  private static final double RELIABILITY_OVERCOMMITMENT_WEIGHT = 1.05;
  private static final double RELIABILITY_INERTIA_WEIGHT = 0.70;
  private static final double RELIABILITY_EFFECTIVE_MASS_WEIGHT = 0.85;
  private static final double RELIABILITY_MODIFIER_MIN = 0.68;
  private static final double RELIABILITY_MODIFIER_MAX = 1.95;

  private static final double STRIKE_STRUCTURE_BASE = 1.48;
  private static final double STRIKE_STRUCTURE_RIGIDITY_WEIGHT = 0.24;
  private static final double STRIKE_STRUCTURE_STABILITY_WEIGHT = 0.24;
  private static final double STRIKE_STRUCTURE_FOCUS_WEIGHT = 0.02;
  private static final double STRIKE_STRUCTURE_BLUNTNESS_WEIGHT = 0.35;
  private static final double STRIKE_STRUCTURE_MIN = 0.72;
  private static final double STRIKE_STRUCTURE_MAX = 1.18;
  private static final double STRIKE_STRUCTURE_DEFICIT_WEIGHT = 0.55;
  private static final double STRIKE_LOW_EFFECTIVE_MASS_WEIGHT = 0.75;
  private static final double STRIKE_STRUCTURE_COMPLIANCE_WEIGHT = 0.45;
  private static final double STRIKE_STRUCTURE_GLANCHING_WEIGHT = 0.40;

  /**
   * Builds a slash context from the assembly and calculates its critical-fail chance.
   *
   * @param weapon assembly to evaluate
   * @return fractional chance, rounded to three decimals (range {@code [0, 0.050]})
   * @throws IllegalStateException if the assembly has no slash-capable part
   */
  public static double slash(WeaponAssembly weapon) {
    return slash(AttackGenerationContextFactory.buildSlashContext(weapon));
  }

  /**
   * Calculates a slash critical-fail chance from edge fragility, material, and handling.
   * The raw chance is {@code 0.0045 * bevelRisk * edgeFragilityRisk * materialRisk * commitment},
   * where bevel risk is {@code clamp(30 / bevelAngleDeg, 0.55, 1.9)}, edge fragility is
   * {@code clamp(1.34 - 0.13 * log1p(edgeRadiusNm), 0.75, 1.48)}, and material risk is
   * {@code clamp(1.20 - 0.50*toughness - 0.15*hardness + 0.44*abs(flexibility - 0.5), 0.45, 1.55)}.
   * Commitment is {@code clamp(0.84 + 0.45*mismatch + 0.70*overcommitment + 0.55*inertia, 0.72, 1.95)}.
   * The bevel angle is limited to 1 through 89 degrees before its risk is calculated.
   *
   * <p>The chance is clamped to {@code [0.0001, 0.05]} and then rounded to three decimal
   * places. Because of that rounding, the returned range is {@code [0, 0.050]}; this method
   * calculates a probability and does not roll it.
   *
   * @param context snapshot containing slash contact, material, delivery, and assembly data
   * @return fractional critical-fail chance (multiply by 100 for percent)
   */
  public static double slash(AttackGenerationContextSlash context) {
    AssemblyPhysicsSnapshot assembly = context.assembly();
    AttackDeliverySnapshot delivery = context.delivery();
    MaterialResponseSnapshot material = context.material();
    ContactSnapshotSlash contact = context.contact();

    double edgeAngle = Math.max(1.0, contact.bevelAngleDegAtCop());
    edgeAngle = Mth.clamp(edgeAngle, 1.0, 89.0);
    double edgeRadius = contact.edgeRadiusNm();

    double bevelRisk = Mth.clamp(30.0 / edgeAngle, 0.55, 1.9);
    double edgeFragilityRisk = Mth.clamp(1.34 - 0.13 * Math.log1p(edgeRadius), 0.75, 1.48);

    double materialRisk = 1.20 - 0.50 * material.toughnessNorm() - 0.15 * material.hardnessNorm() + 0.22 * Math.abs(material.flexibilityNorm() - 0.5) * 2.0;
    double commitmentModifier = slashOrThrustCommitmentModifier(delivery, assembly);
    double failChance = SLASH_BASE_CHANCE * bevelRisk * edgeFragilityRisk * Mth.clamp(materialRisk, 0.45, 1.55) * commitmentModifier;
    double finalFailChance = clampAndRoundFailChance(failChance, SLASH_FAIL_CHANCE_MIN, SLASH_FAIL_CHANCE_MAX);
    if (SkadaData.DEBUG_ENABLED) {
      Skada.LOGGER.debug("[GEN][FailChance][slash] edgeAngleDeg={}, edgeRadiusNm={}, base={}, bevelRisk={}, edgeFragilityRisk={}, materialRisk={}, commitmentModifier={}, final={}",
        edgeAngle, edgeRadius, SLASH_BASE_CHANCE, bevelRisk, edgeFragilityRisk, materialRisk, commitmentModifier, finalFailChance);
    }
    return finalFailChance;
  }

  /**
   * Builds a thrust context from the assembly and calculates its critical-fail chance.
   *
   * @param weapon assembly to evaluate
   * @return fractional chance, rounded to three decimals (range {@code [0, 0.050]})
   * @throws IllegalStateException if the assembly has no thrust-capable part
   */
  public static double thrust(WeaponAssembly weapon) {
    return thrust(AttackGenerationContextFactory.buildThrustContext(weapon));
  }

  /**
   * Calculates a thrust critical-fail chance from taper, tip fragility, material, handling,
   * motion mode, and alignment. The raw chance starts at {@code 0.0035} and multiplies
   * slenderness risk {@code clamp(0.82 + (10 + 90*pointTaper)/140, 0.82, 1.55)}, tip-radius
   * risk {@code clamp(1.22 - 0.07*log1p(tipRadiusNm), 0.82, 1.32)}, and material risk
   * {@code clamp(1.15 - 0.32*toughness - 0.16*hardness + 0.26*clamp(abs(flexibility - 0.4)/0.4, 0, 1.5), 0.45, 1.60)}.
   * It also multiplies commitment {@code clamp(0.84 + 0.45*mismatch + 0.70*overcommitment + 0.55*inertia, 0.72, 1.95)}
   * and reliability {@code clamp(0.74 + 0.80*mismatch + 1.05*directionalPenalty + 0.70*inertia + 0.85*(1-effectiveMassRatio), 0.68, 1.95)}.
   * Motion risk is 0.78 for linear and 1.35 for rotational thrust. Rotational thrust also
   * multiplies alignment risk {@code clamp(1.55 - 0.65*clamp(alignmentEfficiency, 0.48, 1.02), 0.95, 1.30)};
   * linear thrust uses alignment risk 1.0.
   *
   * <p>Directional reliability risk uses forward overcommitment for rotational thrust and
   * {@code 0.35*forwardOvercommitment + 0.15*rearUnderweight} for linear thrust. The chance
   * is clamped to {@code [0.0001, 0.05]} and rounded to three decimals, giving a returned
   * range of {@code [0, 0.050]}. This method calculates a probability; it does not roll it.
   *
   * @param context snapshot containing thrust contact, material, delivery, and assembly data
   * @return fractional critical-fail chance (multiply by 100 for percent)
   */
  public static double thrust(AttackGenerationContextThrust context) {
    AssemblyPhysicsSnapshot assembly = context.assembly();
    AttackDeliverySnapshot delivery = context.delivery();
    MaterialResponseSnapshot material = context.material();
    ContactSnapshotThrust contact = context.contact();

    double taperValue = 10.0 + 90.0 * contact.pointTaper();
    double tipRadius = Math.max(0.1, contact.tipRadiusNm());

    double slenderRisk = Mth.clamp(0.82 + taperValue / 140.0, 0.82, 1.55);
    double tipFragilityRisk = Mth.clamp(1.22 - 0.07 * Math.log1p(tipRadius), 0.82, 1.32);

    double materialRisk = 1.15 - 0.32 * material.toughnessNorm() - 0.16 * material.hardnessNorm() + 0.26 * Mth.clamp(Math.abs(material.flexibilityNorm() - 0.4) / 0.4, 0.0, 1.5);
    boolean rotationalThrust = delivery.rotationalThrust();
    double commitmentModifier = slashOrThrustCommitmentModifier(delivery, assembly);
    double reliabilityModifier = thrustReliabilityModifier(delivery, assembly, rotationalThrust);
    double motionRisk = rotationalThrust ? THRUST_ROTATIONAL_MOTION_RISK : THRUST_LINEAR_MOTION_RISK;
    double alignmentRisk = rotationalThrust ? thrustAlignmentRisk(contact.alignmentEfficiency()) : 1.0;
    double failChance = THRUST_BASE_CHANCE * slenderRisk * tipFragilityRisk * Mth.clamp(materialRisk, 0.45, 1.60) * commitmentModifier * reliabilityModifier * motionRisk * alignmentRisk;
    double finalFailChance = clampAndRoundFailChance(failChance, THRUST_FAIL_CHANCE_MIN, THRUST_FAIL_CHANCE_MAX);
    if (SkadaData.DEBUG_ENABLED) {
      Skada.LOGGER.debug("[GEN][FailChance][thrust] taperValue={}, tipRadiusMicrons={}, base={}, slenderRisk={}, tipFragilityRisk={}, materialRisk={}, rotationalThrust={}, commitmentModifier={}, reliabilityModifier={}, motionRisk={}, final={}",
        taperValue, tipRadius, THRUST_BASE_CHANCE, slenderRisk, tipFragilityRisk, materialRisk, rotationalThrust, commitmentModifier, reliabilityModifier, motionRisk, finalFailChance);
    }
    return finalFailChance;
  }

  /**
   * Builds a strike context from the assembly and calculates its critical-fail chance.
   *
   * @param weapon assembly to evaluate
   * @return fractional chance, rounded to three decimals (range {@code [0, 0.030]})
   * @throws IllegalStateException if the assembly has no strike-capable part
   */
  public static double strike(WeaponAssembly weapon) {
    return strike(AttackGenerationContextFactory.buildStrikeContext(weapon));
  }

  /**
   * Calculates a strike critical-fail chance from weapon mass, material, handling,
   * structure, and repeatability. Its raw value starts at {@code 0.0015} and multiplies
   * a mass risk {@code clamp(1 + 0.35*log1p(max(1, massG)/1800), 0.8, 1.5)}, toughness
   * protection {@code 1.20 - 0.70*toughness}, brittleness risk
   * {@code 1 + 0.40*max(0, hardness - 0.75)}, commitment, reliability, structure, and
   * repeatability risks. Let {@code h = max(0, -pointOfBalanceDelta)}. Commitment is
   * {@code clamp(0.84 + 0.45*mismatch + 0.70*h + 0.55*inertia, 0.72, 1.95)}; reliability is
   * {@code clamp(0.74 + 0.80*mismatch + 1.05*h + 0.70*inertia + 0.85*(1-effectiveMassRatio) + 0.75*max(0, 0.55-effectiveMassRatio), 0.68, 1.95)}.
   * With each contact input clamped to its snapshot range, the structure factor is
   * {@code clamp(1.48 - 0.24*rigidity - 0.24*stability + 0.02*max(0, focus-1) + 0.35*max(0, 1-focus) + 0.55*max(0, 1-rigidity) + 0.55*max(0, 1-stability) + 0.45*max(0, 1-structuralEfficiency) + 0.40*max(0, 1-incidenceEfficiency), 0.72, 1.18)}.
   * Repeatability risk is {@code clamp(1.50 - 0.50*clamp(repeatability, 0.70, 1.02), 1.0, 1.18)}.
   * Repeatability risk is {@code clamp(1.50 - 0.50*clamp(repeatability, 0.70, 1.02), 1.0, 1.18)}.
   *
   * <p>The chance is clamped to {@code [0.00005, 0.03]} and rounded to three decimal
   * places. The returned range is therefore {@code [0, 0.030]}. This method calculates
   * a probability and does not roll it.
   *
   * @param context snapshot containing strike contact, material, delivery, and assembly data
   * @return fractional critical-fail chance (multiply by 100 for percent)
   */
  public static double strike(AttackGenerationContextStrike context) {
    AssemblyPhysicsSnapshot assembly = context.assembly();
    AttackDeliverySnapshot delivery = context.delivery();
    MaterialResponseSnapshot material = context.material();
    ContactSnapshotStrike contact = context.contact();

    double weight = Math.max(1.0, assembly.massG());
    double weightRisk = Mth.clamp(1.0 + 0.35 * Math.log1p(weight / 1800.0), 0.8, 1.5);

    double toughnessProtection = 1.20 - 0.70 * material.toughnessNorm();
    double brittlenessRisk = 1.0 + 0.40 * Math.max(0.0, material.hardnessNorm() - 0.75);

    double commitmentModifier = strikeCommitmentModifier(delivery, assembly);
    double reliabilityModifier = strikeReliabilityModifier(delivery, assembly);
    double structureModifier = strikeStructureModifier(contact);
    double repeatabilityRisk = strikeRepeatabilityRisk(contact.strikeRepeatability());
    double failChance = STRIKE_BASE_CHANCE * weightRisk * toughnessProtection * brittlenessRisk * commitmentModifier * reliabilityModifier * structureModifier * repeatabilityRisk;
    double finalFailChance = clampAndRoundFailChance(failChance, STRIKE_FAIL_CHANCE_MIN, STRIKE_FAIL_CHANCE_MAX);
    return finalFailChance;
  }

  static double clampAndRoundFailChance(double failChance, double minChance, double maxChance) {
    return Util.round(Mth.clamp(failChance, minChance, maxChance), 3);
  }

  static double thrustAlignmentRisk(double thrustAlignmentEfficiency) {
    return Mth.clamp(1.55 - 0.65 * Mth.clamp(thrustAlignmentEfficiency, 0.48, 1.02), 0.95, 1.30);
  }

  static double strikeRepeatabilityRisk(double strikeRepeatability) {
    return Mth.clamp(1.50 - 0.50 * Mth.clamp(strikeRepeatability, 0.70, 1.02), 1.0, 1.18);
  }

  private static double slashOrThrustCommitmentModifier(AttackDeliverySnapshot delivery, AssemblyPhysicsSnapshot assembly) {
    double modifier = COMMITMENT_BASE
      + COMMITMENT_BALANCE_MISMATCH_WEIGHT * delivery.balanceMismatch()
      + COMMITMENT_OVERCOMMITMENT_WEIGHT * delivery.forwardOvercommitment()
      + COMMITMENT_INERTIA_WEIGHT * assembly.normalizedInertiaCoefficient();
    return Mth.clamp(modifier, COMMITMENT_MODIFIER_MIN, COMMITMENT_MODIFIER_MAX);
  }

  private static double strikeCommitmentModifier(AttackDeliverySnapshot delivery, AssemblyPhysicsSnapshot assembly) {
    double underweightedHead = Math.max(0.0, -delivery.pointOfBalanceDelta());
    double modifier = COMMITMENT_BASE
      + COMMITMENT_BALANCE_MISMATCH_WEIGHT * delivery.balanceMismatch()
      + COMMITMENT_OVERCOMMITMENT_WEIGHT * underweightedHead
      + COMMITMENT_INERTIA_WEIGHT * assembly.normalizedInertiaCoefficient();
    return Mth.clamp(modifier, COMMITMENT_MODIFIER_MIN, COMMITMENT_MODIFIER_MAX);
  }

  private static double thrustReliabilityModifier(AttackDeliverySnapshot delivery, AssemblyPhysicsSnapshot assembly, boolean rotationalThrust) {
    double directionalPenalty = rotationalThrust ? delivery.forwardOvercommitment() : 0.35 * delivery.forwardOvercommitment() + 0.15 * delivery.rearUnderweight();
    double modifier = RELIABILITY_BASE
      + RELIABILITY_BALANCE_MISMATCH_WEIGHT * delivery.balanceMismatch()
      + RELIABILITY_OVERCOMMITMENT_WEIGHT * directionalPenalty
      + RELIABILITY_INERTIA_WEIGHT * assembly.normalizedInertiaCoefficient()
      + RELIABILITY_EFFECTIVE_MASS_WEIGHT * (1.0 - delivery.effectiveMassRatio());
    return Mth.clamp(modifier, RELIABILITY_MODIFIER_MIN, RELIABILITY_MODIFIER_MAX);
  }

  private static double strikeReliabilityModifier(AttackDeliverySnapshot delivery, AssemblyPhysicsSnapshot assembly) {
    double underweightedHead = Math.max(0.0, -delivery.pointOfBalanceDelta());
    double modifier = RELIABILITY_BASE
      + RELIABILITY_BALANCE_MISMATCH_WEIGHT * delivery.balanceMismatch()
      + RELIABILITY_OVERCOMMITMENT_WEIGHT * underweightedHead
      + RELIABILITY_INERTIA_WEIGHT * assembly.normalizedInertiaCoefficient()
      + RELIABILITY_EFFECTIVE_MASS_WEIGHT * (1.0 - delivery.effectiveMassRatio())
      + STRIKE_LOW_EFFECTIVE_MASS_WEIGHT * Math.max(0.0, 0.55 - delivery.effectiveMassRatio());
    return Mth.clamp(modifier, RELIABILITY_MODIFIER_MIN, RELIABILITY_MODIFIER_MAX);
  }

  private static double strikeStructureModifier(ContactSnapshotStrike strikeCapable) {
    double focus = Mth.clamp(strikeCapable.strikeFaceGeometryFocus(), 0.75, 1.35);
    double rigidity = Mth.clamp(strikeCapable.strikeHeadRigidity(), 0.65, 1.35);
    double stability = Mth.clamp(strikeCapable.strikeAssemblyStability(), 0.65, 1.35);
    double structuralEfficiency = Mth.clamp(strikeCapable.strikeStructuralEfficiency(), 0.72, 1.08);
    double incidenceEfficiency = Mth.clamp(strikeCapable.strikeIncidenceEfficiency(), 0.72, 1.05);

    double modifier = STRIKE_STRUCTURE_BASE
      - STRIKE_STRUCTURE_RIGIDITY_WEIGHT * rigidity
      - STRIKE_STRUCTURE_STABILITY_WEIGHT * stability
      + STRIKE_STRUCTURE_FOCUS_WEIGHT * Math.max(0.0, focus - 1.0)
      + STRIKE_STRUCTURE_BLUNTNESS_WEIGHT * Math.max(0.0, 1.0 - focus)
      + STRIKE_STRUCTURE_DEFICIT_WEIGHT * Math.max(0.0, 1.0 - rigidity)
      + STRIKE_STRUCTURE_DEFICIT_WEIGHT * Math.max(0.0, 1.0 - stability)
      + STRIKE_STRUCTURE_COMPLIANCE_WEIGHT * Math.max(0.0, 1.0 - structuralEfficiency)
      + STRIKE_STRUCTURE_GLANCHING_WEIGHT * Math.max(0.0, 1.0 - incidenceEfficiency);
    return Mth.clamp(modifier, STRIKE_STRUCTURE_MIN, STRIKE_STRUCTURE_MAX);
  }

}
