package com.cwjn.skada.generation.armour;

/** Physical mass estimate for a covered armour piece. Lengths use millimetres and areas cm^2. */
public final class ArmourMassUtil {
    private ArmourMassUtil() {}

    /**
     * Estimates mass using one material density for shell and padding. Each physical shell layer
     * contributes its full thickness; padding contributes one additional layer of the same density.
     */
    public static double estimateKg(double densityGPerCm3, double thicknessMm, int layerCount,
                                    double paddingMm, double coverageAreaCm2) {
        if (!Double.isFinite(densityGPerCm3) || densityGPerCm3 <= 0.0
                || !Double.isFinite(thicknessMm) || thicknessMm < 0.0
                || !Double.isFinite(paddingMm) || paddingMm < 0.0
                || !Double.isFinite(coverageAreaCm2) || coverageAreaCm2 < 0.0 || layerCount < 0) {
            return 0.0;
        }
        double totalThicknessCm = (thicknessMm * layerCount + paddingMm) / 10.0;
        double massKg = densityGPerCm3 * totalThicknessCm * coverageAreaCm2 / 1000.0;
        return Double.isFinite(massKg) ? massKg : 0.0;
    }
}
