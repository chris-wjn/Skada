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

import static com.cwjn.skada.data.SkadaData.EDGE_ANGLE_DEFAULT;
import static com.cwjn.skada.data.SkadaData.MATERIAL_PROPERTY_SOFT_CAP;

public abstract class PrecisionGenerationUtil {

  private static final AttackType SLASH_CONTEXT = new AttackType("slash", null, null, null, SlashCapable.class);
  private static final AttackType THRUST_CONTEXT = new AttackType("thrust", null, null, null, ThrustCapable.class);
  private static final AttackType STRIKE_CONTEXT = new AttackType("strike", null, null, null, StrikeCapable.class);

  private static final double HANDLING_SENSITIVITY = 2.2;
  private static final double HANDLING_FACTOR_MIN = 0.35;
  private static final double HANDLING_FACTOR_MAX = 1.08;
  private static final double CONTROL_BASE = 1.12;
  private static final double CONTROL_BALANCE_MISMATCH_WEIGHT = 0.60;
  private static final double CONTROL_OVERCOMMITMENT_WEIGHT = 0.90;
  private static final double CONTROL_INERTIA_WEIGHT = 0.45;
  private static final double CONTROL_EFFECTIVE_MASS_WEIGHT = 0.70;

  private static final double SLASH_EDGE_RADIUS_MIN_NM = 0.1;
  private static final double SLASH_COP_MIN = 0.0;
  private static final double SLASH_COP_MAX = 1.0;
  private static final double SLASH_EDGE_ANGLE_MIN_DEG = 1.0;
  private static final double SLASH_EDGE_ANGLE_FACTOR_MIN = 0.45;
  private static final double SLASH_EDGE_ANGLE_FACTOR_MAX = 1.40;
  private static final double SLASH_PRECISION_MIN = 1.0;
  private static final double SLASH_PRECISION_MAX = 34.0;
  private static final int SLASH_PRECISION_ROUND_DECIMALS = 3;

  public static double slash(WeaponAssembly weapon) {
    WeaponPartEntry slashHead = weapon.primaryPartForAttackType(SLASH_CONTEXT).orElseThrow(() -> new IllegalStateException("Tried to generate slash precision for weapon without slash capability"));
    MaterialInfo material = slashHead.material();
    SlashCapable slashCapable = (SlashCapable) slashHead.part();

    double edgeRadiusNm = Math.max(SLASH_EDGE_RADIUS_MIN_NM, slashCapable.edgeRadiusNm());
    double basePrecision = edgeRadiusToPrecisionBase(edgeRadiusNm);

    double cop = Mth.clamp(weapon.centreOfPercussion(WeaponAssembly.LARGE_SAMPLE_SIZE), SLASH_COP_MIN, SLASH_COP_MAX);
    double edgeAngleDeg = Math.max(SLASH_EDGE_ANGLE_MIN_DEG, slashCapable.edgeAngleDegreesAt(cop));
    double edgeAngleFactor = Mth.clamp(EDGE_ANGLE_DEFAULT / edgeAngleDeg, SLASH_EDGE_ANGLE_FACTOR_MIN, SLASH_EDGE_ANGLE_FACTOR_MAX);

    double materialModifier = slashMaterialModifier(material);
    double controlFactor = controlFactor(weapon, SLASH_CONTEXT);
    double finalPrecision = Util.round(Mth.clamp(basePrecision * edgeAngleFactor * materialModifier * controlFactor, SLASH_PRECISION_MIN, SLASH_PRECISION_MAX), SLASH_PRECISION_ROUND_DECIMALS);
    return finalPrecision;
  }

  private static final double SLASH_HARDNESS_DEFICIT_WEIGHT = 0.50;
  private static final double SLASH_TOUGHNESS_DEFICIT_WEIGHT = 0.65;
  private static final double SLASH_FLEXIBILITY_WEIGHT = 0.18;
  private static final double SLASH_BASE_MODIFIER_IDEAL = 1.10;
  private static final double SLASH_RISK_PENALTY_SCALE = 0.52;
  private static final double SLASH_MODIFIER_MIN = 0.45;
  private static final double SLASH_MODIFIER_MAX = 1.08;

