package com.cwjn.skada.network.client_to_server;

import com.cwjn.skada.data.damage.AttackSelection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Requests a supported attack ID for the currently held stack. */
public record C2SSelectAttackType(ResourceLocation attackId) {
    /** Encodes the stable selection. */
    public static void encode(C2SSelectAttackType msg, FriendlyByteBuf buffer) { buffer.writeResourceLocation(msg.attackId); }
    /** Decodes the stable selection. */
    public static C2SSelectAttackType decode(FriendlyByteBuf buffer) { return new C2SSelectAttackType(buffer.readResourceLocation()); }
    /** Validates against the server's held item before accepting client prediction. */
    public static void handle(C2SSelectAttackType msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                if (AttackSelection.select(player.getMainHandItem(), msg.attackId)) player.containerMenu.broadcastChanges();
                else player.containerMenu.broadcastFullState();
            }
        });
        context.setPacketHandled(true);
    }
}
