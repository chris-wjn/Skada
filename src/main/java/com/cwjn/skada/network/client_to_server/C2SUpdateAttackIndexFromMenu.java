package com.cwjn.skada.network.client_to_server;

import com.cwjn.skada.util.UtilData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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
                UtilData.setAttackTypeIndex(ctx.getSender().containerMenu.getSlot(msg.slot).getItem(), msg.value);
            }
        });
        ctxSupplier.get().setPacketHandled(true);
    }

}