  /**
   * Applies a geometry-dominant material modifier for slash precision.
   *
     * Material contribution is continuous: better hardness/toughness and lower flexibility
     * naturally improve edge retention, while weak/brittle/over-flexible materials degrade it.
     * No discrete quality threshold is used.
   */
  private static double slashMaterialModifier(MaterialInfo material) {
    double normalizedHardness = normalizeMaterial(material.hardness());
    double normalizedToughness = normalizeMaterial(material.toughness());
    double normalizedFlexibility = normalizeMaterial(material.flexibility());

    double hardnessDeficit = 1.0 - normalizedHardness;
    double toughnessDeficit = 1.0 - normalizedToughness;

    double deformationRisk = Mth.clamp(
        SLASH_HARDNESS_DEFICIT_WEIGHT * hardnessDeficit * hardnessDeficit
            + SLASH_TOUGHNESS_DEFICIT_WEIGHT * toughnessDeficit * toughnessDeficit
            + SLASH_FLEXIBILITY_WEIGHT * normalizedFlexibility * normalizedFlexibility,
        0.0,
        1.0);

    double modifier = SLASH_BASE_MODIFIER_IDEAL - SLASH_RISK_PENALTY_SCALE * deformationRisk;
    return Mth.clamp(modifier, SLASH_MODIFIER_MIN, SLASH_MODIFIER_MAX);
  }

  private static final double THRUST_TAPER_MIN = 0.0;
  private static final double THRUST_TAPER_MAX = 1.0;
  private static final double THRUST_POINT_BASE_WIDTH_MIN_CM = 0.05;
  private static final double THRUST_POINT_BASE_THICKNESS_MIN_CM = 0.05;
  private static final double THRUST_TIP_LENGTH_MIN_CM = 0.10;
  private static final double THRUST_REFERENCE_POINT_BASE_WIDTH_CM = 2.2;
  private static final double THRUST_REFERENCE_POINT_BASE_THICKNESS_CM = 0.70;
  private static final double THRUST_REFERENCE_TIP_ASPECT_RATIO = 6.0;
  private static final double THRUST_TIP_ASPECT_RATIO_EXPONENT = 0.50;
  private static final double THRUST_TIP_LENGTH_FACTOR_MIN = 0.50;
  private static final double THRUST_TIP_LENGTH_FACTOR_MAX = 1.28;
  private static final double THRUST_WIDTH_FACTOR_MIN = 0.55;
  private static final double THRUST_WIDTH_FACTOR_MAX = 1.50;
  private static final double THRUST_THICKNESS_FACTOR_MIN = 0.50;
  private static final double THRUST_THICKNESS_FACTOR_MAX = 1.65;
  private static final double THRUST_POINT_BASE_PRECISION_MIN = 7.5;
  private static final double THRUST_POINT_BASE_PRECISION_MAX = 30.0;
  private static final double THRUST_TAPER_EXPONENT = 1.55;
  private static final double THRUST_TAPER_WEIGHT = 0.70;
  private static final double THRUST_POINT_BASE_GEOMETRY_WEIGHT = 0.30;
  private static final double THRUST_GEOMETRY_WIDTH_WEIGHT = 0.45;
  private static final double THRUST_GEOMETRY_THICKNESS_WEIGHT = 0.55;
  private static final double THRUST_BASE_MODIFIER_IDEAL = 1.09;
  private static final double THRUST_RISK_PENALTY_SCALE = 0.56;
  private static final double THRUST_MODIFIER_MIN = 0.30;
  private static final double THRUST_MODIFIER_MAX = 1.12;
  private static final double THRUST_PENETRATION_FACTOR_MIN = 0.40;
  private static final double THRUST_PENETRATION_FACTOR_MAX = 1.05;
  private static final double THRUST_PRECISION_MIN = 3.0;
  private static final double THRUST_PRECISION_MAX = 40.0;
  private static final int THRUST_PRECISION_ROUND_DECIMALS = 3;
  private static final double THRUST_ROTATIONAL_MOTION_FACTOR_BASE = 0.72;
  private static final double THRUST_ROTATIONAL_BALANCE_MISMATCH_WEIGHT = 0.55;
  private static final double THRUST_ROTATIONAL_OVERCOMMITMENT_WEIGHT = 0.40;
  private static final double THRUST_ROTATIONAL_EFFECTIVE_MASS_WEIGHT = 0.22;
  private static final double THRUST_ROTATIONAL_MOTION_FACTOR_MIN = 0.26;
  private static final double THRUST_ROTATIONAL_MOTION_FACTOR_MAX = 0.72;

