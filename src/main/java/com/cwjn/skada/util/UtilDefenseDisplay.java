package com.cwjn.skada.util;

import com.cwjn.skada.damage.DamageMath;

import java.util.function.DoubleUnaryOperator;

/**
 * Display-only defence helpers for UI summaries and same-slot equipment previews.
 *
 * <p>These methods intentionally mirror the runtime mitigation formulas without changing
 * any combat behavior. They should be used only for presentation.</p>
 */
public final class UtilDefenseDisplay {

  private UtilDefenseDisplay() {
  }

  /**
   * Converts a total armour rating into a signed mitigation percentage.
   * Negative values represent vulnerability instead of reduction.
   *
   * @param armourTotal the aggregated armour rating
   * @return the signed mitigation percentage for the supplied total
   */
  public static double armourMitigationPercent(double armourTotal) {
    return DamageMath.getSignedArmourStyleMitigationPercent(armourTotal);
  }

  /**
   * Converts an aggregated elemental resistance rating into a signed mitigation percentage.
   * Positive and negative totals use the same signed armour-scale curve as runtime.
   *
   * @param resistanceTotal the aggregated elemental resistance rating
   * @return the signed mitigation percentage for the supplied total
   */
  public static double elementalMitigationPercent(double resistanceTotal) {
    return DamageMath.getSignedArmourStyleMitigationPercent(resistanceTotal);
  }

  /**
   * Converts an aggregated attack-type resistance rating into a signed mitigation percentage.
   * Negative values represent vulnerability instead of reduction.
   *
   * @param resistanceTotal the aggregated attack-type resistance rating
   * @return the signed mitigation percentage for the supplied total
   */
  public static double attackTypeMitigationPercent(double resistanceTotal) {
    return 100.0 - (10000.0 / (100.0 + resistanceTotal));
  }

  /**
   * Builds an armour preview by replacing the currently equipped same-slot contribution with the hovered one.
   *
   * @param currentTotal the player's current aggregated armour total
   * @param equippedSlotContribution the currently equipped item's contribution in the same slot
   * @param hoveredSlotContribution the hovered item's contribution in that slot
   * @return a preview of the total and mitigation change after the swap
   */
  public static EquippedSwapPreview previewArmourSwap(
    double currentTotal,
    double equippedSlotContribution,
    double hoveredSlotContribution
  ) {
    return previewSwap(currentTotal, equippedSlotContribution, hoveredSlotContribution, UtilDefenseDisplay::armourMitigationPercent);
  }

  /**
   * Builds an elemental-resistance preview by replacing the currently equipped same-slot contribution with the hovered one.
   *
   * @param currentTotal the player's current aggregated elemental resistance total
   * @param equippedSlotContribution the currently equipped item's contribution in the same slot
   * @param hoveredSlotContribution the hovered item's contribution in that slot
   * @return a preview of the total and mitigation change after the swap
   */
  public static EquippedSwapPreview previewElementalResistanceSwap(
    double currentTotal,
    double equippedSlotContribution,
    double hoveredSlotContribution
  ) {
    return previewSwap(currentTotal, equippedSlotContribution, hoveredSlotContribution, UtilDefenseDisplay::elementalMitigationPercent);
  }

  /**
   * Builds an attack-type-resistance preview by replacing the currently equipped same-slot contribution with the hovered one.
   *
   * @param currentTotal the player's current aggregated attack-type resistance total
   * @param equippedSlotContribution the currently equipped item's contribution in the same slot
   * @param hoveredSlotContribution the hovered item's contribution in that slot
   * @return a preview of the total and mitigation change after the swap
   */
  public static EquippedSwapPreview previewAttackTypeResistanceSwap(
    double currentTotal,
    double equippedSlotContribution,
    double hoveredSlotContribution
  ) {
    return previewSwap(currentTotal, equippedSlotContribution, hoveredSlotContribution, UtilDefenseDisplay::attackTypeMitigationPercent);
  }

  private static EquippedSwapPreview previewSwap(
    double currentTotal,
    double equippedSlotContribution,
    double hoveredSlotContribution,
    DoubleUnaryOperator mitigationFunction
  ) {
    double newTotal = currentTotal - equippedSlotContribution + hoveredSlotContribution;
    return new EquippedSwapPreview(
      currentTotal,
      newTotal,
      mitigationFunction.applyAsDouble(currentTotal),
      mitigationFunction.applyAsDouble(newTotal));
  }

  /**
   * Bundles current and preview totals together with their derived mitigation percentages.
   *
   * @param currentTotal the current aggregated total before the swap
   * @param newTotal the aggregated total after the same-slot swap preview
   * @param currentMitigationPercent the derived mitigation percentage for the current total
   * @param newMitigationPercent the derived mitigation percentage for the preview total
   */
  public record EquippedSwapPreview(
    double currentTotal,
    double newTotal,
    double currentMitigationPercent,
    double newMitigationPercent
  ) {

    /**
     * Returns the preview change expressed in percentage points.
     *
     * @return the signed percentage-point delta from current to preview total
     */
    public double percentagePointDelta() {
      return newMitigationPercent - currentMitigationPercent;
    }
  }
}