package com.cwjn.skada.data.gen.weapon.generation_algo.context;

import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;

import net.minecraft.util.Mth;

/**
 * Immutable snapshot of attack-specific delivery behavior derived from an assembled weapon.
 *
 * <p>Delivery descriptors separate the whole-weapon physics from the attack
 * interpretation used by the generators.
 */
public final class AttackDeliverySnapshot {

  private static final double NUMERIC_EPSILON = 1.0e-6;
  private static final double IMPACT_EFFICIENCY_SENSITIVITY = 3.0;
  private static final double IMPACT_EFFICIENCY_MIN = 0.45;
  private static final double IMPACT_EFFICIENCY_MAX = 1.0;

  private final AttackType attackType;
  private final double actualPointOfBalanceNorm;
  private final double actualPointOfBalanceCm;
  private final double strikePointNorm;
  private final double strikePointCm;
  private final double idealPointOfBalanceNorm;
  private final double idealPointOfBalanceCm;
  private final double pointOfBalanceDelta;
  private final double balanceMismatch;
  private final double forwardOvercommitment;
  private final double rearUnderweight;
  private final double leverageGap;
  private final double copStrikeDelta;
  private final double effectiveMassRatio;
  private final boolean rotationalThrust;
  private final double impactEfficiency;

  /**
   * Creates a new attack-delivery snapshot.
   *
   * @param attackType attack type used to derive the delivery descriptors
   * @param actualPointOfBalanceNorm actual point of balance normalized to weapon length
   * @param actualPointOfBalanceCm actual point of balance in centimeters
   * @param strikePointNorm normalized strike point for the attack
   * @param strikePointCm strike point in centimeters
   * @param idealPointOfBalanceNorm ideal point of balance for the attack
   * @param idealPointOfBalanceCm ideal point of balance in centimeters
   * @param pointOfBalanceDelta difference between actual and ideal point of balance
   * @param balanceMismatch absolute balance delta
   * @param forwardOvercommitment forward bias above the ideal point of balance
   * @param rearUnderweight rearward bias below the ideal point of balance
   * @param leverageGap difference between strike point and point of balance
   * @param copStrikeDelta difference between strike point and center of percussion
   * @param effectiveMassRatio attack-specific effective mass ratio
   * @param rotationalThrust whether the thrust behaves as a rotational motion mode
   * @param impactEfficiency impact efficiency derived from CoP alignment
   */
  private AttackDeliverySnapshot(
      AttackType attackType,
      double actualPointOfBalanceNorm,
      double actualPointOfBalanceCm,
      double strikePointNorm,
      double strikePointCm,
      double idealPointOfBalanceNorm,
      double idealPointOfBalanceCm,
      double pointOfBalanceDelta,
      double balanceMismatch,
      double forwardOvercommitment,
      double rearUnderweight,
      double leverageGap,
      double copStrikeDelta,
      double effectiveMassRatio,
      boolean rotationalThrust,
      double impactEfficiency) {
    this.attackType = attackType;
    this.actualPointOfBalanceNorm = actualPointOfBalanceNorm;
    this.actualPointOfBalanceCm = actualPointOfBalanceCm;
    this.strikePointNorm = strikePointNorm;
    this.strikePointCm = strikePointCm;
    this.idealPointOfBalanceNorm = idealPointOfBalanceNorm;
    this.idealPointOfBalanceCm = idealPointOfBalanceCm;
    this.pointOfBalanceDelta = pointOfBalanceDelta;
    this.balanceMismatch = balanceMismatch;
    this.forwardOvercommitment = forwardOvercommitment;
    this.rearUnderweight = rearUnderweight;
    this.leverageGap = leverageGap;
    this.copStrikeDelta = copStrikeDelta;
    this.effectiveMassRatio = effectiveMassRatio;
    this.rotationalThrust = rotationalThrust;
    this.impactEfficiency = impactEfficiency;
  }

