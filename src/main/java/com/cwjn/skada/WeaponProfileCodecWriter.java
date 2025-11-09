package com.cwjn.skada;// java
import com.cwjn.skada.data.gen.weapon.WeaponProfile;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Utility to write a default mapping to
 * `skada/generator_data/weapon/by_item_name.json` under the config directory,
 * with JSON object keys ordered to match the codec field order for known types.
 */
public final class WeaponProfileCodecWriter {

  private WeaponProfileCodecWriter() {}

  public static void writeDefaultByItemNameJson() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    Map<String, WeaponProfile> map = new HashMap<>();
    map.put("sword", new WeaponProfile());

    WeaponProfile.STRING_MAP_CODEC.encodeStart(JsonOps.INSTANCE, map).result().ifPresent(jsonElement -> {
      // sort object keys deterministically and, for known objects, follow codec field order
      JsonElement ordered = orderJsonAccordingToCodec(jsonElement);
      String json = gson.toJson(ordered);
      Path outDir = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(),
              "skada", "generator_data", "weapon");
      try {
        Files.createDirectories(outDir);
        File outFile = new File(outDir.toFile(), "by_item_name.json");
        FileUtils.write(outFile, json, "UTF-8");
        Skada.LOGGER.info("Wrote default WeaponProfile map to {}", outFile.getAbsolutePath());
      } catch (IOException e) {
        Skada.LOGGER.error("Failed to write by_item_name.json", e);
      }
    });
  }

  // Field orders matching the RecordCodecBuilder group order in WeaponProfile and nested records
  private static final List<String> WEAPON_ORDER = List.of(
          "attackTypes",
          "singleEdged",
          "handleLength",
          "bladeLength",
          "bladeSpineCrossguardThickness",
          "bladeSpineTipShoulderThickness",
          "bladeCrossguardWidth",
          "bladeTipShoulderWidth",
          "pointOfBalance",
          "edgeBevel",
          "primaryBevel",
          "tipSpecifications"
  );

  private static final List<String> TIP_SPEC_ORDER = List.of(
          "tipRadius",
          "tipBevelAngle",
          "tipBevelShoulderAngle"
  );

  private static final List<String> BEVEL_ORDER = List.of(
          "percentageOfBladeWidth",
          "bevelType"
  );

  private static final List<String> EDGEBEVEL_ORDER = List.of(
          "angle",
          "shoulderAngle",
          "bevelType",
          "edgeRadius"
  );

  /**
   * Top-level dispatcher: recursively order the JSON structure, using codec orders for known objects.
   */
  private static JsonElement orderJsonAccordingToCodec(JsonElement element) {
    if (element == null || element.isJsonNull()) return JsonNull.INSTANCE;

    if (element.isJsonObject()) {
      JsonObject obj = element.getAsJsonObject();

      // If this object is a WeaponProfile (has singleEdged), use WEAPON_ORDER for its members.
      if (obj.has("singleEdged")) {
        return orderObjectWithFieldOrder(obj, WEAPON_ORDER);
      }
      // TipSpecifications detection
      if (obj.has("tipRadius") || (obj.has("tipBevelAngle") && obj.has("tipBevelShoulderAngle"))) {
        return orderObjectWithFieldOrder(obj, TIP_SPEC_ORDER);
      }
      // Bevel detection (primaryBevel)
      if (obj.has("percentageOfBladeWidth") && obj.has("bevelType")) {
        return orderObjectWithFieldOrder(obj, BEVEL_ORDER);
      }
      // EdgeBevel detection
      if (obj.has("angle") && (obj.has("edgeRadius") || obj.has("shoulderAngle"))) {
        return orderObjectWithFieldOrder(obj, EDGEBEVEL_ORDER);
      }

      // Generic object: top-level map (e.g. by_item_name) or other unknown objects
      // For top-level maps we want deterministic ordering: sort keys alphabetically,
      // but for inner unknown objects we also sort keys alphabetically for determinism.
      List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(obj.entrySet());
      entries.sort(Comparator.comparing(Map.Entry::getKey));

      JsonObject out = new JsonObject();
      for (Map.Entry<String, JsonElement> e : entries) {
        out.add(e.getKey(), orderJsonAccordingToCodec(e.getValue()));
      }
      return out;
    }

    if (element.isJsonArray()) {
      JsonArray arr = element.getAsJsonArray();
      JsonArray outArr = new JsonArray();
      for (JsonElement e : arr) {
        outArr.add(orderJsonAccordingToCodec(e));
      }
      return outArr;
    }

    // Primitive or other - return a safe copy
    if (element.isJsonPrimitive()) {
      JsonPrimitive p = element.getAsJsonPrimitive();
      if (p.isNumber()) return new JsonPrimitive(p.getAsNumber());
      if (p.isBoolean()) return new JsonPrimitive(p.getAsBoolean());
      return new JsonPrimitive(p.getAsString());
    }

    return element;
  }

  /**
   * Build a new JsonObject where members from \`fieldOrder\` appear first in that order (if present),
   * followed by any remaining members sorted alphabetically. Values are recursively ordered.
   */
  private static JsonObject orderObjectWithFieldOrder(JsonObject obj, List<String> fieldOrder) {
    JsonObject out = new JsonObject();

    // Add fields in the specified order if present
    for (String key : fieldOrder) {
      if (obj.has(key)) {
        out.add(key, orderJsonAccordingToCodec(obj.get(key)));
      }
    }

    // Collect remaining keys and sort them
    List<String> remaining = new ArrayList<>();
    for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
      if (!fieldOrder.contains(e.getKey())) remaining.add(e.getKey());
    }
    remaining.sort(String::compareTo);

    for (String key : remaining) {
      out.add(key, orderJsonAccordingToCodec(obj.get(key)));
    }

    return out;
  }
}
