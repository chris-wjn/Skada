package com.cwjn.skada.network.server_to_client;

import com.cwjn.skada.client.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Syncs the authoritative unlocked bestiary entry ids for one player and the
 * subset that should be treated as newly unlocked on the client.
 */
public class S2CSyncBestiaryUnlocks {

    private final List<ResourceLocation> unlockedEntryIds;
    private final List<ResourceLocation> newlyUnlockedEntryIds;

    public S2CSyncBestiaryUnlocks(Collection<ResourceLocation> unlockedEntryIds,
                                  Collection<ResourceLocation> newlyUnlockedEntryIds) {
        this.unlockedEntryIds = normalizeIds(unlockedEntryIds);
        this.newlyUnlockedEntryIds = normalizeNewlyUnlockedIds(newlyUnlockedEntryIds, this.unlockedEntryIds);
    }

    public List<ResourceLocation> unlockedEntryIds() {
        return unlockedEntryIds;
    }

    public List<ResourceLocation> newlyUnlockedEntryIds() {
        return newlyUnlockedEntryIds;
    }

    public static void encode(S2CSyncBestiaryUnlocks msg, FriendlyByteBuf buf) {
        writeIds(buf, msg.unlockedEntryIds);
        writeIds(buf, msg.newlyUnlockedEntryIds);
    }

    public static S2CSyncBestiaryUnlocks decode(FriendlyByteBuf buf) {
        return new S2CSyncBestiaryUnlocks(readIds(buf), readIds(buf));
    }

    public static void handle(S2CSyncBestiaryUnlocks msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientHandler.updateBestiaryUnlocks(msg.unlockedEntryIds, msg.newlyUnlockedEntryIds));
        ctx.get().setPacketHandled(true);
    }

    private static void writeIds(FriendlyByteBuf buf, List<ResourceLocation> ids) {
        buf.writeInt(ids.size());
        for (ResourceLocation id : ids) {
            buf.writeResourceLocation(id);
        }
    }

    private static List<ResourceLocation> readIds(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ResourceLocation> ids = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ids.add(buf.readResourceLocation());
        }
        return ids;
    }

    private static List<ResourceLocation> normalizeIds(Collection<ResourceLocation> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    private static List<ResourceLocation> normalizeNewlyUnlockedIds(Collection<ResourceLocation> ids,
                                                                    List<ResourceLocation> unlockedEntryIds) {
        if (ids == null || ids.isEmpty() || unlockedEntryIds.isEmpty()) {
            return List.of();
        }

        Set<ResourceLocation> unlockedSet = Set.copyOf(unlockedEntryIds);
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(unlockedSet::contains)
                .distinct()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }
}