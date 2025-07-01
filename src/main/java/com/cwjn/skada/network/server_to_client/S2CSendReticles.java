package com.cwjn.skada.network.server_to_client;

import com.cwjn.skada.client.ClientHandler;
import com.cwjn.skada.client.hud.ReticleShape;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CSendReticles {

    private final List<ReticleShape> reticles;

    public S2CSendReticles(List<ReticleShape> reticles) {
        this.reticles = reticles;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(reticles.size());
        for (ReticleShape r : reticles) {
            buf.writeJsonWithCodec(ReticleShape.CODEC, r);
        }
    }

    public static S2CSendReticles decode(FriendlyByteBuf buf) {
        List<ReticleShape> retList = new ArrayList<>();
        for (int i = 0; i < buf.readInt(); i++) {
            retList.add(buf.readJsonWithCodec(ReticleShape.CODEC));
        }
        return new S2CSendReticles(retList);
    }

    public static void handle(S2CSendReticles packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientHandler.updateReticles(packet.reticles);
        });
        ctx.get().setPacketHandled(true);
    }

}
