package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Spherical pommel.
 */
public final class PommelPart implements WeaponPart, StrikeCapable {
    public static final String TYPE = "pommel";

    public static final Codec<PommelPart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("radius").forGetter(PommelPart::radius)
    ).apply(instance, PommelPart::new));

    private final double radius;

    public PommelPart(double radius) {
        if (radius <= 0.0) {
            throw new IllegalArgumentException("radius must be > 0");
        }
        this.radius = radius;
    }

    public double radius() {
        return radius;
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public double effectiveContactAreaCm2() {
        return Math.PI * radius * radius * 0.28;
    }

    @Override
    public double strikeFaceGeometryFocus() {
        return 1.12;
    }

    @Override
    public double strikeHeadRigidity() {
        return 1.02;
    }

    @Override
    public double strikeAssemblyStability() {
        return 1.04;
    }

    @Override
    public double normalizedStrikeContactPointOnPart(int samples) {
        return 1.0;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        double volume = (4.0 / 3.0) * Math.PI * radius * radius * radius;
        double mass = volume * densityGPerCm3;
        Vec3 com = new Vec3(0.0, 0.0, 0.0);
        return new MassProperties(volume, mass, com);
    }

    @Override
    public GeometryUtil.Bounds localBounds(int samples) {
        double r = radius;
        return new GeometryUtil.Bounds(-r, r, -r, r, -r, r);
    }

    @Override
    public double momentOfInertiaAboutCenterOfMass(WeaponAxis axis, double densityGPerCm3, int samples) {
        double volume = (4.0 / 3.0) * Math.PI * radius * radius * radius;
        double mass = volume * densityGPerCm3;
        double iCenter = (2.0 / 5.0) * mass * radius * radius;

        return switch (axis) {
            case X, Y, Z -> iCenter;
        };
    }

}
