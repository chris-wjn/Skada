package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.WeaponProfile;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.SlashCapable;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.ThrustCapable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Class that represents a sickle or scythe head. We'll treat this
 * as a curved blade with one edge and a pointed tip.
 */
public class SickleHead extends WeaponHead implements ThrustCapable, SlashCapable {

  public static final Codec<SickleHead> CODEC = RecordCodecBuilder.create((RecordCodecBuilder.Instance<SickleHead> instance) ->
          instance.group(
                  Codec.DOUBLE.fieldOf("spineArcLength").forGetter(s -> s.spineArcLength),
                  Codec.DOUBLE.fieldOf("spineChordLength").forGetter(s -> s.spineChordLength),
                  Codec.DOUBLE.fieldOf("spineSagittaHeight").forGetter(s -> s.spineSagittaHeight),
                  Codec.DOUBLE.fieldOf("bladeArcLength").forGetter(s -> s.bladeArcLength),
                  Codec.DOUBLE.fieldOf("bladeChordLength").forGetter(s -> s.bladeChordLength),
                  Codec.DOUBLE.fieldOf("bladeSagittaHeight").forGetter(s -> s.bladeSagittaHeight),
                  Codec.DOUBLE.fieldOf("spineBaseToBladeBaseDistance").forGetter(s -> s.spineBaseToBladeBaseDistance),
                  Codec.DOUBLE.fieldOf("spineTipToBladeTipDistance").forGetter(s -> s.spineTipToBladeTipDistance),
                  Codec.DOUBLE.fieldOf("spineThickness").forGetter(s -> s.spineThickness),
                  Blade.Bevel.CODEC.optionalFieldOf("primaryBevel", Blade.Bevel.defaultBevel()).forGetter(s -> s.primaryBevel),
                  Blade.EdgeBevel.CODEC.optionalFieldOf("edgeBevel", Blade.EdgeBevel.noBevel()).forGetter(s -> s.edgeBevel)
          ).apply(instance, SickleHead::new)
  );

  @Override
  public Codec<? extends WeaponHead> type() {
    return CODEC;
  }

  @Override
  public String typeKey() { return "sickle"; }

  private final double spineArcLength; //Arc length of the spine of the blade in mm
  private final double spineChordLength; //Straight line distance between the ends of the spine in mm
  private final double spineSagittaHeight; //Distance from chord midpoint to arc midpoint in mm
  private final double bladeArcLength; //Arc length of the cutting edge of the blade in mm
  private final double bladeChordLength; //Straight line distance between the ends of the blade in mm
  private final double bladeSagittaHeight; //Distance from chord midpoint to arc midpoint in mm
  private final double spineBaseToBladeBaseDistance; //Vertical distance between the start of the spine and the start of the blade in mm
  private final double spineTipToBladeTipDistance; //Vertical distance between the tip of the spine and the tip of the blade in mm
  private final double spineThickness; //Thickness of the spine in mm
  private final Blade.EdgeBevel edgeBevel;
  private final Blade.Bevel primaryBevel;

  /**
   * Constructor for SickleHead.
   * @param spineArcLength Arc length of the spine
   * @param spineChordLength Chord length of the spine
   * @param spineSagittaHeight Sagitta height of the spine
   * @param bladeArcLength Arc length of the blade
   * @param bladeChordLength Chord length of the blade
   * @param bladeSagittaHeight Sagitta height of the blade
   * @param spineBaseToBladeBaseDistance Vertical distance from spine base to blade base
   * @param spineTipToBladeTipDistance Vertical distance from spine tip to blade tip
   * @param spineThickness Thickness of the spine
   * @param primaryBevel Primary bevel along bottom/cutting edge of sickle
   * @param edgeBevel Edge bevel along bottom/cutting edge of sickle
   */
  public SickleHead(double spineArcLength,
                    double spineChordLength,
                    double spineSagittaHeight,
                    double bladeArcLength,
                    double bladeChordLength,
                    double bladeSagittaHeight,
                    double spineBaseToBladeBaseDistance,
                    double spineTipToBladeTipDistance,
                    double spineThickness,
                    Blade.Bevel primaryBevel,
                    Blade.EdgeBevel edgeBevel) {
    this.spineArcLength = spineArcLength;
    this.spineChordLength = spineChordLength;
    this.spineSagittaHeight = spineSagittaHeight;
    this.bladeArcLength = bladeArcLength;
    this.bladeChordLength = bladeChordLength;
    this.bladeSagittaHeight = bladeSagittaHeight;
    this.spineBaseToBladeBaseDistance = spineBaseToBladeBaseDistance;
    this.spineTipToBladeTipDistance = spineTipToBladeTipDistance;
    this.spineThickness = spineThickness;
    this.primaryBevel = primaryBevel;
    this.edgeBevel = edgeBevel;
  }

