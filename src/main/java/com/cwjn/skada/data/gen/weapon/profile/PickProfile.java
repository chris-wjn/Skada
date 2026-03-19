package com.cwjn.skada.data.gen.weapon.profile;

import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Bounds;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Geometry and runtime physics model for pick head profiles.
 *
 * Units: centimeters for geometry, grams for mass, g/cm^3 for density.
 */
public final class PickProfile {

  private static final int CURRENT_VERSION = 1;
  private static final double BORE_CLAMP_EPSILON = 1.0e-3;

  public static final Codec<PickProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("version").forGetter(PickProfile::getVersion),
      Eye.CODEC.fieldOf("eye").forGetter(PickProfile::getEye),
      Spike.CODEC.fieldOf("front").forGetter(PickProfile::getFront),
      Rear.CODEC.fieldOf("rear").forGetter(PickProfile::getRear))
      .apply(instance, PickProfile::new));

  private final int version;
  private final Eye eye;
  private final Spike front;
  private final Rear rear;

  private record ComponentMass(double volumeCm3, Vec3 center) {
  }

  public record Bore(double width, double thickness, String shape) {
    public static final Codec<Bore> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("width").forGetter(Bore::width),
        Codec.DOUBLE.fieldOf("thickness").forGetter(Bore::thickness),
        Codec.STRING.optionalFieldOf("shape", "ellipse").forGetter(Bore::shape)).apply(instance, Bore::new));

    public Bore {
      if (width <= 0.0) {
        throw new IllegalArgumentException("bore.width must be > 0");
      }
      if (thickness <= 0.0) {
        throw new IllegalArgumentException("bore.thickness must be > 0");
      }
      if (!"ellipse".equals(shape) && !"rect".equals(shape)) {
        throw new IllegalArgumentException("bore.shape must be \"ellipse\" or \"rect\"");
      }
    }
  }

  public record Eye(double length, double height, double thickness, double xOffset, Bore bore) {
    public static final Codec<Eye> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("length").forGetter(Eye::length),
        Codec.DOUBLE.fieldOf("height").forGetter(Eye::height),
        Codec.DOUBLE.fieldOf("thickness").forGetter(Eye::thickness),
        Codec.DOUBLE.optionalFieldOf("xOffset", 0.0).forGetter(Eye::xOffset),
        Bore.CODEC.optionalFieldOf("bore").forGetter(eye -> Optional.ofNullable(eye.bore())))
        .apply(instance, (length, height, thickness, xOffset, bore) -> new Eye(length, height, thickness, xOffset,
            bore.orElse(null))));

    public Eye {
      if (length <= 0.0 || height <= 0.0 || thickness <= 0.0) {
        throw new IllegalArgumentException("eye dimensions must be > 0");
      }
    }
  }

  public record Section(String kind, double r) {
    public static final Codec<Section> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("kind", "diamond").forGetter(Section::kind),
        Codec.DOUBLE.fieldOf("r").forGetter(Section::r)).apply(instance, Section::new));

    public Section {
      if (!"diamond".equals(kind) && !"lenticular".equals(kind) && !"squareDiamond".equals(kind)) {
        throw new IllegalArgumentException(
            "section.kind must be \"diamond\", \"lenticular\", or \"squareDiamond\"");
      }
      if (r <= 0.0) {
        throw new IllegalArgumentException("section.r must be > 0");
      }
    }
  }

  public record Station(double s, double width, double thickness, Section section) {
    public static final Codec<Station> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("s").forGetter(Station::s),
        Codec.DOUBLE.fieldOf("width").forGetter(Station::width),
        Codec.DOUBLE.fieldOf("thickness").forGetter(Station::thickness),
        Section.CODEC.fieldOf("section").forGetter(Station::section)).apply(instance, Station::new));

    public Station {
      if (s < 0.0 || s > 1.0) {
        throw new IllegalArgumentException("station.s must be in [0,1]");
      }
      if (width <= 0.0 || thickness <= 0.0) {
        throw new IllegalArgumentException("station width/thickness must be > 0");
      }
      Objects.requireNonNull(section, "section");
    }
  }

  public record Spike(List<Vec3> centerline, double pointTaper, Double tipRadiusNm, List<Station> stations) {
    public static final Codec<Spike> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Vec3.CODEC.listOf().fieldOf("centerline").forGetter(Spike::centerline),
        Codec.DOUBLE.fieldOf("pointTaper").forGetter(Spike::pointTaper),
        Codec.DOUBLE.optionalFieldOf("tipRadiusNm").forGetter(spike -> Optional.ofNullable(spike.tipRadiusNm())),
        Station.CODEC.listOf().fieldOf("stations").forGetter(Spike::stations))
        .apply(instance, (centerline, pointTaper, tipRadiusNm, stations) -> new Spike(centerline, pointTaper,
            tipRadiusNm.orElse(null), stations)));

    public Spike {
      centerline = List.copyOf(Objects.requireNonNull(centerline, "centerline"));
      stations = List.copyOf(Objects.requireNonNull(stations, "stations"));

      if (centerline.size() < 2) {
        throw new IllegalArgumentException("spike centerline must have at least 2 points");
      }
      if (stations.size() < 2) {
        throw new IllegalArgumentException("spike stations must have at least 2 entries");
      }
      if (pointTaper < 0.0 || pointTaper > 1.0) {
        throw new IllegalArgumentException("pointTaper must be in [0,1]");
      }
      if (tipRadiusNm != null && tipRadiusNm <= 0.0) {
        throw new IllegalArgumentException("tipRadiusNm must be > 0 when provided");
      }

      double previousS = -1.0;
      for (Station station : stations) {
        if (station.s() < previousS) {
          throw new IllegalArgumentException("station s values must be monotonic increasing");
        }
        previousS = station.s();
      }
    }
  }

  public record Rear(
      String type,
      Double length,
      Double faceWidth,
      Double faceHeight,
      Double faceCrownRadiusCm,
      Double edgeRadiusMm,
      Double taper,
      Spike spike) {
    public static final Codec<Rear> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("type").forGetter(Rear::type),
        Codec.DOUBLE.optionalFieldOf("length").forGetter(rear -> Optional.ofNullable(rear.length())),
        Codec.DOUBLE.optionalFieldOf("faceWidth").forGetter(rear -> Optional.ofNullable(rear.faceWidth())),
        Codec.DOUBLE.optionalFieldOf("faceHeight").forGetter(rear -> Optional.ofNullable(rear.faceHeight())),
        Codec.DOUBLE.optionalFieldOf("faceCrownRadiusCm")
            .forGetter(rear -> Optional.ofNullable(rear.faceCrownRadiusCm())),
        Codec.DOUBLE.optionalFieldOf("edgeRadiusMm").forGetter(rear -> Optional.ofNullable(rear.edgeRadiusMm())),
        Codec.DOUBLE.optionalFieldOf("taper").forGetter(rear -> Optional.ofNullable(rear.taper())),
        Spike.CODEC.optionalFieldOf("spike").forGetter(rear -> Optional.ofNullable(rear.spike())))
        .apply(instance,
            (type, length, faceWidth, faceHeight, faceCrownRadiusCm, edgeRadiusMm, taper, spike) -> new Rear(
                type,
                length.orElse(null),
                faceWidth.orElse(null),
                faceHeight.orElse(null),
                faceCrownRadiusCm.orElse(null),
                edgeRadiusMm.orElse(null),
                taper.orElse(null),
                spike.orElse(null))));

    public Rear {
      Objects.requireNonNull(type, "type");
      if (!"hammer".equals(type) && !"spike".equals(type) && !"none".equals(type)) {
        throw new IllegalArgumentException("rear.type must be \"hammer\", \"spike\", or \"none\"");
      }

      if ("hammer".equals(type)) {
        requirePositive(length, "rear.length");
        requirePositive(faceWidth, "rear.faceWidth");
        requirePositive(faceHeight, "rear.faceHeight");
        requireNonNegative(faceCrownRadiusCm, "rear.faceCrownRadiusCm");
        requireNonNegative(edgeRadiusMm, "rear.edgeRadiusMm");
        if (taper == null || taper < 0.0 || taper >= 1.0) {
          throw new IllegalArgumentException("rear.taper must be in [0,1)");
        }
        if (spike != null) {
          throw new IllegalArgumentException("rear.spike must be omitted when rear.type is \"hammer\"");
        }
      }

      if ("spike".equals(type)) {
        if (spike == null) {
          throw new IllegalArgumentException("rear.spike must be provided when rear.type is \"spike\"");
        }
        if (length != null || faceWidth != null || faceHeight != null || faceCrownRadiusCm != null
            || edgeRadiusMm != null || taper != null) {
          throw new IllegalArgumentException("hammer-only rear fields must be omitted when rear.type is \"spike\"");
        }
      }

      if ("none".equals(type)) {
        if (length != null || faceWidth != null || faceHeight != null || faceCrownRadiusCm != null
            || edgeRadiusMm != null || taper != null || spike != null) {
          throw new IllegalArgumentException("rear.type \"none\" must omit hammer/spike-only fields");
        }
      }
    }

    private static void requirePositive(Double value, String name) {
      if (value == null || value <= 0.0) {
        throw new IllegalArgumentException(name + " must be > 0");
      }
    }

    private static void requireNonNegative(Double value, String name) {
      if (value == null || value < 0.0) {
        throw new IllegalArgumentException(name + " must be >= 0");
      }
    }
  }

  public record SpikeSlice(double width, double thickness, double area, Vec3 position) {
  }

  public PickProfile(int version, Eye eye, Spike front, Rear rear) {
    this.version = version;
    this.eye = Objects.requireNonNull(eye, "eye");
    this.front = Objects.requireNonNull(front, "front");
    this.rear = Objects.requireNonNull(rear, "rear");

    if (version != CURRENT_VERSION) {
      throw new IllegalArgumentException("version must be " + CURRENT_VERSION);
    }

    Vec3 frontBase = this.front.centerline().get(0);
    Vec3 frontTip = this.front.centerline().get(this.front.centerline().size() - 1);
    if (frontTip.x() <= frontBase.x()) {
      throw new IllegalArgumentException("front centerline must progress toward +x");
    }

    if ("spike".equals(this.rear.type())) {
      Vec3 rearBase = this.rear.spike().centerline().get(0);
      Vec3 rearTip = this.rear.spike().centerline().get(this.rear.spike().centerline().size() - 1);
      if (rearTip.x() >= rearBase.x()) {
        throw new IllegalArgumentException("rear spike centerline must progress toward -x");
      }
    }

    Bore clamped = getClampedBore(this.eye);
    if (this.eye.bore() != null && clamped == null) {
      throw new IllegalArgumentException("eye bore does not fit inside the eye at the origin-centered bore position");
    }
  }

  public static MassProperties computeMassProperties(PickProfile profile, double densityGPerCm3, int samples) {
    Objects.requireNonNull(profile, "profile");
    if (densityGPerCm3 <= 0.0) {
      throw new IllegalArgumentException("density must be > 0");
    }

    int steps = Math.max(8, samples);
    ComponentMass eyeMass = computeEyeMass(profile.eye);
    ComponentMass frontMass = computeSpikeMass(profile.front, steps);
    ComponentMass rearMass = computeRearMass(profile, steps);

    double totalVolume = eyeMass.volumeCm3() + frontMass.volumeCm3() + rearMass.volumeCm3();
    Vec3 weighted = eyeMass.center().mul(eyeMass.volumeCm3())
        .add(frontMass.center().mul(frontMass.volumeCm3()))
      .add(rearMass.center().mul(rearMass.volumeCm3()));

    Vec3 center = totalVolume > 0.0 ? weighted.mul(1.0 / totalVolume) : new Vec3(0.0, 0.0, 0.0);
    return new MassProperties(totalVolume, totalVolume * densityGPerCm3, center);
  }

  public MassProperties computeMassProperties(double densityGPerCm3, int samples) {
    return computeMassProperties(this, densityGPerCm3, samples);
  }

  public static Bounds localBounds(PickProfile profile, int samples) {
    Objects.requireNonNull(profile, "profile");

    double minX = eyeMinX(profile.eye);
    double maxX = eyeMaxX(profile.eye);
    double minY = -profile.eye.height() / 2.0;
    double maxY = profile.eye.height() / 2.0;
    double minZ = -profile.eye.thickness() / 2.0;
    double maxZ = profile.eye.thickness() / 2.0;

    Bounds frontBounds = spikeBounds(profile.front, Math.max(16, samples));
    minX = Math.min(minX, frontBounds.minX());
    maxX = Math.max(maxX, frontBounds.maxX());
    minY = Math.min(minY, frontBounds.minY());
    maxY = Math.max(maxY, frontBounds.maxY());
    minZ = Math.min(minZ, frontBounds.minZ());
    maxZ = Math.max(maxZ, frontBounds.maxZ());

    Bounds rearBounds = rearBounds(profile, Math.max(16, samples));
    minX = Math.min(minX, rearBounds.minX());
    maxX = Math.max(maxX, rearBounds.maxX());
    minY = Math.min(minY, rearBounds.minY());
    maxY = Math.max(maxY, rearBounds.maxY());
    minZ = Math.min(minZ, rearBounds.minZ());
    maxZ = Math.max(maxZ, rearBounds.maxZ());

    return new Bounds(minX, maxX, minY, maxY, minZ, maxZ);
  }

  public Bounds localBounds(int samples) {
    return localBounds(this, samples);
  }

  public SpikeSlice sampleFrontSliceAt(double s) {
    return sampleSpikeSlice(front, s);
  }

  public Optional<SpikeSlice> sampleRearSpikeSliceAt(double s) {
    if (!"spike".equals(rear.type())) {
      return Optional.empty();
    }
    return Optional.of(sampleSpikeSlice(rear.spike(), s));
  }

  public double frontLengthCm() {
    return GeometryUtil.polylineLength(front.centerline());
  }

  public Optional<Double> rearSpikeLengthCm() {
    if (!"spike".equals(rear.type())) {
      return Optional.empty();
    }
    return Optional.of(GeometryUtil.polylineLength(rear.spike().centerline()));
  }

  public int getVersion() {
    return version;
  }

  public Eye getEye() {
    return eye;
  }

  public Spike getFront() {
    return front;
  }

  public Rear getRear() {
    return rear;
  }

  public static double eyeMinX(Eye eye) {
    Objects.requireNonNull(eye, "eye");
    return eye.xOffset() - (eye.length() / 2.0);
  }

  public static double eyeMaxX(Eye eye) {
    Objects.requireNonNull(eye, "eye");
    return eye.xOffset() + (eye.length() / 2.0);
  }

  public static Bore getClampedBore(Eye eye) {
    Objects.requireNonNull(eye, "eye");
    Bore bore = eye.bore();
    if (bore == null) {
      return null;
    }

    double leftSpace = -eyeMinX(eye);
    double rightSpace = eyeMaxX(eye);
    double maxHalfWidth = Math.min(leftSpace, rightSpace) - (BORE_CLAMP_EPSILON / 2.0);
    double maxWidth = Math.max(0.0, 2.0 * maxHalfWidth);
    double maxThickness = Math.max(0.0, eye.thickness() - BORE_CLAMP_EPSILON);

    if (maxWidth <= 0.0 || maxThickness <= 0.0) {
      return null;
    }

    double clampedWidth = Math.min(bore.width(), maxWidth);
    double clampedThickness = Math.min(bore.thickness(), maxThickness);
    if (clampedWidth <= 0.0 || clampedThickness <= 0.0) {
      return null;
    }

    return new Bore(clampedWidth, clampedThickness, bore.shape());
  }

  public static double boreArea(Bore bore) {
    Objects.requireNonNull(bore, "bore");
    if ("ellipse".equals(bore.shape())) {
      return Math.PI * (bore.width() / 2.0) * (bore.thickness() / 2.0);
    }
    return bore.width() * bore.thickness();
  }

  public static SpikeSlice sampleSpikeSlice(Spike spike, double s) {
    Objects.requireNonNull(spike, "spike");
    double clampedS = Math.max(0.0, Math.min(1.0, s));
    List<Station> sortedStations = sortedStations(spike);
    Vec3 position = GeometryUtil.pointOnPolyline(spike.centerline(), clampedS);

    Station station = interpolateStation(sortedStations, clampedS);
    double[] tapered = applyPointTaper(station.width(), station.thickness(), spike.pointTaper(), clampedS,
        sortedStations);
    double width = tapered[0];
    double thickness = tapered[1];
    double area = GeometryUtil.modifiedSuperellipseArea(width, thickness, station.section().r());

    return new SpikeSlice(width, thickness, area, position);
  }

  private static ComponentMass computeEyeMass(Eye eye) {
    double eyeVolume = eye.length() * eye.height() * eye.thickness();
    double weightedX = eye.xOffset() * eyeVolume;
    double volume = eyeVolume;

    Bore bore = getClampedBore(eye);
    if (bore != null) {
      double boreVolume = boreArea(bore) * eye.height();
      volume -= boreVolume;
    }

    if (volume <= 0.0) {
      return new ComponentMass(0.0, new Vec3(0.0, 0.0, 0.0));
    }

    return new ComponentMass(volume, new Vec3(weightedX / volume, 0.0, 0.0));
  }

  private static ComponentMass computeSpikeMass(Spike spike, int samples) {
    int steps = Math.max(8, samples);
    double totalLength = GeometryUtil.polylineLength(spike.centerline());
    double dl = totalLength / steps;

    double volume = 0.0;
    Vec3 weighted = new Vec3(0.0, 0.0, 0.0);
    for (int i = 0; i < steps; i++) {
      double s = (i + 0.5) / steps;
      SpikeSlice slice = sampleSpikeSlice(spike, s);
      double sliceVolume = slice.area() * dl;
      volume += sliceVolume;
      weighted = weighted.add(slice.position().mul(sliceVolume));
    }

    if (volume <= 0.0) {
      return new ComponentMass(0.0, new Vec3(0.0, 0.0, 0.0));
    }
    return new ComponentMass(volume, weighted.mul(1.0 / volume));
  }

  private static ComponentMass computeRearMass(PickProfile profile, int samples) {
    Rear rear = profile.rear;
    if ("none".equals(rear.type())) {
      return new ComponentMass(0.0, new Vec3(0.0, 0.0, 0.0));
    }
    if ("spike".equals(rear.type())) {
      return computeSpikeMass(rear.spike(), samples);
    }
    return computeHammerMass(profile.eye, rear, samples);
  }

  private static ComponentMass computeHammerMass(Eye eye, Rear rear, int samples) {
    int steps = Math.max(8, samples);
    double dx = rear.length() / steps;
    double eyeSideScale = 1.0 - rear.taper();
    double volume = 0.0;
    Vec3 weighted = new Vec3(0.0, 0.0, 0.0);

    for (int i = 0; i < steps; i++) {
      double t = (i + 0.5) / steps;
      double scale = GeometryUtil.lerp(eyeSideScale, 1.0, t);
      double width = rear.faceWidth() * scale;
      double thickness = rear.faceHeight() * scale;
      double area = width * thickness;
      double sliceVolume = area * dx;
      double x = eyeMinX(eye) - ((i + 0.5) * dx);
      Vec3 position = new Vec3(x, 0.0, 0.0);

      volume += sliceVolume;
      weighted = weighted.add(position.mul(sliceVolume));
    }

    if (volume <= 0.0) {
      return new ComponentMass(0.0, new Vec3(0.0, 0.0, 0.0));
    }
    return new ComponentMass(volume, weighted.mul(1.0 / volume));
  }

  private static Bounds spikeBounds(Spike spike, int samples) {
    int steps = Math.max(8, samples);
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    double minZ = Double.POSITIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;

    for (int i = 0; i <= steps; i++) {
      double s = (double) i / steps;
      SpikeSlice slice = sampleSpikeSlice(spike, s);
      double halfWidth = slice.width() / 2.0;
      double halfThickness = slice.thickness() / 2.0;
      Vec3 position = slice.position();

      minX = Math.min(minX, position.x());
      maxX = Math.max(maxX, position.x());
      minY = Math.min(minY, position.y() - halfWidth);
      maxY = Math.max(maxY, position.y() + halfWidth);
      minZ = Math.min(minZ, position.z() - halfThickness);
      maxZ = Math.max(maxZ, position.z() + halfThickness);
    }

    if (!Double.isFinite(minX)) {
      return Bounds.zero();
    }
    return new Bounds(minX, maxX, minY, maxY, minZ, maxZ);
  }

  private static Bounds rearBounds(PickProfile profile, int samples) {
    Rear rear = profile.rear;
    if ("none".equals(rear.type())) {
      return Bounds.zero();
    }
    if ("spike".equals(rear.type())) {
      return spikeBounds(rear.spike(), samples);
    }

    double eyeMinX = eyeMinX(profile.eye);
    return new Bounds(
        eyeMinX - rear.length(),
        eyeMinX,
        -rear.faceWidth() / 2.0,
        rear.faceWidth() / 2.0,
        -rear.faceHeight() / 2.0,
        rear.faceHeight() / 2.0);
  }

  private static List<Station> sortedStations(Spike spike) {
    List<Station> sorted = new ArrayList<>(spike.stations());
    sorted.sort(Comparator.comparingDouble(Station::s));
    return sorted;
  }

  private static Station interpolateStation(List<Station> stations, double s) {
    if (s <= stations.get(0).s()) {
      return stations.get(0);
    }
    if (s >= stations.get(stations.size() - 1).s()) {
      return stations.get(stations.size() - 1);
    }

    for (int i = 0; i < stations.size() - 1; i++) {
      Station a = stations.get(i);
      Station b = stations.get(i + 1);
      if (s >= a.s() && s <= b.s()) {
        double t = (s - a.s()) / (b.s() - a.s());
        double width = GeometryUtil.lerp(a.width(), b.width(), t);
        double thickness = GeometryUtil.lerp(a.thickness(), b.thickness(), t);
        double r = GeometryUtil.lerp(a.section().r(), b.section().r(), t);
        String kind = t < 0.5 ? a.section().kind() : b.section().kind();
        return new Station(s, width, thickness, new Section(kind, r));
      }
    }

    return stations.get(stations.size() - 1);
  }

  private static double[] applyPointTaper(double width, double thickness, double pointTaper, double s,
      List<Station> stations) {
    double lastS = stations.get(stations.size() - 1).s();
    if (s <= lastS || lastS >= 1.0) {
      return new double[] { width, thickness };
    }

    double t = (s - lastS) / Math.max(1.0e-9, 1.0 - lastS);
    double clampedTaper = Math.max(0.0, Math.min(1.0, pointTaper));
    double exponent = 1.0 + ((1.0 - clampedTaper) * 4.0);
    double scale = 1.0 - Math.pow(t, exponent);
    scale = Math.max(0.0, Math.min(1.0, scale));

    return new double[] { Math.max(0.0, width * scale), Math.max(0.0, thickness * scale) };
  }
}