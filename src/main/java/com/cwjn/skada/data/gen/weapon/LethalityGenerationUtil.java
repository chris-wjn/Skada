package com.cwjn.skada.data.gen.weapon;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.SlashCapable;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.ThrustCapable;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.Util;

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
    double primaryBevelAngle = slashHead.primaryBevelAngle(); //angle of the primary bevel, if the edge bevel shoulder angle is 180, this is just the edge bevel angle
    double primaryBevelAngleNormalized = Util.normalizeBevelAngle(primaryBevelAngle); //acuter angle means more lethality cause better cutting
    double normalizedBladeWeight = profile.normalizeBladeWeight(material);

    //Some calculations about the balance point of the weapon, using material density and profile dimensions
    double bladeStartPercentage = profile.getHandle().getLength() / profile.getTotalLength(); //the distance up the weapon where the blade portion starts, as a percentage of total length
    double idealPointOfBalanceNormalized = profile.getIdealPointOfBalanceWithHead(head, AttackType.slash())/profile.getTotalLength();

    // most important thing is bevel angle and length, so let's start there
    double lethality = bevelLengthToLethalityBase(slashHead.absoluteBevelLength()) * primaryBevelAngleNormalized; //multiply here to make both stats relevant

    // next we'll do some modifications based on the shoulder angle. Shoulder angles closer to 180 degrees are better, as the bump is less pronounced
    // and the blade can cut more cleanly. The curve factor of the primary bevel rounds this off, causing larger shoulders to be less punishing if the bevel is more convex.
    // shoulder angles above 180 are twice as punishing as those below 180, as they create a negative angle on the edge, meaning the blade curves "upwards", perpendicular
    // to the direction of the cut.
    double shoulderAngle = slashHead.edgeBevel().shoulderAngle();
    double distFromOptimalShoulder = Math.abs(180 - shoulderAngle);
    if (shoulderAngle > 180) distFromOptimalShoulder*=2;
    lethality -= distFromOptimalShoulder * Math.max(0, 2-slashHead.primaryBevel().curveFactor());

    // bevel curvature also affects lethality directly. more convex bevels cut better, more concave bevels cut worse
    lethality *= bevelCurvatureToLethalityMult(slashHead.primaryBevel().curveFactor());

    if (pointOfBalanceNormalized >= bladeStartPercentage) {
      lethality += bladeStartPercentage*10; //if the point of balance is on the blade, give a little bonus
    }
    lethality *= 0.5 + ((pointOfBalanceNormalized/(2*idealPointOfBalanceNormalized))); //the ideal point of balance is where lethality and attack speed are balanced, higher = more lethality, lower = more speed.

    double normalizedHardness = material.hardness()/MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedFlexibility = Math.abs(material.flexibility()-MATERIAL_PROPERTY_SOFT_CAP/2)/(MATERIAL_PROPERTY_SOFT_CAP/2);
    lethality *= 1 + 0.08*normalizedHardness + 0.14*normalizedBladeWeight - 0.1*normalizedFlexibility; //factor material properties
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
    double primaryAngle = Util.findBevelAngle(head.tipSpecs().tipBevelAngle(), head.tipSpecs().tipBevelShoulderAngle());
    double primaryAngleNormalized = Util.normalizeBevelAngle(primaryAngle); //acuter angle means more lethality cause better piercing
    double lethality = primaryAngleNormalized * bladeDimensionsToLethalityBase(head);

    double shoulderAngle = head.tipSpecs().tipBevelShoulderAngle();
    double distFromOptimalShoulder = Math.abs(180 - shoulderAngle);
    if (shoulderAngle > 180) distFromOptimalShoulder*=2;
    lethality -= distFromOptimalShoulder * (1-head.tipSpecs().tipShoulderRoundedness());

    if (head instanceof SlashCapable slashCapable && slashCapable.isSingleEdged()) {
      lethality *= 0.5;
    }

    double normalizedHardness = tierInfo.hardness()/MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedFlexibility = tierInfo.flexibility()/MATERIAL_PROPERTY_SOFT_CAP;
    lethality *= 1.0 + 0.1 * normalizedHardness - 0.06 * normalizedFlexibility;

    return lethality;
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
