package com.cwjn.skada.data.damage;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.generation.weapon.attack_capability.NoneCapable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AttackSelectionKnockbackMigrationTest {
    private static Supplier<IForgeRegistry<AttackType>> oldAttacks;
    private static AttackType strike;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void setupRegistry() {
        oldAttacks = SkadaData.REGISTRY_ATTACK_TYPE;
        strike = new AttackType("strike", null, null, null, NoneCapable.class);
        IForgeRegistry<AttackType> attacks = mock(IForgeRegistry.class);
        when(attacks.getValue(new ResourceLocation("skada:strike"))).thenReturn(strike);
        SkadaData.REGISTRY_ATTACK_TYPE = () -> attacks;
    }

    @AfterAll
    static void restoreRegistry() {
        SkadaData.REGISTRY_ATTACK_TYPE = oldAttacks;
    }

    @Test
    void missingSavedKnockbackInheritsCurrentItemDefaultAndPreservesOtherStats() {
        CompoundTag saved = savedAttack(null);

        AttackSelection.migrateMissingKnockback(saved, Map.of(strike, attack(0.8)));

        CompoundTag migrated = saved.getCompound("attack_types").getCompound("skada:strike");
        assertEquals(0.8, migrated.getDouble("knockback"));
        assertEquals(21.0, migrated.getDouble("lethality"));
        assertEquals(3.0, migrated.getDouble("damage"));
    }

    @Test
    void explicitSavedZeroSurvivesDefaultMigration() {
        CompoundTag saved = savedAttack(0.0);

        AttackSelection.migrateMissingKnockback(saved, Map.of(strike, attack(0.8)));

        assertEquals(0.0, saved.getCompound("attack_types").getCompound("skada:strike").getDouble("knockback"));
    }

    @Test
    void missingValueWithoutCurrentDefaultGetsZero() {
        CompoundTag saved = savedAttack(null);

        AttackSelection.migrateMissingKnockback(saved, Map.of());

        assertEquals(0.0, saved.getCompound("attack_types").getCompound("skada:strike").getDouble("knockback"));
    }

    private static AttackTypeInfo attack(double knockback) {
        return AttackTypeInfo.of(1, 2, 0, 3, 1, 1, 0, knockback, List.of());
    }

    private static CompoundTag savedAttack(Double knockback) {
        CompoundTag attack = new CompoundTag();
        attack.putDouble("lethality", 21);
        attack.putDouble("precision", 11);
        attack.putDouble("minReach", 0.2);
        attack.putDouble("maxReach", 2.4);
        attack.putDouble("attackSpeed", 1.1);
        attack.putDouble("damage", 3);
        attack.putDouble("failChance", 0.03);
        if (knockback != null) attack.putDouble("knockback", knockback);
        CompoundTag attacks = new CompoundTag();
        attacks.put("skada:strike", attack);
        CompoundTag saved = new CompoundTag();
        saved.put("attack_types", attacks);
        return saved;
    }
}
