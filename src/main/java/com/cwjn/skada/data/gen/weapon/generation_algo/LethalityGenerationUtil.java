package com.cwjn.skada.data.gen.weapon.generation_algo;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.cwjn.skada.data.registry.AttackType;

import net.minecraft.util.Mth;
import com.cwjn.skada.util.Util;

public abstract class LethalityGenerationUtil {

  private static final AttackType SLASH_CONTEXT = new AttackType("slash", null, null, null, SlashCapable.class);
  private static final AttackType THRUST_CONTEXT = new AttackType("thrust", null, null, null, ThrustCapable.class);
  private static final AttackType STRIKE_CONTEXT = new AttackType("strike", null, null, null, StrikeCapable.class);

  // Shared constants
  private static final double MIN_LETHALITY = 1.0;
  private static final double MAX_LETHALITY = 120.0;
  private static final double NUMERIC_EPSILON = 1.0e-6;
  private static final int LETHALITY_ROUND_DECIMALS = 3;

  // Slash tuning constants
  private static final double SLASH_BASE_LETHALITY_SCALE = 24.0;
  private static final double SLASH_BASE_LETHALITY_ENERGY_FACTOR = 26.0;
  private static final double SLASH_WEDGE_POTENTIAL_PER_CM = 8.0;
  private static final double SLASH_REQUIRED_MOMENTUM_SCALE = 0.04;
  private static final double SLASH_MATERIAL_HARDNESS_SCALE = 0.08;
  private static final double SLASH_MATERIAL_FLEXIBILITY_SCALE = 0.08;
  private static final double IMPACT_EFFICIENCY_SENSITIVITY = 3.0;
  private static final double IMPACT_EFFICIENCY_MIN = 0.45;
  private static final double IMPACT_EFFICIENCY_MAX = 1.0;
  private static final double LOCAL_FACTOR_MIN = 0.75;
  private static final double LOCAL_FACTOR_MAX = 1.35;

  private static final double SLASH_DELIVERY_BALANCE_MISMATCH_WEIGHT = 0.55;
  private static final double SLASH_DELIVERY_FORWARD_BIAS_WEIGHT = 0.80;
  private static final double SLASH_DELIVERY_INERTIA_WEIGHT = 0.50;
  private static final double SLASH_DELIVERY_EFFECTIVE_MASS_WEIGHT = 0.55;
  private static final double SLASH_DELIVERY_LEVERAGE_THRESHOLD = 0.20;
  private static final double SLASH_DELIVERY_LEVERAGE_WEIGHT = 1.25;
  private static final double SLASH_DELIVERY_FACTOR_MIN = 0.45;
  private static final double SLASH_DELIVERY_FACTOR_MAX = 1.05;

  // Thrust tuning constants
  private static final double THRUST_STRENGTH_REFERENCE = 50.0;
  private static final double THRUST_REFERENCE_MASS_KG = 1.0;
  private static final double THRUST_MASS_PENALTY_EXPONENT = 0.32;
  private static final double THRUST_LINEAR_VELOCITY_SCALE = 3.8;

  private static final double THRUST_BASE_LETHALITY_SCALE = 9.5;
  private static final double THRUST_BASE_LETHALITY_ENERGY_FACTOR = 0.38;

  private static final double THRUST_WEDGE_POTENTIAL_PER_CM = 4.4;
  private static final double THRUST_REQUIRED_MOMENTUM_SCALE = 0.10;

  private static final double THRUST_TAPER_BASE = 0.65;
  private static final double THRUST_TAPER_SCALE = 0.45;

