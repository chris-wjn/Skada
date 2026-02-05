package com.cwjn.skada.util;

import java.util.function.DoubleUnaryOperator;

/**
 * Physics utility functions for weapon calculations.
 * All units: cm for length, g for mass, g/cm³ for density, g·cm² for moment of inertia.
 */
public final class PhysicsUtil {

    private PhysicsUtil() {} // Utility class

    /**
     * Adaptive Simpson's rule integration with configurable tolerance.
     * Used for accurate integration of non-linear weapon geometry.
     *
     * @param f         Function to integrate
     * @param a         Lower bound
     * @param b         Upper bound
     * @param tolerance Target accuracy (recommend 1e-6)
     * @return Approximate integral value
     */
    public static double adaptiveSimpson(DoubleUnaryOperator f, double a, double b, double tolerance) {
        double c = (a + b) / 2.0;
        double h = b - a;
        double fa = f.applyAsDouble(a);
        double fb = f.applyAsDouble(b);
        double fc = f.applyAsDouble(c);
        double S = (h / 6.0) * (fa + 4.0 * fc + fb);
        return adaptiveSimpsonRecursive(f, a, b, tolerance, S, fa, fb, fc, 15);
    }

    private static double adaptiveSimpsonRecursive(DoubleUnaryOperator f, double a, double b,
                                                    double tolerance, double S, double fa, double fb, double fc, int depth) {
        double c = (a + b) / 2.0;
        double d = (a + c) / 2.0;
        double e = (c + b) / 2.0;
        double h = b - a;
        double fd = f.applyAsDouble(d);
        double fe = f.applyAsDouble(e);
        double Sleft = (h / 12.0) * (fa + 4.0 * fd + fc);
        double Sright = (h / 12.0) * (fc + 4.0 * fe + fb);
        double S2 = Sleft + Sright;

        if (depth <= 0 || Math.abs(S2 - S) <= 15.0 * tolerance) {
            return S2 + (S2 - S) / 15.0;
        }

        return adaptiveSimpsonRecursive(f, a, c, tolerance / 2.0, Sleft, fa, fc, fd, depth - 1)
             + adaptiveSimpsonRecursive(f, c, b, tolerance / 2.0, Sright, fc, fb, fe, depth - 1);
    }

    /**
     * Modified superellipse area coefficient.
     * For the equation |x/a|^r + |y/b| = 1 (NOT standard symmetric superellipse).
     *
     * @param r Curve factor (exponent on x term)
     * @return Area coefficient K = r/(r+1), multiply by 4ab for full area
     */
    public static double modifiedSuperellipseCoefficient(double r) {
        if (r <= 0) return 0.0;
        return r / (r + 1.0);
    }

    /**
     * Calculate area of modified superellipse bevel cross-section.
     * Uses formula: Area = 4 * a * b * r/(r+1)
     *
     * @param halfWidth  Semi-width (a) of bounding rectangle
     * @param halfHeight Semi-height (b) of bounding rectangle
     * @param curveFactor r value in |x/a|^r + |y/b| = 1
     * @return Area of the modified superellipse shape
     */
    public static double modifiedSuperellipseArea(double halfWidth, double halfHeight, double curveFactor) {
        return 4.0 * halfWidth * halfHeight * modifiedSuperellipseCoefficient(curveFactor);
    }

    /**
     * Bevel K-factor: ratio of beveled cross-section area to rectangular area.
     * Used to reduce volume for beveled weapon heads.
     *
     * @param bevelPercentage Fraction of width that is beveled (0.0-1.0)
     * @param curveFactor     r value for superellipse curve
     * @return K-factor to multiply against rectangular volume
     */
    public static double bevelKFactor(double bevelPercentage, double curveFactor) {
        double coeff = modifiedSuperellipseCoefficient(curveFactor);
        return (1.0 - bevelPercentage) + bevelPercentage * coeff;
    }

    /**
     * Calculate circular segment area from chord and sagitta (for fullers).
     *
     * @param chord   Chord length (c)
     * @param sagitta Sagitta height (s)
     * @return Area of circular segment
     */
    public static double circularSegmentArea(double chord, double sagitta) {
        if (chord <= 0 || sagitta <= 0) return 0.0;
        double radius = circularSegmentRadius(chord, sagitta);
        double theta = 2.0 * Math.asin(chord / (2.0 * radius));
        return 0.5 * radius * radius * (theta - Math.sin(theta));
    }

    /**
     * Calculate circle radius from chord and sagitta.
     *
     * @param chord   Chord length (c)
     * @param sagitta Sagitta height (s)
     * @return Radius of the circle
     */
    public static double circularSegmentRadius(double chord, double sagitta) {
        if (sagitta <= 0) return Double.POSITIVE_INFINITY;
        return (chord * chord) / (8.0 * sagitta) + sagitta / 2.0;
    }

