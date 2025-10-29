package com.cwjn.skada.data.gen;

import static com.cwjn.skada.data.SkadaData.*;
import static com.cwjn.skada.data.SkadaData.BEVEL_ANGLE_DEFAULT;
import static com.cwjn.skada.data.SkadaData.MATERIAL_PROPERTY_SOFT_CAP;
import static com.cwjn.skada.util.Util.*;

public abstract class LethalityGenerationUtil {

  /**
   * Calculates lethality for slashing attacks.
   * Average lethality for a sword should be around 50-70
   * @param profile The weapon profile of the weapon
   * @param tierInfo The weapon material info
   * @return a double representing lethality
   */
  public static double slashLethality(WeaponProfile profile, ExtraTierInfo tierInfo) {
    //First we'll normalize our values for the weapon profile. We take 1 to be the "default" value.
    double pointOfBalanceNormalized = profile.pointOfBalance() / (profile.bladeLength() + profile.handleLength()); //percentage from 0-1, where 1 is furthest from pommel
    double primaryEdgeBevelAngle = -180 + profile.edgeBevel().angle() + profile.edgeBevel().shoulderAngle(); //mathematically derived
    double primaryEdgeBevelAngleNormalized = BEVEL_ANGLE_DEFAULT / primaryEdgeBevelAngle; //acuter angle means more lethality cause better cutting

    //Some calculations about the balance point of the weapon, using material density and profile dimensions
    double bladeStartPercentage = profile.handleLength() / (profile.bladeLength() + profile.handleLength()); //the distance up the weapon where the blade portion starts, as a percentage of total length
    double idealPointOfBalance = bladeStartPercentage + (profile.bladeLength() * 0.33) / (profile.bladeLength() + profile.handleLength()); //ideal point of balance is 33% up the blade from the blade start

    // most important thing is bevel angle and length, so let's start there
    double lethality = bevelLengthToLethalityBase(profile.absoluteBevelLength()) * primaryEdgeBevelAngleNormalized; //multiply here to make both stats relevant
    if (profile.primaryBevel().bevelType() == WeaponProfile.BevelType.CONCAVE) {
      lethality += 10; //concave bevels are slightly more lethal
    } else if (profile.primaryBevel().bevelType() == WeaponProfile.BevelType.CONVEX) {
      lethality -= 10; //convex bevels are slightly less lethal
    }
    if (pointOfBalanceNormalized >= bladeStartPercentage) {
      lethality += bladeStartPercentage*10; //if the point of balance is on the blade, give a little bonus
    }
    lethality *= 0.5 + ((pointOfBalanceNormalized/(2*idealPointOfBalance))); //the ideal point of balance is where lethality and attack speed are balanced, higher = more lethality, lower = more speed.

    double normalizedHardness = tierInfo.hardness()/MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedFlexibility = tierInfo.flexibility()/MATERIAL_PROPERTY_SOFT_CAP;
    lethality *= 1 + 0.09*normalizedHardness - 0.04*Math.abs(normalizedFlexibility-5)/5; //factor material properties
    /*
      increases with hardness (keeps a sharp edge), flexibility has a sweet spot
      at flexibility = 5. Too low flexibility and the blade can't flex at all
      to achieve the correct angle, too high and the blade might bounce off the target
      instead of cutting in.
    */

    return lethality;
  }

  /**
   * Calculates a base lethality value for slash from the bevel length.
   * Bevel length is derived from blade width and bevel percentage.
   * The formula provides diminishing returns for bevel lengths over 80mm,
   * because the average bevel length should be roughly 40mm.
   * @param bevelLength the length of the bevel in millimetres, always > 0.
   * @return the base lethality value, somewhere between 0 and ~80.
   */
  private static double bevelLengthToLethalityBase(double bevelLength) {
    if (bevelLength <= 80) return 0.7*bevelLength;
    else {
      return 56.0 + 7*Math.log(1 + 0.1*(bevelLength-80));
    }
  }