  private static final double THRUST_MATERIAL_HARDNESS_SCALE = 0.06;
  private static final double THRUST_MATERIAL_TOUGHNESS_SCALE = 0.08;
  private static final double THRUST_MATERIAL_FLEXIBILITY_SCALE = 0.06;
  private static final double THRUST_LINEAR_DELIVERY_BALANCE_MISMATCH_WEIGHT = 0.32;
  private static final double THRUST_LINEAR_DELIVERY_DIRECTIONAL_WEIGHT = 0.20;
  private static final double THRUST_LINEAR_DELIVERY_INERTIA_WEIGHT = 0.25;
  private static final double THRUST_LINEAR_DELIVERY_EFFECTIVE_MASS_WEIGHT = 0.60;
  private static final double THRUST_LINEAR_DELIVERY_FACTOR_MIN = 0.60;
  private static final double THRUST_LINEAR_DELIVERY_FACTOR_MAX = 1.10;
  private static final double THRUST_ROTATIONAL_DELIVERY_BALANCE_MISMATCH_WEIGHT = 0.55;
  private static final double THRUST_ROTATIONAL_DELIVERY_DIRECTIONAL_WEIGHT = 0.90;
  private static final double THRUST_ROTATIONAL_DELIVERY_INERTIA_WEIGHT = 0.45;
  private static final double THRUST_ROTATIONAL_DELIVERY_EFFECTIVE_MASS_WEIGHT = 0.60;
  private static final double THRUST_ROTATIONAL_DELIVERY_FACTOR_MIN = 0.38;
  private static final double THRUST_ROTATIONAL_DELIVERY_FACTOR_MAX = 1.05;
  private static final double THRUST_PENETRATION_FACTOR_MIN = 0.32;
  private static final double THRUST_PENETRATION_FACTOR_MAX = 1.05;

  // Strike tuning constants
  private static final double STRIKE_BASE_LETHALITY_SCALE = 56.0;
  private static final double STRIKE_MOMENTUM_EXPONENTIAL_SCALE = 14.0;
  private static final double STRIKE_DELIVERY_BALANCE_MISMATCH_WEIGHT = 0.40;
  private static final double STRIKE_DELIVERY_UNDERWEIGHTED_HEAD_WEIGHT = 0.70;
  private static final double STRIKE_DELIVERY_INERTIA_WEIGHT = 0.40;
  private static final double STRIKE_DELIVERY_EFFECTIVE_MASS_WEIGHT = 0.50;
  private static final double STRIKE_DELIVERY_FACTOR_MIN = 0.45;
  private static final double STRIKE_DELIVERY_FACTOR_MAX = 1.08;
  private static final double STRIKE_GEOMETRY_FOCUS_WEIGHT = 0.55;
  private static final double STRIKE_GEOMETRY_RIGIDITY_WEIGHT = 0.25;
  private static final double STRIKE_GEOMETRY_STABILITY_WEIGHT = 0.20;
  private static final double STRIKE_LOCALIZATION_REFERENCE_CM2 = 1.6;
  private static final double STRIKE_LOCALIZATION_AREA_EXPONENT = 0.50;
  private static final double STRIKE_LOCALIZATION_AREA_WEIGHT = 0.70;
  private static final double STRIKE_LOCALIZATION_FOCUS_WEIGHT = 0.30;
  private static final double STRIKE_LOCALIZATION_FACTOR_MIN = 0.55;
  private static final double STRIKE_LOCALIZATION_FACTOR_MAX = 1.25;
  private static final double STRIKE_COMPLIANCE_FACTOR_MIN = 0.72;
  private static final double STRIKE_COMPLIANCE_FACTOR_MAX = 1.08;
  private static final double STRIKE_INCIDENCE_FACTOR_MIN = 0.72;
  private static final double STRIKE_INCIDENCE_FACTOR_MAX = 1.05;

