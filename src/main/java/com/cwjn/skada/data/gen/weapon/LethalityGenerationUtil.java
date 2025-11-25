package com.cwjn.skada.data.gen.weapon;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.SlashCapable;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.ThrustCapable;
import com.cwjn.skada.data.registry.AttackType;

import static com.cwjn.skada.data.SkadaData.BEVEL_ANGLE_DEFAULT;
import static com.cwjn.skada.data.SkadaData.MATERIAL_PROPERTY_SOFT_CAP;

public abstract class LethalityGenerationUtil {

  /**
   * Calculates lethality for slashing attacks.
   * Average lethality for a sword should be around 50-70
   * @param profile The weapon profile of the weapon
   * @param material The weapon material info
   * @return a double representing lethality
   */
  public static double slash(WeaponProfile profile, ExtraTierInfo material) {
    //First we'll normalize our values for the weapon profile. We take 1 to be the "default" value.
    WeaponProfile.WeaponHeadEntry head = profile.getSlashHead();
    if (head.getMaterial().isPresent()) material = head.getMaterial().get();
    SlashCapable slashHead = (SlashCapable) head.getHead(); //this is a bit dubious but it should be fine
    double pointOfBalanceNormalized = profile.getPointOfBalance() / profile.getTotalLength(); //percentage from 0-1, where 1 is furthest from pommel
    double primaryBevelAngle = slashHead.primaryBevelAngle();
    double primaryBevelAngleNormalized = BEVEL_ANGLE_DEFAULT / primaryBevelAngle; //acuter angle means more lethality cause better cutting
    double normalizedBladeWeight = profile.normalizeBladeWeight(material);

    //Some calculations about the balance point of the weapon, using material density and profile dimensions
    double bladeStartPercentage = profile.getHandle().getLength() / profile.getTotalLength(); //the distance up the weapon where the blade portion starts, as a percentage of total length
    double idealPointOfBalanceNormalized = profile.getIdealPointOfBalanceWithHead(head, AttackType.slash())/profile.getTotalLength();

    // most important thing is bevel angle and length, so let's start there
    double lethality = bevelLengthToLethalityBase(slashHead.absoluteBevelLength()) * primaryBevelAngleNormalized; //multiply here to make both stats relevant

    lethality *= bevelCurvatureToLethalityMult(slashHead.primaryBevel().curveFactor());

    if (pointOfBalanceNormalized >= bladeStartPercentage) {
      lethality += bladeStartPercentage*10; //if the point of balance is on the blade, give a little bonus
    }
    lethality *= 0.5 + ((pointOfBalanceNormalized/(2*idealPointOfBalanceNormalized))); //the ideal point of balance is where lethality and attack speed are balanced, higher = more lethality, lower = more speed.

    double normalizedHardness = material.hardness()/MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedFlexibility = Math.abs(material.flexibility()-MATERIAL_PROPERTY_SOFT_CAP/2)/(MATERIAL_PROPERTY_SOFT_CAP/2);
    lethality *= 1 + 0.04*normalizedHardness + 0.07*normalizedBladeWeight - 0.05*normalizedFlexibility; //factor material properties
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

  /**
   * Derives a lethality multiplier from the bevel curvature factor.
   * Convex bevels (higher curve factor) are more lethal, concave bevels (lower curve factor) are less lethal.
   * The curve factor is expected to be between 0.33 and 2.0, but could be higher or lower.
   * @param curveFactor the curve factor of the bevel.
   * @return a lethality multiplier, where 1.0 is neutral.
   */
  private static double bevelCurvatureToLethalityMult(double curveFactor) {
    if (curveFactor <= 0) return 1;
    return 1/(2*curveFactor) + 0.5;
  }

  public static double thrust(WeaponProfile profile, ExtraTierInfo tierInfo) {
    WeaponProfile.WeaponHeadEntry thrustHead = profile.getThrustHead();
    if (thrustHead.getMaterial().isPresent()) tierInfo = thrustHead.getMaterial().get();
    ThrustCapable head = (ThrustCapable) thrustHead.getHead();
    //weapon profile values and normalizations
    double primaryAngle = -180 + head.tipSpecs().tipBevelAngle() + head.tipSpecs().tipBevelShoulderAngle(); //mathematically derived
    double primaryAngleNormalized = BEVEL_ANGLE_DEFAULT / primaryAngle; //acuter angle means more lethality cause better piercing
    double lethality = primaryAngleNormalized * bladeDimensionsToLethalityBase(head);

    if (head instanceof SlashCapable slashCapable && slashCapable.isSingleEdged()) {
      lethality *= 0.5;
    }

    double normalizedHardness = tierInfo.hardness()/MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedFlexibility = tierInfo.flexibility()/MATERIAL_PROPERTY_SOFT_CAP;
    lethality *= 1.0 + 0.1 * normalizedHardness - 0.06 * normalizedFlexibility;

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

  /**
   * Calculates a base lethality value for thrust from the dimensions of a ThrustCapable head.
   * The head could be any shape, blade, spike, cone, etc, so we need the head to provide its
   * own taper value.
   * @param head the ThrustCapable head of the weapon
   * @return the base lethality value, somewhere between 0 and ~80.
   */
  public static double bladeDimensionsToLethalityBase(ThrustCapable head) {
    double taperValue = head.getTaperValue();
    if (taperValue <= 91.3) {
      return 0.82 * taperValue;
    } else {
      return 75.0 + 5*Math.log(1 + 0.1*(taperValue-91.3));
    }
  }

  public static double strike(WeaponProfile profile, ExtraTierInfo tierInfo) {
    //Some calculations about the balance point of the weapon, using material density and profile dimensions
    double pointOfBalanceNormalized = profile.getPointOfBalance() / profile.getTotalLength(); //percentage from 0-1, where 1 is furthest from pommel

    double weight = profile.getVolume() * tierInfo.density(); //in grams
    double lethality = weightToLethalityBase(weight);

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
