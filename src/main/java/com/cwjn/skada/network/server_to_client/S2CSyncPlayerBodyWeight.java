package com.cwjn.skada.network.server_to_client;

import com.cwjn.skada.client.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Synchronizes server-resolved body and total weight for the local player journal. */
public record S2CSyncPlayerBodyWeight(double bodyWeightKg, double totalWeightKg) {

  public static void encode(S2CSyncPlayerBodyWeight message, FriendlyByteBuf buffer) {
    buffer.writeDouble(message.bodyWeightKg());
    buffer.writeDouble(message.totalWeightKg());
  }

  public static S2CSyncPlayerBodyWeight decode(FriendlyByteBuf buffer) {
    return new S2CSyncPlayerBodyWeight(buffer.readDouble(), buffer.readDouble());
  }

  public static void handle(S2CSyncPlayerBodyWeight message, Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    context.enqueueWork(() -> ClientHandler.updatePlayerWeights(message.bodyWeightKg(), message.totalWeightKg()));
    context.setPacketHandled(true);
  }
}