  /**
   * Slash lethality:
   * - primary: rotational kinetic energy
   * - secondary: wedge thickness bonus gated by available angular momentum
   */
  public static double slash(WeaponAssembly weapon) {
    WeaponPartEntry partEntry = weapon.primaryPartForAttackType(SLASH_CONTEXT).orElseThrow(() -> new IllegalStateException("Tried to generate slash lethality for weapon without slash capability"));
    MaterialInfo material = partEntry.material();
    SlashCapable slashCapable = (SlashCapable) partEntry.part();

    double momentOfInertia = PhysicsUtil.toKgM2(weapon.momentOfInertiaAboutBase(WeaponAxis.Z, WeaponAssembly.LARGE_SAMPLE_SIZE));
    double angularVelocity = Util.angularVelocity(momentOfInertia, SkadaData.PLAYER_STRENGTH);
    double angularMomentum = momentOfInertia * angularVelocity;
    double rotationalKineticEnergy = 0.5 * momentOfInertia * angularVelocity * angularVelocity;
    double baseLethality = slashBaseLethalityFromEnergy(rotationalKineticEnergy);

    double cop = weapon.centreOfPercussion(WeaponAssembly.LARGE_SAMPLE_SIZE);
    double wedgeThicknessCm = slashCapable.wedgeThicknessCmAt(cop);
    double bevelAngle = slashCapable.edgeAngleDegreesAt(cop);

    double wedgePotential = SLASH_WEDGE_POTENTIAL_PER_CM * wedgeThicknessCm;
    double requiredMomentum = SLASH_REQUIRED_MOMENTUM_SCALE * wedgeThicknessCm * (1.0 + Math.tan(Math.toRadians(bevelAngle)));
    double driveRatio = angularMomentum / Math.max(NUMERIC_EPSILON, requiredMomentum);
    double wedgeDrive = Mth.clamp(driveRatio, 0.0, 1.0);
    double wedgeBonus = wedgePotential * wedgeDrive;

    double normalizedHardness = MaterialInfo.normalizeMaterial(material.hardness());
    double normalizedCenteredFlexibility = Math.abs(MaterialInfo.normalizeMaterial(material.flexibility()) - 0.5) * 2.0;
    double materialFactor = 1.0 + SLASH_MATERIAL_HARDNESS_SCALE * normalizedHardness - SLASH_MATERIAL_FLEXIBILITY_SCALE * normalizedCenteredFlexibility;

    double edgeRadiusNm = Math.max(NUMERIC_EPSILON, slashCapable.edgeRadiusNm());
    double bevelAcuity = Mth.clamp(18.0 / Math.max(NUMERIC_EPSILON, bevelAngle), 0.35, 1.15);
    double wedgeThinness = Mth.clamp(0.35 / Math.max(NUMERIC_EPSILON, wedgeThicknessCm), 0.45, 1.20);
    double edgeAcuity = Mth.clamp(10.0 / edgeRadiusNm, 0.45, 1.25);
    double specializationFactor = Mth.clamp(0.55 * bevelAcuity + 0.25 * wedgeThinness + 0.20 * edgeAcuity, 0.40, 1.25);

    double strikePointNorm = weapon.normalizedStrikePointForAttackType(SLASH_CONTEXT, WeaponAssembly.LARGE_SAMPLE_SIZE);
    double impactEfficiency = impactEfficiency(weapon, strikePointNorm);

    double deliveryFactor = slashDeliveryFactor(weapon);
    double realizedSpecialization = realizedLocalFactor(specializationFactor, deliveryFactor);
    double lethality = (baseLethality + wedgeBonus * deliveryFactor) * impactEfficiency * materialFactor * realizedSpecialization * deliveryFactor;
    double finalLethality = Util.round(Mth.clamp(lethality, MIN_LETHALITY, MAX_LETHALITY), LETHALITY_ROUND_DECIMALS);
    
    return finalLethality;
  }

