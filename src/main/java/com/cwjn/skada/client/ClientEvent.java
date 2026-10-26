package com.cwjn.skada.client;

import com.cwjn.skada.ClientConfig;
import com.cwjn.skada.client.gui.button.OpenStatScreenButton;
import com.cwjn.skada.client.gui.particles.NumberParticle;
import com.cwjn.skada.client.gui.screen.StatScreen;
import com.cwjn.skada.client.gui.tooltip.ArmourTooltipComponent;
import com.cwjn.skada.client.gui.tooltip.ClientArmourTooltipComponent;
import com.cwjn.skada.client.gui.tooltip.ClientWeaponTooltipComponent;
import com.cwjn.skada.client.gui.tooltip.WeaponTooltipComponent;
import com.cwjn.skada.client.hud.MobHealthBar;
import com.cwjn.skada.client.hud.ReticleCoordinate;
import com.cwjn.skada.client.hud.ReticleShape;
import com.cwjn.skada.client.hud.ReticleViewportScaler;
import com.cwjn.skada.client.render.BestiaryEntitySketchRenderer;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.AttackSelection;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.network.SkadaNetwork;
import com.cwjn.skada.network.client_to_server.C2SSelectAttackType;
import com.cwjn.skada.network.client_to_server.C2SSelectAttackTypeFromMenu;
import com.cwjn.skada.util.Keybinds;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilData;
import com.cwjn.skada.util.UtilText;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.*;

import static com.cwjn.skada.data.SkadaData.*;

@OnlyIn(Dist.CLIENT)
public class ClientEvent {

  private static final HashMap<Mob, Integer> displayHealthbarTicksForMob = new HashMap<>();
  private static final float CUSTOM_RETICLE_OUTLINE_THICKNESS_SCALE = 2.0F / 3.0F;
  private static final int VANILLA_CROSSHAIR_SIZE = 15;
  private static final int VANILLA_DEBUG_CROSSHAIR_LINE_LENGTH = 10;
  private static final float CROSSHAIR_AXIS_GAP = 2F;
  private static final int ATTACK_TYPE_ICON_TEXTURE_SIZE = 16;
  private static final int ATTACK_TYPE_ICON_OFFSET_X = 2;
  private static final int ATTACK_TYPE_ICON_OFFSET_Y = 2;
  private static final float ATTACK_TYPE_ICON_SCALE = 0.25F;
  private static final ResourceLocation SLASH_ATTACK_TYPE_ICON = Util.rl("textures/gui/player_page/slash_icon.png");
  private static final ResourceLocation THRUST_ATTACK_TYPE_ICON = Util.rl("textures/gui/player_page/thrust_icon.png");
  private static final ResourceLocation STRIKE_ATTACK_TYPE_ICON = Util.rl("textures/gui/player_page/strike_icon.png");

  /**
   * Resolves the currently active reticle shapes for the supplied player.
   *
   * @param player the local player whose active attack type is being rendered
   * @return the active custom reticle shapes, or an empty list when no reticle is
   *         available
   */
  public static List<ReticleShape> getActiveReticleShapes(Player player) {
    AttackSelection selected = AttackSelection.melee(player.getMainHandItem());
    AttackType attackType = selected.type();
    if (attackType == null) {
      return Collections.emptyList();
    }

    AttackTypeInfo attackTypeInfo = selected.stats();
    ReticleShape defaultShape = RETICLES.get(attackType.name() + "_default");
    if (attackTypeInfo != null && attackTypeInfo.hasReticleShapes()) {
      return attackTypeInfo.getReticleShapes();
    }
    return defaultShape == null ? Collections.emptyList() : Collections.singletonList(defaultShape);
  }

