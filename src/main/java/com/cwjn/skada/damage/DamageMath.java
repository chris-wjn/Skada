package com.cwjn.skada.damage;

import com.cwjn.skada.data.damage.ElementSpreadInstance;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilCombat;

import net.minecraft.util.RandomSource;

public final class DamageMath {

  private static final double MIN_DAMAGE_MULTIPLIER = 0.5;

  private DamageMath() {
  }

  /**
   * Converts an armour-scale rating into a signed mitigation percentage.
   * Positive values mitigate damage while negative values become vulnerability.
   *
   * @param rating the armour-style mitigation rating
   * @return the signed mitigation percentage represented by the rating
   */
  public static double getSignedArmourStyleMitigationPercent(double rating) {
    if (rating == 0.0) {
      return 0.0;
    }

    return rating < 0.0
      ? -100.0 / (1.0 + Math.exp((rating / 10.0) + 2.0))
      : 100.0 / (1.0 + Math.exp((-rating / 10.0) + 2.0));
  }

  /**
   * Armour reduction formula, using a logistic curve to convert armour points to percentage resistance.
   *  <pre>
   *    if x >= 0, y = 100 / (1 + e^((-x/10) + 2))
   *    if x < 0, y = -100 / (1 + e^((x/10) + 2))
   *  </pre>
   * @param damage The damage to be reduced.
   * @param armour The armour value, can be negative.
   * @return The damage after armour reduction.
   */
  public static double getDamageAfterArmourReduction(double damage, double armour) {
    if (armour == 0) {
      return damage;
    }

    double resistance = getSignedArmourStyleMitigationPercent(armour);
    return resistance >= 100 ? 0 : damage * (1 - resistance / 100);
  }

  /**
   * Uses the shared armour-style signed mitigation curve for non-physical elemental resistance.
   * @param damage The damage to be reduced.
   * @param resistance The resistance value, can be negative.
   * @return The damage after elemental resistance reduction.
   */
  public static double getDamageAfterElementalResistance(double damage, double resistance) {
    return getDamageAfterArmourReduction(damage, resistance);
  }

  /**
   * Get a percentage of damage reduced based on the resistance value, using the damage class formula.
   * The result of this function should be multiplied with the value to be reduced.
   *  <pre>
   *    y = 100 / (100 + x)
   *  </pre>
   * @param damage The damage to be reduced.
   * @param resistance The resistance value, can be negative.
   * @return The damage after reduction.
   */
  public static double getDamageAfterAttackTypeReduction(double damage, double resistance) {
    return damage * (100 / (100 + resistance));
  }

  /**
   * Calculates damage based on precision using a normal distribution. Higher precision results
   * in lower standard deviation, leading to more consistent damage.
   *
   * @param rawPrecision The generated precision score for this attack.
   * @param targetToughness The target's armour toughness, which degrades effective consistency.
   * @param damage The initial damage value.
   * @param random The random source for generating normal distribution values.
   * @return The damage after applying precision adjustments.
   */
  public static double getDamageFromPrecisionNormalDistribution(double rawPrecision, double targetToughness, double damage, RandomSource random) {
    double consistency = UtilCombat.precisionScoreToConsistency(rawPrecision, targetToughness);
    double standardDeviation = (1.0 - consistency) * damage;
    double z = random.nextGaussian();
    double normalDistributionModifier = Math.abs(z) * standardDeviation;
    return Math.max(damage * MIN_DAMAGE_MULTIPLIER, damage - normalDistributionModifier);
  }

  /*
   * This method increases the element spread based on the attacker's finesse and the defender's mobility. Uses
   * a roll system, with 4 tiers.
   * 
   * Currently unused, kept for potential future use.
   */
  private static int finesseMobilityFormula(ElementSpreadInstance spread, double finesse, double mobility, double secondaryStat, double agility, RandomSource random) {
    int difference = Math.min((int) ((finesse * secondaryStat * 0.1) - (mobility * agility * 0.1)), 300);
    if (difference == 0) return 0;
    if (difference > 0) {
      int extraHits = 0;
      while (true) {
        double rate = 0.5 - extraHits * 0.25;
        if (extraHits == 1) rate = 0.33;
        if (difference < 100) {
          if (rate * difference * 0.01 > random.nextDouble()) {
            extraHits++;
          }
          break;
        } else {
          difference -= 100;
          if (rate > random.nextDouble()) {
            extraHits++;
          } else {
            break;
          }
        }
      }
      return extraHits;
    } else {
      difference *= -1;
      int tier = 0;
      double damageReduc = 0.0;
      while (true) {
        double rate = 0.5 - tier * 0.25;
        if (tier == 1) rate = 0.33;
        if (difference < 100) {
          if (rate * difference * 0.01 > random.nextDouble()) {
            damageReduc += 16.5;
          }
          break;
        } else {
          difference -= 100;
          if (rate > random.nextDouble()) {
            damageReduc += 16.5;
            tier++;
          } else {
            break;
          }
        }
      }
      double finalDamageReduc = damageReduc;
      spread.applyFunctionToAll(d -> d * (1 - finalDamageReduc * 0.01));
    }
    return 0;
  }

}