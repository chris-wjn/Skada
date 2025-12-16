package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.WeaponProfile;
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
  public double getPrimaryAxisLength() {
    return Math.max(0.0, socketLength) + Math.max(0.0, headLength);
  }

  @Override
  public double getSecondaryAxisLength() {
    return 2.0 * Math.max(0.0, headRadius);
  }

  @Override
  public double getMomentOfInertia(double distanceFromPivot, double density, WeaponProfile.HeadOrientation orientation) {
    // Convert density from g/cm³ to g/mm³ for calculations
    double densityPerMM3 = density / 1000.0;

    // Calculate moment of inertia about the pivot point.
    // Mace head is inline with the handle.
    // Pivot is at distance 'distanceFromPivot' from the base of the socket.
    // I = I_socket + I_head + I_flanges
    // Axis of rotation is transverse (perpendicular to handle).
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

    // 2. Head
    // Base of head starts at socketLength.
    // CM position depends on shape.
    double hLen = Math.max(0.0, headLength);
    double hRad = Math.max(0.0, headRadius);
    double hVol = 0.0;
    double hIcm = 0.0;
    double hCmOffset = 0.0; // Distance from base of head (socketLength) to CM

    switch (shape) {
      case SPHERE:
        // Sphere or Spherical Cap
        if (hLen >= 2 * hRad) {
          // Full sphere
          hVol = (4.0 / 3.0) * Math.PI * Math.pow(hRad, 3);
          double hMass = hVol * densityPerMM3;
          hIcm = (2.0 / 5.0) * hMass * hRad * hRad;
          hCmOffset = hRad;
        } else {
          // Spherical cap
          double h = hLen;
          hVol = Math.PI * h * h * (3 * hRad - h) / 3.0;
          double hMass = hVol * densityPerMM3;
          // CM from base of cap (flat side):
          // z_bar = h * (4r - h) / (4 * (3r - h))
          hCmOffset = h * (4*hRad - h) / (4.0 * (3*hRad - h));

          // Moment of inertia of spherical cap approx as sphere scaled by mass ratio?
          // Or point mass + I_sphere approx?
          // Let's use I_sphere_cm = 2/5 M r^2.
          hIcm = (2.0 / 5.0) * hMass * hRad * hRad;
        }
        break;
      case CONE:
        // Cone
        hVol = (Math.PI * hRad * hRad * hLen) / 3.0;
        double hMassCone = hVol * densityPerMM3;
        hCmOffset = hLen / 4.0;
        // I_cm for cone about transverse axis through CM:
        // I_cm = 3/80 * m * (4r^2 + h^2)
        hIcm = (3.0 / 80.0) * hMassCone * (4.0*hRad*hRad + hLen*hLen);
        break;
      case OCTAGONAL:
        // Octagonal prism
        // Approx as cylinder with effective radius
        double sideLength = hRad * 0.765;
        hVol = 2 * (1 + Math.sqrt(2)) * sideLength * sideLength * hLen;
        double hMassOct = hVol * densityPerMM3;
        hCmOffset = hLen / 2.0;
        // Equivalent radius for cylinder inertia: R_eq^2 = Area / PI
        double rEqSq = hVol / (Math.PI * hLen);
        hIcm = (1.0/12.0) * hMassOct * (3.0*rEqSq + hLen*hLen);
        break;
      case CYLINDER:
      default:
        // Cylinder
        hVol = Math.PI * hRad * hRad * hLen;
        double hMassCyl = hVol * densityPerMM3;
        hCmOffset = hLen / 2.0;
        hIcm = (1.0/12.0) * hMassCyl * (3.0*hRad*hRad + hLen*hLen);
        break;
    }

    if (hVol > 0) {
        double hMass = hVol * densityPerMM3;
        double hDist = distanceFromPivot + sLen + hCmOffset;
        totalInertia += hIcm + hMass * hDist * hDist;
    }

    // 3. Flanges
    // Number: flangeCount
    // Dimensions: flangeHeight (radial extent), flangeThickness (width), headLength (length)
    if (flangeCount > 0) {
        double fH = Math.max(0.0, flangeHeight);
        double fT = Math.max(0.0, flangeThickness);
        double fVol = fH * fT * hLen; // One flange
        double totalFVol = fVol * flangeCount;
        double totalFMass = totalFVol * densityPerMM3;

        // Distance from pivot to flange CM (along axis)
        double fDistAxial = distanceFromPivot + sLen + hLen / 2.0;

        // Radial distance of flange CM
        double fRad = hRad + fH / 2.0;

        // Moment of inertia of flanges about pivot.
        // I = I_axial + I_transverse_shift
        // Let's treat the set of flanges as a cylindrical shell of effective radius fRad.
        // I_shell_cm = 1/2 M R^2 + 1/12 M L^2.
        double fIcm = (1.0/2.0) * totalFMass * fRad*fRad + (1.0/12.0) * totalFMass * hLen*hLen;

        totalInertia += fIcm + totalFMass * fDistAxial * fDistAxial;
    }

    return totalInertia;
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
