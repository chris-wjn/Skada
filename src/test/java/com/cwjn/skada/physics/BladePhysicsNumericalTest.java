package com.cwjn.skada.physics;

import com.cwjn.skada.data.gen.weapon.WeaponProfile;
import com.cwjn.skada.data.gen.weapon.parts.Blade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Numerical integration baseline tests for complex blades (bevels, fullers).
 * We build a high-resolution composite Simpson baseline using the same geometric
 * definitions (bevel factor Kbase, modified superellipse coefficient, fuller area/inertia),
 * but an independent integration path to cross-check Blade implementations.
 */
public class BladePhysicsNumericalTest {

    private static final double STEEL_DENSITY = 7.85; // g/cm^3

    // ------------------------ Helpers (replicate Blade-side math) ------------------------

    private static double normalizePercentKey(double key) {
        return key > 1.5 ? key / 100.0 : key;
    }

    private static TreeMap<Double, Double> buildMap(Map<Double, Double> src, double startValue, double endValue) {
        TreeMap<Double, Double> map = new TreeMap<>();
        map.put(0.0, startValue);
        map.put(1.0, endValue);
        if (src != null) {
            for (Map.Entry<Double, Double> e : src.entrySet()) {
                map.put(normalizePercentKey(e.getKey()), e.getValue());
            }
        }
        return map;
    }

    private static double interp(NavigableMap<Double, Double> map, double p) {
        if (map.containsKey(p)) return map.get(p);
        Map.Entry<Double, Double> floor = map.floorEntry(p);
        Map.Entry<Double, Double> ceil = map.ceilingEntry(p);
        if (floor == null && ceil == null) return 0.0;
        if (floor == null) return ceil.getValue();
        if (ceil == null) return floor.getValue();
        double x0 = floor.getKey(), x1 = ceil.getKey();
        double y0 = floor.getValue(), y1 = ceil.getValue();
        if (Double.compare(x1, x0) == 0) return y0;
        double t = (p - x0) / (x1 - x0);
        return y0 + t * (y1 - y0);
    }

    private static double superEllipseCoeff(double r) {
        if (r <= 0.0) return 0.0;
        return r / (r + 1.0);
    }

    // Shape factor k = I / (W^3 * T) for composite cross-section (duplicate of Blade logic)
    private static double inertiaShapeFactor(double p, double r) {
        if (p <= 0.0) return 1.0 / 12.0;
        if (p >= 1.0) return r / (12.0 * (r + 3.0));
        double x0 = 1.0 - p; // half-width of flat part when W normalized to 2
        double a = p;        // width of one bevel
        double I_flat = (2.0/3.0) * Math.pow(x0, 3);
        double term1 = Math.pow(a, 3) * r / (3.0 * (r + 3.0));
        double term2 = x0 * Math.pow(a, 2) * r / (r + 2.0);
        double term3 = Math.pow(x0, 2) * a * r / (r + 1.0);
        double I_bevel = term1 + term2 + term3;
        return (I_flat + 2.0 * I_bevel) / 8.0;
    }

    private static double circleSegmentArea(double c, double h) {
        if (c <= 0 || h <= 0) return 0.0;
        double c2 = c * c;
        double h2 = h * h;
        double first = Math.pow((c2 + 4*h2)/(8*h), 2);
        double second = Math.acos((c2 - 4*h2) / (c2 + 4*h2));
        double third = (c/(16*h)) * (c2 - 4*h2);
        double area = first * second - third;
        return Double.isFinite(area) && area > 0 ? area : 0.0;
    }

