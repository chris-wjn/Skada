package com.cwjn.skada.data.gen.weapon.profile;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.gen.weapon.profile.BladeProfile.Station.Section;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.*;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 
 * Geometry and physics of a blade profile. To be used by weapon part
 * subclasses.
 * Cannot be used directly as a weapon part.
 * 
 * Units: centimeters for length, grams for mass, g/cm^3 for density.
 * Density is supplied at runtime (not stored in JSON).
 */
public final class BladeProfile {

  private static final int CURRENT_VERSION = 1;
  private static final Flamboyance DEFAULT_FLAMBOYANCE = new Flamboyance(0.0, 0.0, 0.0);

  public static final Codec<BladeProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("version").forGetter(BladeProfile::getVersion),
      Vec3.CODEC.listOf().fieldOf("spine").forGetter(BladeProfile::getSpine),
      Codec.DOUBLE.fieldOf("pointTaper").forGetter(BladeProfile::getPointTaper),
      Codec.DOUBLE.optionalFieldOf("edgeRadiusNm", 5.0).forGetter(BladeProfile::getEdgeRadiusNm),
      Codec.DOUBLE.optionalFieldOf("edgeBevel", 0.0).forGetter(BladeProfile::getEdgeBevel),
      Station.CODEC.listOf().fieldOf("stations").forGetter(BladeProfile::getStations),
      Fuller.CODEC.listOf().optionalFieldOf("fullers", List.of()).forGetter(BladeProfile::getFullers),
      Flamboyance.CODEC.optionalFieldOf("flamboyance", DEFAULT_FLAMBOYANCE).forGetter(BladeProfile::getFlamboyance))
      .apply(instance, BladeProfile::new));

  private final int version;
  private List<Vec3> spine;
  private double pointTaper;
  private double edgeRadiusNm;
  private double edgeBevel;
  private List<Station> stations;
  private List<Fuller> fullers;
  private Flamboyance flamboyance;

  public BladeProfile(int version, List<Vec3> spine, double pointTaper, double edgeRadiusNm, double edgeBevel,
      List<Station> stations, List<Fuller> fullers, Flamboyance flamboyance) {
    this.version = version;
    if (version != CURRENT_VERSION) {
      throw new IllegalArgumentException("version must be " + CURRENT_VERSION);
    }
    if (spine == null || spine.size() < 2) {
      throw new IllegalArgumentException("spine must have at least 2 points");
    }
    this.spine = Objects.requireNonNull(spine, "spine");
    if (pointTaper < 0.0 || pointTaper > 1.0) {
      throw new IllegalArgumentException("pointTaper must be in [0,1]");
    }
    if (edgeRadiusNm <= 0.0) {
      throw new IllegalArgumentException("edgeRadiusNm must be > 0");
    }
    if (stations.size() < 2) {
      throw new IllegalArgumentException("stations must have at least 2 entries");
    }
    this.stations = Objects.requireNonNull(stations, "stations");
    this.pointTaper = pointTaper;
    this.edgeRadiusNm = edgeRadiusNm;
    this.edgeBevel = edgeBevel;
    this.fullers = Objects.requireNonNull(fullers, "fullers");
    this.flamboyance = Objects.requireNonNull(flamboyance, "flamboyance");
  }

  public record Station(double s, double width, double thickness, Section section) {

    public static final Codec<Station> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("s").forGetter(Station::s),
        Codec.DOUBLE.fieldOf("width").forGetter(Station::width),
        Codec.DOUBLE.fieldOf("thickness").forGetter(Station::thickness),
        Section.CODEC.fieldOf("section").forGetter(Station::section)).apply(instance, Station::new));

    public Station {
      if (s < 0.0 || s > 1.0) {
        throw new IllegalArgumentException("s must be in [0,1]");
      }
      if (width <= 0.0 || thickness <= 0.0) {
        throw new IllegalArgumentException("width/thickness must be > 0");
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
          throw new IllegalArgumentException("r must be > 0");
        }
        if (edgeOffset < -1.0 || edgeOffset > 1.0) {
          throw new IllegalArgumentException("edgeOffset must be in [-1,1]");
        }
        if (spineFlat < 0.0 || spineFlat > 1.0) {
          throw new IllegalArgumentException("spineFlat must be in [0,1]");
        }
      }

    }

  }

  public record Fuller(double s0, double s1, double width, double depth, String profile, int count) {
    public static final Codec<Fuller> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("s0").forGetter(Fuller::s0),
        Codec.DOUBLE.fieldOf("s1").forGetter(Fuller::s1),
        Codec.DOUBLE.fieldOf("width").forGetter(Fuller::width),
        Codec.DOUBLE.fieldOf("depth").forGetter(Fuller::depth),
        Codec.STRING.fieldOf("profile").forGetter(Fuller::profile),
        Codec.INT.fieldOf("count").forGetter(Fuller::count)).apply(instance, Fuller::new));

    public Fuller {
      if (s0 < 0.0 || s1 > 1.0 || s0 > s1) {
        throw new IllegalArgumentException("sRange must be within [0,1]");
      }
      if (width < 0.0 || depth < 0.0) {
        throw new IllegalArgumentException("fuller width/depth must be >= 0");
      }
      if (count < 1) {
        throw new IllegalArgumentException("fuller count must be >= 1");
      }
      Objects.requireNonNull(profile, "profile");
    }
  }

  public record Flamboyance(double amplitude, double frequency, double phase) {
    public static final Codec<Flamboyance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("amplitude").forGetter(Flamboyance::amplitude),
        Codec.DOUBLE.fieldOf("frequency").forGetter(Flamboyance::frequency),
        Codec.DOUBLE.fieldOf("phase").forGetter(Flamboyance::phase)).apply(instance, Flamboyance::new));

    public Flamboyance {
      if (amplitude < 0.0 || frequency < 0.0) {
        throw new IllegalArgumentException("flamboyance amplitude/frequency must be >= 0");
      }
    }
  }

  public record BladeSlice(double width, double thickness, double area, Vec3 position) {
  }

  public static MassProperties computeMassProperties(BladeProfile blade, double densityGPerCm3, int samples) {
    Objects.requireNonNull(blade, "blade");
    if (densityGPerCm3 <= 0.0) {
      throw new IllegalArgumentException("density must be > 0");
    }
    if (samples < 4) {
      Skada.LOGGER
          .error("Tried to compute properties of mass of blade with less than 4 samples. Using 200 samples instead.");
      samples = 200;
    }

    List<Station> sortedStations = new ArrayList<>(blade.stations);
    sortedStations.sort(Comparator.comparingDouble(Station::s));

    List<Vec3> spine = blade.spine;
    double totalLength = GeometryUtil.polylineLength(spine);
    double dl = totalLength / samples;

    double volume = 0.0;
    Vec3 weightedSum = new Vec3(0, 0, 0);

    for (int i = 0; i < samples; i++) {
      double s = (i + 0.5) / samples;
      Vec3 pos = GeometryUtil.pointOnPolyline(spine, s);

      Station interp = interpolateStation(sortedStations, s);
      double width = applyFlamboyance(interp.width(), blade.flamboyance, s);
      double thickness = interp.thickness();
      double[] tapered = applyPointTaper(width, thickness, blade.pointTaper, s, sortedStations);
      width = tapered[0];
      thickness = tapered[1];
      double area = GeometryUtil.modifiedSuperellipseArea(width, thickness, interp.section().r(),
          interp.section().spineFlat());
      area = Math.max(0.0, area - fullerAreaReduction(blade.fullers, s));

      double sliceVolume = area * dl;
      volume += sliceVolume;
      weightedSum = weightedSum.add(pos.mul(sliceVolume));
    }

    double mass = volume * densityGPerCm3;
    GeometryUtil.Vec3 com = volume > 0.0 ? weightedSum.mul(1.0 / volume) : new GeometryUtil.Vec3(0, 0, 0);
    return new MassProperties(volume, mass, com);
  }

  public static BladeSlice sampleSlice(BladeProfile blade, double s) {
    Objects.requireNonNull(blade, "blade");
    List<Station> sortedStations = new ArrayList<>(blade.stations);
    sortedStations.sort(Comparator.comparingDouble(Station::s));
    return sampleSlice(blade, sortedStations, s);
  }

  public static BladeSlice sampleSlice(BladeProfile blade, List<Station> sortedStations, double s) {
    Objects.requireNonNull(blade, "blade");
    Objects.requireNonNull(sortedStations, "sortedStations");
    GeometryUtil.Vec3 pos = GeometryUtil.pointOnPolyline(blade.spine, s);

    Station interp = interpolateStation(sortedStations, s);
    double width = applyFlamboyance(interp.width(), blade.flamboyance, s);
    double thickness = interp.thickness();
    double[] tapered = applyPointTaper(width, thickness, blade.pointTaper, s, sortedStations);
    width = tapered[0];
    thickness = tapered[1];
    double area = GeometryUtil.modifiedSuperellipseArea(width, thickness, interp.section().r(),
        interp.section().spineFlat());
    area = Math.max(0.0, area - fullerAreaReduction(blade.fullers, s));

    return new BladeSlice(width, thickness, area, pos);
  }

  private static double applyFlamboyance(double baseWidth, Flamboyance f, double s) {
    if (f == null) {
      return baseWidth;
    }
    double wobble = f.amplitude() * Math.sin((2.0 * Math.PI * f.frequency() * s) + f.phase());
    return Math.max(0.0, baseWidth + wobble);
  }

  private static double fullerAreaReduction(List<Fuller> f, double s) {
    if (f == null || f.isEmpty()) {
      return 0.0;
    }
    double reduction = 0.0;
    for (Fuller fuller : f) {
      if (s < fuller.s0() || s > fuller.s1()) {
        continue;
      }
      double shapeFactor = switch (fuller.profile()) {
        case "u" -> Math.PI / 4.0; // semicircle-like
        case "v" -> 0.5; // triangular
        case "flat" -> 1.0; // rectangle
        default -> 0.5;
      };
      reduction += fuller.count() * fuller.width() * fuller.depth() * shapeFactor;
    }
    return reduction;
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
        return new Station(s, width, thickness, new Section(r, edgeOffset, spineFlat));
      }
    }
    return stations.get(stations.size() - 1);
  }

  private static double[] applyPointTaper(double width, double thickness, double pointTaper, double s,
      List<Station> stations) {
    if (pointTaper <= 0.0) {
      pointTaper = 0.0;
    }
    double lastS = stations.get(stations.size() - 1).s();
    if (s <= lastS || lastS >= 1.0) {
      return new double[] { width, thickness };
    }
    double t = (s - lastS) / Math.max(1e-9, (1.0 - lastS));
    t = Math.min(1.0, Math.max(0.0, t));

    double clampedPointTaper = Math.min(1.0, Math.max(0.0, pointTaper));
    double r = 1.0 + ((1.0 - clampedPointTaper) * 4.0);
    double profileScale = 1.0 - Math.pow(t, r);
    profileScale = Math.min(1.0, Math.max(0.0, profileScale));

    double widthT = width * profileScale;
    double thicknessT = thickness * profileScale;
    return new double[] { Math.max(0.0, widthT), Math.max(0.0, thicknessT) };
  }

  public List<Station> getStations() {
    return stations;
  }

  public int getVersion() {
    return version;
  }

  public List<Vec3> getSpine() {
    return spine;
  }

  public List<Fuller> getFullers() {
    return fullers;
  }

  public Flamboyance getFlamboyance() {
    return flamboyance;
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

  public double length() {
    return GeometryUtil.polylineLength(spine);
  }

  public BladeSlice sampleSliceAt(double s) {
    return sampleSlice(this, s);
  }

  public Station sampleStationAt(double s) {
    List<Station> sortedStations = new ArrayList<>(stations);
    sortedStations.sort(Comparator.comparingDouble(Station::s));
    return interpolateStation(sortedStations, Math.max(0.0, Math.min(1.0, s)));
  }

}
