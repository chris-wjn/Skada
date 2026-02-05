package com.cwjn.skada.data.gen.weapon.new_system.weapon.parts;

import com.cwjn.skada.data.gen.weapon.new_system.geometry.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.new_system.weapon.WeaponAxis;
import com.cwjn.skada.data.gen.weapon.new_system.weapon.WeaponPart;
import com.cwjn.skada.data.gen.weapon.new_system.geometry.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.new_system.geometry.PhysicsUtil.MassProperties;

import java.util.Objects;

/**
 * Cylindrical handle aligned along the x-axis.
 */
public final class HandlePart implements WeaponPart {
    private final double length;
    private final double radius;
    private final GeometryUtil.Vec3 position;

    public HandlePart(double length, double radius, GeometryUtil.Vec3 position) {
        if (length <= 0.0) {
            throw new IllegalArgumentException("length must be > 0");
        }
        if (radius <= 0.0) {
            throw new IllegalArgumentException("radius must be > 0");
        }
        this.length = length;
        this.radius = radius;
        this.position = Objects.requireNonNull(position, "position");
    }

    public double length() {
        return length;
    }

    public double radius() {
        return radius;
    }

    @Override
    public GeometryUtil.Vec3 position() {
        return position;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        double volume = Math.PI * radius * radius * length;
        double mass = volume * densityGPerCm3;
        Vec3 com = transform().apply(new Vec3(length / 2.0, 0.0, 0.0));
        return new MassProperties(volume, mass, com);
    }

    @Override
    public GeometryUtil.Bounds localBounds(int samples) {
        return new GeometryUtil.Bounds(0.0, length, -radius, radius, -radius, radius);
    }

    @Override
    public double momentOfInertiaAboutWeaponBase(WeaponAxis axis, double densityGPerCm3, int samples) {
        double volume = Math.PI * radius * radius * length;
        double mass = volume * densityGPerCm3;

        double ixx = 0.5 * mass * radius * radius;
        double iyy = (1.0 / 12.0) * mass * (3.0 * radius * radius + length * length);
        double izz = iyy;

        Vec3 center = position.add(transform().apply(new Vec3(length / 2.0, 0.0, 0.0)));
        double dx = center.x();
        double dy = center.y();
        double dz = center.z();

        WeaponAxis localAxis = transform().localAxisForWeaponAxis(axis);
        double iLocal = switch (localAxis) {
            case X -> ixx;
            case Y -> iyy;
            case Z -> izz;
        };

        return switch (axis) {
            case X -> iLocal + mass * (dy * dy + dz * dz);
            case Y -> iLocal + mass * (dx * dx + dz * dz);
            case Z -> iLocal + mass * (dx * dx + dy * dy);
        };
    }

}
