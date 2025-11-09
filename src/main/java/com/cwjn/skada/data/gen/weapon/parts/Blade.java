package com.cwjn.skada.data.gen.weapon.parts;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A class that represents a blade, like on a sword or knife.
 */
public class Blade {

  private final double length; // length of blade in mm
  private final double widthAtBase; // base of blade in mm
  private final double widthAtTip; // width of the blade before the tip bevel, or if there is no tip bevel, this is considered 1mm for the purposes of interpolation
  private final double thicknessAtBase; // thickness of the spine of the blade at base in mm
  private final double thicknessAtTip; // thickness of the spine of the blade at tip in mm, or 1mm if there is no tip bevel
  private final Map<Double, Double> widthAtPoints; //key: distance from base by percentage, value: width at that point in mm
  private final Map<Double, Double> thicknessAtPoints; //key: distance from base by percentage, value: thickness at that point in mm
  private final Bevel primaryBevel; // primary bevel of the blade. Every blade has one.
  private final EdgeBevel edgeBevel; // edge bevel of the blade. Not every blade has one, in which case we set the shoulder angle to 180, and then it is ignored.
  private final TipSpecifications tipSpecifications; // the tip specifications of the blade. Every blade has one.
  private final Fuller fuller; // fuller specifications of the blade. Not every blade has one.
  private final double volume; // volume of the blade in cubic mm
  private final double pointOfBalance; // point of balance from base in mm

  public Blade (double widthAtBase, double widthAtTip, Map<Double, Double> widthAtPoints, double thicknessAtBase, double thicknessAtTip, Map<Double, Double> thicknessAtPoints, double length, Bevel primaryBevel, EdgeBevel edgeBevel, TipSpecifications tipSpecifications, Fuller fuller) {
    this.length = length;
    this.widthAtBase = widthAtBase;
    this.widthAtTip = widthAtTip;
    this.widthAtPoints = widthAtPoints;
    this.thicknessAtPoints = thicknessAtPoints;
    this.thicknessAtBase = thicknessAtBase;
    this.thicknessAtTip = thicknessAtTip;
    this.primaryBevel = primaryBevel;
    this.edgeBevel = edgeBevel;
    this.tipSpecifications = tipSpecifications;
    this.fuller = fuller;
    this.volume = getVolume();
    this.pointOfBalance = getPointOfBalance();
  }

  public double getWidthAtPercentage(double percentage) {
    return widthAtPoints.getOrDefault(percentage, (widthAtBase+widthAtTip)/2);
  }

  public double getThicknessAtPercentage(double percentage) {
    return thicknessAtPoints.getOrDefault(percentage, (thicknessAtBase+thicknessAtTip)/2);
  }

  public double getVolume() {
    if (volume > 0) return volume;
    // build normalized, sorted maps including the endpoints at 0.0 and 1.0
    TreeMap<Double, Double> wMap = buildNormalizedMap(widthAtPoints, widthAtBase, widthAtTip);
    TreeMap<Double, Double> tMap = buildNormalizedMap(thicknessAtPoints, thicknessAtBase, thicknessAtTip);

    // union of sample points
    TreeSet<Double> points = new TreeSet<>();
    points.addAll(wMap.keySet());
    points.addAll(tMap.keySet());

    if (points.isEmpty()) {
      // fallback: simple prism
      double area = (widthAtBase + widthAtTip) / 2.0 * (thicknessAtBase + thicknessAtTip) / 2.0;
      return area * length;
    }

    double volume = 0.0;
    Double prevP = null;
    double prevArea = 0.0;

    for (Double p : points) {
      double w = interpolate(wMap, p);
      double t = interpolate(tMap, p);
      double wBevel = w * primaryBevel.percentageOfBladeWidth();
      double wSpine = w * (1.0 - primaryBevel.percentageOfBladeWidth());
      double areaSpine = t * wSpine; // rectangular spine area
      double areaBevel = getSuperEllipseArea(wBevel*0.5, t*0.5, primaryBevel.curveFactor());
      double area = areaBevel + areaSpine; // cross-sectional area at this percentage

      if (prevP != null) {
        double delta = p - prevP;
        double segmentLength = delta * length;
        // trapezoidal rule on the cross-sectional area
        volume += 0.5 * (prevArea + area) * segmentLength;
      }

      prevP = p;
      prevArea = area;
    }

    return volume;
  }

  /**
   * Calculates the area of an ellipse given its semi-major axis (a) and semi-minor axis (b).
   * @param a the half-diameter of the long part of the ellipse
   * @param b the half-diameter of the short part of the ellipse
   * @return the area of the ellipse in mm^2
   */
  public static double getEllipseArea(double a, double b) {
    if (a <= 0 || b <= 0) throw new IllegalArgumentException("axes must be > 0");
    return Math.PI * a * b;
  }

