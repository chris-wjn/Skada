package com.cwjn.skada.data.gen.weapon.generation_algo.context;

import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;

import net.minecraft.util.Mth;

/**
 * Thrust-contact snapshot derived from a thrust-capable part.
 *
 * <p>It stores the local tip geometry and alignment descriptors that matter to
 * thrust-oriented generation formulas.
 */
public final class ContactSnapshotThrust {

  private static final double NUMERIC_EPSILON = 1.0e-6;

  private final double pointTaper;
  private final double wedgeThicknessCm;
  private final double tipLengthCm;
  private final double tipRadiusNm;
  private final double bevelAngleDeg;
  private final double thicknessAtPointBaseCm;
  private final double tipSectionMeanCm;
  private final double taperFactor;
  private final double needleFactor;
  private final double tipSlenderness;
  private final double slendernessFactor;
  private final double penetrationEfficiency;
  private final double alignmentEfficiency;
  private final double specializationPotential;

  /**
   * Creates a thrust contact snapshot.
   *
   * @param pointTaper normalized point taper
   * @param wedgeThicknessCm width at the point base in centimeters
   * @param tipLengthCm tip length in centimeters
   * @param tipRadiusNm tip radius in nanometers
   * @param bevelAngleDeg implied bevel angle in degrees
   * @param thicknessAtPointBaseCm thickness at the point base in centimeters
   * @param tipSectionMeanCm mean section thickness near the tip
   * @param taperFactor normalized taper factor
   * @param needleFactor normalized needle factor
   * @param tipSlenderness tip slenderness ratio
   * @param slendernessFactor normalized slenderness factor
   * @param penetrationEfficiency normalized penetration efficiency
   * @param alignmentEfficiency normalized alignment efficiency
   * @param specializationPotential combined specialization potential for thrust lethality
   */
  private ContactSnapshotThrust(
      double pointTaper,
      double wedgeThicknessCm,
      double tipLengthCm,
      double tipRadiusNm,
      double bevelAngleDeg,
      double thicknessAtPointBaseCm,
      double tipSectionMeanCm,
      double taperFactor,
      double needleFactor,
      double tipSlenderness,
      double slendernessFactor,
      double penetrationEfficiency,
      double alignmentEfficiency,
      double specializationPotential) {
    this.pointTaper = pointTaper;
    this.wedgeThicknessCm = wedgeThicknessCm;
    this.tipLengthCm = tipLengthCm;
    this.tipRadiusNm = tipRadiusNm;
    this.bevelAngleDeg = bevelAngleDeg;
    this.thicknessAtPointBaseCm = thicknessAtPointBaseCm;
    this.tipSectionMeanCm = tipSectionMeanCm;
    this.taperFactor = taperFactor;
    this.needleFactor = needleFactor;
    this.tipSlenderness = tipSlenderness;
    this.slendernessFactor = slendernessFactor;
    this.penetrationEfficiency = penetrationEfficiency;
    this.alignmentEfficiency = alignmentEfficiency;
    this.specializationPotential = specializationPotential;
  }

  /**
   * Builds a thrust contact snapshot from a thrust-capable weapon part.
   *
   * @param thrustCapable thrust-capable part implementation
   * @return immutable thrust contact snapshot
   */
  public static ContactSnapshotThrust fromPart(ThrustCapable thrustCapable) {
    double pointTaper = Mth.clamp(thrustCapable.pointTaper(), 0.0, 1.0);
    double wedgeThicknessCm = Math.max(NUMERIC_EPSILON, thrustCapable.widthAtPointBase());
    double tipLengthCm = Math.max(NUMERIC_EPSILON, thrustCapable.tipLengthCm());
    double tipRadiusNm = Math.max(NUMERIC_EPSILON, thrustCapable.tipRadiusNm());
    double bevelAngleDeg = Math.toDegrees(Math.atan((wedgeThicknessCm * 0.5) / tipLengthCm));
    double thicknessAtPointBaseCm = Math.max(NUMERIC_EPSILON, thrustCapable.thicknessAtPointBase());
    double tipSectionMeanCm = 0.5 * (wedgeThicknessCm + thicknessAtPointBaseCm);
    double taperFactor = 0.65 + 0.45 * pointTaper;
    double needleFactor = Mth.clamp(0.90 / Math.max(NUMERIC_EPSILON, tipSectionMeanCm), 0.35, 1.40);
    double tipSlenderness = tipLengthCm / Math.max(wedgeThicknessCm, thicknessAtPointBaseCm);
    double slendernessFactor = Mth.clamp((tipSlenderness - 1.5) / 6.5, 0.30, 1.20);
    double penetrationEfficiency = Mth.clamp(thrustCapable.thrustPenetrationEfficiency(), 0.32, 1.05);
    double alignmentEfficiency = thrustCapable.thrustAlignmentEfficiency();
    double specializationPotential = Mth.clamp(0.55 * taperFactor + 0.25 * needleFactor + 0.20 * slendernessFactor, 0.35, 1.20);

    return new ContactSnapshotThrust(
      pointTaper,
      wedgeThicknessCm,
      tipLengthCm,
      tipRadiusNm,
      bevelAngleDeg,
      thicknessAtPointBaseCm,
      tipSectionMeanCm,
      taperFactor,
      needleFactor,
      tipSlenderness,
      slendernessFactor,
      penetrationEfficiency,
      alignmentEfficiency,
      specializationPotential);
  }

  public double pointTaper() {
    return pointTaper;
  }

  public double wedgeThicknessCm() {
    return wedgeThicknessCm;
  }

  public double tipLengthCm() {
    return tipLengthCm;
  }

  public double tipRadiusNm() {
    return tipRadiusNm;
  }

  public double bevelAngleDeg() {
    return bevelAngleDeg;
  }

  public double thicknessAtPointBaseCm() {
    return thicknessAtPointBaseCm;
  }

  public double tipSectionMeanCm() {
    return tipSectionMeanCm;
  }

  public double taperFactor() {
    return taperFactor;
  }

  public double needleFactor() {
    return needleFactor;
  }

  public double tipSlenderness() {
    return tipSlenderness;
  }

  public double slendernessFactor() {
    return slendernessFactor;
  }

  public double penetrationEfficiency() {
    return penetrationEfficiency;
  }

  public double alignmentEfficiency() {
    return alignmentEfficiency;
  }

  public double specializationPotential() {
    return specializationPotential;
  }

}