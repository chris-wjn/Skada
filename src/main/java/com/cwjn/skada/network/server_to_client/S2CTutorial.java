package com.cwjn.skada.network.server_to_client;

import com.cwjn.skada.client.TutorialClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Opens or releases a protected tutorial screen after the server establishes its session. */
public record S2CTutorial(UUID session, boolean open, boolean introduction) {
    /** Encodes the server session and presentation mode. */
    public static void encode(S2CTutorial msg, FriendlyByteBuf buf) { buf.writeUUID(msg.session); buf.writeBoolean(msg.open); buf.writeBoolean(msg.introduction); }
    /** Decodes the server session and presentation mode. */
    public static S2CTutorial decode(FriendlyByteBuf buf) { return new S2CTutorial(buf.readUUID(), buf.readBoolean(), buf.readBoolean()); }
    /** Dispatches presentation on the client game thread. */
    public static void handle(S2CTutorial msg, Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> TutorialClient.receive(msg));
        supplier.get().setPacketHandled(true);
    }
}
