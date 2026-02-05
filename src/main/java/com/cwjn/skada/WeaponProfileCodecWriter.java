package com.cwjn.skada;// java
import com.cwjn.skada.data.gen.attack.AttackTypeJsonInfo;
import com.cwjn.skada.data.gen.weapon.old_system.WeaponProfile;
import com.cwjn.skada.data.gen.weapon.parts.Handle;
import com.cwjn.skada.data.gen.weapon.parts.WeaponHead;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Utility to write a default mapping to
 * `skada/generator_data/weapon/sword.json` under the config directory,
 * with JSON object keys ordered to match the codec field order for known types.
 *
 * Note: When run inside a Forge environment, this writes to the Forge config
 * directory (FMLPaths.CONFIGDIR). When run outside Forge (for example from an
 * IDE JVM), it falls back to the current working directory.
 */
public final class WeaponProfileCodecWriter {

  private WeaponProfileCodecWriter() {}

  /**
   * Encode the default {@link WeaponProfile#WeaponProfile()} and write it to
   * <config>/skada/generator_data/weapon/sword.json as pretty-printed JSON.
   * @return path to the written file
   */
  public static Path writeDefaultSwordJson() throws IOException {
    WeaponProfile profile = WeaponProfile.axeTest();
    System.out.println("Starting encoding WeaponProfile");
    // Encode the profile to a Gson JsonElement using Mojang's JsonOps
    DataResult<com.google.gson.JsonElement> result = WeaponProfile.CODEC.encodeStart(JsonOps.INSTANCE, profile);
    System.out.println("Finished encoding WeaponProfile");
    Optional<com.google.gson.JsonElement> maybeJson = result.result();
    if (maybeJson.isEmpty()) {
      String err = result.error().map(Object::toString).orElse("Unknown error during encoding");
      throw new IOException("Failed to encode WeaponProfile: " + err);
    }

    com.google.gson.JsonElement jsonElem = maybeJson.get();

    // Pretty print using Gson
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String pretty = gson.toJson(jsonElem);

    // Determine config path: prefer Forge config dir, fall back to current working dir
    Path configDir;
    try {
      configDir = FMLPaths.CONFIGDIR.get();
      if (configDir == null) throw new IllegalStateException("FMLPaths.CONFIGDIR returned null");
    } catch (Throwable t) {
      configDir = Paths.get(".");
    }

    Path outDir = configDir.resolve("skada").resolve("generator_data").resolve("weapon");
    Files.createDirectories(outDir);
    Path outFile = outDir.resolve("axe.json");

    // Use writeString for convenience
    Files.writeString(outFile, pretty, StandardCharsets.UTF_8);

    return outFile;
  }

  /** Convenient CLI entry to run the writer. */
  public static void main(String[] args) {
    try {
      Path p = writeDefaultSwordJson();
      System.out.println("Wrote default weapon profile to: " + p.toAbsolutePath());
    } catch (IOException e) {
      System.err.println("Error writing default weapon profile: " + e.getMessage());
      System.exit(1);
    }
  }

  public static void diagnoseEncode(WeaponProfile profile) {
    // 1) Try encoding the full profile and print the DataResult and any error messages
    System.out.println("--- Encoding full WeaponProfile ---");
    //DataResult<com.google.gson.JsonElement> full = WeaponProfile.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, profile);
    //System.out.println("Full DataResult: " + full);
    //full.result().ifPresent(json -> System.out.println("Full JSON (partial): " + json));
    //full.error().ifPresent(err -> System.err.println("Full encode error: " + err.toString()));

    // 2) Try encoding individual top-level parts so we can narrow down the failure
    System.out.println("--- Encoding handle ---");
    try {
      DataResult<com.google.gson.JsonElement> handleRes = Handle.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, profile.getHandle());
      System.out.println("Handle DataResult: " + handleRes);
      handleRes.result().ifPresent(j -> System.out.println("Handle JSON: " + j));
      handleRes.error().ifPresent(e -> System.err.println("Handle error: " + e.toString()));
    } catch (Throwable t) {
      System.err.println("Exception encoding handle:"); t.printStackTrace();
    }

    System.out.println("--- Encoding weapon_heads list ---");
    try {
      com.mojang.serialization.Codec<java.util.List<WeaponProfile.WeaponHeadEntry>> listCodec =
              com.mojang.serialization.Codec.list(WeaponProfile.WeaponHeadEntry.CODEC);
      DataResult<com.google.gson.JsonElement> listRes = listCodec.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, profile.getWeaponHeads());
      System.out.println("weapon_heads DataResult: " + listRes);
      listRes.result().ifPresent(j -> System.out.println("weapon_heads JSON: " + j));
      listRes.error().ifPresent(e -> System.err.println("weapon_heads error: " + e.toString()));
    } catch (Throwable t) {
      System.err.println("Exception encoding weapon_heads list:"); t.printStackTrace();
    }

    // 3) Encode each WeaponHeadEntry / WeaponHead individually
    System.out.println("--- Encoding each WeaponHeadEntry ---");
    for (int i = 0; i < profile.getWeaponHeads().size(); i++) {
      WeaponProfile.WeaponHeadEntry entry = profile.getWeaponHeads().get(i);
      System.out.println("Entry #" + i + " type: " + (entry.getHead() == null ? "null" : entry.getHead().getClass().getName()));
      try {
        DataResult<com.google.gson.JsonElement> entryRes = WeaponProfile.WeaponHeadEntry.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, entry);
        System.out.println("Entry#" + i + " DataResult: " + entryRes);
        int finalI2 = i;
        entryRes.result().ifPresent(j -> System.out.println("Entry#" + finalI2 + " JSON: " + j));
        int finalI3 = i;
        entryRes.error().ifPresent(e -> System.err.println("Entry#" + finalI3 + " error: " + e.toString()));
      } catch (Throwable t) {
        System.err.println("Exception encoding entry #" + i + ":"); t.printStackTrace();
      }

      // Also try the head codec directly (helps if problem is in WeaponHead)
      try {
        @SuppressWarnings("unchecked")
        com.mojang.serialization.Codec<WeaponHead> headCodec = WeaponHead.CODEC;
        DataResult<com.google.gson.JsonElement> headRes = headCodec.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, entry.getHead());
        System.out.println("Entry#" + i + " head DataResult: " + headRes);
        int finalI = i;
        headRes.result().ifPresent(j -> System.out.println("Entry#" + finalI + " head JSON: " + j));
        int finalI1 = i;
        headRes.error().ifPresent(e -> System.err.println("Entry#" + finalI1 + " head error: " + e.toString()));
      } catch (Throwable t) {
        System.err.println("Exception encoding head #" + i + ":"); t.printStackTrace();
      }
    }

    // 4) Encode attack_types map and each entry
    System.out.println("--- Encoding attack_types map ---");
    try {
      com.mojang.serialization.Codec<java.util.Map<String, AttackTypeJsonInfo>> mapCodec =
              com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING, AttackTypeJsonInfo.CODEC);
      DataResult<com.google.gson.JsonElement> mapRes = mapCodec.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, profile.attackTypeStringMap());
      System.out.println("attack_types DataResult: " + mapRes);
      mapRes.result().ifPresent(j -> System.out.println("attack_types JSON: " + j));
      mapRes.error().ifPresent(e -> System.err.println("attack_types error: " + e.toString()));
    } catch (Throwable t) {
      System.err.println("Exception encoding attack_types:"); t.printStackTrace();
    }
  }

}