    private static double circleSegmentInertiaCentroid(double c, double h) {
        if (c <= 0 || h <= 0) return 0.0;
        double cSq = c * c;
        double hSq = h * h;
        double R = (cSq + 4 * hSq) / (8 * h);
        if (R <= 0.0) return 0.0;
        double sinTerm = c / (2 * R);
        sinTerm = Math.max(-1.0, Math.min(1.0, sinTerm));
        double theta = 2 * Math.asin(sinTerm);
        double sinTheta = Math.sin(theta);
        double A = R * R * (theta - sinTheta) / 2.0;
        if (A <= 0.0) return 0.0;
        double sinHalf = Math.sin(theta / 2.0);
        double denom = theta - sinTheta;
        if (Math.abs(denom) < 1e-12) return 0.0;
        double d_c = (4 * R * sinHalf * sinHalf * sinHalf) / (3 * denom);
        double I_chord = Math.pow(R, 4) * (theta - sinTheta) / 8.0;
        double I_centroid = I_chord - A * d_c * d_c;
        return Double.isFinite(I_centroid) && I_centroid > 0 ? I_centroid : 0.0;
    }

    private static double localThickness(double t, Blade.Bevel bevel, boolean singleEdged, double widthFraction) {
        double p = bevel.percentageOfBladeWidth();
        double r = bevel.curveFactor();
        if (singleEdged) {
            if (widthFraction <= (1.0 - p)) return t;
            double u = (widthFraction - (1.0 - p)) / p;
            if (u >= 1.0) return 0.0;
            return t * (1.0 - Math.pow(u, r));
        } else {
            double distFromCenter = Math.abs(widthFraction - 0.5) * 2.0;
            double flat = 1.0 - p;
            if (distFromCenter <= flat) return t;
            double u = (distFromCenter - flat) / p;
            if (u >= 1.0) return 0.0;
            return t * (1.0 - Math.pow(u, r));
        }
    }

    private static double distAxisSq(double dPivot, double s, WeaponProfile.HeadOrientation orientation) {
        if (orientation == WeaponProfile.HeadOrientation.PARALLEL) {
            return Math.pow(dPivot + s, 2);
        } else {
            return dPivot * dPivot + s * s;
        }
    }

    // Composite Simpson over p in [0,1] with N even subintervals
    private static double simpsonIntegrate(int N, java.util.function.DoubleFunction<Double> f) {
        if (N % 2 == 1) N++;
        double h = 1.0 / N;
        double sum = f.apply(0.0) + f.apply(1.0);
        double odd = 0.0, even = 0.0;
        for (int i = 1; i < N; i++) {
            double x = i * h;
            double fx = f.apply(x);
            if ((i & 1) == 1) odd += fx; else even += fx;
        }
        return (h / 3.0) * (sum + 4.0 * odd + 2.0 * even);
    }

    // ------------------------ Baselines ------------------------

    private static class BladeParams {
        double L, W0, W1, T0, T1; // base/tip widths and thicknesses
        TreeMap<Double, Double> wMap, tMap;
        Blade.Bevel bevel;
        Blade.Fuller fuller;
        boolean singleEdged;
        BladeParams(double L, double W0, double W1, Map<Double, Double> wPts,
                    double T0, double T1, Map<Double, Double> tPts,
                    Blade.Bevel bevel, Blade.Fuller fuller, boolean singleEdged) {
            this.L = L; this.W0 = W0; this.W1 = W1; this.T0 = T0; this.T1 = T1;
            this.wMap = buildMap(wPts, W0, W1);
            this.tMap = buildMap(tPts, T0, T1);
            this.bevel = bevel;
            this.fuller = fuller;
            this.singleEdged = singleEdged;
        }
    }

    private static class Baseline {
        double volume;
        double firstMoment; // for PoB
        double inertiaParallel;
        double inertiaPerp;
    }