  /**
   * Thrust lethality:
   * - primary: linear kinetic energy
   * - secondary: wedge-thickness contribution gated by available linear momentum
   */
  public static double thrust(WeaponAssembly weapon) {
    WeaponPartEntry thrustHead = weapon.primaryPartForAttackType(THRUST_CONTEXT).orElseThrow(() -> new IllegalStateException("Tried to generate thrust lethality for weapon without thrust capability"));
    MaterialInfo material = thrustHead.material();
    ThrustCapable thrustCapable = (ThrustCapable) thrustHead.part();

    double massKg = PhysicsUtil.toKg(weapon.mass(WeaponAssembly.LARGE_SAMPLE_SIZE));

    double strengthFactor = Math.sqrt(SkadaData.PLAYER_STRENGTH / THRUST_STRENGTH_REFERENCE);
    double massPenalty = Math.pow(Math.max(NUMERIC_EPSILON, massKg) / THRUST_REFERENCE_MASS_KG, THRUST_MASS_PENALTY_EXPONENT);
    double linearVelocity = THRUST_LINEAR_VELOCITY_SCALE * strengthFactor / Math.max(NUMERIC_EPSILON, massPenalty);

    double linearMomentum = massKg * linearVelocity;
    double linearKineticEnergy = 0.5 * massKg * linearVelocity * linearVelocity;
    double baseLethality = thrustBaseLethalityFromEnergy(linearKineticEnergy);

    double wedgeThicknessCm = thrustCapable.widthAtPointBase();
    double tipLengthCm = thrustCapable.tipLengthCm();
    double bevelAngle = Math.toDegrees(Math.atan((wedgeThicknessCm * 0.5) / tipLengthCm));

    double wedgePotential = THRUST_WEDGE_POTENTIAL_PER_CM * wedgeThicknessCm;
    double requiredMomentum = THRUST_REQUIRED_MOMENTUM_SCALE * wedgeThicknessCm * (1.0 + Math.tan(Math.toRadians(bevelAngle)));
    double driveRatio = linearMomentum / Math.max(NUMERIC_EPSILON, requiredMomentum);
    double wedgeDrive = Mth.clamp(driveRatio, 0.0, 1.0);
    double wedgeBonus = wedgePotential * wedgeDrive;

    double taperFactor = THRUST_TAPER_BASE + THRUST_TAPER_SCALE * thrustCapable.pointTaper();
    double thicknessAtPointBase = Math.max(NUMERIC_EPSILON, thrustCapable.thicknessAtPointBase());
    double tipSectionMean = 0.5 * (wedgeThicknessCm + thicknessAtPointBase);
    double needleFactor = Mth.clamp(0.90 / Math.max(NUMERIC_EPSILON, tipSectionMean), 0.35, 1.40);
    double tipSlenderness = tipLengthCm / Math.max(wedgeThicknessCm, thicknessAtPointBase);
    double slendernessFactor = Mth.clamp((tipSlenderness - 1.5) / 6.5, 0.30, 1.20);
    double specializationFactor = Mth.clamp(0.55 * taperFactor + 0.25 * needleFactor + 0.20 * slendernessFactor, 0.35, 1.20);

    double normalizedHardness = MaterialInfo.normalizeMaterial(material.hardness());
    double normalizedToughness = MaterialInfo.normalizeMaterial(material.toughness());
    double normalizedCenteredFlexibility = Math.abs(MaterialInfo.normalizeMaterial(material.flexibility()) - 0.5) * 2.0;
    double materialFactor = 1.0 + THRUST_MATERIAL_HARDNESS_SCALE * normalizedHardness
      + THRUST_MATERIAL_TOUGHNESS_SCALE * normalizedToughness
      - THRUST_MATERIAL_FLEXIBILITY_SCALE * normalizedCenteredFlexibility;

    boolean rotationalThrust = weapon.isThrustRotational(WeaponAssembly.LARGE_SAMPLE_SIZE);
    double impactEfficiency = 1.0;
    if (rotationalThrust) {
      double strikePointNorm = weapon.normalizedStrikePointForAttackType(THRUST_CONTEXT, WeaponAssembly.LARGE_SAMPLE_SIZE);
      impactEfficiency = impactEfficiency(weapon, strikePointNorm);
    }
    double deliveryFactor = thrustDeliveryFactor(weapon, rotationalThrust);
    double realizedSpecialization = realizedLocalFactor(specializationFactor, deliveryFactor);
    double penetrationFactor = thrustPenetrationFactor(thrustCapable.thrustPenetrationEfficiency());

    double lethality = (baseLethality + wedgeBonus * deliveryFactor) * impactEfficiency * realizedSpecialization * penetrationFactor * materialFactor * deliveryFactor;
    double finalLethality = Util.round(Mth.clamp(lethality, MIN_LETHALITY, MAX_LETHALITY), LETHALITY_ROUND_DECIMALS);
    if (SkadaData.DEBUG_ENABLED) {
      Skada.LOGGER.debug("[GEN][Lethality][thrust] massKg={}, linVel={}, linMomentum={}, linKE={}, wedgeThicknessCm={}, thicknessAtPointBaseCm={}, tipLengthCm={}, bevelDeg={}, wedgePotential={}, requiredMomentum={}, driveRatio={}, wedgeDrive={}, wedgeBonus={}, taperFactor={}, needleFactor={}, tipSlenderness={}, slendernessFactor={}, rotationalThrust={}, impactEfficiency={}, deliveryFactor={}, realizedSpecialization={}, materialFactor={}, final={}",
        massKg, linearVelocity, linearMomentum, linearKineticEnergy, wedgeThicknessCm, thicknessAtPointBase, tipLengthCm, bevelAngle,
        wedgePotential, requiredMomentum, driveRatio, wedgeDrive, wedgeBonus, taperFactor, needleFactor,
        tipSlenderness, slendernessFactor, rotationalThrust, impactEfficiency, deliveryFactor, realizedSpecialization,
        materialFactor, finalLethality);
    }
    return finalLethality;
  }

