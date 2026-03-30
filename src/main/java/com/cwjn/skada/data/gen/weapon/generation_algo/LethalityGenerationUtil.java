package com.cwjn.skada.data.gen.weapon.generation_algo;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackDeliverySnapshot;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextFactory;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AssemblyPhysicsSnapshot;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.MaterialResponseSnapshot;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextSlash;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.ContactSnapshotSlash;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextStrike;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.ContactSnapshotStrike;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextThrust;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.ContactSnapshotThrust;

import net.minecraft.util.Mth;
import com.cwjn.skada.util.Util;

public abstract class LethalityGenerationUtil {

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
    return slash(AttackGenerationContextFactory.buildSlashContext(weapon));
  }

  public static double slash(AttackGenerationContextSlash context) {
    AssemblyPhysicsSnapshot assembly = context.assembly();
    ContactSnapshotSlash contact = context.contact();
    AttackDeliverySnapshot delivery = context.delivery();
    MaterialResponseSnapshot material = context.material();

    double momentOfInertia = assembly.momentOfInertiaBaseZKgM2();
    double angularVelocity = PhysicsUtil.angularVelocity(momentOfInertia, SkadaData.PLAYER_STRENGTH);
    double angularMomentum = momentOfInertia * angularVelocity;
    double rotationalKineticEnergy = 0.5 * momentOfInertia * angularVelocity * angularVelocity;
    double baseLethality = slashBaseLethalityFromEnergy(rotationalKineticEnergy);

    double wedgeThicknessCm = contact.wedgeThicknessCmAtCop();
    double bevelAngle = contact.bevelAngleDegAtCop();
    double wedgePotential = SLASH_WEDGE_POTENTIAL_PER_CM * wedgeThicknessCm;
    double requiredMomentum = SLASH_REQUIRED_MOMENTUM_SCALE * wedgeThicknessCm * (1.0 + Math.tan(Math.toRadians(bevelAngle)));
    double driveRatio = angularMomentum / Math.max(NUMERIC_EPSILON, requiredMomentum);
    double wedgeDrive = Mth.clamp(driveRatio, 0.0, 1.0);
    double wedgeBonus = wedgePotential * wedgeDrive;

    double materialFactor = material.slashMaterialFactor();
    double strikePointNorm = delivery.strikePointNorm();
    double impactEfficiency = delivery.impactEfficiency();
    double deliveryFactor = slashDeliveryFactor(context);
    double realizedSpecialization = realizedLocalFactor(contact.specializationPotential(), deliveryFactor);
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
    return thrust(AttackGenerationContextFactory.buildThrustContext(weapon));
  }

  public static double thrust(AttackGenerationContextThrust context) {
    AssemblyPhysicsSnapshot assembly = context.assembly();
    ContactSnapshotThrust contact = context.contact();
    AttackDeliverySnapshot delivery = context.delivery();
    MaterialResponseSnapshot material = context.material();

    double massKg = com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.toKg(assembly.massG());

    double strengthFactor = Math.sqrt(SkadaData.PLAYER_STRENGTH / THRUST_STRENGTH_REFERENCE);
    double massPenalty = Math.pow(Math.max(NUMERIC_EPSILON, massKg) / THRUST_REFERENCE_MASS_KG, THRUST_MASS_PENALTY_EXPONENT);
    double linearVelocity = THRUST_LINEAR_VELOCITY_SCALE * strengthFactor / Math.max(NUMERIC_EPSILON, massPenalty);

    double linearMomentum = massKg * linearVelocity;
    double linearKineticEnergy = 0.5 * massKg * linearVelocity * linearVelocity;
    double baseLethality = thrustBaseLethalityFromEnergy(linearKineticEnergy);

    double wedgeThicknessCm = contact.wedgeThicknessCm();
    double tipLengthCm = contact.tipLengthCm();
    double bevelAngle = contact.bevelAngleDeg();

    double wedgePotential = THRUST_WEDGE_POTENTIAL_PER_CM * wedgeThicknessCm;
    double requiredMomentum = THRUST_REQUIRED_MOMENTUM_SCALE * wedgeThicknessCm * (1.0 + Math.tan(Math.toRadians(bevelAngle)));
    double driveRatio = linearMomentum / Math.max(NUMERIC_EPSILON, requiredMomentum);
    double wedgeDrive = Mth.clamp(driveRatio, 0.0, 1.0);
    double wedgeBonus = wedgePotential * wedgeDrive;

    double materialFactor = material.thrustMaterialFactor();
    boolean rotationalThrust = delivery.rotationalThrust();
    double impactEfficiency = rotationalThrust ? delivery.impactEfficiency() : 1.0;
    double deliveryFactor = thrustDeliveryFactor(context);
    double realizedSpecialization = realizedLocalFactor(contact.specializationPotential(), deliveryFactor);
    double penetrationFactor = contact.penetrationEfficiency();

    double lethality = (baseLethality + wedgeBonus * deliveryFactor) * impactEfficiency * realizedSpecialization * penetrationFactor * materialFactor * deliveryFactor;
    double finalLethality = Util.round(Mth.clamp(lethality, MIN_LETHALITY, MAX_LETHALITY), LETHALITY_ROUND_DECIMALS);
    if (SkadaData.DEBUG_ENABLED) {
      Skada.LOGGER.debug("[GEN][Lethality][thrust] massKg={}, linVel={}, linMomentum={}, linKE={}, wedgeThicknessCm={}, thicknessAtPointBaseCm={}, tipLengthCm={}, bevelDeg={}, wedgePotential={}, requiredMomentum={}, driveRatio={}, wedgeDrive={}, wedgeBonus={}, taperFactor={}, needleFactor={}, tipSlenderness={}, slendernessFactor={}, rotationalThrust={}, impactEfficiency={}, deliveryFactor={}, realizedSpecialization={}, materialFactor={}, final={}",
        massKg, linearVelocity, linearMomentum, linearKineticEnergy, wedgeThicknessCm, contact.thicknessAtPointBaseCm(), tipLengthCm, bevelAngle,
        wedgePotential, requiredMomentum, driveRatio, wedgeDrive, wedgeBonus, contact.taperFactor(), contact.needleFactor(),
        contact.tipSlenderness(), contact.slendernessFactor(), rotationalThrust, impactEfficiency, deliveryFactor, realizedSpecialization,
        materialFactor, finalLethality);
    }
    return finalLethality;
  }

  /**
   * Strike lethality is based entirely on angular momentum.
   */
  public static double strike(WeaponAssembly weapon) {
    return strike(AttackGenerationContextFactory.buildStrikeContext(weapon));
  }

  public static double strike(AttackGenerationContextStrike context) {
    AssemblyPhysicsSnapshot assembly = context.assembly();
    ContactSnapshotStrike contact = context.contact();
    AttackDeliverySnapshot delivery = context.delivery();

    double momentOfInertia = assembly.momentOfInertiaBaseZKgM2();
    double angularVelocity = PhysicsUtil.angularVelocity(momentOfInertia, SkadaData.PLAYER_STRENGTH);
    double angularMomentum = Math.max(0.0, momentOfInertia * angularVelocity);

    double strikePointNorm = delivery.strikePointNorm();
    double impactEfficiency = delivery.impactEfficiency();
    double deliveryFactor = strikeDeliveryFactor(context);
    double localizationFactor = contact.localizationFactor();
    double complianceFactor = strikeComplianceFactor(contact.strikeStructuralEfficiency());
    double incidenceFactor = strikeIncidenceFactor(contact.strikeIncidenceEfficiency());
    double strikeGeometryFactor = contact.strikeGeometryFactor();
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

  private static double slashDeliveryFactor(AttackGenerationContextSlash context) {
    double baseFactor = deliveryFactor(
      context.delivery(),
      context.assembly(),
      delivery -> delivery.forwardOvercommitment(),
      SLASH_DELIVERY_BALANCE_MISMATCH_WEIGHT,
      SLASH_DELIVERY_FORWARD_BIAS_WEIGHT,
      SLASH_DELIVERY_INERTIA_WEIGHT,
      SLASH_DELIVERY_EFFECTIVE_MASS_WEIGHT,
      SLASH_DELIVERY_FACTOR_MIN,
      SLASH_DELIVERY_FACTOR_MAX);
    double leveragePenalty = Math.max(0.0, context.delivery().leverageGap() - SLASH_DELIVERY_LEVERAGE_THRESHOLD);
    return Mth.clamp(baseFactor - SLASH_DELIVERY_LEVERAGE_WEIGHT * leveragePenalty, SLASH_DELIVERY_FACTOR_MIN, SLASH_DELIVERY_FACTOR_MAX);
  }

  private static double thrustDeliveryFactor(AttackGenerationContextThrust context) {
    boolean rotationalThrust = context.delivery().rotationalThrust();
    if (!rotationalThrust) {
      return deliveryFactor(
        context.delivery(),
        context.assembly(),
        delivery -> 0.65 * delivery.forwardOvercommitment() + 0.35 * delivery.rearUnderweight(),
        THRUST_LINEAR_DELIVERY_BALANCE_MISMATCH_WEIGHT,
        THRUST_LINEAR_DELIVERY_DIRECTIONAL_WEIGHT,
        THRUST_LINEAR_DELIVERY_INERTIA_WEIGHT,
        THRUST_LINEAR_DELIVERY_EFFECTIVE_MASS_WEIGHT,
        THRUST_LINEAR_DELIVERY_FACTOR_MIN,
        THRUST_LINEAR_DELIVERY_FACTOR_MAX);
    }
    return deliveryFactor(
      context.delivery(),
      context.assembly(),
      delivery -> delivery.forwardOvercommitment(),
      THRUST_ROTATIONAL_DELIVERY_BALANCE_MISMATCH_WEIGHT,
      THRUST_ROTATIONAL_DELIVERY_DIRECTIONAL_WEIGHT,
      THRUST_ROTATIONAL_DELIVERY_INERTIA_WEIGHT,
      THRUST_ROTATIONAL_DELIVERY_EFFECTIVE_MASS_WEIGHT,
      THRUST_ROTATIONAL_DELIVERY_FACTOR_MIN,
      THRUST_ROTATIONAL_DELIVERY_FACTOR_MAX);
  }

  private static double strikeDeliveryFactor(AttackGenerationContextStrike context) {
    return deliveryFactor(
      context.delivery(),
      context.assembly(),
      delivery -> delivery.rearUnderweight(),
      STRIKE_DELIVERY_BALANCE_MISMATCH_WEIGHT,
      STRIKE_DELIVERY_UNDERWEIGHTED_HEAD_WEIGHT,
      STRIKE_DELIVERY_INERTIA_WEIGHT,
      STRIKE_DELIVERY_EFFECTIVE_MASS_WEIGHT,
      STRIKE_DELIVERY_FACTOR_MIN,
      STRIKE_DELIVERY_FACTOR_MAX);
  }

  private static double deliveryFactor(
      AttackDeliverySnapshot delivery,
      AssemblyPhysicsSnapshot assembly,
      DirectionalPenalty directionalPenalty,
      double balanceMismatchWeight,
      double directionalWeight,
      double inertiaWeight,
      double effectiveMassWeight,
      double minFactor,
      double maxFactor) {
    double balanceMismatch = delivery.balanceMismatch();
    double directional = directionalPenalty.value(delivery);
    double effectiveMassRatio = delivery.effectiveMassRatio();
    double inertiaCoefficient = assembly.normalizedInertiaCoefficient();

    double factor = 1.0
      - balanceMismatchWeight * balanceMismatch
      - directionalWeight * directional
      - inertiaWeight * inertiaCoefficient
      + effectiveMassWeight * (effectiveMassRatio - 0.5);
    return Mth.clamp(factor, minFactor, maxFactor);
  }

  private static double realizedLocalFactor(double localPotential, double deliveryFactor) {
    double clampedPotential = Mth.clamp(localPotential, LOCAL_FACTOR_MIN, LOCAL_FACTOR_MAX);
    return 1.0 + (clampedPotential - 1.0) * Mth.clamp(deliveryFactor, 0.0, 1.0);
  }

  @FunctionalInterface
  private interface DirectionalPenalty {
    double value(AttackDeliverySnapshot delivery);
  }

}


