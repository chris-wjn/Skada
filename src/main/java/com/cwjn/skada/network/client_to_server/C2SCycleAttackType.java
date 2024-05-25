package com.cwjn.skada.network.client_to_server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;

public class C2SCycleAttackType {

    private final int value;

    public C2SCycleAttackType(int value) {
        this.value = value;
    }

    public static void encode(C2SCycleAttackType msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.value);
    }

    public static C2SCycleAttackType decode(FriendlyByteBuf buffer) {
        return new C2SCycleAttackType(buffer.readInt());
    }

    public static void handle(C2SCycleAttackType msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> ctxSupplier.get().getSender().getMainHandItem().getTag().putInt(CURRENT_ATTACK_TYPE_TAG_KEY, msg.value));
        ctxSupplier.get().setPacketHandled(true);
    }

}
