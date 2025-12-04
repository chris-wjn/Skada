package com.cwjn.skada.data.gen.weapon.parts.attack_types;

import com.cwjn.skada.data.gen.weapon.parts.Blade;
import com.cwjn.skada.util.Util;

/**
 * Interface indicating the weapon head can perform slashing attacks. Includes default
 * methods for calculating slash-specific properties if needed.
 */
public interface SlashCapable {

  Blade.Bevel primaryBevel();
  Blade.EdgeBevel edgeBevel();

  /**
   * Calculate the median width of a slashing weapon's blade.
   * @return the median width in millimetres.
   */
  double getMedianWidth();

  /**
   * Calculate the primary bevel angle of a weapon, if it has one.
   *
   * @return the primary bevel angle in degrees.
   */
  default double primaryBevelAngle() {
    return Util.findBevelAngle(edgeBevel().angle(), edgeBevel().shoulderAngle());
  }

  /**
   * Calculate the length of the primary bevel in mm.
   *
   * @return the absolute bevel length in millimetres.
   */
  default double absoluteBevelLength() {
    return primaryBevel().percentageOfBladeWidth()*getMedianWidth();
  }

  default boolean isSingleEdged() {
    return false;
  }

  /**
   * Get the ideal point of balance for the weapon head for slash attack.
   * @return Ideal point of balance in percentage from the base.
   */
  default double getSlashNormalizedIdealPointOfBalance() {
    return 0.33;
  }

}
