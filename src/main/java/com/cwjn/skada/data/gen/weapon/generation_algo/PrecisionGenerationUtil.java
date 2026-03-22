package com.cwjn.skada.data.gen.weapon.generation_algo;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextFactory;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackDeliverySnapshot;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AssemblyPhysicsSnapshot;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.MaterialResponseSnapshot;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextSlash;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.ContactSnapshotSlash;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextStrike;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.ContactSnapshotStrike;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextThrust;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.ContactSnapshotThrust;
import com.cwjn.skada.util.Util;
import net.minecraft.util.Mth;

import static com.cwjn.skada.data.SkadaData.EDGE_ANGLE_DEFAULT;

public abstract class PrecisionGenerationUtil {

  private static final double HANDLING_SENSITIVITY = 2.2;
  private static final double HANDLING_FACTOR_MIN = 0.35;
  private static final double HANDLING_FACTOR_MAX = 1.08;
  private static final double CONTROL_BASE = 1.12;
  private static final double CONTROL_BALANCE_MISMATCH_WEIGHT = 0.60;
  private static final double CONTROL_OVERCOMMITMENT_WEIGHT = 0.90;
  private static final double CONTROL_INERTIA_WEIGHT = 0.45;
  private static final double CONTROL_EFFECTIVE_MASS_WEIGHT = 0.70;

  private static final double SLASH_EDGE_RADIUS_MIN_NM = 0.1;
  private static final double SLASH_EDGE_ANGLE_MIN_DEG = 1.0;
  private static final double SLASH_EDGE_ANGLE_FACTOR_MIN = 0.45;
  private static final double SLASH_EDGE_ANGLE_FACTOR_MAX = 1.40;
  private static final double SLASH_PRECISION_MIN = 1.0;
  private static final double SLASH_PRECISION_MAX = 34.0;
  private static final int SLASH_PRECISION_ROUND_DECIMALS = 3;

  private static final double SLASH_HARDNESS_DEFICIT_WEIGHT = 0.50;
  private static final double SLASH_TOUGHNESS_DEFICIT_WEIGHT = 0.65;
  private static final double SLASH_FLEXIBILITY_WEIGHT = 0.18;
  private static final double SLASH_BASE_MODIFIER_IDEAL = 1.10;
  private static final double SLASH_RISK_PENALTY_SCALE = 0.52;
  private static final double SLASH_MODIFIER_MIN = 0.45;
  private static final double SLASH_MODIFIER_MAX = 1.08;

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

  public static double slash(WeaponAssembly weapon) {
    return slash(AttackGenerationContextFactory.buildSlashContext(weapon));
  }

  public static double slash(AttackGenerationContextSlash context) {
    AssemblyPhysicsSnapshot assembly = context.assembly();
    AttackDeliverySnapshot delivery = context.delivery();
    MaterialResponseSnapshot material = context.material();
    ContactSnapshotSlash contact = context.contact();

    double edgeRadiusNm = Math.max(SLASH_EDGE_RADIUS_MIN_NM, contact.edgeRadiusNm());
    double basePrecision = edgeRadiusToPrecisionBase(edgeRadiusNm);

    double edgeAngleDeg = Math.max(SLASH_EDGE_ANGLE_MIN_DEG, contact.bevelAngleDegAtCop());
    double edgeAngleFactor = Mth.clamp(EDGE_ANGLE_DEFAULT / edgeAngleDeg, SLASH_EDGE_ANGLE_FACTOR_MIN, SLASH_EDGE_ANGLE_FACTOR_MAX);

    double materialModifier = slashMaterialModifier(material);
    double controlFactor = controlFactor(delivery, assembly);
    double finalPrecision = Util.round(Mth.clamp(basePrecision * edgeAngleFactor * materialModifier * controlFactor, SLASH_PRECISION_MIN, SLASH_PRECISION_MAX), SLASH_PRECISION_ROUND_DECIMALS);
    return finalPrecision;
  }

  private static double slashMaterialModifier(MaterialResponseSnapshot material) {
    double hardnessDeficit = 1.0 - material.hardnessNorm();
    double toughnessDeficit = 1.0 - material.toughnessNorm();
    double deformationRisk = Mth.clamp(
      SLASH_HARDNESS_DEFICIT_WEIGHT * hardnessDeficit * hardnessDeficit
        + SLASH_TOUGHNESS_DEFICIT_WEIGHT * toughnessDeficit * toughnessDeficit
        + SLASH_FLEXIBILITY_WEIGHT * material.flexibilityNorm() * material.flexibilityNorm(),
      0.0,
      1.0);
    double modifier = SLASH_BASE_MODIFIER_IDEAL - SLASH_RISK_PENALTY_SCALE * deformationRisk;
    return Mth.clamp(modifier, SLASH_MODIFIER_MIN, SLASH_MODIFIER_MAX);
  }

