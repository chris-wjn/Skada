package com.cwjn.skada.data.gen.weapon;

import com.cwjn.skada.data.gen.weapon.new_system.weapon.WeaponAssembly;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.util.Mth;

/**
 * Generates attack speed multipliers based on point of balance and weapon weight.
 * Returns values typically in the range 0.9x - 1.1x, where 1.0 is "normal" speed.
 * 
 * The attack speed is influenced by:
 * - Point of balance relative to ideal: closer to handle = faster, further = slower
 * - Weapon weight: heavier weapons magnify the PoB effect slightly
 * 
 * The final multiplier is constrained to prevent extreme values that would
 * make weapons feel broken in gameplay.
 */
public abstract class AttackSpeedGenerationUtil {

  // Constraints for attack speed multiplier
  private static final double MIN_SPEED_MULT = 0.85;
  private static final double MAX_SPEED_MULT = 1.15;
  private static final double SOFT_MIN = 0.90;
  private static final double SOFT_MAX = 1.10;

  public static double slash(WeaponAssembly weapon, ExtraTierInfo material) {
    double idealPointOfBalance = weapon.idealPointOfBalanceForAttackType(AttackType.slash());
    return getBaseAttackSpeedMultiplier(weapon, material, idealPointOfBalance);
  }

  public static double thrust(WeaponAssembly weapon, ExtraTierInfo tierInfo) {
    double idealPointOfBalance = weapon.idealPointOfBalanceForAttackType(AttackType.thrust());
    return getBaseAttackSpeedMultiplier(weapon, tierInfo, idealPointOfBalance);
  }

  public static double strike(WeaponAssembly weapon, ExtraTierInfo tierInfo) {
    double idealPointOfBalance = weapon.idealPointOfBalanceForAttackType(AttackType.strike());
    return getBaseAttackSpeedMultiplier(weapon, tierInfo, idealPointOfBalance);
  }

  /**
   * Calculates the attack speed multiplier based on point of balance differential.
   * 
   * The formula uses a sigmoid-like mapping to ensure:
   * - Values stay within a reasonable range (0.85 - 1.15, soft limits 0.9 - 1.1)
   * - Small deviations from ideal PoB have proportional effects
   * - Large deviations are compressed to prevent extreme values
   * - Heavier weapons have slightly more pronounced effects
   * 
   * @param weapon the weapon assembly
   * @param tierInfo the weapon material info
   * @param idealPointOfBalance the ideal point of balance for this attack type in cm
   * @return a multiplier for attack speed, typically in range 0.9 - 1.1
   */
  private static double getBaseAttackSpeedMultiplier(WeaponAssembly weapon, ExtraTierInfo tierInfo, double idealPointOfBalance) {
    double normalizedIdealPointOfBalance = Mth.clamp(idealPointOfBalance/weapon.length(), 0.001, 0.999);
    double normalizedPointOfBalance = Mth.clamp(weapon.pointOfBalance(WeaponAssembly.LARGE_SAMPLE_SIZE) / weapon.length(), 0.001, 0.999);
    
    // Calculate differential: positive means PoB is closer to handle than ideal (faster)
    // negative means PoB is further from handle than ideal (slower)
    double pobDifferential = normalizedIdealPointOfBalance - normalizedPointOfBalance;
    
    // For very small differences, return 1.0 immediately
    if (Math.abs(pobDifferential) < 0.005) {
      return 1.0;
    }
    
    // Normalize the differential relative to the maximum possible deviation
    double normalizedDiff = normalizePoBDifferential(idealPointOfBalance, pobDifferential);
    
    // Weight factor: heavier weapons have slightly more pronounced PoB effects
    // but we keep this subtle (5-15% amplification based on weight)
    double normalizedMass = weapon.normalizedMass(tierInfo);
    double massFactor = 0.85 + 0.15 * Mth.clamp(normalizedMass, 0.5, 1.5);
    
    // Apply a tanh-based mapping for smooth compression at extremes
    // This ensures we stay within bounds while maintaining sensitivity near 1.0
    // The 0.15 coefficient limits the maximum deviation to roughly ±0.15 before clamping
    double rawMultiplier = 1.0 + 0.15 * Math.tanh(normalizedDiff * massFactor * 2.0);
    
    // Soft clamping: values beyond soft limits are compressed further
    double result;
    if (rawMultiplier > SOFT_MAX) {
      // Compress values above soft max
      result = SOFT_MAX + (rawMultiplier - SOFT_MAX) * 0.3;
    } else if (rawMultiplier < SOFT_MIN) {
      // Compress values below soft min
      result = SOFT_MIN + (rawMultiplier - SOFT_MIN) * 0.3;
    } else {
      result = rawMultiplier;
    }
    
    // Hard clamp to absolute limits
    return Mth.clamp(result, MIN_SPEED_MULT, MAX_SPEED_MULT);
  }

  /**
   * Normalizes the PoB differential to a -1 to +1 range based on the maximum
   * possible deviation in each direction from the ideal point of balance.
   */
  private static double normalizePoBDifferential(double idealPointOfBalance, double pobDifferential) {
    double maxTowardsTip = 1.0 - idealPointOfBalance;
    double maxTowardsHandle = idealPointOfBalance;
    
    if (pobDifferential < 0) {
      // PoB is further from handle than ideal (slower)
      return maxTowardsTip > 0.001 ? pobDifferential / maxTowardsTip : 0.0;
    } else {
      // PoB is closer to handle than ideal (faster)
      return maxTowardsHandle > 0.001 ? pobDifferential / maxTowardsHandle : 0.0;
    }
  }

}
