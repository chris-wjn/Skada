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
public class Blade extends WeaponHead {

  public static final Codec<Blade> CODEC = RecordCodecBuilder.create((RecordCodecBuilder.Instance<Blade> instance) ->
          instance.group(
                  Codec.BOOL.optionalFieldOf("singleEdged", false).forGetter(b -> b.singleEdged),
                  Codec.DOUBLE.fieldOf("widthAtBase").forGetter(b -> b.widthAtBase),
                  Codec.DOUBLE.fieldOf("widthAtTip").forGetter(b -> b.widthAtTip),
                  Codec.unboundedMap(Codec.DOUBLE, Codec.DOUBLE).optionalFieldOf("widthAtPoints", null).forGetter(b -> b.widthAtPoints),
                  Codec.DOUBLE.fieldOf("thicknessAtBase").forGetter(b -> b.thicknessAtBase),
                  Codec.DOUBLE.fieldOf("thicknessAtTip").forGetter(b -> b.thicknessAtTip),
                  Codec.unboundedMap(Codec.DOUBLE, Codec.DOUBLE).optionalFieldOf("thicknessAtPoints", null).forGetter(b -> b.thicknessAtPoints),
                  Codec.DOUBLE.fieldOf("length").forGetter(b -> b.length),
                  Bevel.CODEC.optionalFieldOf("primaryBevel", Bevel.defaultBevel()).forGetter(b -> b.primaryBevel),
                  EdgeBevel.CODEC.optionalFieldOf("edgeBevel", EdgeBevel.noBevel()).forGetter(b -> b.edgeBevel),
                  TipSpecifications.CODEC.optionalFieldOf("tipSpecifications", TipSpecifications.noTip()).forGetter(b -> b.tipSpecifications),
                  Fuller.CODEC.optionalFieldOf("fuller", null).forGetter(b -> b.fuller)
          ).apply(instance, Blade::new)
  );

  @Override
  public Codec<? extends WeaponHead> type() {
    return CODEC;
  }

  @Override
  public String typeKey() {
    return "blade";
  }

  private final boolean singleEdged; // true if the blade is single-edged, false if double-edged
  private final double length; // length of blade in mm
  private final double widthAtBase; // base of blade in mm
  private final double widthAtTip; // width of the blade before the tip bevel, or if there is no tip bevel, this is considered 1mm for the purposes of interpolation
  private final double thicknessAtBase; // thickness of the spine of the blade at base in mm
  private final double thicknessAtTip; // thickness of the spine of the blade at tip in mm, or 1mm if there is no tip bevel
  private final Map<Double, Double> widthAtPoints; //key: distance from base by percentage, value: width at that point in mm
  private final Map<Double, Double> thicknessAtPoints; //key: distance from base by percentage, value: thickness at that point in mm
  private final Bevel primaryBevel; // primary bevel of the blade. Every blade has one.
  private final EdgeBevel edgeBevel; // edge bevel of the blade. Not every blade has one, but this class still needs an EdgeBevel object for edge radius.
  private final TipSpecifications tipSpecifications; // the tip specifications of the blade. Every blade has one.
  private final Fuller fuller; // fuller specifications of the blade. Not every blade has one.
  private final double volume; // volume of the blade in cubic mm
  private final double pointOfBalance; // point of balance from base in mm

  public Blade (boolean singleEdged, double widthAtBase, double widthAtTip, Map<Double, Double> widthAtPoints, double thicknessAtBase, double thicknessAtTip, Map<Double, Double> thicknessAtPoints, double length, Bevel primaryBevel, EdgeBevel edgeBevel, TipSpecifications tipSpecifications, Fuller fuller) {
    this.singleEdged = singleEdged;
    this.length = length;
    this.widthAtBase = widthAtBase;
    this.widthAtTip = widthAtTip;
    this.widthAtPoints = widthAtPoints;
    this.thicknessAtPoints = thicknessAtPoints;
    this.thicknessAtBase = thicknessAtBase;
    this.thicknessAtTip = thicknessAtTip;
    this.edgeBevel = edgeBevel;
    this.tipSpecifications = tipSpecifications;
    this.fuller = fuller;

    // before we calculate volume and PoB, we need to make sure the fuller and the primary bevel check out mathematically.
    // Specifically, if the chord of the circle segment defined by the fuller is longer than the width of non-bevel portion of
    // the blade, we cut off the bevel early to make room.
    if (fuller != null) {
      if (singleEdged) this.primaryBevel = primaryBevel; // single-edged blades don't have to worry about fullers taking up too much space, because the fuller can go on the bevel.
      else {
        double chordPct = fuller.chordLengthByPercent();
        double nonBevelPortion = 1.0 - primaryBevel.percentageOfBladeWidth();
        if (chordPct > nonBevelPortion) {
          // need to adjust the primary bevel percentage to make room for the fuller
          double difference = chordPct - nonBevelPortion; // how much extra space we need
          double newBevelPercentage = nonBevelPortion + difference;
          this.primaryBevel = new Bevel(newBevelPercentage, primaryBevel.curveFactor());
        } else {
          this.primaryBevel = primaryBevel;
        }
      }
    }
    else {
      this.primaryBevel = primaryBevel;
    }

    this.volume = getVolume();
    this.pointOfBalance = getPointOfBalance();
  }

