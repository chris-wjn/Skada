package com.cwjn.skada.util;

import com.cwjn.skada.data.gen.JsonUtil;
import com.cwjn.skada.data.gen.weapon.parts.HandlePart;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class UtilWeaponAssemblyResolutionTest {

    @Test
    void resolvesNamedPartsAndSchemaStyleTransformsBeforeAssemblyDecode() {
        JsonObject wrappedLocalPart = JsonParser.parseString("""
                {
                  "part": {
                    "type": "handle",
                    "length": 13.0,
                    "radius": 1.3
                  }
                }
                """).getAsJsonObject();

        JsonObject externalPart = JsonParser.parseString("""
                {
                  "type": "handle",
                  "length": 9.0,
                  "radius": 0.9
                }
                """).getAsJsonObject();

        JsonObject assemblyJson = JsonParser.parseString("""
                {
                  "parts": [
                    {
                      "part": "flamberge_blade",
                      "position": { "x": 13.0, "y": 0.0, "z": 0.0 },
                      "transform": {
                        "x": { "axis": "y", "sign": 1 },
                        "y": { "axis": "x", "sign": 1 },
                        "z": { "axis": "z", "sign": 1 }
                      }
                    },
                    {
                      "part": "othermod:grip",
                      "position": { "x": 0.0, "y": 0.0, "z": 0.0 }
                    }
                  ],
                  "attack_types": {}
                }
                """).getAsJsonObject();

        Map<String, JsonObject> partMap = Map.of(
                "skada:flamberge_blade", JsonUtil.normalizeWeaponPartDefinitionJson(wrappedLocalPart),
                "othermod:grip", JsonUtil.normalizeWeaponPartDefinitionJson(externalPart));

        JsonObject resolvedAssemblyJson = JsonUtil.resolveWeaponAssemblyPartReferences(assemblyJson, "skada", partMap);
        JsonArray resolvedParts = resolvedAssemblyJson.getAsJsonArray("parts");
        WeaponPartEntry firstEntry = WeaponPartEntry.CODEC.parse(JsonOps.INSTANCE, resolvedParts.get(0)).result().orElseThrow();
        WeaponPartEntry secondEntry = WeaponPartEntry.CODEC.parse(JsonOps.INSTANCE, resolvedParts.get(1)).result().orElseThrow();

        assertEquals(2, resolvedParts.size());
        assertInstanceOf(HandlePart.class, firstEntry.part());
        assertInstanceOf(HandlePart.class, secondEntry.part());
        assertEquals(WeaponAxis.Y, firstEntry.transform().xMap().localAxis());
        assertEquals(WeaponAxis.X, firstEntry.transform().yMap().localAxis());
        assertEquals(WeaponAxis.Z, firstEntry.transform().zMap().localAxis());
        assertEquals(13.0, firstEntry.position().x(), 1.0e-9);
        assertEquals(13.0, ((HandlePart) firstEntry.part()).length(), 1.0e-9);
        assertEquals(9.0, ((HandlePart) secondEntry.part()).length(), 1.0e-9);
    }
}