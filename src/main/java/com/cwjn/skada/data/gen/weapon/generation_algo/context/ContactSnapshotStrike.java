package com.cwjn.skada.data.gen.weapon.generation_algo.context;

import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;

import net.minecraft.util.Mth;

/**
 * Strike-contact snapshot derived from a strike-capable part.
 *
 * <p>It stores the face geometry and structural descriptors used by the
 * strike-oriented generation formulas.
 */
public final class ContactSnapshotStrike {

  private static final double NUMERIC_EPSILON = 1.0e-6;
  private static final double LOCAL_FACTOR_MIN = 0.75;
  private static final double LOCAL_FACTOR_MAX = 1.35;

  private final double effectiveContactAreaCm2;
  private final double strikeFaceGeometryFocus;
  private final double strikeHeadRigidity;
  private final double strikeAssemblyStability;
  private final double strikeStructuralEfficiency;
  private final double strikeIncidenceEfficiency;
  private final double strikeRepeatability;
  private final double localizationFactor;
  private final double strikeGeometryFactor;

  /**
   * Creates a strike contact snapshot.
   *
   * @param effectiveContactAreaCm2 effective strike contact area in cm^2
   * @param strikeFaceGeometryFocus strike-face focus factor
   * @param strikeHeadRigidity strike-head rigidity factor
   * @param strikeAssemblyStability strike assembly stability factor
   * @param strikeStructuralEfficiency strike structural efficiency factor
   * @param strikeIncidenceEfficiency strike incidence efficiency factor
   * @param strikeRepeatability strike repeatability factor
   * @param localizationFactor normalized localization factor
   * @param strikeGeometryFactor combined strike geometry factor
   */
  private ContactSnapshotStrike(
      double effectiveContactAreaCm2,
      double strikeFaceGeometryFocus,
      double strikeHeadRigidity,
      double strikeAssemblyStability,
      double strikeStructuralEfficiency,
      double strikeIncidenceEfficiency,
      double strikeRepeatability,
      double localizationFactor,
      double strikeGeometryFactor) {
    this.effectiveContactAreaCm2 = effectiveContactAreaCm2;
    this.strikeFaceGeometryFocus = strikeFaceGeometryFocus;
    this.strikeHeadRigidity = strikeHeadRigidity;
    this.strikeAssemblyStability = strikeAssemblyStability;
    this.strikeStructuralEfficiency = strikeStructuralEfficiency;
    this.strikeIncidenceEfficiency = strikeIncidenceEfficiency;
    this.strikeRepeatability = strikeRepeatability;
    this.localizationFactor = localizationFactor;
    this.strikeGeometryFactor = strikeGeometryFactor;
  }

  /**
   * Builds a strike contact snapshot from a strike-capable weapon part.
   *
   * @param strikeCapable strike-capable part implementation
   * @return immutable strike contact snapshot
   */
  public static ContactSnapshotStrike fromPart(StrikeCapable strikeCapable) {
    double effectiveContactAreaCm2 = Math.max(0.02, strikeCapable.effectiveContactAreaCm2());
    double strikeFaceGeometryFocus = Mth.clamp(strikeCapable.strikeFaceGeometryFocus(), 0.75, 1.35);
    double strikeHeadRigidity = Mth.clamp(strikeCapable.strikeHeadRigidity(), 0.65, 1.35);
    double strikeAssemblyStability = Mth.clamp(strikeCapable.strikeAssemblyStability(), 0.65, 1.35);
    double strikeStructuralEfficiency = Mth.clamp(strikeCapable.strikeStructuralEfficiency(), 0.72, 1.08);
    double strikeIncidenceEfficiency = Mth.clamp(strikeCapable.strikeIncidenceEfficiency(), 0.72, 1.05);
    double strikeRepeatability = Mth.clamp(strikeCapable.strikeRepeatability(), 0.70, 1.02);
    double localizationFactor = localizationFactor(effectiveContactAreaCm2, strikeFaceGeometryFocus);
    double strikeGeometryFactor = Mth.clamp(
      0.55 * strikeFaceGeometryFocus + 0.25 * strikeHeadRigidity + 0.20 * strikeAssemblyStability,
      LOCAL_FACTOR_MIN,
      LOCAL_FACTOR_MAX);

    return new ContactSnapshotStrike(
      effectiveContactAreaCm2,
      strikeFaceGeometryFocus,
      strikeHeadRigidity,
      strikeAssemblyStability,
      strikeStructuralEfficiency,
      strikeIncidenceEfficiency,
      strikeRepeatability,
      localizationFactor,
      strikeGeometryFactor);
  }

  private static double localizationFactor(double effectiveContactAreaCm2, double strikeFaceGeometryFocus) {
    double areaFactor = Mth.clamp(
      Math.pow(1.6 / Math.max(NUMERIC_EPSILON, effectiveContactAreaCm2), 0.50),
      0.55,
      1.25);
    double focusFactor = Mth.clamp(strikeFaceGeometryFocus, 0.55, 1.25);
    return Mth.clamp(0.70 * areaFactor + 0.30 * focusFactor, 0.55, 1.25);
  }

  public double effectiveContactAreaCm2() {
    return effectiveContactAreaCm2;
  }

  public double strikeFaceGeometryFocus() {
    return strikeFaceGeometryFocus;
  }

  public double strikeHeadRigidity() {
    return strikeHeadRigidity;
  }

  public double strikeAssemblyStability() {
    return strikeAssemblyStability;
  }

  public double strikeStructuralEfficiency() {
    return strikeStructuralEfficiency;
  }

  public double strikeIncidenceEfficiency() {
    return strikeIncidenceEfficiency;
  }

  public double strikeRepeatability() {
    return strikeRepeatability;
  }

  public double localizationFactor() {
    return localizationFactor;
  }

  public double strikeGeometryFactor() {
    return strikeGeometryFactor;
  }

}