  public static double thrustLethality(WeaponProfile profile, ExtraTierInfo tierInfo) {
    //weapon profile values and normalizations
    double tipBevelAngleNormalized = EDGE_ANGLE_DEFAULT / profile.tipSpecs().tipBevelAngle(); //acuter angle means more lethality cause better piercing
    double primaryAngle = -180 + profile.tipSpecs().tipBevelAngle() + profile.tipSpecs().tipBevelShoulderAngle(); //mathematically derived
    double primaryAngleNormalized = BEVEL_ANGLE_DEFAULT / primaryAngle; //acuter angle means more lethality cause better piercing
    double lethality = primaryAngleNormalized * bladeDimensionsToLethalityBase(profile.bladeLength(), profile.bladeTipShoulderWidth(), profile.bladeCrossguardWidth());
    if (profile.singleEdged()) lethality*=0.5;

    double normalizedHardness = tierInfo.hardness()/MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedFlexibility = tierInfo.flexibility()/MATERIAL_PROPERTY_SOFT_CAP;
    lethality *= 1.0 + 0.08 * normalizedHardness - 0.06 * normalizedFlexibility;

    return lethality;
  }

  /**
   * Calculates a base lethality value for thrust from sword dimensions.
   * Formula considers blade length, which increases lethality, and blade width.
   * The narrower the blade, the higher the lethality, and vice versa.
   * The wider the blade is at any particular point, the less the length of the blade
   * contributes to lethality.
   * @param bladeLength the length of the blade in millimetres
   * @param bladeTipWidth the width of the blade at the tip shoulder bevel in millimetres
   * @param bladeCrossguardWidth the width of the blade at the crossguard in millimetres
   * @return the base lethality value, somewhere between 0 and ~80.
   */
  public static double bladeDimensionsToLethalityBase(double bladeLength, double bladeTipWidth, double bladeCrossguardWidth) {
    //the average blade length is around 750mm, with 1500mm being a top 1% and 300mm being a bottom 1%.
    //the amount the length contributes should be calculated based on how wide the blade is at that point.
    //the average blade has width of roughly 50mm at crossguard and 30mm at tip shoulder, so average is 40mm.
    //the ratio of length to avg width to achieve 75 lethality is 91.3, because the average blade isn't
    //a good thrusting sword. Instead, the average rapier is 1050mm long, 15mm at crossguard and 8mm at tip shoulder.
    double averageBladeWidth = (bladeTipWidth + bladeCrossguardWidth) * 0.5;
    double lengthContribution = (bladeLength / averageBladeWidth);
    //so, we need a function that linearly increases lethality based on length contribution from
    //0 to 91.3, achieving y = 75 at x = 91.3, and then diminishing returns after that.
    if (lengthContribution <= 91.3) {
      return 0.82 * lengthContribution;
    } else {
      return 75.0 + 5*Math.log(1 + 0.1*(lengthContribution-91.3));
    }
  }

  public static double strikeLethality(WeaponProfile profile, ExtraTierInfo tierInfo) {
    //Some calculations about the balance point of the weapon, using material density and profile dimensions
    double pointOfBalanceNormalized = profile.pointOfBalance() / (profile.bladeLength() + profile.handleLength());

    double bladeWeight = profile.estimateBladeVolume() * tierInfo.density(); //in grams
    double lethality = weightToLethalityBase(bladeWeight);

    lethality *= 0.5 + 0.75*pointOfBalanceNormalized;

    double normalizedHardness = tierInfo.hardness()/MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedFlexibility = tierInfo.flexibility()/MATERIAL_PROPERTY_SOFT_CAP;
    lethality *= 1.0 + 0.15 * normalizedHardness - 0.15 * normalizedFlexibility;

    return lethality;
  }

  /**
   * Converts weapon weight to a lethality linearly, since for strike,
   * you're literally just smacking them with a piece of metal or whatever.
   * The weight of whatever it is matters most.
   * @param weight the weight of the weapon in grams
   * @return the base lethality value for strike attacks
   */
  public static double weightToLethalityBase(double weight) {
    if (weight <= 3000) return 0.0167*weight + 30; //3000g = 80 leth
    else {
      return 80.0 + 4*Math.log(1 + 0.01*(weight-3000)); //diminishing returns after 3000g
    }
  }



}
