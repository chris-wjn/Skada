package com.cwjn.skada.client;

import com.cwjn.skada.client.gui.screen.TutorialScreen;
import com.cwjn.skada.network.SkadaNetwork;
import com.cwjn.skada.network.client_to_server.C2STutorial;
import com.cwjn.skada.network.server_to_client.S2CTutorial;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Defers server-requested tutorial presentation until vanilla world setup is ready. */
@Mod.EventBusSubscriber(modid = "skada", value = Dist.CLIENT)
public final class TutorialClient {
    private static S2CTutorial pending;

    private TutorialClient() {}

    /** Accepts only server-issued sessions; close messages affect the matching screen only. */
    public static void receive(S2CTutorial message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (message.open()) pending = message;
        else {
            if (pending != null && pending.session().equals(message.session())) pending = null;
            if (minecraft.screen instanceof TutorialScreen screen && screen.session().equals(message.session())) screen.released();
        }
    }

    /** Requests a replay; the client waits for server protection before opening a pause screen. */
    public static void replay() {
        SkadaNetwork.playerToServer(new C2STutorial(new UUID(0, 0), C2STutorial.REPLAY));
    }

    /** Opens after player/world setup, allowing the loading screen to finish first. */
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || pending == null || minecraft.player == null || minecraft.level == null
            || (minecraft.screen != null && !(minecraft.screen instanceof com.cwjn.skada.client.gui.screen.StatScreen))) return;
        S2CTutorial message = pending;
        pending = null;
        minecraft.setScreen(new TutorialScreen(message.session(), message.introduction()));
    }

    /** A different connection must never inherit a queued tutorial or session token. */
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { pending = null; }
}
