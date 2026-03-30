package com.cwjn.skada.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilPrecisionMappingTest {

  @Test
  void precisionScoreMapsToBoundedConsistency() {
    double low = UtilCombat.precisionScoreToConsistency(1.0);
    double mid = UtilCombat.precisionScoreToConsistency(10.0);
    double high = UtilCombat.precisionScoreToConsistency(40.0);

    assertTrue(low >= 0.0 && low <= 1.0);
    assertTrue(mid >= 0.0 && mid <= 1.0);
    assertTrue(high >= 0.0 && high <= 1.0);
    assertTrue(low < mid);
    assertTrue(mid < high);
  }

  @Test
  void targetToughnessReducesEffectiveConsistency() {
    double unopposed = UtilCombat.precisionScoreToConsistency(20.0, 0.0);
    double armoured = UtilCombat.precisionScoreToConsistency(20.0, 10.0);
    double veryArmoured = UtilCombat.precisionScoreToConsistency(20.0, 20.0);

    assertTrue(armoured < unopposed);
    assertTrue(veryArmoured < armoured);
  }

  @Test
  void projectileInaccuracyFallsAsPrecisionRises() {
    double lowPrecision = UtilCombat.precisionScoreToProjectileInaccuracy(1.0);
    double midPrecision = UtilCombat.precisionScoreToProjectileInaccuracy(10.0);
    double highPrecision = UtilCombat.precisionScoreToProjectileInaccuracy(40.0);

    assertTrue(lowPrecision > midPrecision);
    assertTrue(midPrecision > highPrecision);
    assertTrue(highPrecision >= 0.0);
  }
}