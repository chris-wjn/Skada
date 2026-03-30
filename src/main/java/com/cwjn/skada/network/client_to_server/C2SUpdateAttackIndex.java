package com.cwjn.skada.network.client_to_server;

import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SUpdateAttackIndex {

    private final int index;

    public C2SUpdateAttackIndex(int index) {
        this.index = index;
    }

    public static void encode(C2SUpdateAttackIndex msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.index);
    }

    public static C2SUpdateAttackIndex decode(FriendlyByteBuf buffer) {
        return new C2SUpdateAttackIndex(buffer.readInt());
    }

    public static void handle(C2SUpdateAttackIndex msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> {
            ServerPlayer player = ctxSupplier.get().getSender();
            if (player != null) {
                UtilData.setAttackTypeIndex(player.getMainHandItem(), msg.index);
            }
        });
        ctxSupplier.get().setPacketHandled(true);
    }

}
