package com.cwjn.skada.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilLethalityRuntimeTest {

  @Test
  void slashBonusDamageIsMonotonicAndNotDrivenByToughness() {
    double lowLethality = Util.percentBonusDamage(10.0, 10.0, 2.0, 20.0);
    double highLethality = Util.percentBonusDamage(60.0, 10.0, 2.0, 20.0);
    double highToughness = Util.percentBonusDamage(60.0, 10.0, 20.0, 20.0);

    assertTrue(lowLethality > 1.0);
    assertTrue(highLethality > lowLethality);
    assertEquals(highLethality, highToughness, 1.0e-9);
  }

  @Test
  void strikeArmourMultiplierRespondsPrimarilyToArmourState() {
    double lightlyArmoured = Util.percentReduc(60.0, 5.0, 2.0, 20.0);
    double heavilyArmoured = Util.percentReduc(60.0, 20.0, 2.0, 20.0);
    double tougherButSameArmour = Util.percentReduc(60.0, 20.0, 8.0, 20.0);

    assertTrue(lightlyArmoured < heavilyArmoured);
    assertTrue(heavilyArmoured < tougherButSameArmour);
    assertTrue(lightlyArmoured >= 0.25 && lightlyArmoured <= 1.0);
  }
}