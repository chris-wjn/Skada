package com.cwjn.skada.event;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.OnboardingSavedData;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Exercises actual mixins, server packet handlers and world completion persistence. */
@GameTestHolder(Skada.MODID)
@PrefixGameTestTemplate(false)
public final class OnboardingGameTests {
    private OnboardingGameTests() {}

    /** Creates a real server player/packet listener without adding a fake connection to the player list. */
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
            new GameProfile(UUID.randomUUID(), "tutorial-test"));
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), player);
        player.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        return player;
    }

    /** Finishing a protected session restores enemy eligibility and never persists ability flags. */
    @GameTest(templateNamespace = Skada.MODID, template = "damage_handler_5x4x5")
    public static void protectedPlayerRejectsDamageAndMobTargets(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            Onboarding.replay(player);
            UUID session = Onboarding.sessionId(player);
            helper.assertTrue(session != null, "Replay must establish a server-owned session");
            helper.assertTrue(player.isInvulnerableTo(player.damageSources().fellOutOfWorld()), "Even bypass damage must be blocked");
            float health = player.getHealth();
            player.hurt(player.damageSources().genericKill(), 5);
            helper.assertTrue(player.getHealth() == health, "Tutorial player must not lose health");
            helper.assertTrue(!player.canBeSeenAsEnemy(), "Normal AI eligibility must exclude protected players");
            Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 1);
            zombie.setTarget(player);
            helper.assertTrue(zombie.getTarget() == null, "Target-change hooks must clear protected targets");
            Onboarding.close(player, session, true);
            helper.assertTrue(!Onboarding.isProtected(player) && player.canBeSeenAsEnemy(), "Completion must end targeting exclusion");
            helper.assertTrue(!player.isInvulnerable() && !player.getAbilities().invulnerable, "Saved invulnerability flags must remain unchanged");
            helper.assertTrue(OnboardingSavedData.get(helper.getLevel()).isCompleted(player.getUUID()), "Explicit completion must persist");
            helper.succeed();
        } finally { Onboarding.logout(new PlayerEvent.PlayerLoggedOutEvent(player)); }
    }

    /** Real packet handlers reject movement, drops and creative inventory edits before mutation. */
    @GameTest(templateNamespace = Skada.MODID, template = "damage_handler_5x4x5")
    public static void protectedPlayerCannotMoveDropOrEditInventory(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            player.setGameMode(GameType.CREATIVE);
            player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
            Onboarding.replay(player);
            var anchor = player.position();
            player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(anchor.x + 10, anchor.y, anchor.z, true));
            helper.assertTrue(player.position().distanceToSqr(anchor) < 0.000001, "Movement packet must not move a protected player");
            player.connection.handlePlayerAction(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.DROP_ITEM, BlockPos.ZERO, Direction.DOWN));
            helper.assertTrue(player.getMainHandItem().is(Items.IRON_SWORD), "Blocked drop must not remove inventory items");
            player.connection.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(36, new ItemStack(Items.DIAMOND)));
            helper.assertTrue(player.getMainHandItem().is(Items.IRON_SWORD), "Creative slot edits must be rejected while protected");
            Onboarding.close(player, Onboarding.sessionId(player), false);
            player.connection.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(36, new ItemStack(Items.DIAMOND)));
            helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND), "Inventory actions must work again after interruption");
            helper.succeed();
        } finally { Onboarding.logout(new PlayerEvent.PlayerLoggedOutEvent(player)); }
    }

    /** First-join policy, stale tokens and unexpected logout cannot silently mark completion. */
    @GameTest(templateNamespace = Skada.MODID, template = "damage_handler_5x4x5")
    public static void interruptedJoinRepeatsAndStaleTokensCannotCloseReplay(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            Onboarding.beforeJoin(new OnDatapackSyncEvent(player.getServer().getPlayerList(), player));
            UUID first = Onboarding.sessionId(player);
            helper.assertTrue(first != null, "First join must start protected onboarding");
            Onboarding.close(player, UUID.randomUUID(), true);
            helper.assertTrue(Onboarding.isProtected(player), "A fabricated token must not release protection");
            Onboarding.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
            helper.assertTrue(!OnboardingSavedData.get(helper.getLevel()).isCompleted(player.getUUID()), "Logout must not record completion");
            Onboarding.beforeJoin(new OnDatapackSyncEvent(player.getServer().getPlayerList(), player));
            UUID second = Onboarding.sessionId(player);
            helper.assertTrue(second != null && !second.equals(first), "Rejoin must receive a new token");
            Onboarding.close(player, first, true);
            helper.assertTrue(Onboarding.isProtected(player), "An old completion must not close a newer session");
            Onboarding.close(player, second, true);
            Onboarding.beforeJoin(new OnDatapackSyncEvent(player.getServer().getPlayerList(), player));
            helper.assertTrue(!Onboarding.isProtected(player), "Completed players must not be prompted on rejoin");
            helper.succeed();
        } finally { Onboarding.logout(new PlayerEvent.PlayerLoggedOutEvent(player)); }
    }

    /** Protection owns no invisibility/creative flags and dimension transitions interrupt safely. */
    @GameTest(templateNamespace = Skada.MODID, template = "damage_handler_5x4x5")
    public static void preservesExistingAbilitiesAndCleansUpDimensionTransition(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            player.setInvulnerable(true);
            player.setInvisible(true);
            player.getAbilities().invulnerable = true;
            Onboarding.replay(player);
            Onboarding.dimension(new PlayerEvent.PlayerChangedDimensionEvent(player, net.minecraft.world.level.Level.OVERWORLD, net.minecraft.world.level.Level.NETHER));
            helper.assertTrue(!Onboarding.isProtected(player), "Dimension transition must release the old anchor");
            helper.assertTrue(player.isInvulnerable() && player.isInvisible() && player.getAbilities().invulnerable,
                "Tutorial cleanup must preserve pre-existing abilities and invisibility");
            helper.assertTrue(!OnboardingSavedData.get(helper.getLevel()).isCompleted(player.getUUID()), "Transition is interruption, not completion");
            helper.succeed();
        } finally { Onboarding.logout(new PlayerEvent.PlayerLoggedOutEvent(player)); }
    }

    /** Saves only explicit UUID completions and round-trips them without active protection. */
    @GameTest(templateNamespace = Skada.MODID, template = "damage_handler_5x4x5")
    public static void completionHistoryRoundTrips(GameTestHelper helper) {
        OnboardingSavedData data = new OnboardingSavedData();
        UUID completed = UUID.randomUUID(), unseen = UUID.randomUUID();
        data.complete(completed);
        OnboardingSavedData loaded = OnboardingSavedData.load(data.save(new CompoundTag()));
        helper.assertTrue(loaded.isCompleted(completed) && !loaded.isCompleted(unseen), "Saved completion must be per UUID");
        helper.assertTrue(!OnboardingSavedData.load(new CompoundTag()).isCompleted(completed), "A fresh world must start uncompleted");
        helper.succeed();
    }
}