  /**
   * Strike lethality is based entirely on angular momentum.
   */
  public static double strike(WeaponAssembly weapon) {
    WeaponPartEntry strikeHead = weapon.primaryPartForAttackType(STRIKE_CONTEXT).orElseThrow(() -> new IllegalStateException("Tried to generate strike lethality for weapon without strike capability"));
    StrikeCapable strikeCapable = (StrikeCapable) strikeHead.part();

    double momentOfInertia = PhysicsUtil.toKgM2(weapon.momentOfInertiaAboutBase(WeaponAxis.Z, WeaponAssembly.LARGE_SAMPLE_SIZE));
    double angularVelocity = Util.angularVelocity(momentOfInertia, SkadaData.PLAYER_STRENGTH);
    double angularMomentum = Math.max(0.0, momentOfInertia * angularVelocity);

    double strikePointNorm = weapon.normalizedStrikePointForAttackType(STRIKE_CONTEXT, WeaponAssembly.LARGE_SAMPLE_SIZE);
    double impactEfficiency = impactEfficiency(weapon, strikePointNorm);
    double deliveryFactor = strikeDeliveryFactor(weapon);
    double localizationFactor = strikeLocalizationFactor(
      strikeCapable.effectiveContactAreaCm2(),
      strikeCapable.strikeFaceGeometryFocus());
    double complianceFactor = strikeComplianceFactor(strikeCapable.strikeStructuralEfficiency());
    double incidenceFactor = strikeIncidenceFactor(strikeCapable.strikeIncidenceEfficiency());
    double strikeGeometryFactor = Mth.clamp(
      STRIKE_GEOMETRY_FOCUS_WEIGHT * strikeCapable.strikeFaceGeometryFocus()
        + STRIKE_GEOMETRY_RIGIDITY_WEIGHT * strikeCapable.strikeHeadRigidity()
        + STRIKE_GEOMETRY_STABILITY_WEIGHT * strikeCapable.strikeAssemblyStability(),
      LOCAL_FACTOR_MIN,
      LOCAL_FACTOR_MAX);
    double realizedStrikeGeometry = realizedLocalFactor(strikeGeometryFactor, deliveryFactor);
    double lethality = strikeLethalityFromMomentum(angularMomentum, impactEfficiency) * deliveryFactor * localizationFactor * complianceFactor * incidenceFactor * realizedStrikeGeometry;
    double finalLethality = Util.round(Mth.clamp(lethality, MIN_LETHALITY, MAX_LETHALITY), LETHALITY_ROUND_DECIMALS);
    if (SkadaData.DEBUG_ENABLED) {
      Skada.LOGGER.debug("[GEN][Lethality][strike] moiKgM2={}, angVel={}, angMomentum={}, strikePointNorm={}, impactEfficiency={}, deliveryFactor={}, localizationFactor={}, complianceFactor={}, incidenceFactor={}, strikeGeometryFactor={}, realizedStrikeGeometry={}, final={}",
        momentOfInertia, angularVelocity, angularMomentum, strikePointNorm, impactEfficiency, deliveryFactor,
        localizationFactor, complianceFactor, incidenceFactor, strikeGeometryFactor, realizedStrikeGeometry, finalLethality);
    }
    return finalLethality;
  }

  static double strikeLethalityFromMomentum(double angularMomentum, double impactEfficiency) {
    return STRIKE_BASE_LETHALITY_SCALE
      * (1.0 - Math.exp(-STRIKE_MOMENTUM_EXPONENTIAL_SCALE * Math.max(0.0, angularMomentum)))
      * Mth.clamp(impactEfficiency, IMPACT_EFFICIENCY_MIN, IMPACT_EFFICIENCY_MAX);
  }

  static double slashBaseLethalityFromEnergy(double rotationalKineticEnergy) {
    return SLASH_BASE_LETHALITY_SCALE * Math.log1p(SLASH_BASE_LETHALITY_ENERGY_FACTOR * Math.max(0.0, rotationalKineticEnergy));
  }

