package com.cwjn.skada.network.server_to_client;

import com.cwjn.skada.client.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CSyncSkyDarken {

    private final int skyDarken;

    public S2CSyncSkyDarken(int skyDarken) {
        this.skyDarken = skyDarken;
    }

    public static void encode(S2CSyncSkyDarken msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.skyDarken);
    }

    public static S2CSyncSkyDarken decode(FriendlyByteBuf buf) {
        return new S2CSyncSkyDarken(buf.readInt());
    }

    public static void handle(S2CSyncSkyDarken packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ClientHandler.skyDarken = packet.skyDarken;
        });
        context.get().setPacketHandled(true);
    }

}
