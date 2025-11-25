package com.cwjn.skada.data.gen.weapon.parts.attack_types;

import com.cwjn.skada.data.gen.weapon.parts.Blade;

public interface ThrustCapable {

  Blade.TipSpecifications tipSpecs();

  /**
   * Calculates how much of a taper the head has starting
   * from the tip and moving towards the base. Implementations
   * of this method should start with a ratio of length to median
   * width as a base, and then take 50 or so sample points moving down
   * the length of the blade, reducing the taper value by some factor
   * of the difference in width from the last sample point. These sample
   * points should be taken on 8, evenly spaced lines around the circumference
   * of the head, and the average width of all these points should be used
   * to calculate the taper value.
   * @return a double representing the taper value (higher = more tapered), where 90 is a very tapered head and 10 is a very blunt head.
   */
  double getTaperValue();

  /**
   * Get the ideal point of balance for the weapon head for thrust attacks.
   * @return Ideal point of balance as a normalized value (0.0 = base, 1.0 = tip)
   */
  default double getThrustNormalizedIdealPointOfBalance() {
    return 0.0;
  }

}
