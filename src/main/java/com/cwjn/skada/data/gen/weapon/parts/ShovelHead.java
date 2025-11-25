package com.cwjn.skada.data.gen.weapon.parts;

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
  public double getLength() {
    // primary length is socket length + blade length
    return Math.max(0.0, socketLength) + Math.max(0.0, bladeLength);
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
