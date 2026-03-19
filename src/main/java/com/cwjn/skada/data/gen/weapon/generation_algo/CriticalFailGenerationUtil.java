package com.cwjn.skada.data.gen.weapon.generation_algo;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.Util;

import net.minecraft.util.Mth;

import static com.cwjn.skada.data.SkadaData.MATERIAL_PROPERTY_SOFT_CAP;

public abstract class CriticalFailGenerationUtil {

  private static final AttackType SLASH_CONTEXT = new AttackType("slash", null, null, null, SlashCapable.class);
  private static final AttackType THRUST_CONTEXT = new AttackType("thrust", null, null, null, ThrustCapable.class);
  private static final AttackType STRIKE_CONTEXT = new AttackType("strike", null, null, null, StrikeCapable.class);

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

  public static double slash(WeaponAssembly weapon) {
    WeaponPartEntry head = weapon.primaryPartForAttackType(SLASH_CONTEXT)
            .orElseThrow(() -> new IllegalStateException("Tried to generate slash critical fail for weapon without slash capability"));
    MaterialInfo material = head.material();
        SlashCapable slashCapable = (SlashCapable) head.part();

        double edgeAngle = slashCapable.edgeAngleDegreesAt(0.5);
    edgeAngle = Mth.clamp(edgeAngle, 1.0, 89.0);
        double edgeRadius = slashCapable.edgeRadiusNm();

    double bevelRisk = Mth.clamp(30.0 / edgeAngle, 0.55, 1.9);
    double edgeFragilityRisk = Mth.clamp(1.34 - 0.13 * Math.log1p(edgeRadius), 0.75, 1.48);

    double toughnessN = normalizeMaterial(material.toughness());
    double hardnessN = normalizeMaterial(material.hardness());
    double flexCentered = Math.abs(normalizeMaterial(material.flexibility()) - 0.5) * 2.0;

    double materialRisk = 1.20 - 0.50 * toughnessN - 0.15 * hardnessN + 0.22 * flexCentered;
    double commitmentModifier = slashOrThrustCommitmentModifier(weapon, SLASH_CONTEXT);
    double failChance = SLASH_BASE_CHANCE * bevelRisk * edgeFragilityRisk * Mth.clamp(materialRisk, 0.45, 1.55) * commitmentModifier;
    double finalFailChance = clampAndRoundFailChance(failChance, SLASH_FAIL_CHANCE_MIN, SLASH_FAIL_CHANCE_MAX);
    if (SkadaData.DEBUG_ENABLED) {
      Skada.LOGGER.debug("[GEN][FailChance][slash] edgeAngleDeg={}, edgeRadiusNm={}, base={}, bevelRisk={}, edgeFragilityRisk={}, toughnessN={}, hardnessN={}, flexCentered={}, materialRisk={}, commitmentModifier={}, final={}",
        edgeAngle, edgeRadius, SLASH_BASE_CHANCE, bevelRisk, edgeFragilityRisk, toughnessN, hardnessN, flexCentered, materialRisk, commitmentModifier, finalFailChance);
    }
    return finalFailChance;
  }

