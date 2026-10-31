package com.cwjn.skada.event;

import com.cwjn.skada.data.OnboardingSavedData;
import com.cwjn.skada.network.SkadaNetwork;
import com.cwjn.skada.network.server_to_client.S2CTutorial;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-owned, transient tutorial sessions. Never changes saved ability or potion flags. */
@Mod.EventBusSubscriber(modid = "skada")
public final class Onboarding {
    private static final Map<ServerPlayer, Session> SESSIONS = new IdentityHashMap<>();

    private Onboarding() {}

    /** Captures an immutable anchor and unguessable identity for one protected screen session. */
    private record Session(UUID id, Vec3 anchor) {}

    /** Tests authoritative protection on the server thread only. */
    public static boolean isProtected(Entity entity) {
        return entity instanceof ServerPlayer player && player.getServer().isSameThread() && SESSIONS.containsKey(player);
    }

    /** Returns the current token for server-side lifecycle coordination, or null when inactive. */
    static UUID sessionId(ServerPlayer player) {
        Session session = SESSIONS.get(player);
        return session == null ? null : session.id();
    }

    /** Establishes protection before the joining player is inserted into the world. */
    @SubscribeEvent public static void beforeJoin(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null && !OnboardingSavedData.get(player.serverLevel()).isCompleted(player.getUUID())) begin(player);
    }

    /** Opens the questionnaire after vanilla join setup; resource synchronization precedes it. */
    @SubscribeEvent public static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SESSIONS.containsKey(player)) {
            player.stopRiding();
            Session session = SESSIONS.get(player);
            SESSIONS.put(player, new Session(session.id(), player.position()));
            SkadaNetwork.serverToPlayer(new S2CTutorial(session.id(), true, true), player);
        }
    }

    /** Starts a replay only for a live player with no existing protected session. */
    public static void replay(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || SESSIONS.containsKey(player)) return;
        begin(player);
        SkadaNetwork.serverToPlayer(new S2CTutorial(SESSIONS.get(player).id(), true, false), player);
    }

    /** Establishes protection and removes existing mob targets without altering invisibility. */
    private static void begin(ServerPlayer player) {
        if (SESSIONS.containsKey(player)) return;
        player.stopRiding();
        player.stopUsingItem();
        SESSIONS.put(player, new Session(UUID.randomUUID(), player.position()));
        for (Entity entity : player.serverLevel().getAllEntities()) {
            if (entity instanceof Mob mob && mob.getTarget() == player) mob.setTarget(null);
        }
    }

    /** Validates completion against the current session; stale packets cannot end a replay. */
    public static void close(ServerPlayer player, UUID sessionId, boolean completed) {
        Session session = SESSIONS.get(player);
        if (session == null || !session.id().equals(sessionId)) return;
        if (completed) OnboardingSavedData.get(player.serverLevel()).complete(player.getUUID());
        end(player);
    }

    /** Ends only our transient protection and tells the matching client screen to close. */
    private static void end(ServerPlayer player) {
        Session session = SESSIONS.remove(player);
        if (session != null) SkadaNetwork.serverToPlayer(new S2CTutorial(session.id(), false, false), player);
    }

    /** Keeps the anchored player still even when environmental forces or other mods move them. */
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Session session = SESSIONS.get(player);
        if (session == null) return;
        if (!player.isAlive()) { end(player); return; }
        if (player.isPassenger()) player.stopRiding();
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        if (player.position().distanceToSqr(session.anchor()) > 0.000001) {
            Vec3 p = session.anchor();
            player.connection.teleport(p.x, p.y, p.z, player.getYRot(), player.getXRot());
        }
    }

    /** Reasserts the anchor when a protected client sends a movement packet. */
    public static void correctPosition(ServerPlayer player) {
        Session session = SESSIONS.get(player);
        if (session != null) {
            Vec3 p = session.anchor();
            player.connection.teleport(p.x, p.y, p.z, player.getYRot(), player.getXRot());
        }
    }

    /** Clears new and retained AI targets rather than cancelling and preserving an old target. */
    @SubscribeEvent public static void targeting(LivingChangeTargetEvent event) {
        if (isProtected(event.getNewTarget())) event.setNewTarget(null);
    }

    /** Logout never records completion and cannot leave a saved invulnerability flag behind. */
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SESSIONS.remove(player);
    }

    /** Dimension transitions interrupt the tutorial so an old anchor cannot teleport across worlds. */
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) end(player);
    }

    /** Entity replacement cannot inherit a transient session. */
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer player) SESSIONS.remove(player);
    }

    /** Releases references when the hosting server stops, including integrated-world changes. */
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        SESSIONS.keySet().removeIf(player -> player.getServer() == event.getServer());
    }
}