  @Override
  public double getPrimaryAxisLength() {
    return Math.max(0.0, spineChordLength);
  }

  @Override
  public double getSecondaryAxisLength() {
    return Math.max(0.0, spineSagittaHeight);
  }

  @Override
  public double getMomentOfInertia(double distanceFromPivot, double density, WeaponProfile.HeadOrientation orientation) {
    // Convert density from g/cm³ to g/mm³ for calculations
    double densityPerMM3 = density / 1000.0;

    // Calculate moment of inertia about the pivot point using numerical integration.
    // Pivot is at (0, -distanceFromPivot) relative to spine base (0,0).
    // Axis of rotation is Z-axis (perpendicular to plane).

    if (spineThickness <= 0.0) return 0.0;

    // 1. Spine Geometry
    double L_s = spineChordLength;
    double h_s = spineSagittaHeight;
    if (L_s <= 0) return 0.0;

    double R_s = getCircleRadius(L_s, h_s);
    if (R_s <= 0) return 0.0;

    // Spine Circle Center (assuming chord on X-axis, arc above)
    double cx_s = L_s / 2.0;
    double cy_s = h_s - R_s;

    // 2. Blade Geometry
    double L_b = bladeChordLength;
    double h_b = bladeSagittaHeight;
    if (L_b <= 0) return 0.0;

    double R_b = getCircleRadius(L_b, h_b);
    if (R_b <= 0) return 0.0;

    double d_base = spineBaseToBladeBaseDistance;
    double d_tip = spineTipToBladeTipDistance;

    // Blade chord projection on X
    double dy_b = d_base - d_tip;
    double term = L_b * L_b - dy_b * dy_b;
    if (term < 0) term = 0;
    double x_tip_b = Math.sqrt(term);

    // Blade Chord Angle alpha
    double alpha = Math.atan2(d_base - d_tip, x_tip_b);

    // Blade Circle Center in Global Coords
    double cu_b = L_b / 2.0;
    double cv_b = h_b - R_b;

    double sinA = Math.sin(alpha);
    double cosA = Math.cos(alpha);

    double cx_b = cu_b * cosA - cv_b * sinA;
    double cy_b = -d_base + cu_b * sinA + cv_b * cosA;

    // 3. Integration
    int numSamples = 200;
    double max_x = Math.min(L_s, x_tip_b);
    double dx = max_x / numSamples;

    // Bevel factor K
    double p = 0.0; double r = 1.0;
    if (primaryBevel != null) { p = primaryBevel.percentageOfBladeWidth(); r = primaryBevel.curveFactor(); }
    p = Math.max(0.0, Math.min(1.0, p)); r = Math.max(1e-6, r);
    double superCoeff = Blade.superEllipseCoefficient(r);
    double K = (1.0 - p) + p * superCoeff;

    double totalInertia = 0.0;

    for (int i = 0; i < numSamples; i++) {
      double x = (i + 0.5) * dx; // Midpoint

      // Spine Y
      double dx_s = x - cx_s;
      double root_s = R_s * R_s - dx_s * dx_s;
      if (root_s < 0) continue;
      double y_s = cy_s + Math.sqrt(root_s);

      // Blade Y
      double dx_b_val = x - cx_b;
      double root_b = R_b * R_b - dx_b_val * dx_b_val;
      if (root_b < 0) continue;
      double y_b = cy_b + Math.sqrt(root_b);

      // Width
      double w = y_s - y_b;
      if (w < 0) w = 0;

      // Mass of element
      double dm = densityPerMM3 * spineThickness * w * dx * K;

      // Centroid Y
      double y_c = (y_s + y_b) / 2.0;

      // Distance from pivot
      double distSq = x * x + Math.pow(y_c + distanceFromPivot, 2);

      // Inertia of element (parallel axis theorem)
      // I_cm of rectangle strip about Z axis
      double dI_cm = (1.0 / 12.0) * dm * (w * w + dx * dx);

      totalInertia += dI_cm + dm * distSq;
    }

    return totalInertia;
  }

  @Override
  public double getMedianWidth() {
    return 0;
  }

