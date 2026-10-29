package com.cwjn.skada.util;

import com.cwjn.skada.data.SkadaData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EquipmentWeightUtilTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void sumsOnlyExplicitlyEquippedStacksAndCountsEachSlot() {
        ItemStack helmet = armourStack(0.7);
        ItemStack chest = armourStack(4.2);
        ItemStack held = armourStack(1.1);
        ItemStack inventoryOnly = armourStack(9.0);

        // The iterable is built from the six equipment slots; inventoryOnly is not part of it.
        assertEquals(6.0, EquipmentWeightUtil.sumEquippedStacks(
                List.of(helmet, chest, held, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)), 1.0e-9);
        assertEquals(1.4, EquipmentWeightUtil.sumEquippedStacks(List.of(helmet, helmet)), 1.0e-9);
        assertEquals(9.0, EquipmentWeightUtil.getStackWeightKg(inventoryOnly), 1.0e-9);
    }

    private static ItemStack armourStack(double weightKg) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        CompoundTag armour = new CompoundTag();
        armour.putDouble("weight_kg", weightKg);
        stack.getOrCreateTag().put(SkadaData.ARMOUR_INFO_TAG_KEY, armour);
        return stack;
    }
}
