package com.cwjn.skada.generation.weapon.generation_algo;

import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextFactory;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackDeliverySnapshot;
import com.cwjn.skada.generation.weapon.WeaponAssembly;
import com.cwjn.skada.generation.weapon.generation_algo.context.AssemblyPhysicsSnapshot;
import com.cwjn.skada.generation.weapon.generation_algo.context.MaterialResponseSnapshot;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextSlash;
import com.cwjn.skada.generation.weapon.generation_algo.context.ContactSnapshotSlash;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextStrike;
import com.cwjn.skada.generation.weapon.generation_algo.context.ContactSnapshotStrike;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextThrust;
import com.cwjn.skada.generation.weapon.generation_algo.context.ContactSnapshotThrust;
import com.cwjn.skada.util.Util;
import net.minecraft.util.Mth;

public abstract class PrecisionGenerationUtil {

  private static final double PRECISION_MIN = 1.0;
  private static final double PRECISION_MAX = 50.0;

  private static final double HANDLING_SENSITIVITY = 2.2;
  private static final double HANDLING_FACTOR_MIN = 0.35;
  private static final double HANDLING_FACTOR_MAX = 1.08;
  private static final double CONTROL_BASE = 1.35;
  private static final double CONTROL_BALANCE_MISMATCH_WEIGHT = 0.60;
  private static final double CONTROL_OVERCOMMITMENT_WEIGHT = 0.90;
  private static final double CONTROL_INERTIA_WEIGHT = 0.45;
  private static final double CONTROL_EFFECTIVE_MASS_WEIGHT = 0.20;

  private static final double SLASH_EDGE_ANGLE_MIN_DEG = 1.0;
  private static final double SLASH_EDGE_RADIUS_ATOMIC_LIMIT_NM = 2.5;
  private static final double SLASH_EDGE_RADIUS_SCALE_NM = 4.0;
  private static final double SLASH_EDGE_RADIUS_BASE_MAX = 46.0;
  private static final double SLASH_EDGE_ANGLE_REFERENCE_DEG = 14.0;
  private static final double SLASH_EDGE_ANGLE_FACTOR_MIN = 0.85;
  private static final double SLASH_EDGE_ANGLE_FACTOR_MAX = 1.15;

  private static final double SLASH_HARDNESS_DEFICIT_WEIGHT = 0.50;
  private static final double SLASH_TOUGHNESS_DEFICIT_WEIGHT = 0.65;
  private static final double SLASH_FLEXIBILITY_WEIGHT = 0.18;
  private static final double SLASH_BASE_MODIFIER_IDEAL = 1.0;
  private static final double SLASH_RISK_PENALTY_SCALE = 0.35;
  private static final double SLASH_MODIFIER_MIN = 0.65;
  private static final double SLASH_MODIFIER_MAX = 1.0;