  @Override
  public boolean isSingleEdged() {
    return true;
  }

  @Override
  public Blade.TipSpecifications tipSpecs() {
    double epsilon = 1e-6;
    boolean arcsMeet = Math.abs(spineTipToBladeTipDistance) < epsilon;

    double spineRadius = getCircleRadius(spineChordLength, spineSagittaHeight);
    double bladeRadius = getCircleRadius(bladeChordLength, bladeSagittaHeight);

    if (spineRadius <= 0 || bladeRadius <= 0) {
      return Blade.TipSpecifications.noTip();
    }

    double spineTheta = 2.0 * Math.asin(Math.min(1.0, spineChordLength / (2.0 * spineRadius))); // radians
    double bladeTheta = 2.0 * Math.asin(Math.min(1.0, bladeChordLength / (2.0 * bladeRadius))); // radians

    // Tangent-chord endpoint angle is θ/2 for each arc; take average then convert to degrees.
    double tangentAngleSpine = spineTheta / 2.0;
    double tangentAngleBlade = bladeTheta / 2.0;
    double bevelAngleRadians = (tangentAngleSpine + tangentAngleBlade) / 2.0;
    double bevelAngleDegrees = Math.toDegrees(bevelAngleRadians);

    // Clamp bevel angle to a reasonable physical range if extreme.
    if (Double.isNaN(bevelAngleDegrees) || bevelAngleDegrees <= 0) bevelAngleDegrees = 15.0;
    if (bevelAngleDegrees > 120.0) bevelAngleDegrees = 120.0;

    double tipRadiusNm = arcsMeet ? 5.0 : 10.0; // nm

    return new Blade.TipSpecifications(
            tipRadiusNm,
            bevelAngleDegrees,
            180.0,
            0
    );
  }

  @Override
  public double getTaperValue() {
    return 0;
  }

  @Override
  public double getSlashNormalizedIdealPointOfBalance() {
    return 0.0;
  }

  @Override
  public double getThrustNormalizedIdealPointOfBalance() {
    return 0.0;
  }

  @Override
  public double getVolume() {
    if (spineThickness <= 0.0) return 0.0;

    //Calculate the area using numerical integration,
    //sampling along the arc length and integrating the width between spine and blade
    int numSamples = 100;
    double totalArea = 0.0;

    // Get radii for both arcs
    double spineRadius = getCircleRadius(spineChordLength, spineSagittaHeight);
    double bladeRadius = getCircleRadius(bladeChordLength, bladeSagittaHeight);

    if (spineRadius <= 0 || bladeRadius <= 0) return 0.0;

    double spineTheta = 2.0 * Math.asin(Math.min(1.0, spineChordLength / (2.0 * spineRadius)));
    double bladeTheta = 2.0 * Math.asin(Math.min(1.0, bladeChordLength / (2.0 * bladeRadius)));

    double maxArcLength = Math.max(spineArcLength, bladeArcLength);
    double stepSize = maxArcLength / numSamples;

    for (int i = 0; i < numSamples; i++) {
      double t = (double) i / numSamples; //0 to 1

      double widthAtT = getWidthAtParameter(t, spineRadius, bladeRadius, spineTheta, bladeTheta);
      double widthAtTplus1 = getWidthAtParameter((double)(i + 1) / numSamples, spineRadius, bladeRadius, spineTheta, bladeTheta);

      //Trapezoidal integration
      totalArea += 0.5 * (widthAtT + widthAtTplus1) * stepSize;
    }

    double bevelFactor = 1.0;
    if (primaryBevel != null) {
      double p = Math.max(0.0, Math.min(1.0, primaryBevel.percentageOfBladeWidth()));
      double r = Math.max(1e-6, primaryBevel.curveFactor());
      double superCoeff = Blade.superEllipseCoefficient(r);
      bevelFactor = (1.0 - p) + p * superCoeff;
    }

    return totalArea * spineThickness * bevelFactor;
  }

