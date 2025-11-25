package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.parts.attack_types.StrikeCapable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Class that represents a mace head part of a weapon.
 * A mace head is typically a heavy mass (sphere, cylinder, or flanged) on a socket.
 */
public class MaceHead extends WeaponHead implements StrikeCapable {

  public static final Codec<MaceHead> CODEC = RecordCodecBuilder.create((RecordCodecBuilder.Instance<MaceHead> instance) ->
          instance.group(
                  Codec.STRING.xmap(HeadShape::valueOf, HeadShape::name).fieldOf("shape").forGetter(MaceHead::getShape),
                  Codec.DOUBLE.fieldOf("headLength").forGetter(MaceHead::getHeadLength),
                  Codec.DOUBLE.fieldOf("headRadius").forGetter(MaceHead::getHeadRadius),
                  Codec.INT.fieldOf("flangeCount").forGetter(MaceHead::getFlangeCount),
                  Codec.DOUBLE.fieldOf("flangeHeight").forGetter(MaceHead::getFlangeHeight),
                  Codec.DOUBLE.fieldOf("flangeThickness").forGetter(MaceHead::getFlangeThickness),
                  Codec.DOUBLE.fieldOf("socketLength").forGetter(MaceHead::getSocketLength),
                  Codec.DOUBLE.fieldOf("socketOuterRadius").forGetter(MaceHead::getSocketOuterRadius),
                  Codec.DOUBLE.fieldOf("socketInnerRadius").forGetter(MaceHead::getSocketInnerRadius)
          ).apply(instance, MaceHead::new)
  );

  @Override
  public Codec<? extends WeaponHead> type() {
    return CODEC;
  }

  @Override
  public String typeKey() { return "mace"; }

  private final HeadShape shape; // Shape of the mace head
  private final double headLength; // Length/height of the mace head in mm
  private final double headRadius; // Radius of the mace head in mm
  private final int flangeCount; // Number of flanges (0 for smooth mace)
  private final double flangeHeight; // Height of each flange in mm
  private final double flangeThickness; // Thickness of each flange in mm
  private final double socketLength; // Length of the socket that connects to handle in mm
  private final double socketOuterRadius; // Outer radius of the socket in mm
  private final double socketInnerRadius; // Inner radius of the socket (hollow tube) in mm

  /**
   * Enum for different mace head shapes.
   */
  public enum HeadShape {
    SPHERE,      // Spherical mace head
    CYLINDER,    // Cylindrical mace head
    CONE,        // Conical mace head (narrows to top)
    OCTAGONAL    // Octagonal prism
  }

  /**
   * Constructor for MaceHead.
   * @param shape Shape of the mace head
   * @param headLength Length/height of the head
   * @param headRadius Radius of the head
   * @param flangeCount Number of flanges (vertical ridges)
   * @param flangeHeight Height of each flange
   * @param flangeThickness Thickness of each flange
   * @param socketLength Length of the socket
   * @param socketOuterRadius Outer radius of the socket
   * @param socketInnerRadius Inner radius of the socket
   */
  public MaceHead(HeadShape shape, double headLength, double headRadius,
                  int flangeCount, double flangeHeight, double flangeThickness,
                  double socketLength, double socketOuterRadius, double socketInnerRadius) {
    this.shape = shape;
    this.headLength = headLength;
    this.headRadius = headRadius;
    this.flangeCount = Math.max(0, flangeCount);
    this.flangeHeight = flangeHeight;
    this.flangeThickness = flangeThickness;
    this.socketLength = socketLength;
    this.socketOuterRadius = socketOuterRadius;
    this.socketInnerRadius = socketInnerRadius;
  }

  @Override
  public double getLength() {
    // primary dimension is socketLength + headLength
    return Math.max(0.0, socketLength) + Math.max(0.0, headLength);
  }

  /**
   * Default constructor with typical flanged mace dimensions.
   */
  public MaceHead() {
    this(HeadShape.CYLINDER, 80, 40, 6, 15, 5, 60, 20, 16);
  }

