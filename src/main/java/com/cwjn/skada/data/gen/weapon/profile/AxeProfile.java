package com.cwjn.skada.data.gen.weapon.profile;

import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Bounds;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec2;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

import java.util.Objects;
import java.util.Optional;

/**
 * Geometry and runtime physics model for axe head profiles.
 *
 * Units: centimeters for geometry, grams for mass, g/cm^3 for density.
 */
public final class AxeProfile {

  private static final int CURRENT_VERSION = 1;
  private static final double BORE_CLAMP_EPSILON = 1.0e-3;

  public enum LobeKind {
    CORE,
    TOP,
    BOTTOM
  }

  public static final Codec<AxeProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("version").forGetter(AxeProfile::getVersion),
      AxeEye.CODEC.fieldOf("eye").forGetter(AxeProfile::eye),
      AxeEdge.CODEC.fieldOf("edge").forGetter(AxeProfile::edge)).apply(instance, AxeProfile::new));

  private final int version;
  private final AxeEye eye;
  private final AxeEdge edge;

  public AxeProfile(int version, AxeEye eye, AxeEdge edge) {
    this.version = version;
    this.eye = Objects.requireNonNull(eye, "eye");
    this.edge = Objects.requireNonNull(edge, "edge");

    if (version != CURRENT_VERSION) {
      throw new IllegalArgumentException("version must be " + CURRENT_VERSION);
    }
  }

  public record AxeCrossSection(double length, double thickness, double curve) {
    public static final Codec<AxeCrossSection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("length").forGetter(AxeCrossSection::length),
        Codec.DOUBLE.fieldOf("thickness").forGetter(AxeCrossSection::thickness),
        Codec.DOUBLE.fieldOf("curve").forGetter(AxeCrossSection::curve)).apply(instance, AxeCrossSection::new));

    public AxeCrossSection {
      if (length <= 0.0) {
        throw new IllegalArgumentException("crossSection.length must be > 0");
      }
      if (thickness <= 0.0) {
        throw new IllegalArgumentException("crossSection.thickness must be > 0");
      }
      if (curve <= 0.0) {
        throw new IllegalArgumentException("crossSection.curve must be > 0");
      }
    }
  }

  public record AxeEyeBore(double width, double thickness, String shape) {
    public static final Codec<AxeEyeBore> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("width").forGetter(AxeEyeBore::width),
        Codec.DOUBLE.fieldOf("thickness").forGetter(AxeEyeBore::thickness),
        Codec.STRING.optionalFieldOf("shape", "rect").forGetter(AxeEyeBore::shape)).apply(instance, AxeEyeBore::new));

    public AxeEyeBore {
      if (width < 0.0) {
        throw new IllegalArgumentException("bore.width must be >= 0");
      }
      if (thickness < 0.0) {
        throw new IllegalArgumentException("bore.thickness must be >= 0");
      }
      if (!"rect".equals(shape) && !"circle".equals(shape)) {
        throw new IllegalArgumentException("bore.shape must be \"rect\" or \"circle\"");
      }
    }
  }

  public record AxeEye(double length, double height, double thickness, double xOffset, AxeEyeBore bore) {
    public static final Codec<AxeEye> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("length").forGetter(AxeEye::length),
        Codec.DOUBLE.fieldOf("height").forGetter(AxeEye::height),
        Codec.DOUBLE.fieldOf("thickness").forGetter(AxeEye::thickness),
        Codec.DOUBLE.optionalFieldOf("xOffset", 0.0).forGetter(AxeEye::xOffset),
        AxeEyeBore.CODEC.optionalFieldOf("bore").forGetter(eye -> Optional.ofNullable(eye.bore())))
        .apply(instance, (length, height, thickness, xOffset, bore) -> new AxeEye(length, height, thickness, xOffset,
            bore.orElse(null))));

    public AxeEye {
      if (length <= 0.0 || height <= 0.0 || thickness <= 0.0) {
        throw new IllegalArgumentException("eye dimensions must be > 0");
      }
    }
  }

  public record AxeLobe(double height, Double tipX, AxeCrossSection crossSection) {
    public static final Codec<AxeLobe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("height").forGetter(AxeLobe::height),
        Codec.DOUBLE.optionalFieldOf("tipX").forGetter(lobe -> Optional.ofNullable(lobe.tipX())),
        AxeCrossSection.CODEC.fieldOf("crossSection").forGetter(AxeLobe::crossSection))
        .apply(instance, (height, tipX, crossSection) -> new AxeLobe(height, tipX.orElse(null), crossSection)));

    public AxeLobe {
      if (height <= 0.0) {
        throw new IllegalArgumentException("lobe.height must be > 0");
      }
      Objects.requireNonNull(crossSection, "crossSection");
    }
  }

  public record AxeEdge(double length, AxeLobe core, AxeLobe top, AxeLobe bottom) {
    public static final Codec<AxeEdge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("length").forGetter(AxeEdge::length),
        AxeLobe.CODEC.fieldOf("core").forGetter(AxeEdge::core),
        AxeLobe.CODEC.fieldOf("top").forGetter(AxeEdge::top),
        AxeLobe.CODEC.fieldOf("bottom").forGetter(AxeEdge::bottom)).apply(instance, AxeEdge::new));

    public AxeEdge {
      if (length <= 0.0) {
        throw new IllegalArgumentException("edge.length must be > 0");
      }
      Objects.requireNonNull(core, "core");
      Objects.requireNonNull(top, "top");
      Objects.requireNonNull(bottom, "bottom");
    }
  }

  private record LobeMass(double volume, Vec3 center) {
  }

  public int getVersion() {
    return version;
  }

  public AxeEye eye() {
    return eye;
  }

  public AxeEdge edge() {
    return edge;
  }

  public MassProperties computeMassProperties(double densityGPerCm3, int samples) {
    return computeMassProperties(this, densityGPerCm3, samples);
  }

  public static MassProperties computeMassProperties(AxeProfile profile, double densityGPerCm3, int samples) {
    Objects.requireNonNull(profile, "profile");
    if (densityGPerCm3 <= 0.0) {
      throw new IllegalArgumentException("density must be > 0");
    }

    int steps = Math.max(4, samples);
    AxeEye eye = profile.eye;
    AxeEdge edge = profile.edge;
    double edgeStart = edgeStart(profile);

    AxeEyeBore bore = getClampedEyeBore(eye);
    double eyeVolume = eye.length() * eye.height() * eye.thickness();
    double boreVolume = bore != null ? boreArea(bore) * eye.height() : 0.0;
    double volume = Math.max(eyeVolume - boreVolume, 0.0);

    Vec3 weighted = new Vec3(eyeCenterX(eye) * eyeVolume, 0.0, 0.0);

    LobeMass coreMass = computeLobeMass(edge, edge.core(), steps, LobeKind.CORE, edge.core().height(), edgeStart);
    LobeMass topMass = computeLobeMass(edge, edge.top(), steps, LobeKind.TOP, edge.core().height(), edgeStart);
    LobeMass bottomMass = computeLobeMass(edge, edge.bottom(), steps, LobeKind.BOTTOM, edge.core().height(), edgeStart);

    weighted = weighted.add(coreMass.center().mul(coreMass.volume()));
    weighted = weighted.add(topMass.center().mul(topMass.volume()));
    weighted = weighted.add(bottomMass.center().mul(bottomMass.volume()));
    volume += coreMass.volume() + topMass.volume() + bottomMass.volume();

    Vec3 centerOfMass = volume > 0.0 ? weighted.mul(1.0 / volume) : new Vec3(0.0, 0.0, 0.0);
    double mass = volume * densityGPerCm3;
    return new MassProperties(volume, mass, centerOfMass);
  }

  public static Bounds localBounds(AxeProfile profile, int samples) {
    Objects.requireNonNull(profile, "profile");

    AxeEye eye = profile.eye;
    AxeEdge edge = profile.edge;

    double edgeStart = edgeStart(profile);
    double minX = eyeMinX(eye);
    double maxX = edgeStart + edge.length();

    double coreHalf = edge.core().height() / 2.0;
    double minY = -Math.max(eye.height() / 2.0, coreHalf + edge.bottom().height());
    double maxY = Math.max(eye.height() / 2.0, coreHalf + edge.top().height());

    double maxThickness = Math.max(
        eye.thickness(),
        Math.max(
            edge.core().crossSection().thickness(),
            Math.max(edge.top().crossSection().thickness(), edge.bottom().crossSection().thickness())));
    double halfThickness = maxThickness / 2.0;

    return new Bounds(minX, maxX, minY, maxY, -halfThickness, halfThickness);
  }

  public static Vec2 sectionRange(AxeProfile profile) {
    Objects.requireNonNull(profile, "profile");

    AxeEye eye = profile.eye;
    AxeEdge edge = profile.edge;

    double coreHalf = edge.core().height() / 2.0;
    double maxTop = coreHalf + edge.top().height();
    double maxBottom = coreHalf + edge.bottom().height();
    double minY = -Math.max(eye.height() / 2.0, maxBottom);
    double maxY = Math.max(eye.height() / 2.0, maxTop);
    return new Vec2(minY, maxY);
  }

  public static double edgeStart(AxeProfile profile) {
    Objects.requireNonNull(profile, "profile");
    return eyeMaxX(profile.eye);
  }

  public static double eyeCenterX(AxeEye eye) {
    Objects.requireNonNull(eye, "eye");
    return eye.xOffset();
  }

  public static double eyeMinX(AxeEye eye) {
    Objects.requireNonNull(eye, "eye");
    return eyeCenterX(eye) - (eye.length() / 2.0);
  }

  public static double eyeMaxX(AxeEye eye) {
    Objects.requireNonNull(eye, "eye");
    return eyeCenterX(eye) + (eye.length() / 2.0);
  }

  public static Vec2 materialHalfThicknessAt(AxeProfile profile, double x, double ySlice) {
    Objects.requireNonNull(profile, "profile");

    AxeEye eye = profile.eye;
    AxeEdge edge = profile.edge;
    double edgeStart = edgeStart(profile);
    double coreHalf = edge.core().height() / 2.0;
    double topLimit = coreHalf + edge.top().height();
    double bottomLimit = -coreHalf - edge.bottom().height();

    double zHalf = 0.0;
    double curve = edge.core().crossSection().curve();

    if (x >= eyeMinX(eye) && x <= eyeMaxX(eye) && ySlice >= -eye.height() / 2.0 && ySlice <= eye.height() / 2.0) {
      zHalf = Math.max(zHalf, eye.thickness() / 2.0);
    }

    if (x >= edgeStart && x <= edgeStart + edge.length()) {
      double xLocal = x - edgeStart;

      if (Math.abs(ySlice) <= coreHalf) {
        double coreHalfThickness = halfThicknessAt(edge.core().crossSection(), xLocal, edge.length());
        if (coreHalfThickness > zHalf) {
          zHalf = coreHalfThickness;
          curve = edge.core().crossSection().curve();
        }
      }

      if (ySlice >= coreHalf && ySlice <= topLimit) {
        double heightAtX = lobeHeight(edge, edge.top(), xLocal, LobeKind.TOP, edgeStart);
        double dy = ySlice - coreHalf;
        double zBase = halfThicknessAt(edge.top().crossSection(), xLocal, edge.length());
        if (heightAtX > 0.0 && dy >= 0.0 && dy <= heightAtX && zBase > 0.0) {
          double zScale = sliceScale(dy, heightAtX, edge.top().crossSection().curve());
          double topHalf = zBase * zScale;
          if (topHalf > zHalf) {
            zHalf = topHalf;
            curve = edge.top().crossSection().curve();
          }
        }
      }

      if (ySlice <= -coreHalf && ySlice >= bottomLimit) {
        double heightAtX = lobeHeight(edge, edge.bottom(), xLocal, LobeKind.BOTTOM, edgeStart);
        double dy = -coreHalf - ySlice;
        double zBase = halfThicknessAt(edge.bottom().crossSection(), xLocal, edge.length());
        if (heightAtX > 0.0 && dy >= 0.0 && dy <= heightAtX && zBase > 0.0) {
          double zScale = sliceScale(dy, heightAtX, edge.bottom().crossSection().curve());
          double bottomHalf = zBase * zScale;
          if (bottomHalf > zHalf) {
            zHalf = bottomHalf;
            curve = edge.bottom().crossSection().curve();
          }
        }
      }
    }

    return new Vec2(zHalf, curve);
  }

  public static double halfThicknessAt(AxeCrossSection section, double xLocal, double edgeLength) {
    Objects.requireNonNull(section, "section");
    if (edgeLength <= 0.0 || section.length() <= 0.0) {
      return 0.0;
    }

    double a = section.length();
    double b = section.thickness() / 2.0;
    double r = Math.max(1.0e-6, section.curve());
    double x = Mth.clamp(xLocal, 0.0, a);
    return Math.max(0.0, b * (1.0 - Math.pow(x / a, r)));
  }

  public static double lobeHeight(AxeEdge edge, AxeLobe lobe, double xLocal, LobeKind kind, double edgeStart) {
    Objects.requireNonNull(edge, "edge");
    Objects.requireNonNull(lobe, "lobe");
    Objects.requireNonNull(kind, "kind");

    if (lobe.height() <= 0.0) {
      return 0.0;
    }
    if (kind == LobeKind.CORE) {
      return lobe.height();
    }

    double x = edgeStart + xLocal;
    double baseStart = edgeStart;
    double baseEnd = edgeStart + edge.length();
    double tipX = lobe.tipX() != null ? lobe.tipX() : baseStart;

    if (x < baseStart || x > baseEnd) {
      return 0.0;
    }

    if (tipX <= baseStart) {
      double span = baseEnd - baseStart;
      return span <= 0.0 ? 0.0 : lobe.height() * (1.0 - ((x - baseStart) / span));
    }

    if (tipX >= baseEnd) {
      double span = baseEnd - baseStart;
      return span <= 0.0 ? 0.0 : lobe.height() * ((x - baseStart) / span);
    }

    if (x <= tipX) {
      double leftSpan = tipX - baseStart;
      return leftSpan <= 0.0 ? 0.0 : lobe.height() * ((x - baseStart) / leftSpan);
    }

    double rightSpan = baseEnd - tipX;
    return rightSpan <= 0.0 ? 0.0 : lobe.height() * ((baseEnd - x) / rightSpan);
  }

  public static double lobeCenterY(LobeKind kind, double coreHeight, double height) {
    return switch (kind) {
      case CORE -> 0.0;
      case TOP -> (coreHeight / 2.0) + (height / 2.0);
      case BOTTOM -> (-coreHeight / 2.0) - (height / 2.0);
    };
  }

  public static double sliceScale(double dy, double heightAtX, double curve) {
    if (heightAtX <= 0.0) {
      return 0.0;
    }
    double t = Mth.clamp(1.0 - (dy / heightAtX), 0.0, 1.0);
    double r = Math.max(1.0e-6, curve);
    return Math.pow(t, 1.0 / r);
  }

  public static AxeEyeBore getClampedEyeBore(AxeEye eye) {
    Objects.requireNonNull(eye, "eye");
    AxeEyeBore bore = eye.bore();
    if (bore == null) {
      return null;
    }

    double maxHalfWidth = Math.min(eyeMaxX(eye), -eyeMinX(eye));
    double maxWidth = Math.max((2.0 * maxHalfWidth) - BORE_CLAMP_EPSILON, 0.0);
    double maxThickness = Math.max(eye.thickness() - BORE_CLAMP_EPSILON, 0.0);
    double width = Mth.clamp(bore.width(), 0.0, maxWidth);
    double thickness = Mth.clamp(bore.thickness(), 0.0, maxThickness);

    if (width <= 0.0 || thickness <= 0.0) {
      return null;
    }

    String shape = "circle".equals(bore.shape()) ? "circle" : "rect";
    if ("circle".equals(shape)) {
      double maxDiameter = Math.min(maxWidth, maxThickness);
      double diameter = Mth.clamp(Math.min(width, thickness), 0.0, maxDiameter);
      if (diameter <= 0.0) {
        return null;
      }
      return new AxeEyeBore(diameter, diameter, shape);
    }

    return new AxeEyeBore(width, thickness, shape);
  }

  public static double boreArea(AxeEyeBore bore) {
    Objects.requireNonNull(bore, "bore");
    if ("circle".equals(bore.shape())) {
      double radius = bore.width() / 2.0;
      return Math.PI * radius * radius;
    }
    return bore.width() * bore.thickness();
  }

  private static LobeMass computeLobeMass(
      AxeEdge edge,
      AxeLobe lobe,
      int samples,
      LobeKind kind,
      double coreHeight,
      double edgeStart) {
    double length = edge.length();
    double dx = length / samples;
    double volume = 0.0;
    Vec3 weighted = new Vec3(0.0, 0.0, 0.0);

    for (int i = 0; i < samples; i++) {
      double xLocal = (i + 0.5) * dx;
      double height = lobeHeight(edge, lobe, xLocal, kind, edgeStart);
      if (height <= 0.0) {
        continue;
      }

      double zHalf = halfThicknessAt(lobe.crossSection(), xLocal, length);
      if (zHalf <= 0.0) {
        continue;
      }

      double area = height * 2.0 * zHalf;
      double sliceVolume = area * dx;
      double yMid = lobeCenterY(kind, coreHeight, height);
      double x = edgeStart + xLocal;

      volume += sliceVolume;
      weighted = weighted.add(new Vec3(x * sliceVolume, yMid * sliceVolume, 0.0));
    }

    Vec3 center = volume > 0.0 ? weighted.mul(1.0 / volume) : new Vec3(0.0, 0.0, 0.0);
    return new LobeMass(volume, center);
  }

}
