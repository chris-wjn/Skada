package com.cwjn.skada.data.gen.weapon.new_system.weapon.parts;

import com.cwjn.skada.data.gen.weapon.new_system.geometry.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.new_system.weapon.AttackCapable;
import com.cwjn.skada.data.gen.weapon.new_system.weapon.WeaponAxis;
import com.cwjn.skada.data.gen.weapon.new_system.weapon.WeaponPart;
import com.cwjn.skada.data.gen.weapon.new_system.geometry.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.new_system.geometry.PhysicsUtil.MassProperties;

import java.util.EnumSet;
import java.util.Objects;

/**
 * Spherical pommel.
 */
public final class PommelPart implements WeaponPart, AttackCapable {
    private final double radius;
    private final GeometryUtil.Vec3 position;

    public PommelPart(double radius, GeometryUtil.Vec3 position) {
        if (radius <= 0.0) {
            throw new IllegalArgumentException("radius must be > 0");
        }
        this.radius = radius;
        this.position = Objects.requireNonNull(position, "position");
    }

    public double radius() {
        return radius;
    }

    @Override
    public EnumSet<AttackCapability> attackCapabilities() {
        return EnumSet.of(AttackCapability.STRIKE);
    }

    @Override
    public Vec3 position() {
        return position;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        double volume = (4.0 / 3.0) * Math.PI * radius * radius * radius;
        double mass = volume * densityGPerCm3;
        Vec3 com = transform().apply(new Vec3(0.0, 0.0, 0.0));
        return new MassProperties(volume, mass, com);
    }

    @Override
    public GeometryUtil.Bounds localBounds(int samples) {
        double r = radius;
        return new GeometryUtil.Bounds(-r, r, -r, r, -r, r);
    }

    @Override
    public double momentOfInertiaAboutWeaponBase(WeaponAxis axis, double densityGPerCm3, int samples) {
        double volume = (4.0 / 3.0) * Math.PI * radius * radius * radius;
        double mass = volume * densityGPerCm3;
        double iCenter = (2.0 / 5.0) * mass * radius * radius;

        double dx = position.x();
        double dy = position.y();
        double dz = position.z();

        return switch (axis) {
            case X -> iCenter + mass * (dy * dy + dz * dz);
            case Y -> iCenter + mass * (dx * dx + dz * dz);
            case Z -> iCenter + mass * (dx * dx + dy * dy);
        };
    }

    @Override
    public double idealPointOfBalance(com.cwjn.skada.data.registry.AttackType attackType) {
        double normalized = normalizedIdealPointOfBalance(attackType);
        double s = Math.max(0.0, Math.min(1.0, normalized));
        double localX = (-radius) + (2.0 * radius * s);
        Vec3 localPoint = new Vec3(localX, 0.0, 0.0);
        Vec3 worldPoint = transform().apply(localPoint).add(position);
        return worldPoint.x();
    }

    private static double normalizedIdealPointOfBalance(com.cwjn.skada.data.registry.AttackType attackType) {
        if (attackType == null) {
            return 0.5;
        }
        if (attackType.equals(com.cwjn.skada.data.registry.AttackType.slash())) {
            return 0.33;
        }
        if (attackType.equals(com.cwjn.skada.data.registry.AttackType.thrust())) {
            return 0.0;
        }
        if (attackType.equals(com.cwjn.skada.data.registry.AttackType.strike())) {
            return 1.0;
        }
        return 0.5;
    }
}