  public static double thrust(WeaponAssembly weapon) {
    WeaponPartEntry head = weapon.primaryPartForAttackType(THRUST_CONTEXT)
            .orElseThrow(() -> new IllegalStateException("Tried to generate thrust critical fail for weapon without thrust capability"));
    MaterialInfo material = head.material();
        ThrustCapable thrustCapable = (ThrustCapable) head.part();

        double taperValue = 10.0 + 90.0 * thrustCapable.pointTaper();
        double tipRadius = thrustCapable.tipRadiusNm();
    tipRadius = Math.max(0.1, tipRadius);

    double slenderRisk = Mth.clamp(0.82 + taperValue / 140.0, 0.82, 1.55);
    double tipFragilityRisk = Mth.clamp(1.22 - 0.07 * Math.log1p(tipRadius), 0.82, 1.32);

    double toughnessN = normalizeMaterial(material.toughness());
    double hardnessN = normalizeMaterial(material.hardness());
    double flexCentered = Math.abs(normalizeMaterial(material.flexibility()) - 0.4) / 0.4;

    double materialRisk = 1.15 - 0.32 * toughnessN - 0.16 * hardnessN + 0.26 * Mth.clamp(flexCentered, 0.0, 1.5);
    boolean rotationalThrust = weapon.isThrustRotational(WeaponAssembly.LARGE_SAMPLE_SIZE);
    double commitmentModifier = slashOrThrustCommitmentModifier(weapon, THRUST_CONTEXT);
    double reliabilityModifier = thrustReliabilityModifier(weapon, rotationalThrust);
    double motionRisk = rotationalThrust ? THRUST_ROTATIONAL_MOTION_RISK : THRUST_LINEAR_MOTION_RISK;
    double alignmentRisk = rotationalThrust ? thrustAlignmentRisk(thrustCapable.thrustAlignmentEfficiency()) : 1.0;
    double failChance = THRUST_BASE_CHANCE * slenderRisk * tipFragilityRisk * Mth.clamp(materialRisk, 0.45, 1.60) * commitmentModifier * reliabilityModifier * motionRisk * alignmentRisk;
    double finalFailChance = clampAndRoundFailChance(failChance, THRUST_FAIL_CHANCE_MIN, THRUST_FAIL_CHANCE_MAX);
    if (SkadaData.DEBUG_ENABLED) {
      Skada.LOGGER.debug("[GEN][FailChance][thrust] taperValue={}, tipRadiusMicrons={}, base={}, slenderRisk={}, tipFragilityRisk={}, toughnessN={}, hardnessN={}, flexCentered={}, materialRisk={}, rotationalThrust={}, commitmentModifier={}, reliabilityModifier={}, motionRisk={}, final={}",
        taperValue, tipRadius, THRUST_BASE_CHANCE, slenderRisk, tipFragilityRisk, toughnessN, hardnessN, flexCentered, materialRisk, rotationalThrust, commitmentModifier, reliabilityModifier, motionRisk, finalFailChance);
    }
    return finalFailChance;
  }

