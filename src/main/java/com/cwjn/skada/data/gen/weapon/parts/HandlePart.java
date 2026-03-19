package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Cylindrical handle aligned along the x-axis.
 */
public final class HandlePart implements WeaponPart {
    public static final String TYPE = "handle";

    public static final Codec<HandlePart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("length").forGetter(HandlePart::length),
            Codec.DOUBLE.fieldOf("radius").forGetter(HandlePart::radius)
    ).apply(instance, HandlePart::new));

    private final double length;
    private final double radius;

    public HandlePart(double length, double radius) {
        if (length <= 0.0) {
            throw new IllegalArgumentException("length must be > 0");
        }
        if (radius <= 0.0) {
            throw new IllegalArgumentException("radius must be > 0");
        }
        this.length = length;
        this.radius = radius;
    }

    public double length() {
        return length;
    }

    public double radius() {
        return radius;
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        double volume = Math.PI * radius * radius * length;
        double mass = volume * densityGPerCm3;
        Vec3 com = new Vec3(length / 2.0, 0.0, 0.0);
        return new MassProperties(volume, mass, com);
    }

    @Override
    public GeometryUtil.Bounds localBounds(int samples) {
        return new GeometryUtil.Bounds(0.0, length, -radius, radius, -radius, radius);
    }

    @Override
    public double momentOfInertiaAboutCenterOfMass(WeaponAxis axis, double densityGPerCm3, int samples) {
        double volume = Math.PI * radius * radius * length;
        double mass = volume * densityGPerCm3;

        double ixx = 0.5 * mass * radius * radius;
        double iyy = (1.0 / 12.0) * mass * (3.0 * radius * radius + length * length);
        double izz = iyy;

        return switch (axis) {
            case X -> ixx;
            case Y -> iyy;
            case Z -> izz;
        };
    }

}
