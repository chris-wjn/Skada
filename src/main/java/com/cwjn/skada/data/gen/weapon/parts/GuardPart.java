package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Rectangular guard (cross-guard) centered at the local origin.
 *
 * Dimensions:
 * x = thickness (along blade length)
 * y = span (cross-guard length)
 * z = height
 */
public final class GuardPart implements WeaponPart {
    public static final String TYPE = "guard";

    public static final Codec<GuardPart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("sizeX").forGetter(GuardPart::sizeX),
            Codec.DOUBLE.fieldOf("sizeY").forGetter(GuardPart::sizeY),
            Codec.DOUBLE.fieldOf("sizeZ").forGetter(GuardPart::sizeZ)
    ).apply(instance, GuardPart::new));

    private final double sizeX;
    private final double sizeY;
    private final double sizeZ;

    public GuardPart(double sizeX, double sizeY, double sizeZ) {
        if (sizeX <= 0.0 || sizeY <= 0.0 || sizeZ <= 0.0) {
            throw new IllegalArgumentException("sizeX/sizeY/sizeZ must be > 0");
        }
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
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
    public String typeId() {
        return TYPE;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        double volume = sizeX * sizeY * sizeZ;
        double mass = volume * densityGPerCm3;
        Vec3 com = new Vec3(0.0, 0.0, 0.0);
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
    public double momentOfInertiaAboutCenterOfMass(WeaponAxis axis, double densityGPerCm3, int samples) {
        double volume = sizeX * sizeY * sizeZ;
        double mass = volume * densityGPerCm3;

        double ixx = (1.0 / 12.0) * mass * (sizeY * sizeY + sizeZ * sizeZ);
        double iyy = (1.0 / 12.0) * mass * (sizeX * sizeX + sizeZ * sizeZ);
        double izz = (1.0 / 12.0) * mass * (sizeX * sizeX + sizeY * sizeY);

        return switch (axis) {
            case X -> ixx;
            case Y -> iyy;
            case Z -> izz;
        };
    }

}