  /**
   * Calculates the centered sprite size to use for the vanilla crosshair while
   * custom reticles
   * are enabled.
   *
   * The visible plus arms are sized against the nearest horizontal or vertical
   * outline
   * intersection so slash-like reticles do not graze the crosshair at the center.
   *
   * @param minecraft the active client instance
   * @return the odd centered sprite size to render, clamped to the vanilla 15
   *         pixel footprint
   */
  public static int getActiveCrosshairSpriteSize(Minecraft minecraft) {
    if (!ClientConfig.ENABLE_CUSTOM_RETICLES.get() || minecraft == null || minecraft.player == null) {
      return VANILLA_CROSSHAIR_SIZE;
    }

    List<ReticleShape> reticleShapes = getActiveReticleShapes(minecraft.player);
    if (reticleShapes.isEmpty()) {
      return VANILLA_CROSSHAIR_SIZE;
    }

    Window window = minecraft.getWindow();
    float nearestExtent = getNearestReticleAxisExtent(
        reticleShapes,
        ReticleViewportScaler.getViewportScale(window.getScreenWidth(), window.getScreenHeight()),
        (float) window.getGuiScale());
    if (!Float.isFinite(nearestExtent)) {
      nearestExtent = getNearestReticleOutlineDistance(
          reticleShapes,
          ReticleViewportScaler.getViewportScale(window.getScreenWidth(), window.getScreenHeight()),
          (float) window.getGuiScale());
    }
    if (!Float.isFinite(nearestExtent)) {
      return VANILLA_CROSSHAIR_SIZE;
    }

    int targetHalfExtent = Mth.clamp(Mth.floor(nearestExtent - CROSSHAIR_AXIS_GAP), 0, VANILLA_CROSSHAIR_SIZE / 2);
    return targetHalfExtent * 2 + 1;
  }

  /**
   * Calculates the debug crosshair line length that corresponds to the active
   * sprite crop.
   *
   * @param minecraft the active client instance
   * @return the debug crosshair line length to pass to
   *         {@link RenderSystem#renderCrosshair(int)}
   */
  public static int getActiveDebugCrosshairLineLength(Minecraft minecraft) {
    int crosshairSize = getActiveCrosshairSpriteSize(minecraft);
    return Mth.clamp(Math.round((float) VANILLA_DEBUG_CROSSHAIR_LINE_LENGTH * crosshairSize / VANILLA_CROSSHAIR_SIZE),
        1, VANILLA_DEBUG_CROSSHAIR_LINE_LENGTH);
  }

  private static float getNearestReticleOutlineDistance(List<ReticleShape> reticleShapes, float viewportScale,
      float guiScale) {
    float nearestExtent = Float.POSITIVE_INFINITY;
    for (ReticleShape reticleShape : reticleShapes) {
      nearestExtent = Math.min(
          nearestExtent,
          ReticleViewportScaler.scaleToGui(reticleShape.getNearestOutlineDistance(), viewportScale, guiScale));
    }
    return nearestExtent;
  }

  private static float getNearestReticleAxisExtent(List<ReticleShape> reticleShapes, float viewportScale,
      float guiScale) {
    float nearestExtent = Float.POSITIVE_INFINITY;
    for (ReticleShape reticleShape : reticleShapes) {
      nearestExtent = Math.min(
          nearestExtent,
          ReticleViewportScaler.scaleToGui(reticleShape.getNearestAxisExtent(), viewportScale, guiScale));
    }
    return nearestExtent;
  }

  @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
  public static class ModBusEvent {

    /**
     * Registers custom client shaders used by Skada GUI previews.
     *
     * @param event the shader registration event fired after vanilla shader loading
     * @throws IOException if the shader program resources cannot be loaded
     */
    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
      BestiaryEntitySketchRenderer.registerShaders(event);
    }

