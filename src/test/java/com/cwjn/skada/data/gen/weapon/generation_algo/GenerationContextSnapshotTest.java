package com.cwjn.skada.data.gen.weapon.generation_algo;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AssemblyPhysicsSnapshot;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackDeliverySnapshot;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.ContactSnapshotStrike;
import com.cwjn.skada.data.gen.weapon.attack_capability.NoneCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.attack.AttackTypeJsonInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationContextSnapshotTest {

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
  void rotationalThrustClassificationMatchesKnownToolProfiles() throws IOException {
    WeaponAssembly pickaxe = loadAssembly("pickaxe");
    WeaponAssembly spear = loadAssembly("spear");
    WeaponAssembly hoe = loadAssembly("hoe");

    AttackDeliverySnapshot pickaxeDelivery = AttackDeliverySnapshot.fromWeapon(pickaxe, AssemblyPhysicsSnapshot.fromWeapon(pickaxe, WeaponAssembly.LARGE_SAMPLE_SIZE), new AttackType("thrust", null, null, null, ThrustCapable.class), WeaponAssembly.LARGE_SAMPLE_SIZE);
    AttackDeliverySnapshot spearDelivery = AttackDeliverySnapshot.fromWeapon(spear, AssemblyPhysicsSnapshot.fromWeapon(spear, WeaponAssembly.LARGE_SAMPLE_SIZE), new AttackType("thrust", null, null, null, ThrustCapable.class), WeaponAssembly.LARGE_SAMPLE_SIZE);
    AttackDeliverySnapshot hoeDelivery = AttackDeliverySnapshot.fromWeapon(hoe, AssemblyPhysicsSnapshot.fromWeapon(hoe, WeaponAssembly.LARGE_SAMPLE_SIZE), new AttackType("thrust", null, null, null, ThrustCapable.class), WeaponAssembly.LARGE_SAMPLE_SIZE);

    assertTrue(pickaxeDelivery.rotationalThrust());
    assertTrue(hoeDelivery.rotationalThrust());
    assertTrue(!spearDelivery.rotationalThrust());
  }

  @Test
  void effectiveMassRatioTracksHeavierForwardCommitment() throws IOException {
    WeaponAssembly sword = loadAssembly("sword");
    WeaponAssembly axe = loadAssembly("axe");

    AttackDeliverySnapshot swordDelivery = AttackDeliverySnapshot.fromWeapon(sword, AssemblyPhysicsSnapshot.fromWeapon(sword, WeaponAssembly.LARGE_SAMPLE_SIZE), new AttackType("slash", null, null, null, SlashCapable.class), WeaponAssembly.LARGE_SAMPLE_SIZE);
    AttackDeliverySnapshot axeDelivery = AttackDeliverySnapshot.fromWeapon(axe, AssemblyPhysicsSnapshot.fromWeapon(axe, WeaponAssembly.LARGE_SAMPLE_SIZE), new AttackType("slash", null, null, null, SlashCapable.class), WeaponAssembly.LARGE_SAMPLE_SIZE);

    assertTrue(axeDelivery.effectiveMassRatio() > swordDelivery.effectiveMassRatio());
  }

  @Test
  void leverageGapRemainsHigherOnSwordThanSpear() throws IOException {
    WeaponAssembly sword = loadAssembly("sword");
    WeaponAssembly spear = loadAssembly("spear");

    AttackDeliverySnapshot swordDelivery = AttackDeliverySnapshot.fromWeapon(sword, AssemblyPhysicsSnapshot.fromWeapon(sword, WeaponAssembly.LARGE_SAMPLE_SIZE), new AttackType("thrust", null, null, null, ThrustCapable.class), WeaponAssembly.LARGE_SAMPLE_SIZE);
    AttackDeliverySnapshot spearDelivery = AttackDeliverySnapshot.fromWeapon(spear, AssemblyPhysicsSnapshot.fromWeapon(spear, WeaponAssembly.LARGE_SAMPLE_SIZE), new AttackType("thrust", null, null, null, ThrustCapable.class), WeaponAssembly.LARGE_SAMPLE_SIZE);

    assertTrue(
      swordDelivery.leverageGap() > spearDelivery.leverageGap(),
      "sword leverageGap=" + swordDelivery.leverageGap() + ", spear leverageGap=" + spearDelivery.leverageGap());
  }

  @Test
  void slashCoPAlignmentIsTighterForAxeThanSword() throws IOException {
    WeaponAssembly sword = loadAssembly("sword");
    WeaponAssembly axe = loadAssembly("axe");

    AttackDeliverySnapshot swordDelivery = AttackDeliverySnapshot.fromWeapon(sword, AssemblyPhysicsSnapshot.fromWeapon(sword, WeaponAssembly.LARGE_SAMPLE_SIZE), new AttackType("slash", null, null, null, SlashCapable.class), WeaponAssembly.LARGE_SAMPLE_SIZE);
    AttackDeliverySnapshot axeDelivery = AttackDeliverySnapshot.fromWeapon(axe, AssemblyPhysicsSnapshot.fromWeapon(axe, WeaponAssembly.LARGE_SAMPLE_SIZE), new AttackType("slash", null, null, null, SlashCapable.class), WeaponAssembly.LARGE_SAMPLE_SIZE);

    assertTrue(
      Math.abs(axeDelivery.copStrikeDelta()) < Math.abs(swordDelivery.copStrikeDelta()),
      "sword copStrikeDelta=" + swordDelivery.copStrikeDelta() + ", axe copStrikeDelta=" + axeDelivery.copStrikeDelta());
  }

  @Test
  void strikeRepeatabilityRemainsHigherForCompactImpactHeads() throws IOException {
    WeaponAssembly shovel = loadAssembly("shovel");
    WeaponAssembly mace = loadAssembly("mace");

    ContactSnapshotStrike shovelContact = ContactSnapshotStrike.fromPart((StrikeCapable) shovel.primaryPartForAttackType(new AttackType("strike", null, null, null, StrikeCapable.class)).orElseThrow().part());
    ContactSnapshotStrike maceContact = ContactSnapshotStrike.fromPart((StrikeCapable) mace.primaryPartForAttackType(new AttackType("strike", null, null, null, StrikeCapable.class)).orElseThrow().part());

    assertTrue(maceContact.strikeRepeatability() > shovelContact.strikeRepeatability());
    assertTrue(maceContact.localizationFactor() > shovelContact.localizationFactor());
  }

  @Test
  void axeAssemblyHasHigherNormalizedInertiaThanSwordAssembly() throws IOException {
    WeaponAssembly sword = loadAssembly("sword");
    WeaponAssembly axe = loadAssembly("axe");

    AssemblyPhysicsSnapshot swordAssembly = AssemblyPhysicsSnapshot.fromWeapon(sword, WeaponAssembly.LARGE_SAMPLE_SIZE);
    AssemblyPhysicsSnapshot axeAssembly = AssemblyPhysicsSnapshot.fromWeapon(axe, WeaponAssembly.LARGE_SAMPLE_SIZE);

    assertTrue(axeAssembly.normalizedInertiaCoefficient() > swordAssembly.normalizedInertiaCoefficient());
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

  @SuppressWarnings("unchecked")
  private static <T> Supplier<IForgeRegistry<T>> registrySupplier(Map<ResourceLocation, T> valuesById) {
    Map<T, ResourceLocation> idsByValue = new IdentityHashMap<>();
    valuesById.forEach((id, value) -> idsByValue.put(value, id));

    IForgeRegistry<T> registry = (IForgeRegistry<T>) Proxy.newProxyInstance(
      GenerationContextSnapshotTest.class.getClassLoader(),
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