  public double primaryBevelAngle() {
    return -180 + this.edgeBevel.angle() + this.edgeBevel.shoulderAngle();
  }

  public double getWidthAtPercentage(double percentage) {
    return widthAtPoints.getOrDefault(percentage, (widthAtBase+widthAtTip)/2);
  }

  public double getThicknessAtPercentage(double percentage) {
    return thicknessAtPoints.getOrDefault(percentage, (thicknessAtBase+thicknessAtTip)/2);
  }

  public double getVolume() {
    TreeMap<Double, Double> wMap = buildNormalizedMap(widthAtPoints, widthAtBase, widthAtTip);
    TreeMap<Double, Double> tMap = buildNormalizedMap(thicknessAtPoints, thicknessAtBase, thicknessAtTip);

    TreeSet<Double> points = new TreeSet<>();
    points.addAll(wMap.keySet());
    points.addAll(tMap.keySet());

    if (points.isEmpty()) {
      double area = (widthAtBase + widthAtTip) / 2.0 * (thicknessAtBase + thicknessAtTip) / 2.0;
      return area * length;
    }

    double totalVolume = 0.0;
    Double prevP = null;
    double prevW = 0.0;
    double prevT = 0.0;

    double coeff = superEllipseCoefficient(primaryBevel.curveFactor());
    double Kbase = (1.0 - primaryBevel.percentageOfBladeWidth()) + primaryBevel.percentageOfBladeWidth() * coeff; //this is so we don't have to do repeated gamma calculations in the loop

    for (Double p : points) {
      double w = interpolate(wMap, p);
      double t = interpolate(tMap, p);

      if (prevP != null) {
        double delta = p - prevP;
        // exact integral of product of two linears over s in [0,1]:
        // w(s) = w0 + dw*s, t(s) = t0 + dt*s
        double w0 = prevW;
        double w1 = w;
        double t0 = prevT;
        double t1 = t;
        double dw = w1 - w0;
        double dt = t1 - t0;

        // coefficients for w*t = A0 + A1*s + A2*s^2
        double A0 = w0 * t0;
        double A1 = w0 * dt + t0 * dw;
        double A2 = dw * dt;

        double segmentLength = delta * length;

        // volume ignoring fuller: segmentLength * Kbase * integral_{0..1} (A0 + A1*s + A2*s^2) ds
        double integralWT = A0 + 0.5 * A1 + (1.0 / 3.0) * A2;
        double segmentVol = segmentLength * Kbase * integralWT;

        // subtract fuller volume if present
        if (fuller != null && fuller.sagitta() > 0.0 && fuller.chordLengthByPercent() > 0.0) {
          double chordPct = fuller.chordLengthByPercent();
          double sag = fuller.sagitta();
          int sides = fuller.bothSides() ? 2 : 1;

          // Simpson's rule on fuller area (nonlinear in w): sample at s=0,0.5,1
          double wMid = 0.5 * (w0 + w1);
          double f0 = getCircleSegmentArea(chordPct * w0, sag);
          double f1 = getCircleSegmentArea(chordPct * w1, sag);
          double fmid = getCircleSegmentArea(chordPct * wMid, sag);
          double simpson = (f0 + 4.0 * fmid + f1) / 6.0;
          double fullerVol = simpson * segmentLength * sides;
          segmentVol -= fullerVol;
        }

        totalVolume += segmentVol;
      }

      prevP = p;
      prevW = w;
      prevT = t;
    }

    return totalVolume;
  }

  /**
   * Calculates the area of the circle segment defined by a chord of length c and height h.
   * @param c the chord length in mm
   * @param h the sagitta (height) in mm
   * @return the area of the circle segment in mm²
   */
  public static double getCircleSegmentArea(double c, double h) {
    if (c <= 0.0 || h <= 0.0) return 0.0;
    try {
      double area = circleSegmentArea(c, h);
      if (Double.isNaN(area) || area < 0.0) return 0.0;
      return area;
    } catch (Exception ex) {
      return 0.0;
    }
  }


