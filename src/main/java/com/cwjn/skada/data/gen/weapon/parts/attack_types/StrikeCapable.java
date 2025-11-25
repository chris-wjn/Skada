package com.cwjn.skada.data.gen.weapon.parts.attack_types;

import com.cwjn.skada.data.gen.weapon.ExtraTierInfo;

public interface StrikeCapable {

  /**
   * Get the ideal point of balance for the weapon head for strike attacks.
   * @return Ideal point of balance as a normalized value (0.0 = base, 1.0 = tip)
   */
  default double getStrikeNormalizedIdealPointOfBalance() {
    return 1.0;
  }

}
