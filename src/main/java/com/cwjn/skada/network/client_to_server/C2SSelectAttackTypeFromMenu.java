package com.cwjn.skada.network.client_to_server;

import com.cwjn.skada.data.damage.AttackSelection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Requests a supported attack ID for a slot in the currently open menu. */
public record C2SSelectAttackTypeFromMenu(ResourceLocation attackId, int containerID, int slot) {
    /** Encodes the stable selection and its menu context. */
    public static void encode(C2SSelectAttackTypeFromMenu msg, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(msg.attackId);
        buffer.writeInt(msg.containerID);
        buffer.writeInt(msg.slot);
    }
    /** Decodes the stable selection and its menu context. */
    public static C2SSelectAttackTypeFromMenu decode(FriendlyByteBuf buffer) {
        return new C2SSelectAttackTypeFromMenu(buffer.readResourceLocation(), buffer.readInt(), buffer.readInt());
    }
    /** Rejects closed/replaced menus, invalid slots and attacks unsupported by the server's stack. */
    public static void handle(C2SSelectAttackTypeFromMenu msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.containerMenu.containerId != msg.containerID
                    || msg.slot < 0 || msg.slot >= player.containerMenu.slots.size()
                    || !player.containerMenu.stillValid(player)) return;
            if (com.cwjn.skada.event.Onboarding.isProtected(player) || player.isSpectator()
                    || player.getAttackStrengthScale(0.5f) <= 0.9) {
                player.containerMenu.broadcastFullState();
                return;
            }
            if (AttackSelection.select(player.containerMenu.getSlot(msg.slot).getItem(), msg.attackId)) {
                player.containerMenu.broadcastChanges();
            } else player.containerMenu.broadcastFullState();
        });
        context.setPacketHandled(true);
    }
}
