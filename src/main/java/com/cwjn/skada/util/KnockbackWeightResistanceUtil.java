package com.cwjn.skada.util;

import java.util.UUID;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Computes and maintains the additive knockback resistance derived from total physical weight. */
public final class KnockbackWeightResistanceUtil {
    public static final UUID MODIFIER_ID = UUID.fromString("92f30cb5-897f-469c-b06c-64a03647db23");
    public static final double REFERENCE_WEIGHT_KG = 70.0;
    public static final double MAX_WEIGHT_RESISTANCE = 0.45;
    private static final double CHANGE_EPSILON = 1.0e-9;

    /** Prevents construction of this stateless resistance utility. */
    private KnockbackWeightResistanceUtil() {}

    /** Weight contribution approved in calibration: 0.45 × W / (W + 70 kg). */
    public static double resistanceForWeightKg(double totalWeightKg) {
        if (!Double.isFinite(totalWeightKg) || totalWeightKg <= 0.0) return 0.0;
        return MAX_WEIGHT_RESISTANCE * totalWeightKg / (totalWeightKg + REFERENCE_WEIGHT_KG);
    }

    /** Keeps one transient, separately identified additive modifier on the entity. */
    public static void updateModifier(LivingEntity entity, double totalWeightKg) {
        AttributeInstance resistance = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance == null) return;
        updateModifier(resistance, resistanceForWeightKg(totalWeightKg));
    }

    /** Updates the derived modifier without disturbing any other modifier on this attribute. */
    public static void updateModifier(AttributeInstance resistance, double amount) {
        double normalized = Double.isFinite(amount) && amount > 0.0
                ? Math.min(MAX_WEIGHT_RESISTANCE, amount)
                : 0.0;
        AttributeModifier existing = resistance.getModifier(MODIFIER_ID);
        if (existing != null && Math.abs(existing.getAmount() - normalized) <= CHANGE_EPSILON) return;
        if (existing != null) resistance.removeModifier(MODIFIER_ID);
        if (normalized > 0.0) {
            resistance.addTransientModifier(new AttributeModifier(
                    MODIFIER_ID, "skada.weight_knockback_resistance", normalized,
                    AttributeModifier.Operation.ADDITION));
        }
    }
}
