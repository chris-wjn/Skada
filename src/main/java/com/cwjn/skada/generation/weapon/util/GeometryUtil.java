package com.cwjn.skada.generation.weapon.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * General geometry helpers for weapon generation.
 *
 * Units: centimeters for length.
 * Centerline interpolation uses Catmull-Rom splines for smooth C2-continuous curves.
 */
public final class GeometryUtil {
    private GeometryUtil() {
    }

    public record Vec2(double x, double y) {
    }

    public record Vec3(double x, double y, double z) {
        public static final Codec<Vec3> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Vec3::x),
                Codec.DOUBLE.fieldOf("y").forGetter(Vec3::y),
                Codec.DOUBLE.fieldOf("z").forGetter(Vec3::z)
        ).apply(instance, Vec3::new));

        public Vec3 add(Vec3 other) {
            return new Vec3(x + other.x, y + other.y, z + other.z);
        }

        public Vec3 mul(double scalar) {
            return new Vec3(x * scalar, y * scalar, z * scalar);
        }

        public double distanceTo(Vec3 other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    public record Bounds(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        public static Bounds zero() {
            return new Bounds(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        public Bounds shifted(double dx, double dy, double dz) {
            return new Bounds(minX + dx, maxX + dx, minY + dy, maxY + dy, minZ + dz, maxZ + dz);
        }
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double modifiedSuperellipseArea(double width, double thickness, double r) {
        return modifiedSuperellipseArea(width, thickness, r, 0.0);
    }

    public static double modifiedSuperellipseArea(double width, double thickness, double r, double spineFlat) {
        double a = width / 2.0;
        double b = thickness / 2.0;
        double k = r / (r + 1.0);
        double flat = Math.max(0.0, Math.min(1.0, spineFlat));
        double bevelBlend = flat + ((1.0 - flat) * k);
        return 4.0 * a * b * bevelBlend;
    }

    public static List<Vec2> sampleCrossSectionOutline(double width, double thickness, double r, int steps) {
        return sampleCrossSectionOutline(width, thickness, r, 0.0, 0.0, steps);
    }

    public static List<Vec2> sampleCrossSectionOutline(double width, double thickness, double r, double edgeOffset, int steps) {
        return sampleCrossSectionOutline(width, thickness, r, edgeOffset, 0.0, steps);
    }

    public static List<Vec2> sampleCrossSectionOutline(double width, double thickness, double r, double edgeOffset, double spineFlat, int steps) {
        if (steps < 4) {
            throw new IllegalArgumentException("steps must be >= 4");
        }
        double a = width / 2.0;
        double b = thickness / 2.0;
        double yOffset = edgeOffset * b;
        double clampedFlat = Math.max(0.0, Math.min(1.0, spineFlat));
        double flatHalfWidth = a * clampedFlat;
        double bevelHalfWidth = Math.max(0.0, a - flatHalfWidth);

        List<Vec2> outline = new ArrayList<>(steps * 2);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = -a + (2.0 * a * t);
            double absX = Math.abs(x);
            double y;
            if (bevelHalfWidth <= 1.0e-9 || absX <= flatHalfWidth) {
                y = b;
            } else {
                double u = (absX - flatHalfWidth) / bevelHalfWidth;
                y = b * (1.0 - Math.pow(Math.max(0.0, Math.min(1.0, u)), r));
            }
            y += yOffset;
            outline.add(new Vec2(x, y));
        }
        for (int i = steps; i >= 0; i--) {
            double t = (double) i / steps;
            double x = -a + (2.0 * a * t);
            double absX = Math.abs(x);
            double y;
            if (bevelHalfWidth <= 1.0e-9 || absX <= flatHalfWidth) {
                y = -b;
            } else {
                double u = (absX - flatHalfWidth) / bevelHalfWidth;
                y = -b * (1.0 - Math.pow(Math.max(0.0, Math.min(1.0, u)), r));
            }
            y += yOffset;
            outline.add(new Vec2(x, y));
        }
        return outline;
    }

    public static double polylineLength(List<Vec3> points) {
        Objects.requireNonNull(points, "points");
        if (points.size() < 2) {
            throw new IllegalArgumentException("Polyline requires at least 2 points");
        }
        double len = 0.0;
        for (int i = 0; i < points.size() - 1; i++) {
            len += points.get(i).distanceTo(points.get(i + 1));
        }
        return len;
    }

    /** Samples the centerline by normalized arc length, ignoring repeated consecutive controls. */
    public static Vec3 pointOnPolyline(List<Vec3> points, double s) {
        Objects.requireNonNull(points, "points");
        if (points.size() < 2) {
            throw new IllegalArgumentException("Polyline requires at least 2 points");
        }
        if (s <= 0.0) {
            return points.get(0);
        }
        if (s >= 1.0) {
            return points.get(points.size() - 1);
        }

        // Zero-length knot intervals make spline interpolation undefined.
        List<Vec3> controls = new ArrayList<>(points.size());
        for (Vec3 point : points) {
            if (controls.isEmpty() || controls.get(controls.size() - 1).distanceTo(point) > 0.0) {
                controls.add(point);
            }
        }
        if (controls.size() == 1) {
            return controls.get(0);
        }
        points = controls.size() > 2 ? sampleSpline(controls) : controls;

        double total = polylineLength(points);
        double target = total * s;
        double accum = 0.0;

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 a = points.get(i);
            Vec3 b = points.get(i + 1);
            double seg = a.distanceTo(b);
            if (accum + seg >= target) {
                double t = (target - accum) / seg;
                return new Vec3(
                        lerp(a.x(), b.x(), t),
                        lerp(a.y(), b.y(), t),
                        lerp(a.z(), b.z(), t)
                );
            }
            accum += seg;
        }
        return points.get(points.size() - 1);
    }

    private static List<Vec3> sampleSpline(List<Vec3> controlPoints) {
        int totalPerSegment = Math.max(20, 8 * controlPoints.size());
        int segments = controlPoints.size() - 1;
        int totalSamples = segments * totalPerSegment + 1;
        List<Vec3> sampled = new ArrayList<>(totalSamples);

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / (totalSamples - 1);
            sampled.add(catmullRom(controlPoints, t));
        }
        return sampled;
    }

    private static Vec3 catmullRom(List<Vec3> points, double t) {
        int n = points.size();
        double[] intervals = new double[n - 1];
        double total = 0.0;
        for (int j = 0; j < n - 1; j++) {
            intervals[j] = Math.pow(points.get(j).distanceTo(points.get(j + 1)), 0.5);
            total += intervals[j];
        }

        if (total <= 0.0) {
            return points.get(0);
        }

        double target = t * total;
        double accum = 0.0;
        int seg = 0;
        for (; seg < n - 1; seg++) {
            if (accum + intervals[seg] >= target || seg == n - 2) {
                break;
            }
            accum += intervals[seg];
        }

        double lt = intervals[seg] > 0.0 ? (target - accum) / intervals[seg] : 0.0;

        Vec3 p0 = points.get(Math.max(0, seg - 1));
        Vec3 p1 = points.get(seg);
        Vec3 p2 = points.get(seg + 1);
        Vec3 p3 = points.get(Math.min(n - 1, seg + 2));

        double k1 = Math.pow(p0.distanceTo(p1), 0.5);
        double k2 = k1 + Math.pow(p1.distanceTo(p2), 0.5);
        double k3 = k2 + Math.pow(p2.distanceTo(p3), 0.5);

        double u = k1 + lt * (k2 - k1);

        Vec3 a1 = k1 <= 1e-9 ? p1 : remap(0.0, k1, p0, p1, u);
        Vec3 a2 = remap(k1, k2, p1, p2, u);
        Vec3 a3 = k3 - k2 <= 1e-9 ? p2 : remap(k2, k3, p2, p3, u);
        Vec3 b1 = remap(0.0, k2, a1, a2, u);
        Vec3 b2 = remap(k1, k3, a2, a3, u);
        return remap(k1, k2, b1, b2, u);
    }

    private static Vec3 remap(double a, double b, Vec3 c, Vec3 d, double u) {
        double t = (u - a) / (b - a);
        return new Vec3(
                lerp(c.x(), d.x(), t),
                lerp(c.y(), d.y(), t),
                lerp(c.z(), d.z(), t)
        );
    }
}
