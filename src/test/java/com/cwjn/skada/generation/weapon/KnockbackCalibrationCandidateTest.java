package com.cwjn.skada.generation.weapon;

import com.cwjn.skada.generation.weapon.generation_algo.KnockbackGenerationUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the ticket-04 resistance proposal and dimensional examples without activating it in gameplay. */
class KnockbackCalibrationCandidateTest {

    private static final double MAX_WEIGHT_RESISTANCE = 0.45;
    private static final double REFERENCE_WEIGHT_KG = 70.0;

    @Test
    void weightResistanceHasDiminishingReturnsAndNeverReachesFullImmunity() {
        double[] weights = {0.0, 1.0, 35.0, 70.0, 300.0, 1000.0, 1.0e12};
        double previous = 0.0;
        for (double weight : weights) {
            double resistance = weightResistance(weight);
            assertTrue(resistance >= previous, "weight resistance must be monotonic");
            assertTrue(resistance < MAX_WEIGHT_RESISTANCE, "weight alone must remain below 0.45");
            previous = resistance;
        }
        assertEquals(0.0, weightResistance(0.0), 1.0e-12);
        assertEquals(0.0, weightResistance(-1.0), 1.0e-12);
        assertEquals(0.0, weightResistance(Double.NaN), 1.0e-12);
        assertEquals(0.0, weightResistance(Double.NEGATIVE_INFINITY), 1.0e-12);
        assertEquals(0.0, weightResistance(Double.POSITIVE_INFINITY), 1.0e-12);
        assertEquals(0.225, weightResistance(70.0), 1.0e-12);
        assertEquals(0.45, weightResistance(1.0e12), 1.0e-9);
    }

    @Test
    void candidateAttributeCombinationUsesVanillaOperationOrderAndClamp() {
        assertEquals(0.984375, combineResistance(0.1, 0.425, 0.5, 0.25), 1.0e-12);
        assertEquals(1.0, combineResistance(0.6, 0.5, 0.0, 0.0), 1.0e-12);
        assertEquals(0.0, combineResistance(-2.0, 0.0, 0.0, 0.0), 1.0e-12);
    }

    @Test
    void targetWeightReducesTheSameSourceOnlyThroughResistance() {
        double sourceKnockback = 1.0;
        double lightTargetResult = sourceKnockback * (1.0 - weightResistance(2.0));
        double referenceTargetResult = sourceKnockback * (1.0 - weightResistance(70.0));

        assertEquals(0.9875, lightTargetResult, 1.0e-12);
        assertEquals(0.775, referenceTargetResult, 1.0e-12);
        assertTrue(referenceTargetResult < lightTargetResult);
    }

    @Test
    void sourceImpulseKeepsSiUnitsZeroAndFiniteCap() {
        KnockbackGenerationUtil.Estimate oneKilogramAtOneMetrePerSecond =
                KnockbackGenerationUtil.fromPhysicalInputs(1.0, 1.0, 0.5);
        assertEquals(0.5, oneKilogramAtOneMetrePerSecond.transferredImpulseNs(), 1.0e-12);
        assertEquals(0.125, oneKilogramAtOneMetrePerSecond.attackKnockback(), 1.0e-12);
        assertEquals(0.0, KnockbackGenerationUtil.fromPhysicalInputs(0.0, 50.0, 0.5)
                .attackKnockback(), 1.0e-12);
        assertEquals(KnockbackGenerationUtil.MAX_ATTACK_KNOCKBACK,
                KnockbackGenerationUtil.fromPhysicalInputs(Double.MAX_VALUE, Double.MAX_VALUE, 0.85)
                        .attackKnockback(), 1.0e-12);
    }

    @Test
    void combinedPostBonusValueAppliesChargeAndReachBeforeTheFourPointCap() {
        double generated = 1.078842480;
        assertEquals(0.0, combinedOrdinary(generated, 1.0, 0.0, 0.0, 0.5), 1.0e-9);
        assertEquals(0.431536992, combinedOrdinary(generated, 1.0, 0.0, 0.0, 0.7), 1.0e-9);
        assertEquals(4.0, combinedOrdinary(generated, 1.0, 1.0, 2.0, 1.0), 1.0e-12,
                "generated + charged sprint + Knockback II reaches and is clamped to 4");
        assertEquals(3.670958232, combinedOrdinary(generated, 1.0, 1.0, 2.0, 0.95), 1.0e-9);
        assertEquals(3.269710620, combinedOrdinary(generated, 0.25, 1.0, 2.0, 1.0), 1.0e-9,
                "minimum reach scales only generated knockback before separate bonuses");
        assertEquals(4.0, combinedOrdinary(2.0, 1.0, 1.0, 3.0, 1.0), 1.0e-12,
                "command-authored Knockback III stress stack remains bounded");
    }

    /** Candidate additive modifier: asymptotes to 0.45 around a 70 kg reference mass. */
    private static double weightResistance(double totalWeightKg) {
        if (!Double.isFinite(totalWeightKg) || totalWeightKg <= 0.0) return 0.0;
        return MAX_WEIGHT_RESISTANCE * totalWeightKg / (totalWeightKg + REFERENCE_WEIGHT_KG);
    }

    /** Mirrors vanilla attribute modifiers: base/additive, multiply-base, multiply-total, then clamp. */
    private static double combineResistance(double base, double additive, double multiplyBase, double multiplyTotal) {
        double value = (base + additive) * (1.0 + multiplyBase) * (1.0 + multiplyTotal);
        return Math.max(0.0, Math.min(1.0, value));
    }

    /** Candidate charge, generated-component reach scale, bonuses, and final combined cap. */
    private static double combinedOrdinary(double generated, double reachFactor, double sprintBonus,
                                           double enchantmentBonus, double charge) {
        double chargeFactor = charge <= 0.5 ? 0.0 : (charge - 0.5) / 0.5;
        double combined = Math.max(0.0, generated) * Math.max(0.0, reachFactor)
                + Math.max(0.0, sprintBonus) + Math.max(0.0, enchantmentBonus);
        return Math.min(4.0, combined * chargeFactor);
    }
}
