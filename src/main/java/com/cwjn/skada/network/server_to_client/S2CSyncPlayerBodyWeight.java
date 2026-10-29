package com.cwjn.skada.network.server_to_client;

import com.cwjn.skada.client.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Synchronizes the server-resolved body weight used by the local player journal preview. */
public record S2CSyncPlayerBodyWeight(double weightKg) {

  /** Writes the resolved body weight in kilograms to the outgoing packet buffer. */
  public static void encode(S2CSyncPlayerBodyWeight message, FriendlyByteBuf buffer) {
    buffer.writeDouble(message.weightKg());
  }

  /** Reads a resolved body weight in kilograms from the incoming packet buffer. */
  public static S2CSyncPlayerBodyWeight decode(FriendlyByteBuf buffer) {
    return new S2CSyncPlayerBodyWeight(buffer.readDouble());
  }

  /** Applies the server-provided body weight to the client journal cache. */
  public static void handle(S2CSyncPlayerBodyWeight message, Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    context.enqueueWork(() -> ClientHandler.updatePlayerBodyWeight(message.weightKg()));
    context.setPacketHandled(true);
  }
}
