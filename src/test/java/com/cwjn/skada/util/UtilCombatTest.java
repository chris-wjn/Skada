package com.cwjn.skada.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilCombatTest {

  private static final double EPS = 1.0e-9;

  @Test
  void zeroToughnessAlwaysGivesMaximumConsistency() {
    assertEquals(0.98, UtilCombat.precisionScoreToConsistency(0.0, 0.0), EPS);
    assertEquals(0.98, UtilCombat.precisionScoreToConsistency(50.0, 0.0), EPS);
    assertEquals(0.98, UtilCombat.precisionScoreToConsistency(-5.0, -3.0), EPS);
    assertEquals(0.98, UtilCombat.precisionScoreToConsistency(10.0), EPS);
  }

  @Test
  void zeroPrecisionHitsConsistencyFloor() {
    assertEquals(0.30, UtilCombat.precisionScoreToConsistency(0.0, 10.0), EPS);
    assertEquals(0.30, UtilCombat.precisionScoreToConsistency(-25.0, 10.0), EPS);
  }

  @Test
  void precisionEqualToToughnessGivesSixtyFivePercent() {
    assertEquals(0.65, UtilCombat.precisionScoreToConsistency(10.0, 10.0), EPS);
    assertEquals(0.65, UtilCombat.precisionScoreToConsistency(20.0, 20.0), EPS);
  }

  @Test
  void precisionAtTwiceToughnessReachesCap() {
    assertEquals(0.98, UtilCombat.precisionScoreToConsistency(20.0, 10.0), EPS);
    assertEquals(0.98, UtilCombat.precisionScoreToConsistency(40.0, 20.0), EPS);
  }

  @Test
  void capHoldsForExcessPrecision() {
    assertEquals(0.98, UtilCombat.precisionScoreToConsistency(1000.0, 1.0), EPS);
  }

  @Test
  void midRampBreakpointsMatchDesign() {
    assertEquals(0.475, UtilCombat.precisionScoreToConsistency(5.0, 10.0), EPS);
    assertEquals(0.815, UtilCombat.precisionScoreToConsistency(15.0, 10.0), EPS);
  }

  @Test
  void projectileInaccuracyMatchesHalvingCurve() {
    assertEquals(15.0, UtilCombat.precisionScoreToProjectileInaccuracy(0.0), EPS);
    assertEquals(7.5, UtilCombat.precisionScoreToProjectileInaccuracy(5.0), EPS);
    assertEquals(15.0 * (1.0 - 8.5 / 13.5), UtilCombat.precisionScoreToProjectileInaccuracy(8.5), EPS);
    assertEquals(15.0 * (1.0 - 11.0 / 16.0), UtilCombat.precisionScoreToProjectileInaccuracy(11.0), EPS);
    assertEquals(0.3, UtilCombat.precisionScoreToProjectileInaccuracy(1.0e9), EPS);
    assertEquals(15.0, UtilCombat.precisionScoreToProjectileInaccuracy(-10.0), EPS);
  }
}
