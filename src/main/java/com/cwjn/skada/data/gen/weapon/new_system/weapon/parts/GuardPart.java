package com.cwjn.skada.data.gen.weapon.new_system.weapon.parts;

import com.cwjn.skada.data.gen.weapon.new_system.geometry.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.new_system.weapon.WeaponAxis;
import com.cwjn.skada.data.gen.weapon.new_system.weapon.WeaponPart;
import com.cwjn.skada.data.gen.weapon.new_system.geometry.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.new_system.geometry.PhysicsUtil.MassProperties;

import java.util.Objects;

/**
 * Rectangular guard (cross-guard) centered at its position.
 *
 * Dimensions:
 * x = thickness (along blade length)
 * y = span (cross-guard length)
 * z = height
 */
public final class GuardPart implements WeaponPart {
    private final double sizeX;
    private final double sizeY;
    private final double sizeZ;
    private final Vec3 position;

    public GuardPart(double sizeX, double sizeY, double sizeZ, Vec3 position) {
        if (sizeX <= 0.0 || sizeY <= 0.0 || sizeZ <= 0.0) {
            throw new IllegalArgumentException("sizeX/sizeY/sizeZ must be > 0");
        }
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.position = Objects.requireNonNull(position, "position");
    }

    public double sizeX() {
        return sizeX;
    }

    public double sizeY() {
        return sizeY;
    }

    public double sizeZ() {
        return sizeZ;
    }

    @Override
    public Vec3 position() {
        return position;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        double volume = sizeX * sizeY * sizeZ;
        double mass = volume * densityGPerCm3;
        Vec3 com = transform().apply(new Vec3(0.0, 0.0, 0.0));
        return new MassProperties(volume, mass, com);
    }

    @Override
    public GeometryUtil.Bounds localBounds(int samples) {
        double hx = sizeX / 2.0;
        double hy = sizeY / 2.0;
        double hz = sizeZ / 2.0;
        return new GeometryUtil.Bounds(-hx, hx, -hy, hy, -hz, hz);
    }

    @Override
    public double momentOfInertiaAboutWeaponBase(WeaponAxis axis, double densityGPerCm3, int samples) {
        double volume = sizeX * sizeY * sizeZ;
        double mass = volume * densityGPerCm3;

        double ixx = (1.0 / 12.0) * mass * (sizeY * sizeY + sizeZ * sizeZ);
        double iyy = (1.0 / 12.0) * mass * (sizeX * sizeX + sizeZ * sizeZ);
        double izz = (1.0 / 12.0) * mass * (sizeX * sizeX + sizeY * sizeY);

        double dx = position.x();
        double dy = position.y();
        double dz = position.z();

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
