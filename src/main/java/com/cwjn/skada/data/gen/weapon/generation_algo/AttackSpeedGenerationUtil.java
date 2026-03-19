package com.cwjn.skada.data.gen.weapon.generation_algo;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.util.Mth;
import com.cwjn.skada.util.Util;

/**
 * Generates attack speed multipliers from weapon mass and point of balance.
 *
 * Design alignment:
 * - mass multiplier: m_mult = 1 - k_m * ln(m / m_std)
 * - balance multiplier: p_mult = 1 - k_p * Δ, where Δ = PoB_actual - PoB_ideal
 * - final: clamp(m_mult * p_mult, 0.5, 1.5)
 */
public abstract class AttackSpeedGenerationUtil {

  private static final AttackType SLASH_CONTEXT = new AttackType("slash", null, null, null, SlashCapable.class);
  private static final AttackType THRUST_CONTEXT = new AttackType("thrust", null, null, null, ThrustCapable.class);
  private static final AttackType STRIKE_CONTEXT = new AttackType("strike", null, null, null, StrikeCapable.class);

  private static final double SPEED_BASE = 1.42;
  private static final double FORWARD_BIAS_WEIGHT = 0.45;
  private static final double BALANCE_MISMATCH_WEIGHT = 0.25;
  private static final double OVERCOMMITMENT_WEIGHT = 0.8;
  private static final double INERTIA_RATIO_WEIGHT = 1.35;
  private static final double MIN_SPEED_MULT = 0.5;
  private static final double MAX_SPEED_MULT = 1.5;

  public static double slash(WeaponAssembly weapon) {
    WeaponPartEntry head = weapon.primaryPartForAttackType(SLASH_CONTEXT)
            .orElseThrow(() -> new IllegalStateException("Tried to generate slash critical fail for weapon without slash capability"));
    MaterialInfo material = head.material();
    return attackSpeed(weapon, material, SLASH_CONTEXT);
  }

  public static double thrust(WeaponAssembly weapon) {
    WeaponPartEntry head = weapon.primaryPartForAttackType(THRUST_CONTEXT)
            .orElseThrow(() -> new IllegalStateException("Tried to generate thrust critical fail for weapon without thrust capability"));
    MaterialInfo material = head.material();
    return attackSpeed(weapon, material, THRUST_CONTEXT);
  }

  public static double strike(WeaponAssembly weapon) {
    WeaponPartEntry head = weapon.primaryPartForAttackType(STRIKE_CONTEXT)
            .orElseThrow(() -> new IllegalStateException("Tried to generate strike critical fail for weapon without strike capability"));
    MaterialInfo material = head.material();
    return attackSpeed(weapon, material, STRIKE_CONTEXT);
  }

  private static double attackSpeed(WeaponAssembly weapon, MaterialInfo material, AttackType attackType) {
    MaterialInfo effectiveMaterial = weapon.primaryPartMaterialOrDefault(attackType, material);

    double mass = Math.max(1.0, weapon.mass(WeaponAssembly.LARGE_SAMPLE_SIZE));
    double totalLength = Math.max(1.0, weapon.length());
    double actualPoBNorm = Mth.clamp(weapon.pointOfBalance(WeaponAssembly.LARGE_SAMPLE_SIZE) / totalLength, 0.0, 1.0);
    double idealPoBNorm = weapon.normalizedIdealPointOfBalanceForAttackType(attackType);

    double delta = actualPoBNorm - idealPoBNorm;
    double balanceMismatch = Math.abs(delta);
    double overcommitment = Math.max(0.0, delta);
    double inertiaRatio = normalizedInertiaRatio(weapon, mass, totalLength);

    double recoveryCost = FORWARD_BIAS_WEIGHT * actualPoBNorm
      + BALANCE_MISMATCH_WEIGHT * balanceMismatch
      + OVERCOMMITMENT_WEIGHT * overcommitment
      + INERTIA_RATIO_WEIGHT * inertiaRatio;

    double speedMult = Mth.clamp(SPEED_BASE - recoveryCost, MIN_SPEED_MULT, MAX_SPEED_MULT);
    double finalSpeedMult = Util.round(speedMult, 3);
    return finalSpeedMult;
  }

  /**
   * Computes the weapon's moment of inertia as a normalized ratio against {@code mass * totalLength^2}.
   *
   * The result is clamped to {@code [0, 1]} so it can be used as a bounded penalty term in attack-speed
   * generation without letting unusually large inertia values dominate the score.
   *
   * @param weapon the weapon assembly being evaluated
   * @param mass the weapon mass in grams, already normalized to a non-zero minimum
   * @param totalLength the weapon length in centimeters, already normalized to a non-zero minimum
   * @return the normalized inertia ratio, clamped to the inclusive range {@code [0, 1]}
   */
  private static double normalizedInertiaRatio(WeaponAssembly weapon, double mass, double totalLength) {
    double denominator = Math.max(1.0e-6, mass * totalLength * totalLength);
    double inertia = Math.max(0.0, weapon.momentOfInertiaAboutBase(com.cwjn.skada.data.gen.weapon.util.WeaponAxis.Z, WeaponAssembly.LARGE_SAMPLE_SIZE));
    return Mth.clamp(inertia / denominator, 0.0, 1.0);
  }

}
