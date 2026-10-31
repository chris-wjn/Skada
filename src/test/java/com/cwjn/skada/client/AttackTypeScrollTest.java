package com.cwjn.skada.client;

import com.cwjn.skada.data.damage.AttackSelection;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.network.SkadaNetwork;
import com.cwjn.skada.network.client_to_server.C2SSelectAttackType;
import com.cwjn.skada.network.client_to_server.C2SSelectAttackTypeFromMenu;
import com.cwjn.skada.util.Keybinds;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.lwjgl.glfw.GLFW;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Exercises actual scroll subscribers, target priority, recovery and cancellation without an OpenGL window. */
class AttackTypeScrollTest {
    private final ResourceLocation attackId = new ResourceLocation("skada:thrust");
    private Minecraft client;
    private ItemStack mainhand;
    private ItemStack offhand;
    private AttackSelection selection;
    private MockedStatic<Minecraft> minecraft;
    private MockedStatic<InputConstants> input;
    private KeyMapping previousModifier;
    private MockedStatic<AttackSelection> selections;
    private MockedStatic<SkadaNetwork> network;
    private MockedStatic<NetworkRegistry> channels;

    /** Initializes vanilla items and sounds used by the input handlers. */
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** Replaces external client services while retaining the production event handlers. */
    @BeforeEach
    void setup() {
        client = mock(Minecraft.class);
        client.player = mock(LocalPlayer.class);
        when(client.player.getInventory()).thenReturn(new Inventory(client.player));
        when(client.player.getAttackStrengthScale(0.5f)).thenReturn(1.0f);
        when(client.getSoundManager()).thenReturn(mock(SoundManager.class));
        mainhand = new ItemStack(Items.STICK);
        offhand = new ItemStack(Items.BLAZE_ROD);
        when(client.player.getMainHandItem()).thenReturn(mainhand);
        when(client.player.getOffhandItem()).thenReturn(offhand);
        when(client.player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(mainhand);
        when(client.player.getItemInHand(InteractionHand.OFF_HAND)).thenReturn(offhand);
        selection = mock(AttackSelection.class);
        when(selection.order()).thenReturn(List.of(mock(AttackType.class), mock(AttackType.class)));
        when(selection.cycle(any(ItemStack.class), anyInt())).thenReturn(attackId);
        previousModifier = Keybinds.attackScrollModifier;
        Keybinds.attackScrollModifier = mock(KeyMapping.class);
        when(Keybinds.attackScrollModifier.getKey()).thenReturn(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_ALT));
        when(Keybinds.attackScrollModifier.isConflictContextAndModifierActive()).thenReturn(true);
        when(client.getWindow()).thenReturn(mock(Window.class));
        minecraft = mockStatic(Minecraft.class);
        input = mockStatic(InputConstants.class);
        selections = mockStatic(AttackSelection.class);
        channels = mockStatic(NetworkRegistry.class);
        channels.when(() -> NetworkRegistry.newSimpleChannel(any(), any(), any(), any())).thenReturn(mock(SimpleChannel.class));
        network = mockStatic(SkadaNetwork.class);
        minecraft.when(Minecraft::getInstance).thenReturn(client);
        input.when(() -> InputConstants.isKeyDown(0L, GLFW.GLFW_KEY_LEFT_ALT)).thenReturn(true);
        selections.when(() -> AttackSelection.resolve(any(ItemStack.class))).thenReturn(selection);
    }

    /** Restores static services for the rest of the suite. */
    @AfterEach
    void teardown() {
        if (network != null) network.close();
        if (channels != null) channels.close();
        if (selections != null) selections.close();
        if (input != null) input.close();
        if (minecraft != null) minecraft.close();
        Keybinds.attackScrollModifier = previousModifier;
    }

    /** Holding two weapons must change only the mainhand and preserve hotbar context. */
    @Test
    void scrollDownSelectsNextMainhandAttack() {
        client.player.getInventory().selected = 4;
        assertTrue(scroll(-1).isCanceled());
        verify(selection).cycle(mainhand, 1);
        verify(selection, never()).cycle(eq(offhand), anyInt());
        network.verify(() -> SkadaNetwork.playerToServer(new C2SSelectAttackType(attackId, InteractionHand.MAIN_HAND, 4)));
    }

    /** An empty mainhand permits reverse selection of the offhand weapon. */
    @Test
    void scrollUpSelectsPreviousOffhandAttackWhenMainhandEmpty() {
        when(client.player.getMainHandItem()).thenReturn(ItemStack.EMPTY);
        assertTrue(scroll(1).isCanceled());
        verify(selection).cycle(offhand, -1);
        network.verify(() -> SkadaNetwork.playerToServer(new C2SSelectAttackType(attackId, InteractionHand.OFF_HAND, 0)));
    }

