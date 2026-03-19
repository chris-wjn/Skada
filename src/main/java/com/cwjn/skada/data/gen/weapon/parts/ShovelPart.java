package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Bounds;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

/**
 * Shovel head part based on {@link ShovelHeadProfile}.
 *
 * Local profile orientation follows the shovel schema: the socket extends toward -X and the distal lip lies toward +X.
 * Shovels remain strike-only even when the lip narrows or points forward.
 */
public final class ShovelPart implements WeaponPart, StrikeCapable {

    public static final String TYPE = "shovel";

    public static final Codec<ShovelPart> CODEC = createCodec();

    private final ShovelHeadProfile profile;

    public ShovelPart(ShovelHeadProfile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    public static Codec<ShovelPart> codec() {
        return createCodec();
    }

    private static Codec<ShovelPart> createCodec() {
        return RecordCodecBuilder.create(instance -> instance.group(
                ShovelHeadProfile.CODEC.fieldOf("shovel").forGetter(ShovelPart::profile)
        ).apply(instance, ShovelPart::new));
    }

    public ShovelHeadProfile profile() {
        return profile;
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        return ShovelHeadProfile.computeMassProperties(profile, densityGPerCm3, samples);
    }

    @Override
    public Bounds localBounds(int samples) {
        return ShovelHeadProfile.localBounds(profile, samples);
    }

    @Override
    public double momentOfInertiaAboutCenterOfMass(WeaponAxis axis, double densityGPerCm3, int samples) {
        if (densityGPerCm3 <= 0.0) {
            throw new IllegalArgumentException("density must be > 0");
        }

        MassProperties props = profile.computeMassProperties(densityGPerCm3, Math.max(32, samples));
        Bounds bounds = profile.localBounds(Math.max(32, samples));
        double mass = props.massG();
        if (mass <= 0.0) {
            return 0.0;
        }

        double sizeX = Math.max(1.0e-6, bounds.maxX() - bounds.minX());
        double sizeY = Math.max(1.0e-6, bounds.maxY() - bounds.minY());
        double sizeZ = Math.max(1.0e-6, bounds.maxZ() - bounds.minZ());
        Vec3 boundsCenter = new Vec3(
                (bounds.minX() + bounds.maxX()) * 0.5,
                (bounds.minY() + bounds.maxY()) * 0.5,
                (bounds.minZ() + bounds.maxZ()) * 0.5);

        double ixx = (1.0 / 12.0) * mass * ((sizeY * sizeY) + (sizeZ * sizeZ));
        double iyy = (1.0 / 12.0) * mass * ((sizeX * sizeX) + (sizeZ * sizeZ));
        double izz = (1.0 / 12.0) * mass * ((sizeX * sizeX) + (sizeY * sizeY));

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
        return profile.deriveStrikeGeometry().stability();
    }

    @Override
    public double strikeStructuralEfficiency() {
        return profile.deriveStrikeComplianceFactor();
    }

    @Override
    public double strikeIncidenceEfficiency() {
        return profile.deriveStrikeGlancingFactor();
    }
}