  @Override
  public double getPointOfBalance() {
    double totalVolume = getVolume();
    if (totalVolume <= 0.0) return spineChordLength / 2.0;

    //Calculate center of mass using numerical integration
    int numSamples = 100;
    double firstMoment = 0.0;

    double spineRadius = getCircleRadius(spineChordLength, spineSagittaHeight);
    double bladeRadius = getCircleRadius(bladeChordLength, bladeSagittaHeight);

    if (spineRadius <= 0 || bladeRadius <= 0) return spineChordLength / 2.0;

    double spineTheta = 2.0 * Math.asin(Math.min(1.0, spineChordLength / (2.0 * spineRadius)));
    double bladeTheta = 2.0 * Math.asin(Math.min(1.0, bladeChordLength / (2.0 * bladeRadius)));

    double maxArcLength = Math.max(spineArcLength, bladeArcLength);
    double stepSize = maxArcLength / numSamples;

    for (int i = 0; i < numSamples; i++) {
      double t = (double) i / numSamples;
      double tNext = (double)(i + 1) / numSamples;

      double xPosition = t * spineChordLength;
      double xPositionNext = tNext * spineChordLength;

      double widthAtT = getWidthAtParameter(t, spineRadius, bladeRadius, spineTheta, bladeTheta);
      double widthAtTplus1 = getWidthAtParameter(tNext, spineRadius, bladeRadius, spineTheta, bladeTheta);

      double segmentArea = 0.5 * (widthAtT + widthAtTplus1) * stepSize;
      double segmentCentroidX = 0.5 * (xPosition + xPositionNext);

      firstMoment += segmentArea * segmentCentroidX;
    }

    double bevelFactor = 1.0;
    if (primaryBevel != null) {
      double p = Math.max(0.0, Math.min(1.0, primaryBevel.percentageOfBladeWidth()));
      double r = Math.max(1e-6, primaryBevel.curveFactor());
      double superCoeff = Blade.superEllipseCoefficient(r);
      bevelFactor = (1.0 - p) + p * superCoeff;
    }

    firstMoment *= bevelFactor;

    return firstMoment / (totalVolume / spineThickness);
  }

  /**
   * Calculates the radius of a circle given chord length and sagitta.
   * @param chordLength The length of the chord.
   * @param sagitta The height from the chord to the arc.
   * @return The radius of the circle.
   */
  private double getCircleRadius(double chordLength, double sagitta) {
    if (chordLength <= 0 || sagitta <= 0) return 0;
    return (chordLength * chordLength) / (8 * sagitta) + sagitta / 2;
  }

  /**
   * Calculates the width between spine and blade at a given parameter t (0 to 1).
   * Assumes the spine arc sits above the blade arc (closer to the observer looking at the concave side),
   * so width is computed as (spineY - bladeY) + interpolated chord offset. If parameters violate this
   * assumption (producing a negative width), the result is clamped to 0.
   *
   * @param t Parameter from 0 (base) to 1 (tip)
   * @param spineRadius Radius of the spine arc
   * @param bladeRadius Radius of the blade arc
   * @param spineTheta Total angle subtended by spine arc
   * @param bladeTheta Total angle subtended by blade arc
   * @return The perpendicular distance between spine and blade curves at parameter t
   */
  private double getWidthAtParameter(double t, double spineRadius, double bladeRadius,
                                     double spineTheta, double bladeTheta) {
    double spineAngle = spineTheta * t - spineTheta / 2.0;
    double bladeAngle = bladeTheta * t - bladeTheta / 2.0;

    double spineLocalSagitta = spineRadius - spineRadius * Math.cos(Math.abs(spineAngle));
    double bladeLocalSagitta = bladeRadius - bladeRadius * Math.cos(Math.abs(bladeAngle));

    double spineY = spineSagittaHeight - spineLocalSagitta; // expected >= bladeY
    double bladeY = bladeSagittaHeight - bladeLocalSagitta;

    double offsetDistance = spineBaseToBladeBaseDistance + (spineTipToBladeTipDistance - spineBaseToBladeBaseDistance) * t;

    double width = (spineY - bladeY) + offsetDistance;
    if (width < 0.0) return 0.0; // guard against inverted inputs
    return width;
  }


  public double spineArcLength() { return spineArcLength; }
  public double spineChordLength() { return spineChordLength; }
  public double spineSagittaHeight() { return spineSagittaHeight; }
  public double bladeArcLength() { return bladeArcLength; }
  public double bladeChordLength() { return bladeChordLength; }
  public double bladeSagittaHeight() { return bladeSagittaHeight; }
  public double spineBaseToBladeBaseDistance() { return spineBaseToBladeBaseDistance; }
  public double spineTipToBladeTipDistance() { return spineTipToBladeTipDistance; }
  public Blade.EdgeBevel edgeBevel() { return edgeBevel; }
  public Blade.Bevel primaryBevel() { return primaryBevel; }

}
