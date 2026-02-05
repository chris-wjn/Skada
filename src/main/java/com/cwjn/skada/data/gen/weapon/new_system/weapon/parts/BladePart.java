package com.cwjn.skada.data.gen.weapon.new_system.weapon.parts;

import com.cwjn.skada.data.gen.weapon.new_system.blade.BladeProfile;
import com.cwjn.skada.data.gen.weapon.new_system.geometry.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.new_system.weapon.AttackCapable;
import com.cwjn.skada.data.gen.weapon.new_system.weapon.WeaponAxis;
import com.cwjn.skada.data.gen.weapon.new_system.weapon.WeaponPart;
import com.cwjn.skada.data.gen.weapon.new_system.weapon.WeaponPartTransform;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.gen.weapon.new_system.geometry.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.new_system.geometry.PhysicsUtil.MassProperties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Blade weapon part backed by BladeProfileV1 geometry.
 */
public final class BladePart implements WeaponPart, AttackCapable {

    private final BladeProfile blade;
    private final GeometryUtil.Vec3 position;

    public BladePart(BladeProfile blade, Vec3 position) {
        this.blade = Objects.requireNonNull(blade, "blade");
        this.position = Objects.requireNonNull(position, "position");
    }

    public BladeProfile blade() {
        return blade;
    }

    @Override
    public EnumSet<AttackCapability> attackCapabilities() {
        return EnumSet.of(AttackCapability.SLASH, AttackCapability.THRUST);
    }

    @Override
    public Vec3 position() {
        return position;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        MassProperties baseProperties = BladeProfile.computeMassProperties(blade, densityGPerCm3, samples);
        Vec3 transformedCom = transform().apply(baseProperties.centerOfMass());
        return new MassProperties(baseProperties.volumeCm3(), baseProperties.massG(), transformedCom);
    }

    @Override
    public GeometryUtil.Bounds localBounds(int samples) {
        return bladeBounds(blade, samples);
    }

    @Override
    public double momentOfInertiaAboutWeaponBase(WeaponAxis axis, double densityGPerCm3, int samples) {
        if (samples < 4) {
            throw new IllegalArgumentException("samples must be >= 4");
        }
        if (densityGPerCm3 <= 0.0) {
            throw new IllegalArgumentException("density must be > 0");
        }

        List<BladeProfile.Station> sortedStations = new ArrayList<>(blade.getStations());
        sortedStations.sort(Comparator.comparingDouble(BladeProfile.Station::s));

        double totalLength = GeometryUtil.polylineLength(blade.getSpine());
        double dl = totalLength / samples;
        WeaponPartTransform transform = transform();
        WeaponAxis localAxis = transform.localAxisForWeaponAxis(axis);

        double inertia = 0.0;
        for (int i = 0; i < samples; i++) {
            double s = (i + 0.5) / samples;
            BladeProfile.BladeSlice slice = BladeProfile.sampleSlice(blade, sortedStations, s);
            double area = slice.area();
            double mass = area * dl * densityGPerCm3;

            GeometryUtil.Vec3 worldPos = transform.apply(slice.position()).add(position);
            double r2 = distanceSquaredToAxis(worldPos, axis);

            double width = slice.width();
            double thickness = slice.thickness();
            double centroidI = centroidInertia(localAxis, mass, dl, width, thickness);

            inertia += centroidI + mass * r2;
        }

        return inertia;
    }

    public double idealPointOfBalance(AttackType attackType) {
        double normalized = normalizedIdealPointOfBalance(attackType);
        double s = Math.max(0.0, Math.min(1.0, normalized));
        Vec3 localPoint = GeometryUtil.pointOnPolyline(blade.getSpine(), s);
        Vec3 worldPoint = transform().apply(localPoint).add(position);
        return worldPoint.x();
    }

    private static double normalizedIdealPointOfBalance(AttackType attackType) {
        if (attackType == null) {
            return 0.5;
        }
        if (attackType.equals(AttackType.slash())) {
            return 0.33;
        }
        if (attackType.equals(AttackType.thrust())) {
            return 0.0;
        }
        if (attackType.equals(AttackType.strike())) {
            return 1.0;
        }
        return 0.5;
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
}