  @Override
  public double getVolume() {
    // Calculate base head volume based on shape
    double headVolume;
    switch (shape) {
      case SPHERE:
        // Sphere volume: (4/3) * π * r³
        // But limited by headLength, so we use a spherical cap if needed
        if (headLength >= 2 * headRadius) {
          headVolume = (4.0 / 3.0) * Math.PI * Math.pow(headRadius, 3);
        } else {
          // Spherical cap
          double h = headLength;
          headVolume = Math.PI * h * h * (3 * headRadius - h) / 3.0;
        }
        break;
      case CYLINDER:
        // Cylinder volume: π * r² * h
        headVolume = Math.PI * headRadius * headRadius * headLength;
        break;
      case CONE:
        // Cone volume: (1/3) * π * r² * h
        headVolume = (Math.PI * headRadius * headRadius * headLength) / 3.0;
        break;
      case OCTAGONAL:
        // Octagonal prism: 2 * (1 + √2) * a² * h, where a is the side length
        // For inscribed octagon in circle: a ≈ r * 0.765
        double sideLength = headRadius * 0.765;
        headVolume = 2 * (1 + Math.sqrt(2)) * sideLength * sideLength * headLength;
        break;
      default:
        headVolume = Math.PI * headRadius * headRadius * headLength;
    }

    // Add flange volume if present
    double flangeVolume = 0;
    if (flangeCount > 0) {
      // Each flange is approximated as a rectangular prism
      double singleFlangeVolume = flangeHeight * flangeThickness * headLength;
      flangeVolume = singleFlangeVolume * flangeCount;
    }

    // Calculate socket volume (hollow cylinder)
    double socketOuterVolume = Math.PI * socketOuterRadius * socketOuterRadius * socketLength;
    double socketInnerVolume = Math.PI * socketInnerRadius * socketInnerRadius * socketLength;
    double socketVolume = socketOuterVolume - socketInnerVolume;

    return headVolume + flangeVolume + socketVolume;
  }

  @Override
  public double getPointOfBalance() {
    // Calculate individual volumes and their centers of mass
    double headVolume;
    double headCoM;

    switch (shape) {
      case SPHERE:
        if (headLength >= 2 * headRadius) {
          headVolume = (4.0 / 3.0) * Math.PI * Math.pow(headRadius, 3);
          headCoM = socketLength + headRadius;
        } else {
          double h = headLength;
          headVolume = Math.PI * h * h * (3 * headRadius - h) / 3.0;
          // Spherical cap CoM is at (3(2r-h)²) / (4(3r-h)) from base
          headCoM = socketLength + (3 * Math.pow(2 * headRadius - h, 2)) / (4 * (3 * headRadius - h));
        }
        break;
      case CONE:
        headVolume = (Math.PI * headRadius * headRadius * headLength) / 3.0;
        // Cone CoM is at 1/4 height from base
        headCoM = socketLength + (headLength * 0.25);
        break;
      case CYLINDER:
      case OCTAGONAL:
      default:
        // For cylinder and octagonal, CoM is at half height
        if (shape == HeadShape.OCTAGONAL) {
          double sideLength = headRadius * 0.765;
          headVolume = 2 * (1 + Math.sqrt(2)) * sideLength * sideLength * headLength;
        } else {
          headVolume = Math.PI * headRadius * headRadius * headLength;
        }
        headCoM = socketLength + (headLength / 2.0);
    }

    // Flange contribution (if present)
    double flangeVolume = 0;
    double flangeCoM = 0;
    if (flangeCount > 0) {
      double singleFlangeVolume = flangeHeight * flangeThickness * headLength;
      flangeVolume = singleFlangeVolume * flangeCount;
      flangeCoM = socketLength + (headLength / 2.0); // Flanges run along the length
    }

    // Socket contribution
    double socketOuterVolume = Math.PI * socketOuterRadius * socketOuterRadius * socketLength;
    double socketInnerVolume = Math.PI * socketInnerRadius * socketInnerRadius * socketLength;
    double socketVolume = socketOuterVolume - socketInnerVolume;
    double socketCoM = socketLength / 2.0;

    double totalVolume = headVolume + flangeVolume + socketVolume;
    if (totalVolume < 1e-6) {
      return socketLength + headLength / 2.0;
    }

    // Weighted average of centers of mass
    double weightedSum = headVolume * headCoM + flangeVolume * flangeCoM + socketVolume * socketCoM;
    return weightedSum / totalVolume;
  }

  // Getters
  public HeadShape getShape() {
    return shape;
  }

  public double getHeadLength() {
    return headLength;
  }

  public double getHeadRadius() {
    return headRadius;
  }

  public int getFlangeCount() {
    return flangeCount;
  }

  public double getFlangeHeight() {
    return flangeHeight;
  }

  public double getFlangeThickness() {
    return flangeThickness;
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

}