  private static final double THRUST_HARDNESS_DEFICIT_WEIGHT = 0.42;
  private static final double THRUST_TOUGHNESS_DEFICIT_WEIGHT = 0.45;
  private static final double THRUST_FLEXIBILITY_WEIGHT = 0.95;

  public static double thrust(WeaponAssembly weapon) {
    WeaponPartEntry thrustHead = weapon.primaryPartForAttackType(THRUST_CONTEXT).orElseThrow(() -> new IllegalStateException("Tried to generate thrust precision for weapon without thrust capability"));
    MaterialInfo material = thrustHead.material();
    ThrustCapable thrustCapable = (ThrustCapable) thrustHead.part();

    double pointTaper = Mth.clamp(thrustCapable.pointTaper(), THRUST_TAPER_MIN, THRUST_TAPER_MAX);
    double taperDrivenPrecision = pointTaperToPrecisionBase(pointTaper);

    double widthAtPointBase = Math.max(THRUST_POINT_BASE_WIDTH_MIN_CM, thrustCapable.widthAtPointBase());
    double thicknessAtPointBase = Math.max(THRUST_POINT_BASE_THICKNESS_MIN_CM, thrustCapable.thicknessAtPointBase());
    double tipLengthCm = Math.max(THRUST_TIP_LENGTH_MIN_CM, thrustCapable.tipLengthCm());

    double tipAspectRatio = tipLengthCm / Math.max(widthAtPointBase, thicknessAtPointBase);
    double tipLengthFactor = Mth.clamp(
      Math.pow(tipAspectRatio / THRUST_REFERENCE_TIP_ASPECT_RATIO, THRUST_TIP_ASPECT_RATIO_EXPONENT),
      THRUST_TIP_LENGTH_FACTOR_MIN,
      THRUST_TIP_LENGTH_FACTOR_MAX);

    double widthFactor = Mth.clamp(
      THRUST_REFERENCE_POINT_BASE_WIDTH_CM / widthAtPointBase,
      THRUST_WIDTH_FACTOR_MIN,
      THRUST_WIDTH_FACTOR_MAX);
    double thicknessFactor = Mth.clamp(
      THRUST_REFERENCE_POINT_BASE_THICKNESS_CM / thicknessAtPointBase,
      THRUST_THICKNESS_FACTOR_MIN,
      THRUST_THICKNESS_FACTOR_MAX);
    double pointBaseGeometryFactor =
      THRUST_GEOMETRY_WIDTH_WEIGHT * widthFactor + THRUST_GEOMETRY_THICKNESS_WEIGHT * thicknessFactor;

    double basePrecision =
      (THRUST_TAPER_WEIGHT * taperDrivenPrecision + THRUST_POINT_BASE_GEOMETRY_WEIGHT * (taperDrivenPrecision * pointBaseGeometryFactor))
        * tipLengthFactor;

    double materialModifier = thrustMaterialModifier(material);
    double controlFactor = controlFactor(weapon, THRUST_CONTEXT);
    double motionFactor = thrustMotionPrecisionFactor(weapon, thrustCapable);
    double penetrationFactor = thrustPenetrationFactor(thrustCapable.thrustPenetrationEfficiency());
    double finalPrecision = Util.round(Mth.clamp(basePrecision * materialModifier * controlFactor * motionFactor * penetrationFactor, THRUST_PRECISION_MIN, THRUST_PRECISION_MAX), THRUST_PRECISION_ROUND_DECIMALS);
    return finalPrecision;
  }

  static double thrustPenetrationFactor(double thrustPenetrationEfficiency) {
    return Mth.clamp(thrustPenetrationEfficiency, THRUST_PENETRATION_FACTOR_MIN, THRUST_PENETRATION_FACTOR_MAX);
  }

