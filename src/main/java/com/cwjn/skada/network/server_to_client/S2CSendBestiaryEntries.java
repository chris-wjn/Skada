package com.cwjn.skada.network.server_to_client;

import com.cwjn.skada.client.ClientHandler;
import com.cwjn.skada.data.mob.BestiaryEntryView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CSendBestiaryEntries {

    private final List<BestiaryEntryView> entries;

    public S2CSendBestiaryEntries(List<BestiaryEntryView> entries) {
        this.entries = List.copyOf(entries);
    }

    public List<BestiaryEntryView> entries() {
        return entries;
    }

    public static void encode(S2CSendBestiaryEntries msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entries.size());
        for (BestiaryEntryView entry : msg.entries) {
            buf.writeJsonWithCodec(BestiaryEntryView.CODEC, entry);
        }
    }

    public static S2CSendBestiaryEntries decode(FriendlyByteBuf buf) {
        List<BestiaryEntryView> entries = new ArrayList<>();
        int entryCount = buf.readInt();
        for (int index = 0; index < entryCount; index++) {
            entries.add(buf.readJsonWithCodec(BestiaryEntryView.CODEC));
        }
        return new S2CSendBestiaryEntries(entries);
    }

    public static void handle(S2CSendBestiaryEntries msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientHandler.updateBestiaryEntries(msg.entries));
        ctx.get().setPacketHandled(true);
    }
}