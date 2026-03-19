package com.cwjn.skada.util;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.gen.attack.AttackTypeJsonInfo;
import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import com.cwjn.skada.data.gen.weapon.attack_capability.NoneCapable;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponProfileGenerationTest {

  private record GeneratedAttackStats(double lethality, double precision, double attackSpeed, double failChance) {
  }

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path GENERATOR_ROOT = Path.of("src", "main", "resources", "data", "skada", "generator_data", "weapon");

  @BeforeAll
  static void bootstrapRegistries() {
    ResourceLocation basicId = new ResourceLocation("skada", "basic");
    Element basic = new Element("basic", null, null, null, 0, basicId, null);
    SkadaData.REGISTRY_ELEMENT = registrySupplier(Map.of(basicId, basic));

    AttackType slash = new AttackType("slash", SkadaData.PERCENT_DAMAGE_BONUS, SkadaData.SLASH_GENERATOR_CONFIG, null, SlashCapable.class);
    AttackType thrust = new AttackType("thrust", SkadaData.PERCENT_HEALTH_DAMAGE, SkadaData.THRUST_GENERATOR_CONFIG, null, ThrustCapable.class);
    AttackType strike = new AttackType("strike", SkadaData.PERCENT_REDUC, SkadaData.STRIKE_GENERATOR_CONFIG, null, StrikeCapable.class);
    AttackType none = new AttackType("none", SkadaData.NONE, SkadaData.NULL_GENERATOR_CONFIG, null, NoneCapable.class);
    SkadaData.REGISTRY_ATTACK_TYPE = registrySupplier(Map.of(
      new ResourceLocation("skada", "slash"), slash,
      new ResourceLocation("skada", "thrust"), thrust,
      new ResourceLocation("skada", "strike"), strike,
      new ResourceLocation("skada", "none"), none));
  }

  @Test
  void swordProfileGeneratesAttackStatsFromJsonAssembly() throws IOException {
    WeaponAssembly sword = loadAssembly("sword");

    assertTrue(LethalityGenerationUtil.slash(sword) > 0.0);
    assertTrue(PrecisionGenerationUtil.slash(sword) > 0.0);
    assertTrue(AttackSpeedGenerationUtil.slash(sword) > 0.0);
    assertTrue(CriticalFailGenerationUtil.slash(sword) > 0.0);
  }

  @Test
  void dedicatedBladesBeatMixedUseImplementsOnControlAndRecovery() throws IOException {
    GeneratedAttackStats swordSlash = stats("sword", "slash");
    GeneratedAttackStats axeSlash = stats("axe", "slash");
    GeneratedAttackStats longswordSlash = stats("longsword", "slash");

    assertTrue(swordSlash.precision() > axeSlash.precision(), comparisonMessage("Sword slash precision should exceed axe slash precision", swordSlash, axeSlash));
    assertTrue(swordSlash.attackSpeed() > axeSlash.attackSpeed(), comparisonMessage("Sword slash speed should exceed axe slash speed", swordSlash, axeSlash));
    assertTrue(swordSlash.failChance() < axeSlash.failChance(), comparisonMessage("Sword slash fail chance should stay below axe slash fail chance", swordSlash, axeSlash));

    assertTrue(longswordSlash.precision() > swordSlash.precision(), comparisonMessage("Longsword slash precision should exceed sword slash precision", longswordSlash, swordSlash));
    assertTrue(longswordSlash.failChance() <= swordSlash.failChance(), comparisonMessage("Longsword slash fail chance should not exceed sword slash fail chance", longswordSlash, swordSlash));
  }

  @Test
  void dedicatedThrustWeaponsBeatImprovisedPointToolsOnLethality() throws IOException {
    GeneratedAttackStats swordThrust = stats("sword", "thrust");
    GeneratedAttackStats spearThrust = stats("spear", "thrust");
    GeneratedAttackStats pickaxeThrust = stats("pickaxe", "thrust");
    GeneratedAttackStats hoeThrust = stats("hoe", "thrust");

    assertTrue(swordThrust.lethality() > hoeThrust.lethality(), comparisonMessage("Sword thrust lethality should exceed hoe thrust lethality", swordThrust, hoeThrust));
    assertTrue(spearThrust.lethality() > pickaxeThrust.lethality(), comparisonMessage("Spear thrust lethality should exceed pickaxe thrust lethality", spearThrust, pickaxeThrust));
    assertTrue(spearThrust.lethality() > hoeThrust.lethality(), comparisonMessage("Spear thrust lethality should exceed hoe thrust lethality", spearThrust, hoeThrust));
  }

  @Test
  void tridentThrustStaysAboveImprovisedPointTools() throws IOException {
    GeneratedAttackStats tridentThrust = stats("trident", "thrust");
    GeneratedAttackStats pickaxeThrust = stats("pickaxe", "thrust");
    GeneratedAttackStats hoeThrust = stats("hoe", "thrust");

    assertTrue(tridentThrust.lethality() > pickaxeThrust.lethality(), comparisonMessage("Trident thrust lethality should exceed pickaxe thrust lethality", tridentThrust, pickaxeThrust));
    assertTrue(tridentThrust.precision() > pickaxeThrust.precision(), comparisonMessage("Trident thrust precision should exceed pickaxe thrust precision", tridentThrust, pickaxeThrust));
    assertTrue(tridentThrust.failChance() < pickaxeThrust.failChance(), comparisonMessage("Trident thrust fail chance should stay below pickaxe thrust fail chance", tridentThrust, pickaxeThrust));

    assertTrue(tridentThrust.lethality() > hoeThrust.lethality(), comparisonMessage("Trident thrust lethality should exceed hoe thrust lethality", tridentThrust, hoeThrust));
    assertTrue(tridentThrust.precision() > hoeThrust.precision(), comparisonMessage("Trident thrust precision should exceed hoe thrust precision", tridentThrust, hoeThrust));
    assertTrue(tridentThrust.failChance() < hoeThrust.failChance(), comparisonMessage("Trident thrust fail chance should stay below hoe thrust fail chance", tridentThrust, hoeThrust));
  }

  @Test
  void dedicatedThrustWeaponsBeatImprovisedPointToolsOnControlEnvelope() throws IOException {
    GeneratedAttackStats spearThrust = stats("spear", "thrust");
    GeneratedAttackStats pickaxeThrust = stats("pickaxe", "thrust");
    GeneratedAttackStats hoeThrust = stats("hoe", "thrust");

    assertTrue(spearThrust.precision() > pickaxeThrust.precision(), comparisonMessage("Spear thrust precision should exceed pickaxe thrust precision", spearThrust, pickaxeThrust));
    assertTrue(spearThrust.failChance() < pickaxeThrust.failChance(), comparisonMessage("Spear thrust fail chance should stay below pickaxe thrust fail chance", spearThrust, pickaxeThrust));
    assertTrue(spearThrust.precision() > hoeThrust.precision(), comparisonMessage("Spear thrust precision should exceed hoe thrust precision", spearThrust, hoeThrust));
    assertTrue(spearThrust.failChance() < hoeThrust.failChance(), comparisonMessage("Spear thrust fail chance should stay below hoe thrust fail chance", spearThrust, hoeThrust));
  }

  @Test
  void dedicatedSlashWeaponsBeatSecondarySlashProfilesOnControl() throws IOException {
    GeneratedAttackStats swordSlash = stats("sword", "slash");
    GeneratedAttackStats spearSlash = stats("spear", "slash");

    assertTrue(swordSlash.precision() > spearSlash.precision(), comparisonMessage("Sword slash precision should exceed spear slash precision", swordSlash, spearSlash));
    assertTrue(swordSlash.attackSpeed() > spearSlash.attackSpeed(), comparisonMessage("Sword slash speed should exceed spear slash speed", swordSlash, spearSlash));
  }

  @Test
  void committedSlashProfilesTradeTempoForHigherPayoff() throws IOException {
    GeneratedAttackStats swordSlash = stats("sword", "slash");
    GeneratedAttackStats longswordSlash = stats("longsword", "slash");
    GeneratedAttackStats axeSlash = stats("axe", "slash");

    assertTrue(longswordSlash.lethality() > swordSlash.lethality(), comparisonMessage("Longsword slash lethality should exceed sword slash lethality", longswordSlash, swordSlash));

    assertTrue(axeSlash.lethality() > swordSlash.lethality(), comparisonMessage("Axe slash lethality should exceed sword slash lethality", axeSlash, swordSlash));
    assertTrue(axeSlash.attackSpeed() < swordSlash.attackSpeed(), comparisonMessage("Axe slash speed should stay below sword slash speed", axeSlash, swordSlash));
    assertTrue(axeSlash.failChance() > swordSlash.failChance(), comparisonMessage("Axe slash fail chance should exceed sword slash fail chance", axeSlash, swordSlash));
  }

  @Test
  void dedicatedStrikersBeatImprovisedToolHeads() throws IOException {
    GeneratedAttackStats maceStrike = stats("mace", "strike");
    GeneratedAttackStats shovelStrike = stats("shovel", "strike");

    assertTrue(maceStrike.lethality() > shovelStrike.lethality(), comparisonMessage("Mace strike lethality should exceed shovel strike lethality", maceStrike, shovelStrike));
    assertTrue(maceStrike.precision() > shovelStrike.precision(), comparisonMessage("Mace strike precision should exceed shovel strike precision", maceStrike, shovelStrike));
    assertTrue(maceStrike.failChance() <= shovelStrike.failChance(), comparisonMessage("Mace strike fail chance should not exceed shovel strike fail chance", maceStrike, shovelStrike));
  }

  @Test
  void generatedWeaponInfoCarriesOptionalDamageBonusFromAttackJson() throws IOException {
    WeaponAssembly sword = loadAssembly("sword");
    Map<AttackType, AttackTypeJsonInfo> updatedAttackTypes = new LinkedHashMap<>(sword.getAttackTypes());
    AttackTypeJsonInfo slashInfo = updatedAttackTypes.entrySet().stream()
      .filter(entry -> "slash".equals(entry.getKey().name()))
      .map(Map.Entry::getValue)
      .findFirst()
      .orElseThrow();
    AttackType slashType = updatedAttackTypes.keySet().stream()
      .filter(type -> "slash".equals(type.name()))
      .findFirst()
      .orElseThrow();
    updatedAttackTypes.put(slashType, new AttackTypeJsonInfo(
      slashInfo.minReach(),
      slashInfo.maxReach(),
      slashInfo.attackSpeedModifier(),
      slashInfo.lethalityModifier(),
      slashInfo.precisionModifier(),
      0.35,
      slashInfo.critFailModifier(),
      slashInfo.reticleShapes()));

    WeaponInfo info = WeaponInfo.generate(MaterialInfo.getDefault(), new WeaponAssembly(sword.parts(), updatedAttackTypes), false);
    AttackTypeInfo generatedSlash = info.getAttackTypes().get(slashType);

    assertEquals(0.35, generatedSlash.damageBonus(), 1.0e-9);
  }

  @Test
  void stockTridentProfileCanOptIntoDamageBonus() throws IOException {
    WeaponInfo info = WeaponInfo.generate(loadAssembly("trident"), false);
    AttackTypeInfo generatedThrust = info.getAttackTypes().entrySet().stream()
      .filter(entry -> "thrust".equals(entry.getKey().name()))
      .map(Map.Entry::getValue)
      .findFirst()
      .orElseThrow();

    assertEquals(0.2, generatedThrust.damageBonus(), 1.0e-9);
  }

  private static WeaponAssembly loadAssembly(String name) throws IOException {
    JsonObject rawAssembly = JsonParser.parseString(Files.readString(GENERATOR_ROOT.resolve(Path.of("weapon_profile", name + ".json")))).getAsJsonObject();
    JsonObject resolvedAssembly = Util.resolveWeaponAssemblyPartReferences(rawAssembly, "skada", loadPartMap());
    return WeaponAssembly.CODEC.parse(JsonOps.INSTANCE, resolvedAssembly).result().orElseThrow().withMaterialWoodenHandle(MaterialInfo.getDefault());
  }

  private static Map<String, JsonObject> loadPartMap() throws IOException {
    Map<String, JsonObject> partMap = new HashMap<>();
    Path partRoot = GENERATOR_ROOT.resolve("part");
    try (var files = Files.list(partRoot)) {
      files.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
        try {
          String name = path.getFileName().toString().replace(".json", "");
          JsonObject rawPart = GSON.fromJson(Files.readString(path), JsonObject.class);
          partMap.put("skada:" + name, Util.normalizeWeaponPartDefinitionJson(rawPart));
        } catch (IOException e) {
          throw new IllegalStateException("Failed to load part json " + path, e);
        }
      });
    }
    return partMap;
  }

  private static GeneratedAttackStats stats(String profileName, String attackKind) throws IOException {
    WeaponAssembly assembly = loadAssembly(profileName);
    return switch (attackKind) {
      case "slash" -> new GeneratedAttackStats(
        LethalityGenerationUtil.slash(assembly),
        PrecisionGenerationUtil.slash(assembly),
        AttackSpeedGenerationUtil.slash(assembly),
        CriticalFailGenerationUtil.slash(assembly));
      case "thrust" -> new GeneratedAttackStats(
        LethalityGenerationUtil.thrust(assembly),
        PrecisionGenerationUtil.thrust(assembly),
        AttackSpeedGenerationUtil.thrust(assembly),
        CriticalFailGenerationUtil.thrust(assembly));
      case "strike" -> new GeneratedAttackStats(
        LethalityGenerationUtil.strike(assembly),
        PrecisionGenerationUtil.strike(assembly),
        AttackSpeedGenerationUtil.strike(assembly),
        CriticalFailGenerationUtil.strike(assembly));
      default -> throw new IllegalArgumentException("Unsupported attack kind: " + attackKind);
    };
  }

  private static String comparisonMessage(String message, GeneratedAttackStats left, GeneratedAttackStats right) {
    return message + " | left=" + left + ", right=" + right;
  }

  @SuppressWarnings("unchecked")
  private static <T> Supplier<IForgeRegistry<T>> registrySupplier(Map<ResourceLocation, T> valuesById) {
    Map<T, ResourceLocation> idsByValue = new IdentityHashMap<>();
    valuesById.forEach((id, value) -> idsByValue.put(value, id));

    IForgeRegistry<T> registry = (IForgeRegistry<T>) Proxy.newProxyInstance(
      WeaponProfileGenerationTest.class.getClassLoader(),
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
    if (!returnType.isPrimitive()) {
      return null;
    }
    if (returnType == boolean.class) {
      return false;
    }
    if (returnType == byte.class) {
      return (byte) 0;
    }
    if (returnType == short.class) {
      return (short) 0;
    }
    if (returnType == int.class) {
      return 0;
    }
    if (returnType == long.class) {
      return 0L;
    }
    if (returnType == float.class) {
      return 0.0f;
    }
    if (returnType == double.class) {
      return 0.0d;
    }
    if (returnType == char.class) {
      return '\0';
    }
    return null;
  }
}