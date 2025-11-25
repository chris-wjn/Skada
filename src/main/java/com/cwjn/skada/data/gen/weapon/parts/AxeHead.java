package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.parts.attack_types.SlashCapable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Arrays;

/**
  * Class that represents the axe head part of a weapon.
 */
public class AxeHead extends WeaponHead implements SlashCapable {

  public static final Codec<AxeHead> CODEC = RecordCodecBuilder.create((RecordCodecBuilder.Instance<AxeHead> instance) ->
          instance.group(
                  Codec.DOUBLE.fieldOf("eyeLength").forGetter(a -> a.eyeLength),
                  Codec.DOUBLE.fieldOf("eyeHeight").forGetter(a -> a.eyeHeight),
                  Codec.DOUBLE.fieldOf("cheekLength").forGetter(a -> a.cheekLength),
                  Codec.DOUBLE.fieldOf("cheekHeight").forGetter(a -> a.cheekHeight),
                  Codec.DOUBLE.fieldOf("beardHeight").forGetter(a -> a.beardHeight),
                  Codec.DOUBLE.fieldOf("beardTipDistance").forGetter(a -> a.beardTipDistance),
                  Codec.DOUBLE.fieldOf("toeHeight").forGetter(a -> a.toeHeight),
                  Codec.DOUBLE.fieldOf("toeTipDistance").forGetter(a -> a.toeTipDistance),
                  Codec.DOUBLE.fieldOf("eyeThickness").forGetter(a -> a.eyeThickness),
                  Codec.DOUBLE.fieldOf("eyeHoleSemiMajorAxis").forGetter(a -> a.eyeHoleSemiMajorAxis),
                  Codec.DOUBLE.fieldOf("eyeHoleSemiMinorAxis").forGetter(a -> a.eyeHoleSemiMinorAxis),
                  Blade.Bevel.CODEC.optionalFieldOf("primaryBevel", null).forGetter(a -> a.primaryBevel),
                  Blade.EdgeBevel.CODEC.optionalFieldOf("edgeBevel", null).forGetter(a -> a.edgeBevel)
          ).apply(instance, AxeHead::new)
  );

  @Override
  public Codec<? extends WeaponHead> type() {
    return CODEC;
  }

  @Override
  public String typeKey() { return "axe"; }

  /**
   * Our approximation of the profile of the axehead is 2 rectangles and 2 triangles:
   * - Eye rectangle (where the handle goes through)
   * - Cheek rectangle (the main body of the axehead)
   * - Beard triangle (the lower pointed part of the axehead)
   * - Toe triangle (the upper pointed part of the axehead)
   * The cheek rectangle is assumed to be directly adjacent to the eye rectangle, while the beard
   * and toe triangles are above and below the cheek rectangle respectively. The height and tip distance
   * refer to the exact coordinates of the tip of the triangle, where the base of the triangle is the
   * cheek length, the height is how far above/below the top/bottom of the cheek rectangle the tip is,
   * and the tip distance is how far horizontally from the place where the cheek and eye rectangles meet.
   * There isn't always a beard or toe, in which case their height and tip distance should be set to 0.
   */
  private final double eyeLength;
  private final double eyeHeight;
  private final double cheekLength;
  private final double cheekHeight;
  private final double beardHeight;
  private final double beardTipDistance;
  private final double toeHeight;
  private final double toeTipDistance;

  /*
    * Thickness of the axehead's eye portion, which will be considered the maximum thickness of the axehead. The primary
    * bevel will determine where the thickness taper begins, and how it behaves. The eye hole is the cutout of the eye
    * where the handle would presumably go through. We'll treat it as an elliptical cylinder, where the eye height
    * is the height of the elliptical cylinder.
   */
  private final double eyeThickness;
  private final double eyeHoleSemiMajorAxis;
  private final double eyeHoleSemiMinorAxis;

  /*
    * Bevel profile of the axehead. We'll use the same bevel classes as Blade, since they are applicable here as well.
    * Primary bevel is the main bevel along the edge of the axehead, while edge bevel is any additional beveling
    * done to the edge (e.g. secondary bevel). Since an axehead is made of two rectangles and potentially two more triangles,
    * we'll treat the triangles as rectangles for the purpose of calculating percentage of blade width, then apply the
    * bevels to the triangles. Whatever portion of the bevel exceeds the triangle's width will be ignored.
   */
  private final Blade.Bevel primaryBevel;
  private final Blade.EdgeBevel edgeBevel;