    private static Baseline computeBaseline(BladeParams bp, double dPivot, double density) {
        double r = bp.bevel.curveFactor();
        double pBevel = bp.bevel.percentageOfBladeWidth();
        double Kbase = (1.0 - pBevel) + pBevel * superEllipseCoeff(r);
        double kShape = inertiaShapeFactor(pBevel, r);
        double kLocalOverK = kShape / Kbase; // so A * (kLocal W^2) = (W*T*Kbase) * (kShape/Kbase * W^2)

        int N = 32768; // high resolution

        // Volume integral f(p) = L * (W*T*Kbase - FullerArea*Sides)
        double vol = simpsonIntegrate(N, p -> {
            double W = interp(bp.wMap, p);
            double T = interp(bp.tMap, p);
            double A = W * T * Kbase;
            double sub = 0.0;
            if (bp.fuller != null && bp.fuller.sagittaHeightByPercent() > 0 && bp.fuller.chordLengthByPercent() > 0) {
                double chordPct = bp.fuller.chordLengthByPercent();
                double depthPct = bp.fuller.sagittaHeightByPercent();
                int sides = bp.fuller.bothSides() ? 2 : 1;
                double tLocal = localThickness(T, bp.bevel, bp.singleEdged, 0.5);
                double sag = Math.min(depthPct, sides == 2 ? 0.5 : 1.0) * tLocal;
                double c = chordPct * W;
                sub = circleSegmentArea(c, sag) * sides;
            }
            return (A - sub) * bp.L;
        });

        // First moment about base: f(p) = L^2 * p * (A - sub)
        double fm = simpsonIntegrate(N, p -> {
            double W = interp(bp.wMap, p);
            double T = interp(bp.tMap, p);
            double A = W * T * Kbase;
            double sub = 0.0;
            if (bp.fuller != null && bp.fuller.sagittaHeightByPercent() > 0 && bp.fuller.chordLengthByPercent() > 0) {
                double chordPct = bp.fuller.chordLengthByPercent();
                double depthPct = bp.fuller.sagittaHeightByPercent();
                int sides = bp.fuller.bothSides() ? 2 : 1;
                double tLocal = localThickness(T, bp.bevel, bp.singleEdged, 0.5);
                double sag = Math.min(depthPct, sides == 2 ? 0.5 : 1.0) * tLocal;
                double c = chordPct * W;
                sub = circleSegmentArea(c, sag) * sides;
            }
            return (A - sub) * (bp.L * bp.L) * p;
        });

        // Inertia: density * ∫ L * (A*(D^2 + (kShape/Kbase)W^2)) - fuller_inertia
        // Parallel orientation
        double Ipar = density * simpsonIntegrate(N, p -> {
            double s = p * bp.L;
            double W = interp(bp.wMap, p);
            double T = interp(bp.tMap, p);
            double A = W * T * Kbase;
            double D2 = distAxisSq(dPivot, s, WeaponProfile.HeadOrientation.PARALLEL);
            double core = A * (D2 + kLocalOverK * W * W);
            // fuller inertia subtract
            double fullerI = 0.0;
            if (bp.fuller != null && bp.fuller.sagittaHeightByPercent() > 0 && bp.fuller.chordLengthByPercent() > 0) {
                double chordPct = bp.fuller.chordLengthByPercent();
                double depthPct = bp.fuller.sagittaHeightByPercent();
                int sides = bp.fuller.bothSides() ? 2 : 1;
                double tLocal = localThickness(T, bp.bevel, bp.singleEdged, 0.5);
                double sag = Math.min(depthPct, sides == 2 ? 0.5 : 1.0) * tLocal;
                double c = chordPct * W;
                double Aseg = circleSegmentArea(c, sag);
                double Icent = circleSegmentInertiaCentroid(c, sag);
                fullerI = sides * (Icent + Aseg * D2);
            }
            return (core - fullerI) * bp.L;
        });

        // Perpendicular orientation
        double Iperp = density * simpsonIntegrate(N, p -> {
            double s = p * bp.L;
            double W = interp(bp.wMap, p);
            double T = interp(bp.tMap, p);
            double A = W * T * Kbase;
            double D2 = distAxisSq(dPivot, s, WeaponProfile.HeadOrientation.PERPENDICULAR);
            double core = A * (D2 + kLocalOverK * W * W);
            double fullerI = 0.0;
            if (bp.fuller != null && bp.fuller.sagittaHeightByPercent() > 0 && bp.fuller.chordLengthByPercent() > 0) {
                double chordPct = bp.fuller.chordLengthByPercent();
                double depthPct = bp.fuller.sagittaHeightByPercent();
                int sides = bp.fuller.bothSides() ? 2 : 1;
                double tLocal = localThickness(T, bp.bevel, bp.singleEdged, 0.5);
                double sag = Math.min(depthPct, sides == 2 ? 0.5 : 1.0) * tLocal;
                double c = chordPct * W;
                double Aseg = circleSegmentArea(c, sag);
                double Icent = circleSegmentInertiaCentroid(c, sag);
                fullerI = sides * (Icent + Aseg * D2);
            }
            return (core - fullerI) * bp.L;
        });

        Baseline b = new Baseline();
        b.volume = vol;
        b.firstMoment = fm;
        b.inertiaParallel = Ipar;
        b.inertiaPerp = Iperp;
        return b;
    }