  private static double thrustMaterialModifier(MaterialInfo material) {
    double normalizedHardness = normalizeMaterial(material.hardness());
    double normalizedToughness = normalizeMaterial(material.toughness());
    double normalizedFlexibility = normalizeMaterial(material.flexibility());

    double hardnessDeficit = 1.0 - normalizedHardness;
    double toughnessDeficit = 1.0 - normalizedToughness;

    double deformationRisk = Mth.clamp(
      THRUST_HARDNESS_DEFICIT_WEIGHT * hardnessDeficit * hardnessDeficit
        + THRUST_TOUGHNESS_DEFICIT_WEIGHT * toughnessDeficit * toughnessDeficit
        + THRUST_FLEXIBILITY_WEIGHT * normalizedFlexibility * normalizedFlexibility,
      0.0,
      1.0);

    double modifier = THRUST_BASE_MODIFIER_IDEAL - THRUST_RISK_PENALTY_SCALE * deformationRisk;
    return Mth.clamp(modifier, THRUST_MODIFIER_MIN, THRUST_MODIFIER_MAX);
  }

  private static final double STRIKE_CONTACT_AREA_MIN_CM2 = 0.02;
  private static final double STRIKE_CONTACT_AREA_REFERENCE_CM2 = 1.6;
  private static final double STRIKE_CONTACT_AREA_FACTOR_MIN = 0.55;
  private static final double STRIKE_CONTACT_AREA_FACTOR_MAX = 1.25;
  private static final double STRIKE_AREA_PRIMARY_WEIGHT = 0.82;
  private static final double STRIKE_SECONDARY_WEIGHT = 0.18;
  private static final double STRIKE_RIGIDITY_SECONDARY_WEIGHT = 0.56;
  private static final double STRIKE_STABILITY_SECONDARY_WEIGHT = 0.44;
  private static final double STRIKE_GEOMETRY_FOCUS_MIN = 0.75;
  private static final double STRIKE_GEOMETRY_FOCUS_MAX = 1.25;
  private static final double STRIKE_HEAD_RIGIDITY_MIN = 0.65;
  private static final double STRIKE_HEAD_RIGIDITY_MAX = 1.35;
  private static final double STRIKE_ASSEMBLY_STABILITY_MIN = 0.65;
  private static final double STRIKE_ASSEMBLY_STABILITY_MAX = 1.35;
  private static final double STRIKE_MATERIAL_HARDNESS_WEIGHT = 0.35;
  private static final double STRIKE_MATERIAL_TOUGHNESS_WEIGHT = 0.45;
  private static final double STRIKE_MATERIAL_FLEXIBILITY_WEIGHT = 0.40;
  private static final double STRIKE_CONTACT_QUALITY_STRUCTURE_WEIGHT = 0.55;
  private static final double STRIKE_CONTACT_QUALITY_INCIDENCE_WEIGHT = 0.45;
  private static final double STRIKE_CONTACT_QUALITY_MIN = 0.78;
  private static final double STRIKE_CONTACT_QUALITY_MAX = 1.05;
  private static final double STRIKE_REPEATABILITY_MIN = 0.70;
  private static final double STRIKE_REPEATABILITY_MAX = 1.02;
  private static final double STRIKE_PRECISION_MIN = 3.0;
  private static final double STRIKE_PRECISION_MAX = 24.0;
  private static final int STRIKE_PRECISION_ROUND_DECIMALS = 3;

