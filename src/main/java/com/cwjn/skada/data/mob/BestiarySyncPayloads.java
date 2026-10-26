package com.cwjn.skada.data.mob;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BestiarySyncPayloads {

  private BestiarySyncPayloads() {
  }

  public static List<BestiaryEntryView> prepare(Map<EntityType<?>, BestiaryEntryView> entryViews) {
    return entryViews.entrySet().stream()
        .map(BestiarySyncPayloads::validatedEntry)
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(view -> view.entityId().toString()))
        .toList();
  }

  private static BestiaryEntryView validatedEntry(Map.Entry<EntityType<?>, BestiaryEntryView> entry) {
    if (entry == null || entry.getKey() == null || entry.getValue() == null || entry.getValue().entityId() == null) {
      return null;
    }

    ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entry.getKey());
    if (entityId == null || !entityId.equals(entry.getValue().entityId())) {
      return null;
    }

    return entry.getValue();
  }
}