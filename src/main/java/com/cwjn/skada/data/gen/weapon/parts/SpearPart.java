package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile;
import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile.Station;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Spear head part based on {@link SpearHeadProfile}.
 *
 * Local profile orientation follows the spear schema: +X runs from shoulder toward tip.
 */
public final class SpearPart implements WeaponPart, ThrustCapable, SlashCapable {

    public static final String TYPE = "spear";

    public static final Codec<SpearPart> CODEC = createCodec();

    private final SpearHeadProfile profile;

    public SpearPart(SpearHeadProfile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    public static Codec<SpearPart> codec() {
        return createCodec();
    }

    private static Codec<SpearPart> createCodec() {
        return RecordCodecBuilder.create(instance -> instance.group(
                SpearHeadProfile.CODEC.fieldOf("spear").forGetter(SpearPart::profile)
        ).apply(instance, SpearPart::new));
    }

    public SpearHeadProfile profile() {
        return profile;
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        return SpearHeadProfile.computeMassProperties(profile, densityGPerCm3, samples);
    }

    @Override
    public GeometryUtil.Bounds localBounds(int samples) {
        return SpearHeadProfile.localBounds(profile, samples);
    }

    @Override
    public double momentOfInertiaAboutCenterOfMass(WeaponAxis axis, double densityGPerCm3, int samples) {
        if (samples < 4) {
            throw new IllegalArgumentException("samples must be >= 4");
        }
        if (densityGPerCm3 <= 0.0) {
            throw new IllegalArgumentException("density must be > 0");
        }

        int steps = Math.max(8, samples);
        MassProperties props = profile.computeMassProperties(densityGPerCm3, steps);
        Vec3 localCom = props.centerOfMass();

        double dl = profile.length() / steps;
        double inertia = 0.0;
        double bladeMass = 0.0;
        Vec3 bladeWeighted = new Vec3(0.0, 0.0, 0.0);

        for (int i = 0; i < steps; i++) {
            double s = (i + 0.5) / steps;
            SpearHeadProfile.SpearSlice slice = profile.sampleSliceAt(s);
            double mass = slice.area() * dl * densityGPerCm3;
            bladeMass += mass;
            bladeWeighted = bladeWeighted.add(slice.position().mul(mass));

            Vec3 relativePos = relative(slice.position(), localCom);
            double r2 = distanceSquaredToAxis(relativePos, axis);
            double centroidI = centroidInertia(axis, mass, dl, slice.width(), slice.thickness());
            inertia += centroidI + (mass * r2);
        }

        double residualMass = Math.max(0.0, props.massG() - bladeMass);
        if (residualMass > 0.0) {
            Vec3 residualCenter = props.centerOfMass().mul(props.massG()).add(bladeWeighted.mul(-1.0)).mul(1.0 / residualMass);
            inertia += residualMass * distanceSquaredToAxis(relative(residualCenter, localCom), axis);
        }

        return inertia;
    }

    @Override
    public SlashCapable.CrossSection crossSectionAt(double normalizedPosition) {
        double s = Math.max(0.0, Math.min(1.0, normalizedPosition));
        SpearHeadProfile.SpearSlice slice = profile.sampleSliceAt(s);
        Station station = profile.sampleStationAt(s);

        double halfWidth = Math.max(1.0e-6, slice.width() * 0.5);
        double bevelRunScale = Math.max(1.0e-3, 1.0 - station.section().midribFlat());
        double effectiveWidth = Math.max(1.0e-6, halfWidth * bevelRunScale);
        double halfThickness = Math.max(0.0, slice.thickness() * 0.5);
        return new SlashCapable.CrossSection(effectiveWidth, halfThickness, Math.max(1.0e-6, station.section().r()));
    }

    @Override
    public Optional<Double> edgeBevelAngle() {
        if (profile.getEdgeBevel() <= 0.0) {
            return Optional.empty();
        }
        return Optional.of(profile.getEdgeBevel());
    }

    @Override
    public double edgeRadiusNm() {
        return Math.max(1.0e-6, profile.getEdgeRadiusNm());
    }

    @Override
    public double normalizedSlashStrikePointOnPart(int samples) {
        int steps = Math.max(24, Math.min(256, samples / 4));
        double bestS = Math.max(profile.getSharpenedRange().s0(), 0.66);
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i <= steps; i++) {
            double s = (double) i / steps;
            SpearHeadProfile.SpearSlice slice = profile.sampleSliceAt(s);
            if (!slice.sharpened()) {
                continue;
            }
            double xNorm = sToXNorm(s, steps);
            double score = Math.max(0.0, slice.area()) * xNorm;
            if (score > bestScore) {
                bestScore = score;
                bestS = s;
            }
        }

        return sToXNorm(bestS, steps);
    }

    @Override
    public double pointTaper() {
        return profile.getPointTaper();
    }

    @Override
    public double tipLengthCm() {
        return profile.tipLengthCm();
    }

    @Override
    public double widthAtPointBase() {
        return Math.max(1.0e-6, profile.sampleSliceAt(tipBaseNormalizedPosition()).width());
    }

    @Override
    public double thicknessAtPointBase() {
        return Math.max(1.0e-6, profile.sampleSliceAt(tipBaseNormalizedPosition()).thickness());
    }

    @Override
    public double tipRadiusNm() {
        if (profile.getTipRadiusNm() != null) {
            return Math.max(1.0e-6, profile.getTipRadiusNm());
        }
        double taper = pointTaper();
        return Math.max(2.0, 60.0 - 54.0 * taper);
    }

    @Override
    public double normalizedThrustContactPointOnPart(int samples) {
        return 1.0;
    }

    private List<Station> sortedStations() {
        List<Station> sorted = new ArrayList<>(profile.getStations());
        sorted.sort(Comparator.comparingDouble(Station::s));
        return sorted;
    }

    private double tipBaseNormalizedPosition() {
        List<Station> sorted = sortedStations();
        return sorted.get(sorted.size() - 1).s();
    }

    private double sToXNorm(double s, int samples) {
        int steps = Math.max(16, samples);
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;

        for (int i = 0; i <= steps; i++) {
            double sampleS = (double) i / steps;
            double x = profile.sampleSliceAt(sampleS).position().x();
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
        }

        if (!Double.isFinite(minX) || !Double.isFinite(maxX) || (maxX - minX) <= 1.0e-6) {
            return Math.max(0.0, Math.min(1.0, s));
        }

        double x = profile.sampleSliceAt(Math.max(0.0, Math.min(1.0, s))).position().x();
        return Math.max(0.0, Math.min(1.0, (x - minX) / (maxX - minX)));
    }

    private static Vec3 relative(Vec3 position, Vec3 centerOfMass) {
        return new Vec3(
                position.x() - centerOfMass.x(),
                position.y() - centerOfMass.y(),
                position.z() - centerOfMass.z());
    }

    private static double distanceSquaredToAxis(Vec3 pos, WeaponAxis axis) {
        return switch (axis) {
            case X -> (pos.y() * pos.y()) + (pos.z() * pos.z());
            case Y -> (pos.x() * pos.x()) + (pos.z() * pos.z());
            case Z -> (pos.x() * pos.x()) + (pos.y() * pos.y());
        };
    }

    private static double centroidInertia(WeaponAxis axis, double mass, double dl, double width, double thickness) {
        return switch (axis) {
            case X -> (1.0 / 12.0) * mass * (width * width + thickness * thickness);
            case Y -> (1.0 / 12.0) * mass * (dl * dl + thickness * thickness);
            case Z -> (1.0 / 12.0) * mass * (dl * dl + width * width);
        };
    }
}