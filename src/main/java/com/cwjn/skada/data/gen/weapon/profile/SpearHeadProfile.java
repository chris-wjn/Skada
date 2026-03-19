package com.cwjn.skada.data.gen.weapon.profile;

import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Bounds;
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
 * Geometry and runtime physics model for spear head profiles.
 *
 * Units: centimeters for geometry, grams for mass, g/cm^3 for density.
 */
public final class SpearHeadProfile {

  private static final int CURRENT_VERSION = 1;
  private static final double SOCKET_EPSILON = 1.0e-6;
  private static final double DEFAULT_EDGE_RADIUS_NM = 7.0;
  private static final SharpenedRange DEFAULT_SHARPENED_RANGE = new SharpenedRange(0.0, 1.0);

  public static final Codec<SpearHeadProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("version").forGetter(SpearHeadProfile::getVersion),
      Mount.CODEC.fieldOf("mount").forGetter(SpearHeadProfile::getMount),
      Vec3.CODEC.listOf().fieldOf("centerline").forGetter(SpearHeadProfile::getCenterline),
      Codec.DOUBLE.fieldOf("pointTaper").forGetter(SpearHeadProfile::getPointTaper),
      Codec.DOUBLE.optionalFieldOf("tipRadiusNm").forGetter(profile -> Optional.ofNullable(profile.getTipRadiusNm())),
      Codec.DOUBLE.optionalFieldOf("edgeRadiusNm", DEFAULT_EDGE_RADIUS_NM).forGetter(SpearHeadProfile::getEdgeRadiusNm),
      Codec.DOUBLE.optionalFieldOf("edgeBevel", 0.0).forGetter(SpearHeadProfile::getEdgeBevel),
      SharpenedRange.CODEC.optionalFieldOf("sharpenedRange", DEFAULT_SHARPENED_RANGE)
          .forGetter(SpearHeadProfile::getSharpenedRange),
      Wings.CODEC.optionalFieldOf("wings").forGetter(profile -> Optional.ofNullable(profile.getWings())),
      Station.CODEC.listOf().fieldOf("stations").forGetter(SpearHeadProfile::getStations))
      .apply(instance,
          (version, mount, centerline, pointTaper, tipRadiusNm, edgeRadiusNm, edgeBevel, sharpenedRange, wings,
            stations) -> new SpearHeadProfile(version, mount, centerline, pointTaper, tipRadiusNm.orElse(null),
                edgeRadiusNm, edgeBevel, sharpenedRange, wings.orElse(null), stations)));

  private final int version;
  private final Mount mount;
  private final List<Vec3> centerline;
  private final double pointTaper;
  private final Double tipRadiusNm;
  private final double edgeRadiusNm;
  private final double edgeBevel;
  private final SharpenedRange sharpenedRange;
  private final Wings wings;
  private final List<Station> stations;

  private record ComponentMass(double volumeCm3, Vec3 center) {
  }

  private record VolumeSlice(double area, Vec3 position, double width, double thickness, double dx) {
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
      Double outerWidth,
      Double outerThickness,
      Double innerWidth,
      Double innerThickness,
      Double baseWidth,
      Double baseThickness,
      Double tipWidth,
      Double tipThickness,
      String shape,
      double transitionLength) {
    public static final Codec<Mount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("type").forGetter(Mount::type),
        Codec.DOUBLE.fieldOf("length").forGetter(Mount::length),
        Codec.DOUBLE.optionalFieldOf("outerWidth").forGetter(mount -> Optional.ofNullable(mount.outerWidth())),
        Codec.DOUBLE.optionalFieldOf("outerThickness")
            .forGetter(mount -> Optional.ofNullable(mount.outerThickness())),
        Codec.DOUBLE.optionalFieldOf("innerWidth").forGetter(mount -> Optional.ofNullable(mount.innerWidth())),
        Codec.DOUBLE.optionalFieldOf("innerThickness")
            .forGetter(mount -> Optional.ofNullable(mount.innerThickness())),
        Codec.DOUBLE.optionalFieldOf("baseWidth").forGetter(mount -> Optional.ofNullable(mount.baseWidth())),
        Codec.DOUBLE.optionalFieldOf("baseThickness").forGetter(mount -> Optional.ofNullable(mount.baseThickness())),
        Codec.DOUBLE.optionalFieldOf("tipWidth").forGetter(mount -> Optional.ofNullable(mount.tipWidth())),
        Codec.DOUBLE.optionalFieldOf("tipThickness").forGetter(mount -> Optional.ofNullable(mount.tipThickness())),
        Codec.STRING.optionalFieldOf("shape", "oval").forGetter(Mount::shape),
        Codec.DOUBLE.optionalFieldOf("transitionLength", 0.0).forGetter(Mount::transitionLength))
        .apply(instance,
            (type, length, outerWidth, outerThickness, innerWidth, innerThickness, baseWidth, baseThickness,
              tipWidth, tipThickness, shape, transitionLength) -> new Mount(type, length, outerWidth.orElse(null),
                  outerThickness.orElse(null), innerWidth.orElse(null), innerThickness.orElse(null),
                  baseWidth.orElse(null), baseThickness.orElse(null), tipWidth.orElse(null),
                  tipThickness.orElse(null), shape, transitionLength)));

    public Mount {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(shape, "shape");

      if (length <= 0.0) {
        throw new IllegalArgumentException("mount.length must be > 0");
      }
      if (transitionLength < 0.0) {
        throw new IllegalArgumentException("mount.transitionLength must be >= 0");
      }
      if (transitionLength > length) {
        throw new IllegalArgumentException("mount.transitionLength must be <= mount.length");
      }
      if (!"socket".equals(type) && !"tang".equals(type)) {
        throw new IllegalArgumentException("mount.type must be \"socket\" or \"tang\"");
      }
      if (!"oval".equals(shape) && !"round".equals(shape) && !"rect".equals(shape)) {
        throw new IllegalArgumentException("mount.shape must be \"oval\", \"round\", or \"rect\"");
      }

      if ("socket".equals(type)) {
        requirePositive(outerWidth, "mount.outerWidth");
        requirePositive(outerThickness, "mount.outerThickness");
        requirePositive(innerWidth, "mount.innerWidth");
        requirePositive(innerThickness, "mount.innerThickness");
        if (innerWidth >= outerWidth - SOCKET_EPSILON || innerThickness >= outerThickness - SOCKET_EPSILON) {
          throw new IllegalArgumentException("socket inner dimensions must remain smaller than outer dimensions");
        }
        requireAbsent(baseWidth, "mount.baseWidth", "socket");
        requireAbsent(baseThickness, "mount.baseThickness", "socket");
        requireAbsent(tipWidth, "mount.tipWidth", "socket");
        requireAbsent(tipThickness, "mount.tipThickness", "socket");
      }

      if ("tang".equals(type)) {
        requirePositive(baseWidth, "mount.baseWidth");
        requirePositive(baseThickness, "mount.baseThickness");
        requirePositive(tipWidth, "mount.tipWidth");
        requirePositive(tipThickness, "mount.tipThickness");
        requireAbsent(outerWidth, "mount.outerWidth", "tang");
        requireAbsent(outerThickness, "mount.outerThickness", "tang");
        requireAbsent(innerWidth, "mount.innerWidth", "tang");
        requireAbsent(innerThickness, "mount.innerThickness", "tang");
      }
    }

    private static void requirePositive(Double value, String field) {
      if (value == null || value <= 0.0) {
        throw new IllegalArgumentException(field + " must be > 0");
      }
    }

    private static void requireAbsent(Double value, String field, String type) {
      if (value != null) {
        throw new IllegalArgumentException(field + " must be omitted for mount.type \"" + type + "\"");
      }
    }
  }

  public record Section(double r, double midribFlat) {
    public static final Codec<Section> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("r").forGetter(Section::r),
        Codec.DOUBLE.optionalFieldOf("midribFlat", 0.0).forGetter(Section::midribFlat)).apply(instance,
            Section::new));

    public Section {
      if (r <= 0.0) {
        throw new IllegalArgumentException("section.r must be > 0");
      }
      if (midribFlat < 0.0 || midribFlat > 1.0) {
        throw new IllegalArgumentException("section.midribFlat must be in [0,1]");
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

  public record Wings(
      boolean enabled,
      double positionS,
      double projection,
      double span,
      double thickness,
      double tipRadiusMm,
      String style) {
    public static final Codec<Wings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Wings::enabled),
        Codec.DOUBLE.fieldOf("positionS").forGetter(Wings::positionS),
        Codec.DOUBLE.fieldOf("projection").forGetter(Wings::projection),
        Codec.DOUBLE.fieldOf("span").forGetter(Wings::span),
        Codec.DOUBLE.fieldOf("thickness").forGetter(Wings::thickness),
        Codec.DOUBLE.optionalFieldOf("tipRadiusMm", 0.0).forGetter(Wings::tipRadiusMm),
        Codec.STRING.optionalFieldOf("style", "triangular").forGetter(Wings::style)).apply(instance,
            Wings::new));

    public Wings {
      Objects.requireNonNull(style, "style");
      if (positionS < 0.0 || positionS > 1.0) {
        throw new IllegalArgumentException("wings.positionS must be in [0,1]");
      }
      if (!"triangular".equals(style) && !"leaf".equals(style) && !"bar".equals(style)) {
        throw new IllegalArgumentException("wings.style must be \"triangular\", \"leaf\", or \"bar\"");
      }
      if (tipRadiusMm < 0.0) {
        throw new IllegalArgumentException("wings.tipRadiusMm must be >= 0");
      }
      if (enabled) {
        if (projection <= 0.0 || span <= 0.0 || thickness <= 0.0) {
          throw new IllegalArgumentException("enabled wings require projection, span, and thickness > 0");
        }
      }
    }
  }

  public record SpearSlice(double width, double thickness, double area, Vec3 position, boolean sharpened) {
  }

  public SpearHeadProfile(
      int version,
      Mount mount,
      List<Vec3> centerline,
      double pointTaper,
      Double tipRadiusNm,
      double edgeRadiusNm,
      double edgeBevel,
      SharpenedRange sharpenedRange,
      Wings wings,
      List<Station> stations) {
    this.version = version;
    this.mount = Objects.requireNonNull(mount, "mount");
    this.centerline = List.copyOf(Objects.requireNonNull(centerline, "centerline"));
    this.pointTaper = pointTaper;
    this.tipRadiusNm = tipRadiusNm;
    this.edgeRadiusNm = edgeRadiusNm;
    this.edgeBevel = edgeBevel;
    this.sharpenedRange = Objects.requireNonNull(sharpenedRange, "sharpenedRange");
    this.wings = wings;
    this.stations = List.copyOf(Objects.requireNonNull(stations, "stations"));

    if (version != CURRENT_VERSION) {
      throw new IllegalArgumentException("version must be " + CURRENT_VERSION);
    }
    if (this.centerline.size() < 2) {
      throw new IllegalArgumentException("centerline must have at least 2 points");
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

    Vec3 base = this.centerline.get(0);
    Vec3 tip = this.centerline.get(this.centerline.size() - 1);
    if (tip.x() <= base.x()) {
      throw new IllegalArgumentException("centerline must progress toward +x");
    }

    double previousS = -1.0;
    for (Station station : this.stations) {
      if (station.s() < previousS) {
        throw new IllegalArgumentException("station s values must be monotonic increasing");
      }
      previousS = station.s();
    }

    if (wings != null && wings.enabled()) {
      Vec3 wingCenter = GeometryUtil.pointOnPolyline(this.centerline, wings.positionS());
      double wingStartX = wingCenter.x() - (wings.span() * 0.5);
      if (wingStartX < base.x() - 1.0e-6) {
        throw new IllegalArgumentException("wings should not extend behind the shoulder");
      }
    }
  }

  public static MassProperties computeMassProperties(SpearHeadProfile profile, double densityGPerCm3, int samples) {
    Objects.requireNonNull(profile, "profile");
    if (densityGPerCm3 <= 0.0) {
      throw new IllegalArgumentException("density must be > 0");
    }

    int steps = Math.max(8, samples);
    double dl = profile.length() / steps;

    double bladeVolume = 0.0;
    Vec3 bladeWeighted = new Vec3(0.0, 0.0, 0.0);
    for (int i = 0; i < steps; i++) {
      double s = (i + 0.5) / steps;
      SpearSlice slice = sampleSlice(profile, s);
      double sliceVolume = slice.area() * dl;
      bladeVolume += sliceVolume;
      bladeWeighted = bladeWeighted.add(slice.position().mul(sliceVolume));
    }

    ComponentMass mountMass = computeMountMass(profile, Math.max(8, steps / 4));
    ComponentMass wingMass = computeWingMass(profile);

    double totalVolume = bladeVolume + mountMass.volumeCm3() + wingMass.volumeCm3();
    Vec3 weighted = bladeWeighted
        .add(mountMass.center().mul(mountMass.volumeCm3()))
        .add(wingMass.center().mul(wingMass.volumeCm3()));

    Vec3 center = totalVolume > 0.0 ? weighted.mul(1.0 / totalVolume) : new Vec3(0.0, 0.0, 0.0);
    return new MassProperties(totalVolume, totalVolume * densityGPerCm3, center);
  }

  public MassProperties computeMassProperties(double densityGPerCm3, int samples) {
    return computeMassProperties(this, densityGPerCm3, samples);
  }

  public static Bounds localBounds(SpearHeadProfile profile, int samples) {
    Objects.requireNonNull(profile, "profile");

    int steps = Math.max(16, samples);
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    double minZ = Double.POSITIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;

    for (int i = 0; i <= steps; i++) {
      double s = (double) i / steps;
      SpearSlice slice = profile.sampleSliceAt(s);
      Vec3 position = slice.position();
      double halfWidth = slice.width() * 0.5;
      double halfThickness = slice.thickness() * 0.5;

      minX = Math.min(minX, position.x());
      maxX = Math.max(maxX, position.x());
      minY = Math.min(minY, position.y() - halfWidth);
      maxY = Math.max(maxY, position.y() + halfWidth);
      minZ = Math.min(minZ, position.z() - halfThickness);
      maxZ = Math.max(maxZ, position.z() + halfThickness);
    }

    for (VolumeSlice slice : sampleMountSlices(profile, Math.max(8, steps / 4))) {
      Vec3 position = slice.position();
      double halfWidth = slice.width() * 0.5;
      double halfThickness = slice.thickness() * 0.5;

      minX = Math.min(minX, position.x() - (slice.dx() * 0.5));
      maxX = Math.max(maxX, position.x() + (slice.dx() * 0.5));
      minY = Math.min(minY, position.y() - halfWidth);
      maxY = Math.max(maxY, position.y() + halfWidth);
      minZ = Math.min(minZ, position.z() - halfThickness);
      maxZ = Math.max(maxZ, position.z() + halfThickness);
    }

    if (profile.wings != null && profile.wings.enabled()) {
      SpearSlice rootSlice = profile.sampleSliceAt(profile.wings.positionS());
      Vec3 wingCenter = rootSlice.position();
      double halfSpan = profile.wings.span() * 0.5;
      double halfThickness = profile.wings.thickness() * 0.5;
      double halfWidth = (rootSlice.width() * 0.5) + profile.wings.projection();

      minX = Math.min(minX, wingCenter.x() - halfSpan);
      maxX = Math.max(maxX, wingCenter.x() + halfSpan);
      minY = Math.min(minY, wingCenter.y() - halfWidth);
      maxY = Math.max(maxY, wingCenter.y() + halfWidth);
      minZ = Math.min(minZ, wingCenter.z() - halfThickness);
      maxZ = Math.max(maxZ, wingCenter.z() + halfThickness);
    }

    if (!Double.isFinite(minX)) {
      return Bounds.zero();
    }
    return new Bounds(minX, maxX, minY, maxY, minZ, maxZ);
  }

  public Bounds localBounds(int samples) {
    return localBounds(this, samples);
  }

  public double length() {
    return GeometryUtil.polylineLength(centerline);
  }

  public SpearSlice sampleSliceAt(double s) {
    return sampleSlice(this, s);
  }

  public Station sampleStationAt(double s) {
    return interpolateStation(sortedStations(), Math.max(0.0, Math.min(1.0, s)));
  }

  public boolean isSharpenedAt(double normalizedPosition) {
    return sharpenedRange.contains(Math.max(0.0, Math.min(1.0, normalizedPosition)));
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

  public List<Vec3> getCenterline() {
    return centerline;
  }

  public double getPointTaper() {
    return pointTaper;
  }

  public Double getTipRadiusNm() {
    return tipRadiusNm;
  }

  public double getEdgeRadiusNm() {
    return edgeRadiusNm;
  }

  public double getEdgeBevel() {
    return edgeBevel;
  }

  public SharpenedRange getSharpenedRange() {
    return sharpenedRange;
  }

  public Wings getWings() {
    return wings;
  }

  public List<Station> getStations() {
    return stations;
  }

  public static SpearSlice sampleSlice(SpearHeadProfile profile, double s) {
    Objects.requireNonNull(profile, "profile");
    return sampleSlice(profile, profile.sortedStations(), s);
  }

  private static SpearSlice sampleSlice(SpearHeadProfile profile, List<Station> sortedStations, double s) {
    double clampedS = Math.max(0.0, Math.min(1.0, s));
    Vec3 position = GeometryUtil.pointOnPolyline(profile.centerline, clampedS);
    Station station = interpolateStation(sortedStations, clampedS);

    double[] tapered = applyPointTaper(station.width(), station.thickness(), profile.pointTaper, clampedS,
        sortedStations);
    double width = tapered[0];
    double thickness = tapered[1];
    double area = GeometryUtil.modifiedSuperellipseArea(width, thickness, station.section().r(),
        station.section().midribFlat());

    return new SpearSlice(width, thickness, area, position, profile.sharpenedRange.contains(clampedS));
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
        double midribFlat = GeometryUtil.lerp(a.section().midribFlat(), b.section().midribFlat(), t);
        return new Station(s, width, thickness, new Section(r, midribFlat));
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
    double taperExponent = 1.0 + ((1.0 - Math.max(0.0, Math.min(1.0, pointTaper))) * 4.0);
    double scale = 1.0 - Math.pow(Math.max(0.0, Math.min(1.0, t)), taperExponent);
    scale = Math.max(0.0, Math.min(1.0, scale));

    return new double[] { Math.max(0.0, width * scale), Math.max(0.0, thickness * scale) };
  }

  private static ComponentMass computeMountMass(SpearHeadProfile profile, int samples) {
    List<VolumeSlice> slices = sampleMountSlices(profile, samples);
    double volume = 0.0;
    Vec3 weighted = new Vec3(0.0, 0.0, 0.0);

    for (VolumeSlice slice : slices) {
      double sliceVolume = slice.area() * slice.dx();
      volume += sliceVolume;
      weighted = weighted.add(slice.position().mul(sliceVolume));
    }

    if (volume <= 0.0) {
      return new ComponentMass(0.0, new Vec3(0.0, 0.0, 0.0));
    }
    return new ComponentMass(volume, weighted.mul(1.0 / volume));
  }

  private static List<VolumeSlice> sampleMountSlices(SpearHeadProfile profile, int samples) {
    Mount mount = profile.mount;
    Vec3 shoulder = profile.centerline.get(0);
    SpearSlice rootSlice = sampleSlice(profile, 0.0);
    double bladeArea = rootSlice.area();

    int steps = Math.max(4, samples);
    double length = mount.length();
    double dx = length / steps;
    double bodyLength = Math.max(0.0, length - mount.transitionLength());
    double transitionLength = length - bodyLength;

    List<VolumeSlice> slices = new ArrayList<>(steps);
    for (int i = 0; i < steps; i++) {
      double u = (i + 0.5) * dx;
      double x = shoulder.x() - length + u;
      Vec3 position = new Vec3(x, shoulder.y(), shoulder.z());

      if ("socket".equals(mount.type())) {
        double shellArea = Math.max(0.0,
            areaByShape(mount.outerWidth(), mount.outerThickness(), mount.shape())
                - areaByShape(mount.innerWidth(), mount.innerThickness(), mount.shape()));

        double t = transitionLength <= 1.0e-9 || u <= bodyLength ? 0.0 : (u - bodyLength) / transitionLength;
        t = Math.max(0.0, Math.min(1.0, t));

        double width = GeometryUtil.lerp(mount.outerWidth(), rootSlice.width(), t);
        double thickness = GeometryUtil.lerp(mount.outerThickness(), rootSlice.thickness(), t);
        double area = GeometryUtil.lerp(shellArea, bladeArea, t);
        slices.add(new VolumeSlice(area, position, width, thickness, dx));
      } else {
        double tBody = bodyLength <= 1.0e-9 ? 1.0 : Math.max(0.0, Math.min(1.0, u / bodyLength));
        double baseArea = mount.baseWidth() * mount.baseThickness();
        double width;
        double thickness;
        double area;

        if (transitionLength > 1.0e-9 && u > bodyLength) {
          double tTransition = (u - bodyLength) / transitionLength;
          tTransition = Math.max(0.0, Math.min(1.0, tTransition));
          width = GeometryUtil.lerp(mount.baseWidth(), rootSlice.width(), tTransition);
          thickness = GeometryUtil.lerp(mount.baseThickness(), rootSlice.thickness(), tTransition);
          area = GeometryUtil.lerp(baseArea, bladeArea, tTransition);
        } else {
          width = GeometryUtil.lerp(mount.tipWidth(), mount.baseWidth(), tBody);
          thickness = GeometryUtil.lerp(mount.tipThickness(), mount.baseThickness(), tBody);
          area = width * thickness;
        }

        slices.add(new VolumeSlice(area, position, width, thickness, dx));
      }
    }

    return slices;
  }

  private static ComponentMass computeWingMass(SpearHeadProfile profile) {
    if (profile.wings == null || !profile.wings.enabled()) {
      return new ComponentMass(0.0, new Vec3(0.0, 0.0, 0.0));
    }

    Wings wings = profile.wings;
    double styleFactor = wingStyleAreaFactor(wings.style());
    double volume = 2.0 * styleFactor * wings.projection() * wings.span() * wings.thickness();
    Vec3 center = GeometryUtil.pointOnPolyline(profile.centerline, wings.positionS());
    return new ComponentMass(volume, center);
  }

  private static double wingStyleAreaFactor(String style) {
    return switch (style) {
      case "bar" -> 1.0;
      case "leaf" -> 0.75;
      default -> 0.5;
    };
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