    // ------------------------ Tests ------------------------

    @Test
    public void testComplexBlade_BevelAndFuller_ParallelAndPerp() {
        double L = 900.0;
        double W0 = 60.0, W1 = 35.0;
        double T0 = 6.0, T1 = 3.0;
        Map<Double, Double> wPts = Map.of(0.4, 50.0, 0.7, 40.0);
        Map<Double, Double> tPts = Map.of(0.5, 4.5);
        Blade.Bevel bevel = new Blade.Bevel(0.33, 1.3);
        Blade.Fuller fuller = new Blade.Fuller(false, 0.5, 0.25); // single side, 50% chord, 25% thickness depth
        boolean singleEdged = true;

        // Construct the Blade under test with the same parameters
        Blade blade = new Blade(singleEdged, W0, W1, wPts, T0, T1, tPts, L, bevel,
                Blade.EdgeBevel.noBevel(), Blade.TipSpecifications.noTip(), fuller);

        // Compute numerical baseline
        BladeParams params = new BladeParams(L, W0, W1, wPts, T0, T1, tPts, bevel, fuller, singleEdged);
        double dPivot = 120.0;
        Baseline ref = computeBaseline(params, dPivot, STEEL_DENSITY);

        double vol = blade.getVolume();
        double pob = blade.getPointOfBalance();
        double Ipar = blade.getMomentOfInertia(dPivot, STEEL_DENSITY, WeaponProfile.HeadOrientation.PARALLEL);
        double Iperp = blade.getMomentOfInertia(dPivot, STEEL_DENSITY, WeaponProfile.HeadOrientation.PERPENDICULAR);

        // Tolerances:
        // - Volume: relative 2e-4 (0.02% - accounts for adaptive vs fixed integration)
        // - PoB: relative 5e-4 (0.05%)
        // - Inertia: relative 1e-4 (0.01%)
        Assertions.assertEquals(ref.volume, vol, Math.max(1e-6, Math.abs(ref.volume) * 2e-4), "Volume baseline mismatch");
        Assertions.assertEquals(ref.firstMoment / ref.volume, pob, Math.max(1e-5, L * 5e-4), "PoB baseline mismatch");
        Assertions.assertEquals(ref.inertiaParallel, Ipar, Math.max(1e-3, Math.abs(ref.inertiaParallel) * 1e-4), "Inertia (parallel) baseline mismatch");
        Assertions.assertEquals(ref.inertiaPerp, Iperp, Math.max(1e-3, Math.abs(ref.inertiaPerp) * 1e-4), "Inertia (perp) baseline mismatch");
    }

