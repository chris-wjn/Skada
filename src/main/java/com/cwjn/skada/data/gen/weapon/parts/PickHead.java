package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.weapon.WeaponProfile;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.ThrustCapable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Class that represents a pick head part of a weapon.
 * A pick head typically has a pointed spike on one or both sides and a socket/eye for the handle.
 */
public class PickHead extends WeaponHead implements ThrustCapable {

  public static final Codec<PickHead> CODEC = RecordCodecBuilder.create((RecordCodecBuilder.Instance<PickHead> instance) ->
          instance.group(
                  Codec.DOUBLE.fieldOf("eyeLength").forGetter(p -> p.eyeLength),
                  Codec.DOUBLE.fieldOf("eyeHeight").forGetter(p -> p.eyeHeight),
                  Codec.DOUBLE.fieldOf("eyeThickness").forGetter(p -> p.eyeThickness),
                  Codec.DOUBLE.fieldOf("eyeHoleSemiMajorAxis").forGetter(p -> p.eyeHoleSemiMajorAxis),
                  Codec.DOUBLE.fieldOf("eyeHoleSemiMinorAxis").forGetter(p -> p.eyeHoleSemiMinorAxis),
                  Codec.DOUBLE.fieldOf("frontSpikeLength").forGetter(p -> p.frontSpikeLength),
                  Codec.DOUBLE.fieldOf("frontSpikeBaseWidth").forGetter(p -> p.frontSpikeBaseWidth),
                  Codec.DOUBLE.fieldOf("frontSpikeBaseHeight").forGetter(p -> p.frontSpikeBaseHeight),
                  Codec.DOUBLE.fieldOf("rearSpikeLength").forGetter(p -> p.rearSpikeLength),
                  Codec.DOUBLE.fieldOf("rearSpikeBaseWidth").forGetter(p -> p.rearSpikeBaseWidth),
                  Codec.DOUBLE.fieldOf("rearSpikeBaseHeight").forGetter(p -> p.rearSpikeBaseHeight)
          ).apply(instance, PickHead::new)
  );

  @Override
  public Codec<? extends WeaponHead> type() {
    return CODEC;
  }

  @Override
  public String typeKey() { return "pick"; }

  private final double eyeLength; // Length of the eye (socket) where handle passes through in mm
  private final double eyeHeight; // Height of the eye in mm
  private final double eyeThickness; // Thickness of the eye in mm
  private final double eyeHoleSemiMajorAxis; // Semi-major axis of elliptical eye hole in mm
  private final double eyeHoleSemiMinorAxis; // Semi-minor axis of elliptical eye hole in mm
  private final double frontSpikeLength; // Length of the front spike in mm
  private final double frontSpikeBaseWidth; // Width of the spike at base in mm
  private final double frontSpikeBaseHeight; // Height of the spike at base in mm
  private final double rearSpikeLength; // Length of the rear spike (0 if single-sided) in mm
  private final double rearSpikeBaseWidth; // Width of the rear spike at base in mm
  private final double rearSpikeBaseHeight; // Height of the rear spike at base in mm

  /**
   * Constructor for a pick head.
   * @param eyeLength Length of the eye
   * @param eyeHeight Height of the eye
   * @param eyeThickness Thickness of the eye
   * @param eyeHoleSemiMajorAxis Semi-major axis of the eye hole
   * @param eyeHoleSemiMinorAxis Semi-minor axis of the eye hole
   * @param frontSpikeLength Length of the front spike
   * @param frontSpikeBaseWidth Width of the front spike base
   * @param frontSpikeBaseHeight Height of the front spike base
   * @param rearSpikeLength Length of the rear spike (0 for single-sided)
   * @param rearSpikeBaseWidth Width of the rear spike base
   * @param rearSpikeBaseHeight Height of the rear spike base
   */
  public PickHead(double eyeLength, double eyeHeight, double eyeThickness,
                  double eyeHoleSemiMajorAxis, double eyeHoleSemiMinorAxis,
                  double frontSpikeLength, double frontSpikeBaseWidth, double frontSpikeBaseHeight,
                  double rearSpikeLength, double rearSpikeBaseWidth, double rearSpikeBaseHeight) {
    this.eyeLength = eyeLength;
    this.eyeHeight = eyeHeight;
    this.eyeThickness = eyeThickness;
    this.eyeHoleSemiMajorAxis = eyeHoleSemiMajorAxis;
    this.eyeHoleSemiMinorAxis = eyeHoleSemiMinorAxis;
    this.frontSpikeLength = frontSpikeLength;
    this.frontSpikeBaseWidth = frontSpikeBaseWidth;
    this.frontSpikeBaseHeight = frontSpikeBaseHeight;
    this.rearSpikeLength = rearSpikeLength;
    this.rearSpikeBaseWidth = rearSpikeBaseWidth;
    this.rearSpikeBaseHeight = rearSpikeBaseHeight;
  }

  @Override
  public double getPrimaryAxisLength() {
    return Math.max(0.0, eyeLength) + Math.max(0.0, frontSpikeLength) + Math.max(0.0, rearSpikeLength);
  }