  private static final double THRUST_TAPER_MIN = 0.0;
  private static final double THRUST_TAPER_MAX = 1.0;
  private static final double THRUST_POINT_BASE_WIDTH_MIN_CM = 0.05;
  private static final double THRUST_POINT_BASE_THICKNESS_MIN_CM = 0.05;
  private static final double THRUST_TIP_LENGTH_MIN_CM = 0.10;
  private static final double THRUST_REFERENCE_POINT_BASE_WIDTH_CM = 2.5;
  private static final double THRUST_REFERENCE_POINT_BASE_THICKNESS_CM = 0.35;
  private static final double THRUST_REFERENCE_TIP_ASPECT_RATIO = 6.0;
  private static final double THRUST_TIP_ASPECT_RATIO_EXPONENT = 0.08;
  private static final double THRUST_TIP_LENGTH_FACTOR_MIN = 0.65;
  private static final double THRUST_TIP_LENGTH_FACTOR_MAX = 1.55;
  private static final double THRUST_WIDTH_FACTOR_MIN = 0.55;
  private static final double THRUST_WIDTH_FACTOR_MAX = 1.75;
  private static final double THRUST_THICKNESS_FACTOR_MIN = 0.55;
  private static final double THRUST_THICKNESS_FACTOR_MAX = 1.75;
  private static final double THRUST_POINT_BASE_PRECISION_MIN = 7.5;
  private static final double THRUST_POINT_BASE_PRECISION_MAX = 50.0;
  private static final double THRUST_TAPER_EXPONENT = 1.55;
  private static final double THRUST_TAPER_WEIGHT = 0.70;
  private static final double THRUST_POINT_BASE_GEOMETRY_WEIGHT = 0.30;
  private static final double THRUST_GEOMETRY_WIDTH_WEIGHT = 0.45;
  private static final double THRUST_GEOMETRY_THICKNESS_WEIGHT = 0.55;
  private static final double THRUST_BASE_MODIFIER_IDEAL = 1.0;
  private static final double THRUST_RISK_PENALTY_SCALE = 0.20;
  private static final double THRUST_MODIFIER_MIN = 0.60;
  private static final double THRUST_MODIFIER_MAX = 1.0;
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
  private static final double STRIKE_CONTACT_AREA_REFERENCE_CM2 = 1.4;
  private static final double STRIKE_AREA_PENALTY_SCALE = 0.14;
  private static final double STRIKE_AREA_FACE_FOCUS_REFERENCE = 1.25;
  private static final double STRIKE_RIGIDITY_SECONDARY_WEIGHT = 0.56;
  private static final double STRIKE_STABILITY_SECONDARY_WEIGHT = 0.44;
  private static final double STRIKE_HEAD_RIGIDITY_MIN = 0.65;
  private static final double STRIKE_HEAD_RIGIDITY_MAX = 1.35;
  private static final double STRIKE_ASSEMBLY_STABILITY_MIN = 0.65;
  private static final double STRIKE_ASSEMBLY_STABILITY_MAX = 1.35;
  private static final double STRIKE_FACE_INTEGRITY_BASE = 0.90;
  private static final double STRIKE_FACE_INTEGRITY_WEIGHT = 0.10;
  private static final double STRIKE_FACE_INTEGRITY_MIN = 0.85;
  private static final double STRIKE_FACE_INTEGRITY_MAX = 1.05;
  private static final double STRIKE_MATERIAL_HARDNESS_WEIGHT = 0.35;
  private static final double STRIKE_MATERIAL_TOUGHNESS_WEIGHT = 0.45;
  private static final double STRIKE_MATERIAL_FLEXIBILITY_WEIGHT = 0.40;
  private static final double STRIKE_MATERIAL_BASE = 0.90;
  private static final double STRIKE_MATERIAL_RESPONSE_WEIGHT = 0.15;
  private static final double STRIKE_MATERIAL_MIN = 0.75;
  private static final double STRIKE_MATERIAL_MAX = 1.05;
  private static final double STRIKE_CONTACT_QUALITY_STRUCTURE_WEIGHT = 0.55;
  private static final double STRIKE_CONTACT_QUALITY_INCIDENCE_WEIGHT = 0.45;
  private static final double STRIKE_CONTACT_QUALITY_MIN = 0.78;
  private static final double STRIKE_CONTACT_QUALITY_MAX = 1.05;
  private static final double STRIKE_REPEATABILITY_MIN = 0.70;
  private static final double STRIKE_REPEATABILITY_MAX = 1.02;
  private static final double STRIKE_PRECISION_SCALE = 53.0;
  private static final double STRIKE_THETA_MIN_DEG = 1.0;
  private static final double STRIKE_THETA_MAX_DEG = 11.0;
  private static final double STRIKE_FOCUS_REFERENCE_MIN = 0.82;
  private static final double STRIKE_FOCUS_REFERENCE_MAX = 1.90;
  private static final double STRIKE_FOCUS_RESPONSE_EXPONENT = 0.65;
  private static final double STRIKE_ANGULAR_ERROR_MIN_DEG = 8.5;
  private static final double STRIKE_ANGULAR_ERROR_SPAN_DEG = 6.5;
  private static final double STRIKE_ERROR_BALANCE_WEIGHT = 0.35;
  private static final double STRIKE_ERROR_OVERCOMMITMENT_WEIGHT = 0.25;
  private static final double STRIKE_ERROR_INERTIA_WEIGHT = 0.20;
  private static final double STRIKE_ERROR_EFFECTIVE_MASS_WEIGHT = 0.20;
  private static final double SQRT_TWO = Math.sqrt(2.0);

  /**
   * Builds a slash context from the assembly and generates its precision.
   *
   * @param weapon assembly to evaluate
   * @return slash precision in the shared range {@code [1, 50]}
   * @throws IllegalStateException if the assembly has no slash-capable part
   */
  public static double slash(WeaponAssembly weapon) {
    return slash(AttackGenerationContextFactory.buildSlashContext(weapon));
  }

