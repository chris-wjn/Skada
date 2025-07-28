package com.cwjn.skada.network.client_to_server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;

public class C2SUpdateAttackIndexFromMenu {

    private final int value;
    private final int containerID;
    private final int slot;

    public C2SUpdateAttackIndexFromMenu(int value, int containerID, int slot) {
        this.value = value;
        this.containerID = containerID;
        this.slot = slot;
    }

    public static void encode(C2SUpdateAttackIndexFromMenu msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.value);
        buffer.writeInt(msg.containerID);
        buffer.writeInt(msg.slot);
    }

    public static C2SUpdateAttackIndexFromMenu decode(FriendlyByteBuf buffer) {
        return new C2SUpdateAttackIndexFromMenu(buffer.readInt(), buffer.readInt(), buffer.readInt());
    }

    public static void handle(C2SUpdateAttackIndexFromMenu msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctxSupplier.get().enqueueWork(() -> {
            if (ctx.getSender() != null && msg.containerID == ctx.getSender().containerMenu.containerId) {
                ctx.getSender().containerMenu.getSlot(msg.slot).getItem().getTag().putInt(CURRENT_ATTACK_TYPE_TAG_KEY, msg.value);
            }
        });
        ctxSupplier.get().setPacketHandled(true);
    }

}
