package com.cwjn.skada.network.server_to_client;

import com.cwjn.skada.client.ClientHandler;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.mojang.serialization.DataResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class S2CSendWeaponInfoMap {

    private final Map<String, Map<String, WeaponInfo>> map;

    public S2CSendWeaponInfoMap(Map<String, Map<String, WeaponInfo>> map) {
        this.map = map;
    }

    public static void encode(S2CSendWeaponInfoMap msg, FriendlyByteBuf buf) {
        buf.writeJsonWithCodec(WeaponInfo.STRING_STRING_MAP_CODEC, msg.map);
    }

    public static S2CSendWeaponInfoMap decode(FriendlyByteBuf buf) {
        return new S2CSendWeaponInfoMap(buf.readJsonWithCodec(WeaponInfo.STRING_STRING_MAP_CODEC));
    }

    public static void handle(S2CSendWeaponInfoMap msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientHandler.updateWeaponInfos(msg.map);
        });
        ctx.get().setPacketHandled(true);
    }

}