  public static double thrust(WeaponAssembly weapon) {
    return thrust(AttackGenerationContextFactory.buildThrustContext(weapon));
  }

  public static double thrust(AttackGenerationContextThrust context) {
    AssemblyPhysicsSnapshot assembly = context.assembly();
    AttackDeliverySnapshot delivery = context.delivery();
    MaterialResponseSnapshot material = context.material();
    ContactSnapshotThrust contact = context.contact();

    double pointTaper = Mth.clamp(contact.pointTaper(), THRUST_TAPER_MIN, THRUST_TAPER_MAX);
    double taperDrivenPrecision = pointTaperToPrecisionBase(pointTaper);

    double widthAtPointBase = Math.max(THRUST_POINT_BASE_WIDTH_MIN_CM, contact.wedgeThicknessCm());
    double thicknessAtPointBase = Math.max(THRUST_POINT_BASE_THICKNESS_MIN_CM, contact.thicknessAtPointBaseCm());
    double tipLengthCm = Math.max(THRUST_TIP_LENGTH_MIN_CM, contact.tipLengthCm());

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
    double pointBaseGeometryFactor = THRUST_GEOMETRY_WIDTH_WEIGHT * widthFactor + THRUST_GEOMETRY_THICKNESS_WEIGHT * thicknessFactor;

    double basePrecision = (THRUST_TAPER_WEIGHT * taperDrivenPrecision + THRUST_POINT_BASE_GEOMETRY_WEIGHT * (taperDrivenPrecision * pointBaseGeometryFactor)) * tipLengthFactor;

    double materialModifier = thrustMaterialModifier(material);
    double controlFactor = controlFactor(delivery, assembly);
    double motionFactor = thrustMotionPrecisionFactor(delivery, contact);
    double penetrationFactor = thrustPenetrationFactor(contact.penetrationEfficiency());
    double finalPrecision = Util.round(Mth.clamp(basePrecision * materialModifier * controlFactor * motionFactor * penetrationFactor, THRUST_PRECISION_MIN, THRUST_PRECISION_MAX), THRUST_PRECISION_ROUND_DECIMALS);
    return finalPrecision;
  }

  static double thrustPenetrationFactor(double thrustPenetrationEfficiency) {
    return Mth.clamp(thrustPenetrationEfficiency, THRUST_PENETRATION_FACTOR_MIN, THRUST_PENETRATION_FACTOR_MAX);
  }

  private static double thrustMaterialModifier(MaterialResponseSnapshot material) {
    double hardnessDeficit = 1.0 - material.hardnessNorm();
    double toughnessDeficit = 1.0 - material.toughnessNorm();
    double deformationRisk = Mth.clamp(
      THRUST_HARDNESS_DEFICIT_WEIGHT * hardnessDeficit * hardnessDeficit
        + THRUST_TOUGHNESS_DEFICIT_WEIGHT * toughnessDeficit * toughnessDeficit
        + THRUST_FLEXIBILITY_WEIGHT * material.flexibilityNorm() * material.flexibilityNorm(),
      0.0,
      1.0);
    double modifier = THRUST_BASE_MODIFIER_IDEAL - THRUST_RISK_PENALTY_SCALE * deformationRisk;
    return Mth.clamp(modifier, THRUST_MODIFIER_MIN, THRUST_MODIFIER_MAX);
  }

  public static double strike(WeaponAssembly weapon) {
    return strike(AttackGenerationContextFactory.buildStrikeContext(weapon));
  }

