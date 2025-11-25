package com.cwjn.skada.data.gen.weapon;

import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.util.Mth;

/**
 * These generators are slightly different, we want to generate a multiplier for base attack speed.
 * So, it'll return some number around 0.5-1.5 probably, where 1.0 is "normal" speed.
 */
public abstract class AttackSpeedGenerationUtil {

  public static double slash(WeaponProfile profile, ExtraTierInfo material) {
    WeaponProfile.WeaponHeadEntry head = profile.getSlashHead();
    if (head.getMaterial().isPresent()) material = head.getMaterial().get();
    double totalLength = profile.getTotalLength();
    double idealPointOfBalance = profile.getIdealPointOfBalanceWithHead(head, AttackType.slash())/totalLength;
    return getBaseAttackSpeedMultiplier(profile, material, idealPointOfBalance);
  }

  public static double thrust(WeaponProfile profile, ExtraTierInfo tierInfo) {
    WeaponProfile.WeaponHeadEntry head = profile.getThrustHead();
    if (head.getMaterial().isPresent()) tierInfo = head.getMaterial().get();
    double totalLength = profile.getTotalLength();
    double idealPointOfBalance = profile.getIdealPointOfBalanceWithHead(head, AttackType.thrust())/ totalLength; //essentially, the very start of the head
    return getBaseAttackSpeedMultiplier(profile, tierInfo, idealPointOfBalance);
  }

  public static double strike(WeaponProfile profile, ExtraTierInfo tierInfo) {
    WeaponProfile.WeaponHeadEntry head = profile.getStrikeHead();
    if (head.getMaterial().isPresent()) tierInfo = head.getMaterial().get();
    double totalLength = profile.getTotalLength();
    double idealPointOfBalance = profile.getIdealPointOfBalanceWithHead(head, AttackType.strike())/ totalLength;
    return getBaseAttackSpeedMultiplier(profile, tierInfo, idealPointOfBalance);
  }

  /**
   * Calculates the base attack speed multiplier for a weapon attack type.
   * If the point of balance matches the ideal, the base multiplier is 1.0 (to then be modified by weight).
   * If the point of balance is closer to the handle than ideal, the multiplier increases (faster attack).
   * If the point of balance is further from the handle than ideal, the multiplier decreases (slower attack).
   * @param profile the weapon profile
   * @param tierInfo the weapon material info
   * @param idealPointOfBalance the ideal point of balance for this attack type, 0.0-1.0
   * @return a double which is a multiplier for attack speed. 1.0 = no change.
   */
  private static double getBaseAttackSpeedMultiplier(WeaponProfile profile, ExtraTierInfo tierInfo, double idealPointOfBalance) {
    idealPointOfBalance = Mth.clamp(idealPointOfBalance, 0.001, 0.999); //this should never be exactly 0 or 1, but just in case
    double pointOfBalanceNormalized = profile.getPointOfBalance() / profile.getTotalLength(); //percentage from 0-1, where 1 is furthest from pommel
    //we'll normalize the differential between actual and ideal point of balance. -1.0 for furthest from ideal towards tip, +1.0 for furthest from ideal towards handle.
    double POBDifferential = idealPointOfBalance - pointOfBalanceNormalized;
    //if the POBDifferential is 0, return 1.0 immediately. Leave room for some delta, since we're dealing with doubles.
    if (Math.abs(POBDifferential) < 0.001) {
      return 1.0;
    }
    //need to normalize the differential to -1.0 to 1.0, depending on which side of the ideal it is.
    double poBDifferentialNormalized = normalizePoBDifferential(idealPointOfBalance, POBDifferential);
    //the difference the PoB makes is dependent on the weight of the weapon, so let's calculate that now.
    double normalizedBladeWeight = profile.normalizeBladeWeight(tierInfo);
    return 1.0 + normalizedBladeWeight*poBDifferentialNormalized;
  }

  private static double normalizePoBDifferential(double idealPointOfBalance, double POBDifferential) {
    double maxPoBTowardsTip = 1.0 - idealPointOfBalance; //keep this as a positive number for easier calculations, even though it represents a negative direction
    double maxPobTowardsHandle = idealPointOfBalance - 0.0;
    double PoBDifferentialNormalized;
    if (POBDifferential < 0) {
      PoBDifferentialNormalized = POBDifferential / maxPoBTowardsTip; //normalize to -1.0 to 0.0
    } else {
      PoBDifferentialNormalized = POBDifferential / maxPobTowardsHandle; //normalize to 0.0 to 1.0
    }
    return PoBDifferentialNormalized;
  }

}