  @Override
  public double getSecondaryAxisLength() {
    return Math.max(0.0, eyeHeight);
  }

  @Override
  public double getMomentOfInertia(double distanceFromPivot, double density, WeaponProfile.HeadOrientation orientation) {
    // Convert density from g/cm³ to g/mm³ for calculations
    double densityPerMM3 = density / 1000.0;

    // Calculate moment of inertia about the pivot point.
    // Pick head is perpendicular to the handle.
    // Pivot is at distance 'distanceFromPivot' from the center of the eye.
    // We assume the handle is the Y-axis, and the pick head extends in the X-direction.
    // Rotation axis is Z-axis (perpendicular to handle and pick length).
    // Pivot at (0, -distanceFromPivot) relative to eye center (0,0).
    // Eye center is at (0, distanceFromPivot) relative to pivot.

    double totalInertia = 0.0;

    // 1. Eye (Box with hole)
    // Dimensions: eyeLength (x), eyeHeight (y), eyeThickness (z)
    // Center at (0, distanceFromPivot).
    double eLen = Math.max(0.0, eyeLength);
    double eH = Math.max(0.0, eyeHeight);
    double eThick = Math.max(0.0, eyeThickness);
    double eVol = eLen * eH * eThick;
    double eMass = eVol * densityPerMM3;

    // I_cm_z for solid box = 1/12 * M * (L^2 + H^2)
    double iEyeSolidCm = (1.0/12.0) * eMass * (eLen*eLen + eH*eH);
    double eyeDistSq = distanceFromPivot * distanceFromPivot;
    totalInertia += iEyeSolidCm + eMass * eyeDistSq;

    // Subtract hole
    double hMaj = Math.max(0.0, eyeHoleSemiMajorAxis);
    double hMin = Math.max(0.0, eyeHoleSemiMinorAxis);
    if (hMaj > 0 && hMin > 0) {
        double hVol = Math.PI * hMaj * hMin * eThick;
        double hMass = hVol * densityPerMM3;
        // Hole is elliptical cylinder along Y axis.
        // I_z = M * (a^2/4 + H^2/12) where a is semi-axis in X.
        // Here a = hMaj (if aligned with X).
        double iHoleCm = hMass * (hMaj*hMaj/4.0 + eH*eH/12.0);
        totalInertia -= (iHoleCm + hMass * eyeDistSq);
    }

    // 2. Front Spike (Pyramid)
    // Length: frontSpikeLength (L_f)
    // Base: frontSpikeBaseWidth (W_f), frontSpikeBaseHeight (H_f)
    // Base centered at x = eLen/2, y = distanceFromPivot.
    // Extends to x = eLen/2 + L_f.
    double fL = Math.max(0.0, frontSpikeLength);
    double fW = Math.max(0.0, frontSpikeBaseWidth);
    double fH_spike = Math.max(0.0, frontSpikeBaseHeight);

    if (fL > 0) {
        double fVol = (fW * fH_spike * fL) / 3.0;
        double fMass = fVol * densityPerMM3;
        double fCx = eLen / 2.0 + fL / 4.0; // Centroid of pyramid is 1/4 from base
        double fCy = distanceFromPivot;

        // I_cm for pyramid about z-axis (perpendicular to length and height).
        // Approx: I_cm = M * (3*L^2/80 + W^2/20)
        // Here W is width in Y direction (fH_spike? No, fW is width, fH_spike is height).
        // Wait, fW is "base width", fH_spike is "base height".
        // Usually width is X, height is Y, thickness is Z?
        // Pick head extends in X.
        // So base is in YZ plane? No, base is attached to eye side (YZ plane).
        // So base dimensions are Y and Z.
        // fW is width (Y?), fH_spike is height (Z?).
        // Or fW is thickness (Z)?
        // "frontSpikeBaseWidth" usually means dimension in the plane of the pick curve?
        // Let's assume fW is along Y (width of pick head), fH_spike is along Z (thickness).
        // Rotation is about Z axis.
        // So we need I_z.
        // I_z depends on X and Y dimensions.
        // Length L is X. Width W is Y.
        // I_cm = M * (3*L^2/80 + W^2/20).
        double fIcm = fMass * (3.0*fL*fL/80.0 + fW*fW/20.0);

        double fDistSq = fCx*fCx + fCy*fCy;
        totalInertia += fIcm + fMass * fDistSq;
    }

    // 3. Rear Spike (Pyramid)
    // Extends from x = -eLen/2 to x = -eLen/2 - L_r.
    double rL = Math.max(0.0, rearSpikeLength);
    double rW = Math.max(0.0, rearSpikeBaseWidth);
    double rH_spike = Math.max(0.0, rearSpikeBaseHeight);

    if (rL > 0) {
        double rVol = (rW * rH_spike * rL) / 3.0;
        double rMass = rVol * densityPerMM3;
        double rCx = -eLen / 2.0 - rL / 4.0;
        double rCy = distanceFromPivot;

        double rIcm = rMass * (3.0*rL*rL/80.0 + rW*rW/20.0);

        double rDistSq = rCx*rCx + rCy*rCy;
        totalInertia += rIcm + rMass * rDistSq;
    }

    return totalInertia;
  }

