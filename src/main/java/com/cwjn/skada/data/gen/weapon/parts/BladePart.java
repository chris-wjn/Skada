package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.profile.BladeProfile;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Weapon part that describes a straight, double-sided blade.
 */
public final class BladePart implements WeaponPart, SlashCapable, ThrustCapable {

    public static final String TYPE = "blade";

    public static final Codec<BladePart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BladeProfile.CODEC.fieldOf("blade").forGetter(BladePart::blade)
    ).apply(instance, BladePart::new));

    private final BladeProfile blade;

    public BladePart(BladeProfile blade) {
        this.blade = Objects.requireNonNull(blade, "blade");
    }

    public BladeProfile blade() {
        return blade;
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        return BladeProfile.computeMassProperties(blade, densityGPerCm3, samples);
    }

    @Override
    public GeometryUtil.Bounds localBounds(int samples) {
        return bladeBounds(blade, samples);
    }

    @Override
    public double momentOfInertiaAboutCenterOfMass(WeaponAxis axis, double densityGPerCm3, int samples) {
        if (samples < 4) {
            throw new IllegalArgumentException("samples must be >= 4");
        }
        if (densityGPerCm3 <= 0.0) {
            throw new IllegalArgumentException("density must be > 0");
        }

        List<BladeProfile.Station> sortedStations = new ArrayList<>(blade.getStations());
        sortedStations.sort(Comparator.comparingDouble(BladeProfile.Station::s));
        Vec3 localCom = BladeProfile.computeMassProperties(blade, densityGPerCm3, samples).centerOfMass();

        double totalLength = GeometryUtil.polylineLength(blade.getSpine());
        double dl = totalLength / samples;

        double inertia = 0.0;
        for (int i = 0; i < samples; i++) {
            double s = (i + 0.5) / samples;
            BladeProfile.BladeSlice slice = BladeProfile.sampleSlice(blade, sortedStations, s);
            double area = slice.area();
            double mass = area * dl * densityGPerCm3;

                GeometryUtil.Vec3 localPos = slice.position();
                GeometryUtil.Vec3 relativePos = new GeometryUtil.Vec3(
                    localPos.x() - localCom.x(),
                    localPos.y() - localCom.y(),
                    localPos.z() - localCom.z());
                double r2 = distanceSquaredToAxis(relativePos, axis);

            double width = slice.width();
            double thickness = slice.thickness();
                double centroidI = centroidInertia(axis, mass, dl, width, thickness);

            inertia += centroidI + mass * r2;
        }

        return inertia;
    }

    private static double distanceSquaredToAxis(GeometryUtil.Vec3 pos, WeaponAxis axis) {
        return switch (axis) {
            case X -> (pos.y() * pos.y()) + (pos.z() * pos.z());
            case Y -> (pos.x() * pos.x()) + (pos.z() * pos.z());
            case Z -> (pos.x() * pos.x()) + (pos.y() * pos.y());
        };
    }

    private static double centroidInertia(WeaponAxis localAxis, double mass, double dl, double width, double thickness) {
        return switch (localAxis) {
            case X -> (1.0 / 12.0) * mass * (width * width + thickness * thickness);
            case Y -> (1.0 / 12.0) * mass * (dl * dl + thickness * thickness);
            case Z -> (1.0 / 12.0) * mass * (dl * dl + width * width);
        };
    }

    private static GeometryUtil.Bounds bladeBounds(BladeProfile blade, int samples) {
        int steps = Math.max(8, samples);
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (int i = 0; i <= steps; i++) {
            double s = (double) i / steps;
            BladeProfile.BladeSlice slice = BladeProfile.sampleSlice(blade, s);
            GeometryUtil.Vec3 pos = slice.position();

            double halfW = slice.width() * 0.5;
            double halfT = slice.thickness() * 0.5;

            minX = Math.min(minX, pos.x());
            maxX = Math.max(maxX, pos.x());
            minY = Math.min(minY, pos.y() - halfW);
            maxY = Math.max(maxY, pos.y() + halfW);
            minZ = Math.min(minZ, pos.z() - halfT);
            maxZ = Math.max(maxZ, pos.z() + halfT);
        }

        if (!Double.isFinite(minX)) {
            return GeometryUtil.Bounds.zero();
        }
        return new GeometryUtil.Bounds(minX, maxX, minY, maxY, minZ, maxZ);
    }

    public double pointTaper() {
        return blade.getPointTaper();
    }

    @Override
    public double tipLengthCm() {
        List<BladeProfile.Station> sorted = new ArrayList<>(blade.getStations());
        sorted.sort(Comparator.comparingDouble(BladeProfile.Station::s));
        double lastS = sorted.isEmpty() ? 0.0 : sorted.get(sorted.size() - 1).s();
        double totalLength = GeometryUtil.polylineLength(blade.getSpine());
        return totalLength * (1.0 - lastS);
    }

    @Override
    public double widthAtPointBase() {
        double s = tipBaseNormalizedPosition();
        return Math.max(1.0e-6, blade.sampleSliceAt(s).width());
    }

    @Override
    public double thicknessAtPointBase() {
        double s = tipBaseNormalizedPosition();
        return Math.max(1.0e-6, blade.sampleSliceAt(s).thickness());
    }


    private double tipBaseNormalizedPosition() {
        List<BladeProfile.Station> sorted = new ArrayList<>(blade.getStations());
        sorted.sort(Comparator.comparingDouble(BladeProfile.Station::s));
        double lastS = sorted.isEmpty() ? 0.0 : sorted.get(sorted.size() - 1).s();
        return Math.max(0.0, Math.min(1.0, lastS));
    }

    @Override
    public SlashCapable.CrossSection crossSectionAt(double normalizedPosition) {
        double s = Math.max(0.0, Math.min(1.0, normalizedPosition));
        BladeProfile.BladeSlice slice = blade.sampleSliceAt(s);
        BladeProfile.Station station = blade.sampleStationAt(s);

        double fullWidth = Math.max(1.0e-6, slice.width());
        double halfWidth = fullWidth * 0.5;
        double spineShift = Math.abs(Math.max(-1.0, Math.min(1.0, station.section().edgeOffset())));
        double spineFlat = Math.max(0.0, Math.min(1.0, station.section().spineFlat()));
        double bevelRunScale = Math.max(1.0e-3, 1.0 - spineFlat);
        double effectiveWidth = Math.max(1.0e-6, halfWidth * bevelRunScale * (1.0 + spineShift));
        double halfThickness = Math.max(0.0, slice.thickness() * 0.5);
        double r = Math.max(1.0e-6, station.section().r());

        return new SlashCapable.CrossSection(effectiveWidth, halfThickness, r);
    }

    @Override
    public Optional<Double> edgeBevelAngle() {
        return Optional.empty();
    }

    @Override
    public double edgeAngleDegreesAt(double normalizedPosition) {
        return SlashCapable.super.edgeAngleDegreesAt(normalizedPosition);
    }

    @Override
    public double edgeRadiusNm() {
        return Math.max(1.0e-6, blade.getEdgeRadiusNm());
    }

    @Override
    public double tipRadiusNm() {
        double taper = pointTaper();
        return Math.max(2.0, 60.0 - 54.0 * taper);
    }

    @Override
    public double normalizedSlashStrikePointOnPart(int samples) {
        int steps = Math.max(24, Math.min(256, samples / 4));

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= steps; i++) {
            double s = (double) i / steps;
            double x = blade.sampleSliceAt(s).position().x();
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
        }

        if (!Double.isFinite(minX) || !Double.isFinite(maxX) || maxX - minX <= 1.0e-6) {
            return 0.66;
        }

        double bestNorm = 0.66;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= steps; i++) {
            double s = (double) i / steps;
            BladeProfile.BladeSlice slice = blade.sampleSliceAt(s);
            double localDensity = Math.max(0.0, slice.area());
            double xNorm = Math.max(0.0, Math.min(1.0, (slice.position().x() - minX) / (maxX - minX)));
            double score = xNorm * localDensity;
            if (score > bestScore) {
                bestScore = score;
                bestNorm = xNorm;
            }
        }

        return Math.max(0.0, Math.min(1.0, bestNorm));
    }

    @Override
    public double normalizedThrustContactPointOnPart(int samples) {
        return 1.0;
    }
}
