package com.cwjn.skada.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilCriticalFailTest {

  @Test
  void criticalFailSeverityReturnsNoneWhenTriggerRollMisses() {
    Util.CriticalFailSeverity severity = Util.criticalFailSeverity(0.02, 0.8, 0.1, 0, 0);

    assertEquals(Util.CriticalFailSeverity.NONE, severity);
  }

  @Test
  void criticalFailSeverityDefaultsToEdgeDamageBeforeEscalation() {
    Util.CriticalFailSeverity severity = Util.criticalFailSeverity(0.02, 0.0, 0.95, 0, 0);

    assertEquals(Util.CriticalFailSeverity.EDGE_DAMAGE, severity);
  }

  @Test
  void criticalFailSeverityEscalatesWithStoredDamageHistory() {
    Util.CriticalFailSeverity deformation = Util.criticalFailSeverity(0.02, 0.0, 0.10, 3, 0);
    Util.CriticalFailSeverity catastrophic = Util.criticalFailSeverity(0.02, 0.0, 0.01, 4, 3);

    assertEquals(Util.CriticalFailSeverity.DEFORMATION, deformation);
    assertEquals(Util.CriticalFailSeverity.CATASTROPHIC, catastrophic);
  }

  @Test
  void criticalFailLossUsesMaxDurabilityAndCapsAtRemaining() {
    assertEquals(3, Util.criticalFailDurabilityLoss(250, 250, 0.01));
    assertEquals(38, Util.criticalFailDurabilityLoss(250, 250, 0.15));
    assertEquals(12, Util.criticalFailDurabilityLoss(250, 12, 1.0));
  }

  @Test
  void projectileVelocityWithDamageBonusIsAdditiveAndNonNegative() {
    assertEquals(3.2, Util.projectileVelocityWithDamageBonus(3.0, 0.2), 1.0e-9);
    assertEquals(0.0, Util.projectileVelocityWithDamageBonus(0.1, -1.0), 1.0e-9);
  }
}