  public static double strike(WeaponAssembly weapon) {
    WeaponPartEntry strikeHead = weapon.primaryPartForAttackType(STRIKE_CONTEXT)
      .orElseThrow(() -> new IllegalStateException("Tried to generate strike precision for weapon without strike capability"));
    MaterialInfo material = strikeHead.material();
    StrikeCapable strikeCapable = (StrikeCapable) strikeHead.part();

    double effectiveContactAreaCm2 = Math.max(STRIKE_CONTACT_AREA_MIN_CM2, strikeCapable.effectiveContactAreaCm2());
    double geometryFocusFactor = Mth.clamp(
      strikeCapable.strikeFaceGeometryFocus(),
      STRIKE_GEOMETRY_FOCUS_MIN,
      STRIKE_GEOMETRY_FOCUS_MAX);

    double areaDrivenPrecision = strikeAreaDrivenPrecision(effectiveContactAreaCm2, geometryFocusFactor);

    double headRigidity = Mth.clamp(strikeCapable.strikeHeadRigidity(), STRIKE_HEAD_RIGIDITY_MIN, STRIKE_HEAD_RIGIDITY_MAX);
    double assemblyStability = Mth.clamp(strikeCapable.strikeAssemblyStability(), STRIKE_ASSEMBLY_STABILITY_MIN, STRIKE_ASSEMBLY_STABILITY_MAX);

    double secondaryFactor = STRIKE_RIGIDITY_SECONDARY_WEIGHT * headRigidity
      + STRIKE_STABILITY_SECONDARY_WEIGHT * assemblyStability;

    double materialModifier = strikeMaterialModifier(material);
    double controlFactor = controlFactor(weapon, STRIKE_CONTEXT);
    double contactQualityFactor = strikeContactQualityFactor(
      strikeCapable.strikeStructuralEfficiency(),
      strikeCapable.strikeIncidenceEfficiency());
    double repeatabilityFactor = strikeRepeatabilityFactor(strikeCapable.strikeRepeatability());
    double finalPrecision = Util.round(Mth.clamp(
      areaDrivenPrecision * (STRIKE_AREA_PRIMARY_WEIGHT + STRIKE_SECONDARY_WEIGHT * secondaryFactor) * materialModifier * controlFactor * contactQualityFactor * repeatabilityFactor,
      STRIKE_PRECISION_MIN,
      STRIKE_PRECISION_MAX), STRIKE_PRECISION_ROUND_DECIMALS);
    return finalPrecision;
  }

  static double strikeAreaDrivenPrecision(double effectiveContactAreaCm2, double geometryFocusFactor) {
    double contactAreaFactor = Mth.clamp(
      STRIKE_CONTACT_AREA_REFERENCE_CM2 / effectiveContactAreaCm2,
      STRIKE_CONTACT_AREA_FACTOR_MIN,
      STRIKE_CONTACT_AREA_FACTOR_MAX);
    return strikeContactAreaToPrecisionBase(effectiveContactAreaCm2) * contactAreaFactor * geometryFocusFactor;
  }

  static double strikeContactAreaToPrecisionBase(double effectiveContactAreaCm2) {
    return Mth.clamp(16.0 * Math.exp(-effectiveContactAreaCm2 / 2.8) + 4.0, 4.0, 20.0);
  }

  static double strikeContactQualityFactor(double strikeStructuralEfficiency, double strikeIncidenceEfficiency) {
    double structural = Mth.clamp(strikeStructuralEfficiency, 0.72, 1.08);
    double incidence = Mth.clamp(strikeIncidenceEfficiency, 0.72, 1.05);
    return Mth.clamp(
      STRIKE_CONTACT_QUALITY_STRUCTURE_WEIGHT * structural + STRIKE_CONTACT_QUALITY_INCIDENCE_WEIGHT * incidence,
      STRIKE_CONTACT_QUALITY_MIN,
      STRIKE_CONTACT_QUALITY_MAX);
  }

  static double strikeRepeatabilityFactor(double strikeRepeatability) {
    return Mth.clamp(strikeRepeatability, STRIKE_REPEATABILITY_MIN, STRIKE_REPEATABILITY_MAX);
  }

  private static double strikeMaterialModifier(MaterialInfo material) {
    double normalizedHardness = normalizeMaterial(material.hardness());
    double normalizedToughness = normalizeMaterial(material.toughness());
    double normalizedFlexibility = normalizeMaterial(material.flexibility());

    double rigidityScore = Mth.clamp(
      STRIKE_MATERIAL_HARDNESS_WEIGHT * normalizedHardness
        + STRIKE_MATERIAL_TOUGHNESS_WEIGHT * normalizedToughness
        - STRIKE_MATERIAL_FLEXIBILITY_WEIGHT * normalizedFlexibility,
      -1.0,
      1.0);

    return Mth.clamp(1.0 + 0.20 * rigidityScore, 0.80, 1.20);
  }

  /**
   * Maps slash edge radius to base precision.
   * Sharper (smaller radius) edges produce higher precision.
   * @param edgeRadius The edge radius in nanometres.
   * @return A double representing precision.
   */
  private static double edgeRadiusToPrecisionBase(double edgeRadius) {
    return Mth.clamp(20.0 * Math.exp(-(edgeRadius - 2.5) / 7.2), 2.0, 30.0);
  }