  /**
   * Strike tools are structurally robust, so baseline failure is lower and mostly driven by
   * brittle materials and very heavy self-loading.
   */
  public static double strike(WeaponAssembly weapon) {
    WeaponPartEntry head = weapon.primaryPartForAttackType(STRIKE_CONTEXT)
            .orElseThrow(() -> new IllegalStateException("Tried to generate strike critical fail for weapon without strike capability"));
    MaterialInfo material = head.material();
    StrikeCapable strikeCapable = (StrikeCapable) head.part();

    double weight = Math.max(1.0, weapon.mass(WeaponAssembly.LARGE_SAMPLE_SIZE));
    double weightRisk = Mth.clamp(1.0 + 0.35 * Math.log1p(weight / 1800.0), 0.8, 1.5);

    double toughnessN = normalizeMaterial(material.toughness());
    double hardnessN = normalizeMaterial(material.hardness());
    double toughnessProtection = 1.20 - 0.70 * toughnessN;
    double brittlenessRisk = 1.0 + 0.40 * Math.max(0.0, hardnessN - 0.75);

    double commitmentModifier = strikeCommitmentModifier(weapon);
    double reliabilityModifier = strikeReliabilityModifier(weapon);
    double structureModifier = strikeStructureModifier(strikeCapable);
    double repeatabilityRisk = strikeRepeatabilityRisk(strikeCapable.strikeRepeatability());
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

  private static double normalizeMaterial(double value) {
    return Mth.clamp(value / MATERIAL_PROPERTY_SOFT_CAP, 0.0, 1.0);
  }

  private static double slashOrThrustCommitmentModifier(WeaponAssembly weapon, AttackType attackType) {
    double actualPoBNorm = weapon.normalizedPointOfBalance(WeaponAssembly.LARGE_SAMPLE_SIZE);
    double idealPoBNorm = weapon.normalizedIdealPointOfBalanceForAttackType(attackType);
    double delta = actualPoBNorm - idealPoBNorm;
    double balanceMismatch = Math.abs(delta);
    double overcommitment = Math.max(0.0, delta);
    double modifier = COMMITMENT_BASE
      + COMMITMENT_BALANCE_MISMATCH_WEIGHT * balanceMismatch
      + COMMITMENT_OVERCOMMITMENT_WEIGHT * overcommitment
      + COMMITMENT_INERTIA_WEIGHT * normalizedInertiaRatio(weapon);
    return Mth.clamp(modifier, COMMITMENT_MODIFIER_MIN, COMMITMENT_MODIFIER_MAX);
  }

  private static double strikeCommitmentModifier(WeaponAssembly weapon) {
    double actualPoBNorm = weapon.normalizedPointOfBalance(WeaponAssembly.LARGE_SAMPLE_SIZE);
    double idealPoBNorm = weapon.normalizedIdealPointOfBalanceForAttackType(STRIKE_CONTEXT);
    double delta = idealPoBNorm - actualPoBNorm;
    double balanceMismatch = Math.abs(delta);
    double underweightedHead = Math.max(0.0, delta);
    double modifier = COMMITMENT_BASE
      + COMMITMENT_BALANCE_MISMATCH_WEIGHT * balanceMismatch
      + COMMITMENT_OVERCOMMITMENT_WEIGHT * underweightedHead
      + COMMITMENT_INERTIA_WEIGHT * normalizedInertiaRatio(weapon);
    return Mth.clamp(modifier, COMMITMENT_MODIFIER_MIN, COMMITMENT_MODIFIER_MAX);
  }

  private static double thrustReliabilityModifier(WeaponAssembly weapon, boolean rotationalThrust) {
    double actualPoBNorm = weapon.normalizedPointOfBalance(WeaponAssembly.LARGE_SAMPLE_SIZE);
    double idealPoBNorm = weapon.normalizedIdealPointOfBalanceForAttackType(THRUST_CONTEXT);
    double delta = actualPoBNorm - idealPoBNorm;
    double balanceMismatch = Math.abs(delta);
    double directionalPenalty = rotationalThrust ? Math.max(0.0, delta) : 0.35 * Math.max(0.0, delta) + 0.15 * Math.max(0.0, -delta);
    double inertiaRatio = normalizedInertiaRatio(weapon);
    double effectiveMassRatio = weapon.effectiveMassRatioForAttackType(THRUST_CONTEXT, WeaponAssembly.LARGE_SAMPLE_SIZE);

    double modifier = RELIABILITY_BASE
      + RELIABILITY_BALANCE_MISMATCH_WEIGHT * balanceMismatch
      + RELIABILITY_OVERCOMMITMENT_WEIGHT * directionalPenalty
      + RELIABILITY_INERTIA_WEIGHT * inertiaRatio
      + RELIABILITY_EFFECTIVE_MASS_WEIGHT * (1.0 - effectiveMassRatio);
    return Mth.clamp(modifier, RELIABILITY_MODIFIER_MIN, RELIABILITY_MODIFIER_MAX);
  }

  private static double strikeReliabilityModifier(WeaponAssembly weapon) {
    double actualPoBNorm = weapon.normalizedPointOfBalance(WeaponAssembly.LARGE_SAMPLE_SIZE);
    double idealPoBNorm = weapon.normalizedIdealPointOfBalanceForAttackType(STRIKE_CONTEXT);
    double delta = idealPoBNorm - actualPoBNorm;
    double balanceMismatch = Math.abs(delta);
    double underweightedHead = Math.max(0.0, delta);
    double inertiaRatio = normalizedInertiaRatio(weapon);
    double effectiveMassRatio = weapon.effectiveMassRatioForAttackType(STRIKE_CONTEXT, WeaponAssembly.LARGE_SAMPLE_SIZE);

    double modifier = RELIABILITY_BASE
      + RELIABILITY_BALANCE_MISMATCH_WEIGHT * balanceMismatch
      + RELIABILITY_OVERCOMMITMENT_WEIGHT * underweightedHead
      + RELIABILITY_INERTIA_WEIGHT * inertiaRatio
      + RELIABILITY_EFFECTIVE_MASS_WEIGHT * (1.0 - effectiveMassRatio)
      + STRIKE_LOW_EFFECTIVE_MASS_WEIGHT * Math.max(0.0, 0.55 - effectiveMassRatio);
    return Mth.clamp(modifier, RELIABILITY_MODIFIER_MIN, RELIABILITY_MODIFIER_MAX);
  }

  private static double strikeStructureModifier(StrikeCapable strikeCapable) {
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

  private static double normalizedInertiaRatio(WeaponAssembly weapon) {
    double mass = Math.max(1.0, weapon.mass(WeaponAssembly.LARGE_SAMPLE_SIZE));
    double length = Math.max(1.0, weapon.length());
    double denominator = Math.max(1.0e-6, mass * length * length);
    double inertia = Math.max(0.0, weapon.momentOfInertiaAboutBase(com.cwjn.skada.data.gen.weapon.util.WeaponAxis.Z, WeaponAssembly.LARGE_SAMPLE_SIZE));
    return Mth.clamp(inertia / denominator, 0.0, 1.0);
  }

}
