package com.cwjn.skada.network.client_to_server;

import com.cwjn.skada.event.Onboarding;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Requests a replay, explicit completion, or interruption of a matching server session. */
public record C2STutorial(UUID session, int action) {
    public static final int REPLAY = 0, COMPLETE = 1, INTERRUPT = 2;
    /** Writes the session identity and bounded request kind. */
    public static void encode(C2STutorial msg, FriendlyByteBuf buf) { buf.writeUUID(msg.session); buf.writeByte(msg.action); }
    /** Reads a request; invalid actions are ignored by the handler. */
    public static C2STutorial decode(FriendlyByteBuf buf) { return new C2STutorial(buf.readUUID(), buf.readUnsignedByte()); }
    /** All state changes run on the server and validate the current session. */
    public static void handle(C2STutorial msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;
            if (msg.action == REPLAY) Onboarding.replay(player);
            else if (msg.action == COMPLETE || msg.action == INTERRUPT) Onboarding.close(player, msg.session, msg.action == COMPLETE);
        });
        context.setPacketHandled(true);
    }
}
