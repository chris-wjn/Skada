package com.cwjn.skada.client.gui.screen.pages;

import static com.cwjn.skada.util.UtilColour.LIGHT;
import static com.cwjn.skada.util.UtilColour.LIGHT_GRAY;
import static com.cwjn.skada.util.UtilColour.UI_BACKGROUND_COLOUR;
import static com.cwjn.skada.util.UtilColour.UI_BORDER_COLOUR;
import static com.cwjn.skada.util.UtilColour.UI_TEXT_COLOUR;
import static com.cwjn.skada.util.UtilColour.WHITE;

import java.util.List;
import java.util.Locale;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.cwjn.skada.SkadaRegistry;
import com.cwjn.skada.client.ClientHandler;
import com.cwjn.skada.client.gui.screen.JournalPage;
import com.cwjn.skada.client.gui.screen.StatScreen;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilCombat;
import com.cwjn.skada.util.UtilDefenseDisplay;
import com.cwjn.skada.util.UtilText;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Renders the player journal page, including the model preview and the compact
 * element/core stat columns shown under the preview frame.
 */
public class PlayerPage extends JournalPage {

  private int hoverX, hoverY;

  private static final ResourceLocation PLAYER_BOX_TEXTURE = Util.rl("textures/gui/player_page/player_box.png");
  private static final ResourceLocation DAMAGE_ICON = Util.rl("textures/gui/book_page_icons/damage_page_icon.png");
  private static final ResourceLocation HEALTH_ICON = Util.rl("textures/gui/player_page/health_icon.png");
  private static final ResourceLocation ARMOUR_ICON = Util.rl("textures/gui/book_page_icons/armour_icon.png");
  private static final ResourceLocation RESISTANCE_ICON = Util.rl("textures/gui/book_page_icons/resistance_icon.png");
  private static final ResourceLocation TOUGHNESS_ICON = Util.rl("textures/gui/player_page/toughness_icon.png");
  private static final ResourceLocation MOVESPEED_ICON = Util.rl("textures/gui/player_page/movespeed_icon.png");
  private static final ResourceLocation SLASH_ICON = Util.rl("textures/gui/player_page/slash_icon.png");
  private static final ResourceLocation THRUST_ICON = Util.rl("textures/gui/player_page/thrust_icon.png");
  private static final ResourceLocation STRIKE_ICON = Util.rl("textures/gui/player_page/strike_icon.png");

  private static final int PLAYER_BOX_X_OFFSET = 55;
  private static final int PLAYER_BOX_Y_OFFSET = 33;
  private static final int PLAYER_BOX_WIDTH = 222;
  private static final int PLAYER_BOX_HEIGHT = 137;
  private static final int PLAYER_BOX_BORDER_THICKNESS = 10;
  private static final int TITLE_Y_OFFSET = 172;
  private static final int ELEMENT_TITLE_X_OFFSET = 61;
  private static final int CORE_TITLE_X_OFFSET = 166;
  private static final int CORE_COLUMN_X_OFFSET = 170;
  private static final int STAT_ROW_SPACING = 20;
  private static final int STATLINE_WIDTH = 94;
  private static final int STATLINE_UNDERLINE_Y_OFFSET = 11;
  private static final int STAT_TEXT_Y_OFFSET = 2;
  private static final int STAT_NUMBER_Y_OFFSET = 4;
  private static final int STAT_SECONDARY_TEXT_Y_OFFSET = 13;
  private static final int STAT_ICON_Y_OFFSET = 1;
  private static final float TITLE_SCALE = 0.8F;
  private static final float ELEMENT_ICON_SCALE = 0.6F;
  private static final float CORE_ICON_SCALE = 0.6F;
  private static final float INLINE_VALUE_ICON_SCALE = 0.4F;
  private static final float STAT_SECONDARY_TEXT_SCALE = 0.45F;
  private static final int OPAQUE_BORDER_COLOUR = UI_BORDER_COLOUR | 0xFF000000;
  private static final int TRANSPARENT_BORDER_COLOUR = UI_BORDER_COLOUR;

