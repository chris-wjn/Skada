package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.WeaponProfile;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.SlashCapable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Arrays;

/**
  * Class that represents the axe head part of a weapon.
 */
public class AxeHead extends WeaponHead implements SlashCapable {

  public static final Codec<AxeHead> CODEC = RecordCodecBuilder.create((RecordCodecBuilder.Instance<AxeHead> instance) -> instance.group(
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
  ).apply(instance, (eyeLen, eyeHt, cheekLen, cheekHt, beardHt, beardDist, toeHt, toeDist, eyeThick, holeMaj, holeMin, primBev, edgeBev) -> new AxeHead(eyeLen, eyeHt, cheekLen, cheekHt, beardHt, beardDist, toeHt, toeDist, eyeThick, holeMaj, holeMin, primBev, edgeBev)));

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
  public double getPrimaryAxisLength() {
    return Math.max(0.0, eyeLength) + Math.max(0.0, cheekLength);
  }

  @Override
  public double getSecondaryAxisLength() {
    return Math.max(0.0, toeHeight) + Math.max(0.0, cheekHeight) + Math.max(0.0, beardHeight);
  }

  @Override
  public double getMomentOfInertia(double distanceFromPivot, double density, WeaponProfile.HeadOrientation orientation) {
    // Convert density from g/cm³ to g/mm³ for calculations
    double densityPerMM3 = density / 1000.0;

    // Calculate moment of inertia about the pivot point.
    // The pivot is at distance 'distanceFromPivot' from the center of the eye (handle axis).
    // We assume the handle is the Y-axis, and the axe head extends in the X-direction.
    // The rotation axis is the Z-axis (perpendicular to both handle and blade length).
    // Pivot point is at (0, -distanceFromPivot) relative to eye center (0,0).
    // Or more simply: Eye center is at distance 'distanceFromPivot' from the pivot.
    // So Eye Center = (0, distanceFromPivot).
    // Rotation axis is at (0,0).

    double eLen = Math.max(0.0, eyeLength);
    double eH = Math.max(0.0, eyeHeight);
    double cLen = Math.max(0.0, cheekLength);
    double cH = Math.max(0.0, cheekHeight);
    double bH = Math.max(0.0, beardHeight);
    double tH = Math.max(0.0, toeHeight);
    double eThick = Math.max(0.0, eyeThickness);

    // Bevel factor K for volume/mass scaling
    double p = 0.0; double r = 1.0;
    if (primaryBevel != null) { p = primaryBevel.percentageOfBladeWidth(); r = primaryBevel.curveFactor(); }
    p = Math.max(0.0, Math.min(1.0, p)); r = Math.max(1e-6, r);
    double superCoeff = Blade.superEllipseCoefficient(r);
    double K = (1.0 - p) + p * superCoeff;

    double totalInertia = 0.0;

    // 1. Eye (Rectangular Prism with Elliptical Hole)
    // Center at (0, distanceFromPivot).
    // Dimensions: eLen (X), eH (Y), eThick (Z).
    // I_z_solid_cm = 1/12 * M_solid * (eLen^2 + eH^2)
    double eyeSolidVol = eLen * eH * eThick;
    double eyeSolidMass = eyeSolidVol * densityPerMM3;
    double eyeSolidIcm = (1.0/12.0) * eyeSolidMass * (eLen*eLen + eH*eH);
    double eyeDistSq = distanceFromPivot * distanceFromPivot;
    totalInertia += eyeSolidIcm + eyeSolidMass * eyeDistSq;

    // Subtract Hole
    if (eyeHoleSemiMajorAxis > 0.0 && eyeHoleSemiMinorAxis > 0.0) {
        // Hole is elliptical cylinder along Y axis.
        // Cross section in XZ plane: semi-axes a=eyeHoleSemiMajorAxis (X), b=eyeHoleSemiMinorAxis (Z).
        // Length = eH.
        // I_z_hole_cm = I_x + I_y (perpendicular axis theorem? No, 3D).
        // I_z (transverse to cylinder axis Y) = 1/4 M (a^2 + H^2/3) + 1/4 M b^2 ?
        // For cylinder along Y: I_x = 1/12 M (3b^2 + H^2), I_z = 1/12 M (3a^2 + H^2).
        // Wait, for elliptical cylinder x^2/a^2 + z^2/b^2 = 1.
        // I_z = integral (x^2 + y^2) dm.
        // integral y^2 dm = M * H^2 / 12.
        // integral x^2 dm = M * a^2 / 4.
        // So I_z = M * (a^2/4 + H^2/12).
        double hVol = Math.PI * eyeHoleSemiMajorAxis * eyeHoleSemiMinorAxis * eH;
        double hMass = hVol * densityPerMM3;
        double hIcm = hMass * (eyeHoleSemiMajorAxis*eyeHoleSemiMajorAxis/4.0 + eH*eH/12.0);
        totalInertia -= (hIcm + hMass * eyeDistSq);
    }

    // 2. Cheek (Rectangular Prism, Beveled)
    // Center X: eLen/2 + cLen/2.
    // Center Y: distanceFromPivot (assuming centered on eye).
    // Mass scaled by K.
    // I_cm approx: 1/12 * M * (cLen^2 + cH^2).
    double cheekVol = cLen * cH * eThick * K;
    double cheekMass = cheekVol * densityPerMM3;
    double cheekCx = eLen/2.0 + cLen/2.0;
    double cheekCy = distanceFromPivot;
    double cheekIcm = (1.0/12.0) * cheekMass * (cLen*cLen + cH*cH);
    double cheekDistSq = cheekCx*cheekCx + cheekCy*cheekCy;
    totalInertia += cheekIcm + cheekMass * cheekDistSq;

    // 3. Beard (Triangle)
    // Vertices relative to pivot:
    // Base start: (eLen/2, distanceFromPivot - cH/2)
    // Base end: (eLen/2 + cLen, distanceFromPivot - cH/2)
    // Tip: (eLen/2 + beardTipDistance, distanceFromPivot - cH/2 - bH)
    if (bH > 0) {
        double beardVol = 0.5 * cLen * bH * eThick * K;
        double beardMass = beardVol * densityPerMM3;

        double x1 = eLen/2.0;
        double y1 = distanceFromPivot - cH/2.0;
        double x2 = eLen/2.0 + cLen;
        double y2 = y1;
        double x3 = eLen/2.0 + beardTipDistance;
        double y3 = y1 - bH;

        double beardCx = (x1 + x2 + x3) / 3.0;
        double beardCy = (y1 + y2 + y3) / 3.0;

        // I_cm for triangle approx: M/18 * (b^2 + h^2) ?
        // Let's use point mass approx for I_cm (small) + parallel axis.
        // Or better: I_z = I_cm + M * d^2.
        // I_cm of triangle is roughly M * (radius_gyration)^2.
        // For right triangle, I_cm = M/18 (b^2 + h^2).
        // Let's use that as approximation for general triangle.
        double beardIcm = (1.0/18.0) * beardMass * (cLen*cLen + bH*bH);

        double beardDistSq = beardCx*beardCx + beardCy*beardCy;
        totalInertia += beardIcm + beardMass * beardDistSq;
    }

    // 4. Toe (Triangle)
    // Vertices relative to pivot:
    // Base start: (eLen/2, distanceFromPivot + cH/2)
    // Base end: (eLen/2 + cLen, distanceFromPivot + cH/2)
    // Tip: (eLen/2 + toeTipDistance, distanceFromPivot + cH/2 + tH)
    if (tH > 0) {
        double toeVol = 0.5 * cLen * tH * eThick * K;
        double toeMass = toeVol * densityPerMM3;

        double x1 = eLen/2.0;
        double y1 = distanceFromPivot + cH/2.0;
        double x2 = eLen/2.0 + cLen;
        double y2 = y1;
        double x3 = eLen/2.0 + toeTipDistance;
        double y3 = y1 + tH;

        double toeCx = (x1 + x2 + x3) / 3.0;
        double toeCy = (y1 + y2 + y3) / 3.0;

        double toeIcm = (1.0/18.0) * toeMass * (cLen*cLen + tH*tH);

        double toeDistSq = toeCx*toeCx + toeCy*toeCy;
        totalInertia += toeIcm + toeMass * toeDistSq;
    }

    return totalInertia;
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

    // Subtract elliptical eyehole
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
