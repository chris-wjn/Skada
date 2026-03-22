package com.cwjn.skada.data.gen.weapon.generation_algo.context;

import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;

import net.minecraft.util.Mth;

/**
 * Slash-contact snapshot derived from a slash-capable part.
 *
 * <p>It stores the local edge geometry that matters to slash-oriented
 * generation formulas.
 */
public final class ContactSnapshotSlash {

  private static final double NUMERIC_EPSILON = 1.0e-6;

  private final double wedgeThicknessCmAtCop;
  private final double bevelAngleDegAtCop;
  private final double edgeRadiusNm;
  private final double bevelAcuity;
  private final double wedgeThinness;
  private final double edgeAcuity;
  private final double specializationPotential;

  /**
   * Creates a slash contact snapshot.
   *
   * @param wedgeThicknessCmAtCop wedge thickness at the center of percussion
   * @param bevelAngleDegAtCop bevel angle at the center of percussion
   * @param edgeRadiusNm edge radius in nanometers
   * @param bevelAcuity normalized bevel acuity factor
   * @param wedgeThinness normalized wedge thinness factor
   * @param edgeAcuity normalized edge acuity factor
   * @param specializationPotential combined specialization potential for slash lethality
   */
  private ContactSnapshotSlash(
      double wedgeThicknessCmAtCop,
      double bevelAngleDegAtCop,
      double edgeRadiusNm,
      double bevelAcuity,
      double wedgeThinness,
      double edgeAcuity,
      double specializationPotential) {
    this.wedgeThicknessCmAtCop = wedgeThicknessCmAtCop;
    this.bevelAngleDegAtCop = bevelAngleDegAtCop;
    this.edgeRadiusNm = edgeRadiusNm;
    this.bevelAcuity = bevelAcuity;
    this.wedgeThinness = wedgeThinness;
    this.edgeAcuity = edgeAcuity;
    this.specializationPotential = specializationPotential;
  }

  /**
   * Builds a slash contact snapshot from a slash-capable weapon part.
   *
   * @param slashCapable slash-capable part implementation
   * @param centreOfPercussionNorm normalized center of percussion for the weapon
   * @return immutable slash contact snapshot
   */
  public static ContactSnapshotSlash fromPart(SlashCapable slashCapable, double centreOfPercussionNorm) {
    double wedgeThicknessCmAtCop = Math.max(NUMERIC_EPSILON, slashCapable.wedgeThicknessCmAt(centreOfPercussionNorm));
    double bevelAngleDegAtCop = Math.max(NUMERIC_EPSILON, slashCapable.edgeAngleDegreesAt(centreOfPercussionNorm));
    double edgeRadiusNm = Math.max(NUMERIC_EPSILON, slashCapable.edgeRadiusNm());
    double bevelAcuity = Mth.clamp(18.0 / Math.max(NUMERIC_EPSILON, bevelAngleDegAtCop), 0.35, 1.15);
    double wedgeThinness = Mth.clamp(0.35 / Math.max(NUMERIC_EPSILON, wedgeThicknessCmAtCop), 0.45, 1.20);
    double edgeAcuity = Mth.clamp(10.0 / edgeRadiusNm, 0.45, 1.25);
    double specializationPotential = Mth.clamp(0.55 * bevelAcuity + 0.25 * wedgeThinness + 0.20 * edgeAcuity, 0.40, 1.25);

    return new ContactSnapshotSlash(
      wedgeThicknessCmAtCop,
      bevelAngleDegAtCop,
      edgeRadiusNm,
      bevelAcuity,
      wedgeThinness,
      edgeAcuity,
      specializationPotential);
  }

  public double wedgeThicknessCmAtCop() {
    return wedgeThicknessCmAtCop;
  }

  public double bevelAngleDegAtCop() {
    return bevelAngleDegAtCop;
  }

  public double edgeRadiusNm() {
    return edgeRadiusNm;
  }

  public double bevelAcuity() {
    return bevelAcuity;
  }

  public double wedgeThinness() {
    return wedgeThinness;
  }

  public double edgeAcuity() {
    return edgeAcuity;
  }

  public double specializationPotential() {
    return specializationPotential;
  }

}