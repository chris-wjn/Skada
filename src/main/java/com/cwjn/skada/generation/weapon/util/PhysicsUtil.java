package com.cwjn.skada.generation.weapon.util;

import com.cwjn.skada.generation.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.util.UtilCombat;

public class PhysicsUtil {

  public static double toKgM2(double gramCentimeterSquared) {
    return gramCentimeterSquared * 1.0e-7;
  }

  public static double toKg(double gram) {
    return gram * 1.0e-3;
  }

  /**
   * Convenience overload using default swing arc and skill exponent.
   *
   * @see #angularVelocity(double, double, double, double)
   */
  public static double angularVelocity(double inertia, double playerStrength) {
    return angularVelocity(inertia, playerStrength,
      UtilCombat.SWING_ARC_DEFAULT, UtilCombat.SKILL_EXPONENT_DEFAULT);
  }

  /**
   * Computes angular velocity from a torque → angular acceleration → angular velocity
   * derivation chain, with a skill-compensation adjustment that accounts for trained
   * fighters partially overcoming weapon inertia.
   *
   * <h3>Derivation chain</h3>
   * <ol>
   *   <li><b>Torque:</b> {@code τ = TORQUE_PER_STRENGTH × playerStrength} — the
   *       effective moment the wielder can apply at the weapon pivot.</li>
   *   <li><b>Angular acceleration:</b> {@code α = τ / I} — Newton's second law
   *       for rotation.</li>
   *   <li><b>Physics angular velocity:</b> {@code ω_physics = √(2αθ) = √(2τθ / I)}
   *       — work-energy theorem: the torque work τ·θ equals the rotational kinetic
   *       energy ½Iω².</li>
   *   <li><b>Skill compensation:</b> The exponent 0.5 (pure Newtonian) is replaced by
   *       the configured {@code skillExponent} via a multiplicative adjustment:
   *       {@code ω = ω_physics × (I / I_ref)^(0.5 - skillExponent)}.</li>
   * </ol>
   *
   * <h3>Why reference inertia exists</h3>
   * The reference inertia I_ref is the pivot point where (I / I_ref)^anything = 1.
   * This decouples the calibration anchor from the curve shape: at I = I_ref the
   * output equals the pure physics answer for that weapon weight, and changing the
   * skill exponent only changes how other weights deviate from it.
   *
   * <h3>Calibration</h3>
   * The constants in {@link UtilCombat} are calibrated to HEMA video data showing
   * trained fighters achieve 5-20 rad/s across typical weapon weights:
   * <ul>
   *   <li>Dagger (~0.00005 kg·m²): 12-18 rad/s</li>
   *   <li>Longsword (~0.00015 kg·m², reference): 8-12 rad/s</li>
   *   <li>Greatsword (~0.0003 kg·m²): 5-8 rad/s</li>
   * </ul>
   *
   * @param inertia        moment of inertia of the weapon in kg·m²
   * @param playerStrength wielder strength (50.0 = average trained human)
   * @param swingArc       angular displacement of the swing in radians
   * @param skillExponent  inertia-compensation exponent; 0.5 = pure Newtonian,
   *                       0.4 = HEMA-trained (default), lower = more skilled
   * @return the angular velocity in radians per second, typically 5-20 rad/s
   */
  public static double angularVelocity(double inertia, double playerStrength,
                                       double swingArc, double skillExponent) {
    // Guard against non-positive inertia
    if (inertia <= 0 || Double.isNaN(inertia) || Double.isInfinite(inertia)) {
      return 0.0;
    }

    // 1. Effective torque applied by the wielder
    double torque = 0.0;
    if (playerStrength > 0 && !Double.isNaN(playerStrength) && !Double.isInfinite(playerStrength)) {
      torque = UtilCombat.TORQUE_PER_STRENGTH * playerStrength;
    }

    if (torque <= 0) {
      return 0.0;
    }

    // 2. Physics angular velocity from work-energy: ω = √(2τθ / I)
    double omegaPhysics = Math.sqrt(2.0 * torque * swingArc / inertia);
    if (!Double.isFinite(omegaPhysics) || omegaPhysics <= 0) {
      return 0.0;
    }

    // 3. Skill compensation factor: adjusts the exponent from pure Newtonian (0.5)
    //    to the actual wielder skill level.
    double inertiaRatio = inertia / UtilCombat.REFERENCE_INERTIA;
    double exponentDelta = 0.5 - skillExponent;         // how far from Newtonian
    double skillFactor = Math.pow(inertiaRatio, exponentDelta);
    if (!Double.isFinite(skillFactor) || skillFactor <= 0) {
      skillFactor = 1.0;
    }

    double result = omegaPhysics * skillFactor;

    // Clamp to reasonable physical bounds
    result = Math.max(0.0, Math.min(result, UtilCombat.ANGULAR_VELOCITY_MAX));
    return result;
  }

  public record MassProperties(double volumeCm3, double massG, Vec3 centerOfMass) {}

}
