package com.cwjn.skada.network.server_to_client;

import com.cwjn.skada.client.ClientHandler;
import com.cwjn.skada.data.armour.ArmourInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class S2CSendArmourInfoMap {

    private final Map<String, Map<String, ArmourInfo>> map;
    public S2CSendArmourInfoMap(Map<String, Map<String, ArmourInfo>> map) {
        this.map = map;
    }

    public static void encode(S2CSendArmourInfoMap msg, FriendlyByteBuf buf) {
        buf.writeJsonWithCodec(ArmourInfo.STRING_STRING_MAP_CODEC, msg.map);
    }

    public static S2CSendArmourInfoMap decode(FriendlyByteBuf buf) {
        return new S2CSendArmourInfoMap(buf.readJsonWithCodec(ArmourInfo.STRING_STRING_MAP_CODEC));
    }

    public static void handle(S2CSendArmourInfoMap msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientHandler.updateArmourInfos(msg.map);
        });
        ctx.get().setPacketHandled(true);
    }

}
