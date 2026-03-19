package com.cwjn.skada.data.gen.weapon.profile;

import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Bounds;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Geometry and runtime physics model for shovel head profiles.
 *
 * Units: centimeters for geometry, grams for mass, g/cm^3 for density.
 */
public final class ShovelHeadProfile {

  private static final int CURRENT_VERSION = 1;
  private static final double LIP_RADIUS_MM_TO_CM = 0.1;
  private static final int DEFAULT_DISH_SHAPE_STEPS = 48;
  private static final double DEFAULT_DISH_EXPONENT = 2.0;
  private static final double MIN_CONTACT_AREA_CM2 = 0.02;

  public static final Codec<ShovelHeadProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("version").forGetter(ShovelHeadProfile::getVersion),
      Mount.CODEC.fieldOf("mount").forGetter(ShovelHeadProfile::getMount),
      Shoulder.CODEC.fieldOf("shoulder").forGetter(ShovelHeadProfile::getShoulder),
      Blade.CODEC.fieldOf("blade").forGetter(ShovelHeadProfile::getBlade),
      Reinforcement.CODEC.optionalFieldOf("reinforcement")
          .forGetter(profile -> Optional.ofNullable(profile.getReinforcement())),
      ImpactOverride.CODEC.optionalFieldOf("impactOverride")
          .forGetter(profile -> Optional.ofNullable(profile.getImpactOverride())))
      .apply(instance, (version, mount, shoulder, blade, reinforcement, impactOverride) -> new ShovelHeadProfile(
          version,
          mount,
          shoulder,
          blade,
          reinforcement.orElse(null),
          impactOverride.orElse(null))));

  private final int version;
  private final Mount mount;
  private final Shoulder shoulder;
  private final Blade blade;
  private final Reinforcement reinforcement;
  private final ImpactOverride impactOverride;

  private record ComponentMass(double volumeCm3, Vec3 center) {
    private static final ComponentMass ZERO = new ComponentMass(0.0, new Vec3(0.0, 0.0, 0.0));
  }

  private record DishStats(double arcWidth, double centroidZ, double minZ, double maxZ) {
  }

  private record BladeSample(double halfWidth, double thickness, double dishDepth, double crownOffset) {
  }

  public record Mount(
      String type,
      double length,
      double rearOuterWidth,
      double rearOuterThickness,
      double frontOuterWidth,
      double frontOuterThickness,
      double innerWidth,
      double innerThickness,
      double wallThickness,
      double transitionLength,
      Double strapLength,
      Double strapThickness,
      Double strapWidth) {

    public static final Codec<Mount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("type").forGetter(Mount::type),
        Codec.DOUBLE.fieldOf("length").forGetter(Mount::length),
        Codec.DOUBLE.fieldOf("rearOuterWidth").forGetter(Mount::rearOuterWidth),
        Codec.DOUBLE.fieldOf("rearOuterThickness").forGetter(Mount::rearOuterThickness),
        Codec.DOUBLE.fieldOf("frontOuterWidth").forGetter(Mount::frontOuterWidth),
        Codec.DOUBLE.fieldOf("frontOuterThickness").forGetter(Mount::frontOuterThickness),
        Codec.DOUBLE.fieldOf("innerWidth").forGetter(Mount::innerWidth),
        Codec.DOUBLE.fieldOf("innerThickness").forGetter(Mount::innerThickness),
        Codec.DOUBLE.fieldOf("wallThickness").forGetter(Mount::wallThickness),
        Codec.DOUBLE.fieldOf("transitionLength").forGetter(Mount::transitionLength),
        Codec.DOUBLE.optionalFieldOf("strapLength").forGetter(mount -> Optional.ofNullable(mount.strapLength())),
        Codec.DOUBLE.optionalFieldOf("strapThickness")
            .forGetter(mount -> Optional.ofNullable(mount.strapThickness())),
        Codec.DOUBLE.optionalFieldOf("strapWidth").forGetter(mount -> Optional.ofNullable(mount.strapWidth())))
        .apply(instance, (type, length, rearOuterWidth, rearOuterThickness, frontOuterWidth, frontOuterThickness,
            innerWidth, innerThickness, wallThickness, transitionLength, strapLength, strapThickness, strapWidth) -> new Mount(
                type,
                length,
                rearOuterWidth,
                rearOuterThickness,
                frontOuterWidth,
                frontOuterThickness,
                innerWidth,
                innerThickness,
                wallThickness,
                transitionLength,
                strapLength.orElse(null),
                strapThickness.orElse(null),
                strapWidth.orElse(null))));

    public Mount {
      Objects.requireNonNull(type, "type");
      if (!"socket".equals(type) && !"strapSocket".equals(type)) {
        throw new IllegalArgumentException("mount.type must be \"socket\" or \"strapSocket\"");
      }
      requirePositive(length, "mount.length");
      requirePositive(rearOuterWidth, "mount.rearOuterWidth");
      requirePositive(rearOuterThickness, "mount.rearOuterThickness");
      requirePositive(frontOuterWidth, "mount.frontOuterWidth");
      requirePositive(frontOuterThickness, "mount.frontOuterThickness");
      requirePositive(innerWidth, "mount.innerWidth");
      requirePositive(innerThickness, "mount.innerThickness");
      requirePositive(wallThickness, "mount.wallThickness");
      if (transitionLength < 0.0 || transitionLength > length) {
        throw new IllegalArgumentException("mount.transitionLength must be in [0,length]");
      }
      if (innerWidth >= rearOuterWidth || innerWidth >= frontOuterWidth) {
        throw new IllegalArgumentException("mount.innerWidth must remain smaller than outer widths");
      }
      if (innerThickness >= rearOuterThickness || innerThickness >= frontOuterThickness) {
        throw new IllegalArgumentException("mount.innerThickness must remain smaller than outer thicknesses");
      }

      if ("strapSocket".equals(type)) {
        requirePositive(strapLength, "mount.strapLength");
        requirePositive(strapThickness, "mount.strapThickness");
        requirePositive(strapWidth, "mount.strapWidth");
      } else if (strapLength != null || strapThickness != null || strapWidth != null) {
        throw new IllegalArgumentException("strap fields must be omitted unless mount.type is \"strapSocket\"");
      }
    }
  }

  public record Shoulder(
      double length,
      double entryHalfWidth,
      double exitHalfWidth,
      double entryThickness,
      double exitThickness,
      double crownHeight) {

    public static final Codec<Shoulder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("length").forGetter(Shoulder::length),
        Codec.DOUBLE.fieldOf("entryHalfWidth").forGetter(Shoulder::entryHalfWidth),
        Codec.DOUBLE.fieldOf("exitHalfWidth").forGetter(Shoulder::exitHalfWidth),
        Codec.DOUBLE.fieldOf("entryThickness").forGetter(Shoulder::entryThickness),
        Codec.DOUBLE.fieldOf("exitThickness").forGetter(Shoulder::exitThickness),
        Codec.DOUBLE.fieldOf("crownHeight").forGetter(Shoulder::crownHeight))
        .apply(instance, Shoulder::new));

    public Shoulder {
      requirePositive(length, "shoulder.length");
      requirePositive(entryHalfWidth, "shoulder.entryHalfWidth");
      requirePositive(exitHalfWidth, "shoulder.exitHalfWidth");
      requirePositive(entryThickness, "shoulder.entryThickness");
      requirePositive(exitThickness, "shoulder.exitThickness");
      requirePositive(crownHeight, "shoulder.crownHeight");
      if (exitHalfWidth < entryHalfWidth) {
        throw new IllegalArgumentException("shoulder.exitHalfWidth must be >= shoulder.entryHalfWidth");
      }
    }
  }

  public record Station(double s, double halfWidth, double thickness, double dishDepth, double crownOffset) {
    public static final Codec<Station> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("s").forGetter(Station::s),
        Codec.DOUBLE.fieldOf("halfWidth").forGetter(Station::halfWidth),
        Codec.DOUBLE.fieldOf("thickness").forGetter(Station::thickness),
        Codec.DOUBLE.fieldOf("dishDepth").forGetter(Station::dishDepth),
        Codec.DOUBLE.fieldOf("crownOffset").forGetter(Station::crownOffset))
        .apply(instance, Station::new));

    public Station {
      if (s < 0.0 || s > 1.0) {
        throw new IllegalArgumentException("blade station s must be in [0,1]");
      }
      requirePositive(halfWidth, "blade station halfWidth");
      requirePositive(thickness, "blade station thickness");
      requirePositive(dishDepth, "blade station dishDepth");
    }
  }

  public record Edge(String profile, double lipRadiusMm, double bevelLength) {
    public static final Codec<Edge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("profile").forGetter(Edge::profile),
        Codec.DOUBLE.fieldOf("lipRadiusMm").forGetter(Edge::lipRadiusMm),
        Codec.DOUBLE.fieldOf("bevelLength").forGetter(Edge::bevelLength))
        .apply(instance, Edge::new));

    public Edge {
      Objects.requireNonNull(profile, "profile");
      if (!"rounded".equals(profile) && !"flat".equals(profile) && !"pointed".equals(profile)) {
        throw new IllegalArgumentException("blade.edge.profile must be \"rounded\", \"flat\", or \"pointed\"");
      }
      requirePositive(lipRadiusMm, "blade.edge.lipRadiusMm");
      requirePositive(bevelLength, "blade.edge.bevelLength");
    }
  }

  public record Blade(double length, List<Station> stations, Edge edge) {
    public static final Codec<Blade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("length").forGetter(Blade::length),
        Station.CODEC.listOf().fieldOf("stations").forGetter(Blade::stations),
        Edge.CODEC.fieldOf("edge").forGetter(Blade::edge))
        .apply(instance, Blade::new));

    public Blade {
      requirePositive(length, "blade.length");
      stations = List.copyOf(Objects.requireNonNull(stations, "stations"));
      Objects.requireNonNull(edge, "edge");
      if (stations.size() < 2) {
        throw new IllegalArgumentException("blade.stations must contain at least 2 entries");
      }
      double previousS = -1.0;
      for (Station station : stations) {
        if (station.s() < previousS) {
          throw new IllegalArgumentException("blade station s values must be monotonic increasing");
        }
        previousS = station.s();
      }
      if (edge.bevelLength() > length) {
        throw new IllegalArgumentException("blade.edge.bevelLength must not exceed blade.length");
      }
    }
  }

  public record CenterRib(double startS, double endS, double height, double baseWidth) {
    public static final Codec<CenterRib> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("startS").forGetter(CenterRib::startS),
        Codec.DOUBLE.fieldOf("endS").forGetter(CenterRib::endS),
        Codec.DOUBLE.fieldOf("height").forGetter(CenterRib::height),
        Codec.DOUBLE.fieldOf("baseWidth").forGetter(CenterRib::baseWidth))
        .apply(instance, CenterRib::new));

    public CenterRib {
      validateNormalizedRange(startS, endS, "reinforcement.centerRib");
      requirePositive(height, "reinforcement.centerRib.height");
      requirePositive(baseWidth, "reinforcement.centerRib.baseWidth");
    }
  }

  public record SideFlanges(double startS, double endS, double height, double width) {
    public static final Codec<SideFlanges> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("startS").forGetter(SideFlanges::startS),
        Codec.DOUBLE.fieldOf("endS").forGetter(SideFlanges::endS),
        Codec.DOUBLE.fieldOf("height").forGetter(SideFlanges::height),
        Codec.DOUBLE.fieldOf("width").forGetter(SideFlanges::width))
        .apply(instance, SideFlanges::new));

    public SideFlanges {
      validateNormalizedRange(startS, endS, "reinforcement.sideFlanges");
      requirePositive(height, "reinforcement.sideFlanges.height");
      requirePositive(width, "reinforcement.sideFlanges.width");
    }
  }

  public record HeelThickening(double length, double extraThickness) {
    public static final Codec<HeelThickening> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("length").forGetter(HeelThickening::length),
        Codec.DOUBLE.fieldOf("extraThickness").forGetter(HeelThickening::extraThickness))
        .apply(instance, HeelThickening::new));

    public HeelThickening {
      requirePositive(length, "reinforcement.heelThickening.length");
      requirePositive(extraThickness, "reinforcement.heelThickening.extraThickness");
    }
  }

  public record Reinforcement(CenterRib centerRib, SideFlanges sideFlanges, HeelThickening heelThickening) {
    public static final Codec<Reinforcement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        CenterRib.CODEC.optionalFieldOf("centerRib").forGetter(reinforcement -> Optional.ofNullable(reinforcement.centerRib())),
        SideFlanges.CODEC.optionalFieldOf("sideFlanges")
            .forGetter(reinforcement -> Optional.ofNullable(reinforcement.sideFlanges())),
        HeelThickening.CODEC.optionalFieldOf("heelThickening")
            .forGetter(reinforcement -> Optional.ofNullable(reinforcement.heelThickening())))
        .apply(instance, (centerRib, sideFlanges, heelThickening) -> new Reinforcement(
            centerRib.orElse(null),
            sideFlanges.orElse(null),
            heelThickening.orElse(null))));
  }

  public record ImpactOverride(
      String contactRegion,
      double effectiveContactAreaCm2,
      double focusFactor,
      double rigidity,
      double stability) {

    public static final Codec<ImpactOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("contactRegion").forGetter(ImpactOverride::contactRegion),
        Codec.DOUBLE.fieldOf("effectiveContactAreaCm2").forGetter(ImpactOverride::effectiveContactAreaCm2),
        Codec.DOUBLE.fieldOf("focusFactor").forGetter(ImpactOverride::focusFactor),
        Codec.DOUBLE.fieldOf("rigidity").forGetter(ImpactOverride::rigidity),
        Codec.DOUBLE.fieldOf("stability").forGetter(ImpactOverride::stability))
        .apply(instance, ImpactOverride::new));

    public ImpactOverride {
      Objects.requireNonNull(contactRegion, "contactRegion");
      validateContactRegion(contactRegion);
      requirePositive(effectiveContactAreaCm2, "impactOverride.effectiveContactAreaCm2");
      requirePositive(focusFactor, "impactOverride.focusFactor");
      requirePositive(rigidity, "impactOverride.rigidity");
      requirePositive(stability, "impactOverride.stability");
    }
  }

  public record DerivedStrikeGeometry(
      String contactRegion,
      double contactPointX,
      double effectiveContactAreaCm2,
      double focusFactor,
      double rigidity,
      double stability) {
  }

  public ShovelHeadProfile(
      int version,
      Mount mount,
      Shoulder shoulder,
      Blade blade,
      Reinforcement reinforcement,
      ImpactOverride impactOverride) {
    this.version = version;
    this.mount = Objects.requireNonNull(mount, "mount");
    this.shoulder = Objects.requireNonNull(shoulder, "shoulder");
    this.blade = Objects.requireNonNull(blade, "blade");
    this.reinforcement = reinforcement;
    this.impactOverride = impactOverride;

    if (version != CURRENT_VERSION) {
      throw new IllegalArgumentException("version must be " + CURRENT_VERSION);
    }

    HeelThickening heel = reinforcement == null ? null : reinforcement.heelThickening();
    if (heel != null && heel.length() > (shoulder.length() + (blade.length() / 3.0))) {
      throw new IllegalArgumentException(
          "reinforcement.heelThickening.length should not exceed shoulder length plus one third of blade length");
    }
  }

  public static MassProperties computeMassProperties(ShovelHeadProfile profile, double densityGPerCm3, int samples) {
    Objects.requireNonNull(profile, "profile");
    if (densityGPerCm3 <= 0.0) {
      throw new IllegalArgumentException("density must be > 0");
    }

    int axialSamples = Math.max(24, samples);
    ComponentMass mountMass = computeMountMass(profile, axialSamples);
    ComponentMass shoulderMass = computeShoulderMass(profile, axialSamples);
    ComponentMass bladeMass = computeBladeMass(profile, axialSamples);
    ComponentMass ribMass = computeCenterRibMass(profile);
    ComponentMass flangeMass = computeSideFlangeMass(profile);
    ComponentMass heelMass = computeHeelMass(profile);

    double totalVolume = mountMass.volumeCm3() + shoulderMass.volumeCm3() + bladeMass.volumeCm3() + ribMass.volumeCm3()
        + flangeMass.volumeCm3() + heelMass.volumeCm3();
    Vec3 weighted = mountMass.center().mul(mountMass.volumeCm3())
        .add(shoulderMass.center().mul(shoulderMass.volumeCm3()))
        .add(bladeMass.center().mul(bladeMass.volumeCm3()))
        .add(ribMass.center().mul(ribMass.volumeCm3()))
        .add(flangeMass.center().mul(flangeMass.volumeCm3()))
        .add(heelMass.center().mul(heelMass.volumeCm3()));

    Vec3 center = totalVolume > 0.0 ? weighted.mul(1.0 / totalVolume) : new Vec3(0.0, 0.0, 0.0);
    return new MassProperties(totalVolume, totalVolume * densityGPerCm3, center);
  }

  public MassProperties computeMassProperties(double densityGPerCm3, int samples) {
    return computeMassProperties(this, densityGPerCm3, samples);
  }

  public static Bounds localBounds(ShovelHeadProfile profile, int samples) {
    Objects.requireNonNull(profile, "profile");

    double minX = -profile.mount.length();
    double maxX = profile.shoulder.length() + profile.blade.length();
    double maxY = Math.max(
        Math.max(profile.mount.rearOuterWidth(), profile.mount.frontOuterWidth()) * 0.5,
        Math.max(profile.shoulder.exitHalfWidth(), maxBladeHalfWidth(profile.blade)));
    double minZ = -Math.max(profile.mount.rearOuterThickness(), profile.mount.frontOuterThickness()) * 0.5;
    double maxZ = Math.max(profile.mount.rearOuterThickness(), profile.mount.frontOuterThickness()) * 0.5;

    int bladeSamples = Math.max(24, samples);
    for (int i = 0; i <= bladeSamples; i++) {
      double s = (double) i / bladeSamples;
      BladeSample sample = sampleBlade(profile.blade, s);
      DishStats stats = computeDishStats(sample.halfWidth(), sample.dishDepth(), sample.crownOffset(), DEFAULT_DISH_EXPONENT,
          DEFAULT_DISH_SHAPE_STEPS);
      minZ = Math.min(minZ, stats.minZ() - (sample.thickness() * 0.5));
      maxZ = Math.max(maxZ, stats.maxZ() + (sample.thickness() * 0.5));
      maxY = Math.max(maxY, sample.halfWidth());
    }

    for (int i = 0; i <= bladeSamples; i++) {
      double t = (double) i / bladeSamples;
      double crown = shoulderCrown(profile.shoulder, t);
      double thickness = GeometryUtil.lerp(profile.shoulder.entryThickness(), profile.shoulder.exitThickness(), t);
      maxZ = Math.max(maxZ, crown + (thickness * 0.5));
      minZ = Math.min(minZ, crown - (thickness * 0.5));
    }

    Reinforcement reinforcement = profile.reinforcement;
    if (reinforcement != null) {
      if (reinforcement.centerRib() != null) {
        CenterRib rib = reinforcement.centerRib();
        BladeSample sample = sampleBlade(profile.blade, (rib.startS() + rib.endS()) * 0.5);
        DishStats stats = computeDishStats(sample.halfWidth(), sample.dishDepth(), sample.crownOffset(), DEFAULT_DISH_EXPONENT,
            DEFAULT_DISH_SHAPE_STEPS);
        maxZ = Math.max(maxZ, stats.centroidZ() + rib.height());
      }

      if (reinforcement.sideFlanges() != null) {
        SideFlanges flanges = reinforcement.sideFlanges();
        BladeSample sample = sampleBlade(profile.blade, (flanges.startS() + flanges.endS()) * 0.5);
        maxY = Math.max(maxY, sample.halfWidth() + flanges.width());
        DishStats stats = computeDishStats(sample.halfWidth(), sample.dishDepth(), sample.crownOffset(), DEFAULT_DISH_EXPONENT,
            DEFAULT_DISH_SHAPE_STEPS);
        maxZ = Math.max(maxZ, stats.maxZ() + flanges.height());
      }

      if (reinforcement.heelThickening() != null) {
        HeelThickening heel = reinforcement.heelThickening();
        BladeSample sample = sampleBlade(profile.blade, 0.0);
        DishStats stats = computeDishStats(sample.halfWidth(), sample.dishDepth(), sample.crownOffset(), DEFAULT_DISH_EXPONENT,
            DEFAULT_DISH_SHAPE_STEPS);
        maxZ = Math.max(maxZ, stats.maxZ() + heel.extraThickness());
        minZ = Math.min(minZ, stats.minZ() - heel.extraThickness());
      }
    }

    if ("strapSocket".equals(profile.mount.type())) {
      maxY = Math.max(maxY, (Math.max(profile.mount.rearOuterWidth(), profile.mount.frontOuterWidth()) * 0.5) + profile.mount.strapWidth());
      maxZ = Math.max(maxZ, profile.mount.strapThickness() * 0.5);
      minZ = Math.min(minZ, -profile.mount.strapThickness() * 0.5);
    }

    return new Bounds(minX, maxX, -maxY, maxY, minZ, maxZ);
  }

  public Bounds localBounds(int samples) {
    return localBounds(this, samples);
  }

  public DerivedStrikeGeometry deriveStrikeGeometry() {
    String contactRegion = impactOverride == null
        ? defaultContactRegion(blade.edge().profile())
        : impactOverride.contactRegion();
    double contactPointX = resolveContactPointX(contactRegion);

    if (impactOverride != null) {
      return new DerivedStrikeGeometry(
          contactRegion,
          contactPointX,
          impactOverride.effectiveContactAreaCm2(),
          impactOverride.focusFactor(),
          impactOverride.rigidity(),
          impactOverride.stability());
    }

    double sampleS = switch (contactRegion) {
      case "heel" -> 0.08;
      case "corner" -> 0.94;
      default -> 0.98;
    };
    BladeSample sample = sampleBlade(blade, sampleS);
    double lipRadiusCm = blade.edge().lipRadiusMm() * LIP_RADIUS_MM_TO_CM;
    double bladeWidth = sample.halfWidth() * 2.0;
    double contactSpan = switch (blade.edge().profile()) {
      case "pointed" -> clamp(bladeWidth * 0.28, 0.9, 3.1);
      case "flat" -> clamp(bladeWidth * 0.72, 2.4, Math.max(2.4, bladeWidth));
      default -> clamp(bladeWidth * 0.54, 1.6, Math.max(1.6, bladeWidth * 0.9));
    };
    if ("corner".equals(contactRegion)) {
      contactSpan *= 0.78;
    }
    if ("heel".equals(contactRegion)) {
      contactSpan *= 1.12;
    }

    double localDepthFactor = 1.0 + clamp(sample.dishDepth() / Math.max(1.0e-6, sample.halfWidth()), 0.0, 0.35);
    double contactThickness = Math.max(0.2, (lipRadiusCm * 2.0) + (sample.thickness() * 0.32));
    double contactArea = Math.max(MIN_CONTACT_AREA_CM2, contactSpan * contactThickness / localDepthFactor);

    double areaTightness = 1.0 / Math.max(0.75, contactArea);
    double focus = 0.82 + (0.11 * areaTightness) + (0.10 * sample.dishDepth() / Math.max(1.0e-6, sample.halfWidth()));
    if ("pointed".equals(blade.edge().profile())) {
      focus += 0.06;
    }
    if ("heel".equals(contactRegion)) {
      focus -= 0.05;
    }

    double rigidity = 0.88 + (0.18 * shoulder.entryThickness()) + (0.08 * shoulder.exitThickness());
    if (reinforcement != null && reinforcement.centerRib() != null) {
      rigidity += 0.11 * reinforcement.centerRib().height();
    }
    if (reinforcement != null && reinforcement.sideFlanges() != null) {
      rigidity += 0.06 * reinforcement.sideFlanges().height();
    }
    if (reinforcement != null && reinforcement.heelThickening() != null) {
      rigidity += 0.04 * reinforcement.heelThickening().extraThickness() / Math.max(0.05, sample.thickness());
    }

    double supportWidth = "heel".equals(contactRegion)
        ? shoulder.exitHalfWidth() * 2.0
        : GeometryUtil.lerp(shoulder.exitHalfWidth() * 2.0, bladeWidth, 0.35);
    double stability = 0.9 + (0.05 * clamp(supportWidth / Math.max(1.0, bladeWidth), 0.6, 1.25));
    stability += 0.04 * clamp(mount.frontOuterWidth() / Math.max(1.0, shoulder.entryHalfWidth() * 2.0), 0.7, 1.2);
    if (reinforcement != null && reinforcement.centerRib() != null) {
      stability += 0.06;
    }
    if (reinforcement != null && reinforcement.sideFlanges() != null) {
      stability += 0.05;
    }

    return new DerivedStrikeGeometry(
        contactRegion,
        contactPointX,
        contactArea,
        clamp(focus, 0.82, 1.2),
        clamp(rigidity, 0.9, 1.28),
        clamp(stability, 0.9, 1.22));
  }

  public double deriveStrikeComplianceFactor() {
    DerivedStrikeGeometry strike = deriveStrikeGeometry();
    if (impactOverride != null) {
      double overrideFactor = 0.76
          + (0.12 * clamp(strike.rigidity(), 0.85, 1.25))
          + (0.12 * clamp(strike.stability(), 0.85, 1.20))
          - (0.04 * clamp(strike.effectiveContactAreaCm2() / 4.0, 0.4, 1.4));
      return clamp(overrideFactor, 0.72, 1.02);
    }

    double sampleS = switch (strike.contactRegion()) {
      case "heel" -> 0.08;
      case "corner" -> 0.94;
      default -> 0.98;
    };
    BladeSample sample = sampleBlade(blade, sampleS);
    double bladeWidth = Math.max(1.0e-6, sample.halfWidth() * 2.0);
    double supportWidth = "heel".equals(strike.contactRegion())
        ? shoulder.exitHalfWidth() * 2.0
        : GeometryUtil.lerp(shoulder.exitHalfWidth() * 2.0, bladeWidth, 0.35);

    double thicknessFactor = clamp(sample.thickness() / 0.30, 0.45, 1.05);
    double supportFactor = clamp(supportWidth / bladeWidth, 0.60, 1.10);
    double dishRatio = clamp(sample.dishDepth() / Math.max(1.0e-6, sample.halfWidth()), 0.0, 0.40);

    double compliance = 0.62 + (0.16 * thicknessFactor) + (0.10 * supportFactor) - (0.16 * dishRatio);
    if (reinforcement != null && reinforcement.centerRib() != null) {
      compliance += 0.05;
    }
    if (reinforcement != null && reinforcement.sideFlanges() != null) {
      compliance += 0.04;
    }
    if (reinforcement != null && reinforcement.heelThickening() != null) {
      compliance += 0.03 * clamp(reinforcement.heelThickening().extraThickness() / Math.max(0.05, sample.thickness()), 0.0, 1.0);
    }
    if ("heel".equals(strike.contactRegion())) {
      compliance += 0.03;
    }
    if ("corner".equals(strike.contactRegion())) {
      compliance -= 0.02;
    }

    return clamp(compliance, 0.72, 1.02);
  }

  public double deriveStrikeGlancingFactor() {
    DerivedStrikeGeometry strike = deriveStrikeGeometry();
    if (impactOverride != null) {
      double overrideFactor = 0.80
          + (0.10 * clamp(strike.focusFactor(), 0.82, 1.20))
          - (0.04 * clamp(strike.effectiveContactAreaCm2() / 4.0, 0.4, 1.4));
      return clamp(overrideFactor, 0.72, 1.02);
    }

    double sampleS = switch (strike.contactRegion()) {
      case "heel" -> 0.08;
      case "corner" -> 0.94;
      default -> 0.98;
    };
    BladeSample sample = sampleBlade(blade, sampleS);
    double bladeWidth = sample.halfWidth() * 2.0;
    double contactSpan = switch (blade.edge().profile()) {
      case "pointed" -> clamp(bladeWidth * 0.28, 0.9, 3.1);
      case "flat" -> clamp(bladeWidth * 0.72, 2.4, Math.max(2.4, bladeWidth));
      default -> clamp(bladeWidth * 0.54, 1.6, Math.max(1.6, bladeWidth * 0.9));
    };
    if ("corner".equals(strike.contactRegion())) {
      contactSpan *= 0.78;
    }
    if ("heel".equals(strike.contactRegion())) {
      contactSpan *= 1.12;
    }

    double spanFactor = clamp(3.0 / Math.max(1.0, contactSpan), 0.70, 1.08);
    double dishRatio = clamp(sample.dishDepth() / Math.max(1.0e-6, sample.halfWidth()), 0.0, 0.35);

    double glancing = 0.74 + (0.18 * spanFactor) - (0.16 * dishRatio);
    if ("pointed".equals(blade.edge().profile())) {
      glancing += 0.05;
    }
    if ("heel".equals(strike.contactRegion())) {
      glancing += 0.04;
    }
    if ("corner".equals(strike.contactRegion())) {
      glancing += 0.02;
    }

    return clamp(glancing, 0.72, 1.02);
  }

  public int getVersion() {
    return version;
  }

  public Mount getMount() {
    return mount;
  }

  public Shoulder getShoulder() {
    return shoulder;
  }

  public Blade getBlade() {
    return blade;
  }

  public Reinforcement getReinforcement() {
    return reinforcement;
  }

  public ImpactOverride getImpactOverride() {
    return impactOverride;
  }

  public double bladeStartX() {
    return shoulder.length();
  }

  public double totalForwardLength() {
    return shoulder.length() + blade.length();
  }

  private double resolveContactPointX(String contactRegion) {
    return switch (contactRegion) {
      case "heel" -> shoulder.length() + Math.min(blade.length() * 0.16, 2.2);
      case "corner" -> shoulder.length() + blade.length() - Math.max(blade.edge().bevelLength() * 0.35, 0.55);
      default -> shoulder.length() + blade.length() - Math.max(blade.edge().bevelLength() * 0.5, 0.8);
    };
  }

  private static ComponentMass computeMountMass(ShovelHeadProfile profile, int samples) {
    Mount mount = profile.mount;
    double dx = mount.length() / samples;
    double volume = 0.0;
    Vec3 weighted = new Vec3(0.0, 0.0, 0.0);

    for (int i = 0; i < samples; i++) {
      double t = (i + 0.5) / samples;
      double outerWidth = GeometryUtil.lerp(mount.rearOuterWidth(), mount.frontOuterWidth(), t);
      double outerThickness = GeometryUtil.lerp(mount.rearOuterThickness(), mount.frontOuterThickness(), t);
      double shellArea = Math.max(0.0, (outerWidth * outerThickness) - (mount.innerWidth() * mount.innerThickness()));
      double sliceVolume = shellArea * dx;
      double x = -mount.length() + (t * mount.length());
      volume += sliceVolume;
      weighted = weighted.add(new Vec3(x * sliceVolume, 0.0, 0.0));
    }

    if ("strapSocket".equals(mount.type())) {
      double strapVolume = 2.0 * mount.strapLength() * mount.strapThickness() * mount.strapWidth();
      double strapX = -mount.strapLength() * 0.5;
      volume += strapVolume;
      weighted = weighted.add(new Vec3(strapX * strapVolume, 0.0, 0.0));
    }

    return volume > 0.0 ? new ComponentMass(volume, weighted.mul(1.0 / volume)) : ComponentMass.ZERO;
  }

  private static ComponentMass computeShoulderMass(ShovelHeadProfile profile, int samples) {
    Shoulder shoulder = profile.shoulder;
    double dx = shoulder.length() / samples;
    double volume = 0.0;
    Vec3 weighted = new Vec3(0.0, 0.0, 0.0);

    for (int i = 0; i < samples; i++) {
      double t = (i + 0.5) / samples;
      double halfWidth = GeometryUtil.lerp(shoulder.entryHalfWidth(), shoulder.exitHalfWidth(), t);
      double thickness = GeometryUtil.lerp(shoulder.entryThickness(), shoulder.exitThickness(), t);
      double crown = shoulderCrown(shoulder, t);
      double sliceVolume = (halfWidth * 2.0) * thickness * dx;
      double x = t * shoulder.length();
      volume += sliceVolume;
      weighted = weighted.add(new Vec3(x * sliceVolume, 0.0, crown * sliceVolume));
    }

    return volume > 0.0 ? new ComponentMass(volume, weighted.mul(1.0 / volume)) : ComponentMass.ZERO;
  }

  private static ComponentMass computeBladeMass(ShovelHeadProfile profile, int samples) {
    double dx = profile.blade.length() / samples;
    double volume = 0.0;
    Vec3 weighted = new Vec3(0.0, 0.0, 0.0);

    for (int i = 0; i < samples; i++) {
      double s = (i + 0.5) / samples;
      BladeSample sample = sampleBlade(profile.blade, s);
      DishStats stats = computeDishStats(sample.halfWidth(), sample.dishDepth(), sample.crownOffset(), DEFAULT_DISH_EXPONENT,
          DEFAULT_DISH_SHAPE_STEPS);
      double sliceVolume = stats.arcWidth() * sample.thickness() * dx;
      double x = profile.shoulder.length() + (s * profile.blade.length());
      volume += sliceVolume;
      weighted = weighted.add(new Vec3(x * sliceVolume, 0.0, stats.centroidZ() * sliceVolume));
    }

    return volume > 0.0 ? new ComponentMass(volume, weighted.mul(1.0 / volume)) : ComponentMass.ZERO;
  }

  private static ComponentMass computeCenterRibMass(ShovelHeadProfile profile) {
    Reinforcement reinforcement = profile.reinforcement;
    if (reinforcement == null || reinforcement.centerRib() == null) {
      return ComponentMass.ZERO;
    }

    CenterRib rib = reinforcement.centerRib();
    double length = (rib.endS() - rib.startS()) * profile.blade.length();
    double area = 0.5 * rib.baseWidth() * rib.height();
    double volume = area * length;
    double midS = (rib.startS() + rib.endS()) * 0.5;
    BladeSample sample = sampleBlade(profile.blade, midS);
    DishStats stats = computeDishStats(sample.halfWidth(), sample.dishDepth(), sample.crownOffset(), DEFAULT_DISH_EXPONENT,
        DEFAULT_DISH_SHAPE_STEPS);
    double x = profile.shoulder.length() + (midS * profile.blade.length());
    double z = stats.centroidZ() + (rib.height() / 3.0);
    return new ComponentMass(volume, new Vec3(x, 0.0, z));
  }

  private static ComponentMass computeSideFlangeMass(ShovelHeadProfile profile) {
    Reinforcement reinforcement = profile.reinforcement;
    if (reinforcement == null || reinforcement.sideFlanges() == null) {
      return ComponentMass.ZERO;
    }

    SideFlanges flanges = reinforcement.sideFlanges();
    double length = (flanges.endS() - flanges.startS()) * profile.blade.length();
    double areaPerFlange = 0.5 * flanges.width() * flanges.height();
    double volume = 2.0 * areaPerFlange * length;
    double midS = (flanges.startS() + flanges.endS()) * 0.5;
    BladeSample sample = sampleBlade(profile.blade, midS);
    DishStats stats = computeDishStats(sample.halfWidth(), sample.dishDepth(), sample.crownOffset(), DEFAULT_DISH_EXPONENT,
        DEFAULT_DISH_SHAPE_STEPS);
    double x = profile.shoulder.length() + (midS * profile.blade.length());
    double z = stats.maxZ() + (flanges.height() * 0.5);
    return new ComponentMass(volume, new Vec3(x, 0.0, z));
  }

  private static ComponentMass computeHeelMass(ShovelHeadProfile profile) {
    Reinforcement reinforcement = profile.reinforcement;
    if (reinforcement == null || reinforcement.heelThickening() == null) {
      return ComponentMass.ZERO;
    }

    HeelThickening heel = reinforcement.heelThickening();
    double normalizedLength = clamp(heel.length() / profile.blade.length(), 0.0, 1.0);
    BladeSample start = sampleBlade(profile.blade, 0.0);
    BladeSample end = sampleBlade(profile.blade, normalizedLength);
    double avgWidth = start.halfWidth() + end.halfWidth();
    double volume = avgWidth * heel.extraThickness() * heel.length();
    BladeSample mid = sampleBlade(profile.blade, normalizedLength * 0.5);
    DishStats stats = computeDishStats(mid.halfWidth(), mid.dishDepth(), mid.crownOffset(), DEFAULT_DISH_EXPONENT,
        DEFAULT_DISH_SHAPE_STEPS);
    double x = profile.shoulder.length() + (heel.length() * 0.5);
    return new ComponentMass(volume, new Vec3(x, 0.0, stats.centroidZ()));
  }

  private static BladeSample sampleBlade(Blade blade, double s) {
    List<Station> stations = blade.stations();
    if (s <= stations.get(0).s()) {
      Station first = stations.get(0);
      return new BladeSample(first.halfWidth(), first.thickness(), first.dishDepth(), first.crownOffset());
    }
    if (s >= stations.get(stations.size() - 1).s()) {
      Station last = stations.get(stations.size() - 1);
      return new BladeSample(last.halfWidth(), last.thickness(), last.dishDepth(), last.crownOffset());
    }

    Station previous = stations.get(0);
    for (int i = 1; i < stations.size(); i++) {
      Station next = stations.get(i);
      if (s <= next.s()) {
        double range = Math.max(1.0e-9, next.s() - previous.s());
        double t = (s - previous.s()) / range;
        return new BladeSample(
            GeometryUtil.lerp(previous.halfWidth(), next.halfWidth(), t),
            GeometryUtil.lerp(previous.thickness(), next.thickness(), t),
            GeometryUtil.lerp(previous.dishDepth(), next.dishDepth(), t),
            GeometryUtil.lerp(previous.crownOffset(), next.crownOffset(), t));
      }
      previous = next;
    }

    Station last = stations.get(stations.size() - 1);
    return new BladeSample(last.halfWidth(), last.thickness(), last.dishDepth(), last.crownOffset());
  }

  private static DishStats computeDishStats(double halfWidth, double dishDepth, double crownOffset, double exponent,
      int steps) {
    if (halfWidth <= 1.0e-9) {
      return new DishStats(0.0, crownOffset, crownOffset, crownOffset);
    }

    double dy = (halfWidth * 2.0) / steps;
    double arcWidth = 0.0;
    double weightedZ = 0.0;
    double minZ = Double.POSITIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < steps; i++) {
      double y = -halfWidth + ((i + 0.5) * dy);
      double u = Math.abs(y / halfWidth);
      double z = crownOffset + (dishDepth * (1.0 - Math.pow(u, exponent)));
      double slope = exponent <= 1.0 && u == 0.0
          ? 0.0
          : -(dishDepth * exponent * Math.pow(u, Math.max(0.0, exponent - 1.0)) / halfWidth) * Math.signum(y);
      double ds = Math.sqrt(1.0 + (slope * slope)) * dy;
      arcWidth += ds;
      weightedZ += z * ds;
      minZ = Math.min(minZ, z);
      maxZ = Math.max(maxZ, z);
    }

    double centroidZ = arcWidth > 0.0 ? weightedZ / arcWidth : crownOffset;
    return new DishStats(arcWidth, centroidZ, minZ, maxZ);
  }

  private static double shoulderCrown(Shoulder shoulder, double t) {
    return shoulder.crownHeight() * Math.sin(Math.PI * clamp(t, 0.0, 1.0));
  }

  private static double maxBladeHalfWidth(Blade blade) {
    double max = 0.0;
    for (Station station : blade.stations()) {
      max = Math.max(max, station.halfWidth());
    }
    return max;
  }

  private static String defaultContactRegion(String edgeProfile) {
    return "pointed".equals(edgeProfile) ? "corner" : "lip";
  }

  private static void requirePositive(double value, String name) {
    if (value <= 0.0) {
      throw new IllegalArgumentException(name + " must be > 0");
    }
  }

  private static void requirePositive(Double value, String name) {
    if (value == null || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be > 0");
    }
  }

  private static void validateNormalizedRange(double startS, double endS, String name) {
    if (startS < 0.0 || endS > 1.0 || startS >= endS) {
      throw new IllegalArgumentException(name + " must satisfy 0 <= startS < endS <= 1");
    }
  }

  private static void validateContactRegion(String contactRegion) {
    if (!"lip".equals(contactRegion) && !"corner".equals(contactRegion) && !"heel".equals(contactRegion)) {
      throw new IllegalArgumentException("impactOverride.contactRegion must be \"lip\", \"corner\", or \"heel\"");
    }
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}