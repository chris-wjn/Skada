package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class WeaponPartEntryTest {

    @Test
    void codecKeepsPlacementOnEntryInsteadOfPart() {
        WeaponPartEntry original = new WeaponPartEntry(
                new HandlePart(12.0, 1.2),
                null,
                new Vec3(4.0, 1.0, -2.0),
                null);

        var encoded = WeaponPartEntry.CODEC.encodeStart(JsonOps.INSTANCE, original).result().orElseThrow();
        WeaponPartEntry decoded = WeaponPartEntry.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

        assertEquals(4.0, decoded.position().x(), 1.0e-9);
        assertEquals(1.0, decoded.position().y(), 1.0e-9);
        assertEquals(-2.0, decoded.position().z(), 1.0e-9);
        assertInstanceOf(HandlePart.class, decoded.part());
    }
}