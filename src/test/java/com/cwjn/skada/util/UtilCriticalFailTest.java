package com.cwjn.skada.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilCriticalFailTest {

  @Test
  void criticalFailSeverityReturnsNoneWhenTriggerRollMisses() {
    UtilCombat.CriticalFailSeverity severity = UtilCombat.criticalFailSeverity(0.02, 0.8, 0.1, 0, 0);

    assertEquals(UtilCombat.CriticalFailSeverity.NONE, severity);
  }

  @Test
  void criticalFailSeverityDefaultsToEdgeDamageBeforeEscalation() {
    UtilCombat.CriticalFailSeverity severity = UtilCombat.criticalFailSeverity(0.02, 0.0, 0.95, 0, 0);

    assertEquals(UtilCombat.CriticalFailSeverity.EDGE_DAMAGE, severity);
  }

  @Test
  void criticalFailSeverityEscalatesWithStoredDamageHistory() {
    UtilCombat.CriticalFailSeverity deformation = UtilCombat.criticalFailSeverity(0.02, 0.0, 0.10, 3, 0);
    UtilCombat.CriticalFailSeverity catastrophic = UtilCombat.criticalFailSeverity(0.02, 0.0, 0.01, 4, 3);

    assertEquals(UtilCombat.CriticalFailSeverity.DEFORMATION, deformation);
    assertEquals(UtilCombat.CriticalFailSeverity.CATASTROPHIC, catastrophic);
  }

  @Test
  void criticalFailLossUsesMaxDurabilityAndCapsAtRemaining() {
    assertEquals(3, UtilCombat.criticalFailDurabilityLoss(250, 250, 0.01));
    assertEquals(38, UtilCombat.criticalFailDurabilityLoss(250, 250, 0.15));
    assertEquals(12, UtilCombat.criticalFailDurabilityLoss(250, 12, 1.0));
  }

  @Test
  void projectileVelocityWithDamageBonusIsAdditiveAndNonNegative() {
    assertEquals(3.2, UtilCombat.projectileVelocityWithDamageBonus(3.0, 0.2), 1.0e-9);
    assertEquals(0.0, UtilCombat.projectileVelocityWithDamageBonus(0.1, -1.0), 1.0e-9);
  }
}