  public AxeHead(double eyeLength, double eyeHeight,
                 double cheekLength, double cheekHeight,
                 double beardHeight, double beardTipDistance,
                 double toeHeight, double toeTipDistance,
                 double eyeThickness, double eyeHoleSemiMajorAxis, double eyeHoleSemiMinorAxis,
                 Blade.Bevel primaryBevel, Blade.EdgeBevel edgeBevel) {
    this.eyeLength = eyeLength;
    this.eyeHeight = eyeHeight;
    this.cheekLength = cheekLength;
    this.cheekHeight = cheekHeight;
    this.beardHeight = beardHeight;
    this.beardTipDistance = beardTipDistance;
    this.toeHeight = toeHeight;
    this.toeTipDistance = toeTipDistance;
    this.eyeThickness = eyeThickness;
    this.eyeHoleSemiMajorAxis = eyeHoleSemiMajorAxis;
    this.eyeHoleSemiMinorAxis = eyeHoleSemiMinorAxis;
    this.primaryBevel = primaryBevel;
    this.edgeBevel = edgeBevel;
  }

  @Override
  public double getLength() {
    return Math.max(0.0, eyeLength) + Math.max(0.0, cheekLength);
  }

  @Override
  public double getWidth() {
    double cheekTop = cheekHeight + Math.max(0.0, toeHeight);
    double cheekBottom = -beardHeight;
    return Math.max(eyeHeight, cheekTop - cheekBottom);
  }

  @Override
  public double getSlashNormalizedIdealPointOfBalance() {
    //the ideal PoB for an axe is where the handle intersects the head, so 0mm up the head.
    return 0.0;
  }

  @Override
  public double getMedianWidth() {
    double eLen = Math.max(0.0, eyeLength);
    double cLen = Math.max(0.0, cheekLength);
    double cH = Math.max(0.0, cheekHeight);
    double bH = Math.max(0.0, beardHeight);
    double tH = Math.max(0.0, toeHeight);

    if (cLen <= 0.0) return 0.0;

    double minY = (bH > 0.0) ? -bH : 0.0;
    double maxY = cH + tH;
    if (maxY < minY) maxY = minY;

    int steps = 25;
    double[] distances = new double[steps + 1];
    for (int i = 0; i <= steps; i++) {
      double frac = (double) i / steps;
      double y = minY + (maxY - minY) * frac;
      double edgeX = bladeEdgeX(eLen, cLen, cH, bH, tH, beardTipDistance, toeTipDistance, y);
      distances[i] = Math.max(0.0, edgeX - eLen);
    }

    Arrays.sort(distances);
    int n = distances.length;
    return (n % 2 == 1)
            ? distances[n / 2]
            : 0.5 * (distances[n / 2 - 1] + distances[n / 2]);
  }

  private double bladeEdgeX(double eLen, double cLen, double cH, double bH, double tH,
                            double beardTipDistance, double toeTipDistance, double y) {
    double frontX = eLen + cLen;

    if (y >= 0.0 && y <= cH) {
      return frontX;
    }
    if (y > cH) {
      if (tH <= 0.0) return frontX;
      double cappedY = Math.min(y, cH + tH);
      double progress = (cappedY - cH) / tH;
      double apexX = eLen + toeTipDistance;
      return frontX + (apexX - frontX) * progress;
    }

    if (bH <= 0.0) return frontX;
    double cappedY = Math.max(y, -bH);
    double progress = (0.0 - cappedY) / bH;
    double apexX = eLen + beardTipDistance;
    return frontX + (apexX - frontX) * progress;
  }

  @Override
  public Blade.Bevel primaryBevel() {
    return primaryBevel;
  }

  @Override
  public Blade.EdgeBevel edgeBevel() {
    return edgeBevel;
  }

