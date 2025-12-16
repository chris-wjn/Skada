package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.WeaponProfile;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.StrikeCapable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Class that represents a shovel head part of a weapon.
 * A shovel head consists of a blade (the flat digging surface) and a socket (where it connects to the handle).
 */
public class ShovelHead extends WeaponHead implements StrikeCapable {

  public static final Codec<ShovelHead> CODEC = RecordCodecBuilder.create((RecordCodecBuilder.Instance<ShovelHead> instance) ->
          instance.group(
                  Codec.DOUBLE.fieldOf("bladeLength").forGetter(s -> s.bladeLength),
                  Codec.DOUBLE.fieldOf("bladeWidth").forGetter(s -> s.bladeWidth),
                  Codec.DOUBLE.fieldOf("bladeThickness").forGetter(s -> s.bladeThickness),
                  Codec.DOUBLE.fieldOf("socketLength").forGetter(s -> s.socketLength),
                  Codec.DOUBLE.fieldOf("socketOuterRadius").forGetter(s -> s.socketOuterRadius),
                  Codec.DOUBLE.fieldOf("socketInnerRadius").forGetter(s -> s.socketInnerRadius),
                  Codec.DOUBLE.fieldOf("tipAngle").forGetter(s -> s.tipAngle)
          ).apply(instance, ShovelHead::new)
  );

  @Override
  public Codec<? extends WeaponHead> type() {
    return CODEC;
  }

  @Override
  public String typeKey() { return "shovel"; }

  private final double bladeLength; // Length of the shovel blade in mm
  private final double bladeWidth; // Width of the shovel blade in mm
  private final double bladeThickness; // Thickness of the shovel blade in mm
  private final double socketLength; // Length of the socket that connects to handle in mm
  private final double socketOuterRadius; // Outer radius of the socket in mm
  private final double socketInnerRadius; // Inner radius of the socket (hollow tube) in mm
  private final double tipAngle; // Angle of the tip (0 = flat, 45 = pointed) in degrees

  /**
   * Constructor for ShovelHead.
   * @param bladeLength Length of the shovel blade
   * @param bladeWidth Width of the shovel blade
   * @param bladeThickness Thickness of the shovel blade
   * @param socketLength Length of the socket
   * @param socketOuterRadius Outer radius of the socket
   * @param socketInnerRadius Inner radius of the socket
   * @param tipAngle Angle of the tip in degrees
   */
  public ShovelHead(double bladeLength, double bladeWidth, double bladeThickness,
                    double socketLength, double socketOuterRadius, double socketInnerRadius,
                    double tipAngle) {
    this.bladeLength = bladeLength;
    this.bladeWidth = bladeWidth;
    this.bladeThickness = bladeThickness;
    this.socketLength = socketLength;
    this.socketOuterRadius = socketOuterRadius;
    this.socketInnerRadius = socketInnerRadius;
    this.tipAngle = tipAngle;
  }

  @Override
  public double getPrimaryAxisLength() {
    return Math.max(0.0, socketLength) + Math.max(0.0, bladeLength);
  }

  @Override
  public double getSecondaryAxisLength() {
    return Math.max(0.0, bladeWidth);
  }

