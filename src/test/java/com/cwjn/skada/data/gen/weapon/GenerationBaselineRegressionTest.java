package com.cwjn.skada.data.gen.weapon;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.JsonUtil;
import com.cwjn.skada.data.gen.attack.AttackTypeJsonInfo;
import com.cwjn.skada.data.gen.attack.ElementSpread;
import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.generation_algo.AttackSpeedGenerationUtil;
import com.cwjn.skada.data.gen.weapon.generation_algo.CriticalFailGenerationUtil;
import com.cwjn.skada.data.gen.weapon.generation_algo.LethalityGenerationUtil;
import com.cwjn.skada.data.gen.weapon.generation_algo.PrecisionGenerationUtil;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the current generator output for known vanilla weapon + material combinations
 * to within ±10%. Any formula change that shifts the balance baseline will be caught here.
 * Values were captured from the live generator and must not be adjusted without a
 * deliberate balance decision.
 */
class GenerationBaselineRegressionTest {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path GENERATOR_ROOT =
      Path.of("src", "main", "resources", "data", "skada", "generator_data", "weapon");

  // Vanilla material definitions matching minecraft.*.json files under generator_data/material/
  // Initialized in @BeforeAll after registries are set up (ElementSpread() requires SkadaData.REGISTRY_ELEMENT)
  private static MaterialInfo WOOD_MATERIAL;
  private static MaterialInfo IRON_MATERIAL;
  private static MaterialInfo DIAMOND_MATERIAL;
  private static MaterialInfo NETHERITE_MATERIAL;

  @BeforeAll
  static void bootstrapRegistries() {
    ResourceLocation basicId = new ResourceLocation("skada", "basic");
    Element basic = new Element("basic", null, null, null, 0, basicId, null);
    SkadaData.REGISTRY_ELEMENT = registrySupplier(Map.of(basicId, basic));

    AttackType slash = new AttackType("slash", SkadaData.PERCENT_DAMAGE_BONUS, SkadaData.SLASH_GENERATOR_CONFIG, null, SlashCapable.class);
    AttackType thrust = new AttackType("thrust", SkadaData.PERCENT_HEALTH_DAMAGE, SkadaData.THRUST_GENERATOR_CONFIG, null, ThrustCapable.class);
    AttackType strike = new AttackType("strike", SkadaData.PERCENT_REDUC, SkadaData.STRIKE_GENERATOR_CONFIG, null, StrikeCapable.class);
    SkadaData.REGISTRY_ATTACK_TYPE = registrySupplier(Map.of(
        new ResourceLocation("skada", "slash"), slash,
        new ResourceLocation("skada", "thrust"), thrust,
        new ResourceLocation("skada", "strike"), strike));

    WOOD_MATERIAL     = new MaterialInfo(0.71,  1.5,  4.0, 5.0, new ElementSpread());
    IRON_MATERIAL     = new MaterialInfo(7.874, 4.0,  8.0, 4.5, new ElementSpread());
    DIAMOND_MATERIAL  = new MaterialInfo(3.53,  10.0, 1.0, 0.2, new ElementSpread());
    NETHERITE_MATERIAL = new MaterialInfo(9.0,  8.5,  8.0, 3.0, new ElementSpread());
  }

  // ── Wood sword (slash) ──────────────────────────────────────────────────────

  @Test
  void woodSwordSlashStatsMatchBaseline() throws IOException {
    WeaponAssembly sword = loadAssembly("sword", WOOD_MATERIAL);
    assertWithinTenPercent("wood sword slash lethality",   4.675, LethalityGenerationUtil.slash(sword));
    assertWithinTenPercent("wood sword slash precision",   9.362, PrecisionGenerationUtil.slash(sword));
    assertWithinTenPercent("wood sword slash attackSpeed", -0.064, AttackSpeedGenerationUtil.slash(sword));
    assertWithinTenPercent("wood sword slash failChance",  0.010, CriticalFailGenerationUtil.slash(sword));
  }

  // ── Iron sword (slash) ──────────────────────────────────────────────────────

  @Test
  void ironSwordSlashStatsMatchBaseline() throws IOException {
    WeaponAssembly sword = loadAssembly("sword", IRON_MATERIAL);
    assertWithinTenPercent("iron sword slash lethality",   6.938, LethalityGenerationUtil.slash(sword));
    assertWithinTenPercent("iron sword slash precision",   10.018, PrecisionGenerationUtil.slash(sword));
    assertWithinTenPercent("iron sword slash attackSpeed", -0.134, AttackSpeedGenerationUtil.slash(sword));
    assertWithinTenPercent("iron sword slash failChance",  0.009, CriticalFailGenerationUtil.slash(sword));
  }

  // ── Diamond sword (slash) ───────────────────────────────────────────────────

  @Test
  void diamondSwordSlashStatsMatchBaseline() throws IOException {
    WeaponAssembly sword = loadAssembly("sword", DIAMOND_MATERIAL);
    assertWithinTenPercent("diamond sword slash lethality",   6.093, LethalityGenerationUtil.slash(sword));
    assertWithinTenPercent("diamond sword slash precision",   8.713, PrecisionGenerationUtil.slash(sword));
    assertWithinTenPercent("diamond sword slash attackSpeed", -0.127, AttackSpeedGenerationUtil.slash(sword));
    assertWithinTenPercent("diamond sword slash failChance",  0.014, CriticalFailGenerationUtil.slash(sword));
  }

