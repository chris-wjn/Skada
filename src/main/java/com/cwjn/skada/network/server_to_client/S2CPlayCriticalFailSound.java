package com.cwjn.skada.network.server_to_client;

import java.util.function.Supplier;

import com.cwjn.skada.client.ClientHandler;
import com.cwjn.skada.util.UtilCombat.CriticalFailSeverity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Plays a critical-fail feedback sound on the receiving client.
 */
public class S2CPlayCriticalFailSound {

  private final CriticalFailSeverity severity;

  /**
   * Creates a critical-fail sound packet.
   *
   * @param severity the critical-fail severity to represent on the client
   */
  public S2CPlayCriticalFailSound(CriticalFailSeverity severity) {
    this.severity = severity;
  }

  /**
   * Encodes the critical-fail severity.
   *
   * @param message the packet being encoded
   * @param buffer the target packet buffer
   */
  public static void encode(S2CPlayCriticalFailSound message, FriendlyByteBuf buffer) {
    buffer.writeEnum(message.severity);
  }

  /**
   * Decodes a critical-fail sound packet from the network buffer.
   *
   * @param buffer the source packet buffer
   * @return the decoded packet
   */
  public static S2CPlayCriticalFailSound decode(FriendlyByteBuf buffer) {
    return new S2CPlayCriticalFailSound(buffer.readEnum(CriticalFailSeverity.class));
  }

  /**
   * Enqueues client-local playback of the critical-fail sound.
   *
   * @param contextSupplier the network context supplier
   */
  public static void handle(S2CPlayCriticalFailSound message, Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context ctx = contextSupplier.get();
    ctx.enqueueWork(() -> ClientHandler.playCriticalFailSound(message.severity));
    ctx.setPacketHandled(true);
  }

}