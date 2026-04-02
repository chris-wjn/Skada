package com.cwjn.skada.client.gui.screen.pages;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.cwjn.skada.SkadaRegistry;
import com.cwjn.skada.client.gui.screen.JournalPage;
import com.cwjn.skada.client.gui.screen.StatScreen;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilColour;
import com.cwjn.skada.util.UtilText;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import static com.cwjn.skada.util.UtilColour.*;

public class PlayerPage extends JournalPage {

  private static final int PLAYER_BOX_X_OFFSET = 55;
  private static final int PLAYER_BOX_Y_OFFSET = 33;
  private static final int PLAYER_BOX_WIDTH = 222;
  private static final int PLAYER_BOX_HEIGHT = 137;
  private static final int PLAYER_BOX_BORDER_THICKNESS = 10;
  private static final ResourceLocation PLAYER_BOX_TEXTURE = Util.rl("textures/gui/player_page/player_box.png");
  private static final ResourceLocation HEALTH_ICON = Util.rl("textures/gui/player_page/health_icon.png");
  private static final ResourceLocation ARMOUR_ICON = Util.rl("textures/gui/book_page_icons/armour_icon.png");
  private static final ResourceLocation TOUGHNESS_ICON = Util.rl("textures/gui/player_page/toughness_icon.png");
  private static final ResourceLocation MOVESPEED_ICON = Util.rl("textures/gui/player_page/movespeed_icon.png");
  private static final ResourceLocation SLASH_ICON = Util.rl("textures/gui/player_page/slash_icon.png");
  private static final ResourceLocation THRUST_ICON = Util.rl("textures/gui/player_page/thrust_icon.png");
  private static final ResourceLocation STRIKE_ICON = Util.rl("textures/gui/player_page/strike_icon.png");
  
  private static final Player player = Minecraft.getInstance().player;
  private static final Font font = Minecraft.getInstance().font;
  

  public PlayerPage(ResourceLocation icon, ResourceLocation pageResource, StatScreen screen) {
    super(icon, pageResource, screen);
  }

