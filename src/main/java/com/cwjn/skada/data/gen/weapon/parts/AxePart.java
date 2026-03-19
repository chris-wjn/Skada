package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.data.gen.weapon.profile.AxeProfile;
import com.cwjn.skada.data.gen.weapon.profile.AxeProfile.AxeEdge;
import com.cwjn.skada.data.gen.weapon.profile.AxeProfile.AxeEye;
import com.cwjn.skada.data.gen.weapon.profile.AxeProfile.AxeEyeBore;
import com.cwjn.skada.data.gen.weapon.profile.AxeProfile.AxeLobe;
import com.cwjn.skada.data.gen.weapon.profile.AxeProfile.LobeKind;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec2;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.cwjn.skada.data.registry.AttackType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

/**
 * Axe head part based on {@link AxeProfile}.
 *
 * Local profile orientation follows the axe schema: edge points toward +X.
 */
public final class AxePart implements WeaponPart, SlashCapable {

    public static final String TYPE = "axe";

    public static final Codec<AxePart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AxeProfile.CODEC.fieldOf("axe").forGetter(AxePart::profile)).apply(instance, AxePart::new));

    private static final int CROSS_SECTION_SAMPLES = 160;

    private final AxeProfile profile;
    public AxePart(AxeProfile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    public AxeProfile profile() {
        return profile;
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        return AxeProfile.computeMassProperties(profile, densityGPerCm3, samples);
    }

    @Override
    public GeometryUtil.Bounds localBounds(int samples) {
        return AxeProfile.localBounds(profile, samples);
    }

    @Override
    public double momentOfInertiaAboutCenterOfMass(WeaponAxis axis, double densityGPerCm3, int samples) {
        if (samples < 4) {
            throw new IllegalArgumentException("samples must be >= 4");
        }
        if (densityGPerCm3 <= 0.0) {
            throw new IllegalArgumentException("density must be > 0");
        }

        AxeEye eye = profile.eye();
        AxeEdge edge = profile.edge();
        double edgeStart = AxeProfile.edgeStart(profile);
        Vec3 partCenter = AxeProfile.computeMassProperties(profile, densityGPerCm3, samples).centerOfMass();

        double inertia = 0.0;

        AxeEyeBore bore = AxeProfile.getClampedEyeBore(eye);
        double eyeVolume = eye.length() * eye.height() * eye.thickness();
        double eyeMass = eyeVolume * densityGPerCm3;

        Vec3 eyeCenterLocal = new Vec3(AxeProfile.eyeCenterX(eye), 0.0, 0.0);
        if (eyeMass > 0.0) {
            double ixx = (1.0 / 12.0) * eyeMass * (eye.height() * eye.height() + eye.thickness() * eye.thickness());
            double iyy = (1.0 / 12.0) * eyeMass * (eye.length() * eye.length() + eye.thickness() * eye.thickness());
            double izz = (1.0 / 12.0) * eyeMass * (eye.length() * eye.length() + eye.height() * eye.height());
            inertia += mappedInertiaWithParallelAxis(axis, ixx, iyy, izz, eyeMass, eyeCenterLocal, partCenter);
        }

        if (bore != null && eyeVolume > 0.0) {
            double boreMass = -(AxeProfile.boreArea(bore) * eye.height()) * densityGPerCm3;
            Vec3 boreCenterLocal = new Vec3(0.0, 0.0, 0.0);
            double ixx;
            double iyy;
            double izz;
            if ("circle".equals(bore.shape())) {
                double radius = bore.width() / 2.0;
                double height = eye.height();
                ixx = (1.0 / 12.0) * boreMass * (3.0 * radius * radius + height * height);
                iyy = (1.0 / 2.0) * boreMass * (radius * radius);
                izz = (1.0 / 12.0) * boreMass * (3.0 * radius * radius + height * height);
            } else {
                ixx = (1.0 / 12.0) * boreMass * (eye.height() * eye.height() + bore.thickness() * bore.thickness());
                iyy = (1.0 / 12.0) * boreMass * (bore.width() * bore.width() + bore.thickness() * bore.thickness());
                izz = (1.0 / 12.0) * boreMass * (bore.width() * bore.width() + eye.height() * eye.height());
            }
                inertia += mappedInertiaWithParallelAxis(axis, ixx, iyy, izz, boreMass, boreCenterLocal, partCenter);
        }

            inertia += accumulateLobeInertia(axis, edge, edge.core(), samples, densityGPerCm3,
                LobeKind.CORE,
                edge.core().height(), edgeStart, partCenter);
            inertia += accumulateLobeInertia(axis, edge, edge.top(), samples, densityGPerCm3,
                LobeKind.TOP,
                edge.core().height(), edgeStart, partCenter);
            inertia += accumulateLobeInertia(axis, edge, edge.bottom(), samples, densityGPerCm3,
                LobeKind.BOTTOM,
                edge.core().height(), edgeStart, partCenter);

        return inertia;
    }

    @Override
    public double idealPointOfBalanceSlash() {
        double normalized = 0.62;
        GeometryUtil.Bounds bounds = AxeProfile.localBounds(profile, 64);
        double s = Math.max(0.0, Math.min(1.0, normalized));
        double localX = bounds.minX() + (bounds.maxX() - bounds.minX()) * s;
        return localX;
    }

    @Override
    public double normalizedSlashStrikePointOnPart(int samples) {
        return 0.92;
    }

    @Override
    public SlashCapable.CrossSection crossSectionAt(double normalizedPosition) {
        double s = Math.max(0.0, Math.min(1.0, normalizedPosition));
        Vec2 range = AxeProfile.sectionRange(profile);
        double ySlice = GeometryUtil.lerp(range.x(), range.y(), s);

        AxeEye eye = profile.eye();
        AxeEdge edge = profile.edge();
        double edgeStart = AxeProfile.edgeStart(profile);
        double minX = AxeProfile.eyeMinX(eye);
        double maxX = edgeStart + edge.length();
        double dx = (maxX - minX) / CROSS_SECTION_SAMPLES;

        double occupiedMinX = Double.POSITIVE_INFINITY;
        double occupiedMaxX = Double.NEGATIVE_INFINITY;
        double maxHalfThickness = 0.0;
        double curve = edge.core().crossSection().curve();

        for (int i = 0; i <= CROSS_SECTION_SAMPLES; i++) {
            double x = minX + (i * dx);
            Vec2 sample = AxeProfile.materialHalfThicknessAt(profile, x, ySlice);
            if (sample.x() <= 0.0) {
                continue;
            }
            occupiedMinX = Math.min(occupiedMinX, x);
            occupiedMaxX = Math.max(occupiedMaxX, x);
            if (sample.x() > maxHalfThickness) {
                maxHalfThickness = sample.x();
                curve = sample.y();
            }
        }

        if (!Double.isFinite(occupiedMinX) || !Double.isFinite(occupiedMaxX) || maxHalfThickness <= 0.0) {
            return new SlashCapable.CrossSection(1.0e-6, 0.0, Math.max(1.0e-6, curve));
        }

        double effectiveWidth = Math.max(1.0e-6, occupiedMaxX - occupiedMinX);
        return new SlashCapable.CrossSection(effectiveWidth, maxHalfThickness, Math.max(1.0e-6, curve));
    }

    @Override
    public Optional<Double> edgeBevelAngle() {
        return Optional.empty();
    }

    @Override
    public double edgeRadiusNm() {
        return 5.0;
    }

    private double accumulateLobeInertia(
            WeaponAxis axis,
            AxeEdge edge,
            AxeLobe lobe,
            int samples,
            double densityGPerCm3,
            LobeKind kind,
            double coreHeight,
            double edgeStart,
            Vec3 partCenter) {
        double length = edge.length();
        double dx = length / samples;
        double totalInertia = 0.0;

        for (int i = 0; i < samples; i++) {
            double xLocal = (i + 0.5) * dx;
            double height = AxeProfile.lobeHeight(edge, lobe, xLocal, kind, edgeStart);
            if (height <= 0.0) {
                continue;
            }
            double zHalf = AxeProfile.halfThicknessAt(lobe.crossSection(), xLocal, length);
            if (zHalf <= 0.0) {
                continue;
            }

            double thickness = 2.0 * zHalf;
            double area = height * thickness;
            double mass = area * dx * densityGPerCm3;

            double yMid = AxeProfile.lobeCenterY(kind, coreHeight, height);
            Vec3 centroidLocal = new Vec3(edgeStart + xLocal, yMid, 0.0);

            double ixx = (1.0 / 12.0) * mass * (height * height + thickness * thickness);
            double iyy = (1.0 / 12.0) * mass * (dx * dx + thickness * thickness);
            double izz = (1.0 / 12.0) * mass * (dx * dx + height * height);

            totalInertia += mappedInertiaWithParallelAxis(axis, ixx, iyy, izz, mass, centroidLocal, partCenter);
        }

        return totalInertia;
    }

    private static double mappedInertiaWithParallelAxis(
            WeaponAxis axis,
            double ixx,
            double iyy,
            double izz,
            double mass,
            Vec3 center,
            Vec3 partCenter) {
        double iLocal = switch (axis) {
            case X -> ixx;
            case Y -> iyy;
            case Z -> izz;
        };

        double dx = center.x() - partCenter.x();
        double dy = center.y() - partCenter.y();
        double dz = center.z() - partCenter.z();

        return switch (axis) {
            case X -> iLocal + mass * (dy * dy + dz * dz);
            case Y -> iLocal + mass * (dx * dx + dz * dz);
            case Z -> iLocal + mass * (dx * dx + dy * dy);
        };
    }

}
