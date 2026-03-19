package com.cwjn.skada.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilAngularVelocityTest {

  @Test
  void representativeWeaponInertiasStayInExpectedBands() {
    double daggerLike = Util.angularVelocity(0.00005, 50.0);
    double swordLike = Util.angularVelocity(0.00015, 50.0);
    double greatswordLike = Util.angularVelocity(0.00030, 50.0);

    assertTrue(daggerLike > swordLike);
    assertTrue(swordLike > greatswordLike);
    assertTrue(daggerLike >= 12.0 && daggerLike <= 18.0);
    assertTrue(swordLike >= 8.0 && swordLike <= 12.0);
    assertTrue(greatswordLike >= 5.0 && greatswordLike <= 8.0);
  }

  @Test
  void nonPositiveInertiaReturnsZeroVelocity() {
    assertEquals(0.0, Util.angularVelocity(0.0, 50.0), 1.0e-9);
    assertEquals(0.0, Util.angularVelocity(-1.0, 50.0), 1.0e-9);
  }
}