    @Test
    public void testComplexBlade_DoubleFuller_Symmetric() {
        double L = 800.0;
        double W0 = 70.0, W1 = 45.0;
        double T0 = 7.0, T1 = 4.0;
        Map<Double, Double> wPts = Map.of(0.25, 55.0, 0.85, 48.0);
        Map<Double, Double> tPts = Map.of(0.5, 5.6);
        Blade.Bevel bevel = new Blade.Bevel(0.25, 1.8);
        Blade.Fuller fuller = new Blade.Fuller(true, 0.4, 0.2); // both sides
        boolean singleEdged = false;

        Blade blade = new Blade(singleEdged, W0, W1, wPts, T0, T1, tPts, L, bevel,
                Blade.EdgeBevel.noBevel(), Blade.TipSpecifications.noTip(), fuller);

        BladeParams params = new BladeParams(L, W0, W1, wPts, T0, T1, tPts, bevel, fuller, singleEdged);
        double dPivot = 200.0;
        Baseline ref = computeBaseline(params, dPivot, STEEL_DENSITY);

        double vol = blade.getVolume();
        double pob = blade.getPointOfBalance();
        double Ipar = blade.getMomentOfInertia(dPivot, STEEL_DENSITY, WeaponProfile.HeadOrientation.PARALLEL);
        double Iperp = blade.getMomentOfInertia(dPivot, STEEL_DENSITY, WeaponProfile.HeadOrientation.PERPENDICULAR);

        Assertions.assertEquals(ref.volume, vol, Math.max(1e-6, Math.abs(ref.volume) * 2e-4));
        Assertions.assertEquals(ref.firstMoment / ref.volume, pob, Math.max(1e-5, L * 5e-4));
        Assertions.assertEquals(ref.inertiaParallel, Ipar, Math.max(1e-3, Math.abs(ref.inertiaParallel) * 1e-4));
        Assertions.assertEquals(ref.inertiaPerp, Iperp, Math.max(1e-3, Math.abs(ref.inertiaPerp) * 1e-4));
    }

    @Test
    public void propertyTests_Invariants_Monotonicity() {
        // Invariants: adding/deepening a fuller reduces volume and inertia; increasing density scales inertia linearly.
        double L = 850.0;
        double W0 = 65.0, W1 = 42.0;
        double T0 = 6.5, T1 = 3.5;
        Map<Double, Double> wPts = Map.of(0.3, 50.0, 0.6, 45.0);
        Map<Double, Double> tPts = Map.of(0.7, 4.2);
        Blade.Bevel bevel = new Blade.Bevel(0.3, 1.4);
        boolean singleEdged = true;

        Blade.Fuller none = Blade.Fuller.noFuller();
        Blade.Fuller shallow = new Blade.Fuller(false, 0.3, 0.1);
        Blade.Fuller deep = new Blade.Fuller(false, 0.3, 0.25);

        Blade bNone = new Blade(singleEdged, W0, W1, wPts, T0, T1, tPts, L, bevel,
                Blade.EdgeBevel.noBevel(), Blade.TipSpecifications.noTip(), none);
        Blade bShallow = new Blade(singleEdged, W0, W1, wPts, T0, T1, tPts, L, bevel,
                Blade.EdgeBevel.noBevel(), Blade.TipSpecifications.noTip(), shallow);
        Blade bDeep = new Blade(singleEdged, W0, W1, wPts, T0, T1, tPts, L, bevel,
                Blade.EdgeBevel.noBevel(), Blade.TipSpecifications.noTip(), deep);

        double dPivot = 150.0;
        double rho = STEEL_DENSITY;

        double Vnone = bNone.getVolume();
        double Vshallow = bShallow.getVolume();
        double Vdeep = bDeep.getVolume();
        Assertions.assertTrue(Vnone >= Vshallow && Vshallow >= Vdeep, "Volume must drop with deeper fuller");

        double Inone = bNone.getMomentOfInertia(dPivot, rho, WeaponProfile.HeadOrientation.PARALLEL);
        double Ishallow = bShallow.getMomentOfInertia(dPivot, rho, WeaponProfile.HeadOrientation.PARALLEL);
        double Ideep = bDeep.getMomentOfInertia(dPivot, rho, WeaponProfile.HeadOrientation.PARALLEL);
        Assertions.assertTrue(Inone >= Ishallow && Ishallow >= Ideep, "Inertia must drop with deeper fuller");

        // Density scaling
        double rho2 = rho * 1.5;
        double I2 = bNone.getMomentOfInertia(dPivot, rho2, WeaponProfile.HeadOrientation.PARALLEL);
        Assertions.assertEquals(I2, Inone * 1.5, Math.abs(Inone) * 1e-9, "Inertia should scale linearly with density");
    }
}