  static double thrustBaseLethalityFromEnergy(double linearKineticEnergy) {
    return THRUST_BASE_LETHALITY_SCALE * Math.log1p(THRUST_BASE_LETHALITY_ENERGY_FACTOR * Math.max(0.0, linearKineticEnergy));
  }

  static double thrustPenetrationFactor(double thrustPenetrationEfficiency) {
    return Mth.clamp(thrustPenetrationEfficiency, THRUST_PENETRATION_FACTOR_MIN, THRUST_PENETRATION_FACTOR_MAX);
  }

  static double strikeLocalizationFactor(double effectiveContactAreaCm2, double strikeFaceGeometryFocus) {
    double contactArea = Math.max(NUMERIC_EPSILON, effectiveContactAreaCm2);
    double areaFactor = Mth.clamp(
      Math.pow(STRIKE_LOCALIZATION_REFERENCE_CM2 / contactArea, STRIKE_LOCALIZATION_AREA_EXPONENT),
      STRIKE_LOCALIZATION_FACTOR_MIN,
      STRIKE_LOCALIZATION_FACTOR_MAX);
    double focusFactor = Mth.clamp(
      strikeFaceGeometryFocus,
      STRIKE_LOCALIZATION_FACTOR_MIN,
      STRIKE_LOCALIZATION_FACTOR_MAX);
    return Mth.clamp(
      STRIKE_LOCALIZATION_AREA_WEIGHT * areaFactor + STRIKE_LOCALIZATION_FOCUS_WEIGHT * focusFactor,
      STRIKE_LOCALIZATION_FACTOR_MIN,
      STRIKE_LOCALIZATION_FACTOR_MAX);
  }

  static double strikeComplianceFactor(double strikeStructuralEfficiency) {
    return Mth.clamp(strikeStructuralEfficiency, STRIKE_COMPLIANCE_FACTOR_MIN, STRIKE_COMPLIANCE_FACTOR_MAX);
  }

  static double strikeIncidenceFactor(double strikeIncidenceEfficiency) {
    return Mth.clamp(strikeIncidenceEfficiency, STRIKE_INCIDENCE_FACTOR_MIN, STRIKE_INCIDENCE_FACTOR_MAX);
  }

  private static double impactEfficiency(WeaponAssembly weapon, double strikePointNorm) {
    double cop = weapon.centreOfPercussion(WeaponAssembly.LARGE_SAMPLE_SIZE);
    double delta = strikePointNorm - cop;
    return Mth.clamp(1.0 - IMPACT_EFFICIENCY_SENSITIVITY * (delta * delta), IMPACT_EFFICIENCY_MIN, IMPACT_EFFICIENCY_MAX);
  }

  private static double slashDeliveryFactor(WeaponAssembly weapon) {
    double baseFactor = deliveryFactor(
      weapon,
      SLASH_CONTEXT,
      (balanceMismatch, delta) -> Math.max(0.0, delta),
      SLASH_DELIVERY_BALANCE_MISMATCH_WEIGHT,
      SLASH_DELIVERY_FORWARD_BIAS_WEIGHT,
      SLASH_DELIVERY_INERTIA_WEIGHT,
      SLASH_DELIVERY_EFFECTIVE_MASS_WEIGHT,
      SLASH_DELIVERY_FACTOR_MIN,
      SLASH_DELIVERY_FACTOR_MAX);
    double strikePointNorm = weapon.normalizedStrikePointForAttackType(SLASH_CONTEXT, WeaponAssembly.LARGE_SAMPLE_SIZE);
    double pointOfBalanceNorm = weapon.normalizedPointOfBalance(WeaponAssembly.LARGE_SAMPLE_SIZE);
    double leveragePenalty = Math.max(0.0, strikePointNorm - pointOfBalanceNorm - SLASH_DELIVERY_LEVERAGE_THRESHOLD);
    return Mth.clamp(baseFactor - SLASH_DELIVERY_LEVERAGE_WEIGHT * leveragePenalty, SLASH_DELIVERY_FACTOR_MIN, SLASH_DELIVERY_FACTOR_MAX);
  }

