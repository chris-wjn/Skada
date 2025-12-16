package com.cwjn.skada.data.gen.weapon;

import com.cwjn.skada.data.gen.weapon.parts.attack_types.SlashCapable;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.ThrustCapable;

import static com.cwjn.skada.data.SkadaData.EDGE_ANGLE_DEFAULT;
import static com.cwjn.skada.data.SkadaData.MATERIAL_PROPERTY_SOFT_CAP;

public abstract class PrecisionGenerationUtil {

  public static double slash(WeaponProfile profile, ExtraTierInfo material) {
    WeaponProfile.WeaponHeadEntry head = profile.getSlashHead();
    if (head.getMaterial().isPresent()) material = head.getMaterial().get();
    SlashCapable slashHead = (SlashCapable) head.getHead(); //this is a bit dubious but it should be fine
    double edgeAngleNormalized = EDGE_ANGLE_DEFAULT / slashHead.edgeBevel().angle(); //acuter angle means better cutting
    double bladeWeight = profile.getVolume() * material.density(); //in grams

    double precision = edgeRadiusToPrecisionBase(slashHead.edgeBevel().edgeRadius()); //the base for precision comes from edge radius
    precision += bladeWeightToPrecisionBase2(bladeWeight); //the blade weight can help, even if the edge is blunt. Heavier blades have more momentum.
    precision *= edgeAngleNormalized; //the edge angle modifies precision

    double normalizedHardness = material.hardness()/MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedFlexibility = Math.abs(material.flexibility()-MATERIAL_PROPERTY_SOFT_CAP/2)/(MATERIAL_PROPERTY_SOFT_CAP/2);
    precision *= 1 + 0.05*normalizedHardness - 0.09*normalizedFlexibility;

    return precision;
  }

  public static double thrust(WeaponProfile profile, ExtraTierInfo tierInfo) {
    WeaponProfile.WeaponHeadEntry thrustHead = profile.getThrustHead();
    if (thrustHead.getMaterial().isPresent()) tierInfo = thrustHead.getMaterial().get();
    ThrustCapable head = (ThrustCapable) thrustHead.getHead();
    double tipBevelAngleNormalized = EDGE_ANGLE_DEFAULT / head.tipSpecs().tipBevelAngle();
    double pointOfBalanceNormalized = profile.getPointOfBalance(tierInfo) / profile.getTotalLength(); //percentage from 0-1, where 1 is furthest from pommel

    double precision = tipRadiusToPrecisionBase(head.tipSpecs().tipRadius()); //the base for precision comes from tip radius
    precision *= tipBevelAngleNormalized; //the tip angle modifies precision
    precision *= 0.5 + (pointOfBalanceNormalized); //the further forward the point of balance, the more precise the thrust

    double normalizedHardness = tierInfo.hardness()/MATERIAL_PROPERTY_SOFT_CAP;
    precision *= 1 + 0.07*normalizedHardness;

    precision -= 1.5*tierInfo.flexibility(); //more flexible blades are less precise when thrusting

    return precision;
  }

  public static double strike(WeaponProfile profile, ExtraTierInfo tierInfo) {
    return 25;
  }

  /**
   * Provides a precision value based on edge radius for slashing attacks.
   * Average precision for a sword is 20, for edge radius  5.
   * Precision decreases sharply as edge radius decreases below 5,
   * and increases sharply as edge radius increases above 5. Anything
   * above 10nm edge radius is effectively blunt.
   * @param edgeRadius The edge radius in nanometres.
   * @return A double representing precision.
   */
  private static double edgeRadiusToPrecisionBase(double edgeRadius) {
    if (edgeRadius < 7)
      return -Math.pow((edgeRadius - 5), 3) + 20;
    else
      return Math.exp(-edgeRadius/2 + 6);
  }

  /**
   * Provides a precision value based on tip radius for thrusting attacks.
   * Average precision for a sword is 20, for tip radius = 5.
   * Precision decreases sharply as tip radius decreases below 5,
   * and increases sharply as tip radius increases above 5. We're
   * more lenient on tip radius than edge radius, so the curve is
   * less steep, and high radii still get 1 precision.
   * @param tipRadius The tip radius in nanometres.
   * @return A double representing precision.
   */
  private static double tipRadiusToPrecisionBase(double tipRadius) {
    if (tipRadius < 7)
      return -Math.pow((tipRadius - 5), 3) + 20;
    else
      return Math.exp(-tipRadius / 60 + 2) + 1;
  }

  /**
   * Provides a precision value based on blade weight for slashing attacks.
   * This is considered less influential than edge radius, so the values will
   * be less from here. The blade weight for our supposed average is 1300g.
   * We'll set 1300g to provide 6 precision, where heavier blades give more
   * precision up to a soft cap of 10 precision, after which there are diminishing returns.
   * And, of course, anything lighter will give less than 6 by a soft curve, down to 1
   * precision at 100g.
   *
   * @param bladeWeight The blade weight in grams.
   * @return A double representing precision.
   */
  private static double bladeWeightToPrecisionBase2(double bladeWeight) {
    if (bladeWeight >= 1300) {
      return 6 + 4 * (1 - Math.exp(-0.0005 * (bladeWeight - 1300)));
    } else {
      return 1 + 5 * Math.exp(-0.002 * (1300 - bladeWeight));
    }
  }

}