  public static double circleSegmentArea(double c, double h) {
    double cSquared = c * c;
    double hSquared = h * h;
    double firstTerm = Math.pow((cSquared + 4*hSquared)/(8*h), 2);
    double secondTerm = Math.acos( (cSquared - (4*hSquared)) / (cSquared + (4*hSquared)) );
    double thirdTerm = (c/(16*h)) * (cSquared - (4*hSquared));
    return (firstTerm * secondTerm) - thirdTerm;
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

  /**
   * Calculates the superellipse coefficient used in area calculations.
   *
   * @param r the exponent defining the shape of the superellipse
   * @return the superellipse coefficient
   */
  public static double superEllipseCoefficient(double r) {
    // returns (Gamma(1 + 1/r)^2) / Gamma(1 + 2/r)
    double g1 = gamma(1.0 + 1.0 / r);
    double g2 = gamma(1.0 + 2.0 / r);
    return (g1 * g1) / g2;
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
    double prevW = 0.0;
    double prevT = 0.0;
    double firstMoment = 0.0;

    double coeff = superEllipseCoefficient(primaryBevel.curveFactor());
    double Kbase = (1.0 - primaryBevel.percentageOfBladeWidth()) + primaryBevel.percentageOfBladeWidth() * coeff;

    for (Double p : points) {
      double w = interpolate(wMap, p);
      double t = interpolate(tMap, p);

      if (prevP != null) {
        double delta = p - prevP;
        double w0 = prevW;
        double w1 = w;
        double t0 = prevT;
        double t1 = t;
        double dw = w1 - w0;
        double dt = t1 - t0;

        double A0 = w0 * t0;
        double A1 = w0 * dt + t0 * dw;
        double A2 = dw * dt;

        double p0 = prevP;
        double D = delta;
        double L = length;

        // firstMomentSegment = L^2 * Kbase * D * [ p0*(A0 + 1/2 A1 + 1/3 A2) + D*(1/2 A0 + 1/3 A1 + 1/4 A2) ]
        double term1 = A0 + 0.5 * A1 + (1.0 / 3.0) * A2;
        double term2 = 0.5 * A0 + (1.0 / 3.0) * A1 + (1.0 / 4.0) * A2;
        double segmentFirstMoment = (L * L) * Kbase * D * (p0 * term1 + D * term2);

        // subtract fuller first moment if present (approximate with Simpson weighting of x*f(w))
        if (fuller != null && fuller.sagitta() > 0.0 && fuller.chordLengthByPercent() > 0.0) {
          double chordPct = fuller.chordLengthByPercent();
          double sag = fuller.sagitta();
          int sides = fuller.bothSides() ? 2 : 1;

          double wMid = 0.5 * (w0 + w1);
          // positions p at s=0,0.5,1
          double pA = p0;
          double pB = p0 + D * 0.5;
          double pC = p0 + D;

          double f0 = getCircleSegmentArea(chordPct * w0, sag);
          double fmid = getCircleSegmentArea(chordPct * wMid, sag);
          double f1 = getCircleSegmentArea(chordPct * w1, sag);

          // Simpson for integral of p * f(w(p)) over s; convert to percent-space then multiply by L^2 * D * sides
          double simpsonPf = (pA * f0 + 4.0 * pB * fmid + pC * f1) / 6.0;
          double fullerFirstMoment = (L * L) * D * simpsonPf * sides;
          segmentFirstMoment -= fullerFirstMoment;
        }

        firstMoment += segmentFirstMoment;
      }

      prevP = p;
      prevW = w;
      prevT = t;
    }

    return firstMoment / vol;
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

    public static Bevel defaultBevel() {
      return new Bevel(0.2, 1.0);
    }

  }

  /**
   * Bevel specifications for the edge bevel of the blade.
   * The edge radius is measured in micrometers because it will be very small (1000 micrometers = 1 millimetre).
   * If the shoulder angle is 180 degrees, there is no shoulder and the edge bevel angle will be taken to
   * be the angle of the primary bevel.
   *
   * @param angle angle of the bevel in degrees
   * @param shoulderAngle angle of the shoulder where the bevel meets the primary bevel in degrees
   * @param edgeRadius radius of the edge in micrometers (1000 micrometers = 1 millimetre)
   */
  public record EdgeBevel(
          double angle,
          double shoulderAngle,
          double edgeRadius
  ) {

    public static final Codec<EdgeBevel> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.DOUBLE.fieldOf("angle").forGetter(EdgeBevel::angle),
                    Codec.DOUBLE.fieldOf("shoulderAngle").forGetter(EdgeBevel::shoulderAngle),
                    Codec.DOUBLE.fieldOf("edgeRadius").forGetter(EdgeBevel::edgeRadius)
            ).apply(instance, EdgeBevel::new)
    );

    public static EdgeBevel noBevel() {
      return new EdgeBevel(22.5, 180.0, 5);
    }

  }

  /**
   * Fuller specifications for the blade. A fuller is a groove or channel
   * that runs along the length of the blade to reduce weight while maintaining strength.
   * We treat the fuller as a circular segment cut out of the blade, defined by its chord length
   * and sagitta (height). We treat the superimposition as the chord being flush with the surface of the blade,
   * and the sagitta being the depth of the fuller into the blade. If bothSides is true,
   * the fuller is mirrored on both sides of the blade.
   *
   * @param bothSides true if the fuller is on both sides of the blade
   * @param chordLengthByPercent the chord length of the fuller as a percentage of the blade width (0.0 - 1.0)
   * @param sagitta the height (depth) of the fuller in mm
   */
  public record Fuller(
          boolean bothSides,
          double chordLengthByPercent,
          double sagitta
  ) {
    public static final Codec<Fuller> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.BOOL.fieldOf("bothSides").forGetter(Fuller::bothSides),
                    Codec.DOUBLE.fieldOf("chordLengthPercentage").forGetter(Fuller::chordLengthByPercent),
                    Codec.DOUBLE.fieldOf("height").forGetter(Fuller::sagitta)
            ).apply(instance, Fuller::new)
    );
  }

}
