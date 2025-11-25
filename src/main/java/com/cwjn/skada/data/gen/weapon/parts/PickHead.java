package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.SkadaData;
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
  public Blade.TipSpecifications tipSpecs() {
    return new Blade.TipSpecifications(
            10,
            SkadaData.EDGE_ANGLE_DEFAULT,
            180
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
    
    return scaled;
  }

  @Override
  public double getThrustNormalizedIdealPointOfBalance() {
    // For a pick, ideal thrust PoB is near the eye where the handle connects
    // to provide maximum penetration force. Return 0.0 (at the base/eye).
    return 0.0;
  }

  @Override
  public double getLength() {
    // primary length is eyeLength + front spike length (rear spike goes backward)
    return Math.max(0.0, eyeLength) + Math.max(0.0, frontSpikeLength) + Math.max(0.0, rearSpikeLength);
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
