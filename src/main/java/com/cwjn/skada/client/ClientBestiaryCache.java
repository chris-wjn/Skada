package com.cwjn.skada.client;

import com.cwjn.skada.data.mob.BestiaryEntryView;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class ClientBestiaryCache {

  private static final Map<ResourceLocation, BestiaryEntryView> ENTRIES_BY_ID = new LinkedHashMap<>();
  private static final Set<ResourceLocation> UNLOCKED_ENTRY_IDS = new LinkedHashSet<>();
  private static final Set<ResourceLocation> NEWLY_UNLOCKED_ENTRY_IDS = new LinkedHashSet<>();

  private ClientBestiaryCache() {
  }

  public static void replaceEntries(List<BestiaryEntryView> entries) {
    ENTRIES_BY_ID.clear();
    for (BestiaryEntryView entry : entries) {
      if (entry == null || entry.entityId() == null) {
        continue;
      }
      ENTRIES_BY_ID.put(entry.entityId(), entry);
    }
  }

  public static void replaceUnlockState(Collection<ResourceLocation> unlockedEntryIds,
                                        Collection<ResourceLocation> newlyUnlockedEntryIds) {
    UNLOCKED_ENTRY_IDS.clear();
    addIds(UNLOCKED_ENTRY_IDS, unlockedEntryIds);

    NEWLY_UNLOCKED_ENTRY_IDS.clear();
    if (newlyUnlockedEntryIds == null || newlyUnlockedEntryIds.isEmpty()) {
      return;
    }

    for (ResourceLocation entryId : newlyUnlockedEntryIds) {
      if (entryId != null && UNLOCKED_ENTRY_IDS.contains(entryId)) {
        NEWLY_UNLOCKED_ENTRY_IDS.add(entryId);
      }
    }
  }

  public static void clear() {
    ENTRIES_BY_ID.clear();
    UNLOCKED_ENTRY_IDS.clear();
    NEWLY_UNLOCKED_ENTRY_IDS.clear();
  }

  public static List<BestiaryEntryView> entries() {
    return List.copyOf(ENTRIES_BY_ID.values());
  }

  public static BestiaryEntryView get(ResourceLocation entityId) {
    return ENTRIES_BY_ID.get(entityId);
  }

  public static Set<ResourceLocation> unlockedEntryIds() {
    return Set.copyOf(UNLOCKED_ENTRY_IDS);
  }

  public static Set<ResourceLocation> newlyUnlockedEntryIds() {
    return Set.copyOf(NEWLY_UNLOCKED_ENTRY_IDS);
  }

  public static boolean isUnlocked(ResourceLocation entityId) {
    return entityId != null && UNLOCKED_ENTRY_IDS.contains(entityId);
  }

  public static boolean isNewlyUnlocked(ResourceLocation entityId) {
    return entityId != null && NEWLY_UNLOCKED_ENTRY_IDS.contains(entityId);
  }

  /**
   * Clears the client-only newly-unlocked marker for an entry after the player has viewed it.
   *
   * @param entityId the bestiary entry id to clear
   * @return true when the entry was marked as newly unlocked before clearing it
   */
  public static boolean markEntryViewed(ResourceLocation entityId) {
    return entityId != null && NEWLY_UNLOCKED_ENTRY_IDS.remove(entityId);
  }

  private static void addIds(Set<ResourceLocation> target, Collection<ResourceLocation> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }

    for (ResourceLocation id : ids) {
      if (id != null) {
        target.add(id);
      }
    }
  }
}