  // ── Netherite sword (slash) ─────────────────────────────────────────────────

  @Test
  void netheriteSwordSlashStatsMatchBaseline() throws IOException {
    WeaponAssembly sword = loadAssembly("sword", NETHERITE_MATERIAL);
    assertWithinTenPercent("netherite sword slash lethality",   7.143, LethalityGenerationUtil.slash(sword));
    assertWithinTenPercent("netherite sword slash precision",   11.001, PrecisionGenerationUtil.slash(sword));
    assertWithinTenPercent("netherite sword slash attackSpeed", -0.135, AttackSpeedGenerationUtil.slash(sword));
    assertWithinTenPercent("netherite sword slash failChance",  0.009, CriticalFailGenerationUtil.slash(sword));
  }

  // ── Iron spear (thrust) ─────────────────────────────────────────────────────

  @Test
  void ironSpearThrustStatsMatchBaseline() throws IOException {
    WeaponAssembly spear = loadAssembly("spear", IRON_MATERIAL);
    assertWithinTenPercent("iron spear thrust lethality",   21.552, LethalityGenerationUtil.thrust(spear));
    assertWithinTenPercent("iron spear thrust precision",    7.268, PrecisionGenerationUtil.thrust(spear));
    assertWithinTenPercent("iron spear thrust attackSpeed", -0.319, AttackSpeedGenerationUtil.thrust(spear));
    assertWithinTenPercent("iron spear thrust failChance",   0.006, CriticalFailGenerationUtil.thrust(spear));
  }

  // ── Iron mace (strike) ──────────────────────────────────────────────────────

  @Test
  void ironMaceStrikeStatsMatchBaseline() throws IOException {
    WeaponAssembly mace = loadAssembly("mace", IRON_MATERIAL);
    assertWithinTenPercent("iron mace strike lethality",   53.463, LethalityGenerationUtil.strike(mace));
    assertWithinTenPercent("iron mace strike precision",    8.757, PrecisionGenerationUtil.strike(mace));
    assertWithinTenPercent("iron mace strike attackSpeed", -0.419, AttackSpeedGenerationUtil.strike(mace));
    assertWithinTenPercent("iron mace strike failChance",   0.002, CriticalFailGenerationUtil.strike(mace));
  }

  // ── helpers ─────────────────────────────────────────────────────────────────

  private static void assertWithinTenPercent(String label, double expected, double actual) {
    double tolerance = Math.abs(expected) * 0.10;
    assertTrue(actual >= expected - tolerance && actual <= expected + tolerance,
        label + ": expected " + expected + " ±10% but got " + actual);
  }

  private static WeaponAssembly loadAssembly(String name, MaterialInfo material) throws IOException {
    JsonObject rawAssembly = JsonParser.parseString(
        Files.readString(GENERATOR_ROOT.resolve(Path.of("weapon_profile", name + ".json"))))
        .getAsJsonObject();
    JsonObject resolvedAssembly =
        JsonUtil.resolveWeaponAssemblyPartReferences(rawAssembly, "skada", loadPartMap());
    return WeaponAssembly.CODEC.parse(JsonOps.INSTANCE, resolvedAssembly).result().orElseThrow()
        .withMaterialWoodenHandle(material);
  }

  private static Map<String, JsonObject> loadPartMap() throws IOException {
    Map<String, JsonObject> partMap = new HashMap<>();
    Path partRoot = GENERATOR_ROOT.resolve("part");
    try (var files = Files.list(partRoot)) {
      files.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
        try {
          String name = path.getFileName().toString().replace(".json", "");
          JsonObject rawPart = GSON.fromJson(Files.readString(path), JsonObject.class);
          partMap.put("skada:" + name, JsonUtil.normalizeWeaponPartDefinitionJson(rawPart));
        } catch (IOException e) {
          throw new IllegalStateException("Failed to load part json " + path, e);
        }
      });
    }
    return partMap;
  }

  @SuppressWarnings("unchecked")
  private static <T> Supplier<IForgeRegistry<T>> registrySupplier(Map<ResourceLocation, T> valuesById) {
    Map<T, ResourceLocation> idsByValue = new IdentityHashMap<>();
    valuesById.forEach((id, value) -> idsByValue.put(value, id));

    IForgeRegistry<T> registry = (IForgeRegistry<T>) Proxy.newProxyInstance(
        GenerationBaselineRegressionTest.class.getClassLoader(),
        new Class<?>[] { IForgeRegistry.class },
        (proxy, method, args) -> switch (method.getName()) {
          case "getValue" -> valuesById.get(args[0]);
          case "getKey" -> idsByValue.get(args[0]);
          case "containsKey" -> valuesById.containsKey(args[0]);
          case "toString" -> "TestForgeRegistry";
          case "hashCode" -> System.identityHashCode(proxy);
          case "equals" -> proxy == args[0];
          default -> defaultValue(method.getReturnType());
        });

    return () -> registry;
  }

  private static Object defaultValue(Class<?> returnType) {
    if (!returnType.isPrimitive()) return null;
    if (returnType == boolean.class) return false;
    if (returnType == byte.class) return (byte) 0;
    if (returnType == short.class) return (short) 0;
    if (returnType == int.class) return 0;
    if (returnType == long.class) return 0L;
    if (returnType == float.class) return 0.0f;
    if (returnType == double.class) return 0.0d;
    if (returnType == char.class) return '\0';
    return null;
  }
}