  private static double pointTaperToPrecisionBase(double pointTaper) {
    double acceleratedTaper = Math.pow(Mth.clamp(pointTaper, THRUST_TAPER_MIN, THRUST_TAPER_MAX), THRUST_TAPER_EXPONENT);
    return THRUST_POINT_BASE_PRECISION_MIN
      + (THRUST_POINT_BASE_PRECISION_MAX - THRUST_POINT_BASE_PRECISION_MIN) * acceleratedTaper;
  }

  private static double normalizeMaterial(double value) {
    return Mth.clamp(value / MATERIAL_PROPERTY_SOFT_CAP, 0.0, 1.0);
  }

  private static double controlFactor(WeaponAssembly weapon, AttackType attackType) {
    double actualPoBNorm = weapon.normalizedPointOfBalance(WeaponAssembly.LARGE_SAMPLE_SIZE);
    double idealPoBNorm = weapon.normalizedIdealPointOfBalanceForAttackType(attackType);
    double deviation = actualPoBNorm - idealPoBNorm;
    double balanceMismatch = Math.abs(deviation);
    double overcommitment = Math.max(0.0, deviation);
    double effectiveMassRatio = weapon.effectiveMassRatioForAttackType(attackType, WeaponAssembly.LARGE_SAMPLE_SIZE);
    double inertiaRatio = normalizedInertiaRatio(weapon);

    double control = CONTROL_BASE
      - CONTROL_BALANCE_MISMATCH_WEIGHT * balanceMismatch
      - CONTROL_OVERCOMMITMENT_WEIGHT * overcommitment
      - CONTROL_INERTIA_WEIGHT * inertiaRatio
      - CONTROL_EFFECTIVE_MASS_WEIGHT * effectiveMassRatio;

    double shapedControl = control * (1.0 - HANDLING_SENSITIVITY * balanceMismatch * balanceMismatch);
    return Mth.clamp(shapedControl, HANDLING_FACTOR_MIN, HANDLING_FACTOR_MAX);
  }

  private static double normalizedInertiaRatio(WeaponAssembly weapon) {
    double mass = Math.max(1.0, weapon.mass(WeaponAssembly.LARGE_SAMPLE_SIZE));
    double length = Math.max(1.0, weapon.length());
    double denominator = Math.max(1.0e-6, mass * length * length);
    double inertia = Math.max(0.0, weapon.momentOfInertiaAboutBase(com.cwjn.skada.data.gen.weapon.util.WeaponAxis.Z, WeaponAssembly.LARGE_SAMPLE_SIZE));
    return Mth.clamp(inertia / denominator, 0.0, 1.0);
  }

  private static double thrustMotionPrecisionFactor(WeaponAssembly weapon, ThrustCapable thrustCapable) {
    if (!weapon.isThrustRotational(WeaponAssembly.LARGE_SAMPLE_SIZE)) {
      return 1.0;
    }

    double actualPoBNorm = weapon.normalizedPointOfBalance(WeaponAssembly.LARGE_SAMPLE_SIZE);
    double idealPoBNorm = weapon.normalizedIdealPointOfBalanceForAttackType(THRUST_CONTEXT);
    double delta = actualPoBNorm - idealPoBNorm;
    double balanceMismatch = Math.abs(delta);
    double overcommitment = Math.max(0.0, delta);
    double effectiveMassRatio = weapon.effectiveMassRatioForAttackType(THRUST_CONTEXT, WeaponAssembly.LARGE_SAMPLE_SIZE);
    double alignmentEfficiency = thrustCapable.thrustAlignmentEfficiency();

    double factor = THRUST_ROTATIONAL_MOTION_FACTOR_BASE
      - THRUST_ROTATIONAL_BALANCE_MISMATCH_WEIGHT * balanceMismatch
      - THRUST_ROTATIONAL_OVERCOMMITMENT_WEIGHT * overcommitment
      - THRUST_ROTATIONAL_EFFECTIVE_MASS_WEIGHT * effectiveMassRatio;
    return Mth.clamp(factor * alignmentEfficiency, THRUST_ROTATIONAL_MOTION_FACTOR_MIN, THRUST_ROTATIONAL_MOTION_FACTOR_MAX);
  }

}
