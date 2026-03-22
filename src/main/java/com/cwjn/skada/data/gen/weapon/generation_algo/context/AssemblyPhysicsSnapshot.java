package com.cwjn.skada.data.gen.weapon.generation_algo.context;

import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;

/**
 * Immutable snapshot of whole-weapon physics used by the stat generators.
 *
 * <p>This class keeps the weapon-wide measurements that are shared across attack
 * types, including mass, length, point of balance, center of percussion, and
 * normalized inertia.
 */
public final class AssemblyPhysicsSnapshot {

  private static final double NUMERIC_EPSILON = 1.0e-6;

  private final double massG;
  private final double lengthCm;
  private final double pointOfBalanceCm;
  private final double normalizedPointOfBalance;
  private final double centreOfPercussionNorm;
  private final double momentOfInertiaBaseZKgM2;
  private final double normalizedInertiaCoefficient;

  /**
   * Creates a new snapshot from the supplied weapon measurements.
   *
   * @param massG weapon mass in grams
   * @param lengthCm weapon length in centimeters
   * @param pointOfBalanceCm point of balance in centimeters
   * @param normalizedPointOfBalance point of balance normalized to weapon length
   * @param centreOfPercussionNorm normalized center of percussion
   * @param momentOfInertiaBaseZKgM2 moment of inertia about the base in kg*m^2
  * @param normalizedInertiaCoefficient inertia normalized against mass and length
   */
  private AssemblyPhysicsSnapshot(
      double massG,
      double lengthCm,
      double pointOfBalanceCm,
      double normalizedPointOfBalance,
      double centreOfPercussionNorm,
      double momentOfInertiaBaseZKgM2,
      double normalizedInertiaCoefficient) {
    this.massG = massG;
    this.lengthCm = lengthCm;
    this.pointOfBalanceCm = pointOfBalanceCm;
    this.normalizedPointOfBalance = normalizedPointOfBalance;
    this.centreOfPercussionNorm = centreOfPercussionNorm;
    this.momentOfInertiaBaseZKgM2 = momentOfInertiaBaseZKgM2;
    this.normalizedInertiaCoefficient = normalizedInertiaCoefficient;
  }

  /**
   * Builds an assembly-physics snapshot from a weapon assembly.
   *
   * @param weapon weapon assembly to measure
   * @param samples geometry sampling hint used by the underlying assembly methods
   * @return immutable physics snapshot for the assembly
   */
  public static AssemblyPhysicsSnapshot fromWeapon(WeaponAssembly weapon, int samples) {
    double massG = Math.max(0.0, weapon.mass(samples));
    double lengthCm = Math.max(0.0, weapon.length());
    double pointOfBalanceCm = Math.max(0.0, weapon.pointOfBalance(samples));
    double normalizedPointOfBalance = lengthCm <= NUMERIC_EPSILON ? 0.0 : Math.max(0.0, Math.min(1.0, pointOfBalanceCm / lengthCm));
    double centreOfPercussionNorm = Math.max(0.0, Math.min(1.0, weapon.centreOfPercussion(samples)));
    double momentOfInertiaBaseZGcm2 = Math.max(0.0, weapon.momentOfInertiaAboutBase(WeaponAxis.Z, samples));
    double momentOfInertiaBaseZKgM2 = PhysicsUtil.toKgM2(momentOfInertiaBaseZGcm2);
    double normalizedInertiaCoefficient = normalizedInertiaCoefficient(massG, lengthCm, momentOfInertiaBaseZGcm2);

    return new AssemblyPhysicsSnapshot(
      massG,
      lengthCm,
      pointOfBalanceCm,
      normalizedPointOfBalance,
      centreOfPercussionNorm,
      momentOfInertiaBaseZKgM2,
      normalizedInertiaCoefficient);
  }
  private static double normalizedInertiaCoefficient(double massG, double lengthCm, double momentOfInertiaBaseZGcm2) {
    double mass = Math.max(1.0, massG);
    double length = Math.max(1.0, lengthCm);
    double denominator = Math.max(NUMERIC_EPSILON, mass * length * length);
    return Math.max(0.0, Math.min(1.0, momentOfInertiaBaseZGcm2 / denominator));
  }

  public double massG() {
    return massG;
  }

  public double lengthCm() {
    return lengthCm;
  }

  public double pointOfBalanceCm() {
    return pointOfBalanceCm;
  }

  public double normalizedPointOfBalance() {
    return normalizedPointOfBalance;
  }

  public double centreOfPercussionNorm() {
    return centreOfPercussionNorm;
  }

  public double momentOfInertiaBaseZKgM2() {
    return momentOfInertiaBaseZKgM2;
  }

  public double normalizedInertiaCoefficient() {
    return normalizedInertiaCoefficient;
  }

}