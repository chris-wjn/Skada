package com.cwjn.skada.data.mob;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.cwjn.skada.data.JsonLoader;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.google.common.collect.ArrayListMultimap;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ForgeRegistries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

class MobDataWeightTest {

  @BeforeAll
  static void bootstrapMinecraftRegistries() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  private final Map<EntityType<?>, MobData> originalMobData = Map.copyOf(SkadaData.MOB_DATA);
  private final java.util.function.Supplier<IForgeRegistry<AttackType>> originalAttackRegistry =
      SkadaData.REGISTRY_ATTACK_TYPE;

  @AfterEach
  void restoreMobData() {
    SkadaData.MOB_DATA.clear();
    SkadaData.MOB_DATA.putAll(originalMobData);
    SkadaData.REGISTRY_ATTACK_TYPE = originalAttackRegistry;
  }

  @Test
  void codecReadsAndWritesReferenceWeightAndKeepsExplicitZero() {
    try (MockedStatic<AttackType> attackTypes = mockStatic(AttackType.class);
         MockedStatic<Element> elements = mockStatic(Element.class)) {
      AttackType fallback = mock(AttackType.class);
      when(fallback.rl()).thenReturn(new ResourceLocation("skada", "strike"));
      attackTypes.when(AttackType::strike).thenReturn(fallback);
      elements.when(Element::physical).thenReturn(mock(Element.class));
      IForgeRegistry<AttackType> attackRegistry = mock(IForgeRegistry.class);
      when(attackRegistry.getValue(new ResourceLocation("skada", "strike"))).thenReturn(fallback);
      SkadaData.REGISTRY_ATTACK_TYPE = () -> attackRegistry;

      MobData weighted = MobData.CODEC.parse(JsonOps.INSTANCE,
          JsonParser.parseString("{\"attackType\":\"skada:strike\",\"weight\":75.5}"))
          .getOrThrow(false, message -> {});
      MobData zero = MobData.CODEC.parse(JsonOps.INSTANCE,
          JsonParser.parseString("{\"attackType\":\"skada:strike\",\"weight\":0.0}"))
          .getOrThrow(false, message -> {});

      assertEquals(75.5, weighted.referenceWeightKg());
      assertEquals(0.0, zero.referenceWeightKg());
      JsonObject encoded = (JsonObject) MobData.CODEC.encodeStart(JsonOps.INSTANCE, zero)
          .getOrThrow(false, message -> {});
      assertEquals(0.0, encoded.get("weight").getAsDouble());
    }
  }

  @Test
  void resourceReloadClearsOldEntriesAndReappliesParentWeightAndExplicitChildOverride() throws IOException {
    try (MockedStatic<AttackType> attackTypes = mockStatic(AttackType.class);
         MockedStatic<Element> elements = mockStatic(Element.class);
         MockedStatic<FMLLoader> forgeLoader = mockStatic(FMLLoader.class)) {
      AttackType fallback = mock(AttackType.class);
      when(fallback.rl()).thenReturn(new ResourceLocation("skada", "strike"));
      attackTypes.when(AttackType::strike).thenReturn(fallback);
      elements.when(Element::physical).thenReturn(mock(Element.class));
      IForgeRegistry<AttackType> attackRegistry = mock(IForgeRegistry.class);
      when(attackRegistry.getValue(new ResourceLocation("skada", "strike"))).thenReturn(fallback);
      SkadaData.REGISTRY_ATTACK_TYPE = () -> attackRegistry;
      LoadingModList loadingModList = mock(LoadingModList.class, RETURNS_DEEP_STUBS);
      forgeLoader.when(FMLLoader::getLoadingModList).thenReturn(loadingModList);

      SkadaData.MOB_DATA.put(EntityType.PLAYER,
          new MobData(null, null, ArrayListMultimap.create(), 999.0));
      JsonObject definitions = JsonParser.parseString("""
          {
            "cow": {"attackType": "skada:strike", "weight": 600.0},
            "pig": {"attackType": "skada:strike", "parents": ["minecraft:cow"]}
          }
          """).getAsJsonObject();
      ResourceLocation resourceId = new ResourceLocation("skada", "mob_info/minecraft.json");
      ResourceManager manager = mock(ResourceManager.class);
      when(manager.listResources(ArgumentMatchers.eq("mob_info"), ArgumentMatchers.any()))
          .thenReturn(Map.of(resourceId, mock(Resource.class)));
      when(manager.openAsReader(resourceId)).thenReturn(new BufferedReader(new StringReader(definitions.toString())));

      JsonLoader.updateMobInfoFromResources(manager);

      EntityType<?> cow = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("minecraft", "cow"));
      EntityType<?> pig = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("minecraft", "pig"));
      assertEquals(600.0, SkadaData.MOB_DATA.get(cow).referenceWeightKg());
      assertEquals(600.0, SkadaData.MOB_DATA.get(pig).referenceWeightKg());
      assertFalse(SkadaData.MOB_DATA.containsKey(EntityType.PLAYER));

      definitions.getAsJsonObject("pig").addProperty("weight", 0.0);
      when(manager.openAsReader(resourceId)).thenReturn(new BufferedReader(new StringReader(definitions.toString())));
      JsonLoader.updateMobInfoFromResources(manager);

      assertEquals(0.0, SkadaData.MOB_DATA.get(pig).referenceWeightKg());
      assertFalse(SkadaData.MOB_DATA.containsKey(EntityType.PLAYER));
    }
  }

  @Test
  void shippedVanillaMobsHaveExplicitFiniteWeightsWithoutPlayerOverride() throws IOException {
    JsonObject data = JsonParser.parseString(Files.readString(
        Path.of("src/main/resources/data/skada/mob_info/minecraft.json"))).getAsJsonObject();

    assertFalse(data.entrySet().isEmpty());
    // Players use the dimension-based fallback instead of a fixed mob-data weight.
    assertFalse(data.has("player"));
    data.entrySet().forEach(entry -> {
      assertTrue(entry.getValue().getAsJsonObject().has("weight"), entry.getKey());
      double weight = entry.getValue().getAsJsonObject().get("weight").getAsDouble();
      assertTrue(Double.isFinite(weight) && weight >= 0.0, entry.getKey());
    });
  }
}
