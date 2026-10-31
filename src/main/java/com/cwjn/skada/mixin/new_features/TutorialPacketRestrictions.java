package com.cwjn.skada.mixin.new_features;

import com.cwjn.skada.event.Onboarding;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rejects gameplay packets before mutation while allowing connection/teleport/session traffic. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class TutorialPacketRestrictions {
    @Shadow public ServerPlayer player;
    private static final String THREAD_GUARD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V";

    /** Prevents player and vehicle movement on the authoritative game thread. */
    @Inject(method = {"handleMovePlayer", "handleMoveVehicle"}, at = @At(value = "INVOKE", target = THREAD_GUARD, shift = At.Shift.AFTER), cancellable = true)
    private void skada$movement(CallbackInfo ci) {
        if (Onboarding.isProtected(player)) { Onboarding.correctPosition(player); ci.cancel(); }
    }

    /** Blocks inventory, combat and use actions before either inventory removal or world mutation. */
    @Inject(method = {"handlePlayerInput", "handleInteract", "handleContainerClick", "handleContainerButtonClick",
        "handleSetCreativeModeSlot", "handleSetCarriedItem", "handlePickItem", "handlePlaceRecipe",
        "handlePlayerCommand", "handlePlayerAbilities"},
        at = @At(value = "INVOKE", target = THREAD_GUARD, shift = At.Shift.AFTER), cancellable = true)
    private void skada$action(CallbackInfo ci) {
        if (Onboarding.isProtected(player)) { player.containerMenu.broadcastFullState(); ci.cancel(); }
    }

    /** Acknowledges rejected block actions so prediction cannot remain pending after exit. */
    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = THREAD_GUARD, shift = At.Shift.AFTER), cancellable = true)
    private void skada$blockAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (Onboarding.isProtected(player)) {
            player.connection.ackBlockChangesUpTo(packet.getSequence());
            player.containerMenu.broadcastFullState();
            ci.cancel();
        }
    }

    /** Rejects block use while acknowledging vanilla's prediction sequence. */
    @Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = THREAD_GUARD, shift = At.Shift.AFTER), cancellable = true)
    private void skada$useOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (Onboarding.isProtected(player)) { player.connection.ackBlockChangesUpTo(packet.getSequence()); player.containerMenu.broadcastFullState(); ci.cancel(); }
    }

    /** Rejects item use while acknowledging vanilla's prediction sequence. */
    @Inject(method = "handleUseItem", at = @At(value = "INVOKE", target = THREAD_GUARD, shift = At.Shift.AFTER), cancellable = true)
    private void skada$use(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (Onboarding.isProtected(player)) { player.connection.ackBlockChangesUpTo(packet.getSequence()); player.containerMenu.broadcastFullState(); ci.cancel(); }
    }
}
