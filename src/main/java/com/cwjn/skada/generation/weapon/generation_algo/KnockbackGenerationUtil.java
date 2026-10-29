package com.cwjn.skada.generation.weapon.generation_algo;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.generation.weapon.WeaponAssembly;
import com.cwjn.skada.generation.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.generation.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.generation.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.generation.weapon.generation_algo.context.AssemblyPhysicsSnapshot;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackDeliverySnapshot;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextFactory;
import com.cwjn.skada.generation.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.generation.weapon.util.PhysicsUtil;
import net.minecraft.util.Mth;

/** Estimates source-side transferred impulse for a generated attack. Constants remain provisional until ticket 04. */
public final class KnockbackGenerationUtil {

  public static final double MIN_TRANSFER_FACTOR = 0.20;
  public static final double MAX_TRANSFER_FACTOR = 0.85;
  public static final double MAX_ESTIMATED_IMPULSE_NS = 32.0;
  public static final double KNOCKBACK_PER_NEWTON_SECOND = 0.25;
  public static final double MAX_ATTACK_KNOCKBACK = 2.0;
  private static final double STRENGTH_REFERENCE = 50.0;
  private static final double THRUST_VELOCITY_SCALE = 3.8;
  private static final double THRUST_MASS_EXPONENT = 0.32;
  private static final int SAMPLES = WeaponAssembly.LARGE_SAMPLE_SIZE;

  private KnockbackGenerationUtil() {}

  /** Intermediate physical inputs and the provisional vanilla-scale mapping for author review. */
  public record Estimate(double effectiveMassKg, double contactSpeedMps, double transferFactor,
                         double transferredImpulseNs, double attackKnockback) {}

  /** Builds a source estimate from an attack's weapon mass, delivery mode and contact geometry. */
  public static Estimate estimate(WeaponAssembly weapon, AttackType attackType) {
    AssemblyPhysicsSnapshot assembly = AttackGenerationContextFactory.buildAssemblyPhysicsSnapshot(weapon);
    AttackDeliverySnapshot delivery = AttackGenerationContextFactory.buildDeliverySnapshot(weapon, assembly, attackType);
    double massKg = PhysicsUtil.toKg(assembly.massG());
    double effectiveMassKg = massKg * finiteNonNegative(delivery.effectiveMassRatio());
    boolean thrust = capable(attackType, ThrustCapable.class);
    boolean rotationalThrust = thrust && delivery.rotationalThrust();
    double contactRadiusM = delivery.strikePointCm() * 0.01;
    double contactSpeedMps;
    if (thrust && !rotationalThrust) {
      double massPenalty = Math.pow(Math.max(1.0e-6, massKg), THRUST_MASS_EXPONENT);
      contactSpeedMps = THRUST_VELOCITY_SCALE * Math.sqrt(SkadaData.PLAYER_STRENGTH / STRENGTH_REFERENCE)
          / massPenalty;
    } else {
      double angularSpeed = PhysicsUtil.angularVelocity(assembly.momentOfInertiaBaseZKgM2(), SkadaData.PLAYER_STRENGTH);
      contactSpeedMps = angularSpeed * contactRadiusM;
    }
    double transferFactor = contactTransferFactor(weapon, attackType);
    return fromPhysicalInputs(effectiveMassKg, contactSpeedMps, transferFactor);
  }

  /** Maps non-negative SI inputs to a bounded provisional vanilla attack-knockback value. */
  public static Estimate fromPhysicalInputs(double effectiveMassKg, double contactSpeedMps, double transferFactor) {
    double mass = finiteNonNegative(effectiveMassKg);
    double speed = finiteNonNegative(contactSpeedMps);
    double transfer = Mth.clamp(Double.isFinite(transferFactor) ? transferFactor : MIN_TRANSFER_FACTOR,
        MIN_TRANSFER_FACTOR, MAX_TRANSFER_FACTOR);
    double impulse = Math.min(MAX_ESTIMATED_IMPULSE_NS, mass * speed * transfer);
    if (!Double.isFinite(impulse)) impulse = MAX_ESTIMATED_IMPULSE_NS;
    double knockback = Math.min(MAX_ATTACK_KNOCKBACK, impulse * KNOCKBACK_PER_NEWTON_SECOND);
    return new Estimate(mass, speed, transfer, impulse, knockback);
  }

  /** Estimates the bounded momentum-transfer tendency from the selected attack's contact geometry. */
  private static double contactTransferFactor(WeaponAssembly weapon, AttackType attackType) {
    WeaponPartEntry primary = weapon.primaryPartForAttackType(attackType).orElse(null);
    if (primary == null) return 0.55;
    Object part = primary.part();
    double factor;
    if (capable(attackType, SlashCapable.class) && part instanceof SlashCapable slash) {
      double point = weapon.normalizedStrikePointForAttackType(attackType, SAMPLES);
      double edgeRadius = Mth.clamp(slash.edgeRadiusNm() / 100.0, 0.0, 1.0);
      double wedge = Mth.clamp(slash.wedgeThicknessCmAt(point) / 0.6, 0.0, 1.0);
      factor = 0.28 + 0.25 * edgeRadius + 0.24 * wedge;
    } else if (capable(attackType, ThrustCapable.class) && part instanceof ThrustCapable thrust) {
      double tipRadius = Mth.clamp(thrust.tipRadiusNm() / 100.0, 0.0, 1.0);
      double pointWidth = Mth.clamp(thrust.widthAtPointBase() / 1.0, 0.0, 1.0);
      double tipLength = Mth.clamp(thrust.tipLengthCm() / 8.0, 0.0, 1.0);
      factor = 0.24 + 0.20 * tipRadius + 0.23 * pointWidth + 0.18 * tipLength;
    } else if (capable(attackType, StrikeCapable.class) && part instanceof StrikeCapable strike) {
      double area = Mth.clamp(strike.effectiveContactAreaCm2() / 20.0, 0.0, 1.0);
      double focus = Mth.clamp(strike.strikeFaceGeometryFocus(), 0.75, 2.0);
      factor = 0.28 + 0.42 * area + 0.12 * Mth.clamp(1.0 / focus, 0.0, 1.0);
    } else {
      factor = 0.55;
    }
    return Mth.clamp(factor, MIN_TRANSFER_FACTOR, MAX_TRANSFER_FACTOR);
  }

  /** Returns whether the registered attack type is built for the requested geometry capability. */
  private static boolean capable(AttackType attackType, Class<?> capability) {
    return attackType.capableInterface() != null && capability.isAssignableFrom(attackType.capableInterface());
  }

  /** Converts invalid or negative physics inputs to zero before applying the estimate. */
  private static double finiteNonNegative(double value) {
    return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
  }
}
