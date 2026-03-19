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
 * Geometry and runtime physics model for mace head profiles.
 *
 * Units: centimeters for geometry, grams for mass, g/cm^3 for density.
 */
public final class MaceHeadProfile {

  private static final int CURRENT_VERSION = 1;
  private static final double TIP_RADIUS_NM_TO_CM = 1.0e-7;
  private static final double MIN_CONTACT_AREA_CM2 = 0.01;

  public static final Codec<MaceHeadProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("version").forGetter(MaceHeadProfile::getVersion),
      Mount.CODEC.fieldOf("mount").forGetter(MaceHeadProfile::getMount),
      Core.CODEC.fieldOf("core").forGetter(MaceHeadProfile::getCore),
      Flanges.CODEC.optionalFieldOf("flanges").forGetter(profile -> Optional.ofNullable(profile.getFlanges())),
      KnobRing.CODEC.listOf().optionalFieldOf("knobRings", List.of()).forGetter(MaceHeadProfile::getKnobRings),
      SpikeRing.CODEC.listOf().optionalFieldOf("spikeRings", List.of()).forGetter(MaceHeadProfile::getSpikeRings),
      ImpactOverride.CODEC.optionalFieldOf("impactOverride")
          .forGetter(profile -> Optional.ofNullable(profile.getImpactOverride())))
      .apply(instance,
          (version, mount, core, flanges, knobRings, spikeRings, impactOverride) -> new MaceHeadProfile(
              version,
              mount,
              core,
              flanges.orElse(null),
              knobRings,
              spikeRings,
              impactOverride.orElse(null))));

  private final int version;
  private final Mount mount;
  private final Core core;
  private final Flanges flanges;
  private final List<KnobRing> knobRings;
  private final List<SpikeRing> spikeRings;
  private final ImpactOverride impactOverride;

  private record ComponentMass(double volumeCm3, Vec3 center) {
    private static final ComponentMass ZERO = new ComponentMass(0.0, new Vec3(0.0, 0.0, 0.0));
  }

  public record Mount(
      String type,
      double length,
      double outerRadius,
      double innerRadius,
      double baseRadius,
      double tipRadius,
      double transitionLength) {

    public static final Codec<Mount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("type").forGetter(Mount::type),
        Codec.DOUBLE.fieldOf("length").forGetter(Mount::length),
        Codec.DOUBLE.optionalFieldOf("outerRadius", 0.0).forGetter(Mount::outerRadius),
        Codec.DOUBLE.optionalFieldOf("innerRadius", 0.0).forGetter(Mount::innerRadius),
        Codec.DOUBLE.optionalFieldOf("baseRadius", 0.0).forGetter(Mount::baseRadius),
        Codec.DOUBLE.optionalFieldOf("tipRadius", 0.0).forGetter(Mount::tipRadius),
        Codec.DOUBLE.optionalFieldOf("transitionLength", 0.0).forGetter(Mount::transitionLength))
        .apply(instance, Mount::new));

    public Mount {
      Objects.requireNonNull(type, "type");
      if (!"socket".equals(type) && !"tang".equals(type)) {
        throw new IllegalArgumentException("mount.type must be \"socket\" or \"tang\"");
      }
      if (length <= 0.0) {
        throw new IllegalArgumentException("mount.length must be > 0");
      }
      if (transitionLength < 0.0 || transitionLength > length) {
        throw new IllegalArgumentException("mount.transitionLength must be in [0,length]");
      }
      if ("socket".equals(type)) {
        if (outerRadius <= 0.0) {
          throw new IllegalArgumentException("socket mount requires outerRadius > 0");
        }
        if (innerRadius < 0.0 || innerRadius >= outerRadius) {
          throw new IllegalArgumentException("socket innerRadius must be >= 0 and smaller than outerRadius");
        }
      }
      if ("tang".equals(type)) {
        if (baseRadius <= 0.0 || tipRadius <= 0.0) {
          throw new IllegalArgumentException("tang mount requires baseRadius and tipRadius > 0");
        }
      }
    }
  }

  public record CoreStation(double s, double radius) {
    public static final Codec<CoreStation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("s").forGetter(CoreStation::s),
        Codec.DOUBLE.fieldOf("radius").forGetter(CoreStation::radius))
        .apply(instance, CoreStation::new));

    public CoreStation {
      if (s < 0.0 || s > 1.0) {
        throw new IllegalArgumentException("core station s must be in [0,1]");
      }
      if (radius <= 0.0) {
        throw new IllegalArgumentException("core station radius must be > 0");
      }
    }
  }

  public record Core(double length, List<CoreStation> stations) {
    public static final Codec<Core> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("length").forGetter(Core::length),
        CoreStation.CODEC.listOf().fieldOf("stations").forGetter(Core::stations))
        .apply(instance, Core::new));

    public Core {
      if (length <= 0.0) {
        throw new IllegalArgumentException("core.length must be > 0");
      }
      stations = List.copyOf(Objects.requireNonNull(stations, "stations"));
      if (stations.size() < 2) {
        throw new IllegalArgumentException("core.stations must contain at least 2 entries");
      }
      double previousS = -1.0;
      for (CoreStation station : stations) {
        if (station.s() < previousS) {
          throw new IllegalArgumentException("core station s values must be monotonic increasing");
        }
        previousS = station.s();
      }
    }
  }

  public record Flanges(
      int count,
      double startS,
      double endS,
      double protrusion,
      double rootWidth,
      double tipWidth,
      double curve,
      double twistDegrees) {

    public static final Codec<Flanges> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("count").forGetter(Flanges::count),
        Codec.DOUBLE.fieldOf("startS").forGetter(Flanges::startS),
        Codec.DOUBLE.fieldOf("endS").forGetter(Flanges::endS),
        Codec.DOUBLE.fieldOf("protrusion").forGetter(Flanges::protrusion),
        Codec.DOUBLE.fieldOf("rootWidth").forGetter(Flanges::rootWidth),
        Codec.DOUBLE.fieldOf("tipWidth").forGetter(Flanges::tipWidth),
        Codec.DOUBLE.fieldOf("curve").forGetter(Flanges::curve),
        Codec.DOUBLE.optionalFieldOf("twistDegrees", 0.0).forGetter(Flanges::twistDegrees))
        .apply(instance, Flanges::new));

    public Flanges {
      if (count <= 0) {
        throw new IllegalArgumentException("flanges.count must be > 0");
      }
      if (startS < 0.0 || endS > 1.0 || startS >= endS) {
        throw new IllegalArgumentException("flanges must satisfy 0 <= startS < endS <= 1");
      }
      if (protrusion <= 0.0) {
        throw new IllegalArgumentException("flanges.protrusion must be > 0");
      }
      if (rootWidth <= 0.0 || tipWidth <= 0.0) {
        throw new IllegalArgumentException("flange widths must be > 0");
      }
      if (tipWidth > rootWidth) {
        throw new IllegalArgumentException("flanges.tipWidth must not exceed rootWidth");
      }
      if (curve <= 0.0) {
        throw new IllegalArgumentException("flanges.curve must be > 0");
      }
    }
  }

  public record KnobRing(double positionS, int count, double radius, double projection) {
    public static final Codec<KnobRing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("positionS").forGetter(KnobRing::positionS),
        Codec.INT.fieldOf("count").forGetter(KnobRing::count),
        Codec.DOUBLE.fieldOf("radius").forGetter(KnobRing::radius),
        Codec.DOUBLE.fieldOf("projection").forGetter(KnobRing::projection))
        .apply(instance, KnobRing::new));

    public KnobRing {
      if (positionS < 0.0 || positionS > 1.0) {
        throw new IllegalArgumentException("knobRing.positionS must be in [0,1]");
      }
      if (count <= 0) {
        throw new IllegalArgumentException("knobRing.count must be > 0");
      }
      if (radius <= 0.0 || projection <= 0.0) {
        throw new IllegalArgumentException("knobRing radius and projection must be > 0");
      }
    }
  }

  public record SpikeRing(
      double positionS,
      int count,
      double length,
      double baseRadius,
      double pointTaper,
      double tipRadiusNm) {

    public static final Codec<SpikeRing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("positionS").forGetter(SpikeRing::positionS),
        Codec.INT.fieldOf("count").forGetter(SpikeRing::count),
        Codec.DOUBLE.fieldOf("length").forGetter(SpikeRing::length),
        Codec.DOUBLE.fieldOf("baseRadius").forGetter(SpikeRing::baseRadius),
        Codec.DOUBLE.fieldOf("pointTaper").forGetter(SpikeRing::pointTaper),
        Codec.DOUBLE.fieldOf("tipRadiusNm").forGetter(SpikeRing::tipRadiusNm))
        .apply(instance, SpikeRing::new));

    public SpikeRing {
      if (positionS < 0.0 || positionS > 1.0) {
        throw new IllegalArgumentException("spikeRing.positionS must be in [0,1]");
      }
      if (count <= 0) {
        throw new IllegalArgumentException("spikeRing.count must be > 0");
      }
      if (length <= 0.0 || baseRadius <= 0.0) {
        throw new IllegalArgumentException("spikeRing length and baseRadius must be > 0");
      }
      if (pointTaper < 0.0 || pointTaper > 1.0) {
        throw new IllegalArgumentException("spikeRing.pointTaper must be in [0,1]");
      }
      if (tipRadiusNm <= 0.0) {
        throw new IllegalArgumentException("spikeRing.tipRadiusNm must be > 0");
      }
    }
  }

  public record ImpactOverride(double effectiveContactAreaCm2, double focusFactor, double rigidity) {
    public static final Codec<ImpactOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("effectiveContactAreaCm2").forGetter(ImpactOverride::effectiveContactAreaCm2),
        Codec.DOUBLE.fieldOf("focusFactor").forGetter(ImpactOverride::focusFactor),
        Codec.DOUBLE.fieldOf("rigidity").forGetter(ImpactOverride::rigidity))
        .apply(instance, ImpactOverride::new));

    public ImpactOverride {
      if (effectiveContactAreaCm2 <= 0.0) {
        throw new IllegalArgumentException("impactOverride.effectiveContactAreaCm2 must be > 0");
      }
      if (focusFactor <= 0.0 || rigidity <= 0.0) {
        throw new IllegalArgumentException("impactOverride focusFactor and rigidity must be > 0");
      }
    }
  }

  public record DerivedStrikeGeometry(
      double contactPointX,
      double effectiveContactAreaCm2,
      double focusFactor,
      double rigidity,
      String featureType) {
  }

  public MaceHeadProfile(
      int version,
      Mount mount,
      Core core,
      Flanges flanges,
      List<KnobRing> knobRings,
      List<SpikeRing> spikeRings,
      ImpactOverride impactOverride) {
    this.version = version;
    this.mount = Objects.requireNonNull(mount, "mount");
    this.core = Objects.requireNonNull(core, "core");
    this.flanges = flanges;
    this.knobRings = List.copyOf(Objects.requireNonNull(knobRings, "knobRings"));
    this.spikeRings = List.copyOf(Objects.requireNonNull(spikeRings, "spikeRings"));
    this.impactOverride = impactOverride;

    if (version != CURRENT_VERSION) {
      throw new IllegalArgumentException("version must be " + CURRENT_VERSION);
    }
  }

  public static MassProperties computeMassProperties(MaceHeadProfile profile, double densityGPerCm3, int samples) {
    Objects.requireNonNull(profile, "profile");
    if (densityGPerCm3 <= 0.0) {
      throw new IllegalArgumentException("density must be > 0");
    }

    int axialSamples = Math.max(24, samples);
    ComponentMass mountMass = computeMountMass(profile, axialSamples);
    ComponentMass coreMass = computeCoreMass(profile, axialSamples);
    ComponentMass flangeMass = computeFlangeMass(profile);
    ComponentMass knobMass = computeKnobMass(profile);
    ComponentMass spikeMass = computeSpikeMass(profile, Math.max(16, samples / 2));

    double totalVolume = mountMass.volumeCm3() + coreMass.volumeCm3() + flangeMass.volumeCm3() + knobMass.volumeCm3()
        + spikeMass.volumeCm3();
    Vec3 weighted = mountMass.center().mul(mountMass.volumeCm3())
        .add(coreMass.center().mul(coreMass.volumeCm3()))
        .add(flangeMass.center().mul(flangeMass.volumeCm3()))
        .add(knobMass.center().mul(knobMass.volumeCm3()))
        .add(spikeMass.center().mul(spikeMass.volumeCm3()));

    Vec3 center = totalVolume > 0.0 ? weighted.mul(1.0 / totalVolume) : new Vec3(0.0, 0.0, 0.0);
    return new MassProperties(totalVolume, totalVolume * densityGPerCm3, center);
  }

  public MassProperties computeMassProperties(double densityGPerCm3, int samples) {
    return computeMassProperties(this, densityGPerCm3, samples);
  }

  public static Bounds localBounds(MaceHeadProfile profile, int samples) {
    Objects.requireNonNull(profile, "profile");

    double minX = 0.0;
    double maxX = profile.totalLength();
    double maxRadial = Math.max(profile.mountMaxRadius(samples), profile.maxCoreRadius());

    if (profile.flanges != null) {
      maxRadial = Math.max(maxRadial,
          maxCoreRadius(profile.core, profile.flanges.startS(), profile.flanges.endS()) + profile.flanges.protrusion());
    }

    for (KnobRing ring : profile.knobRings) {
      double centerX = profile.coreStartX() + (ring.positionS() * profile.core.length());
      double radial = profile.coreRadiusAt(ring.positionS()) + ring.projection() + ring.radius();
      minX = Math.min(minX, centerX - ring.radius());
      maxX = Math.max(maxX, centerX + ring.radius());
      maxRadial = Math.max(maxRadial, radial);
    }

    for (SpikeRing ring : profile.spikeRings) {
      double centerX = profile.coreStartX() + (ring.positionS() * profile.core.length());
      double radial = profile.coreRadiusAt(ring.positionS()) + ring.length() + Math.max(ring.baseRadius(), tipRadiusCm(ring));
      minX = Math.min(minX, centerX - ring.baseRadius());
      maxX = Math.max(maxX, centerX + ring.baseRadius());
      maxRadial = Math.max(maxRadial, radial);
    }

    return new Bounds(minX, maxX, -maxRadial, maxRadial, -maxRadial, maxRadial);
  }

  public Bounds localBounds(int samples) {
    return localBounds(this, samples);
  }

  public DerivedStrikeGeometry deriveStrikeGeometry() {
    DerivedStrikeGeometry derived = spikeRings.isEmpty()
        ? (flanges != null ? deriveFlangeStrikeGeometry() : (knobRings.isEmpty() ? deriveCrownStrikeGeometry() : deriveKnobStrikeGeometry()))
        : deriveSpikeStrikeGeometry();

    if (impactOverride == null) {
      return derived;
    }

    return new DerivedStrikeGeometry(
        derived.contactPointX(),
        impactOverride.effectiveContactAreaCm2(),
        impactOverride.focusFactor(),
        impactOverride.rigidity(),
        derived.featureType());
  }

  public boolean hasSpikeRings() {
    return !spikeRings.isEmpty();
  }

  public double coreStartX() {
    return mount.length();
  }

  public double totalLength() {
    return mount.length() + core.length();
  }

  public double coreRadiusAt(double s) {
    return coreRadiusAt(core, s);
  }

  public int getVersion() {
    return version;
  }

  public Mount getMount() {
    return mount;
  }

  public Core getCore() {
    return core;
  }

  public Flanges getFlanges() {
    return flanges;
  }

  public List<KnobRing> getKnobRings() {
    return knobRings;
  }

  public List<SpikeRing> getSpikeRings() {
    return spikeRings;
  }

  public ImpactOverride getImpactOverride() {
    return impactOverride;
  }

  private DerivedStrikeGeometry deriveCrownStrikeGeometry() {
    double endRadius = coreRadiusAt(1.0);
    double taperRate = distalCoreTaperRate();
    double contactRadius = Math.max(0.12, endRadius * (0.24 - (0.05 * clamp(taperRate, 0.0, 1.0))));
    double area = Math.max(MIN_CONTACT_AREA_CM2, Math.PI * contactRadius * contactRadius);
    double focus = clamp(1.0 + (0.22 * taperRate), 1.0, 1.28);
    double rigidity = clamp(1.0 + (0.08 * endRadius / Math.max(0.1, maxCoreRadius())), 1.0, 1.12);
    return new DerivedStrikeGeometry(totalLength(), area, focus, rigidity, "crown");
  }

  private DerivedStrikeGeometry deriveFlangeStrikeGeometry() {
    double contactX = coreStartX() + (flanges.endS() * core.length());
    double contactArea = Math.max(MIN_CONTACT_AREA_CM2,
        flanges.tipWidth() * Math.max(0.05, Math.min(0.3, flanges.protrusion() * 0.22)));
    double focus = clamp(1.12 + (0.42 * flanges.protrusion() / Math.max(0.1, flanges.rootWidth())), 1.12, 1.72);
    double rigidity = clamp(1.02 + (0.12 * flanges.protrusion() / Math.max(0.1, coreRadiusAt(flanges.endS()))), 1.02, 1.18);
    return new DerivedStrikeGeometry(contactX, contactArea, focus, rigidity, "flange");
  }

  private DerivedStrikeGeometry deriveKnobStrikeGeometry() {
    KnobRing distalRing = knobRings.stream().max((left, right) -> Double.compare(left.positionS(), right.positionS()))
        .orElseThrow();
    double contactX = coreStartX() + (distalRing.positionS() * core.length());
    double contactArea = Math.max(MIN_CONTACT_AREA_CM2,
        Math.PI * Math.pow(Math.max(0.08, distalRing.radius() * 0.3), 2.0));
    double focus = clamp(1.04 + (0.18 * distalRing.projection() / distalRing.radius()), 1.04, 1.34);
    double rigidity = clamp(1.0 + (0.08 * distalRing.radius() / Math.max(0.1, coreRadiusAt(distalRing.positionS()))), 1.0, 1.14);
    return new DerivedStrikeGeometry(contactX, contactArea, focus, rigidity, "knob");
  }

  private DerivedStrikeGeometry deriveSpikeStrikeGeometry() {
    SpikeRing distalRing = spikeRings.stream().max((left, right) -> Double.compare(left.positionS(), right.positionS()))
        .orElseThrow();
    double contactX = coreStartX() + (distalRing.positionS() * core.length());
    double tipRadiusCm = tipRadiusCm(distalRing);
    double derivedTipRadius = Math.max(0.015, Math.max(tipRadiusCm, distalRing.baseRadius() * (0.08 - (0.04 * distalRing.pointTaper()))));
    double contactArea = Math.max(MIN_CONTACT_AREA_CM2, Math.PI * derivedTipRadius * derivedTipRadius);
    double focus = clamp(1.35 + (0.55 * distalRing.pointTaper()), 1.35, 1.9);
    double rigidity = clamp(1.08 + (0.12 * distalRing.baseRadius() / Math.max(0.1, coreRadiusAt(distalRing.positionS()))), 1.08, 1.2);
    return new DerivedStrikeGeometry(contactX, contactArea, focus, rigidity, "spike");
  }

  private double distalCoreTaperRate() {
    List<CoreStation> stations = core.stations();
    CoreStation last = stations.get(stations.size() - 1);
    CoreStation previous = stations.get(stations.size() - 2);
    double ds = Math.max(1.0e-6, last.s() - previous.s());
    double dx = Math.max(1.0e-6, ds * core.length());
    return clamp(Math.abs(previous.radius() - last.radius()) / dx, 0.0, 1.0);
  }

  private double mountMaxRadius(int samples) {
    int steps = Math.max(8, samples / 4);
    double maxRadius = 0.0;
    for (int i = 0; i <= steps; i++) {
      double x = mount.length() * i / steps;
      maxRadius = Math.max(maxRadius, mountOuterRadiusAt(this, x));
    }
    return maxRadius;
  }

  private double maxCoreRadius() {
    double maxRadius = 0.0;
    for (CoreStation station : core.stations()) {
      maxRadius = Math.max(maxRadius, station.radius());
    }
    return maxRadius;
  }

  private static ComponentMass computeMountMass(MaceHeadProfile profile, int samples) {
    Mount mount = profile.mount;
    double dx = mount.length() / samples;
    double totalVolume = 0.0;
    double weightedX = 0.0;

    for (int i = 0; i < samples; i++) {
      double x = (i + 0.5) * dx;
      double outerRadius = mountOuterRadiusAt(profile, x);
      double innerRadius = mountInnerRadiusAt(profile, x);
      double area = Math.PI * Math.max(0.0, (outerRadius * outerRadius) - (innerRadius * innerRadius));
      double sliceVolume = area * dx;
      totalVolume += sliceVolume;
      weightedX += x * sliceVolume;
    }

    if (totalVolume <= 0.0) {
      return ComponentMass.ZERO;
    }
    return new ComponentMass(totalVolume, new Vec3(weightedX / totalVolume, 0.0, 0.0));
  }

  private static ComponentMass computeCoreMass(MaceHeadProfile profile, int samples) {
    double dx = profile.core.length() / samples;
    double totalVolume = 0.0;
    double weightedX = 0.0;

    for (int i = 0; i < samples; i++) {
      double s = (i + 0.5) / samples;
      double radius = profile.coreRadiusAt(s);
      double x = profile.coreStartX() + (s * profile.core.length());
      double sliceVolume = Math.PI * radius * radius * dx;
      totalVolume += sliceVolume;
      weightedX += x * sliceVolume;
    }

    if (totalVolume <= 0.0) {
      return ComponentMass.ZERO;
    }
    return new ComponentMass(totalVolume, new Vec3(weightedX / totalVolume, 0.0, 0.0));
  }

  private static ComponentMass computeFlangeMass(MaceHeadProfile profile) {
    Flanges flanges = profile.flanges;
    if (flanges == null) {
      return ComponentMass.ZERO;
    }

    double shoulderWidth = Math.max(0.0, flanges.rootWidth() - flanges.tipWidth());
    double shoulderArea = flanges.protrusion() * shoulderWidth * (flanges.curve() / (flanges.curve() + 1.0));
    double crestArea = flanges.protrusion() * flanges.tipWidth();
    double areaPerFlange = shoulderArea + crestArea;
    double length = profile.core.length() * (flanges.endS() - flanges.startS());
    double totalVolume = areaPerFlange * length * flanges.count();
    double centerX = profile.coreStartX() + (profile.core.length() * (flanges.startS() + flanges.endS()) * 0.5);
    return new ComponentMass(totalVolume, new Vec3(centerX, 0.0, 0.0));
  }

  private static ComponentMass computeKnobMass(MaceHeadProfile profile) {
    double totalVolume = 0.0;
    double weightedX = 0.0;

    for (KnobRing ring : profile.knobRings) {
      double sphereVolume = (4.0 / 3.0) * Math.PI * Math.pow(ring.radius(), 3.0);
      double ringVolume = sphereVolume * ring.count();
      double x = profile.coreStartX() + (ring.positionS() * profile.core.length());
      totalVolume += ringVolume;
      weightedX += x * ringVolume;
    }

    if (totalVolume <= 0.0) {
      return ComponentMass.ZERO;
    }
    return new ComponentMass(totalVolume, new Vec3(weightedX / totalVolume, 0.0, 0.0));
  }

  private static ComponentMass computeSpikeMass(MaceHeadProfile profile, int samples) {
    double totalVolume = 0.0;
    double weightedX = 0.0;

    for (SpikeRing ring : profile.spikeRings) {
      double perSpikeVolume = 0.0;
      double ds = 1.0 / samples;
      for (int i = 0; i < samples; i++) {
        double t = (i + 0.5) * ds;
        double radius = spikeRadiusAt(ring, t);
        perSpikeVolume += Math.PI * radius * radius * ring.length() * ds;
      }
      double ringVolume = perSpikeVolume * ring.count();
      double x = profile.coreStartX() + (ring.positionS() * profile.core.length());
      totalVolume += ringVolume;
      weightedX += x * ringVolume;
    }

    if (totalVolume <= 0.0) {
      return ComponentMass.ZERO;
    }
    return new ComponentMass(totalVolume, new Vec3(weightedX / totalVolume, 0.0, 0.0));
  }

  private static double mountOuterRadiusAt(MaceHeadProfile profile, double x) {
    Mount mount = profile.mount;
    double clampedX = clamp(x, 0.0, mount.length());
    double coreBaseRadius = profile.coreRadiusAt(0.0);

    if ("socket".equals(mount.type())) {
      if (mount.transitionLength() <= 0.0) {
        return mount.outerRadius();
      }
      double transitionStart = mount.length() - mount.transitionLength();
      if (clampedX <= transitionStart) {
        return mount.outerRadius();
      }
      double t = (clampedX - transitionStart) / Math.max(1.0e-6, mount.transitionLength());
      return GeometryUtil.lerp(mount.outerRadius(), coreBaseRadius, t);
    }

    double taperedRadius = GeometryUtil.lerp(mount.baseRadius(), mount.tipRadius(), clampedX / mount.length());
    if (mount.transitionLength() <= 0.0) {
      return taperedRadius;
    }
    double transitionStart = mount.length() - mount.transitionLength();
    if (clampedX <= transitionStart) {
      return taperedRadius;
    }
    double t = (clampedX - transitionStart) / Math.max(1.0e-6, mount.transitionLength());
    return GeometryUtil.lerp(taperedRadius, coreBaseRadius, t);
  }

  private static double mountInnerRadiusAt(MaceHeadProfile profile, double x) {
    Mount mount = profile.mount;
    if (!"socket".equals(mount.type())) {
      return 0.0;
    }
    if (mount.transitionLength() <= 0.0) {
      return mount.innerRadius();
    }
    double clampedX = clamp(x, 0.0, mount.length());
    double transitionStart = mount.length() - mount.transitionLength();
    if (clampedX <= transitionStart) {
      return mount.innerRadius();
    }
    double t = (clampedX - transitionStart) / Math.max(1.0e-6, mount.transitionLength());
    return GeometryUtil.lerp(mount.innerRadius(), 0.0, t);
  }

  private static double coreRadiusAt(Core core, double s) {
    double clampedS = clamp(s, 0.0, 1.0);
    List<CoreStation> stations = core.stations();
    if (clampedS <= stations.get(0).s()) {
      return stations.get(0).radius();
    }

    for (int i = 1; i < stations.size(); i++) {
      CoreStation previous = stations.get(i - 1);
      CoreStation current = stations.get(i);
      if (clampedS <= current.s()) {
        double range = Math.max(1.0e-6, current.s() - previous.s());
        double t = (clampedS - previous.s()) / range;
        return GeometryUtil.lerp(previous.radius(), current.radius(), t);
      }
    }

    return stations.get(stations.size() - 1).radius();
  }

  private static double maxCoreRadius(Core core, double startS, double endS) {
    double maxRadius = Math.max(coreRadiusAt(core, startS), coreRadiusAt(core, endS));
    for (CoreStation station : core.stations()) {
      if (station.s() >= startS && station.s() <= endS) {
        maxRadius = Math.max(maxRadius, station.radius());
      }
    }
    return maxRadius;
  }

  private static double spikeRadiusAt(SpikeRing ring, double t) {
    double clampedT = clamp(t, 0.0, 1.0);
    double tipRadiusCm = tipRadiusCm(ring);
    double exponent = 1.0 + (2.0 * ring.pointTaper());
    return tipRadiusCm + ((ring.baseRadius() - tipRadiusCm) * Math.pow(1.0 - clampedT, exponent));
  }

  private static double tipRadiusCm(SpikeRing ring) {
    return ring.tipRadiusNm() * TIP_RADIUS_NM_TO_CM;
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}