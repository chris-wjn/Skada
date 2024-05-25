package com.cwjn.skada.network.client_to_server;

import com.cwjn.skada.util.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;

public class C2SAddWeaponTag {

    private final int containerID;
    private final int slot;

    public C2SAddWeaponTag(int containerID, int slot) {
        this.containerID = containerID;
        this.slot = slot;
    }

    public static void encode(C2SAddWeaponTag msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.containerID);
        buffer.writeInt(msg.slot);
    }

    public static C2SAddWeaponTag decode(FriendlyByteBuf buffer) {
        return new C2SAddWeaponTag(buffer.readInt(), buffer.readInt());
    }

    public static void handle(C2SAddWeaponTag msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctxSupplier.get().enqueueWork(() -> {
            if (ctx.getSender() != null && msg.containerID == ctx.getSender().containerMenu.containerId) {
                Util.addWeaponInfoTagIfNotExists(ctx.getSender().containerMenu.getSlot(msg.slot).getItem());
            }
        });
        ctxSupplier.get().setPacketHandled(true);
    }

}
