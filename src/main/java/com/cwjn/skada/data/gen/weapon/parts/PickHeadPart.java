package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Bore;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Eye;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Rear;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Spike;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Station;
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

/**
 * Pick head part based on {@link PickProfile}.
 *
 * Local profile orientation follows the pick schema: front spike points toward +X.
 */
public final class PickHeadPart implements WeaponPart, ThrustCapable, StrikeCapable {

    public static final String TYPE = "pick_head";

    public static final Codec<PickHeadPart> CODEC = createCodec();

    public static Codec<PickHeadPart> codec() {
        return createCodec();
    }

    private final PickProfile profile;

    public PickHeadPart(PickProfile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    private static Codec<PickHeadPart> createCodec() {
        return RecordCodecBuilder.create(instance -> instance.group(
                PickProfile.CODEC.fieldOf("pick").forGetter(PickHeadPart::profile)
        ).apply(instance, PickHeadPart::new));
    }

    public PickProfile profile() {
        return profile;
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public MassProperties massProperties(double densityGPerCm3, int samples) {
        return PickProfile.computeMassProperties(profile, densityGPerCm3, samples);
    }

    @Override
    public GeometryUtil.Bounds localBounds(int samples) {
        return PickProfile.localBounds(profile, samples);
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
        Vec3 localCom = profile.computeMassProperties(densityGPerCm3, steps).centerOfMass();
        double inertia = 0.0;

        inertia += accumulateEyeInertia(axis, densityGPerCm3, localCom);
        inertia += accumulateSpikeInertia(profile.getFront(), axis, densityGPerCm3, steps, localCom);

        Rear rear = profile.getRear();
        if ("spike".equals(rear.type())) {
            inertia += accumulateSpikeInertia(rear.spike(), axis, densityGPerCm3, steps, localCom);
        } else if ("hammer".equals(rear.type())) {
            inertia += accumulateHammerInertia(axis, densityGPerCm3, steps, localCom);
        }
        return inertia;
    }

    @Override
    public double pointTaper() {
        return profile.getFront().pointTaper();
    }

    @Override
    public double tipLengthCm() {
        return profile.frontLengthCm();
    }

    @Override
    public double widthAtPointBase() {
        return Math.max(1.0e-6, profile.sampleFrontSliceAt(frontTipBaseNormalizedPosition()).width());
    }

    @Override
    public double thicknessAtPointBase() {
        return Math.max(1.0e-6, profile.sampleFrontSliceAt(frontTipBaseNormalizedPosition()).thickness());
    }

    @Override
    public double tipRadiusNm() {
        Double tipRadius = profile.getFront().tipRadiusNm();
        if (tipRadius != null) {
            return Math.max(1.0e-6, tipRadius);
        }
        double taper = pointTaper();
        return Math.max(2.0, 60.0 - 54.0 * taper);
    }

    @Override
    public double normalizedThrustContactPointOnPart(int samples) {
        return 1.0;
    }

    @Override
    public ThrustMotionMode thrustMotionMode() {
        return ThrustMotionMode.ROTATIONAL;
    }

    @Override
    public double normalizedStrikeContactPointOnPart(int samples) {
        return 0.0;
    }

    @Override
    public double effectiveContactAreaCm2() {
        Rear rear = profile.getRear();
        if ("hammer".equals(rear.type())) {
            double baseArea = rear.faceWidth() * rear.faceHeight();
            double minFace = Math.min(rear.faceWidth(), rear.faceHeight());
            double crownFactor = rear.faceCrownRadiusCm() <= 0.0
                    ? 1.0
                    : clamp(rear.faceCrownRadiusCm() / (rear.faceCrownRadiusCm() + (0.5 * minFace)), 0.45, 1.0);
            double edgeFactor = clamp(0.68 + 0.03 * rear.edgeRadiusMm(), 0.65, 1.0);
            return Math.max(0.02, baseArea * crownFactor * edgeFactor);
        }

        if ("spike".equals(rear.type())) {
            PickProfile.SpikeSlice slice = profile.sampleRearSpikeSliceAt(rearTipBaseNormalizedPosition()).orElseThrow();
            return Math.max(0.02, 0.12 * slice.width() * slice.thickness());
        }

        Eye eye = profile.getEye();
        return Math.max(0.02, 0.55 * eye.height() * eye.thickness());
    }

    @Override
    public double strikeFaceGeometryFocus() {
        Rear rear = profile.getRear();
        if ("hammer".equals(rear.type())) {
            double baseArea = Math.max(1.0e-6, rear.faceWidth() * rear.faceHeight());
            double localization = baseArea / Math.max(1.0e-6, effectiveContactAreaCm2());
            return clamp(0.92 + 0.12 * localization, 0.95, 1.18);
        }
        if ("spike".equals(rear.type())) {
            return 1.25;
        }
        return 0.92;
    }

    @Override
    public double strikeHeadRigidity() {
        Rear rear = profile.getRear();
        if ("hammer".equals(rear.type())) {
            return 1.08;
        }
        if ("spike".equals(rear.type())) {
            return 1.12;
        }
        return 0.96;
    }

    @Override
    public double strikeAssemblyStability() {
        Rear rear = profile.getRear();
        if ("hammer".equals(rear.type())) {
            return 1.05;
        }
        if ("spike".equals(rear.type())) {
            return 0.96;
        }
        return 1.0;
    }

    private double accumulateEyeInertia(WeaponAxis axis, double densityGPerCm3, Vec3 localCom) {
        Eye eye = profile.getEye();
        double eyeVolume = eye.length() * eye.height() * eye.thickness();
        double eyeMass = eyeVolume * densityGPerCm3;
        Vec3 eyeCenter = new Vec3(eye.xOffset(), 0.0, 0.0);

        double ixx = (1.0 / 12.0) * eyeMass * (eye.height() * eye.height() + eye.thickness() * eye.thickness());
        double iyy = (1.0 / 12.0) * eyeMass * (eye.length() * eye.length() + eye.thickness() * eye.thickness());
        double izz = (1.0 / 12.0) * eyeMass * (eye.length() * eye.length() + eye.height() * eye.height());
        double inertia = mappedInertiaWithParallelAxis(axis, ixx, iyy, izz, eyeMass, eyeCenter, localCom);

        Bore bore = PickProfile.getClampedBore(eye);
        if (bore != null) {
            double boreMass = -(PickProfile.boreArea(bore) * eye.height()) * densityGPerCm3;
            double boreIxx;
            double boreIyy;
            double boreIzz;
            if ("ellipse".equals(bore.shape())) {
                double a = bore.width() / 2.0;
                double b = bore.thickness() / 2.0;
                boreIxx = boreMass * ((b * b) / 4.0 + (eye.height() * eye.height()) / 12.0);
                boreIyy = boreMass * ((a * a) + (b * b)) / 4.0;
                boreIzz = boreMass * ((a * a) / 4.0 + (eye.height() * eye.height()) / 12.0);
            } else {
                boreIxx = (1.0 / 12.0) * boreMass * (eye.height() * eye.height() + bore.thickness() * bore.thickness());
                boreIyy = (1.0 / 12.0) * boreMass * (bore.width() * bore.width() + bore.thickness() * bore.thickness());
                boreIzz = (1.0 / 12.0) * boreMass * (bore.width() * bore.width() + eye.height() * eye.height());
            }
            inertia += mappedInertiaWithParallelAxis(axis, boreIxx, boreIyy, boreIzz, boreMass, new Vec3(0.0, 0.0, 0.0), localCom);
        }

        return inertia;
    }

    private double accumulateSpikeInertia(Spike spike, WeaponAxis axis, double densityGPerCm3, int samples, Vec3 localCom) {
        double length = Math.max(1.0e-6, GeometryUtil.polylineLength(spike.centerline()));
        double dl = length / samples;
        double inertia = 0.0;

        for (int i = 0; i < samples; i++) {
            double s = (i + 0.5) / samples;
            PickProfile.SpikeSlice slice = PickProfile.sampleSpikeSlice(spike, s);
            double mass = slice.area() * dl * densityGPerCm3;
            double centroidI = centroidInertia(axis, mass, dl, slice.width(), slice.thickness());
            inertia += centroidI + mass * distanceSquaredToAxis(relative(slice.position(), localCom), axis);
        }

        return inertia;
    }

    private double accumulateHammerInertia(WeaponAxis axis, double densityGPerCm3, int samples, Vec3 localCom) {
        Rear rear = profile.getRear();
        Eye eye = profile.getEye();
        double dx = rear.length() / samples;
        double eyeSideScale = 1.0 - rear.taper();
        double inertia = 0.0;

        for (int i = 0; i < samples; i++) {
            double t = (i + 0.5) / samples;
            double scale = GeometryUtil.lerp(eyeSideScale, 1.0, t);
            double width = rear.faceWidth() * scale;
            double thickness = rear.faceHeight() * scale;
            double mass = width * thickness * dx * densityGPerCm3;
            double x = PickProfile.eyeMinX(eye) - ((i + 0.5) * dx);
            Vec3 slicePosition = new Vec3(x, 0.0, 0.0);
            double centroidI = centroidInertia(axis, mass, dx, width, thickness);
            inertia += centroidI + mass * distanceSquaredToAxis(relative(slicePosition, localCom), axis);
        }

        return inertia;
    }

    private double frontTipBaseNormalizedPosition() {
        List<Station> sorted = new ArrayList<>(profile.getFront().stations());
        sorted.sort(Comparator.comparingDouble(Station::s));
        return sorted.get(sorted.size() - 1).s();
    }

    private double rearTipBaseNormalizedPosition() {
        Spike rearSpike = profile.getRear().spike();
        List<Station> sorted = new ArrayList<>(rearSpike.stations());
        sorted.sort(Comparator.comparingDouble(Station::s));
        return sorted.get(sorted.size() - 1).s();
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

    private static double mappedInertiaWithParallelAxis(WeaponAxis axis, double ixx, double iyy, double izz,
            double mass, Vec3 center, Vec3 partCenter) {
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}