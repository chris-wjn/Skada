package com.cwjn.skada.data.gen.armour;

import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmourCodecTest {

  private static final Path DATA_ROOT = Path.of("src", "main", "resources", "data", "skada");

  @BeforeAll
  static void bootstrap() {
    ArmourGenerationBootstrap.bootstrapRegistries();
  }

  @Test
  void sharedMaterialJsonDecodesFromCanonicalPath() throws IOException {
    JsonObject json = JsonParser.parseString(
        Files.readString(DATA_ROOT.resolve(Path.of("generator_data", "material", "minecraft.iron.json"))))
        .getAsJsonObject();

    MaterialInfo material = MaterialInfo.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();

    assertEquals(7.874, material.density(), 1.0e-9);
    assertEquals(4.0, material.hardness(), 1.0e-9);
  }

  @Test
  void armourConstructionCodecDefaultsBurdenMultiplier() {
    JsonObject json = JsonParser.parseString("""
        {
          "thickness_mm": 4.0,
          "layer_count": 2,
          "padding_mm": 2.0,
          "rigidity": 0.6,
          "seam_quality": 0.8,
          "gap_exposure": 0.2,
          "curvature": 0.5,
          "articulation_penalty": 0.1
        }
        """).getAsJsonObject();

    ArmourConstructionInfo info = ArmourConstructionInfo.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();

    assertEquals(1.0, info.burdenMultiplier(), 1.0e-9);
  }

  @Test
  void bundledItemMappingsDecodeNewSharedMaterialShape() throws IOException {
    JsonObject json = JsonParser.parseString(
        Files.readString(DATA_ROOT.resolve(Path.of("generator_data", "armour", "by_item_name.json"))))
        .getAsJsonObject();

    Map<String, ArmourItemMapping> mappings = ArmourItemMapping.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, json).result()
        .orElseThrow();

    ArmourItemMapping diamondChest = mappings.get("diamond_chestplate");
    assertEquals("diamond", diamondChest.material());
    assertEquals("plate_refined", diamondChest.construction());
    assertEquals("chestplate", diamondChest.piece());
  }

  @Test
  void bundledRuntimeArmourInfoDecodesBurdenField() throws IOException {
    JsonObject json = JsonParser.parseString(
        Files.readString(DATA_ROOT.resolve(Path.of("armour_info", "minecraft.json"))))
        .getAsJsonObject();

    Map<String, ArmourInfo> armourInfos = ArmourInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();

    assertTrue(armourInfos.get("iron_chestplate").burden() > 0.0);
    assertTrue(armourInfos.get("netherite_chestplate").burden() > armourInfos.get("diamond_chestplate").burden());
  }
}