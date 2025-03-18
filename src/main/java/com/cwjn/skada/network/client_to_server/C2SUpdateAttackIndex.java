package com.cwjn.skada.network.client_to_server;

import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.util.SkadaEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;

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
            player.getMainHandItem().getTag().putInt(CURRENT_ATTACK_TYPE_TAG_KEY, msg.index);
        });
        ctxSupplier.get().setPacketHandled(true);
    }

}
