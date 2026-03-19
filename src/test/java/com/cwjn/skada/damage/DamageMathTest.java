package com.cwjn.skada.damage;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageMathTest {

  @Test
  void armourReductionLeavesDamageUnchangedAtZeroArmour() {
    assertEquals(12.0, DamageMath.getDamageAfterArmourReduction(12.0, 0.0), 1.0e-9);
  }

  @Test
  void armourReductionIsMonotonicAcrossTypicalValues() {
    double unarmoured = DamageMath.getDamageAfterArmourReduction(20.0, 0.0);
    double lightlyArmoured = DamageMath.getDamageAfterArmourReduction(20.0, 10.0);
    double heavilyArmoured = DamageMath.getDamageAfterArmourReduction(20.0, 30.0);

    assertTrue(unarmoured > lightlyArmoured);
    assertTrue(lightlyArmoured > heavilyArmoured);
    assertTrue(heavilyArmoured >= 0.0);
  }

  @Test
  void negativeArmourAmplifiesDamage() {
    double unarmoured = DamageMath.getDamageAfterArmourReduction(20.0, 0.0);
    double vulnerable = DamageMath.getDamageAfterArmourReduction(20.0, -10.0);

    assertTrue(vulnerable > unarmoured);
  }

  @Test
  void elementalResistanceReducesAndVulnerabilityIncreasesDamage() {
    double neutral = DamageMath.getDamageAfterElementalResistance(16.0, 0.0);
    double resistant = DamageMath.getDamageAfterElementalResistance(16.0, 1.0);
    double vulnerable = DamageMath.getDamageAfterElementalResistance(16.0, -1.0);

    assertTrue(resistant < neutral);
    assertTrue(vulnerable > neutral);
  }

  @Test
  void attackTypeResistanceRespondsMonotonicallyWithinSafeDomain() {
    double neutral = DamageMath.getDamageAfterAttackTypeReduction(18.0, 0.0);
    double resistant = DamageMath.getDamageAfterAttackTypeReduction(18.0, 50.0);
    double vulnerable = DamageMath.getDamageAfterAttackTypeReduction(18.0, -50.0);

    assertTrue(resistant < neutral);
    assertTrue(vulnerable > neutral);
  }

  @Test
  void precisionDamageStaysWithinConfiguredBounds() {
    RandomSource random = RandomSource.create(12345L);

    for (int index = 0; index < 200; index++) {
      double result = DamageMath.getDamageFromPrecisionNormalDistribution(12.0, 4.0, 20.0, random);
      assertTrue(result >= 10.0);
      assertTrue(result <= 20.0);
    }
  }

  @Test
  void higherPrecisionProducesHigherDamageForSameRoll() {
    RandomSource lowPrecisionRandom = RandomSource.create(9876L);
    RandomSource highPrecisionRandom = RandomSource.create(9876L);

    double lowPrecision = DamageMath.getDamageFromPrecisionNormalDistribution(5.0, 2.0, 20.0, lowPrecisionRandom);
    double highPrecision = DamageMath.getDamageFromPrecisionNormalDistribution(30.0, 2.0, 20.0, highPrecisionRandom);

    assertTrue(highPrecision >= lowPrecision);
  }

  @Test
  void higherToughnessProducesLowerDamageForSameRoll() {
    RandomSource lowToughnessRandom = RandomSource.create(2468L);
    RandomSource highToughnessRandom = RandomSource.create(2468L);

    double lowToughness = DamageMath.getDamageFromPrecisionNormalDistribution(20.0, 0.0, 20.0, lowToughnessRandom);
    double highToughness = DamageMath.getDamageFromPrecisionNormalDistribution(20.0, 12.0, 20.0, highToughnessRandom);

    assertTrue(highToughness <= lowToughness);
  }
}