  /**
   * Calculates the area of a superellipse given its semi-major axis (a), semi-minor axis (b), and exponent (r).
   * r > 0. For r = 2, this is a normal ellipse. For r < 2, the shape is more "pointed", and for r > 2, the shape is more "squared".
   *
   * @param a the half-diameter of the long part of the superellipse, in mm
   * @param b the half-diameter of the short part of the superellipse, in mm
   * @param r the exponent defining the shape of the superellipse
   * @return the area of the superellipse in mm^2
   */
  public static double getSuperEllipseArea(double a, double b, double r) {
    if (a <= 0.0 || b <= 0.0) throw new IllegalArgumentException("a and b must be > 0");
    if (r <= 0.0) throw new IllegalArgumentException("r must be > 0");
    double g1 = gamma(1.0 + 1.0 / r);
    double g2 = gamma(1.0 + 2.0 / r);
    return 4.0 * a * b * (g1 * g1) / g2;
  }

  // Lanczos approximation for Gamma(z)
  private static double gamma(double z) {
    double[] p = {
            676.5203681218851,
            -1259.1392167224028,
            771.32342877765313,
            -176.61502916214059,
            12.507343278686905,
            -0.13857109526572012,
            9.9843695780195716e-6,
            1.5056327351493116e-7
    };
    double g = 7.0;
    if (z < 0.5) {
      return Math.PI / (Math.sin(Math.PI * z) * gamma(1.0 - z));
    } else {
      z -= 1.0;
      double x = 0.99999999999980993;
      for (int i = 0; i < p.length; i++) {
        x += p[i] / (z + i + 1.0);
      }
      double t = z + g + 0.5;
      return Math.sqrt(2.0 * Math.PI) * Math.pow(t, z + 0.5) * Math.exp(-t) * x;
    }
  }

  private static TreeMap<Double, Double> buildNormalizedMap(Map<Double, Double> src, double startValue, double endValue) {
    TreeMap<Double, Double> map = new TreeMap<>();
    map.put(0.0, startValue);
    map.put(1.0, endValue);
    if (src != null) {
      for (Map.Entry<Double, Double> e : src.entrySet()) {
        double key = normalizePercentKey(e.getKey());
        map.put(key, e.getValue());
      }
    }
    return map;
  }

  private static double normalizePercentKey(double key) {
    // accept either 0..1 or 0..100 input
    if (key > 1.5) {
      return key / 100.0;
    }
    return key;
  }

  private static double interpolate(NavigableMap<Double, Double> map, double p) {
    if (map.containsKey(p)) return map.get(p);
    Map.Entry<Double, Double> floor = map.floorEntry(p);
    Map.Entry<Double, Double> ceil = map.ceilingEntry(p);

    if (floor == null && ceil == null) return 0.0;
    if (floor == null) return ceil.getValue();
    if (ceil == null) return floor.getValue();

    double x0 = floor.getKey();
    double x1 = ceil.getKey();
    double y0 = floor.getValue();
    double y1 = ceil.getValue();

    if (Double.compare(x1, x0) == 0) return y0;
    double t = (p - x0) / (x1 - x0);
    return y0 + t * (y1 - y0);
  }

  public double getPointOfBalance() {
    if (pointOfBalance > 0) return pointOfBalance;
    double vol = getVolume();
    if (vol <= 0.0) return length / 2.0;

    TreeMap<Double, Double> wMap = buildNormalizedMap(widthAtPoints, widthAtBase, widthAtTip);
    TreeMap<Double, Double> tMap = buildNormalizedMap(thicknessAtPoints, thicknessAtBase, thicknessAtTip);

    TreeSet<Double> points = new TreeSet<>();
    points.addAll(wMap.keySet());
    points.addAll(tMap.keySet());

    if (points.isEmpty()) return length / 2.0;

    Double prevP = null;
    double prevArea = 0.0;
    double firstMoment = 0.0;

    for (Double p : points) {
      double w = interpolate(wMap, p);
      double t = interpolate(tMap, p);
      double area = w * t; // cross-sectional area at this percentage

      if (prevP != null) {
        double x0 = prevP * length;
        double x1 = p * length;
        double f0 = x0 * prevArea; // x * A(x) at start
        double f1 = x1 * area;     // x * A(x) at end
        double segmentLength = x1 - x0;
        // trapezoidal rule on f(x) = x * A(x)
        firstMoment += 0.5 * (f0 + f1) * segmentLength;
      }

      prevP = p;
      prevArea = area;
    }

    return firstMoment / vol;
  }

  public enum BevelType {
    CONVEX,
    CONCAVE,
    FLAT
  }

