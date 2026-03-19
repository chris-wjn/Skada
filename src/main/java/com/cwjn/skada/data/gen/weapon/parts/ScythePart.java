package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.profile.ScytheProfile;
import com.cwjn.skada.data.gen.weapon.profile.ScytheProfile.Station;
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
 * Scythe head part based on {@link ScytheProfile}.
 *
 * The local profile uses +X from heel toward toe, with +Y as in-plane sweep.
 */
public final class ScythePart implements WeaponPart, SlashCapable, ThrustCapable {

    public static final String TYPE = "scythe";

    public static final Codec<ScythePart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ScytheProfile.CODEC.fieldOf("scythe").forGetter(ScythePart::profile)
    ).apply(instance, ScythePart::new));

    private final ScytheProfile profile;

        public ScythePart(ScytheProfile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    public ScytheProfile profile() {
        return profile;
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        return ScytheProfile.computeMassProperties(profile, densityGPerCm3, samples);
    }

    @Override
    public GeometryUtil.Bounds localBounds(int samples) {
        int steps = Math.max(8, samples);

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (int i = 0; i <= steps; i++) {
            double s = (double) i / steps;
            ScytheProfile.ScytheSlice slice = profile.sampleSliceAt(s);
            Vec3 p = slice.position();
            double halfW = slice.width() * 0.5;
            double halfT = slice.thickness() * 0.5;

            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y() - halfW);
            maxY = Math.max(maxY, p.y() + halfW);
            minZ = Math.min(minZ, p.z() - halfT);
            maxZ = Math.max(maxZ, p.z() + halfT);
        }

        // Include mount envelope so socket/tang mass isn't clipped from bounds.
        ScytheProfile.Mount mount = profile.getMount();
        Vec3 heel = profile.getSpine().get(0);
        Vec3 mountDir = mountDirection();
        Vec3 mountEnd = heel.add(mountDir.mul(mount.length()));
        double halfW = mountHalfWidth();
        double halfT = mountHalfThickness();
        double radial = Math.max(halfW, halfT);

        minX = Math.min(minX, Math.min(heel.x(), mountEnd.x()) - radial);
        maxX = Math.max(maxX, Math.max(heel.x(), mountEnd.x()) + radial);
        minY = Math.min(minY, Math.min(heel.y(), mountEnd.y()) - radial);
        maxY = Math.max(maxY, Math.max(heel.y(), mountEnd.y()) + radial);
        minZ = Math.min(minZ, heel.z() - halfT);
        maxZ = Math.max(maxZ, heel.z() + halfT);

        if (!Double.isFinite(minX)) {
            return GeometryUtil.Bounds.zero();
        }
        return new GeometryUtil.Bounds(minX, maxX, minY, maxY, minZ, maxZ);
    }

    @Override
    public double momentOfInertiaAboutCenterOfMass(WeaponAxis axis, double densityGPerCm3, int samples) {
        if (samples < 4) {
            throw new IllegalArgumentException("samples must be >= 4");
        }
        if (densityGPerCm3 <= 0.0) {
            throw new IllegalArgumentException("density must be > 0");
        }

        int steps = Math.max(4, samples);
        List<Station> sortedStations = sortedStations();
        MassProperties props = profile.computeMassProperties(densityGPerCm3, steps);
        Vec3 localCom = props.centerOfMass();

        double dl = profile.length() / steps;

        double inertia = 0.0;
        double bladeMass = 0.0;

        for (int i = 0; i < steps; i++) {
            double s = (i + 0.5) / steps;
            ScytheProfile.ScytheSlice slice = ScytheProfile.sampleSlice(profile, s);
            double mass = slice.area() * dl * densityGPerCm3;
            bladeMass += mass;

            Vec3 localPos = slice.position();
            Vec3 relativePos = new Vec3(
                    localPos.x() - localCom.x(),
                    localPos.y() - localCom.y(),
                    localPos.z() - localCom.z());
            double r2 = distanceSquaredToAxis(relativePos, axis);
            double centroidI = centroidInertia(axis, mass, dl, slice.width(), slice.thickness());
            inertia += centroidI + (mass * r2);
        }

        // Add mount/neck residual mass not represented by exposed-blade slices.
        double residualMass = Math.max(0.0, props.massG() - bladeMass);
        if (residualMass > 0.0) {
            Vec3 heel = profile.getSpine().get(0);
            Vec3 relativeHeel = new Vec3(
                    heel.x() - localCom.x(),
                    heel.y() - localCom.y(),
                    heel.z() - localCom.z());
            inertia += residualMass * distanceSquaredToAxis(relativeHeel, axis);
        }

        return inertia;
    }

    @Override
    public SlashCapable.CrossSection crossSectionAt(double normalizedPosition) {
        double s = Math.max(0.0, Math.min(1.0, normalizedPosition));
        ScytheProfile.ScytheSlice slice = profile.sampleSliceAt(s);
        Station station = profile.sampleStationAt(s);

        double fullWidth = Math.max(1.0e-6, slice.width());
        double halfWidth = fullWidth * 0.5;
        double spineShift = Math.abs(Math.max(-1.0, Math.min(1.0, station.section().edgeOffset())));
        double spineFlat = Math.max(0.0, Math.min(1.0, station.section().spineFlat()));
        double bevelRunScale = Math.max(1.0e-3, 1.0 - spineFlat);
        double effectiveWidth = Math.max(1.0e-6, halfWidth * bevelRunScale * (1.0 + spineShift));
        double halfThickness = Math.max(0.0, slice.thickness() * 0.5);
        double r = Math.max(1.0e-6, station.section().r());

        return new SlashCapable.CrossSection(effectiveWidth, halfThickness, r);
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
        Double preferredS = profile.getPreferredStrikeS();
        if (preferredS != null) {
            return sToXNorm(Math.max(0.0, Math.min(1.0, preferredS)), samples);
        }

        int steps = Math.max(24, Math.min(256, samples / 4));
        double bestS = 0.72;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i <= steps; i++) {
            double s = (double) i / steps;
            ScytheProfile.ScytheSlice slice = profile.sampleSliceAt(s);
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
        double s = tipBaseNormalizedPosition();
        return Math.max(1.0e-6, profile.sampleSliceAt(s).width());
    }

    @Override
    public double thicknessAtPointBase() {
        double s = tipBaseNormalizedPosition();
        return Math.max(1.0e-6, profile.sampleSliceAt(s).thickness());
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

    private Vec3 mountDirection() {
        Vec3 p0 = profile.getSpine().get(0);
        Vec3 p1 = profile.getSpine().get(1);

        double fx = p1.x() - p0.x();
        double fy = p1.y() - p0.y();
        double len = Math.sqrt((fx * fx) + (fy * fy));

        double baseAngle = len > 1.0e-9 ? Math.atan2(fy, fx) : 0.0;
        double theta = baseAngle - Math.toRadians(profile.getMount().angleDegrees());
        return new Vec3(Math.cos(theta), Math.sin(theta), 0.0);
    }

    private double mountHalfWidth() {
        ScytheProfile.Mount mount = profile.getMount();
        if ("socket".equals(mount.type())) {
            return mount.outerWidth() * 0.5;
        }
        return mount.width() * 0.5;
    }

    private double mountHalfThickness() {
        ScytheProfile.Mount mount = profile.getMount();
        if ("socket".equals(mount.type())) {
            return mount.outerThickness() * 0.5;
        }
        return mount.thickness() * 0.5;
    }

    private static double distanceSquaredToAxis(Vec3 pos, WeaponAxis axis) {
        return switch (axis) {
            case X -> (pos.y() * pos.y()) + (pos.z() * pos.z());
            case Y -> (pos.x() * pos.x()) + (pos.z() * pos.z());
            case Z -> (pos.x() * pos.x()) + (pos.y() * pos.y());
        };
    }

    private static double centroidInertia(WeaponAxis localAxis, double mass, double dl, double width, double thickness) {
        return switch (localAxis) {
            case X -> (1.0 / 12.0) * mass * (width * width + thickness * thickness);
            case Y -> (1.0 / 12.0) * mass * (dl * dl + thickness * thickness);
            case Z -> (1.0 / 12.0) * mass * (dl * dl + width * width);
        };
    }
}
