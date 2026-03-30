package com.cwjn.skada.data.gen.weapon.generation_algo;

import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextFactory;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextSlash;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextStrike;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextThrust;
import com.cwjn.skada.util.Util;

/**
 * Generates attack speed offsets from weapon mass and point of balance.
 *
 * The shared balance model evaluates a handling score internally, then converts it into
 * a flat attack-speed addition or subtraction so the game's built-in base weapon pacing
 * is preserved instead of being multiplied again.
 */
public abstract class AttackSpeedGenerationUtil {

  private static final double SPEED_SCORE_MIDPOINT = 1.00; // neutral handling score before converting to an additive delta
  private static final double SPEED_SCORE_SWING = 0.30; // maximum shared handling swing in either direction before attack-type offsets
  private static final double HANDLING_NEUTRAL_RECOVERY_COST = 0.24; // recovery cost where attacks stay close to their native item tempo
  private static final double HANDLING_CURVE_SCALE = 0.20; // lower values make the score saturate sooner at the extremes
  private static final double BALANCE_POSITION_WEIGHT = 0.8; // PoB position overcommitment penalty or rearweighting bonus
  private static final double INERTIA_RATIO_WEIGHT = 1.15; // higher inertia ratio (more mass distributed toward the tip) reduces handling
  private static final double SLASH_ATTACK_SPEED_OFFSET = 0.12; // slash attacks reward fluid recovery slightly more than the shared baseline
  private static final double THRUST_ATTACK_SPEED_OFFSET = -0.03; // thrusts stay slightly more deliberate because they must both extend and recover
  private static final double STRIKE_ATTACK_SPEED_OFFSET = -0.12; // strikes remain the most committed attack type, but without collapsing into unusable APS

  // Following 3 methods are overload methods for convenience when you only have the weapon assembly and 
  // want to generate the attack speed directly without manually building the context
  // Calling these methods with context directly is preferred
  
  public static double slash(WeaponAssembly weapon) {
    return slash(AttackGenerationContextFactory.buildSlashContext(weapon));
  }

  public static double thrust(WeaponAssembly weapon) {
    return thrust(AttackGenerationContextFactory.buildThrustContext(weapon));
  }

  public static double strike(WeaponAssembly weapon) {
    return strike(AttackGenerationContextFactory.buildStrikeContext(weapon));
  }

  /**
   * Generates the additive attack-speed delta for a slash attack from the slash context.
   * @param context The context containing slash-specific parameters for attack speed calculation.
   * @return The calculated attack speed delta.
   */
  public static double slash(AttackGenerationContextSlash context) {
    return Util.round(baseAttackSpeedDelta(context.delivery().rearUnderweight(), context.delivery().forwardOvercommitment(), context.assembly().normalizedInertiaCoefficient())
      + SLASH_ATTACK_SPEED_OFFSET, 3);
  }

  /**
   * Generates the additive attack speed delta for a thrust attack from the thrust context.
   * @param context The context containing thrust-specific parameters for attack speed calculation.
   * @return The calculated attack speed delta.
   */
  public static double thrust(AttackGenerationContextThrust context) {
    return Util.round(baseAttackSpeedDelta(context.delivery().rearUnderweight(), context.delivery().forwardOvercommitment(), context.assembly().normalizedInertiaCoefficient())
      + THRUST_ATTACK_SPEED_OFFSET, 3);
  }

  /**
   * Generates the additive attack speed delta for a strike attack from the strike context.
   * @param context The context containing strike-specific parameters for attack speed calculation.
   * @return The calculated attack speed delta.
   */
  public static double strike(AttackGenerationContextStrike context) {
    return Util.round(baseAttackSpeedDelta(context.delivery().rearUnderweight(), context.delivery().forwardOvercommitment(), context.assembly().normalizedInertiaCoefficient())
      + STRIKE_ATTACK_SPEED_OFFSET, 3);
  }

  public static double baseAttackSpeedDelta(double rearUnderweight, double forwardOvercommitment, double normalizedInertiaCoefficient) {
    double balancePosition = forwardOvercommitment > 0.0 ? forwardOvercommitment : -rearUnderweight;
    return Util.round(attackSpeedScore(balancePosition, normalizedInertiaCoefficient) - 1.0, 3);
  }

  /**
   * Core attack speed formula that applies across all attack types.
   *
   * @return The calculated shared handling score before converting to a delta.
   */
  private static double attackSpeedScore(double balancePosition, double normalizedInertiaCoefficient) {
    double recoveryCost = BALANCE_POSITION_WEIGHT * balancePosition
                        + INERTIA_RATIO_WEIGHT * normalizedInertiaCoefficient;

    double handlingPressure = (recoveryCost - HANDLING_NEUTRAL_RECOVERY_COST) / HANDLING_CURVE_SCALE;
    double speedScore = SPEED_SCORE_MIDPOINT - SPEED_SCORE_SWING * Math.tanh(handlingPressure);
    return Util.round(speedScore, 3);
  }

}
