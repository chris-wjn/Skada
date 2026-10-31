package com.cwjn.skada.damage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/** Verifies the scalar bounds used by ordinary mob melee. */
class OrdinaryMeleeKnockbackUtilTest {
    /** Confirms mob attack knockback remains finite, non-negative, and capped. */
    @Test
    void mobImpulseScaleIsFiniteNonNegativeAndBounded() {
        assertEquals(0.0, OrdinaryMeleeKnockbackUtil.boundedKnockback(Double.NaN));
        assertEquals(0.0, OrdinaryMeleeKnockbackUtil.boundedKnockback(Double.NEGATIVE_INFINITY));
        assertEquals(0.0, OrdinaryMeleeKnockbackUtil.boundedKnockback(-1.0));
        assertEquals(0.0, OrdinaryMeleeKnockbackUtil.boundedKnockback(0.0));
        assertEquals(1.5, OrdinaryMeleeKnockbackUtil.boundedKnockback(1.5));
        assertEquals(4.0, OrdinaryMeleeKnockbackUtil.boundedKnockback(100.0));
        assertEquals(4.0, OrdinaryMeleeKnockbackUtil.boundedKnockback(Double.POSITIVE_INFINITY));
    }
}