    /** An occupied mainhand with one attack must neither claim scrolling nor fall back to the offhand. */
    @Test
    void singleAttackMainhandLeavesHotbarScrollingAlone() {
        when(selection.order()).thenReturn(List.of(mock(AttackType.class)));
        assertFalse(scroll(-1).isCanceled());
        verify(selection, never()).cycle(any(), anyInt());
        selections.verify(() -> AttackSelection.resolve(offhand), never());
        network.verifyNoInteractions();
    }

    /** Recovery still consumes the wheel, but produces no selection or delayed request. */
    @Test
    void recoveryConsumesScrollWithoutSwitching() {
        when(client.player.getAttackStrengthScale(0.5f)).thenReturn(0.9f);
        assertTrue(scroll(-1).isCanceled());
        verify(selection, never()).cycle(any(), anyInt());
        network.verifyNoInteractions();
        when(client.player.getAttackStrengthScale(0.5f)).thenReturn(1.0f);
        assertTrue(scroll(1).isCanceled());
        verify(selection).cycle(mainhand, -1);
        network.verify(() -> SkadaNetwork.playerToServer(new C2SSelectAttackType(attackId, InteractionHand.MAIN_HAND, 0)), times(1));
    }

    /** Ordinary wheel input must remain entirely available to vanilla. */
    @Test
    void scrollingWithoutModifierDoesNothing() {
        input.when(() -> InputConstants.isKeyDown(0L, GLFW.GLFW_KEY_LEFT_ALT)).thenReturn(false);
        assertFalse(scroll(-1).isCanceled());
        selections.verifyNoInteractions();
        network.verifyNoInteractions();
    }

    /** Container input must query the rebound key even though vanilla leaves its mapping's held flag unset. */
    @Test
    void modifierUsesReboundPhysicalKey() {
        when(Keybinds.attackScrollModifier.getKey()).thenReturn(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_CONTROL));
        input.when(() -> InputConstants.isKeyDown(0L, GLFW.GLFW_KEY_LEFT_ALT)).thenReturn(false);
        input.when(() -> InputConstants.isKeyDown(0L, GLFW.GLFW_KEY_LEFT_CONTROL)).thenReturn(true);
        assertFalse(Keybinds.attackScrollModifier.isDown());
        assertTrue(scroll(-1).isCanceled());
        verify(selection).cycle(mainhand, 1);
    }

    /** A container screen must modify only the hovered item, never a held weapon. */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void inventoryScrollTargetsOnlyHoveredSlot() {
        AbstractContainerScreen screen = mock(AbstractContainerScreen.class);
        AbstractContainerMenu menu = mock(AbstractContainerMenu.class);
        Slot slot = mock(Slot.class);
        ItemStack hovered = new ItemStack(Items.BONE);
        slot.index = 6;
        when(screen.getSlotUnderMouse()).thenReturn(slot);
        when(screen.getMenu()).thenReturn(menu);
        when(slot.getItem()).thenReturn(hovered);
        client.screen = screen;
        ScreenEvent.MouseScrolled.Pre event = new ScreenEvent.MouseScrolled.Pre(screen, 10, 10, -1);
        ClientEvent.ForgeBusEvent.listenForAttackTypeScrollInGUI(event);
        assertTrue(event.isCanceled());
        verify(selection).cycle(hovered, 1);
        verify(selection, never()).cycle(eq(mainhand), anyInt());
        network.verify(() -> SkadaNetwork.playerToServer(new C2SSelectAttackTypeFromMenu(attackId, menu.containerId, 6)));
    }

    /** Hovering outside inventory slots must never fall back to a held weapon. */
    @Test
    void inventoryWithoutHoveredSlotLeavesScrollAlone() {
        AbstractContainerScreen<?> screen = mock(AbstractContainerScreen.class);
        client.screen = screen;
        ScreenEvent.MouseScrolled.Pre event = new ScreenEvent.MouseScrolled.Pre(screen, 10, 10, -1);
        ClientEvent.ForgeBusEvent.listenForAttackTypeScrollInGUI(event);
        assertFalse(event.isCanceled());
        selections.verifyNoInteractions();
        network.verifyNoInteractions();
    }

    /** Sends a real cancelable gameplay event to the registered production subscriber. */
    private InputEvent.MouseScrollingEvent scroll(double delta) {
        InputEvent.MouseScrollingEvent event = new InputEvent.MouseScrollingEvent(delta, false, false, false, 10, 10);
        ClientEvent.ForgeBusEvent.listenForAttackTypeScrollInGame(event);
        return event;
    }
}
