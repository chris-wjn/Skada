package com.cwjn.skada.client.gui.button;

import com.cwjn.skada.data.mob.BestiaryEntryView;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilColour;
import com.cwjn.skada.util.UtilText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class EntityButton extends Button {

  private static final Minecraft MINECRAFT = Minecraft.getInstance();
  private static final int ENTRY_WIDTH = 94;
  private static final int ENTRY_HEIGHT = 24;
  private static final ResourceLocation PANEL_TEXTURE = Util.rl("textures/gui/damage_page/element_info_panel.png");
  private static final int PANEL_TEXTURE_WIDTH = 171;
  private static final int PANEL_TEXTURE_HEIGHT = 88;
  private static final int PANEL_WIDTH = 171;
  private static final int PANEL_HEIGHT = 44;

  private final BestiaryEntryView entry;
  private boolean hoveredWithinViewport;
  private boolean newlyUnlocked;
  private boolean selected;

  public EntityButton(int x, int y, BestiaryEntryView entry, OnPress onPress) {
    super(x, y, ENTRY_WIDTH, ENTRY_HEIGHT,
        Component.translatable(entry.displayNameKey()), onPress, Button.DEFAULT_NARRATION);
    this.entry = entry;
  }

  public BestiaryEntryView getEntry() {
    return entry;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public void setHoveredWithinViewport(boolean hoveredWithinViewport) {
    this.hoveredWithinViewport = hoveredWithinViewport;
  }

  public void setNewlyUnlocked(boolean newlyUnlocked) {
    this.newlyUnlocked = newlyUnlocked;
  }

  public void setThumbnailTexture(@Nullable ResourceLocation thumbnailTexture) {
    // Thumbnail support is optional and not implemented yet.
  }

  public void renderTile(GuiGraphics graphics) {
    int textureV = selected || hoveredWithinViewport ? PANEL_HEIGHT : 0;
    graphics.blit(PANEL_TEXTURE, getX(), getY(), width, height, 0, textureV, PANEL_WIDTH, PANEL_HEIGHT,
        PANEL_TEXTURE_WIDTH, PANEL_TEXTURE_HEIGHT);

    if (selected) {
      graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0x1AFFFFFF);
    }

    int badgeWidth = 0;
    if (newlyUnlocked) {
      Component badgeText = Component.translatable("skada.journal.bestiary.new_badge");
      badgeWidth = MINECRAFT.font.width(badgeText) + 8;
      int badgeX = getX() + width - badgeWidth - 4;
      int badgeY = getY() + 4;

      graphics.fill(getX() + 2, getY() + 2, getX() + 6, getY() + height - 2, 0xFFDBA84A);
      graphics.fill(getX() + 6, getY() + 2, getX() + 7, getY() + height - 2, 0xFFF7E1AE);
      graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + 3, 0xCCF1CD8D);
      graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 10, 0xE0C98C34);
      graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 1, 0xFFF4D7A2);
      graphics.drawString(MINECRAFT.font, badgeText, badgeX + 4, badgeY + 1, UtilColour.WHITE, false);
    }

    int textX = getX() + 8;
    int textWidth = Math.max(18, width - 16 - badgeWidth);
    Component nameText = UtilText.pixelFontComponent(trimmedTranslation(entry.displayNameKey(), textWidth), false, true,
        false);
    int nameColor = selected ? UtilColour.WHITE : UtilColour.UI_TEXT_COLOUR;
    graphics.drawString(MINECRAFT.font, nameText, textX,
        getY() + Math.max(5, (height - MINECRAFT.font.lineHeight) / 2), nameColor, false);
  }

  @Override
  public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
  }

  @Override
  protected boolean clicked(double mouseX, double mouseY) {
    return this.active && this.visible && hoveredWithinViewport;
  }

  private static String trimmedTranslation(String translationKey, int maxWidth) {
    String resolved = Component.translatable(translationKey).getString();
    return MINECRAFT.font.plainSubstrByWidth(resolved, maxWidth);
  }
}
