package com.cwjn.skada.util;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Keeps generation's base-item lookup tied to vanilla modifiers. */
class UtilVanillaItemStatsTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void generationBaseLookupUsesVanillaDefaultModifiers() {
        assertEquals(8.0, Util.getDamageModifierForItem(Items.DIAMOND_AXE), 1.0e-9);
        assertEquals(6.0, Util.getDamageModifierForItem(Items.WOODEN_AXE), 1.0e-9);
        assertEquals(1.0, Util.getAttackSpeedForItem(Items.DIAMOND_AXE), 1.0e-9);
        assertEquals(4.0, Util.getAttackSpeedForItem(Items.DIAMOND_HOE), 1.0e-9);
    }
}
