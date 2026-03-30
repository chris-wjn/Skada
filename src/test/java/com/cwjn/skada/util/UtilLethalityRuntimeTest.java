package com.cwjn.skada.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilLethalityRuntimeTest {

  @Test
  void slashBonusDamageIsMonotonicAndNotDrivenByToughness() {
    double lowLethality = UtilCombat.percentBonusDamage(10.0, 10.0, 20.0);
    double highLethality = UtilCombat.percentBonusDamage(60.0, 10.0, 20.0);
    double differentArmourSameLethality = UtilCombat.percentBonusDamage(60.0, 25.0, 20.0);

    assertTrue(lowLethality > 1.0);
    assertTrue(highLethality > lowLethality);
    assertEquals(highLethality, differentArmourSameLethality, 1.0e-9);
    assertTrue(highLethality < 1.5);
  }

  @Test
  void strikeArmourMultiplierDependsOnlyOnLethality() {
    double lowLethality = UtilCombat.percentReduc(18.0, 5.0, 20.0);
    double highLethality = UtilCombat.percentReduc(72.0, 20.0, 20.0);
    double sameLethalityDifferentArmour = UtilCombat.percentReduc(60.0, 5.0, 20.0);
    double sameLethalityMoreArmour = UtilCombat.percentReduc(60.0, 20.0, 20.0);

    assertTrue(lowLethality > highLethality);
    assertEquals(sameLethalityDifferentArmour, sameLethalityMoreArmour, 1.0e-9);
    assertTrue(highLethality >= 0.4 && highLethality <= 1.0);
  }
}