  private static double thrustDeliveryFactor(WeaponAssembly weapon, boolean rotationalThrust) {
    if (!rotationalThrust) {
      return deliveryFactor(
        weapon,
        THRUST_CONTEXT,
        (balanceMismatch, delta) -> 0.65 * Math.max(0.0, delta) + 0.35 * Math.max(0.0, -delta),
        THRUST_LINEAR_DELIVERY_BALANCE_MISMATCH_WEIGHT,
        THRUST_LINEAR_DELIVERY_DIRECTIONAL_WEIGHT,
        THRUST_LINEAR_DELIVERY_INERTIA_WEIGHT,
        THRUST_LINEAR_DELIVERY_EFFECTIVE_MASS_WEIGHT,
        THRUST_LINEAR_DELIVERY_FACTOR_MIN,
        THRUST_LINEAR_DELIVERY_FACTOR_MAX);
    }
    return deliveryFactor(
      weapon,
      THRUST_CONTEXT,
      (balanceMismatch, delta) -> Math.max(0.0, delta),
      THRUST_ROTATIONAL_DELIVERY_BALANCE_MISMATCH_WEIGHT,
      THRUST_ROTATIONAL_DELIVERY_DIRECTIONAL_WEIGHT,
      THRUST_ROTATIONAL_DELIVERY_INERTIA_WEIGHT,
      THRUST_ROTATIONAL_DELIVERY_EFFECTIVE_MASS_WEIGHT,
      THRUST_ROTATIONAL_DELIVERY_FACTOR_MIN,
      THRUST_ROTATIONAL_DELIVERY_FACTOR_MAX);
  }

  private static double strikeDeliveryFactor(WeaponAssembly weapon) {
    return deliveryFactor(
      weapon,
      STRIKE_CONTEXT,
      (actualDelta, directionalDelta) -> Math.max(0.0, -directionalDelta),
      STRIKE_DELIVERY_BALANCE_MISMATCH_WEIGHT,
      STRIKE_DELIVERY_UNDERWEIGHTED_HEAD_WEIGHT,
      STRIKE_DELIVERY_INERTIA_WEIGHT,
      STRIKE_DELIVERY_EFFECTIVE_MASS_WEIGHT,
      STRIKE_DELIVERY_FACTOR_MIN,
      STRIKE_DELIVERY_FACTOR_MAX);
  }

  private static double deliveryFactor(
      WeaponAssembly weapon,
      AttackType attackType,
      DirectionalPenalty directionalPenalty,
      double balanceMismatchWeight,
      double directionalWeight,
      double inertiaWeight,
      double effectiveMassWeight,
      double minFactor,
      double maxFactor) {
    double actualPoBNorm = weapon.normalizedPointOfBalance(WeaponAssembly.LARGE_SAMPLE_SIZE);
    double idealPoBNorm = weapon.normalizedIdealPointOfBalanceForAttackType(attackType);
    double delta = actualPoBNorm - idealPoBNorm;
    double balanceMismatch = Math.abs(delta);
    double directional = directionalPenalty.value(balanceMismatch, delta);
    double effectiveMassRatio = weapon.effectiveMassRatioForAttackType(attackType, WeaponAssembly.LARGE_SAMPLE_SIZE);
    double inertiaRatio = normalizedInertiaRatio(weapon);

    double factor = 1.0
      - balanceMismatchWeight * balanceMismatch
      - directionalWeight * directional
      - inertiaWeight * inertiaRatio
      + effectiveMassWeight * (effectiveMassRatio - 0.5);
    return Mth.clamp(factor, minFactor, maxFactor);
  }

  private static double normalizedInertiaRatio(WeaponAssembly weapon) {
    double mass = Math.max(1.0, weapon.mass(WeaponAssembly.LARGE_SAMPLE_SIZE));
    double length = Math.max(1.0, weapon.length());
    double denominator = Math.max(1.0e-6, mass * length * length);
    double inertia = Math.max(0.0, weapon.momentOfInertiaAboutBase(WeaponAxis.Z, WeaponAssembly.LARGE_SAMPLE_SIZE));
    return Mth.clamp(inertia / denominator, 0.0, 1.0);
  }

  private static double realizedLocalFactor(double localPotential, double deliveryFactor) {
    double clampedPotential = Mth.clamp(localPotential, LOCAL_FACTOR_MIN, LOCAL_FACTOR_MAX);
    return 1.0 + (clampedPotential - 1.0) * Mth.clamp(deliveryFactor, 0.0, 1.0);
  }

  @FunctionalInterface
  private interface DirectionalPenalty {
    double value(double balanceMismatch, double delta);
  }

}


