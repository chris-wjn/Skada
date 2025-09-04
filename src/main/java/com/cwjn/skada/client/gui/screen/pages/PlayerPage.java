package com.cwjn.skada.client.gui.screen.pages;

import com.cwjn.skada.SkadaRegistry;
import com.cwjn.skada.client.gui.screen.JournalPage;
import com.cwjn.skada.client.gui.screen.StatScreen;
import com.cwjn.skada.util.ColourLibrary;
import com.cwjn.skada.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.opengl.GL11;

import java.math.BigDecimal;

public class PlayerPage extends JournalPage {

  private static final int PLAYER_BOX_X_OFFSET = 102;
  private static final int PLAYER_BOX_Y_OFFSET = 28;
  private static final ResourceLocation PLAYER_BOX_TEXTURE = Util.rl("textures/gui/player_page/player_box.png");
  private static final ResourceLocation HEALTH_ICON = Util.rl("textures/gui/player_page/health_icon.png");
  private static final ResourceLocation ARMOUR_ICON = Util.rl("textures/gui/book_page_icons/armour_icon.png");
  private static final ResourceLocation TOUGHNESS_ICON = Util.rl("textures/gui/player_page/toughness_icon.png");
  private static final ResourceLocation MOVESPEED_ICON = Util.rl("textures/gui/player_page/movespeed_icon.png");
  private static final ResourceLocation SLASH_ICON = Util.rl("textures/gui/player_page/slash_icon.png");
  private static final ResourceLocation THRUST_ICON = Util.rl("textures/gui/player_page/thrust_icon.png");
  private static final ResourceLocation STRIKE_ICON = Util.rl("textures/gui/player_page/strike_icon.png");
  private static final int PLAYER_BOX_WIDTH = 120;
  private static final int PLAYER_BOX_HEIGHT = 180;
  private static final Player player = Minecraft.getInstance().player;

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
    int bookX = screen.getBookX();
    int bookY = screen.getBookY();
    int playerX = bookX + PLAYER_BOX_X_OFFSET + 60; //add offset to get to center of box
    int playerY = bookY + PLAYER_BOX_Y_OFFSET + 260; //add offset to get to center of box
    GL11.glEnable(GL11.GL_SCISSOR_TEST);
    int scale = (int) Minecraft.getInstance().getWindow().getGuiScale();
    GL11.glScissor((bookX + PLAYER_BOX_X_OFFSET) * scale, Minecraft.getInstance().getWindow().getScreenHeight() - (bookY + PLAYER_BOX_Y_OFFSET + PLAYER_BOX_HEIGHT -2) * scale,
            PLAYER_BOX_WIDTH * scale, (PLAYER_BOX_HEIGHT-2) * scale);
    pGuiGraphics.pose().pushPose();
    pGuiGraphics.pose().translate(0, 0, -50);
    InventoryScreen.renderEntityInInventoryFollowsMouse(pGuiGraphics, playerX, playerY, 120,
            playerX - pMouseX, playerY - pMouseY - 195, //50 is the height of the player model, so that the cursor is centered on the player's eyes
            Minecraft.getInstance().player);
    pGuiGraphics.pose().popPose();
    GL11.glDisable(GL11.GL_SCISSOR_TEST);
    pGuiGraphics.pose().pushPose();
    pGuiGraphics.pose().translate(0, 0, 200); // Ensure the player box is rendered above the player
    pGuiGraphics.blit(PLAYER_BOX_TEXTURE, bookX + PLAYER_BOX_X_OFFSET, bookY + PLAYER_BOX_Y_OFFSET, 0, 0, PLAYER_BOX_WIDTH, PLAYER_BOX_HEIGHT, PLAYER_BOX_WIDTH, PLAYER_BOX_HEIGHT);
    pGuiGraphics.pose().popPose();
  }

  private void drawPlayerStats(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    Font font = Minecraft.getInstance().font;
    int healthX = screen.getBookX() + PLAYER_BOX_X_OFFSET + 60;
    int healthY = screen.getBookY() + PLAYER_BOX_Y_OFFSET + 182;
    float currentHealth = Util.round(Minecraft.getInstance().player.getHealth(), 1);
    float maxHealth = Util.round(Minecraft.getInstance().player.getMaxHealth(), 1);
    Component health = Util.pixelFontComponent(currentHealth + "/" + maxHealth, false, true, false);
    pGuiGraphics.drawCenteredString(font, health, healthX, healthY, ColourLibrary.HEALTH_RED);
    int healthIconX = -18 + healthX - font.width(health.getVisualOrderText()) / 2;
    pGuiGraphics.blit(HEALTH_ICON, healthIconX, healthY, 0, 0, 16, 16, 16, 16);

    drawIconThenNumber(pGuiGraphics, TOUGHNESS_ICON, screen.getBookX() + PLAYER_BOX_X_OFFSET - 45, screen.getBookY() + 50, player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), ColourLibrary.LIGHTER_GRAY);
    drawIconThenNumber(pGuiGraphics, ARMOUR_ICON, screen.getBookX() + PLAYER_BOX_X_OFFSET - 45, screen.getBookY() + 70, player.getAttributeValue(Attributes.ARMOR), ColourLibrary.LIGHTER_GRAY);
    double bps = Util.getPlayerSpeedInBlocksPerSecond(player.getAttributeValue(Attributes.MOVEMENT_SPEED));
    drawIconThenNumber(pGuiGraphics, MOVESPEED_ICON, screen.getBookX() + PLAYER_BOX_X_OFFSET - 45, screen.getBookY() + 90, bps, Util.getColourByPercentage(bps, 4.3, true));
    drawIconThenNumber(pGuiGraphics, SLASH_ICON, screen.getBookX() + PLAYER_BOX_X_OFFSET - 45, screen.getBookY() + 110, player.getAttributeValue(SkadaRegistry.SLASH.get().resistAttribute()), Util.getColourByPercentage(player.getAttributeValue(SkadaRegistry.SLASH.get().resistAttribute()), 0, true));
    drawIconThenNumber(pGuiGraphics, THRUST_ICON, screen.getBookX() + PLAYER_BOX_X_OFFSET - 45, screen.getBookY() + 130, player.getAttributeValue(SkadaRegistry.THRUST.get().resistAttribute()), Util.getColourByPercentage(player.getAttributeValue(SkadaRegistry.THRUST.get().resistAttribute()), 0, true));
    drawIconThenNumber(pGuiGraphics, STRIKE_ICON, screen.getBookX() + PLAYER_BOX_X_OFFSET - 45, screen.getBookY() + 150, player.getAttributeValue(SkadaRegistry.STRIKE.get().resistAttribute()), Util.getColourByPercentage(player.getAttributeValue(SkadaRegistry.STRIKE.get().resistAttribute()), 0, true));
    drawIconThenNumber(pGuiGraphics, SkadaRegistry.HEAT.get().icon(), screen.getBookX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40, screen.getBookY() + 50, player.getAttributeValue(SkadaRegistry.HEAT.get().resist()), ColourLibrary.HEAT);
    drawIconThenNumber(pGuiGraphics, SkadaRegistry.COLD.get().icon(), screen.getBookX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40, screen.getBookY() + 70, player.getAttributeValue(SkadaRegistry.COLD.get().resist()), ColourLibrary.COLD);
    drawIconThenNumber(pGuiGraphics, SkadaRegistry.LIGHTNING.get().icon(), screen.getBookX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40, screen.getBookY() + 90, player.getAttributeValue(SkadaRegistry.LIGHTNING.get().resist()), ColourLibrary.LIGHTNING);
    drawIconThenNumber(pGuiGraphics, SkadaRegistry.ENDER.get().icon(), screen.getBookX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40, screen.getBookY() + 110, player.getAttributeValue(SkadaRegistry.ENDER.get().resist()), ColourLibrary.ENDER);
    drawIconThenNumber(pGuiGraphics, SkadaRegistry.WITHER.get().icon(), screen.getBookX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40, screen.getBookY() + 130, player.getAttributeValue(SkadaRegistry.WITHER.get().resist()), ColourLibrary.WITHER);
    drawIconThenNumber(pGuiGraphics, SkadaRegistry.AETHER.get().icon(), screen.getBookX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40, screen.getBookY() + 150, player.getAttributeValue(SkadaRegistry.AETHER.get().resist()), ColourLibrary.AETHER);
    pGuiGraphics.pose().pushPose();
    pGuiGraphics.pose().scale(0.5f, 0.5f, 1f);
    pGuiGraphics.blit(ARMOUR_ICON, (screen.getBookX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40 - 10)*2, (screen.getBookY() + 50 +9)*2, 0, 0, 16, 16, 16, 16);
    pGuiGraphics.blit(ARMOUR_ICON, (screen.getBookX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40 - 10)*2, (screen.getBookY() + 70 +9)*2, 0, 0, 16, 16, 16, 16);
    pGuiGraphics.blit(ARMOUR_ICON, (screen.getBookX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40 - 10)*2, (screen.getBookY() + 90 +9)*2, 0, 0, 16, 16, 16, 16);
    pGuiGraphics.blit(ARMOUR_ICON, (screen.getBookX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40 - 10)*2, (screen.getBookY() + 110 +9)*2, 0, 0, 16, 16, 16, 16);
    pGuiGraphics.blit(ARMOUR_ICON, (screen.getBookX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40 - 10)*2, (screen.getBookY() + 130 +9)*2, 0, 0, 16, 16, 16, 16);
    pGuiGraphics.blit(ARMOUR_ICON, (screen.getBookX() + PLAYER_BOX_X_OFFSET + PLAYER_BOX_WIDTH + 40 - 10)*2, (screen.getBookY() + 150 +9)*2, 0, 0, 16, 16, 16, 16);
    pGuiGraphics.pose().popPose();

  }

  private void drawIconThenNumber(GuiGraphics pGuiGraphics, ResourceLocation icon, int x, int y, double value, int colour) {
    String valueString = new BigDecimal(value).setScale(1, BigDecimal.ROUND_HALF_UP).toString();
    Component valueComponent = Util.pixelFontComponent(valueString, false, true, false);
    pGuiGraphics.drawString(Minecraft.getInstance().font, valueComponent, x, y, colour);
    pGuiGraphics.blit(icon, -18 + x, y, 0, 0, 16, 16, 16, 16);
  }

}
