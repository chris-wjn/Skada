package com.cwjn.skada.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilPrecisionMappingTest {

  @Test
  void precisionScoreMapsToBoundedConsistency() {
    double low = Util.precisionScoreToConsistency(1.0);
    double mid = Util.precisionScoreToConsistency(10.0);
    double high = Util.precisionScoreToConsistency(40.0);

    assertTrue(low >= 0.0 && low <= 1.0);
    assertTrue(mid >= 0.0 && mid <= 1.0);
    assertTrue(high >= 0.0 && high <= 1.0);
    assertTrue(low < mid);
    assertTrue(mid < high);
  }

  @Test
  void targetToughnessReducesEffectiveConsistency() {
    double unopposed = Util.precisionScoreToConsistency(20.0, 0.0);
    double armoured = Util.precisionScoreToConsistency(20.0, 10.0);
    double veryArmoured = Util.precisionScoreToConsistency(20.0, 20.0);

    assertTrue(armoured < unopposed);
    assertTrue(veryArmoured < armoured);
  }

  @Test
  void projectileInaccuracyFallsAsPrecisionRises() {
    double lowPrecision = Util.precisionScoreToProjectileInaccuracy(1.0);
    double midPrecision = Util.precisionScoreToProjectileInaccuracy(10.0);
    double highPrecision = Util.precisionScoreToProjectileInaccuracy(40.0);

    assertTrue(lowPrecision > midPrecision);
    assertTrue(midPrecision > highPrecision);
    assertTrue(highPrecision >= 0.0);
  }
}