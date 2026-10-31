package com.cwjn.skada.damage;

import net.minecraft.util.Mth;

/** Applies the approved player charge, reach, bonus, and cap rules to an ordinary melee impulse. */
public final class PlayerKnockbackUtil {
    public static final double MAX_COMBINED_KNOCKBACK = 4.0;
    private static final double CHARGE_THRESHOLD = 0.5;
    private static final double SPRINT_CHARGE_THRESHOLD = 0.9;

    /** Prevents construction of this stateless calculation utility. */
    private PlayerKnockbackUtil() {}

    /** Combines generated, attribute, enchantment, and sprint push under the charge, reach, and cap rules. */
    public static double combinedKnockback(double generated, double otherAttackAttribute, int enchantmentBonus,
                                           boolean sprinting, double charge, boolean insideMinimumReach,
                                           double ineffectiveReachMultiplier) {
        double safeCharge = Double.isFinite(charge) ? Mth.clamp(charge, 0.0, 1.0) : 0.0;
        if (safeCharge <= CHARGE_THRESHOLD) return 0.0;

        double generatedComponent = finiteNonNegative(generated);
        if (insideMinimumReach) {
            double reachMultiplier = Double.isFinite(ineffectiveReachMultiplier)
                    ? Mth.clamp(ineffectiveReachMultiplier, 0.0, 1.0) : 0.0;
            generatedComponent *= reachMultiplier;
        }
        double otherAttribute = finiteNonNegative(otherAttackAttribute);
        double enchantment = Math.max(0, enchantmentBonus);
        double sprint = sprinting && safeCharge > SPRINT_CHARGE_THRESHOLD ? 1.0 : 0.0;
        double chargeMultiplier = (safeCharge - CHARGE_THRESHOLD) / (1.0 - CHARGE_THRESHOLD);
        double combined = (generatedComponent + otherAttribute + enchantment + sprint) * chargeMultiplier;
        if (!Double.isFinite(combined)) return MAX_COMBINED_KNOCKBACK;
        return Mth.clamp(combined, 0.0, MAX_COMBINED_KNOCKBACK);
    }

    /** Converts invalid or negative knockback inputs to zero. */
    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}
