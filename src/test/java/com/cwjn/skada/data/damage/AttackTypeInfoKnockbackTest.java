package com.cwjn.skada.data.damage;

import com.cwjn.skada.generation.attack.AttackTypeJsonInfo;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttackTypeInfoKnockbackTest {

    @Test
    void codecAndNbtRoundTripExplicitKnockbackAndPreserveZero() {
        AttackTypeInfo authored = AttackTypeInfo.of(10, 12, 0.1, 2.5, 1.2, 3, 0.04, 0.0, List.of("punch"));

        AttackTypeInfo decoded = AttackTypeInfo.CODEC.parse(JsonOps.INSTANCE,
                AttackTypeInfo.CODEC.encodeStart(JsonOps.INSTANCE, authored).result().orElseThrow())
                .result().orElseThrow();
        AttackTypeInfo fromNbt = AttackTypeInfo.fromCompoundTag(authored.toCompoundTag());

        assertEquals(0.0, decoded.knockback());
        assertEquals(0.0, fromNbt.knockback());
        assertEquals(3.0, fromNbt.damage());
        assertEquals(List.of("punch"), fromNbt.reticleShapes());
    }

    @Test
    void legacyJsonWithoutKnockbackDefaultsToZero() {
        var legacy = JsonParser.parseString("""
                {"lethality":10,"precision":12,"minReach":0.1,"maxReach":2.5,
                 "attackSpeed":1.2,"damage":3,"failChance":0.04}
                """);

        AttackTypeInfo decoded = AttackTypeInfo.CODEC.parse(JsonOps.INSTANCE, legacy).result().orElseThrow();

        assertEquals(0.0, decoded.knockback());
    }

    @Test
    void legacyNbtCanBeDistinguishedForDefaultMigration() {
        CompoundTag old = new CompoundTag();
        old.putDouble("lethality", 10);
        old.putDouble("precision", 12);
        old.putDouble("minReach", 0.1);
        old.putDouble("maxReach", 2.5);
        old.putDouble("attackSpeed", 1.2);
        old.putDouble("damage", 3);
        old.putDouble("failChance", 0.04);

        assertFalse(old.contains("knockback"));
        assertEquals(0.0, AttackTypeInfo.fromCompoundTag(old).knockback());
    }

    @Test
    void attackTypeAuthoringCodecAcceptsExplicitZeroOverride() {
        var json = JsonParser.parseString("""
                {"minReach":0,"maxReach":3,"knockback":0}
                """);

        AttackTypeJsonInfo authored = AttackTypeJsonInfo.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();

        assertEquals(0.0, authored.knockback());
    }
}
