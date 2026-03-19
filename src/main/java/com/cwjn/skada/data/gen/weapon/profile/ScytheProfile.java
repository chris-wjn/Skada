package com.cwjn.skada.data.gen.weapon.profile;

import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Geometry and runtime physics model for scythe head profiles.
 *
 * Units: centimeters for geometry, grams for mass, g/cm^3 for density.
 */
public final class ScytheProfile {

  private static final int CURRENT_VERSION = 1;
  private static final double SOCKET_EPSILON = 1.0e-6;
  private static final double DEFAULT_EDGE_RADIUS_NM = 6.0;
  private static final SharpenedRange DEFAULT_SHARPENED_RANGE = new SharpenedRange(0.12, 1.0);

  public static final Codec<ScytheProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("version").forGetter(ScytheProfile::getVersion),
      Mount.CODEC.fieldOf("mount").forGetter(ScytheProfile::getMount),
      Neck.CODEC.optionalFieldOf("neck").forGetter(profile -> Optional.ofNullable(profile.getNeck())),
      Vec3.CODEC.listOf().fieldOf("spine").forGetter(ScytheProfile::getSpine),
      Codec.DOUBLE.fieldOf("pointTaper").forGetter(ScytheProfile::getPointTaper),
      Codec.DOUBLE.optionalFieldOf("edgeRadiusNm", DEFAULT_EDGE_RADIUS_NM).forGetter(ScytheProfile::getEdgeRadiusNm),
      Codec.DOUBLE.optionalFieldOf("edgeBevel", 0.0).forGetter(ScytheProfile::getEdgeBevel),
      Codec.DOUBLE.optionalFieldOf("tipRadiusNm").forGetter(profile -> Optional.ofNullable(profile.getTipRadiusNm())),
      SharpenedRange.CODEC.optionalFieldOf("sharpenedRange", DEFAULT_SHARPENED_RANGE)
          .forGetter(ScytheProfile::getSharpenedRange),
      Codec.DOUBLE.optionalFieldOf("preferredStrikeS")
          .forGetter(profile -> Optional.ofNullable(profile.getPreferredStrikeS())),
      Station.CODEC.listOf().fieldOf("stations").forGetter(ScytheProfile::getStations))
      .apply(instance,
          (version, mount, neck, spine, pointTaper, edgeRadiusNm, edgeBevel, tipRadiusNm, sharpenedRange,
            preferredStrikeS, stations) -> new ScytheProfile(version, mount, neck.orElse(null), spine,
                  pointTaper, edgeRadiusNm, edgeBevel, tipRadiusNm.orElse(null), sharpenedRange,
                  preferredStrikeS.orElse(null), stations)));

  private final int version;
  private final Mount mount;
  private final Neck neck;
  private final List<Vec3> spine;
  private final double pointTaper;
  private final double edgeRadiusNm;
  private final double edgeBevel;
  private final Double tipRadiusNm;
  private final SharpenedRange sharpenedRange;
  private final Double preferredStrikeS;
  private final List<Station> stations;

  private record ComponentMass(double volumeCm3, Vec3 center) {
  }

  public record SharpenedRange(double s0, double s1) {
    public static final Codec<SharpenedRange> CODEC = Codec.DOUBLE.listOf().comapFlatMap(
        list -> {
          if (list.size() != 2) {
            return DataResult.error(() -> "sharpenedRange must contain exactly two entries [s0, s1]");
          }
          return DataResult.success(new SharpenedRange(list.get(0), list.get(1)));
        },
        range -> List.of(range.s0(), range.s1()));

    public SharpenedRange {
      if (s0 < 0.0 || s1 > 1.0 || s0 > s1) {
        throw new IllegalArgumentException("sharpenedRange must satisfy 0 <= s0 <= s1 <= 1");
      }
    }

    public boolean contains(double s) {
      return s >= s0 && s <= s1;
    }
  }

  public record Mount(
      String type,
      double length,
      double outerWidth,
      double outerThickness,
      double innerWidth,
      double innerThickness,
      double width,
      double thickness,
      String shape,
      double angleDegrees,
      double transitionLength) {
    public static final Codec<Mount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("type").forGetter(Mount::type),
        Codec.DOUBLE.fieldOf("length").forGetter(Mount::length),
        Codec.DOUBLE.optionalFieldOf("outerWidth", 0.0).forGetter(Mount::outerWidth),
        Codec.DOUBLE.optionalFieldOf("outerThickness", 0.0).forGetter(Mount::outerThickness),
        Codec.DOUBLE.optionalFieldOf("innerWidth", 0.0).forGetter(Mount::innerWidth),
        Codec.DOUBLE.optionalFieldOf("innerThickness", 0.0).forGetter(Mount::innerThickness),
        Codec.DOUBLE.optionalFieldOf("width", 0.0).forGetter(Mount::width),
        Codec.DOUBLE.optionalFieldOf("thickness", 0.0).forGetter(Mount::thickness),
        Codec.STRING.optionalFieldOf("shape", "oval").forGetter(Mount::shape),
        Codec.DOUBLE.optionalFieldOf("angleDegrees", 0.0).forGetter(Mount::angleDegrees),
        Codec.DOUBLE.optionalFieldOf("transitionLength", 0.0).forGetter(Mount::transitionLength))
        .apply(instance, Mount::new));

    public Mount {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(shape, "shape");
      if (length <= 0.0) {
        throw new IllegalArgumentException("mount.length must be > 0");
      }
      if (transitionLength < 0.0) {
        throw new IllegalArgumentException("mount.transitionLength must be >= 0");
      }
      if (!"tang".equals(type) && !"socket".equals(type)) {
        throw new IllegalArgumentException("mount.type must be \"tang\" or \"socket\"");
      }
      if (!"oval".equals(shape) && !"rect".equals(shape) && !"circle".equals(shape)) {
        throw new IllegalArgumentException("mount.shape must be \"oval\", \"rect\", or \"circle\"");
      }
      if ("socket".equals(type)) {
        if (outerWidth <= 0.0 || outerThickness <= 0.0) {
          throw new IllegalArgumentException("socket mount requires outerWidth/outerThickness > 0");
        }
        if (innerWidth < 0.0 || innerThickness < 0.0) {
          throw new IllegalArgumentException("socket inner dimensions must be >= 0");
        }
        if (innerWidth >= outerWidth - SOCKET_EPSILON || innerThickness >= outerThickness - SOCKET_EPSILON) {
          throw new IllegalArgumentException("socket inner dimensions must remain smaller than outer dimensions");
        }
      }
      if ("tang".equals(type) && (width <= 0.0 || thickness <= 0.0)) {
        throw new IllegalArgumentException("tang mount requires width/thickness > 0");
      }
    }
  }

  public record Neck(double length, double drop, double lateralOffset, double thickness) {
    public static final Codec<Neck> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("length").forGetter(Neck::length),
        Codec.DOUBLE.fieldOf("drop").forGetter(Neck::drop),
        Codec.DOUBLE.fieldOf("lateralOffset").forGetter(Neck::lateralOffset),
        Codec.DOUBLE.fieldOf("thickness").forGetter(Neck::thickness)).apply(instance, Neck::new));

    public Neck {
      if (length <= 0.0) {
        throw new IllegalArgumentException("neck.length must be > 0");
      }
      if (thickness <= 0.0) {
        throw new IllegalArgumentException("neck.thickness must be > 0");
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

    public record Section(double r, double edgeOffset, double spineFlat) {
      public static final Codec<Section> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.DOUBLE.fieldOf("r").forGetter(Section::r),
          Codec.DOUBLE.optionalFieldOf("edgeOffset", 0.0).forGetter(Section::edgeOffset),
          Codec.DOUBLE.optionalFieldOf("spineFlat", 0.0).forGetter(Section::spineFlat)).apply(instance, Section::new));

      public Section {
        if (r <= 0.0) {
          throw new IllegalArgumentException("section.r must be > 0");
        }
        if (edgeOffset < -1.0 || edgeOffset > 1.0) {
          throw new IllegalArgumentException("section.edgeOffset must be in [-1,1]");
        }
        if (spineFlat < 0.0 || spineFlat > 1.0) {
          throw new IllegalArgumentException("section.spineFlat must be in [0,1]");
        }
      }
    }
  }

  public record ScytheSlice(double width, double thickness, double area, Vec3 position, boolean sharpened) {
  }

  public ScytheProfile(
      int version,
      Mount mount,
      Neck neck,
      List<Vec3> spine,
      double pointTaper,
      double edgeRadiusNm,
      double edgeBevel,
      Double tipRadiusNm,
      SharpenedRange sharpenedRange,
      Double preferredStrikeS,
      List<Station> stations) {
    this.version = version;
    this.mount = Objects.requireNonNull(mount, "mount");
    this.neck = neck;
    this.spine = List.copyOf(Objects.requireNonNull(spine, "spine"));
    this.pointTaper = pointTaper;
    this.edgeRadiusNm = edgeRadiusNm;
    this.edgeBevel = edgeBevel;
    this.tipRadiusNm = tipRadiusNm;
    this.sharpenedRange = Objects.requireNonNull(sharpenedRange, "sharpenedRange");
    this.preferredStrikeS = preferredStrikeS;
    this.stations = List.copyOf(Objects.requireNonNull(stations, "stations"));

    if (version != CURRENT_VERSION) {
      throw new IllegalArgumentException("version must be " + CURRENT_VERSION);
    }
    if (this.spine.size() < 2) {
      throw new IllegalArgumentException("spine must have at least 2 points");
    }
    if (this.stations.size() < 2) {
      throw new IllegalArgumentException("stations must have at least 2 entries");
    }
    if (pointTaper < 0.0 || pointTaper > 1.0) {
      throw new IllegalArgumentException("pointTaper must be in [0,1]");
    }
    if (edgeRadiusNm <= 0.0) {
      throw new IllegalArgumentException("edgeRadiusNm must be > 0");
    }
    if (tipRadiusNm != null && tipRadiusNm <= 0.0) {
      throw new IllegalArgumentException("tipRadiusNm must be > 0 when provided");
    }
    if (preferredStrikeS != null && (preferredStrikeS < 0.0 || preferredStrikeS > 1.0)) {
      throw new IllegalArgumentException("preferredStrikeS must be in [0,1] when provided");
    }

    double prev = -1.0;
    for (Station station : this.stations) {
      if (station.s() < prev) {
        throw new IllegalArgumentException("station s values must be monotonic increasing");
      }
      prev = station.s();
    }

    if (preferredStrikeS != null && !sharpenedRange.contains(preferredStrikeS)) {
      throw new IllegalArgumentException("preferredStrikeS should lie inside sharpenedRange");
    }
  }

  public static MassProperties computeMassProperties(ScytheProfile profile, double densityGPerCm3, int samples) {
    Objects.requireNonNull(profile, "profile");
    if (densityGPerCm3 <= 0.0) {
      throw new IllegalArgumentException("density must be > 0");
    }

    int steps = Math.max(4, samples);
    List<Station> sortedStations = profile.sortedStations();

    double spineLength = GeometryUtil.polylineLength(profile.spine);
    double dl = spineLength / steps;

    double bladeVolume = 0.0;
    Vec3 bladeWeighted = new Vec3(0.0, 0.0, 0.0);

    for (int i = 0; i < steps; i++) {
      double s = (i + 0.5) / steps;
      ScytheSlice slice = sampleSlice(profile, sortedStations, s);
      double sliceVolume = slice.area() * dl;
      bladeVolume += sliceVolume;
      bladeWeighted = bladeWeighted.add(slice.position().mul(sliceVolume));
    }

    ComponentMass mount = computeMountMass(profile);
    ComponentMass neck = computeNeckMass(profile);

    double totalVolume = bladeVolume + mount.volumeCm3() + neck.volumeCm3();
    Vec3 weighted = bladeWeighted
        .add(mount.center().mul(mount.volumeCm3()))
        .add(neck.center().mul(neck.volumeCm3()));

    Vec3 center = totalVolume > 0.0 ? weighted.mul(1.0 / totalVolume) : new Vec3(0.0, 0.0, 0.0);
    double mass = totalVolume * densityGPerCm3;
    return new MassProperties(totalVolume, mass, center);
  }

  public MassProperties computeMassProperties(double densityGPerCm3, int samples) {
    return computeMassProperties(this, densityGPerCm3, samples);
  }

  public double length() {
    return GeometryUtil.polylineLength(spine);
  }

  public ScytheSlice sampleSliceAt(double s) {
    return sampleSlice(this, s);
  }

  public Station sampleStationAt(double s) {
    return interpolateStation(sortedStations(), Math.max(0.0, Math.min(1.0, s)));
  }

  public boolean isSharpenedAt(double normalizedPosition) {
    double clamped = Math.max(0.0, Math.min(1.0, normalizedPosition));
    return sharpenedRange.contains(clamped);
  }

  public double tipLengthCm() {
    double lastS = sortedStations().get(sortedStations().size() - 1).s();
    return length() * (1.0 - lastS);
  }

  public int getVersion() {
    return version;
  }

  public Mount getMount() {
    return mount;
  }

  public Neck getNeck() {
    return neck;
  }

  public List<Vec3> getSpine() {
    return spine;
  }

  public double getPointTaper() {
    return pointTaper;
  }

  public double getEdgeRadiusNm() {
    return edgeRadiusNm;
  }

  public double getEdgeBevel() {
    return edgeBevel;
  }

  public Double getTipRadiusNm() {
    return tipRadiusNm;
  }

  public SharpenedRange getSharpenedRange() {
    return sharpenedRange;
  }

  public Double getPreferredStrikeS() {
    return preferredStrikeS;
  }

  public List<Station> getStations() {
    return stations;
  }

  public static ScytheSlice sampleSlice(ScytheProfile profile, double s) {
    Objects.requireNonNull(profile, "profile");
    return sampleSlice(profile, profile.sortedStations(), s);
  }

  private static ScytheSlice sampleSlice(ScytheProfile profile, List<Station> sortedStations, double s) {
    double clampedS = Math.max(0.0, Math.min(1.0, s));
    Vec3 position = GeometryUtil.pointOnPolyline(profile.spine, clampedS);
    Station station = interpolateStation(sortedStations, clampedS);

    double[] tapered = applyPointTaper(station.width(), station.thickness(), profile.pointTaper, clampedS,
        sortedStations);
    double width = tapered[0];
    double thickness = tapered[1];

    double area = GeometryUtil.modifiedSuperellipseArea(width, thickness, station.section().r(),
        station.section().spineFlat());
    boolean sharpened = profile.sharpenedRange.contains(clampedS);
    return new ScytheSlice(width, thickness, area, position, sharpened);
  }

  private List<Station> sortedStations() {
    List<Station> sorted = new ArrayList<>(stations);
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
        double edgeOffset = GeometryUtil.lerp(a.section().edgeOffset(), b.section().edgeOffset(), t);
        double spineFlat = GeometryUtil.lerp(a.section().spineFlat(), b.section().spineFlat(), t);
        return new Station(s, width, thickness, new Station.Section(r, edgeOffset, spineFlat));
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

    double t = (s - lastS) / Math.max(1.0e-9, (1.0 - lastS));
    t = Math.max(0.0, Math.min(1.0, t));

    double clampedPointTaper = Math.max(0.0, Math.min(1.0, pointTaper));
    double r = 1.0 + ((1.0 - clampedPointTaper) * 4.0);
    double profileScale = 1.0 - Math.pow(t, r);
    profileScale = Math.max(0.0, Math.min(1.0, profileScale));

    return new double[] { Math.max(0.0, width * profileScale), Math.max(0.0, thickness * profileScale) };
  }

  private static ComponentMass computeMountMass(ScytheProfile profile) {
    Mount mount = profile.mount;
    Vec3 heel = profile.spine.get(0);
    Vec3 dir = mountDirection(profile);

    double volume;
    if ("socket".equals(mount.type())) {
      double outerArea = areaByShape(mount.outerWidth(), mount.outerThickness(), mount.shape());
      double innerArea = areaByShape(mount.innerWidth(), mount.innerThickness(), mount.shape());
      volume = Math.max(0.0, (outerArea - innerArea) * mount.length());
    } else {
      volume = mount.width() * mount.thickness() * mount.length();
    }

    Vec3 center = heel.add(dir.mul(mount.length() * 0.5));
    return new ComponentMass(volume, center);
  }

  private static ComponentMass computeNeckMass(ScytheProfile profile) {
    Neck neck = profile.neck;
    if (neck == null) {
      return new ComponentMass(0.0, new Vec3(0.0, 0.0, 0.0));
    }

    Vec3 heel = profile.spine.get(0);
    Vec3 center = heel.add(new Vec3(neck.length() * 0.5, neck.lateralOffset() * 0.5, neck.drop() * 0.5));

    double crossSectionArea = neck.thickness() * neck.thickness();
    double volume = crossSectionArea * neck.length();
    return new ComponentMass(volume, center);
  }

  private static Vec3 mountDirection(ScytheProfile profile) {
    Vec3 p0 = profile.spine.get(0);
    Vec3 p1 = profile.spine.get(1);

    double fx = p1.x() - p0.x();
    double fy = p1.y() - p0.y();
    double len = Math.sqrt((fx * fx) + (fy * fy));

    double baseAngle = len > 1.0e-9 ? Math.atan2(fy, fx) : 0.0;
    double theta = baseAngle - Math.toRadians(profile.mount.angleDegrees());

    return new Vec3(Math.cos(theta), Math.sin(theta), 0.0);
  }

  private static double areaByShape(double width, double thickness, String shape) {
    if (width <= 0.0 || thickness <= 0.0) {
      return 0.0;
    }
    if ("rect".equals(shape)) {
      return width * thickness;
    }
    return Math.PI * (width * 0.5) * (thickness * 0.5);
  }
}
