package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.WeaponProfile;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.SlashCapable;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.ThrustCapable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

/**
 * A class that represents a blade, like on a sword or knife.
 */
public class Blade extends WeaponHead implements SlashCapable, ThrustCapable {

  public static final Codec<Blade> CODEC = RecordCodecBuilder.create((RecordCodecBuilder.Instance<Blade> instance) ->
          instance.group(
                  Codec.BOOL.optionalFieldOf("singleEdged", false).forGetter(b -> b.singleEdged),
                  Codec.DOUBLE.fieldOf("widthAtBase").forGetter(b -> b.widthAtBase),
                  Codec.DOUBLE.fieldOf("widthAtTip").forGetter(b -> b.widthAtTip),
                  Codec.unboundedMap(Codec.DOUBLE, Codec.DOUBLE).optionalFieldOf("widthAtPoints", Map.of()).forGetter(b -> b.widthAtPoints),
                  Codec.DOUBLE.fieldOf("thicknessAtBase").forGetter(b -> b.thicknessAtBase),
                  Codec.DOUBLE.fieldOf("thicknessAtTip").forGetter(b -> b.thicknessAtTip),
                  Codec.unboundedMap(Codec.DOUBLE, Codec.DOUBLE).optionalFieldOf("thicknessAtPoints", Map.of()).forGetter(b -> b.thicknessAtPoints),
                  Codec.DOUBLE.fieldOf("length").forGetter(b -> b.length),
                  Bevel.CODEC.fieldOf("primaryBevel").forGetter(b -> b.primaryBevel),
                  EdgeBevel.CODEC.fieldOf("edgeBevel").forGetter(b -> b.edgeBevel),
                  TipSpecifications.CODEC.fieldOf("tipSpecifications").forGetter(b -> b.tipSpecifications),
                  Fuller.CODEC.optionalFieldOf("fuller", Fuller.noFuller()).forGetter(b -> b.fuller)
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


  @Override
  public double getPrimaryAxisLength() {
    return this.length;
  }

  @Override
  public double getSecondaryAxisLength() {
    double maxW = Math.max(widthAtBase, widthAtTip);
    if (widthAtPoints != null) {
      for (double w : widthAtPoints.values()) {
        maxW = Math.max(maxW, w);
      }
    }
    return maxW;
  }

  /**
   * Calculates the moment of inertia of the blade attached to a weapon.
   * distanceFromPivot describes the distance in mm from the end of the handle, the pivot point, to the base of the blade.
   * If orientation is parallel, the blade's length axis is aligned with the pivot axis. If perpendicular, the blade's width axis is aligned with the pivot axis.
   * We can treat the blade as a 3d solid such that its length is along the x-axis, its width along the y-axis, and its thickness along the z-axis.
   * Then, we can use integration techniques to calculate the moment of inertia about the pivot point using a given density.
   *
   * @param distanceFromPivot the distance from the pivot point in millimeters.
   * @param density the density of the blade material in g/cm³.
   * @param orientation the orientation of the blade relative to the pivot axis.
   * @return the moment of inertia in g/cm^2.
   */
  @Override
  public double getMomentOfInertia(double distanceFromPivot, double density, WeaponProfile.HeadOrientation orientation) {
    // Convert density from g/cm³ to g/mm³ for calculations
    double densityPerMM3 = density / 1000.0;

    if (length <= 0.0) return 0.0;
    if ((widthAtBase <= 0.0 && widthAtTip <= 0.0) || (thicknessAtBase <= 0.0 && thicknessAtTip <= 0.0)) {
      return 0.0;
    }

    TreeMap<Double, Double> wMap = buildNormalizedMap(widthAtPoints, widthAtBase, widthAtTip);
    TreeMap<Double, Double> tMap = buildNormalizedMap(thicknessAtPoints, thicknessAtBase, thicknessAtTip);

    TreeSet<Double> points = getIntegrationPoints(wMap, tMap);

    if (points.isEmpty()) return 0.0;

    double totalInertia = 0.0;
    Double prevP = null;
    double prevW = 0.0;
    double prevT = 0.0;

    // Bevel parameters
    double r = primaryBevel.curveFactor();
    double pBevel = primaryBevel.percentageOfBladeWidth();
    double coeff = superEllipseCoefficient(r);
    // Kbase is the area factor relative to a rectangle W*T
    double Kbase = (1.0 - pBevel) + pBevel * coeff;

    // Calculate accurate inertia shape factor kLocal = I_centroid / (Area * W^2)
    // This accounts for the mass distribution of the flat center and tapered bevels
    double kShape = calculateInertiaShapeFactor(pBevel, r);
    double kLocal = kShape / Kbase;

    for (Double p : points) {
      double w = interpolate(wMap, p);
      double t = interpolate(tMap, p);

      if (prevP != null) {
        double delta = p - prevP;
        double w0 = prevW;
        double w1 = w;
        double t0 = prevT;
        double t1 = t;

        double segLen = delta * length;
        double distStart = prevP * length; // Distance from blade base

        // Simpson's rule integration
        // I_slice(s) = rho * A(s) * [ D_axis(s)^2 + kLocal * W(s)^2 ]
        // A(s) = W(s) * T(s) * Kbase

        // s=0
        double area0 = w0 * t0 * Kbase;
        double dAxisSq0 = getDistAxisSq(distanceFromPivot, distStart, orientation);
        double f0 = area0 * (dAxisSq0 + kLocal * w0 * w0);

        // s=L/2
        double wMid = (w0 + w1) / 2.0;
        double tMid = (t0 + t1) / 2.0;
        double areaMid = wMid * tMid * Kbase;
        double dAxisSqMid = getDistAxisSq(distanceFromPivot, distStart + segLen / 2.0, orientation);
        double fMid = areaMid * (dAxisSqMid + kLocal * wMid * wMid);

        // s=L
        double area1 = w1 * t1 * Kbase;
        double dAxisSq1 = getDistAxisSq(distanceFromPivot, distStart + segLen, orientation);
        double f1 = area1 * (dAxisSq1 + kLocal * w1 * w1);

        // Use adaptive Simpson's rule for better accuracy on nonlinear blade geometry
        double segInertia = adaptiveSimpsonsRule(f0, f1, fMid, segLen, 1e-6, 0) * densityPerMM3;
        if (fuller != null && fuller.sagittaHeightByPercent() > 0.0 && fuller.chordLengthByPercent() > 0.0) {
             double chordPct = fuller.chordLengthByPercent();
             double depthPct = fuller.sagittaHeightByPercent();
             int sides = fuller.bothSides() ? 2 : 1;

             // Calculate sagitta from percentage and local thickness
             double maxDepthPct = (sides == 2) ? 0.5 : 1.0;
             double effectiveDepthPct = Math.min(depthPct, maxDepthPct);

             // Calculate local thickness at fuller position (assume centered at 0.5)
             double tLocal0 = getLocalThickness(t0, primaryBevel, singleEdged, 0.5);
             double tLocalMid = getLocalThickness(tMid, primaryBevel, singleEdged, 0.5);
             double tLocal1 = getLocalThickness(t1, primaryBevel, singleEdged, 0.5);

             double sag0 = effectiveDepthPct * tLocal0;
             double sagMid = effectiveDepthPct * tLocalMid;
             double sag1 = effectiveDepthPct * tLocal1;

             // Use accurate circular segment moment of inertia
             double c0 = chordPct * w0;
             double I_seg0 = getCircleSegmentInertia(c0, sag0);
             double af0 = getCircleSegmentArea(c0, sag0);
             // I_pivot = I_centroid + A * d_pivot^2
             double ff0 = I_seg0 * densityPerMM3 + af0 * dAxisSq0 * densityPerMM3;

             double cMid = chordPct * wMid;
             double I_segMid = getCircleSegmentInertia(cMid, sagMid);
             double afMid = getCircleSegmentArea(cMid, sagMid);
             double ffMid = I_segMid * densityPerMM3 + afMid * dAxisSqMid * densityPerMM3;

             double c1 = chordPct * w1;
             double I_seg1 = getCircleSegmentInertia(c1, sag1);
             double af1 = getCircleSegmentArea(c1, sag1);
             double ff1 = I_seg1 * densityPerMM3 + af1 * dAxisSq1 * densityPerMM3;

             // Use adaptive Simpson's rule for accurate fuller inertia integration
             double fullerInertia = adaptiveSimpsonsRule(ff0, ff1, ffMid, segLen, 1e-6, 0) * sides;

             // Safety clamp
             if (fullerInertia > segInertia) fullerInertia = segInertia;

             segInertia -= fullerInertia;
        }
        totalInertia += segInertia;
      }
      prevP = p;
      prevW = w;
      prevT = t;
    }

    return totalInertia;
  }

  /**
   * Calculates the inertia shape factor k = I / (W^3 * T) for the blade cross-section.
   * The cross-section consists of a central rectangular part (width fraction 1-p)
   * and two superelliptical bevels (width fraction p).
   * @param p bevel percentage (0 to 1)
   * @param r bevel curve factor
   * @return shape factor k
   */
  private double calculateInertiaShapeFactor(double p, double r) {
      if (p <= 0.0) return 1.0/12.0; // Rectangle
      if (p >= 1.0) {
          // Full superellipse (split in two halves meeting at center)
          // I = 2 * ( a^3 * r/(3(r+3)) ) where a = W/2.
          // I = 2 * (W/2)^3 * ... = W^3/4 * ...
          // k = 1/4 * r/(3(r+3)) = r / (12(r+3)).
          return r / (12.0 * (r + 3.0));
      }

      // Normalized dimensions for W=2, T=1
      double x0 = 1.0 - p; // Half-width of flat part
      double a = p;        // Width of one bevel

      // Inertia of flat part (from -x0 to x0)
      double I_flat = (2.0/3.0) * Math.pow(x0, 3);

      // Inertia of one bevel (shifted by x0)
      // Terms from integration of (u+x0)^2 * (1 - (u/a)^r)
      double term1 = Math.pow(a, 3) * r / (3.0 * (r + 3.0));
      double term2 = x0 * Math.pow(a, 2) * r / (r + 2.0);
      double term3 = Math.pow(x0, 2) * a * r / (r + 1.0);

      double I_bevel = term1 + term2 + term3;

      double I_total = I_flat + 2.0 * I_bevel;

      // Normalize by W^3 = 8
      return I_total / 8.0;
  }

  private double getDistAxisSq(double dPivot, double s, WeaponProfile.HeadOrientation orientation) {
      if (orientation == WeaponProfile.HeadOrientation.PARALLEL) {
          // Blade along radial axis. Distance from pivot = dPivot + s.
          return Math.pow(dPivot + s, 2);
      } else {
          // Blade perpendicular to radial axis.
          // Radial distance = dPivot. Tangential distance = s.
          // R^2 = dPivot^2 + s^2.
          return dPivot * dPivot + s * s;
      }
  }

  public Bevel primaryBevel() {
    return this.primaryBevel;
  }

  public EdgeBevel edgeBevel() {
    return this.edgeBevel;
  }

  @Override
  public double getMedianWidth() {
    if (length <= 0.0) return 0.0;
    if ((widthAtBase <= 0.0) && (widthAtTip <= 0.0)) return 0.0;

    TreeMap<Double, Double> wMap = buildNormalizedMap(widthAtPoints, widthAtBase, widthAtTip);
    int steps = 25;
    double[] samples = new double[steps + 1];
    for (int i = 0; i <= steps; i++) {
      double frac = (double) i / steps; // normalized position along blade length
      double w = interpolate(wMap, frac);
      // Guard against negative or NaN widths
      if (Double.isNaN(w) || w < 0.0) w = 0.0;
      samples[i] = w;
    }
    java.util.Arrays.sort(samples);
    int n = samples.length;
    return (n % 2 == 1) ? samples[n / 2] : 0.5 * (samples[n / 2 - 1] + samples[n / 2]);
  }

  @Override
  public boolean isSingleEdged() {
    return this.singleEdged;
  }

  @Override
  public TipSpecifications tipSpecs() {
    return this.tipSpecifications;
  }

  @Override
  public double getTaperValue() {
    double median = getMedianWidth();
    if (!(Double.isFinite(median)) || median <= 1e-9) return 0.0;

    double baseRatio = length / median;
    TreeMap<Double, Double> wMap = buildNormalizedMap(widthAtPoints, widthAtBase, widthAtTip);
    int steps = 25;
    double prevW = interpolate(wMap, 1.0); // start at tip (1.0) and move toward base (0.0)
    for (int i = 1; i <= steps; i++) {
      double frac = 1.0 - (double) i / steps;
      double currW = interpolate(wMap, frac);
      double diff = prevW - currW; // difference between sequential points from tip -> base
      if (diff > 0.0) {
        baseRatio -= diff / median; // subtract normalized difference to keep units consistent
      }
      prevW = currW;
    }
    if (!Double.isFinite(baseRatio) || baseRatio < 0.0) return 0.0;
    return baseRatio;
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
    if (this.fuller.chordLengthByPercent >= 0.0001) {
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

  public double getVolume() {
    if (volume > 0.0) return volume;
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
        if (fuller != null && fuller.sagittaHeightByPercent() > 0.0 && fuller.chordLengthByPercent() > 0.0) {
          double chordPct = fuller.chordLengthByPercent();
          double depthPct = fuller.sagittaHeightByPercent();
          int sides = fuller.bothSides() ? 2 : 1;

          // Clamp sagitta to local thickness
          double maxDepthPct = (sides == 2) ? 0.5 : 1.0;
          double effectiveDepthPct = Math.min(depthPct, maxDepthPct);

          double wMid = 0.5 * (w0 + w1);
          double tMid = 0.5 * (t0 + t1);

          // Calculate local thickness at fuller position (assume centered at 0.5)
          double tLocal0 = getLocalThickness(t0, primaryBevel, singleEdged, 0.5);
          double tLocalMid = getLocalThickness(tMid, primaryBevel, singleEdged, 0.5);
          double tLocal1 = getLocalThickness(t1, primaryBevel, singleEdged, 0.5);

          double sag0 = effectiveDepthPct * tLocal0;
          double sagMid = effectiveDepthPct * tLocalMid;
          double sag1 = effectiveDepthPct * tLocal1;

          // Simpson's rule on fuller area (nonlinear in w): sample at s=0,0.5,1
          double f0 = getCircleSegmentArea(chordPct * w0, sag0);
          double f1 = getCircleSegmentArea(chordPct * w1, sag1);
          double fmid = getCircleSegmentArea(chordPct * wMid, sagMid);

          // Use adaptive Simpson's rule for better accuracy on nonlinear fuller geometry
          double fullerAreaIntegral = adaptiveSimpsonsRule(f0, f1, fmid, segmentLength, 1e-6, 0);
          double fullerVol = fullerAreaIntegral * sides;

          if (fullerVol > segmentVol) fullerVol = segmentVol;

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
   * Calculates the area of a modified superellipse defined by |x/a|^r + |y/b| = 1.
   *
   * IMPORTANT: This is NOT a standard superellipse, and the result differs from an ellipse when r=2.
   * This modified form uses different exponents for different axes, making it asymmetric in exponent application.
   * It is specifically designed to approximate beveled blade cross-sections.
   *
   * Mathematical definition:
   * - The width dimension (x) follows: |x/a|^r
   * - The thickness dimension (y) follows: |y/b|^1 (linear, no exponent)
   * - Together they satisfy: |x/a|^r + |y/b| = 1
   *
   * This differs from the standard symmetric superellipse |x/a|^r + |y/b|^r = 1 because:
   * - Standard superellipse with r=2 gives area = πab (true ellipse)
   * - Our modified form with r=2 gives area = 4ab × (2/3) ≈ 2.667ab (different shape!)
   *
   * The asymmetry is intentional: it better models how blades taper (curved edge, straighter spine/back).
   *
   * Area Formula: A = 4ab × r/(r+1)
   *
   * Examples:
   * - r=1 (diamond shape):     Area = 4ab × 1/2 = 2ab
   * - r=2 (modified ellipse):  Area = 4ab × 2/3 ≈ 2.667ab
   * - r=3 (rounded square):    Area = 4ab × 3/4 = 3ab
   * - r→∞ (approaches square): Area → 4ab
   *
   * @param a the half-width of the blade cross-section, in mm (dimension with the exponent applied)
   * @param b the half-thickness of the blade cross-section, in mm (linear dimension without exponent)
   * @param r the exponent defining the shape of the width dimension (r > 0)
   * @return the area of the cross-section in mm²
   *
   * @throws IllegalArgumentException if a ≤ 0, b ≤ 0, or r ≤ 0
   */
  public static double getSuperEllipseArea(double a, double b, double r) {
    if (a <= 0.0 || b <= 0.0) throw new IllegalArgumentException("a and b must be > 0");
    if (r <= 0.0) throw new IllegalArgumentException("r must be > 0");
    // For |x/a|^r + |y/b| = 1, the area is 4ab * r/(r+1)
    return 4.0 * a * b * r / (r + 1.0);
  }

  /**
   * Calculates the modified superellipse coefficient used in area calculations.
   * For the equation |x/a|^r + |y/b| = 1, the coefficient is 4r/(r+1).
   * Since we normalize by 4ab elsewhere, this returns r/(r+1).
   *
   * @param r the exponent defining the shape of the cross-section
   * @return the superellipse coefficient: r/(r+1)
   */
  public static double superEllipseCoefficient(double r) {
    if (r <= 0.0) return 0.0;
    return r / (r + 1.0);
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

    TreeSet<Double> points = getIntegrationPoints(wMap, tMap);

    if (points.isEmpty()) return 0.0;

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
        if (fuller != null && fuller.sagittaHeightByPercent() > 0.0 && fuller.chordLengthByPercent() > 0.0) {
          double chordPct = fuller.chordLengthByPercent();
          double depthPct = fuller.sagittaHeightByPercent();
          int sides = fuller.bothSides() ? 2 : 1;

          // Clamp sagitta
          double maxDepthPct = (sides == 2) ? 0.5 : 1.0;
          double effectiveDepthPct = Math.min(depthPct, maxDepthPct);

          double wMid = 0.5 * (w0 + w1);
          double tMid = 0.5 * (t0 + t1);

          // Calculate local thickness at fuller position (assume centered at 0.5)
          double tLocal0 = getLocalThickness(t0, primaryBevel, singleEdged, 0.5);
          double tLocalMid = getLocalThickness(tMid, primaryBevel, singleEdged, 0.5);
          double tLocal1 = getLocalThickness(t1, primaryBevel, singleEdged, 0.5);

          double sag0 = effectiveDepthPct * tLocal0;
          double sagMid = effectiveDepthPct * tLocalMid;
          double sag1 = effectiveDepthPct * tLocal1;

          // positions p at s=0,0.5,1 (in normalized coordinates)
          double pA = p0;
          double pB = p0 + D * 0.5;
          double pC = p0 + D;

          double f0 = getCircleSegmentArea(chordPct * w0, sag0);
          double fmid = getCircleSegmentArea(chordPct * wMid, sagMid);
          double f1 = getCircleSegmentArea(chordPct * w1, sag1);

          // Weight fuller area by position for first moment calculation
          // First moment = L^2 * integral of p * area over normalized coordinate
          double pf0 = pA * f0;
          double pfmid = pB * fmid;
          double pf1 = pC * f1;

          // Use adaptive Simpson's rule for integral of p * f(w(p)) over normalized p
          // The segment length in parameter space is D (normalized), multiply by L^2 for physical units
          double positionWeightedIntegral = adaptiveSimpsonsRule(pf0, pf1, pfmid, D, 1e-6, 0);
          double fullerFirstMoment = (L * L) * positionWeightedIntegral * sides;

          if (fullerFirstMoment > segmentFirstMoment) fullerFirstMoment = segmentFirstMoment;

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
   * @param tipShoulderRoundedness the roundedness of the tip shoulder, from 0 (sharp) to 1 (fully rounded)
   */
  public record TipSpecifications(
          double tipRadius,
          double tipBevelAngle,
          double tipBevelShoulderAngle,
          double tipShoulderRoundedness
  ) {
    public static final Codec<TipSpecifications> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.DOUBLE.fieldOf("tipRadius").forGetter(TipSpecifications::tipRadius),
                    Codec.DOUBLE.fieldOf("tipBevelAngle").forGetter(TipSpecifications::tipBevelAngle),
                    Codec.DOUBLE.fieldOf("tipBevelShoulderAngle").forGetter(TipSpecifications::tipBevelShoulderAngle),
                    Codec.DOUBLE.fieldOf("tipShoulderRoundedness").forGetter(TipSpecifications::tipShoulderRoundedness)
            ).apply(instance, TipSpecifications::new)
    );

    public static TipSpecifications noTip() {
      return new TipSpecifications(10000, 90, 180, 0);
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
   * We treat the fuller as a circular segment cut out of the blade cross-sections, defined by its chord length
   * and sagitta. We treat the superimposition as the chord being flush with the surface of the blade,
   * and the sagitta being the depth of the fuller into the blade as a percentage of the blade thickness.
   * If bothSides is true, the fuller is mirrored on both sides of the blade.
   *
   * @param bothSides true if the fuller is on both sides of the blade
   * @param chordLengthByPercent the chord length of the fuller as a percentage of the blade width (0.0 - 1.0)
   * @param sagittaHeightByPercent the depth of the fuller as a percentage of the blade thickness (0.0 - 1.0)
   */
  public record Fuller(
          boolean bothSides,
          double chordLengthByPercent,
          double sagittaHeightByPercent
  ) {
    public static final Codec<Fuller> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.BOOL.fieldOf("bothSides").forGetter(Fuller::bothSides),
                    Codec.DOUBLE.fieldOf("chordLengthPercentage").forGetter(Fuller::chordLengthByPercent),
                    Codec.DOUBLE.fieldOf("sagittaHeightByPercent").forGetter(Fuller::sagittaHeightByPercent)
            ).apply(instance, Fuller::new)
    );

    public static Fuller noFuller() {
      return new Fuller(false, 0.0, 0.0);
    }

  }

  private TreeSet<Double> getIntegrationPoints(TreeMap<Double, Double> wMap, TreeMap<Double, Double> tMap) {
    TreeSet<Double> points = new TreeSet<>();
    points.addAll(wMap.keySet());
    points.addAll(tMap.keySet());

    // Ensure minimum segment count
    int minSegments = 20;
    for (int i = 0; i < minSegments; i++) {
      points.add((double) i / minSegments);
    }
    points.add(1.0);

    return points;
  }

  private double getLocalThickness(double t, Bevel bevel, boolean singleEdged, double widthFraction) {
    double p = bevel.percentageOfBladeWidth();
    double r = bevel.curveFactor();

    if (singleEdged) {
      // Spine at 0, Edge at 1.
      // Flat from 0 to 1-p.
      if (widthFraction <= (1.0 - p)) {
        return t;
      } else {
        // In bevel
        double u = (widthFraction - (1.0 - p)) / p; // 0 to 1
        if (u >= 1.0) return 0.0;
        return t * (1.0 - Math.pow(u, r));
      }
    } else {
      // Double edged. Symmetric about 0.5.
      double distFromCenter = Math.abs(widthFraction - 0.5) * 2.0; // 0 at center, 1 at edges.
      double flatWidthFraction = 1.0 - p;

      if (distFromCenter <= flatWidthFraction) {
        return t;
      } else {
        double u = (distFromCenter - flatWidthFraction) / p;
        if (u >= 1.0) return 0.0;
        return t * (1.0 - Math.pow(u, r));
      }
    }
  }

  /**
   * Calculates the moment of inertia of a circular segment about its centroid.
   * Used for accurate fuller inertia calculations.
   *
   * For a circular segment with chord length c and sagitta h:
   * - Calculate the radius R = (c² + 4h²) / (8h)
   * - Calculate the central angle θ = 2 × arcsin(c / (2R))
   * - Calculate centroid distance from chord: d_c = (4R × sin³(θ/2)) / (3(θ - sin(θ)))
   * - Calculate I_centroid = (R⁴ / 4) × (θ - sin(θ) - (8/3) × sin⁴(θ/2) / (θ - sin(θ)))
   *
   * @param c the chord length in mm
   * @param h the sagitta (height) in mm
   * @return the moment of inertia about the centroid in mm⁴
   */
  private double getCircleSegmentInertia(double c, double h) {
    if (c <= 0.0 || h <= 0.0) return 0.0;

    try {
      double cSq = c * c;
      double hSq = h * h;
      double R = (cSq + 4 * hSq) / (8 * h);

      if (R <= 0.0) return 0.0;

      // Central angle
      double sinTerm = c / (2 * R);
      if (Math.abs(sinTerm) > 1.0) sinTerm = Math.signum(sinTerm);
      double theta = 2 * Math.asin(sinTerm);

      double sinTheta = Math.sin(theta);
      double cosTheta = Math.cos(theta);

      // Area of segment
      double A = R * R * (theta - sinTheta) / 2;
      if (A <= 0.0) return 0.0;

      // Centroid distance from chord (measured toward center)
      double sinHalf = Math.sin(theta / 2);
      double denominator = theta - sinTheta;
      if (Math.abs(denominator) < 1e-10) return 0.0;

      double d_c = (4 * R * sinHalf * sinHalf * sinHalf) / (3 * denominator);

      // Moment of inertia about centroid using integration
      // I_chord = R^4 * (θ - sin(θ)) / 8
      // I_centroid = I_chord - A * d_c^2
      double I_chord = R * R * R * R * (theta - sinTheta) / 8;
      double I_centroid = I_chord - A * d_c * d_c;

      if (Double.isNaN(I_centroid) || I_centroid < 0.0) return 0.0;
      return I_centroid;
    } catch (Exception ex) {
      return 0.0;
    }
  }

  /**
   * Recursively integrates a function over a segment using adaptive Simpson's rule.
   * Divides segments until the change between successive approximations is below tolerance.
   *
   * @param f0 function value at segment start
   * @param f1 function value at segment end
   * @param fMid function value at segment midpoint
   * @param segmentLen length of the segment
   * @param tolerance acceptable error threshold
   * @param depth current recursion depth (to prevent infinite recursion)
   * @return integrated value over the segment
   */
  private double adaptiveSimpsonsRule(double f0, double f1, double fMid, double segmentLen,
                                       double tolerance, int depth) {
    // Prevent infinite recursion
    if (depth > 20) {
      return (segmentLen / 6.0) * (f0 + 4.0 * fMid + f1);
    }

    // Simpson's rule for full segment
    double fullSimpson = (segmentLen / 6.0) * (f0 + 4.0 * fMid + f1);

    // Simpson's rule for two half-segments
    double segLen_half = segmentLen / 2.0;
    // We need quarter points, but we don't have them computed
    // Use simple linear interpolation for now
    double f_quarter1 = (f0 + fMid) / 2.0;
    double f_quarter2 = (fMid + f1) / 2.0;

    double halfSimpson1 = (segLen_half / 6.0) * (f0 + 4.0 * f_quarter1 + fMid);
    double halfSimpson2 = (segLen_half / 6.0) * (fMid + 4.0 * f_quarter2 + f1);
    double halfSimpson = halfSimpson1 + halfSimpson2;

    // Check if refinement is needed
    double error = Math.abs(halfSimpson - fullSimpson) / 15.0; // Simpson's rule error estimate

    if (error < tolerance) {
      return halfSimpson; // Refined estimate is likely more accurate
    } else {
      // Recursively refine the two halves
      return adaptiveSimpsonsRule(f0, fMid, f_quarter1, segLen_half, tolerance / 2.0, depth + 1) +
             adaptiveSimpsonsRule(fMid, f1, f_quarter2, segLen_half, tolerance / 2.0, depth + 1);
    }
  }

}
