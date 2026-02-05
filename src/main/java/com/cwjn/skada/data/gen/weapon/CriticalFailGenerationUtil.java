package com.cwjn.skada.data.gen.weapon;

import com.cwjn.skada.data.gen.weapon.new_system.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.old_system.WeaponProfile;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.SlashCapable;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.ThrustCapable;
import com.cwjn.skada.util.Util;

import static com.cwjn.skada.data.SkadaData.MATERIAL_PROPERTY_SOFT_CAP;

public abstract class CriticalFailGenerationUtil {

  public static double slash(WeaponAssembly profile, ExtraTierInfo material) {
    //Main factor here is the primary edge bevel. A steeper edge angle means lower chance for crit fail because there's more material supporting the edge.
    double pos = head.getHead().pointOfBalance();
    double failChance = critFailFromPrimaryBevel(slashHead.primaryBevelAngle(pos));
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

  /**
   * Calculates critical fail chance for strike attacks.
   * 
   * Strike weapons can fail by:
   * - Cracking or shattering on impact (harder, less tough materials)
   * - Handle breaking from shock (heavy heads, weak handles)
   * - Deformation (soft materials under high impact)
   * 
   * Key factors:
   * - Toughness: Primary defense against breaking. Higher toughness = lower fail chance.
   * - Hardness: Very hard materials are brittle. Extreme hardness increases fail chance.
   * - Weight: Heavier weapons generate more impact force, increasing stress on the weapon.
   * 
   * The formula balances these factors:
   * - Base fail chance starts at 10% for an "average" strike weapon
   * - Toughness reduces fail chance (exponential decay)
   * - Extreme hardness (>7) increases brittleness risk
   * - Heavier weapons have slightly higher fail chance due to impact stress
   * 
   * @param profile the weapon profile
   * @param tierInfo the material properties
   * @return critical fail chance as a decimal (e.g., 0.05 = 5%)
   */
  public static double strike(WeaponProfile profile, ExtraTierInfo tierInfo) {
    WeaponProfile.WeaponHeadEntry head = profile.getStrikeHead();
    if (head.getMaterial().isPresent()) tierInfo = head.getMaterial().get();
    
    // Normalize material properties (1-10 scale, soft cap at 10)
    double toughness = Math.max(1.0, Math.min(tierInfo.toughness(), MATERIAL_PROPERTY_SOFT_CAP));
    double hardness = Math.max(1.0, Math.min(tierInfo.hardness(), MATERIAL_PROPERTY_SOFT_CAP));
    double normalizedToughness = toughness / MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedHardness = hardness / MATERIAL_PROPERTY_SOFT_CAP;
    
    // Base fail chance: 15% for a "standard" strike weapon
    double baseFailChance = 15.0;
    
    // Toughness reduction: exponential decay
    // At toughness 1: multiplier ≈ 1.0 (no reduction)
    // At toughness 5: multiplier ≈ 0.5 (50% reduction)
    // At toughness 10: multiplier ≈ 0.25 (75% reduction)
    double toughnessMultiplier = Math.exp(-0.15 * toughness);
    
    // Brittleness factor: very hard materials are more prone to shattering
    // Hardness 1-5: no brittleness penalty
    // Hardness 6-8: slight penalty (1.0 - 1.3x)
    // Hardness 9-10: moderate penalty (1.3 - 1.6x)
    double brittlenessFactor = 1.0;
    if (hardness > 5.0) {
      brittlenessFactor = 1.0 + 0.12 * (hardness - 5.0);
    }
    
    // Weight factor: heavier weapons put more stress on themselves
    // This is a subtle effect (±10%)
    double weight = profile.getWeight(tierInfo);
    // Reference weight: 2000g (2kg) = neutral
    double weightFactor = 0.95 + 0.05 * Math.log(1.0 + weight / 2000.0);
    weightFactor = Math.max(0.9, Math.min(weightFactor, 1.2));
    
    // Calculate final fail chance
    double failChance = baseFailChance * toughnessMultiplier * brittlenessFactor * weightFactor;
    
    // Clamp to reasonable bounds: 1% minimum (nothing is indestructible), 30% maximum
    failChance = Math.max(1.0, Math.min(failChance, 30.0));
    
    // Return as decimal (divide by 100)
    return Util.round(failChance / 100.0, 3);
  }

}