  public PlayerPage(ResourceLocation icon, ResourceLocation pageResource, StatScreen screen) {
    super(icon, pageResource, screen);
  }

  @Override
  public void init() {

  }

  @Override
  public void renderExtraDrawables(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    super.renderExtraDrawables(pGuiGraphics, pMouseX, pMouseY);
    drawPlayerBox(pGuiGraphics, pMouseX, pMouseY);
    drawPlayerStats(pGuiGraphics, pMouseX, pMouseY);
  }

  @Override
  public void clearButtons() {

  }

  @Override
  public void isRender(boolean render) {
    super.isRender(render);
  }

  private void drawPlayerBox(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    int bookX = screen.getBookLocalX();
    int bookY = screen.getBookLocalY();
    int playerX = bookX + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH/2; //add offset to get to center of box
    int playerY = bookY + PLAYER_BOX_Y_OFFSET + PLAYER_BOX_HEIGHT +110; //add offset to get to center of box

    //render box
    pGuiGraphics.pose().pushPose();
    pGuiGraphics.pose().translate(0, 0, -300); // Ensure the player box is rendered above the player
    pGuiGraphics.pose().popPose();
    pGuiGraphics.blit(PLAYER_BOX_TEXTURE, bookX + PLAYER_BOX_X_OFFSET, bookY + PLAYER_BOX_Y_OFFSET, 0, 0, 
      PLAYER_BOX_WIDTH, PLAYER_BOX_HEIGHT, PLAYER_BOX_WIDTH, PLAYER_BOX_HEIGHT);

    screen.enableScissor(
      bookX + PLAYER_BOX_X_OFFSET + PLAYER_BOX_BORDER_THICKNESS,
      bookY + PLAYER_BOX_Y_OFFSET + PLAYER_BOX_BORDER_THICKNESS,
      PLAYER_BOX_WIDTH - 2 * PLAYER_BOX_BORDER_THICKNESS,
      PLAYER_BOX_HEIGHT - 2 * PLAYER_BOX_BORDER_THICKNESS);
    pGuiGraphics.pose().pushPose();
    pGuiGraphics.pose().translate(0, 0, 0);
    InventoryScreen.renderEntityInInventoryFollowsMouse(pGuiGraphics, playerX, playerY, 120,
            playerX - pMouseX, playerY - pMouseY - 195, //50 is the height of the player model, so that the cursor is centered on the player's eyes
            Minecraft.getInstance().player);
    pGuiGraphics.pose().popPose();
    screen.disableScissor();
  }