  public static double strike(AttackGenerationContextStrike context) {
    AssemblyPhysicsSnapshot assembly = context.assembly();
    AttackDeliverySnapshot delivery = context.delivery();
    MaterialResponseSnapshot material = context.material();
    ContactSnapshotStrike contact = context.contact();

    double effectiveContactAreaCm2 = Math.max(STRIKE_CONTACT_AREA_MIN_CM2, contact.effectiveContactAreaCm2());
    double geometryFocusFactor = Mth.clamp(contact.strikeFaceGeometryFocus(), STRIKE_GEOMETRY_FOCUS_MIN, STRIKE_GEOMETRY_FOCUS_MAX);
    double areaDrivenPrecision = strikeAreaDrivenPrecision(effectiveContactAreaCm2, geometryFocusFactor);

    double headRigidity = Mth.clamp(contact.strikeHeadRigidity(), STRIKE_HEAD_RIGIDITY_MIN, STRIKE_HEAD_RIGIDITY_MAX);
    double assemblyStability = Mth.clamp(contact.strikeAssemblyStability(), STRIKE_ASSEMBLY_STABILITY_MIN, STRIKE_ASSEMBLY_STABILITY_MAX);
    double secondaryFactor = STRIKE_RIGIDITY_SECONDARY_WEIGHT * headRigidity + STRIKE_STABILITY_SECONDARY_WEIGHT * assemblyStability;

    double materialModifier = strikeMaterialModifier(material);
    double controlFactor = controlFactor(delivery, assembly);
    double contactQualityFactor = strikeContactQualityFactor(contact.strikeStructuralEfficiency(), contact.strikeIncidenceEfficiency());
    double repeatabilityFactor = strikeRepeatabilityFactor(contact.strikeRepeatability());
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

  private static double strikeMaterialModifier(MaterialResponseSnapshot material) {
    double rigidityScore = Mth.clamp(
      STRIKE_MATERIAL_HARDNESS_WEIGHT * material.hardnessNorm()
        + STRIKE_MATERIAL_TOUGHNESS_WEIGHT * material.toughnessNorm()
        - STRIKE_MATERIAL_FLEXIBILITY_WEIGHT * material.flexibilityNorm(),
      -1.0,
      1.0);
    return Mth.clamp(1.0 + 0.20 * rigidityScore, 0.80, 1.20);
  }

  private static double edgeRadiusToPrecisionBase(double edgeRadius) {
    return Mth.clamp(20.0 * Math.exp(-(edgeRadius - 2.5) / 7.2), 2.0, 30.0);
  }

  private static double pointTaperToPrecisionBase(double pointTaper) {
    double acceleratedTaper = Math.pow(Mth.clamp(pointTaper, THRUST_TAPER_MIN, THRUST_TAPER_MAX), THRUST_TAPER_EXPONENT);
    return THRUST_POINT_BASE_PRECISION_MIN + (THRUST_POINT_BASE_PRECISION_MAX - THRUST_POINT_BASE_PRECISION_MIN) * acceleratedTaper;
  }

  private static double controlFactor(AttackDeliverySnapshot delivery, AssemblyPhysicsSnapshot assembly) {
    double balanceMismatch = delivery.balanceMismatch();
    double overcommitment = delivery.forwardOvercommitment();
    double effectiveMassRatio = delivery.effectiveMassRatio();
    double inertiaCoefficient = assembly.normalizedInertiaCoefficient();

    double control = CONTROL_BASE
      - CONTROL_BALANCE_MISMATCH_WEIGHT * balanceMismatch
      - CONTROL_OVERCOMMITMENT_WEIGHT * overcommitment
      - CONTROL_INERTIA_WEIGHT * inertiaCoefficient
      - CONTROL_EFFECTIVE_MASS_WEIGHT * effectiveMassRatio;

    double shapedControl = control * (1.0 - HANDLING_SENSITIVITY * balanceMismatch * balanceMismatch);
    return Mth.clamp(shapedControl, HANDLING_FACTOR_MIN, HANDLING_FACTOR_MAX);
  }

  private static double thrustMotionPrecisionFactor(AttackDeliverySnapshot delivery, ContactSnapshotThrust contact) {
    if (!delivery.rotationalThrust()) {
      return 1.0;
    }

    double balanceMismatch = delivery.balanceMismatch();
    double overcommitment = delivery.forwardOvercommitment();
    double effectiveMassRatio = delivery.effectiveMassRatio();
    double alignmentEfficiency = contact.alignmentEfficiency();

    double factor = THRUST_ROTATIONAL_MOTION_FACTOR_BASE
      - THRUST_ROTATIONAL_BALANCE_MISMATCH_WEIGHT * balanceMismatch
      - THRUST_ROTATIONAL_OVERCOMMITMENT_WEIGHT * overcommitment
      - THRUST_ROTATIONAL_EFFECTIVE_MASS_WEIGHT * effectiveMassRatio;
    return Mth.clamp(factor * alignmentEfficiency, THRUST_ROTATIONAL_MOTION_FACTOR_MIN, THRUST_ROTATIONAL_MOTION_FACTOR_MAX);
  }

}
