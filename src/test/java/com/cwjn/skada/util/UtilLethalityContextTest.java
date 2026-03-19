package com.cwjn.skada.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilLethalityContextTest {

  @Test
  void thrustHealthDamageFallsAsCurrentHealthDrops() {
    double healthyTargetDamage = Util.percentHealthDamage(20.0, 0.0, 10.0, 20.0);
    double injuredTargetDamage = Util.percentHealthDamage(20.0, 0.0, 10.0, 5.0);

    assertTrue(healthyTargetDamage > injuredTargetDamage);
  }
}