  @Override
  public Blade.TipSpecifications tipSpecs() {
    return new Blade.TipSpecifications(
            10,
            SkadaData.EDGE_ANGLE_DEFAULT,
            180,
            0.5
    );
  }

  @Override
  public double getTaperValue() {
    if (frontSpikeLength <= 0.0) return 10.0;
    
    double baseAvg = (frontSpikeBaseWidth + frontSpikeBaseHeight) / 2.0;
    if (baseAvg <= 0.0) return 10.0;

    double ratio = frontSpikeLength / baseAvg;

    double scaled = 10.0 + ratio * 6.0;

    if (scaled < 10.0) scaled = 10.0;
    if (scaled > 90.0) scaled = 90.0;

    if (Double.isNaN(scaled) || Double.isInfinite(scaled)) {
      return 10.0;
    }

    return scaled;
  }

  @Override
  public double getThrustNormalizedIdealPointOfBalance() {
    // For a pick, ideal thrust PoB is near the eye where the handle connects
    // to provide maximum penetration force. Return 0.0 (at the base/eye).
    return 0.0;
  }


  /**
   * Default constructor with typical pickaxe dimensions.
   */
  public PickHead() {
    this(50, 40, 30, 18, 12, 180, 25, 25, 80, 20, 20);
  }

  @Override
  public double getVolume() {
    // Eye volume (solid rectangle minus elliptical hole)
    double eyeSolidVolume = eyeLength * eyeHeight * eyeThickness;
    double eyeHoleVolume = Math.PI * eyeHoleSemiMajorAxis * eyeHoleSemiMinorAxis * eyeThickness;
    double eyeVolume = Math.max(0, eyeSolidVolume - eyeHoleVolume);

    // Front spike volume (pyramid approximation)
    double frontSpikeVolume = (frontSpikeBaseWidth * frontSpikeBaseHeight * frontSpikeLength) / 3.0;

    // Rear spike volume (pyramid approximation)
    double rearSpikeVolume = 0;
    if (rearSpikeLength > 0) {
      rearSpikeVolume = (rearSpikeBaseWidth * rearSpikeBaseHeight * rearSpikeLength) / 3.0;
    }

    return eyeVolume + frontSpikeVolume + rearSpikeVolume;
  }

  @Override
  public double getPointOfBalance() {
    // Calculate individual volumes
    double eyeSolidVolume = eyeLength * eyeHeight * eyeThickness;
    double eyeHoleVolume = Math.PI * eyeHoleSemiMajorAxis * eyeHoleSemiMinorAxis * eyeThickness;
    double eyeVolume = Math.max(0, eyeSolidVolume - eyeHoleVolume);
    double eyeCoM = 0; // Eye is at the reference point

    double frontSpikeVolume = (frontSpikeBaseWidth * frontSpikeBaseHeight * frontSpikeLength) / 3.0;
    // For a pyramid, CoM is at 1/4 of the length from the base
    double frontSpikeCoM = frontSpikeLength * 0.25;

    double rearSpikeVolume = 0;
    double rearSpikeCoM = 0;
    if (rearSpikeLength > 0) {
      rearSpikeVolume = (rearSpikeBaseWidth * rearSpikeBaseHeight * rearSpikeLength) / 3.0;
      // Rear spike extends in negative direction
      rearSpikeCoM = -(rearSpikeLength * 0.25);
    }

    double totalVolume = eyeVolume + frontSpikeVolume + rearSpikeVolume;
    if (totalVolume < 1e-6) {
      return frontSpikeLength / 2.0;
    }

    // Weighted average of centers of mass
    double weightedSum = eyeVolume * eyeCoM + frontSpikeVolume * frontSpikeCoM + rearSpikeVolume * rearSpikeCoM;
    return weightedSum / totalVolume;
  }

  // Getters
  public double getEyeLength() {
    return eyeLength;
  }

  public double getEyeHeight() {
    return eyeHeight;
  }

  public double getEyeThickness() {
    return eyeThickness;
  }

  public double getEyeHoleSemiMajorAxis() {
    return eyeHoleSemiMajorAxis;
  }

  public double getEyeHoleSemiMinorAxis() {
    return eyeHoleSemiMinorAxis;
  }

  public double getFrontSpikeLength() {
    return frontSpikeLength;
  }

  public double getFrontSpikeBaseWidth() {
    return frontSpikeBaseWidth;
  }

  public double getFrontSpikeBaseHeight() {
    return frontSpikeBaseHeight;
  }

  public double getRearSpikeLength() {
    return rearSpikeLength;
  }

  public double getRearSpikeBaseWidth() {
    return rearSpikeBaseWidth;
  }

  public double getRearSpikeBaseHeight() {
    return rearSpikeBaseHeight;
  }

}