  @Override
  public double getVolume() {
    if (eyeThickness <= 0.0) return 0.0;

    double eLen = Math.max(0.0, eyeLength);
    double eH = Math.max(0.0, eyeHeight);
    double cLen = Math.max(0.0, cheekLength);
    double cH = Math.max(0.0, cheekHeight);
    double bH = Math.max(0.0, beardHeight);
    double tH = Math.max(0.0, toeHeight);

    // Eye rectangle (unbeveled)
    double eyeArea = eLen * eH;

    // Cheek rectangle (beveled)
    double cheekArea = cLen * cH;

    // Beard triangle (always full base; negative tip distance does NOT clip area per new requirement)
    double beardArea = 0.5 * cLen * bH;

    // Toe triangle (same logic)
    double toeArea = 0.5 * cLen * tH;

    // Bevel parameters (applied independently to cheek + triangles)
    double p = 0.0;
    double r = 1.0;
    if (primaryBevel != null) {
      p = primaryBevel.percentageOfBladeWidth();
      r = primaryBevel.curveFactor();
    }
    p = Math.max(0.0, Math.min(1.0, p));
    r = Math.max(1e-6, r);

    // Superellipse coefficient for a=b=0.5 (area equals coefficient)
    double superCoeff = Blade.superEllipseCoefficient(r);

    double K = (1.0 - p) + p * superCoeff;

    // Component volumes: apply K separately (mathematically equivalent to combined, but explicit per part)
    double eyeVolume = eyeArea * eyeThickness; // not beveled
    double cheekVolume = cheekArea * eyeThickness * K;
    double beardVolume = beardArea * eyeThickness * K;
    double toeVolume = toeArea * eyeThickness * K;

    double grossVolume = eyeVolume + cheekVolume + beardVolume + toeVolume;

    // Subtract elliptical eye hole
    double holeVol = 0.0;
    if (eyeHoleSemiMajorAxis > 0.0 && eyeHoleSemiMinorAxis > 0.0) {
      holeVol = Math.PI * eyeHoleSemiMajorAxis * eyeHoleSemiMinorAxis * eyeThickness;
    }

    double finalVol = grossVolume - holeVol;
    return Math.max(finalVol, 0.0);
  }

  @Override
  public double getPointOfBalance() {
    // Point of balance measured from the start of the eye rectangle (x = 0)
    // If volume is zero, default to half of total length (eyeLength + cheekLength)/2
    double totalVolume = getVolume();
    double totalLength = Math.max(0.0, eyeLength) + Math.max(0.0, cheekLength);
    if (totalVolume <= 0.0) return totalLength / 2.0;

    double eLen = Math.max(0.0, eyeLength);
    double eH = Math.max(0.0, eyeHeight);
    double cLen = Math.max(0.0, cheekLength);
    double cH = Math.max(0.0, cheekHeight);
    double bH = Math.max(0.0, beardHeight);
    double tH = Math.max(0.0, toeHeight);

    // Areas (plan)
    double eyeArea = eLen * eH;
    double cheekArea = cLen * cH;
    double beardArea = 0.5 * cLen * bH;
    double toeArea = 0.5 * cLen * tH;

    // Bevel factor K for beveled parts
    double p = 0.0; double r = 1.0;
    if (primaryBevel != null) { p = primaryBevel.percentageOfBladeWidth(); r = primaryBevel.curveFactor(); }
    p = Math.max(0.0, Math.min(1.0, p)); r = Math.max(1e-6, r);
    double superCoeff = Blade.superEllipseCoefficient(r);
    double K = (1.0 - p) + p * superCoeff;

    // Component volumes (must mirror logic in getVolume())
    double eyeVolume = eyeArea * eyeThickness;
    double cheekVolume = cheekArea * eyeThickness * K;
    double beardVolume = beardArea * eyeThickness * K;
    double toeVolume = toeArea * eyeThickness * K;

    // Eye hole subtraction (assume centered in eye rectangle at X = eLen/2)
    double holeVolume = 0.0;
    double holeCentroidX = eLen / 2.0;
    if (eyeHoleSemiMajorAxis > 0.0 && eyeHoleSemiMinorAxis > 0.0) {
      holeVolume = Math.PI * eyeHoleSemiMajorAxis * eyeHoleSemiMinorAxis * eyeThickness;
    }

    // Centroids along x
    double eyeCentroidX = eLen / 2.0; // center of eye rectangle
    double cheekCentroidX = eLen + cLen / 2.0; // center of cheek rectangle

    // Triangle centroids: average of vertex x-coordinates (base endpoints + apex) / 3
    double baseStart = eLen;
    double baseEnd = eLen + cLen;
    double beardApexX = eLen + beardTipDistance; // can be negative relative to 0
    double toeApexX = eLen + toeTipDistance;
    double beardCentroidX = (baseStart + baseEnd + beardApexX) / 3.0;
    double toeCentroidX = (baseStart + baseEnd + toeApexX) / 3.0;

    // First moment contributions
    double firstMoment = 0.0;
    firstMoment += eyeVolume * eyeCentroidX;
    firstMoment += cheekVolume * cheekCentroidX;
    firstMoment += beardVolume * beardCentroidX;
    firstMoment += toeVolume * toeCentroidX;
    if (holeVolume > 0.0) firstMoment -= holeVolume * holeCentroidX;

    return firstMoment / totalVolume;
  }

}