  /**
   * Specifications for the tip of the blade. If the blade doesn't have a distinct tip,
   * like for example if the blade is squared off on the end, the tip radius should be set
   * to 0, and the bevel angles should be set to 180 degrees.
   * @param tipRadius the radius of the tip in micrometers (1000 micrometers = 1 millimetre)
   * @param tipBevelAngle the angle of the bevel at the tip in degrees
   * @param tipBevelShoulderAngle the angle of the shoulder where the tip bevel meets the rest of the blade in degrees
   */
  public record TipSpecifications(
          double tipRadius,
          double tipBevelAngle,
          double tipBevelShoulderAngle
  ) {
    public static final Codec<TipSpecifications> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.DOUBLE.fieldOf("tipRadius").forGetter(TipSpecifications::tipRadius),
                    Codec.DOUBLE.fieldOf("tipBevelAngle").forGetter(TipSpecifications::tipBevelAngle),
                    Codec.DOUBLE.fieldOf("tipBevelShoulderAngle").forGetter(TipSpecifications::tipBevelShoulderAngle)
            ).apply(instance, TipSpecifications::new)
    );

    public static TipSpecifications noTip() {
      return new TipSpecifications(10000, 90, 180);
    }

  }

  /**
   * Bevel specifications for the primary bevel of the blade.
   * The length of the bevel is measured as a percentage of the blade width.
   * @param percentageOfBladeWidth
   * @param curveFactor the concavity or convexity of the bevel. 0 < curveFactor < 2. 1 = diamond, 2/3 = astroid, 2 = circle.
   */
  public record Bevel(
          double percentageOfBladeWidth,
          double curveFactor
  ) {

    public static final Codec<Bevel> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.DOUBLE.fieldOf("percentageOfBladeWidth").forGetter(Bevel::percentageOfBladeWidth),
                    Codec.DOUBLE.fieldOf("curveFactor").forGetter(Bevel::curveFactor)
            ).apply(instance, Bevel::new)
    );

  }

  /**
   * Bevel specifications for the edge bevel of the blade.
   * The edge radius is measured in micrometers because it will be very small (1000 micrometers = 1 millimetre).
   * If the shoulder angle is 180 degrees, there is no shoulder and the primary bevel will be ignored.
   *
   * @param angle angle of the bevel in degrees
   * @param shoulderAngle angle of the shoulder where the bevel meets the primary bevel in degrees
   * @param bevelType type of bevel (convex, concave, flat)
   * @param edgeRadius radius of the edge in micrometers (1000 micrometers = 1 millimetre)
   */
  public record EdgeBevel(
          double angle,
          double shoulderAngle,
          BevelType bevelType,
          double edgeRadius
  ) {

    public static final Codec<EdgeBevel> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.DOUBLE.fieldOf("angle").forGetter(EdgeBevel::angle),
                    Codec.DOUBLE.fieldOf("shoulderAngle").forGetter(EdgeBevel::shoulderAngle),
                    Codec.STRING.fieldOf("bevelType").forGetter(bevel -> bevel.bevelType().name()),
                    Codec.DOUBLE.fieldOf("edgeRadius").forGetter(EdgeBevel::edgeRadius)
            ).apply(instance, (bevelAngle, shoulderAngle, type, edgeRadius) ->
                    new EdgeBevel(bevelAngle, shoulderAngle, getBevelType(type), edgeRadius))
    );

    static BevelType getBevelType(String bevel) {
      return BevelType.valueOf(bevel);
    }

    public static EdgeBevel noEdge() {
      return new EdgeBevel(180.0, 180.0, BevelType.FLAT, 1000.0);
    }

  }

  /**
   * Fuller specifications for the blade. A fuller is a groove or channel
   * that runs along the length of the blade to reduce weight while maintaining strength.
   * We'll consider fullers as half cylinder segments subtracted from the blade volume.
   *
   * @param radiusAtBase
   * @param radiusAtTip
   * @param depthAtBase
   * @param depthAtTip
   * @param length
   */
  public record Fuller(
          double radiusAtBase,
          double radiusAtTip,
          double depthAtBase,
          double depthAtTip,
          double length
  ) {
    public static final Codec<Fuller> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.DOUBLE.fieldOf("widthAtBase").forGetter(Fuller::radiusAtBase),
                    Codec.DOUBLE.fieldOf("widthAtTip").forGetter(Fuller::radiusAtTip),
                    Codec.DOUBLE.fieldOf("depthAtBase").forGetter(Fuller::depthAtBase),
                    Codec.DOUBLE.fieldOf("depthAtTip").forGetter(Fuller::depthAtTip),
                    Codec.DOUBLE.fieldOf("length").forGetter(Fuller::length)
            ).apply(instance, Fuller::new)
    );
  }


}
