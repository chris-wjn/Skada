package com.cwjn.skada.data.mob;

import java.util.List;

/**
 * Resolves provisional body mass from authored reference weights and normal-pose dimensions.
 * Dimensions are measured in metres and weight is measured in kilograms.
 */
public final class BodyWeightUtil {

  /**
   * Average bulk density used only when an author has not supplied a reference body weight.
   * The estimate intentionally treats the width as both horizontal dimensions; it is a coarse
   * sizing fallback, not a claim about anatomical density.
   */
  public static final double FALLBACK_BULK_DENSITY_KG_PER_M3 = 90.0;

  private BodyWeightUtil() {
  }

  /**
   * Estimates reference mass as width squared times height and the fallback bulk density.
   * Invalid or degenerate dimensions resolve to zero.
   */
  public static double estimateReferenceWeightKg(double widthMeters, double heightMeters) {
    if (!isPositiveFinite(widthMeters) || !isPositiveFinite(heightMeters)) {
      return 0.0;
    }
    return FALLBACK_BULK_DENSITY_KG_PER_M3 * widthMeters * widthMeters * heightMeters;
  }

  /**
   * Scales a reference mass by the relative volume implied by normal-pose dimensions.
   * Width is assumed to scale equally on both horizontal axes. Invalid inputs resolve to zero.
   */
  public static double scaleByBodyVolume(double referenceWeightKg,
                                         double referenceWidthMeters,
                                         double referenceHeightMeters,
                                         double currentWidthMeters,
                                         double currentHeightMeters) {
    if (!isNonNegativeFinite(referenceWeightKg)
        || !isPositiveFinite(referenceWidthMeters)
        || !isPositiveFinite(referenceHeightMeters)
        || !isNonNegativeFinite(currentWidthMeters)
        || !isNonNegativeFinite(currentHeightMeters)) {
      return 0.0;
    }
    double referenceVolume = referenceWidthMeters * referenceWidthMeters * referenceHeightMeters;
    double currentVolume = currentWidthMeters * currentWidthMeters * currentHeightMeters;
    if (!isPositiveFinite(referenceVolume) || !Double.isFinite(currentVolume)) {
      return 0.0;
    }
    return referenceWeightKg * currentVolume / referenceVolume;
  }

  /**
   * Resolves authored or estimated reference mass, then applies the current size ratio.
   * Invalid authored weights are treated as missing; explicit zero is retained.
   */
  public static double resolveWeightKg(Double referenceWeightKg,
                                       double referenceWidthMeters,
                                       double referenceHeightMeters,
                                       double currentWidthMeters,
                                       double currentHeightMeters) {
    double usableReferenceWeight = isNonNegativeFinite(referenceWeightKg)
        ? referenceWeightKg
        : estimateReferenceWeightKg(referenceWidthMeters, referenceHeightMeters);
    return scaleByBodyVolume(usableReferenceWeight,
        referenceWidthMeters, referenceHeightMeters, currentWidthMeters, currentHeightMeters);
  }

  /** Returns a valid non-negative authored reference weight, or {@code null} when it is missing or invalid. */
  public static Double normalizeReferenceWeight(Double referenceWeightKg) {
    return isNonNegativeFinite(referenceWeightKg) ? referenceWeightKg : null;
  }

  /** Preserves an explicit child value, otherwise selects the first valid parent reference. */
  public static Double inheritReferenceWeight(Double ownWeightKg, List<Double> parentWeightsKg) {
    Double own = normalizeReferenceWeight(ownWeightKg);
    if (own != null) {
      return own;
    }
    for (Double parentWeightKg : parentWeightsKg) {
      Double parent = normalizeReferenceWeight(parentWeightKg);
      if (parent != null) {
        return parent;
      }
    }
    return null;
  }

  private static boolean isPositiveFinite(double value) {
    return Double.isFinite(value) && value > 0.0;
  }

  private static boolean isNonNegativeFinite(double value) {
    return Double.isFinite(value) && value >= 0.0;
  }

  private static boolean isNonNegativeFinite(Double value) {
    return value != null && isNonNegativeFinite(value.doubleValue());
  }
}
