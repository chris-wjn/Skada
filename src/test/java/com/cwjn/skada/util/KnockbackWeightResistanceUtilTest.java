package com.cwjn.skada.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Verifies the weight curve and its separately identified attribute modifier. */
class KnockbackWeightResistanceUtilTest {
    /** Initializes Minecraft registries required by the vanilla attribute instances in this suite. */
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** Confirms increasing mass has diminishing returns and never grants immunity alone. */
    @Test
    void weightCurveIsMonotoneDiminishingAndBelowImmunity() {
        double oneKg = KnockbackWeightResistanceUtil.resistanceForWeightKg(1.0);
        double reference = KnockbackWeightResistanceUtil.resistanceForWeightKg(70.0);
        double heavy = KnockbackWeightResistanceUtil.resistanceForWeightKg(300.0);
        double extreme = KnockbackWeightResistanceUtil.resistanceForWeightKg(Double.MAX_VALUE);

        assertEquals(0.006338028169014085, oneKg, 1.0e-12);
        assertEquals(0.225, reference, 1.0e-12);
        assertEquals(0.36486486486486486, heavy, 1.0e-12);
        assertTrue(oneKg < reference && reference < heavy && heavy < extreme);
        assertTrue(extreme <= KnockbackWeightResistanceUtil.MAX_WEIGHT_RESISTANCE);
        assertEquals(0.0, KnockbackWeightResistanceUtil.resistanceForWeightKg(0.0));
        assertEquals(0.0, KnockbackWeightResistanceUtil.resistanceForWeightKg(-1.0));
        assertEquals(0.0, KnockbackWeightResistanceUtil.resistanceForWeightKg(Double.NaN));
        assertEquals(0.0, KnockbackWeightResistanceUtil.resistanceForWeightKg(Double.POSITIVE_INFINITY));
    }

    /** Confirms the weight modifier coexists with other sources and updates without duplication. */
    @Test
    void derivedModifierUpdatesWithoutReplacingOtherSourcesOrDuplicating() {
        AttributeInstance resistance = new AttributeInstance(Attributes.KNOCKBACK_RESISTANCE, ignored -> {});
        UUID innateId = UUID.randomUUID();
        resistance.addTransientModifier(new AttributeModifier(
                innateId, "innate", 0.2, AttributeModifier.Operation.ADDITION));

        KnockbackWeightResistanceUtil.updateModifier(resistance,
                KnockbackWeightResistanceUtil.resistanceForWeightKg(70.0));
        KnockbackWeightResistanceUtil.updateModifier(resistance,
                KnockbackWeightResistanceUtil.resistanceForWeightKg(70.0));
        assertEquals(0.425, resistance.getValue(), 1.0e-12);
        assertEquals(2, resistance.getModifiers().size());
        assertTrue(resistance.getModifier(innateId) != null);

        KnockbackWeightResistanceUtil.updateModifier(resistance,
                KnockbackWeightResistanceUtil.resistanceForWeightKg(300.0));
        assertEquals(0.5648648648648649, resistance.getValue(), 1.0e-12);
        assertEquals(2, resistance.getModifiers().size());
        assertTrue(resistance.getModifier(innateId) != null);

        KnockbackWeightResistanceUtil.updateModifier(resistance, 0.0);
        assertEquals(0.2, resistance.getValue(), 1.0e-12);
        assertFalse(resistance.getModifier(KnockbackWeightResistanceUtil.MODIFIER_ID) != null);
        assertTrue(resistance.getModifier(innateId) != null);
    }
}
