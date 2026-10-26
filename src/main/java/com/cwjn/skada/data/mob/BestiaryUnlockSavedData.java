package com.cwjn.skada.data.mob;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent per-world bestiary unlock state keyed by player UUID.
 */
public final class BestiaryUnlockSavedData extends SavedData {

  private static final String DATA_NAME = "skada_bestiary_unlocks";
  private static final String PLAYERS_TAG = "players";
  private static final String PLAYER_UUID_TAG = "player_uuid";
  private static final String UNLOCKED_ENTRIES_TAG = "entries";

  private final Map<UUID, Set<ResourceLocation>> unlockedEntriesByPlayer = new HashMap<>();

  /**
   * Loads saved bestiary unlock state from NBT.
   *
   * @param tag the stored tag payload
   * @return the loaded saved data instance
   */
  public static BestiaryUnlockSavedData load(CompoundTag tag) {
    BestiaryUnlockSavedData data = new BestiaryUnlockSavedData();
    ListTag players = tag.getList(PLAYERS_TAG, Tag.TAG_COMPOUND);

    for (int playerIndex = 0; playerIndex < players.size(); playerIndex++) {
      CompoundTag playerTag = players.getCompound(playerIndex);
      if (!playerTag.hasUUID(PLAYER_UUID_TAG)) {
        continue;
      }

      UUID playerId = playerTag.getUUID(PLAYER_UUID_TAG);
      Set<ResourceLocation> unlockedEntries = new HashSet<>();
      ListTag entries = playerTag.getList(UNLOCKED_ENTRIES_TAG, Tag.TAG_STRING);
      for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
        ResourceLocation entryId = ResourceLocation.tryParse(entries.getString(entryIndex));
        if (entryId != null) {
          unlockedEntries.add(entryId);
        }
      }

      if (!unlockedEntries.isEmpty()) {
        data.unlockedEntriesByPlayer.put(playerId, unlockedEntries);
      }
    }

    return data;
  }

  /**
   * Resolves the persistent unlock state from overworld data storage.
   *
   * @param level any server level belonging to the current server
   * @return the shared bestiary unlock saved data
   */
  public static BestiaryUnlockSavedData get(ServerLevel level) {
    Objects.requireNonNull(level, "level");
    Objects.requireNonNull(level.getServer(), "level server");
    Objects.requireNonNull(level.getServer().overworld(), "server overworld");
    return get(level.getServer().overworld().getDataStorage());
  }

  /**
   * Unlocks a bestiary entry for the given player.
   *
   * @param playerId the player UUID
   * @param entryId the unlocked bestiary entry id
   * @return {@code true} when the saved state changed
   */
  public boolean unlock(UUID playerId, ResourceLocation entryId) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(entryId, "entryId");

    boolean changed = this.unlockedEntriesByPlayer
        .computeIfAbsent(playerId, unused -> new HashSet<>())
        .add(entryId);
    if (changed) {
      this.setDirty();
    }

    return changed;
  }

  /**
   * Unlocks a collection of bestiary entries for one player.
   *
   * @param playerId the player UUID
   * @param entryIds the entry ids to unlock
   * @return the subset that was newly unlocked by this call
   */
  public Set<ResourceLocation> unlockAll(UUID playerId, Collection<ResourceLocation> entryIds) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(entryIds, "entryIds");

    if (entryIds.isEmpty()) {
      return Set.of();
    }

    Set<ResourceLocation> unlockedEntries = this.unlockedEntriesByPlayer
        .computeIfAbsent(playerId, unused -> new HashSet<>());
    Set<ResourceLocation> newlyUnlockedEntries = new HashSet<>();
    for (ResourceLocation entryId : entryIds) {
      Objects.requireNonNull(entryId, "entryId");
      if (unlockedEntries.add(entryId)) {
        newlyUnlockedEntries.add(entryId);
      }
    }

    if (!newlyUnlockedEntries.isEmpty()) {
      this.setDirty();
      return Set.copyOf(newlyUnlockedEntries);
    }

    return Set.of();
  }

  /**
   * Checks whether a player has already unlocked a bestiary entry.
   *
   * @param playerId the player UUID
   * @param entryId the bestiary entry id
   * @return {@code true} when the entry is unlocked for that player
   */
  public boolean isUnlocked(UUID playerId, ResourceLocation entryId) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(entryId, "entryId");

    Set<ResourceLocation> unlockedEntries = this.unlockedEntriesByPlayer.get(playerId);
    return unlockedEntries != null && unlockedEntries.contains(entryId);
  }

  /**
   * Returns the unlocked bestiary entry ids for a player.
   *
   * @param playerId the player UUID
   * @return an immutable snapshot of unlocked entry ids
   */
  public Set<ResourceLocation> unlockedEntries(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");

    Set<ResourceLocation> unlockedEntries = this.unlockedEntriesByPlayer.get(playerId);
    if (unlockedEntries == null || unlockedEntries.isEmpty()) {
      return Set.of();
    }

    return Set.copyOf(unlockedEntries);
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag players = new ListTag();

    this.unlockedEntriesByPlayer.entrySet().stream()
        .filter(entry -> !entry.getValue().isEmpty())
        .sorted(Map.Entry.comparingByKey((first, second) -> first.toString().compareTo(second.toString())))
        .forEach(entry -> {
          CompoundTag playerTag = new CompoundTag();
          playerTag.putUUID(PLAYER_UUID_TAG, entry.getKey());

          ListTag unlockedEntries = new ListTag();
          entry.getValue().stream()
              .sorted((first, second) -> first.toString().compareTo(second.toString()))
              .map(ResourceLocation::toString)
              .map(StringTag::valueOf)
              .forEach(unlockedEntries::add);

          playerTag.put(UNLOCKED_ENTRIES_TAG, unlockedEntries);
          players.add(playerTag);
        });

    tag.put(PLAYERS_TAG, players);
    return tag;
  }

  static BestiaryUnlockSavedData get(DimensionDataStorage dataStorage) {
    Objects.requireNonNull(dataStorage, "dataStorage");
    return dataStorage.computeIfAbsent(BestiaryUnlockSavedData::load, BestiaryUnlockSavedData::new, DATA_NAME);
  }
}