    @SubscribeEvent
    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent e) {
      e.register(WeaponTooltipComponent.class, t -> new ClientWeaponTooltipComponent(t.item));
      e.register(ArmourTooltipComponent.class, t -> new ClientArmourTooltipComponent(t.item));
    }

    @SuppressWarnings("null")
    @SubscribeEvent
    public static void registerParticleFactories(final RegisterParticleProvidersEvent e) {
      e.registerSpriteSet(Particles.NUMBER_PARTICLE.get(), NumberParticle.Provider::new);
    }

  }

  @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
  public static class ForgeBusEvent {

    @SuppressWarnings("null")
    @SubscribeEvent
    public static void onTooltipRender(RenderTooltipEvent.GatherComponents e) {
      // Only add to items that we consider weapons or armour
      if (e.getItemStack().hasTag() && e.getItemStack().getTag().contains(WEAPON_INFO_TAG_KEY)) {
        e.getTooltipElements().add(Either.right(new WeaponTooltipComponent(e.getItemStack())));
      } else if (e.getItemStack().hasTag() && e.getItemStack().getTag().contains(ARMOUR_INFO_TAG_KEY)) {
        e.getTooltipElements().add(Either.right(new ArmourTooltipComponent(e.getItemStack())));
      }
    }

    @SuppressWarnings("null")
    @SubscribeEvent
    public static void onTooltipEvent(ItemTooltipEvent e) {
      // if the item has modifiers but isn't a weapon or armour, use the Vanilla
      // tooltip method in Util to add default modifier tooltip lines
      if (e.getItemStack().hasTag()
          && shouldShowInTooltip(getHideFlags(e.getItemStack()), ItemStack.TooltipPart.MODIFIERS)
          && !e.getItemStack().getTag().contains(WEAPON_INFO_TAG_KEY)
          && !e.getItemStack().getTag().contains(ARMOUR_INFO_TAG_KEY)) {
        e.getToolTip().addAll(UtilText.getVanillaTooltip(Minecraft.getInstance().player, e.getItemStack()));
      }
    }

    @SuppressWarnings("null")
    private static int getHideFlags(ItemStack i) {
      return i.hasTag() && i.getTag().contains("HideFlags", 99) ? i.getTag().getInt("HideFlags")
          : i.getItem().getDefaultTooltipHideFlags(i);
    }

    private static boolean shouldShowInTooltip(int pHideFlags, ItemStack.TooltipPart pPart) {
      return (pHideFlags & pPart.getMask()) == 0;
    }

    /** Resolves and predicts one cycle; the same ordered ID is sent to the server. */
    private static ResourceLocation cycleAttackType(ItemStack stack) {
      AttackSelection selected = AttackSelection.resolve(stack);
      if (selected.order().size() < 2 || Minecraft.getInstance().screen instanceof ChatScreen) return null;
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F, 1.0F));
      return selected.cycle(stack);
    }

    @SubscribeEvent
    public static void listenForAttackTypeCycleInGame(ClientTickEvent e) {
      if (e.phase != ClientTickEvent.Phase.END || Minecraft.getInstance().player == null
          || Minecraft.getInstance().player.getAttackStrengthScale(0.5f) <= 0.9) return;
      while (Keybinds.cycleAttackType.consumeClick()) {
        ResourceLocation selected = cycleAttackType(Minecraft.getInstance().player.getMainHandItem());
        if (selected != null) SkadaNetwork.playerToServer(new C2SSelectAttackType(selected));
      }
    }

    /** Keeps vanilla creative stack transport and normal-menu server requests aligned. */
    private static void cycleHoveredAttackType() {
      if (Minecraft.getInstance().player == null || Minecraft.getInstance().player.getAttackStrengthScale(0.5f) <= 0.9) return;
      if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getSlotUnderMouse() != null) {
        Slot hoveredSlot = screen.getSlotUnderMouse();
        ItemStack stack = hoveredSlot.getItem();
        ResourceLocation selected = cycleAttackType(stack);
        if (selected == null) return;
        if (screen instanceof CreativeModeInventoryScreen) {
          Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(stack.copy(), hoveredSlot.index);
        } else {
          SkadaNetwork.playerToServer(new C2SSelectAttackTypeFromMenu(selected, screen.getMenu().containerId, hoveredSlot.index));
        }
      }
    }

    @SubscribeEvent
    public static void listenForAttackTypeCycleKeybindInGUI(ScreenEvent.KeyPressed.Post e) {
      if (e.getKeyCode() == Keybinds.cycleAttackType.getKey().getValue()) cycleHoveredAttackType();
    }

    @SubscribeEvent
    public static void listenForAttackTypeCycleMousebindInGUI(ScreenEvent.MouseButtonPressed.Post e) {
      if (e.getButton() == Keybinds.cycleAttackType.getKey().getValue()) cycleHoveredAttackType();
    }

    @SubscribeEvent
    public static void listenForStatScreenOpen(ClientTickEvent e) {
      if (e.phase != ClientTickEvent.Phase.END)
        return;
      if (Minecraft.getInstance().player == null)
        return;
      while (Keybinds.statScreen.consumeClick()) {
        Player p = Minecraft.getInstance().player;
        AttackSelection selected = AttackSelection.melee(p.getMainHandItem());
        Minecraft.getInstance().setScreen(new StatScreen(selected.weapon(), selected.type(), selected.stats()));
      }
    }

    // TODO: actually use this once the stat screen is done!
    @SubscribeEvent
    public static void onInitGui(ScreenEvent.Init e) {
      Screen s = e.getScreen();
      if (s instanceof InventoryScreen || s instanceof CreativeModeInventoryScreen) {
        int x = (s.width - (s instanceof CreativeModeInventoryScreen ? 195 : 176)) / 2 - 28;
        int y = (s.height - (s instanceof CreativeModeInventoryScreen ? 136 : 166)) / 2;
        Player p = Minecraft.getInstance().player;
        e.addListener(new OpenStatScreenButton(x, y, Component.empty(), b -> {
          AttackSelection selected = AttackSelection.melee(p.getMainHandItem());
          Minecraft.getInstance().setScreen(new StatScreen(selected.weapon(), selected.type(), selected.stats()));
        }));
      }
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      ClientBestiaryCache.clear();
    }

    /**
     * Renders the active reticle shapes as layered screen-space geometry.
     *
     * The gameplay footprint still comes from the existing reticle polygons and
     * ray-trace logic.
     * This handler only improves how those same shapes read against the world.
     *
     * @param event the overlay event that provides the current GUI pose stack and
     *              window metrics
     */
    @SuppressWarnings("null")
    @SubscribeEvent
    public static void renderCustomReticles(RenderGuiOverlayEvent.Post event) {
      if (!ClientConfig.ENABLE_CUSTOM_RETICLES.get())
        return;
      if (!VanillaGuiOverlay.CROSSHAIR.id().equals(event.getOverlay().id()))
        return;
      Player player = Minecraft.getInstance().player;
      if (player == null)
        return;
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.getBuilder();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      PoseStack stack = event.getGuiGraphics().pose();
      stack.pushPose();
      float middleX = event.getWindow().getGuiScaledWidth() * 0.5F;
      float middleY = event.getWindow().getGuiScaledHeight() * 0.5F;
      stack.translate(middleX - 0.5f, middleY, 0);
      float guiScale = (float) event.getWindow().getGuiScale();
      stack.scale(1.0F / guiScale, 1.0F / guiScale, 1.0f);
      float reticleScale = ReticleViewportScaler.getViewportScale(event.getWindow().getScreenWidth(),
          event.getWindow().getScreenHeight());
      AttackType attackType = UtilData.getAttackType(player);
      List<ReticleShape> reticleShapes = getActiveReticleShapes(player);

      int hitResultIndex = 0;
      for (ReticleShape reticleShape : reticleShapes) {
        int sampleCount = reticleShape.getFilledShape().size();
        boolean hasMobWithinReticle = hasMobWithinReticle(hitResultIndex, sampleCount);
        renderReticleShape(tesselator, buffer, stack, reticleShape, reticleScale, guiScale, hasMobWithinReticle);
        if (DEBUG_ENABLED) {
          renderDebugReticleSamples(tesselator, buffer, stack, reticleShape, reticleScale);
        }
        hitResultIndex += sampleCount;
      }
      stack.popPose();
      renderAttackTypeIcon(event.getGuiGraphics(), attackType, event.getWindow().getGuiScaledWidth() / 2,
          event.getWindow().getGuiScaledHeight() / 2);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Renders a single reticle as layered fill and outline passes.
     *
     * @param tesselator          the shared tesselator used for GUI geometry
     *                            submission
     * @param buffer              the shared buffer builder associated with the
     *                            tesselator
     * @param stack               the pose stack already centered on the crosshair
     * @param reticleShape        the reticle polygon to render
     * @param reticleScale        the current viewport-derived screen scale for
     *                            reticle coordinates
     * @param guiScale            the active Minecraft GUI scale used by the vanilla
     *                            crosshair sprite
     * @param hasMobWithinReticle whether any sampled ray for this reticle currently
     *                            intersects a live mob
     */
    private static void renderReticleShape(Tesselator tesselator,
        BufferBuilder buffer,
        PoseStack stack,
        ReticleShape reticleShape,
        float reticleScale,
        float guiScale,
        boolean hasMobWithinReticle) {
      if (hasMobWithinReticle) {
        renderFilledShape(tesselator, buffer, stack, reticleShape, reticleScale, 0.96F, 0.18F, 0.18F, 0.25F);
      }

      RenderSystem.blendFuncSeparate(
          GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
          GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
          GlStateManager.SourceFactor.ONE,
          GlStateManager.DestFactor.ZERO);
      renderOutlinePass(tesselator,
          buffer,
          stack,
          reticleShape,
          reticleScale,
          Math.max(CUSTOM_RETICLE_OUTLINE_THICKNESS_SCALE,
              Math.max(1.0F, guiScale) * CUSTOM_RETICLE_OUTLINE_THICKNESS_SCALE),
          1.0F,
          1.0F,
          1.0F,
          1.0F);
      RenderSystem.defaultBlendFunc();
    }

    /**
     * Draws a compact badge for the currently selected attack type beside the
     * crosshair.
     *
     * @param guiGraphics the GUI graphics instance used for HUD rendering
     * @param attackType  the currently active attack type
     * @param centerX     the GUI-space x coordinate of the crosshair center
     * @param centerY     the GUI-space y coordinate of the crosshair center
     */
    private static void renderAttackTypeIcon(GuiGraphics guiGraphics,
        AttackType attackType,
        int centerX,
        int centerY) {
      ResourceLocation attackTypeIcon = getAttackTypeIcon(attackType);
      if (attackTypeIcon == null) {
        return;
      }

      PoseStack stack = guiGraphics.pose();
      stack.pushPose();
      stack.translate(centerX + ATTACK_TYPE_ICON_OFFSET_X, centerY + ATTACK_TYPE_ICON_OFFSET_Y, 0.0F);
      stack.scale(ATTACK_TYPE_ICON_SCALE, ATTACK_TYPE_ICON_SCALE, 1.0F);
      guiGraphics.blit(
          attackTypeIcon,
          0,
          0,
          0,
          0,
          ATTACK_TYPE_ICON_TEXTURE_SIZE,
          ATTACK_TYPE_ICON_TEXTURE_SIZE,
          ATTACK_TYPE_ICON_TEXTURE_SIZE,
          ATTACK_TYPE_ICON_TEXTURE_SIZE);
      stack.popPose();
    }

    /**
     * Resolves the HUD icon texture for the supplied attack type.
     *
     * @param attackType the currently active attack type
     * @return the icon texture to render, or {@code null} when no badge should be
     *         drawn
     */
    private static ResourceLocation getAttackTypeIcon(AttackType attackType) {
      if (attackType == null) {
        return null;
      }

      return switch (attackType.name()) {
        case "slash" -> SLASH_ATTACK_TYPE_ICON;
        case "thrust" -> THRUST_ATTACK_TYPE_ICON;
        case "strike" -> STRIKE_ATTACK_TYPE_ICON;
        default -> null;
      };
    }

    /**
     * Checks whether any sampled hit result for the current reticle slice resolved
     * to a live mob.
     *
     * @param startIndex  the first hit-result slot that belongs to the reticle
     *                    shape
     * @param sampleCount the number of sampled rays used by the reticle shape
     * @return {@code true} when a live mob is currently within the sampled reticle
     *         area
     */
    private static boolean hasMobWithinReticle(int startIndex, int sampleCount) {
      if (sampleCount <= 0 || ClientHandler.hitResults == null || startIndex >= ClientHandler.hitResults.length) {
        return false;
      }

      int endIndex = Math.min(ClientHandler.hitResults.length, startIndex + sampleCount);
      for (int index = startIndex; index < endIndex; index++) {
        HitResult hitResult = ClientHandler.hitResults[index];
        if (hitResult instanceof EntityHitResult entityHitResult
            && entityHitResult.getEntity() instanceof Mob mob
            && mob.isAlive()) {
          return true;
        }
      }
      return false;
    }

    /**
     * Draws a continuous alpha-blended interior for the current reticle polygon.
     *
     * The current authored reticles are convex polygons, so a simple triangle fan
     * keeps the
     * fill aligned to the actual gameplay footprint without the checkerboard
     * artifacts that
     * came from rendering sparse fill samples as quads.
     *
     * @param tesselator   the shared tesselator used for GUI geometry submission
     * @param buffer       the shared buffer builder associated with the tesselator
     * @param stack        the pose stack already centered on the crosshair
     * @param reticleShape the reticle polygon whose interior should be rendered
     * @param reticleScale the current viewport-derived screen scale for reticle
     *                     coordinates
     * @param red          the red colour component
     * @param green        the green colour component
     * @param blue         the blue colour component
     * @param alpha        the alpha colour component
     */
    private static void renderFilledShape(Tesselator tesselator,
        BufferBuilder buffer,
        PoseStack stack,
        ReticleShape reticleShape,
        float reticleScale,
        float red,
        float green,
        float blue,
        float alpha) {
      List<ReticleCoordinate> outline = reticleShape.getOutline();
      int pointCount = outline.size() - 1;
      if (pointCount < 3)
        return;

      float centroidX = 0.0F;
      float centroidY = 0.0F;
      for (int index = 0; index < pointCount; index++) {
        ReticleCoordinate point = outline.get(index);
        centroidX += ReticleViewportScaler.scaleToScreen(point.x(), reticleScale);
        centroidY += ReticleViewportScaler.scaleToScreen(point.y(), reticleScale);
      }
      centroidX /= pointCount;
      centroidY /= pointCount;

      buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
      for (int index = 0; index < pointCount; index++) {
        ReticleCoordinate start = outline.get(index);
        ReticleCoordinate end = outline.get(index + 1);
        addTriangle(buffer, stack,
            centroidX,
            centroidY,
            ReticleViewportScaler.scaleToScreen(start.x(), reticleScale),
            ReticleViewportScaler.scaleToScreen(start.y(), reticleScale),
            ReticleViewportScaler.scaleToScreen(end.x(), reticleScale),
            ReticleViewportScaler.scaleToScreen(end.y(), reticleScale),
            red,
            green,
            blue,
            alpha);
      }
      tesselator.end();
    }

    /**
     * Draws the reticle edge as thickened quads so the result is consistent across
     * drivers.
     *
     * @param tesselator   the shared tesselator used for GUI geometry submission
     * @param buffer       the shared buffer builder associated with the tesselator
     * @param stack        the pose stack already centered on the crosshair
     * @param reticleShape the reticle polygon whose outline should be rendered
     * @param reticleScale the current viewport-derived screen scale for reticle
     *                     coordinates
     * @param thickness    the total screen-space width of the outline pass
     * @param red          the red colour component
     * @param green        the green colour component
     * @param blue         the blue colour component
     * @param alpha        the alpha colour component
     */
    private static void renderOutlinePass(Tesselator tesselator,
        BufferBuilder buffer,
        PoseStack stack,
        ReticleShape reticleShape,
        float reticleScale,
        float thickness,
        float red,
        float green,
        float blue,
        float alpha) {
      List<ReticleCoordinate> outline = reticleShape.getOutline();
      buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      for (int index = 0; index < outline.size() - 1; index++) {
        ReticleCoordinate start = outline.get(index);
        ReticleCoordinate end = outline.get(index + 1);
        float startX = ReticleViewportScaler.scaleToScreen(start.x(), reticleScale);
        float startY = ReticleViewportScaler.scaleToScreen(start.y(), reticleScale);
        float endX = ReticleViewportScaler.scaleToScreen(end.x(), reticleScale);
        float endY = ReticleViewportScaler.scaleToScreen(end.y(), reticleScale);
        addSegmentQuad(buffer, stack, startX, startY, endX, endY, thickness, red, green, blue, alpha);
      }
      tesselator.end();

      renderOutlineJoins(tesselator, buffer, stack, outline, reticleScale, thickness, red, green, blue, alpha);
    }

    /**
     * Fills the space around each reticle vertex so adjacent outline segments read
     * as one
     * continuous stroke instead of disconnected quads.
     *
     * @param tesselator   the shared tesselator used for GUI geometry submission
     * @param buffer       the shared buffer builder associated with the tesselator
     * @param stack        the pose stack already centered on the crosshair
     * @param outline      the closed reticle outline list
     * @param reticleScale the current viewport-derived screen scale for reticle
     *                     coordinates
     * @param thickness    the total screen-space width of the outline pass
     * @param red          the red colour component
     * @param green        the green colour component
     * @param blue         the blue colour component
     * @param alpha        the alpha colour component
     */
    private static void renderOutlineJoins(Tesselator tesselator,
        BufferBuilder buffer,
        PoseStack stack,
        List<ReticleCoordinate> outline,
        float reticleScale,
        float thickness,
        float red,
        float green,
        float blue,
        float alpha) {
      if (outline.size() <= 1)
        return;

      float radius = thickness * 0.5F;
      buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
      for (int index = 0; index < outline.size() - 1; index++) {
        ReticleCoordinate point = outline.get(index);
        addRoundJoin(buffer,
            stack,
            ReticleViewportScaler.scaleToScreen(point.x(), reticleScale),
            ReticleViewportScaler.scaleToScreen(point.y(), reticleScale),
            radius,
            red,
            green,
            blue,
            alpha);
      }
      tesselator.end();
    }

    /**
     * Renders the reticle sample points in red when debug rendering is enabled.
     *
     * @param tesselator   the shared tesselator used for GUI geometry submission
     * @param buffer       the shared buffer builder associated with the tesselator
     * @param stack        the pose stack already centered on the crosshair
     * @param reticleShape the reticle polygon whose filled sample points should be
     *                     visualized
     * @param reticleScale the current viewport-derived screen scale for reticle
     *                     coordinates
     */
    private static void renderDebugReticleSamples(Tesselator tesselator,
        BufferBuilder buffer,
        PoseStack stack,
        ReticleShape reticleShape,
        float reticleScale) {
      buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      for (Map.Entry<Float, Collection<Float>> samplesByX : reticleShape.getFilledShape().asMap().entrySet()) {
        float scaledX = ReticleViewportScaler.scaleToScreen(samplesByX.getKey(), reticleScale);
        for (Float sampleY : samplesByX.getValue()) {
          float scaledY = ReticleViewportScaler.scaleToScreen(sampleY, reticleScale);
          addAxisAlignedQuad(buffer, stack,
              scaledX - 0.5F,
              scaledY - 0.5F,
              scaledX + 0.5F,
              scaledY + 0.5F,
              1.0F,
              0.0F,
              0.0F,
              1.0F);
        }
      }
      tesselator.end();
    }

    /**
     * Adds an oriented quad for a reticle outline segment.
     *
     * @param buffer    the shared buffer builder receiving geometry
     * @param stack     the pose stack already centered on the crosshair
     * @param startX    the screen-space start x coordinate
     * @param startY    the screen-space start y coordinate
     * @param endX      the screen-space end x coordinate
     * @param endY      the screen-space end y coordinate
     * @param thickness the total screen-space width of the segment quad
     * @param red       the red colour component
     * @param green     the green colour component
     * @param blue      the blue colour component
     * @param alpha     the alpha colour component
     */
    private static void addSegmentQuad(BufferBuilder buffer,
        PoseStack stack,
        float startX,
        float startY,
        float endX,
        float endY,
        float thickness,
        float red,
        float green,
        float blue,
        float alpha) {
      float deltaX = endX - startX;
      float deltaY = endY - startY;
      float segmentLength = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
      if (segmentLength <= 0.001F)
        return;

      float halfThickness = thickness * 0.5F;
      float directionX = deltaX / segmentLength;
      float directionY = deltaY / segmentLength;
      float extensionX = directionX * halfThickness;
      float extensionY = directionY * halfThickness;
      float expandedStartX = startX - extensionX;
      float expandedStartY = startY - extensionY;
      float expandedEndX = endX + extensionX;
      float expandedEndY = endY + extensionY;
      float offsetX = -directionY * halfThickness;
      float offsetY = directionX * halfThickness;
      buffer.vertex(stack.last().pose(), expandedStartX - offsetX, expandedStartY - offsetY, 0.0F)
          .color(red, green, blue, alpha).endVertex();
      buffer.vertex(stack.last().pose(), expandedEndX - offsetX, expandedEndY - offsetY, 0.0F)
          .color(red, green, blue, alpha).endVertex();
      buffer.vertex(stack.last().pose(), expandedEndX + offsetX, expandedEndY + offsetY, 0.0F)
          .color(red, green, blue, alpha).endVertex();
      buffer.vertex(stack.last().pose(), expandedStartX + offsetX, expandedStartY + offsetY, 0.0F)
          .color(red, green, blue, alpha).endVertex();
    }

    /**
     * Adds a small round join at a reticle vertex to smooth neighbouring outline
     * segments.
     *
     * @param buffer  the shared buffer builder receiving geometry
     * @param stack   the pose stack already centered on the crosshair
     * @param centerX the vertex x coordinate
     * @param centerY the vertex y coordinate
     * @param radius  the radius of the join disk
     * @param red     the red colour component
     * @param green   the green colour component
     * @param blue    the blue colour component
     * @param alpha   the alpha colour component
     */
    private static void addRoundJoin(BufferBuilder buffer,
        PoseStack stack,
        float centerX,
        float centerY,
        float radius,
        float red,
        float green,
        float blue,
        float alpha) {
      if (radius <= 0.0F)
        return;

      int segments = 10;
      float angleStep = (float) (Math.PI * 2.0D / segments);
      for (int index = 0; index < segments; index++) {
        float startAngle = index * angleStep;
        float endAngle = (index + 1) * angleStep;
        addTriangle(buffer,
            stack,
            centerX,
            centerY,
            centerX + Mth.cos(startAngle) * radius,
            centerY + Mth.sin(startAngle) * radius,
            centerX + Mth.cos(endAngle) * radius,
            centerY + Mth.sin(endAngle) * radius,
            red,
            green,
            blue,
            alpha);
      }
    }

    /**
     * Adds a triangle to the current reticle render pass.
     *
     * @param buffer the shared buffer builder receiving geometry
     * @param stack  the pose stack already centered on the crosshair
     * @param x1     the first vertex x coordinate
     * @param y1     the first vertex y coordinate
     * @param x2     the second vertex x coordinate
     * @param y2     the second vertex y coordinate
     * @param x3     the third vertex x coordinate
     * @param y3     the third vertex y coordinate
     * @param red    the red colour component
     * @param green  the green colour component
     * @param blue   the blue colour component
     * @param alpha  the alpha colour component
     */
    private static void addTriangle(BufferBuilder buffer,
        PoseStack stack,
        float x1,
        float y1,
        float x2,
        float y2,
        float x3,
        float y3,
        float red,
        float green,
        float blue,
        float alpha) {
      buffer.vertex(stack.last().pose(), x1, y1, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(stack.last().pose(), x2, y2, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(stack.last().pose(), x3, y3, 0.0F).color(red, green, blue, alpha).endVertex();
    }

    /**
     * Adds an axis-aligned quad to the current reticle render pass.
     *
     * @param buffer the shared buffer builder receiving geometry
     * @param stack  the pose stack already centered on the crosshair
     * @param minX   the minimum screen-space x coordinate
     * @param minY   the minimum screen-space y coordinate
     * @param maxX   the maximum screen-space x coordinate
     * @param maxY   the maximum screen-space y coordinate
     * @param red    the red colour component
     * @param green  the green colour component
     * @param blue   the blue colour component
     * @param alpha  the alpha colour component
     */
    private static void addAxisAlignedQuad(BufferBuilder buffer,
        PoseStack stack,
        float minX,
        float minY,
        float maxX,
        float maxY,
        float red,
        float green,
        float blue,
        float alpha) {
      buffer.vertex(stack.last().pose(), minX, minY, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(stack.last().pose(), minX, maxY, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(stack.last().pose(), maxX, maxY, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(stack.last().pose(), maxX, minY, 0.0F).color(red, green, blue, alpha).endVertex();
    }

    @SubscribeEvent
    public static void prepareHealthbar(RenderNameTagEvent event) {
      if (!ClientConfig.ENABLE_MOB_HEALTHBARS.get())
        return;
      if (event.getEntity() instanceof Mob entity) {
        if (entity.isInvisible())
          return;
        if (entity.isVehicle())
          return;
        if (ClientConfig.BLACKLISTED_HEALTHBAR_ENTITIES.get().contains(Util.getEntityRegistryName(entity.getType())))
          return;
        if (ClientConfig.HEALTHBAR_ONLY_ON_DAMAGE.get()) {
          if (displayHealthbarTicksForMob.containsKey(entity)) {
            int unpackedLight = Math.max(LightTexture.sky(event.getPackedLight()) - ClientHandler.skyDarken,
                LightTexture.block(event.getPackedLight()));
            @SuppressWarnings("null")
            float distance = entity.distanceTo(Minecraft.getInstance().player) - unpackedLight + 15;
            float minAlpha = distance < 30 * 0.5 ? 1 : (1 - (distance / 30));
            float alpha = (float) Math.min(minAlpha, ((float) displayHealthbarTicksForMob.get(entity)
                / (ClientConfig.HEALTHBAR_ON_DAMAGE_DISPLAY_TIME.get() * 0.5)));
            MobHealthBar.prepare(entity, alpha);
          }
        } else {
          int unpackedLight = Math.max(LightTexture.sky(event.getPackedLight()) - ClientHandler.skyDarken,
              LightTexture.block(event.getPackedLight()));
          @SuppressWarnings("null")
          float distance = entity.distanceTo(Minecraft.getInstance().player) - unpackedLight + 15;
          float alpha = distance < 30 * 0.5 ? 1 : (1 - (distance / 30));
          MobHealthBar.prepare(entity, alpha);
        }
      }
    }

    @SubscribeEvent
    public static void renderHealthbar(RenderLevelStageEvent event) {
      if (!ClientConfig.ENABLE_MOB_HEALTHBARS.get())
        return;
      if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
        Camera c = Minecraft.getInstance().gameRenderer.getMainCamera();
        MobHealthBar.renderBars(event.getPartialTick(), event.getPoseStack(), c);
      }
    }

  }

}
