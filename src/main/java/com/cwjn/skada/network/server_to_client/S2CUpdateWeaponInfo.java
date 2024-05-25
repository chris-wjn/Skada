package com.cwjn.skada.network.server_to_client;

import com.cwjn.skada.client.ClientHandler;
import com.cwjn.skada.data.damage.WeaponInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CUpdateWeaponInfo {

    private final CompoundTag info;

    public S2CUpdateWeaponInfo(WeaponInfo info) {
        this.info = info.toCompoundTag();
    }

    public S2CUpdateWeaponInfo(CompoundTag tag) {
        this.info = tag;
    }

    public static void encode(S2CUpdateWeaponInfo msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.info);
    }

    public static S2CUpdateWeaponInfo decode(FriendlyByteBuf buf) {
        return new S2CUpdateWeaponInfo(buf.readNbt());
    }

    public static void handle(S2CUpdateWeaponInfo msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientHandler.updateClientWeaponInfo(msg.info);
        });
        ctx.get().setPacketHandled(true);
    }

}