    /**
     * Derive arc length from chord and sagitta for a circular arc.
     *
     * @param chord   Chord length
     * @param sagitta Sagitta height
     * @return Arc length
     */
    public static double arcLengthFromChordSagitta(double chord, double sagitta) {
        if (chord <= 0 || sagitta <= 0) return chord;
        double radius = circularSegmentRadius(chord, sagitta);
        double theta = 2.0 * Math.asin(chord / (2.0 * radius));
        return radius * theta;
    }

    // ==================== Moment of Inertia Formulas ====================

    /**
     * Moment of inertia of solid cylinder about transverse axis through center.
     * I_cm = (1/12) * m * (3r² + L²)
     */
    public static double solidCylinderMoI(double mass, double radius, double length) {
        return (1.0 / 12.0) * mass * (3.0 * radius * radius + length * length);
    }

    /**
     * Moment of inertia of hollow cylinder about transverse axis through center.
     * I_cm = (1/12) * m * (3(Ro² + Ri²) + L²)
     */
    public static double hollowCylinderMoI(double mass, double outerRadius, double innerRadius, double length) {
        return (1.0 / 12.0) * mass * (3.0 * (outerRadius * outerRadius + innerRadius * innerRadius) + length * length);
    }

    /**
     * Moment of inertia of rectangular plate about transverse axis through center.
     * I_cm = (1/12) * m * (L² + T²)
     */
    public static double rectangularPlateMoI(double mass, double length, double thickness) {
        return (1.0 / 12.0) * mass * (length * length + thickness * thickness);
    }

    /**
     * Moment of inertia of right triangular prism about axis through centroid.
     * I_cm = (m/18) * (b² + h²) for right triangle
     */
    public static double rightTriangularPrismMoI(double mass, double base, double height) {
        return (mass / 18.0) * (base * base + height * height);
    }

    /**
     * Moment of inertia of pyramid about transverse axis through center of mass.
     * I_cm ≈ m * (3L²/80 + W²/20)
     */
    public static double pyramidMoI(double mass, double length, double baseWidth) {
        return mass * (3.0 * length * length / 80.0 + baseWidth * baseWidth / 20.0);
    }

    /**
     * Moment of inertia of solid sphere about any axis through center.
     * I_cm = (2/5) * m * r²
     */
    public static double solidSphereMoI(double mass, double radius) {
        return (2.0 / 5.0) * mass * radius * radius;
    }

    /**
     * Moment of inertia of cone about transverse axis through center of mass.
     * I_cm = (3/80) * m * (4r² + h²)
     */
    public static double coneMoI(double mass, double radius, double height) {
        return (3.0 / 80.0) * mass * (4.0 * radius * radius + height * height);
    }

    /**
     * Parallel axis theorem: shift moment of inertia from center of mass to new axis.
     * I = I_cm + m * d²
     */
    public static double parallelAxis(double massCenterMoI, double mass, double distance) {
        return massCenterMoI + mass * distance * distance;
    }

    // ==================== Volume Formulas ====================

    /**
     * Volume of solid cylinder.
     */
    public static double cylinderVolume(double radius, double length) {
        return Math.PI * radius * radius * length;
    }

    /**
     * Volume of hollow cylinder (tube).
     */
    public static double hollowCylinderVolume(double outerRadius, double innerRadius, double length) {
        return Math.PI * (outerRadius * outerRadius - innerRadius * innerRadius) * length;
    }

    /**
     * Volume of rectangular prism.
     */
    public static double boxVolume(double length, double width, double height) {
        return length * width * height;
    }

    /**
     * Volume of right triangular prism.
     */
    public static double triangularPrismVolume(double triangleBase, double triangleHeight, double prismThickness) {
        return 0.5 * triangleBase * triangleHeight * prismThickness;
    }

    /**
     * Volume of pyramid.
     */
    public static double pyramidVolume(double baseWidth, double baseHeight, double pyramidLength) {
        return (baseWidth * baseHeight * pyramidLength) / 3.0;
    }

    /**
     * Volume of sphere.
     */
    public static double sphereVolume(double radius) {
        return (4.0 / 3.0) * Math.PI * radius * radius * radius;
    }

    /**
     * Volume of spherical cap.
     */
    public static double sphericalCapVolume(double sphereRadius, double capHeight) {
        return Math.PI * capHeight * capHeight * (3.0 * sphereRadius - capHeight) / 3.0;
    }

    /**
     * Volume of cone.
     */
    public static double coneVolume(double radius, double height) {
        return Math.PI * radius * radius * height / 3.0;
    }

    /**
     * Volume of regular octagonal prism (inscribed in circle).
     * Side length a ≈ r * 0.765 for octagon inscribed in circle of radius r.
     */
    public static double octagonalPrismVolume(double inscribedRadius, double height) {
        double sideLength = inscribedRadius * 0.7653668647; // 2 * sin(π/8)
        return 2.0 * (1.0 + Math.sqrt(2.0)) * sideLength * sideLength * height;
    }

    /**
     * Area of ellipse.
     */
    public static double ellipseArea(double semiMajorAxis, double semiMinorAxis) {
        return Math.PI * semiMajorAxis * semiMinorAxis;
    }
}
