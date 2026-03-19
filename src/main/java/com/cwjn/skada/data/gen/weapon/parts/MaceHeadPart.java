package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Bounds;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

/**
 * Mace head part based on {@link MaceHeadProfile}.
 *
 * Local profile orientation follows the mace schema: +X runs from mount toward the distal crown.
 * Maces remain strike-only even when the profile includes spike rings.
 */
public final class MaceHeadPart implements WeaponPart, StrikeCapable {

    public static final String TYPE = "mace_head";

    public static final Codec<MaceHeadPart> CODEC = createCodec();

    private final MaceHeadProfile profile;

    public MaceHeadPart(MaceHeadProfile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    public static Codec<MaceHeadPart> codec() {
        return createCodec();
    }

    private static Codec<MaceHeadPart> createCodec() {
        return RecordCodecBuilder.create(instance -> instance.group(
                MaceHeadProfile.CODEC.fieldOf("mace").forGetter(MaceHeadPart::profile)
        ).apply(instance, MaceHeadPart::new));
    }

    public MaceHeadProfile profile() {
        return profile;
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        return MaceHeadProfile.computeMassProperties(profile, densityGPerCm3, samples);
    }

    @Override
    public Bounds localBounds(int samples) {
        return MaceHeadProfile.localBounds(profile, samples);
    }

    @Override
    public double momentOfInertiaAboutCenterOfMass(WeaponAxis axis, double densityGPerCm3, int samples) {
        if (densityGPerCm3 <= 0.0) {
            throw new IllegalArgumentException("density must be > 0");
        }

        MassProperties props = profile.computeMassProperties(densityGPerCm3, Math.max(24, samples));
        Bounds bounds = profile.localBounds(Math.max(24, samples));
        double mass = props.massG();
        if (mass <= 0.0) {
            return 0.0;
        }

        double a = Math.max(1.0e-6, (bounds.maxX() - bounds.minX()) * 0.5);
        double b = Math.max(1.0e-6, (bounds.maxY() - bounds.minY()) * 0.5);
        double c = Math.max(1.0e-6, (bounds.maxZ() - bounds.minZ()) * 0.5);
        Vec3 boundsCenter = new Vec3(
                (bounds.minX() + bounds.maxX()) * 0.5,
                (bounds.minY() + bounds.maxY()) * 0.5,
                (bounds.minZ() + bounds.maxZ()) * 0.5);

        double ixx = 0.2 * mass * ((b * b) + (c * c));
        double iyy = 0.2 * mass * ((a * a) + (c * c));
        double izz = 0.2 * mass * ((a * a) + (b * b));

        Vec3 offset = new Vec3(
                boundsCenter.x() - props.centerOfMass().x(),
                boundsCenter.y() - props.centerOfMass().y(),
                boundsCenter.z() - props.centerOfMass().z());

        return switch (axis) {
            case X -> ixx + (mass * ((offset.y() * offset.y()) + (offset.z() * offset.z())));
            case Y -> iyy + (mass * ((offset.x() * offset.x()) + (offset.z() * offset.z())));
            case Z -> izz + (mass * ((offset.x() * offset.x()) + (offset.y() * offset.y())));
        };
    }

    @Override
    public double normalizedStrikeContactPointOnPart(int samples) {
        Bounds bounds = profile.localBounds(Math.max(24, samples));
        double span = Math.max(1.0e-6, bounds.maxX() - bounds.minX());
        double contactX = profile.deriveStrikeGeometry().contactPointX();
        return Math.max(0.0, Math.min(1.0, (contactX - bounds.minX()) / span));
    }

    @Override
    public double effectiveContactAreaCm2() {
        return profile.deriveStrikeGeometry().effectiveContactAreaCm2();
    }

    @Override
    public double strikeFaceGeometryFocus() {
        return profile.deriveStrikeGeometry().focusFactor();
    }

    @Override
    public double strikeHeadRigidity() {
        return profile.deriveStrikeGeometry().rigidity();
    }

    @Override
    public double strikeAssemblyStability() {
        double base = "socket".equals(profile.getMount().type()) ? 1.06 : 0.98;
        return profile.getFlanges() != null ? Math.min(1.18, base + 0.04) : base;
    }
}