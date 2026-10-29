package com.cwjn.skada.generation.weapon;

import com.cwjn.skada.generation.weapon.generation_algo.KnockbackGenerationUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KnockbackGenerationUtilTest {

    @Test
    void mapsTransferredImpulseToFiniteNonNegativeVanillaScale() {
        var zero = KnockbackGenerationUtil.fromPhysicalInputs(0.0, 8.0, 0.6);
        var estimate = KnockbackGenerationUtil.fromPhysicalInputs(0.5, 8.0, 0.6);
        var extreme = KnockbackGenerationUtil.fromPhysicalInputs(Double.MAX_VALUE, Double.MAX_VALUE, 1.0);

        assertEquals(0.0, zero.transferredImpulseNs());
        assertEquals(0.0, zero.attackKnockback());
        assertEquals(2.4, estimate.transferredImpulseNs(), 1e-9);
        assertTrue(estimate.attackKnockback() > 0.0);
        assertTrue(Double.isFinite(extreme.transferredImpulseNs()));
        assertTrue(Double.isFinite(extreme.attackKnockback()));
        assertTrue(extreme.attackKnockback() <= KnockbackGenerationUtil.MAX_ATTACK_KNOCKBACK);
    }

    @Test
    void clampsContactTransferToItsBoundedRange() {
        var estimate = KnockbackGenerationUtil.fromPhysicalInputs(1.0, 4.0, 5.0);

        assertEquals(KnockbackGenerationUtil.MAX_TRANSFER_FACTOR, estimate.transferFactor());
    }
}