  @Override
  public double getMomentOfInertia(double distanceFromPivot, double density, WeaponProfile.HeadOrientation orientation) {
    // Convert density from g/cm³ to g/mm³ for calculations
    double densityPerMM3 = density / 1000.0;

    // Calculate moment of inertia about the pivot point.
    // Shovel head is inline with the handle.
    // Pivot is at distance 'distanceFromPivot' from the base of the socket.
    // I = I_socket + I_blade
    // Axis of rotation is transverse (perpendicular to handle and blade width).
    // Parallel axis theorem: I_part = I_part_cm + M_part * (dist_to_cm)^2

    double totalInertia = 0.0;

    // 1. Socket (Hollow Cylinder)
    // Length: socketLength, Outer R: socketOuterRadius, Inner R: socketInnerRadius
    // CM is at socketLength / 2
    double sLen = Math.max(0.0, socketLength);
    double sRo = Math.max(0.0, socketOuterRadius);
    double sRi = Math.max(0.0, socketInnerRadius);
    double sVol = Math.PI * (sRo*sRo - sRi*sRi) * sLen;
    double sMass = sVol * densityPerMM3;

    if (sVol > 0) {
        // I_cm for hollow cylinder about transverse axis:
        // I = 1/12 * m * (3*(Ro^2 + Ri^2) + h^2)
        double sIcm = (1.0/12.0) * sMass * (3.0*(sRo*sRo + sRi*sRi) + sLen*sLen);
        double sDist = distanceFromPivot + sLen / 2.0;
        totalInertia += sIcm + sMass * sDist * sDist;
    }

    // 2. Blade (Rectangular Plate with Taper)
    // Base of blade starts at socketLength.
    // Dimensions: bladeLength (L), bladeWidth (W), bladeThickness (T).
    double bLen = Math.max(0.0, bladeLength);
    double bW = Math.max(0.0, bladeWidth);
    double bT = Math.max(0.0, bladeThickness);

    if (bLen > 0 && bW > 0 && bT > 0) {
        double tipCutoff = 0.0;
        if (tipAngle > 0) {
            tipCutoff = bW * Math.tan(Math.toRadians(tipAngle));
        }
        // Effective length approximation for volume/mass
        double effLen = Math.max(0.0, bLen - tipCutoff);

        // If we assume the blade is a rectangle of length effLen:
        double bVol = bW * bT * effLen;
        double bMass = bVol * densityPerMM3;

        if (bVol > 0) {
            // I_cm for rectangular plate about transverse axis (parallel to width).
            // I = 1/12 * m * (L^2 + T^2)
            double bIcm = (1.0/12.0) * bMass * (effLen*effLen + bT*bT);

            // CM is at socketLength + effLen / 2.0
            double bDist = distanceFromPivot + sLen + effLen / 2.0;
            totalInertia += bIcm + bMass * bDist * bDist;
        }
    }

    return totalInertia;
  }

  /**
   * Default constructor with typical shovel dimensions.
   */
  public ShovelHead() {
    this(200, 150, 2, 100, 20, 16, 15);
  }

  @Override
  public double getVolume() {
    // Calculate blade volume
    double bladeVolume;
    if (tipAngle > 0) {
      // If there's a tip angle, we lose some volume at the tip
      double tipCutoff = bladeWidth * Math.tan(Math.toRadians(tipAngle));
      // Approximate as trapezoid: full width at base, tapered at tip
      double effectiveLength = bladeLength - tipCutoff;
      bladeVolume = bladeWidth * bladeThickness * effectiveLength;
    } else {
      // Flat tip, simple rectangular blade
      bladeVolume = bladeLength * bladeWidth * bladeThickness;
    }

    // Calculate socket volume (hollow cylinder)
    double socketOuterVolume = Math.PI * socketOuterRadius * socketOuterRadius * socketLength;
    double socketInnerVolume = Math.PI * socketInnerRadius * socketInnerRadius * socketLength;
    double socketVolume = socketOuterVolume - socketInnerVolume;

    return bladeVolume + socketVolume;
  }

  @Override
  public double getPointOfBalance() {
    // Calculate individual volumes
    double bladeVolume;
    double bladeCoM;

    if (tipAngle > 0) {
      double tipCutoff = bladeWidth * Math.tan(Math.toRadians(tipAngle));
      double effectiveLength = bladeLength - tipCutoff;
      bladeVolume = bladeWidth * bladeThickness * effectiveLength;
      // For tapered shape, CoM is slightly closer to base
      bladeCoM = socketLength + (effectiveLength * 0.45);
    } else {
      bladeVolume = bladeLength * bladeWidth * bladeThickness;
      bladeCoM = socketLength + (bladeLength / 2.0);
    }

    double socketOuterVolume = Math.PI * socketOuterRadius * socketOuterRadius * socketLength;
    double socketInnerVolume = Math.PI * socketInnerRadius * socketInnerRadius * socketLength;
    double socketVolume = socketOuterVolume - socketInnerVolume;
    double socketCoM = socketLength / 2.0;

    double totalVolume = bladeVolume + socketVolume;
    if (totalVolume < 1e-6) {
      return socketLength + bladeLength / 2.0;
    }

    // Weighted average of centers of mass
    return (bladeVolume * bladeCoM + socketVolume * socketCoM) / totalVolume;
  }

  // Getters
  public double getBladeLength() {
    return bladeLength;
  }

  public double getBladeWidth() {
    return bladeWidth;
  }

  public double getBladeThickness() {
    return bladeThickness;
  }

  public double getSocketLength() {
    return socketLength;
  }

  public double getSocketOuterRadius() {
    return socketOuterRadius;
  }

  public double getSocketInnerRadius() {
    return socketInnerRadius;
  }

  public double getTipAngle() {
    return tipAngle;
  }

}