  /**
   * Builds an attack-delivery snapshot for the requested attack type.
   *
   * @param weapon weapon assembly being analyzed
   * @param assembly shared assembly-physics snapshot
   * @param attackType attack type being evaluated
   * @param samples geometry sampling hint used by the underlying weapon methods
   * @return immutable delivery snapshot for the attack
   */
  public static AttackDeliverySnapshot fromWeapon(WeaponAssembly weapon, AssemblyPhysicsSnapshot assembly, AttackType attackType, int samples) {
    double lengthCm = Math.max(NUMERIC_EPSILON, assembly.lengthCm());
    double actualPointOfBalanceNorm = assembly.normalizedPointOfBalance();
    double actualPointOfBalanceCm = assembly.pointOfBalanceCm();
    double strikePointNorm = weapon.normalizedStrikePointForAttackType(attackType, samples);
    double strikePointCm = strikePointNorm * lengthCm;
    double idealPointOfBalanceNorm = weapon.normalizedIdealPointOfBalanceForAttackType(attackType);
    double idealPointOfBalanceCm = idealPointOfBalanceNorm * lengthCm;
    double pointOfBalanceDelta = actualPointOfBalanceNorm - idealPointOfBalanceNorm;
    double balanceMismatch = Math.abs(pointOfBalanceDelta);
    double forwardOvercommitment = Math.max(0.0, pointOfBalanceDelta);
    double rearUnderweight = Math.max(0.0, -pointOfBalanceDelta);
    double leverageGap = Math.max(0.0, strikePointNorm - actualPointOfBalanceNorm);
    double copStrikeDelta = strikePointNorm - assembly.centreOfPercussionNorm();
    double effectiveMassRatio = weapon.effectiveMassRatioForAttackType(attackType, samples);
    boolean rotationalThrust = isThrustAttack(attackType) && weapon.isThrustRotational(samples);
    double impactEfficiency = rotationalThrust && isThrustAttack(attackType)
      ? 1.0
      : impactEfficiency(assembly.centreOfPercussionNorm(), strikePointNorm);

    return new AttackDeliverySnapshot(
      attackType,
      actualPointOfBalanceNorm,
      actualPointOfBalanceCm,
      strikePointNorm,
      strikePointCm,
      idealPointOfBalanceNorm,
      idealPointOfBalanceCm,
      pointOfBalanceDelta,
      balanceMismatch,
      forwardOvercommitment,
      rearUnderweight,
      leverageGap,
      copStrikeDelta,
      effectiveMassRatio,
      rotationalThrust,
      impactEfficiency);
  }

  private static boolean isThrustAttack(AttackType attackType) {
    return attackType != null && attackType.capableInterface() != null && ThrustCapable.class.isAssignableFrom(attackType.capableInterface());
  }

  private static double impactEfficiency(double centreOfPercussionNorm, double strikePointNorm) {
    double delta = strikePointNorm - centreOfPercussionNorm;
    return Mth.clamp(1.0 - IMPACT_EFFICIENCY_SENSITIVITY * (delta * delta), IMPACT_EFFICIENCY_MIN, IMPACT_EFFICIENCY_MAX);
  }

  public AttackType attackType() {
    return attackType;
  }

  public double actualPointOfBalanceNorm() {
    return actualPointOfBalanceNorm;
  }

  public double actualPointOfBalanceCm() {
    return actualPointOfBalanceCm;
  }

  public double strikePointNorm() {
    return strikePointNorm;
  }

  public double strikePointCm() {
    return strikePointCm;
  }

  public double idealPointOfBalanceNorm() {
    return idealPointOfBalanceNorm;
  }

  public double idealPointOfBalanceCm() {
    return idealPointOfBalanceCm;
  }

  public double pointOfBalanceDelta() {
    return pointOfBalanceDelta;
  }

  public double balanceMismatch() {
    return balanceMismatch;
  }

  public double forwardOvercommitment() {
    return forwardOvercommitment;
  }

  public double rearUnderweight() {
    return rearUnderweight;
  }

  public double leverageGap() {
    return leverageGap;
  }

  public double copStrikeDelta() {
    return copStrikeDelta;
  }

  public double effectiveMassRatio() {
    return effectiveMassRatio;
  }

  public boolean rotationalThrust() {
    return rotationalThrust;
  }

  public double impactEfficiency() {
    return impactEfficiency;
  }

}