  /**
   * Generates slash precision from edge geometry, material response, and weapon control.
   * The edge-radius base is {@code 46 * exp(-(radiusNm - 2.5) / 4)} and is multiplied
   * by {@code clamp(14 / bevelAngleDegAtCop, 0.85, 1.15)}, a material modifier, and the
   * shared control factor. The material modifier penalizes squared hardness and toughness
   * deficits and squared flexibility. The control factor is
   * {@code clamp((1.35 - 0.60*m - 0.90*o - 0.45*j - 0.20*(1-e)) * (1 - 2.2*m*m), 0.35, 1.08)},
   * where {@code m} is balance mismatch, {@code o} forward overcommitment, {@code j}
   * normalized inertia, and {@code e} effective-mass ratio.
   *
   * <p>Edge radius and bevel angle are floored before use. The final result is clamped
   * to the shared precision range {@code [1, 50]} and rounded to three decimal places.
   *
   * @param context snapshot containing slash contact, material, delivery, and assembly data
   * @return generated slash precision
   */
  public static double slash(AttackGenerationContextSlash context) {
    AssemblyPhysicsSnapshot assembly = context.assembly();
    AttackDeliverySnapshot delivery = context.delivery();
    MaterialResponseSnapshot material = context.material();
    ContactSnapshotSlash contact = context.contact();

    double edgeRadiusNm = Math.max(SLASH_EDGE_RADIUS_ATOMIC_LIMIT_NM, contact.edgeRadiusNm());
    double basePrecision = edgeRadiusToPrecisionBase(edgeRadiusNm);

    double edgeAngleDeg = Math.max(SLASH_EDGE_ANGLE_MIN_DEG, contact.bevelAngleDegAtCop());
    double edgeAngleFactor = slashBevelAngleFactor(edgeAngleDeg);

    double materialModifier = slashMaterialModifier(material);
    double controlFactor = controlFactor(delivery, assembly);
    return slashScore(basePrecision * edgeAngleFactor, materialModifier, controlFactor);
  }

  static double slashScore(double geometryBase, double materialModifier, double controlFactor) {
    return Util.round(Mth.clamp(geometryBase * materialModifier * controlFactor, PRECISION_MIN, PRECISION_MAX), 3);
  }

  static double slashMaterialModifier(MaterialResponseSnapshot material) {
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

  /**
   * Builds a thrust context from the assembly and generates its precision.
   *
   * @param weapon assembly to evaluate
   * @return thrust precision in the shared range {@code [1, 50]}
   * @throws IllegalStateException if the assembly has no thrust-capable part
   */
  public static double thrust(WeaponAssembly weapon) {
    return thrust(AttackGenerationContextFactory.buildThrustContext(weapon));
  }

  /**
   * Generates thrust precision from point taper, point-base geometry, tip aspect ratio,
   * material response, control, and motion mode. Taper produces
   * {@code taperBase = 7.5 + 42.5 * pointTaper^1.55}. Width, thickness, and tip aspect
   * factors use square-root/power curves anchored to observed profile dimensions, with
   * widened bounds to keep realistic points responsive.
   * Together they form {@code base = taperBase * (0.70 + 0.30*geometryFactor) * tipFactor}.
   * The material modifier starts at 1.0 and subtracts {@code 0.20 * risk}, where risk is
   * {@code clamp(0.42*(1-hardness)^2 + 0.45*(1-toughness)^2 + 0.95*flexibility^2, 0, 1)};
   * its effective range for normalized materials is 0.80 to 1.0.
   *
   * <p>Geometry descriptors are each applied once; penetration efficiency remains a
   * lethality input so point width and thickness are not penalized a second time. The
   * result is multiplied by the shared control factor documented on
   * {@link #slash(AttackGenerationContextSlash)} and a motion factor. Linear thrust uses 1.0; rotational thrust
   * uses {@code clamp((0.72 - 0.55*m - 0.40*o - 0.22*e) * alignmentEfficiency, 0.26, 0.72)}.
   * The final result is clamped to the shared precision range {@code [1, 50]} and rounded
   * to three decimal places.
   *
   * @param context snapshot containing thrust contact, material, delivery, and assembly data
   * @return generated thrust precision
   */
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
    double tipLengthFactor = thrustTipAspectFactor(tipAspectRatio);

    double widthFactor = thrustWidthFactor(widthAtPointBase);
    double thicknessFactor = thrustThicknessFactor(thicknessAtPointBase);
    double pointBaseGeometryFactor = THRUST_GEOMETRY_WIDTH_WEIGHT * widthFactor + THRUST_GEOMETRY_THICKNESS_WEIGHT * thicknessFactor;

    double basePrecision = (THRUST_TAPER_WEIGHT * taperDrivenPrecision + THRUST_POINT_BASE_GEOMETRY_WEIGHT * (taperDrivenPrecision * pointBaseGeometryFactor)) * tipLengthFactor;

    double materialModifier = thrustMaterialModifier(material);
    double controlFactor = controlFactor(delivery, assembly);
    double motionFactor = thrustMotionPrecisionFactor(delivery, contact);
    return thrustScore(basePrecision, materialModifier, controlFactor, motionFactor);
  }

