package com.cwjn.skada.network.server_to_client;

import com.cwjn.skada.client.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class S2CCreateDamageIndicator {

    private final double x, y, z;
    private final float f;
    private final UUID id;
    private final int colour;
    private final float horizontalOffset;

    public S2CCreateDamageIndicator(double x, double y, double z, float f, float h, int colour, UUID id) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.f = f;
        this.horizontalOffset = h;
        this.colour = colour;
        this.id = id;
    }

    public static void encode(S2CCreateDamageIndicator message, FriendlyByteBuf buffer) {
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeFloat(message.f);
        buffer.writeFloat(message.horizontalOffset);
        buffer.writeInt(message.colour);
        buffer.writeUUID(message.id);
    }

    public static S2CCreateDamageIndicator decode(FriendlyByteBuf buffer) {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        float f = buffer.readFloat();
        float h = buffer.readFloat();
        int colour = buffer.readInt();
        UUID id = buffer.readUUID();
        return new S2CCreateDamageIndicator(x, y, z, f, h, colour, id);
    }

    public static void handle(S2CCreateDamageIndicator message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> ClientHandler.createDamageIndicator(message.x, message.y, message.z, message.f, message.colour, message.horizontalOffset, message.id));
        ctx.setPacketHandled(true);
    }

}