  /**
   * Draws the two stat sections that sit beneath the preview frame.
   *
   * @param pGuiGraphics the graphics context used for rendering
   * @param pMouseX pointer x in book-local coordinates
   * @param pMouseY pointer y in book-local coordinates
   */
  private void drawPlayerStats(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    hoverX = pMouseX;
    hoverY = pMouseY;
    Player currentPlayer = Minecraft.getInstance().player;
    if (currentPlayer == null) {
      return;
    }
    Font currentFont = Minecraft.getInstance().font;
    int bookX = screen.getBookLocalX();
    int bookY = screen.getBookLocalY();
    int titleUnderlineY = TITLE_Y_OFFSET + UtilText.compass9ScaledHeight(TITLE_SCALE) + 2;
    int elementColumnX = ELEMENT_TITLE_X_OFFSET - UtilText.scaledWidth(currentFont, Component.translatable("skada.journal.player.element_title"), TITLE_SCALE);
    int coreColumnX = CORE_COLUMN_X_OFFSET - UtilText.scaledWidth(currentFont, Component.translatable("skada.journal.player.core_title"), TITLE_SCALE);
    int statRowY = titleUnderlineY + 4;

    drawSectionTitle(pGuiGraphics, currentFont, Component.translatable("skada.journal.player.element_title"),
      bookX + ELEMENT_TITLE_X_OFFSET, bookY + TITLE_Y_OFFSET, titleUnderlineY);
    drawSectionTitle(pGuiGraphics, currentFont, Component.translatable("skada.journal.player.core_title"),
      bookX + CORE_TITLE_X_OFFSET, bookY + TITLE_Y_OFFSET, titleUnderlineY);

    int elementRow = 0;
    for (Element element : getDisplayedElements()) {
      double elementResistance = currentPlayer.getAttributeValue(element.resist());
      drawElementStatline(
        pGuiGraphics,
        elementColumnX,
        statRowY + elementRow * STAT_ROW_SPACING,
        element.icon(),
        Component.translatable("skada.journal.player.element." + element.name() + ".compact"),
        UtilCombat.getElementalDamageForElement(currentPlayer, element),
        elementResistance,
        element.colour());
      elementRow++;
    }

    int coreRow = 0;
    drawPlayerStatline(pGuiGraphics, coreColumnX, statRowY + coreRow++ * STAT_ROW_SPACING, HEALTH_ICON,
      Component.translatable("skada.journal.player.core.health.compact"), currentPlayer.getAttributeValue(Attributes.MAX_HEALTH));
    drawPlayerDefenceStatline(pGuiGraphics, coreColumnX, statRowY + coreRow++ * STAT_ROW_SPACING, ARMOUR_ICON,
      Component.translatable("skada.journal.player.core.armour.compact"), currentPlayer.getAttributeValue(Attributes.ARMOR),
      UtilDefenseDisplay.armourMitigationPercent(currentPlayer.getAttributeValue(Attributes.ARMOR)));
    drawPlayerStatline(pGuiGraphics, coreColumnX, statRowY + coreRow++ * STAT_ROW_SPACING, TOUGHNESS_ICON,
      Component.translatable("skada.journal.player.core.toughness.compact"), currentPlayer.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    drawPlayerStatline(pGuiGraphics, coreColumnX, statRowY + coreRow++ * STAT_ROW_SPACING, MOVESPEED_ICON,
      Component.translatable("skada.journal.player.core.move_speed.compact"),
      Util.getPlayerSpeedInBlocksPerSecond(currentPlayer.getAttributeValue(Attributes.MOVEMENT_SPEED)));
    if (Double.isFinite(ClientHandler.playerTotalWeightKg)) {
      drawPlayerWeightStatline(pGuiGraphics, coreColumnX, statRowY + coreRow++ * STAT_ROW_SPACING,
          ClientHandler.playerTotalWeightKg);
    }
    drawPlayerStatline(pGuiGraphics, coreColumnX, statRowY + coreRow++ * STAT_ROW_SPACING, RESISTANCE_ICON,
      Component.translatable("skada.journal.player.core.knockback_resistance.compact"),
      currentPlayer.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 100.0);
    drawPlayerDefenceStatline(pGuiGraphics, coreColumnX, statRowY + coreRow++ * STAT_ROW_SPACING, SLASH_ICON,
      Component.translatable("skada.journal.player.core.slash.compact"), currentPlayer.getAttributeValue(SkadaRegistry.SLASH.get().resistAttribute()),
      UtilDefenseDisplay.attackTypeMitigationPercent(currentPlayer.getAttributeValue(SkadaRegistry.SLASH.get().resistAttribute())));
    drawPlayerDefenceStatline(pGuiGraphics, coreColumnX, statRowY + coreRow++ * STAT_ROW_SPACING, THRUST_ICON,
      Component.translatable("skada.journal.player.core.thrust.compact"), currentPlayer.getAttributeValue(SkadaRegistry.THRUST.get().resistAttribute()),
      UtilDefenseDisplay.attackTypeMitigationPercent(currentPlayer.getAttributeValue(SkadaRegistry.THRUST.get().resistAttribute())));
    drawPlayerDefenceStatline(pGuiGraphics, coreColumnX, statRowY + coreRow++ * STAT_ROW_SPACING, STRIKE_ICON,
      Component.translatable("skada.journal.player.core.strike.compact"), currentPlayer.getAttributeValue(SkadaRegistry.STRIKE.get().resistAttribute()),
      UtilDefenseDisplay.attackTypeMitigationPercent(currentPlayer.getAttributeValue(SkadaRegistry.STRIKE.get().resistAttribute())));
  }

  /** Draws the server-authoritative total weight preview without conflating it with burden. */
  private void drawPlayerWeightStatline(GuiGraphics graphics, int x, int y, double weightKg) {
    screen.offerTooltip(x, y, STATLINE_WIDTH, STAT_ROW_SPACING, hoverX, hoverY,
        com.cwjn.skada.client.gui.StatHelp.lines("weight", null));

    drawStatlineUnderline(graphics, x, y, STATLINE_WIDTH);
    Font currentFont = Minecraft.getInstance().font;
    Component label = UtilText.compassNineFontComponent(
        Component.translatable("skada.journal.player.core.weight.compact"));
    Component value = UtilText.pixelFontComponent(Util.roundToString(weightKg, 1) + " kg", true, false, false);
    UtilText.drawStringScaled(graphics, currentFont, label, x + 4, y + STAT_TEXT_Y_OFFSET, 0.5f,
        UI_TEXT_COLOUR, false);
    UtilText.drawStringScaled(graphics, currentFont, value, x + 48, y + STAT_NUMBER_Y_OFFSET, 0.75f, LIGHT, false);
  }

  /**
   * Draws a single-value statline for the core column.
   *
   * @param pGuiGraphics the graphics context used for rendering
   * @param x the statline x position in book-local space
   * @param y the statline y position in book-local space
   * @param icon the icon shown at the start of the row
   * @param name the localized stat label
   * @param value the stat value shown at the right edge of the row
   */
  private void drawPlayerStatline(GuiGraphics pGuiGraphics, int x, int y, ResourceLocation icon, MutableComponent name, double value) {
    String helpId = name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
        ? com.cwjn.skada.client.gui.StatHelp.statForLabel(contents.getKey()) : "configured_stat";
    screen.offerTooltip(x, y, STATLINE_WIDTH, STAT_ROW_SPACING, hoverX, hoverY,
        com.cwjn.skada.client.gui.StatHelp.lines(helpId, value));
    Font currentFont = Minecraft.getInstance().font;

    drawStatlineUnderline(pGuiGraphics, x, y, STATLINE_WIDTH);
    drawScaledIcon(pGuiGraphics, icon, x, y + STAT_ICON_Y_OFFSET, CORE_ICON_SCALE);

    MutableComponent valueComponent = UtilText.pixelFontComponent(Util.roundToString(value, 1), true, false, false);
    int coreLabelOffsetFromIcon = 10;
    UtilText.drawStringScaled(pGuiGraphics, currentFont, UtilText.compassNineFontComponent(name), x + coreLabelOffsetFromIcon, y + STAT_TEXT_Y_OFFSET, 0.5f, UI_TEXT_COLOUR, false);
    int coreValueOffsetFromLabel = coreLabelOffsetFromIcon + 60;
    UtilText.drawStringScaled(pGuiGraphics, currentFont, valueComponent, x + coreValueOffsetFromLabel, y + STAT_NUMBER_Y_OFFSET, 0.75f, LIGHT, false);
  }

  /**
   * Draws a defence rating whose derived mitigation is available in its hover explanation.
   *
   * @param pGuiGraphics the graphics context used for rendering
   * @param x the statline x position in book-local space
   * @param y the statline y position in book-local space
   * @param icon the icon shown at the start of the row
   * @param name the localized stat label
   * @param value the raw defence rating shown on the main line
   * @param mitigationPercent the derived total mitigation percentage for the row
   */
  private void drawPlayerDefenceStatline(GuiGraphics pGuiGraphics, int x, int y, ResourceLocation icon, MutableComponent name,
      double value, double mitigationPercent) {
    drawPlayerStatline(pGuiGraphics, x, y, icon, name, value);
  }

  /**
   * Draws the compact two-value statline used by the element column.
   *
   * @param pGuiGraphics the graphics context used for rendering
   * @param x the statline x position in book-local space
   * @param y the statline y position in book-local space
   * @param icon the element icon shown at the start of the row
   * @param label the localized element label
   * @param damage the total damage shown in the first inline value group
   * @param resist the resistance shown in the second inline value group
   */
  private void drawElementStatline(GuiGraphics pGuiGraphics, int x, int y, ResourceLocation icon, MutableComponent label, double damage,
      double resist, int colour) {
    screen.offerTooltip(x, y, 78, STAT_ROW_SPACING, hoverX, hoverY,
        com.cwjn.skada.client.gui.StatHelp.lines("element_damage", null));
    screen.offerTooltip(x + 78, y, 32, STAT_ROW_SPACING, hoverX, hoverY,
        com.cwjn.skada.client.gui.StatHelp.lines(icon.getPath().contains("physical") ? "physical_resist" : "element_resist", resist));
    Font currentFont = Minecraft.getInstance().font;

    drawStatlineUnderline(pGuiGraphics, x, y, STATLINE_WIDTH);
    drawScaledIcon(pGuiGraphics, icon, x, y + STAT_ICON_Y_OFFSET, ELEMENT_ICON_SCALE);
    int elementLabelOffsetFromIcon = 10;
    UtilText.drawStringScaled(pGuiGraphics, currentFont, UtilText.compassNineFontComponent(label), x + elementLabelOffsetFromIcon, y + STAT_TEXT_Y_OFFSET, 0.5f, UI_TEXT_COLOUR, false);
    int elementDamageOffsetFromLabel = 50;
    int elementResistOffsetFromDamage = elementDamageOffsetFromLabel + 30;
    drawInlineValueGroup(pGuiGraphics, DAMAGE_ICON, x + elementDamageOffsetFromLabel, y + STAT_NUMBER_Y_OFFSET, damage, colour);
    drawInlineValueGroup(pGuiGraphics, RESISTANCE_ICON, x + elementResistOffsetFromDamage, y + STAT_NUMBER_Y_OFFSET, resist, colour);
  }


  /**
   * Draws a compact section title and the mirrored underline used in the book UI.
   *
   * @param pGuiGraphics the graphics context used for rendering
   * @param currentFont the font used to render the title
   * @param title the localized title component
   * @param centerX the horizontal center of the title
   * @param titleY the top y position of the title
   * @param underlineY the y position of the underline
   */
  private void drawSectionTitle(GuiGraphics pGuiGraphics, Font currentFont, Component title, int centerX, int titleY,
      int underlineY) {
    Component compassTitle = UtilText.compassNineFontComponent(title.copy());
    int titleWidth = UtilText.scaledWidth(currentFont, compassTitle, TITLE_SCALE);

    UtilText.drawCenteredStringScaled(pGuiGraphics, currentFont, compassTitle, centerX, titleY, TITLE_SCALE,
      UI_TEXT_COLOUR, false);
    drawHorizontalGradient(pGuiGraphics, centerX, underlineY - 1, centerX + titleWidth / 2.0F, underlineY, 0,
      OPAQUE_BORDER_COLOUR, TRANSPARENT_BORDER_COLOUR);
    drawHorizontalGradient(pGuiGraphics, centerX - titleWidth / 2.0F, underlineY - 1, centerX, underlineY, 0,
      TRANSPARENT_BORDER_COLOUR, OPAQUE_BORDER_COLOUR);
  }

  /**
   * Draws the underline that anchors each stat row.
   *
   * @param pGuiGraphics the graphics context used for rendering
   * @param x the start x position of the underline
   * @param y the row y position used as the underline anchor
   * @param width the width of the underline
   */
  private void drawStatlineUnderline(GuiGraphics pGuiGraphics, int x, int y, int width) {
    int underlineY = y + STATLINE_UNDERLINE_Y_OFFSET;
    drawHorizontalGradient(pGuiGraphics, x, underlineY, x + width, underlineY + 1, 0, OPAQUE_BORDER_COLOUR,
      TRANSPARENT_BORDER_COLOUR);
  }

  /**
   * Draws the small value group used on the element rows.
   *
   * @param pGuiGraphics the graphics context used for rendering
   * @param icon the icon shown before the value
   * @param x the left x position of the group
   * @param y the top y position of the group
   * @param value the numeric value to render
   */
  private void drawInlineValueGroup(GuiGraphics pGuiGraphics, ResourceLocation icon, int x, int y, double value, int colour) {
    Font currentFont = Minecraft.getInstance().font;
    Component valueComponent = UtilText.pixelFontComponent(Util.roundToString(value, 1), true, false, false);
    int iconWidth = Math.round(16 * INLINE_VALUE_ICON_SCALE);

    float red = FastColor.ARGB32.red(colour) / 255.0F;
    float green = FastColor.ARGB32.green(colour) / 255.0F;
    float blue = FastColor.ARGB32.blue(colour) / 255.0F;
    RenderSystem.setShaderColor(red, green, blue, 0.45f);
    drawScaledIcon(pGuiGraphics, icon, x, y, INLINE_VALUE_ICON_SCALE);
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    UtilText.drawStringScaled(pGuiGraphics, currentFont, valueComponent, x + iconWidth + 2, y, 0.75f, colour, false);
  }

  /**
   * Draws a scaled icon anchored to the provided top-left position.
   *
   * @param pGuiGraphics the graphics context used for rendering
   * @param icon the icon resource to draw
   * @param x the icon x position
   * @param y the icon y position
   * @param scale the scale to apply to the 16x16 source icon
   */
  private void drawScaledIcon(GuiGraphics pGuiGraphics, ResourceLocation icon, int x, int y, float scale) {
    pGuiGraphics.pose().pushPose();
    pGuiGraphics.pose().translate(x, y, 0.0F);
    pGuiGraphics.pose().scale(scale, scale, 1.0F);
    pGuiGraphics.blit(icon, 0, 0, 0, 0, 16, 16, 16, 16);
    pGuiGraphics.pose().popPose();
  }

  /**
   * Returns the ordered set of elemental rows shown on the page.
   *
   * @return the element rows in their intended visual order
   */
  private List<Element> getDisplayedElements() {
    return List.of(
      SkadaRegistry.HEAT.get(),
      SkadaRegistry.COLD.get(),
      SkadaRegistry.LIGHTNING.get(),
      SkadaRegistry.ENDER.get(),
      SkadaRegistry.WITHER.get(),
      SkadaRegistry.AETHER.get(),
      Element.physical());
  }

  private void drawHorizontalGradient(GuiGraphics graphics, float pX1, float pY1, float pX2, float pY2, float pZ,
      int pColorFrom, int pColorTo) {
    VertexConsumer vertexConsumer = graphics.bufferSource().getBuffer(RenderType.gui());
    float f = (float) FastColor.ARGB32.alpha(pColorFrom) / 255.0F;
    float f1 = (float) FastColor.ARGB32.red(pColorFrom) / 255.0F;
    float f2 = (float) FastColor.ARGB32.green(pColorFrom) / 255.0F;
    float f3 = (float) FastColor.ARGB32.blue(pColorFrom) / 255.0F;
    float f4 = (float) FastColor.ARGB32.alpha(pColorTo) / 255.0F;
    float f5 = (float) FastColor.ARGB32.red(pColorTo) / 255.0F;
    float f6 = (float) FastColor.ARGB32.green(pColorTo) / 255.0F;
    float f7 = (float) FastColor.ARGB32.blue(pColorTo) / 255.0F;
    Matrix4f matrix4f = graphics.pose().last().pose();
    vertexConsumer.vertex(matrix4f, pX1, pY1, pZ).color(f1, f2, f3, f).endVertex();
    vertexConsumer.vertex(matrix4f, pX1, pY2, pZ).color(f1, f2, f3, f).endVertex();
    vertexConsumer.vertex(matrix4f, pX2, pY2, pZ).color(f5, f6, f7, f4).endVertex();
    vertexConsumer.vertex(matrix4f, pX2, pY1, pZ).color(f5, f6, f7, f4).endVertex();
  }

}
