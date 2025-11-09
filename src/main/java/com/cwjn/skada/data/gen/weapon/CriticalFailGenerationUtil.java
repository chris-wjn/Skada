//package com.cwjn.skada.data.gen.weapon;
//
//import com.cwjn.skada.util.Util;
//
//import static com.cwjn.skada.data.SkadaData.MATERIAL_PROPERTY_SOFT_CAP;
//
//public abstract class CriticalFailGenerationUtil {
//
//  public static double slash(WeaponProfile profile, ExtraTierInfo tierInfo) {
//    //Main factor here is the primary edge bevel. A steeper edge angle means lower chance for crit fail because there's more material supporting the edge.
//    double failChance = critFailFromPrimaryBevel(profile.primaryBevelAngle());
//    if (profile.primaryBevel().bevelType() == WeaponProfile.BevelType.CONCAVE) {
//      failChance *= 2; //concave bevels are more likely to crit fail
//    } else if (profile.primaryBevel().bevelType() == WeaponProfile.BevelType.CONVEX) {
//      failChance /= 2; //convex bevels are less likely to crit fail
//    }
//    if (profile.edgeBevel().bevelType() == WeaponProfile.BevelType.CONCAVE) {
//      failChance *= 1.5; //concave edge bevels are more likely to crit fail
//    } else if (profile.edgeBevel().bevelType() == WeaponProfile.BevelType.CONVEX) {
//      failChance /= 1.5; //convex edge bevels are less likely to crit fail
//    }
//    double normalizedToughness = tierInfo.toughness()/MATERIAL_PROPERTY_SOFT_CAP;
//    double normalizedFlexibility = tierInfo.flexibility()/MATERIAL_PROPERTY_SOFT_CAP;
//
//    failChance *= 1 - (normalizedToughness*0.6) - (normalizedFlexibility*0.2);
//    return Util.round(failChance/100, 3);
//  }
//
//  /**
//   * Calculates critical fail chance based on primary bevel angle. Steeper bevels have lower chance
//   * to crit fail, and vice versa.
//   * Uses a curve that approaches 0% crit fail as bevel angle approaches inf,
//   * and approaches 30% crit fail as bevel angle approaches 1.
//   * We return the percentage as non-normalized value because it's easier
//   * to work with.
//   * @param bevelAngle The primary bevel angle in degrees.
//   * @return A double representing critical fail chance percentage, between 0 and 30.
//   */
//  public static double critFailFromPrimaryBevel(double bevelAngle) {
//    if (bevelAngle <= 1) return 30;
//    return 30/bevelAngle;
//  }
//
//  public static double thrust(WeaponProfile profile, ExtraTierInfo tierInfo) {
//    double failChance = critFailFromBladeDimensions(profile);
//    double normalizedToughness = tierInfo.toughness()/MATERIAL_PROPERTY_SOFT_CAP;
//    double normalizedFlexibility = tierInfo.flexibility()/MATERIAL_PROPERTY_SOFT_CAP;
//    failChance *= 1 - (normalizedToughness*0.3) - (normalizedFlexibility*0.4);
//    return Util.round(failChance/100, 3);
//  }
//
//  private static double critFailFromBladeDimensions(WeaponProfile profile) {
//    double averageBladeWidth = (profile.bladeTipShoulderWidth() + profile.bladeCrossguardWidth()) * 0.5;
//    double lengthContribution = (profile.bladeLength() / averageBladeWidth);
//
//    //Map slenderness ratio to crit fail chance using a curve.
//    //At ratio 20, crit fail chance is 5%. At ratio 50, crit fail chance is 25%.
//    if (lengthContribution <= 20) return 5;
//    if (lengthContribution >= 50) return 25;
//    return 5 + ((lengthContribution - 20) * (20.0/30.0));
//  }
//
//  public static double strike(WeaponProfile profile, ExtraTierInfo tierInfo) {
//      return Math.exp(-tierInfo.toughness()/2);
//  }
//
//}
