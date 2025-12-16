package com.cwjn.skada.physics;

import com.cwjn.skada.data.gen.weapon.WeaponProfile;
import com.cwjn.skada.data.gen.weapon.parts.Blade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Rigorous tests for Blade physics methods (volume, point of balance, inertia)
 * using cases with known analytical solutions.
 */
public class BladePhysicsTest {

    private static final double STEEL_DENSITY = 7.85; // g/cm^3

    private Blade makeRectBlade(double length, double width, double thickness) {
        return new Blade(
                false,
                width, width, Map.of(),
                thickness, thickness, Map.of(),
                length,
                new Blade.Bevel(0.0, 1.0), // no bevel: p=0 -> Kbase = 1
                Blade.EdgeBevel.noBevel(),
                Blade.TipSpecifications.noTip(),
                Blade.Fuller.noFuller()
        );
    }

    private Blade makeLinearWidthBlade(double length, double widthBase, double widthTip, double thickness) {
        return new Blade(
                false,
                widthBase, widthTip, Map.of(),
                thickness, thickness, Map.of(),
                length,
                new Blade.Bevel(0.0, 1.0),
                Blade.EdgeBevel.noBevel(),
                Blade.TipSpecifications.noTip(),
                Blade.Fuller.noFuller()
        );
    }

    // ============ Volume Tests ============

    @Test
    public void testVolume_RectangularBlade() {
        double L = 1000.0, W = 50.0, T = 5.0;
        Blade blade = makeRectBlade(L, W, T);
        double expected = L * W * T;
        double actual = blade.getVolume();
        Assertions.assertEquals(expected, actual, expected * 1e-9, "Rectangular blade volume should be exact");
    }

    @Test
    public void testVolume_LinearWidthBlade() {
        double L = 1000.0, W0 = 60.0, W1 = 20.0, T = 5.0;
        Blade blade = makeLinearWidthBlade(L, W0, W1, T);
        // Volume = L * T * average(width)
        double expected = L * T * 0.5 * (W0 + W1);
        double actual = blade.getVolume();
        Assertions.assertEquals(expected, actual, Math.max(1e-6, expected * 1e-9));
    }

    // ============ Point of Balance Tests ============

    @Test
    public void testPointOfBalance_RectangularBlade() {
        double L = 1000.0, W = 50.0, T = 5.0;
        Blade blade = makeRectBlade(L, W, T);
        double expected = L / 2.0;
        double actual = blade.getPointOfBalance();
        Assertions.assertEquals(expected, actual, expected * 1e-9, "PoB for uniform blade should be at L/2");
    }

    @Test
    public void testPointOfBalance_LinearWidthBlade() {
        double L = 1000.0, W0 = 60.0, W1 = 20.0, T = 5.0;
        Blade blade = makeLinearWidthBlade(L, W0, W1, T);
        // First moment M1 = T * ∫ x * W(x) dx, W(x) = W0 + (W1-W0) x/L
        // ∫ x W(x) dx = W0 * L^2/2 + (W1 - W0) * L^2/3
        double M1 = T * (W0 * L * L / 2.0 + (W1 - W0) * L * L / 3.0);
        double V = L * T * 0.5 * (W0 + W1);
        double expected = M1 / V;
        double actual = blade.getPointOfBalance();
        Assertions.assertEquals(expected, actual, Math.max(1e-6, expected * 1e-6), "PoB for linear width should match analytic");
    }

    // ============ Moment of Inertia Tests ============

    private static double expectedInertiaParallel_Rectangular(double L, double W, double T, double d, double rho) {
        double A = W * T;
        double mPrime = rho * A; // mass per unit length
        // ∫ (d + x)^2 dx from 0..L = ( (d+L)^3 - d^3 ) / 3
        double distTerm = mPrime * (Math.pow(d + L, 3) - Math.pow(d, 3)) / 3.0;
        // Cross-section inertia per unit length: rho * I_centroid, I = (1/12) W^3 T (p=0 => kLocal=1/12)
        double IcentroidPerLen = rho * (W * W * W * T) / 12.0;
        double crossTerm = IcentroidPerLen * L;
        return distTerm + crossTerm;
    }

    private static double expectedInertiaPerpendicular_Rectangular(double L, double W, double T, double d, double rho) {
        double A = W * T;
        double mPrime = rho * A;
        // ∫ (d^2 + x^2) dx from 0..L = d^2 L + L^3/3
        double distTerm = mPrime * (d * d * L + (L * L * L) / 3.0);
        double IcentroidPerLen = rho * (W * W * W * T) / 12.0;
        double crossTerm = IcentroidPerLen * L;
        return distTerm + crossTerm;
    }

