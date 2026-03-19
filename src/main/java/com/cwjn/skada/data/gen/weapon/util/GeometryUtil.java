package com.cwjn.skada.data.gen.weapon.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * General geometry helpers for weapon generation.
 *
 * Units: centimeters for length.
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
}
