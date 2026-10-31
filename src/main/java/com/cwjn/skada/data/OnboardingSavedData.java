package com.cwjn.skada.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-world completion history; transient protection is deliberately never saved. */
public final class OnboardingSavedData extends SavedData {
    private final Set<UUID> completed = new HashSet<>();

    /** Resolves the world's shared completion history, including from other dimensions. */
    public static OnboardingSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            OnboardingSavedData::load, OnboardingSavedData::new, "skada_onboarding");
    }

    /** Reads completion history while ignoring malformed entries. */
    public static OnboardingSavedData load(CompoundTag tag) {
        OnboardingSavedData data = new OnboardingSavedData();
        ListTag entries = tag.getList("completed", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            if (entries.getCompound(i).hasUUID("player")) data.completed.add(entries.getCompound(i).getUUID("player"));
        }
        return data;
    }

    /** Checks whether this player explicitly finished or skipped onboarding. */
    public boolean isCompleted(UUID player) { return completed.contains(player); }

    /** Records an explicit completion, marking world data dirty only on change. */
    public void complete(UUID player) { if (completed.add(player)) setDirty(); }

    /** Writes only completion history, never immunity or active-session state. */
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        completed.stream().sorted().forEach(id -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", id);
            entries.add(entry);
        });
        tag.put("completed", entries);
        return tag;
    }
}