    @Test
    public void testInertia_RectangularBlade_Parallel() {
        double L = 1000.0, W = 50.0, T = 5.0, d = 100.0;
        Blade blade = makeRectBlade(L, W, T);
        double expected = expectedInertiaParallel_Rectangular(L, W, T, d, STEEL_DENSITY);
        double actual = blade.getMomentOfInertia(d, STEEL_DENSITY, WeaponProfile.HeadOrientation.PARALLEL);
        Assertions.assertEquals(expected, actual, expected * 1e-6, "MOI (parallel) should match analytic rectangle");
    }

    @Test
    public void testInertia_RectangularBlade_Perp() {
        double L = 1000.0, W = 50.0, T = 5.0, d = 100.0;
        Blade blade = makeRectBlade(L, W, T);
        double expected = expectedInertiaPerpendicular_Rectangular(L, W, T, d, STEEL_DENSITY);
        double actual = blade.getMomentOfInertia(d, STEEL_DENSITY, WeaponProfile.HeadOrientation.PERPENDICULAR);
        Assertions.assertEquals(expected, actual, expected * 1e-3, "MOI (perpendicular) should match analytic rectangle");
    }

    // Linear width, constant thickness: analytic MOI (parallel)
    private static double expectedInertiaParallel_LinearWidth(double L, double W0, double W1, double T, double d, double rho) {
        double m = (W1 - W0) / L; // slope of width
        // Term 1: rho * ∫ A(x) (d + x)^2 dx, A(x) = W(x) T
        // Expand integrand: T * (W0 + m x) * (d^2 + 2 d x + x^2)
        // = T * [ W0 d^2 + (2 W0 d + m d^2) x + (W0 + 2 m d) x^2 + m x^3 ]
        double c0 = W0 * d * d;
        double c1 = (2 * W0 * d) + (m * d * d);
        double c2 = (W0 + 2 * m * d);
        double c3 = m;
        double term1 = rho * T * (
                c0 * L +
                c1 * (L * L) / 2.0 +
                c2 * (L * L * L) / 3.0 +
                c3 * (L * L * L * L) / 4.0
        );
        // Term 2: rho * ∫ (I_centroid) dx = rho * (1/12) T ∫ W(x)^3 dx
        // ∫ (W0 + m x)^3 dx = W0^3 x + (3/2) W0^2 m x^2 + W0 m^2 x^3 + (1/4) m^3 x^4
        double term2 = rho * (T / 12.0) * (
                Math.pow(W0, 3) * L +
                1.5 * (W0 * W0) * m * (L * L) +
                W0 * (m * m) * (L * L * L) +
                0.25 * Math.pow(m, 3) * (L * L * L * L)
        );
        return term1 + term2;
    }

    @Test
    public void testInertia_LinearWidthBlade_Parallel() {
        double L = 1000.0, W0 = 60.0, W1 = 20.0, T = 5.0, d = 100.0;
        Blade blade = makeLinearWidthBlade(L, W0, W1, T);
        double expected = expectedInertiaParallel_LinearWidth(L, W0, W1, T, d, STEEL_DENSITY);
        double actual = blade.getMomentOfInertia(d, STEEL_DENSITY, WeaponProfile.HeadOrientation.PARALLEL);
        Assertions.assertEquals(expected, actual, Math.max(1e-6, expected * 1e-6), "MOI parallel linear width should match analytic");
    }

    // Optionally test perpendicular case similarly
    private static double expectedInertiaPerpendicular_LinearWidth(double L, double W0, double W1, double T, double d, double rho) {
        double m = (W1 - W0) / L;
        // Term 1: rho * ∫ A(x) (d^2 + x^2) dx = rho * T ∫ (W0 + m x)(d^2 + x^2) dx
        double c0 = W0 * d * d;
        double c1 = m * d * d;
        double c2 = W0;
        double c3 = m;
        double term1 = rho * T * (
                c0 * L +
                c1 * (L * L) / 2.0 +
                c2 * (L * L * L) / 3.0 +
                c3 * (L * L * L * L) / 4.0
        );
        // Term 2: same as parallel (cross-section inertia)
        double term2 = rho * (T / 12.0) * (
                Math.pow(W0, 3) * L +
                1.5 * (W0 * W0) * m * (L * L) +
                W0 * (m * m) * (L * L * L) +
                0.25 * Math.pow(m, 3) * (L * L * L * L)
        );
        return term1 + term2;
    }

    @Test
    public void testInertia_LinearWidthBlade_Perp() {
        double L = 1000.0, W0 = 60.0, W1 = 20.0, T = 5.0, d = 100.0;
        Blade blade = makeLinearWidthBlade(L, W0, W1, T);
        double expected = expectedInertiaPerpendicular_LinearWidth(L, W0, W1, T, d, STEEL_DENSITY);
        double actual = blade.getMomentOfInertia(d, STEEL_DENSITY, WeaponProfile.HeadOrientation.PERPENDICULAR);
        Assertions.assertEquals(expected, actual, Math.max(1e-6, expected * 1e-6), "MOI perp linear width should match analytic");
    }
}

