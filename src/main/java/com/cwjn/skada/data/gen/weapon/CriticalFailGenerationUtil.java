package com.cwjn.skada.data.gen.weapon;

import com.cwjn.skada.data.gen.weapon.parts.attack_types.SlashCapable;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.ThrustCapable;
import com.cwjn.skada.util.Util;

import static com.cwjn.skada.data.SkadaData.MATERIAL_PROPERTY_SOFT_CAP;

public abstract class CriticalFailGenerationUtil {

  public static double slash(WeaponProfile profile, ExtraTierInfo material) {
    WeaponProfile.WeaponHeadEntry head = profile.getSlashHead();
    if (head.getMaterial().isPresent()) material = head.getMaterial().get();
    SlashCapable slashHead = (SlashCapable) head.getHead();
    //Main factor here is the primary edge bevel. A steeper edge angle means lower chance for crit fail because there's more material supporting the edge.
    double failChance = critFailFromPrimaryBevel(slashHead.primaryBevelAngle());
    failChance *= slashHead.primaryBevel().curveFactor();

    double normalizedToughness = material.toughness()/MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedFlexibility = material.flexibility()/MATERIAL_PROPERTY_SOFT_CAP;

    failChance *= 1 - (normalizedToughness*0.6) - (normalizedFlexibility*0.2);
    return Util.round(failChance/100, 3);
  }

  /**
   * Calculates critical fail chance based on primary bevel angle. Steeper bevels have lower chance
   * to crit fail, and vice versa.
   * Uses a curve that approaches 0% crit fail as bevel angle approaches inf,
   * and approaches 30% crit fail as bevel angle approaches 1.
   * We return the percentage as non-normalized value because it's easier
   * to work with.
   * @param bevelAngle The primary bevel angle in degrees.
   * @return A double representing critical fail chance percentage, between 0 and 30.
   */
  public static double critFailFromPrimaryBevel(double bevelAngle) {
    if (bevelAngle <= 1) return 30;
    return 30/bevelAngle;
  }

  public static double thrust(WeaponProfile profile, ExtraTierInfo material) {
    WeaponProfile.WeaponHeadEntry head = profile.getThrustHead();
    if (head.getMaterial().isPresent()) material = head.getMaterial().get();
    ThrustCapable thrustHead = (ThrustCapable) head.getHead();
    double failChance = critFailFromBladeDimensions(thrustHead);
    double normalizedToughness = material.toughness()/MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedFlexibility = material.flexibility()/MATERIAL_PROPERTY_SOFT_CAP;
    failChance *= 1 - (normalizedToughness*0.3) - (normalizedFlexibility*0.4);
    return Util.round(failChance/100, 3);
  }

  private static double critFailFromBladeDimensions(ThrustCapable head) {
    double lengthContribution = head.getTaperValue();

    //Map slenderness ratio to crit fail chance using a curve.
    //At ratio 20, crit fail chance is 5%. At ratio 50, crit fail chance is 25%.
    if (lengthContribution <= 20) return 5;
    if (lengthContribution >= 50) return 25;
    return 5 + ((lengthContribution - 20) * (20.0/30.0));
  }

  public static double strike(WeaponProfile profile, ExtraTierInfo tierInfo) {
      return Math.exp(-tierInfo.toughness()/2);
  }

}
