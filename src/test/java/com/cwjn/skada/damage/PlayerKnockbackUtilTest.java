package com.cwjn.skada.damage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies player charge, reach, bonus, and impulse-cap calculations. */
class PlayerKnockbackUtilTest {
    /** Checks the zero-charge boundary and continuous interpolation above it. */
    @Test
    void chargeBoundaryScalesTheFullCombinedPush() {
        assertEquals(0.0, PlayerKnockbackUtil.combinedKnockback(1.0, 0.0, 1, false, 0.5, false, 0.25));
        assertEquals(1.0, PlayerKnockbackUtil.combinedKnockback(1.0, 0.0, 1, false, 0.75, false, 0.25));
        assertEquals(2.0, PlayerKnockbackUtil.combinedKnockback(1.0, 0.0, 1, false, 1.0, false, 0.25));
    }

    /** Checks that reach, sprint timing, and independent bonuses retain their separate rules. */
    @Test
    void minimumReachScalesOnlyGeneratedComponentAndSprintRequiresOverNinetyPercent() {
        assertEquals(1.0, PlayerKnockbackUtil.combinedKnockback(1.0, 0.0, 1, true, 0.9, true, 0.25));
        assertEquals(1.845, PlayerKnockbackUtil.combinedKnockback(1.0, 0.0, 1, true, 0.91, true, 0.25), 1.0e-12);
    }

    /** Ensures invalid values cannot produce an impulse above the approved cap. */
    @Test
    void combinedKnockbackIsFiniteNonnegativeAndCappedAfterBonuses() {
        assertEquals(4.0, PlayerKnockbackUtil.combinedKnockback(Double.MAX_VALUE, 0.0, 3, true, 1.0, false, 0.25));
        assertEquals(0.0, PlayerKnockbackUtil.combinedKnockback(-1.0, Double.NaN, -1, false, Double.NaN, false, 0.25));
    }
}