  @Override
  public void renderExtraDrawables(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    super.renderExtraDrawables(pGuiGraphics, pMouseX, pMouseY);
    drawPlayerBox(pGuiGraphics, pMouseX, pMouseY);
    drawPlayerStats(pGuiGraphics, pMouseX, pMouseY);
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

  private void drawPlayerStatsOld(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    Font font = Minecraft.getInstance().font;
    int healthX = screen.getBookLocalX() + PLAYER_BOX_X_OFFSET;
    int healthY = screen.getBookLocalY() + PLAYER_BOX_Y_OFFSET;
    float currentHealth = Util.round(Minecraft.getInstance().player.getHealth(), 1);
    float maxHealth = Util.round(Minecraft.getInstance().player.getMaxHealth(), 1);
    Component health = UtilText.pixelFontComponent(currentHealth + "/" + maxHealth, false, true, false);
    pGuiGraphics.drawCenteredString(font, health, healthX, healthY, UtilColour.HEALTH_RED);
    int healthIconX = -18 + healthX - font.width(health.getVisualOrderText()) / 2;
    pGuiGraphics.blit(HEALTH_ICON, healthIconX, healthY, 0, 0, 16, 16, 16, 16);

    drawIconThenNumber(pGuiGraphics, TOUGHNESS_ICON, screen.getBookLocalX() + PLAYER_BOX_X_OFFSET - 45, screen.getBookLocalY() + 50, player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), UtilColour.LIGHTER_GRAY);
    drawIconThenNumber(pGuiGraphics, ARMOUR_ICON, screen.getBookLocalX() + PLAYER_BOX_X_OFFSET - 45, screen.getBookLocalY() + 70, player.getAttributeValue(Attributes.ARMOR), UtilColour.LIGHTER_GRAY);
    double bps = Util.getPlayerSpeedInBlocksPerSecond(player.getAttributeValue(Attributes.MOVEMENT_SPEED));
    drawIconThenNumber(pGuiGraphics, MOVESPEED_ICON, screen.getBookLocalX() + PLAYER_BOX_X_OFFSET - 45, screen.getBookLocalY() + 90, bps, UtilColour.getColourByPercentage(bps, 4.3, true));
    drawIconThenNumber(pGuiGraphics, SLASH_ICON, screen.getBookLocalX() + PLAYER_BOX_X_OFFSET - 45, screen.getBookLocalY() + 110, player.getAttributeValue(SkadaRegistry.SLASH.get().resistAttribute()), UtilColour.getColourByPercentage(player.getAttributeValue(SkadaRegistry.SLASH.get().resistAttribute()), 0, true));
    drawIconThenNumber(pGuiGraphics, THRUST_ICON, screen.getBookLocalX() + PLAYER_BOX_X_OFFSET - 45, screen.getBookLocalY() + 130, player.getAttributeValue(SkadaRegistry.THRUST.get().resistAttribute()), UtilColour.getColourByPercentage(player.getAttributeValue(SkadaRegistry.THRUST.get().resistAttribute()), 0, true));
    drawIconThenNumber(pGuiGraphics, STRIKE_ICON, screen.getBookLocalX() + PLAYER_BOX_X_OFFSET - 45, screen.getBookLocalY() + 150, player.getAttributeValue(SkadaRegistry.STRIKE.get().resistAttribute()), UtilColour.getColourByPercentage(player.getAttributeValue(SkadaRegistry.STRIKE.get().resistAttribute()), 0, true));
    drawIconThenNumber(pGuiGraphics, SkadaRegistry.HEAT.get().icon(), screen.getBookLocalX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40, screen.getBookLocalY() + 50, player.getAttributeValue(SkadaRegistry.HEAT.get().resist()), UtilColour.HEAT);
    drawIconThenNumber(pGuiGraphics, SkadaRegistry.COLD.get().icon(), screen.getBookLocalX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40, screen.getBookLocalY() + 70, player.getAttributeValue(SkadaRegistry.COLD.get().resist()), UtilColour.COLD);
    drawIconThenNumber(pGuiGraphics, SkadaRegistry.LIGHTNING.get().icon(), screen.getBookLocalX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40, screen.getBookLocalY() + 90, player.getAttributeValue(SkadaRegistry.LIGHTNING.get().resist()), UtilColour.LIGHTNING);
    drawIconThenNumber(pGuiGraphics, SkadaRegistry.ENDER.get().icon(), screen.getBookLocalX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40, screen.getBookLocalY() + 110, player.getAttributeValue(SkadaRegistry.ENDER.get().resist()), UtilColour.ENDER);
    drawIconThenNumber(pGuiGraphics, SkadaRegistry.WITHER.get().icon(), screen.getBookLocalX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40, screen.getBookLocalY() + 130, player.getAttributeValue(SkadaRegistry.WITHER.get().resist()), UtilColour.WITHER);
    drawIconThenNumber(pGuiGraphics, SkadaRegistry.AETHER.get().icon(), screen.getBookLocalX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40, screen.getBookLocalY() + 150, player.getAttributeValue(SkadaRegistry.AETHER.get().resist()), UtilColour.AETHER);
    pGuiGraphics.pose().pushPose();
    pGuiGraphics.pose().scale(0.5f, 0.5f, 1f);
    pGuiGraphics.blit(ARMOUR_ICON, (screen.getBookLocalX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40 - 10)*2, (screen.getBookLocalY() + 50 +9)*2, 0, 0, 16, 16, 16, 16);
    pGuiGraphics.blit(ARMOUR_ICON, (screen.getBookLocalX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40 - 10)*2, (screen.getBookLocalY() + 70 +9)*2, 0, 0, 16, 16, 16, 16);
    pGuiGraphics.blit(ARMOUR_ICON, (screen.getBookLocalX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40 - 10)*2, (screen.getBookLocalY() + 90 +9)*2, 0, 0, 16, 16, 16, 16);
    pGuiGraphics.blit(ARMOUR_ICON, (screen.getBookLocalX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40 - 10)*2, (screen.getBookLocalY() + 110 +9)*2, 0, 0, 16, 16, 16, 16);
    pGuiGraphics.blit(ARMOUR_ICON, (screen.getBookLocalX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40 - 10)*2, (screen.getBookLocalY() + 130 +9)*2, 0, 0, 16, 16, 16, 16);
    pGuiGraphics.blit(ARMOUR_ICON, (screen.getBookLocalX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40 - 10)*2, (screen.getBookLocalY() + 150 +9)*2, 0, 0, 16, 16, 16, 16);
    pGuiGraphics.pose().popPose();

  }

