package com.cwjn.skada.network.client_to_server;

import com.cwjn.skada.data.damage.AttackSelection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Requests a supported attack ID for a hand in the selected hotbar context. */
public record C2SSelectAttackType(ResourceLocation attackId, InteractionHand hand, int hotbarSlot) {
    /** Encodes the stable selection and its hand/hotbar context. */
    public static void encode(C2SSelectAttackType msg, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(msg.attackId);
        buffer.writeEnum(msg.hand);
        buffer.writeVarInt(msg.hotbarSlot);
    }
    /** Decodes the stable selection and its hand/hotbar context. */
    public static C2SSelectAttackType decode(FriendlyByteBuf buffer) {
        return new C2SSelectAttackType(buffer.readResourceLocation(), buffer.readEnum(InteractionHand.class), buffer.readVarInt());
    }
    /** Validates against the server's held item before accepting client prediction. */
    public static void handle(C2SSelectAttackType msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            InteractionHand expectedHand = player.getMainHandItem().isEmpty() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (com.cwjn.skada.event.Onboarding.isProtected(player) || player.isSpectator()
                    || player.getAttackStrengthScale(0.5f) <= 0.9 || msg.hand != expectedHand
                    || msg.hotbarSlot < 0 || msg.hotbarSlot >= 9 || player.getInventory().selected != msg.hotbarSlot
                    || player.containerMenu != player.inventoryMenu) {
                player.containerMenu.broadcastFullState();
                return;
            }
            if (AttackSelection.select(player.getItemInHand(msg.hand), msg.attackId)) player.containerMenu.broadcastChanges();
            else player.containerMenu.broadcastFullState();
        });
        context.setPacketHandled(true);
    }
}
