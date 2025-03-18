package com.cwjn.skada.network.server_to_client;

import com.cwjn.skada.client.ClientHandler;
import com.cwjn.skada.data.damage.WeaponInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CUpdateWeaponInfo {

    private final int entityID;
    private final CompoundTag info;

    public S2CUpdateWeaponInfo(WeaponInfo info, int entityID) {
        this.info = info.toCompoundTag();
        this.entityID = entityID;
    }

    public S2CUpdateWeaponInfo(CompoundTag tag, int entityID) {
        this.info = tag;
        this.entityID = entityID;
    }

    public static void encode(S2CUpdateWeaponInfo msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.info);
        buf.writeInt(msg.entityID);
    }

    public static S2CUpdateWeaponInfo decode(FriendlyByteBuf buf) {
        return new S2CUpdateWeaponInfo(buf.readNbt(), buf.readInt());
    }

    public static void handle(S2CUpdateWeaponInfo msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientHandler.updateClientWeaponInfo(msg.info, msg.entityID);
        });
        ctx.get().setPacketHandled(true);
    }

}
