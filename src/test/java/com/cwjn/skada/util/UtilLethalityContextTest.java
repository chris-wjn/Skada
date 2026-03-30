package com.cwjn.skada.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilLethalityContextTest {

  @Test
  void thrustHealthDamageFallsAsCurrentHealthDrops() {
    double healthyTargetDamage = UtilCombat.percentHealthDamage(20.0, 0.0, 20.0);
    double injuredTargetDamage = UtilCombat.percentHealthDamage(20.0, 0.0, 5.0);
    double differentArmourSameHealthDamage = UtilCombat.percentHealthDamage(20.0, 25.0, 20.0);

    assertTrue(healthyTargetDamage > injuredTargetDamage);
    assertEquals(healthyTargetDamage, differentArmourSameHealthDamage, 1.0e-9);
  }
}