  static double thrustScore(
      double geometryBase,
      double materialModifier,
      double controlFactor,
      double motionFactor) {
    return Util.round(
      Mth.clamp(geometryBase * materialModifier * controlFactor * motionFactor,
        PRECISION_MIN, PRECISION_MAX),
      3);
  }

  static double thrustMaterialModifier(MaterialResponseSnapshot material) {
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

  /**
   * Builds a strike context from the assembly and generates its precision.
   *
   * @param weapon assembly to evaluate
   * @return strike precision in the shared range {@code [1, 50]}
   * @throws IllegalStateException if the assembly has no strike-capable part
   */
  public static double strike(WeaponAssembly weapon) {
    return strike(AttackGenerationContextFactory.buildStrikeContext(weapon));
  }

  /**
   * Generates strike precision from the probability that the face lands within its angular
   * tolerance. Focus maps to a face tolerance of roughly 1-11 degrees, which is compared
   * against an angular delivery-error estimate with the normal-CDF expression
   * {@code erf(thetaMax / (sigmaError * sqrt(2)))}. Contact area contributes only a soft
   * penalty for broad, relatively flat faces; crowned faces are not penalized for area.
   * Material response, face integrity, structural/incidence quality, and repeatability
   * supply the remaining multipliers.
   *
   * <p>Contact area is floored at 0.02 cm^2. The final result is clamped to the shared
   * precision range {@code [1, 50]} and rounded to three decimal places.
   *
   * @param context snapshot containing strike contact, material, delivery, and assembly data
   * @return generated strike precision
   */
  public static double strike(AttackGenerationContextStrike context) {
    AssemblyPhysicsSnapshot assembly = context.assembly();
    AttackDeliverySnapshot delivery = context.delivery();
    MaterialResponseSnapshot material = context.material();
    ContactSnapshotStrike contact = context.contact();

    double effectiveContactAreaCm2 = Math.max(STRIKE_CONTACT_AREA_MIN_CM2, contact.effectiveContactAreaCm2());
    double geometryFocus = contact.strikeFaceGeometryFocus();
    double headRigidity = Mth.clamp(contact.strikeHeadRigidity(), STRIKE_HEAD_RIGIDITY_MIN, STRIKE_HEAD_RIGIDITY_MAX);
    double assemblyStability = Mth.clamp(contact.strikeAssemblyStability(), STRIKE_ASSEMBLY_STABILITY_MIN, STRIKE_ASSEMBLY_STABILITY_MAX);
    double secondaryFactor = STRIKE_RIGIDITY_SECONDARY_WEIGHT * headRigidity + STRIKE_STABILITY_SECONDARY_WEIGHT * assemblyStability;

    double thetaMaxDeg = strikeThetaMaxDegrees(geometryFocus);
    double sigmaErrorDeg = strikeAngularErrorSigmaDegrees(
      delivery.balanceMismatch(),
      delivery.forwardOvercommitment(),
      assembly.normalizedInertiaCoefficient(),
      delivery.effectiveMassRatio());
    double flushProbability = strikeFlushProbability(thetaMaxDeg, sigmaErrorDeg);
    double materialModifier = strikeMaterialModifier(material);
    double faceIntegrityFactor = strikeFaceIntegrityFactor(secondaryFactor);
    double contactQualityFactor = strikeContactQualityFactor(contact.strikeStructuralEfficiency(), contact.strikeIncidenceEfficiency());
    double repeatabilityFactor = strikeRepeatabilityFactor(contact.strikeRepeatability());
    double areaInteractionFactor = strikeAreaInteractionFactor(effectiveContactAreaCm2, geometryFocus);
    return strikeScore(
      flushProbability,
      materialModifier,
      faceIntegrityFactor,
      contactQualityFactor,
      repeatabilityFactor,
      areaInteractionFactor);
  }

  static double strikeFaceIntegrityFactor(double weightedRigidityAndStability) {
    return Mth.clamp(
      STRIKE_FACE_INTEGRITY_BASE + STRIKE_FACE_INTEGRITY_WEIGHT * weightedRigidityAndStability,
      STRIKE_FACE_INTEGRITY_MIN,
      STRIKE_FACE_INTEGRITY_MAX);
  }

  static double strikeScore(
      double flushProbability,
      double materialModifier,
      double faceIntegrityFactor,
      double contactQualityFactor,
      double repeatabilityFactor,
      double areaInteractionFactor) {
    return Util.round(Mth.clamp(
      STRIKE_PRECISION_SCALE * flushProbability * materialModifier * faceIntegrityFactor
        * contactQualityFactor * repeatabilityFactor * areaInteractionFactor,
      PRECISION_MIN,
      PRECISION_MAX), 3);
  }

  static double strikeThetaMaxDegrees(double strikeFaceGeometryFocus) {
    double normalizedFocus = Mth.clamp(
      (strikeFaceGeometryFocus - STRIKE_FOCUS_REFERENCE_MIN)
        / (STRIKE_FOCUS_REFERENCE_MAX - STRIKE_FOCUS_REFERENCE_MIN),
      0.0,
      1.0);
    return STRIKE_THETA_MIN_DEG + (STRIKE_THETA_MAX_DEG - STRIKE_THETA_MIN_DEG)
      * Math.pow(normalizedFocus, STRIKE_FOCUS_RESPONSE_EXPONENT);
  }

  static double strikeAngularErrorSigmaDegrees(
      double balanceMismatch,
      double forwardOvercommitment,
      double normalizedInertia,
      double effectiveMassRatio) {
    double severity = Mth.clamp(
      STRIKE_ERROR_BALANCE_WEIGHT * Math.max(0.0, balanceMismatch)
        + STRIKE_ERROR_OVERCOMMITMENT_WEIGHT * Math.max(0.0, forwardOvercommitment)
        + STRIKE_ERROR_INERTIA_WEIGHT * Math.max(0.0, normalizedInertia)
        + STRIKE_ERROR_EFFECTIVE_MASS_WEIGHT * Math.max(0.0, effectiveMassRatio),
      0.0,
      1.0);
    return STRIKE_ANGULAR_ERROR_MIN_DEG + STRIKE_ANGULAR_ERROR_SPAN_DEG * severity;
  }

  static double strikeFlushProbability(double thetaMaxDeg, double sigmaErrorDeg) {
    double safeSigma = Math.max(1.0e-6, sigmaErrorDeg);
    return errorFunction(Math.max(0.0, thetaMaxDeg) / (safeSigma * SQRT_TWO));
  }

  static double strikeAreaInteractionFactor(double effectiveContactAreaCm2, double strikeFaceGeometryFocus) {
    double broadFaceShare = Mth.clamp(
      (STRIKE_AREA_FACE_FOCUS_REFERENCE - strikeFaceGeometryFocus) / STRIKE_AREA_FACE_FOCUS_REFERENCE,
      0.0,
      1.0);
    double areaExcess = Math.max(0.0,
      Math.log(Math.max(STRIKE_CONTACT_AREA_MIN_CM2, effectiveContactAreaCm2) / STRIKE_CONTACT_AREA_REFERENCE_CM2));
    return Math.exp(-STRIKE_AREA_PENALTY_SCALE * broadFaceShare * areaExcess);
  }

  private static double errorFunction(double value) {
    // Abramowitz-Stegun 7.1.26 approximation; maximum absolute error is about 1.5e-7.
    double x = Math.abs(value);
    double t = 1.0 / (1.0 + 0.3275911 * x);
    double polynomial = (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t
      - 0.284496736) * t + 0.254829592) * t;
    double result = 1.0 - polynomial * Math.exp(-x * x);
    return value < 0.0 ? -result : result;
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

  static double strikeMaterialModifier(MaterialResponseSnapshot material) {
    double rigidityScore = Mth.clamp(
      STRIKE_MATERIAL_HARDNESS_WEIGHT * material.hardnessNorm()
        + STRIKE_MATERIAL_TOUGHNESS_WEIGHT * material.toughnessNorm()
        - STRIKE_MATERIAL_FLEXIBILITY_WEIGHT * material.flexibilityNorm(),
      -1.0,
      1.0);
    return Mth.clamp(
      STRIKE_MATERIAL_BASE + STRIKE_MATERIAL_RESPONSE_WEIGHT * rigidityScore,
      STRIKE_MATERIAL_MIN,
      STRIKE_MATERIAL_MAX);
  }

  static double edgeRadiusToPrecisionBase(double edgeRadius) {
    return SLASH_EDGE_RADIUS_BASE_MAX
      * Math.exp(-(Math.max(SLASH_EDGE_RADIUS_ATOMIC_LIMIT_NM, edgeRadius) - SLASH_EDGE_RADIUS_ATOMIC_LIMIT_NM)
        / SLASH_EDGE_RADIUS_SCALE_NM);
  }

  static double slashBevelAngleFactor(double bevelAngleDeg) {
    return Mth.clamp(
      SLASH_EDGE_ANGLE_REFERENCE_DEG / Math.max(SLASH_EDGE_ANGLE_MIN_DEG, bevelAngleDeg),
      SLASH_EDGE_ANGLE_FACTOR_MIN,
      SLASH_EDGE_ANGLE_FACTOR_MAX);
  }

  static double pointTaperToPrecisionBase(double pointTaper) {
    double acceleratedTaper = Math.pow(Mth.clamp(pointTaper, THRUST_TAPER_MIN, THRUST_TAPER_MAX), THRUST_TAPER_EXPONENT);
    return THRUST_POINT_BASE_PRECISION_MIN + (THRUST_POINT_BASE_PRECISION_MAX - THRUST_POINT_BASE_PRECISION_MIN) * acceleratedTaper;
  }

  static double thrustWidthFactor(double widthAtPointBaseCm) {
    return Mth.clamp(
      Math.pow(THRUST_REFERENCE_POINT_BASE_WIDTH_CM / Math.max(THRUST_POINT_BASE_WIDTH_MIN_CM, widthAtPointBaseCm), 0.5),
      THRUST_WIDTH_FACTOR_MIN,
      THRUST_WIDTH_FACTOR_MAX);
  }

  static double thrustThicknessFactor(double thicknessAtPointBaseCm) {
    return Mth.clamp(
      Math.pow(THRUST_REFERENCE_POINT_BASE_THICKNESS_CM / Math.max(THRUST_POINT_BASE_THICKNESS_MIN_CM, thicknessAtPointBaseCm), 0.5),
      THRUST_THICKNESS_FACTOR_MIN,
      THRUST_THICKNESS_FACTOR_MAX);
  }

  static double thrustTipAspectFactor(double tipAspectRatio) {
    return Mth.clamp(
      Math.pow(Math.max(0.0, tipAspectRatio) / THRUST_REFERENCE_TIP_ASPECT_RATIO, THRUST_TIP_ASPECT_RATIO_EXPONENT),
      THRUST_TIP_LENGTH_FACTOR_MIN,
      THRUST_TIP_LENGTH_FACTOR_MAX);
  }

  static double controlFactor(AttackDeliverySnapshot delivery, AssemblyPhysicsSnapshot assembly) {
    double balanceMismatch = delivery.balanceMismatch();
    double overcommitment = delivery.forwardOvercommitment();
    double effectiveMassRatio = delivery.effectiveMassRatio();
    double inertiaCoefficient = assembly.normalizedInertiaCoefficient();

    double boundedEffectiveMassRatio = Mth.clamp(effectiveMassRatio, 0.0, 1.0);
    double control = CONTROL_BASE
      - CONTROL_BALANCE_MISMATCH_WEIGHT * balanceMismatch
      - CONTROL_OVERCOMMITMENT_WEIGHT * overcommitment
      - CONTROL_INERTIA_WEIGHT * inertiaCoefficient
      - CONTROL_EFFECTIVE_MASS_WEIGHT * (1.0 - boundedEffectiveMassRatio);

    double shapedControl = control * (1.0 - HANDLING_SENSITIVITY * balanceMismatch * balanceMismatch);
    return Mth.clamp(shapedControl, HANDLING_FACTOR_MIN, HANDLING_FACTOR_MAX);
  }

  static double thrustMotionPrecisionFactor(AttackDeliverySnapshot delivery, ContactSnapshotThrust contact) {
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
