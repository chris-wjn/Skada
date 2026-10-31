package com.cwjn.skada.network.client_to_server;

import com.cwjn.skada.data.damage.AttackSelection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

/** Checks server authority for the new offhand and hotbar context, including rejected client prediction. */
class AttackSelectionPacketTest {
    private static final ResourceLocation ATTACK = new ResourceLocation("skada:thrust");

    /** Initializes vanilla registries before constructing test stacks and mocking players. */
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** The server must accept an offhand request when the mainhand really is empty. */
    @Test
    void emptyMainhandAllowsOffhandSelection() throws Exception {
        checkRequest(ItemStack.EMPTY, InteractionHand.OFF_HAND, 4, 1.0f, true);
    }

    /** Two equipped weapons must still select the mainhand. */
    @Test
    void equippedMainhandAllowsMainhandSelection() throws Exception {
        checkRequest(new ItemStack(Items.STICK), InteractionHand.MAIN_HAND, 4, 1.0f, true);
    }

    /** A client cannot bypass mainhand priority by explicitly naming the offhand. */
    @Test
    void occupiedMainhandRejectsOffhandSelection() throws Exception {
        checkRequest(new ItemStack(Items.STICK), InteractionHand.OFF_HAND, 4, 1.0f, false);
    }

    /** Late requests must not change a different weapon after a hotbar switch. */
    @Test
    void changedHotbarRejectsStaleRequest() throws Exception {
        checkRequest(new ItemStack(Items.STICK), InteractionHand.MAIN_HAND, 3, 1.0f, false);
    }

    /** A modified client must not bypass the agreed charge threshold. */
    @Test
    void recoveryRejectsSelection() throws Exception {
        checkRequest(new ItemStack(Items.STICK), InteractionHand.MAIN_HAND, 4, 0.9f, false);
    }

    /** Runs the actual packet handler's queued work against a server-owned hand and menu. */
    private void checkRequest(ItemStack mainhand, InteractionHand hand, int requestedSlot, float charge, boolean accepted) throws Exception {
        ServerPlayer player = mock(ServerPlayer.class);
        when(player.getServer()).thenReturn(mock(MinecraftServer.class));
        Inventory inventory = new Inventory(player);
        inventory.selected = 4;
        when(player.getInventory()).thenReturn(inventory);
        when(player.getMainHandItem()).thenReturn(mainhand);
        ItemStack target = hand == InteractionHand.MAIN_HAND ? mainhand : new ItemStack(Items.BLAZE_ROD);
        when(player.getItemInHand(hand)).thenReturn(target);
        when(player.getAttackStrengthScale(0.5f)).thenReturn(charge);
        InventoryMenu menu = mock(InventoryMenu.class);
        var inventoryMenuField = Player.class.getDeclaredField("inventoryMenu");
        inventoryMenuField.setAccessible(true);
        inventoryMenuField.set(player, menu);
        player.containerMenu = menu;
        NetworkEvent.Context context = mock(NetworkEvent.Context.class);
        when(context.getSender()).thenReturn(player);
        when(context.enqueueWork(any())).thenAnswer(call -> {
            ((Runnable) call.getArgument(0)).run();
            return CompletableFuture.completedFuture(null);
        });
        try (var selection = mockStatic(AttackSelection.class)) {
            selection.when(() -> AttackSelection.select(target, ATTACK)).thenReturn(true);
            C2SSelectAttackType.handle(new C2SSelectAttackType(ATTACK, hand, requestedSlot), () -> context);
            if (accepted) {
                selection.verify(() -> AttackSelection.select(target, ATTACK));
                verify(menu).broadcastChanges();
                verify(menu, never()).broadcastFullState();
            } else {
                selection.verifyNoInteractions();
                verify(menu).broadcastFullState();
            }
            verify(context).setPacketHandled(true);
        }
    }
}
