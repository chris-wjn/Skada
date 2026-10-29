package com.cwjn.skada.data.armour;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmourInfoWeightTest {
    @Test
    void weightRoundTripsWhileLegacyCompoundsDefaultToZero() {
        ArmourInfo authored = new ArmourInfo(Map.of(), Map.of(), 3, 4, 0.5, 0.0);
        ArmourInfo restored = ArmourInfo.fromCompoundTag(authored.toCompoundTag());
        assertEquals(0.0, restored.weightKg());

        CompoundTag legacy = new CompoundTag();
        legacy.putDouble("armour", 3.0);
        legacy.putDouble("armour_toughness", 4.0);
        legacy.putDouble("burden", 0.5);
        assertEquals(0.0, ArmourInfo.fromCompoundTag(legacy).weightKg());
        assertEquals(3.0, ArmourInfo.fromCompoundTag(legacy).armour());
        assertEquals(0.5, ArmourInfo.fromCompoundTag(legacy).burden());
    }
}