  private final int ELEMENT_TITLE_X_OFFSET = 50;
  private final int ELEMENT_TITLE_Y_OFFSET = 180;
  private final int CORE_TITLE_X_OFFSET = 165;
  private final int CORE_TITLE_Y_OFFSET = 180;
  private final int UNDERLINE_Y_OFFSET = 16 + 6; //16 is the height of the font, 6 is the distance from the bottom of the text to the underline
  private final int opaqueBorderColour = UI_BORDER_COLOUR | 0xFF000000;
  private final int transparentBorderColour = UI_BORDER_COLOUR;
  private void drawPlayerStats(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    int elementTitleX = screen.getBookLocalX() + ELEMENT_TITLE_X_OFFSET;
    int elementTitleY = screen.getBookLocalY() + ELEMENT_TITLE_Y_OFFSET;
    int coreTitleX = screen.getBookLocalX() + CORE_TITLE_X_OFFSET;
    int coreTitleY = screen.getBookLocalY() + CORE_TITLE_Y_OFFSET;

    Component elementTitle = UtilText.pixelFontComponent(Component.translatable("skada.journal.player.element_title"), false, true, false);
    Component elementTitleCompass9 = UtilText.compassNineFontComponent(Component.translatable("skada.journal.player.element_title"));
    drawCenteredStringNoShadow(pGuiGraphics, elementTitleCompass9, elementTitleX, elementTitleY, UI_TEXT_COLOUR);
    int elementTitleWidth = font.width(elementTitle.getVisualOrderText());
    int elementCompass9Width = font.width(elementTitleCompass9.getVisualOrderText());
    drawHorizontalGradient(pGuiGraphics, elementTitleX, elementTitleY+UNDERLINE_Y_OFFSET-1, elementTitleX+elementCompass9Width/2, elementTitleY+UNDERLINE_Y_OFFSET, 0, opaqueBorderColour, transparentBorderColour);
    drawHorizontalGradient(pGuiGraphics, elementTitleX-elementCompass9Width/2, elementTitleY+UNDERLINE_Y_OFFSET-1, elementTitleX, elementTitleY+UNDERLINE_Y_OFFSET, 0, transparentBorderColour, opaqueBorderColour);

    Component coreTitle = UtilText.pixelFontComponent(Component.translatable("skada.journal.player.core_title"), false, true, false);
    Component coreTitleCompass9 = UtilText.compassNineFontComponent(Component.translatable("skada.journal.player.core_title"));
    drawCenteredStringNoShadow(pGuiGraphics, coreTitleCompass9, coreTitleX, coreTitleY, UI_TEXT_COLOUR);
    int coreTitleWidth = font.width(coreTitle.getVisualOrderText());
    int coreTitleCompass9Width = font.width(coreTitleCompass9.getVisualOrderText());
    drawHorizontalGradient(pGuiGraphics, coreTitleX, coreTitleY+UNDERLINE_Y_OFFSET-1, coreTitleX+coreTitleCompass9Width/2, coreTitleY+UNDERLINE_Y_OFFSET, 0, opaqueBorderColour, transparentBorderColour);
    drawHorizontalGradient(pGuiGraphics, coreTitleX-coreTitleCompass9Width/2, coreTitleY+UNDERLINE_Y_OFFSET-1, coreTitleX, coreTitleY+UNDERLINE_Y_OFFSET, 0, transparentBorderColour, opaqueBorderColour);
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

  private void drawCenteredStringNoShadow(GuiGraphics pGuiGraphics, Component text, int x, int y, int colour) {
    pGuiGraphics.drawString(font, text, x - font.width(text.getVisualOrderText()) / 2, y, colour, false);
  }

  /**
   * Draws an icon with a number to the right of it
   * @param pGuiGraphics
   * @param icon
   * @param x
   * @param y
   * @param value
   * @param colour
   */
  private void drawIconThenNumber(GuiGraphics pGuiGraphics, ResourceLocation icon, int x, int y, double value, int colour) {
    String valueString = new BigDecimal(value).setScale(1, BigDecimal.ROUND_HALF_UP).toString();
    Component valueComponent = UtilText.pixelFontComponent(valueString, false, true, false);
    pGuiGraphics.drawString(Minecraft.getInstance().font, valueComponent, x, y, colour);
    pGuiGraphics.blit(icon, -18 + x, y, 0, 0, 16, 16, 16, 16);
  }

}
