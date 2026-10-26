package com.cwjn.skada.client.gui.button;

import com.cwjn.skada.util.Util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CarouselArrowButton extends Button {

  private static final int BUTTON_WIDTH = 17;
  private static final int BUTTON_HEIGHT = 12;
  public static final int ARROW_WIDTH = 15;
  private static final int ARROW_HEIGHT = 10;
  private static final ResourceLocation RIGHT_ARROW_TEXTURE = Util.rl("textures/gui/carousel_arrow_right.png");
  private static final ResourceLocation RIGHT_ARROW_HIGHLIGHTED_TEXTURE = Util
      .rl("textures/gui/carousel_arrow_right_highlighted.png");
  private static final ResourceLocation LEFT_ARROW_TEXTURE = Util.rl("textures/gui/carousel_arrow_left.png");
  private static final ResourceLocation LEFT_ARROW_HIGHLIGHTED_TEXTURE = Util
      .rl("textures/gui/carousel_arrow_left_highlighted.png");

  private final boolean pointLeft;

  public CarouselArrowButton(int x, int y, boolean pointLeft, OnPress onPress) {
    super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.empty(), onPress, DEFAULT_NARRATION);
    this.pointLeft = pointLeft;
  }

  @Override
  public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    boolean hovered = this.isHovered();
    ResourceLocation texture = getTexture(hovered);
    int drawWidth = hovered ? BUTTON_WIDTH : ARROW_WIDTH;
    int drawHeight = hovered ? BUTTON_HEIGHT : ARROW_HEIGHT;
    int drawX = this.getX() + ((BUTTON_WIDTH - drawWidth) / 2);
    int drawY = this.getY() + ((BUTTON_HEIGHT - drawHeight) / 2);

    guiGraphics.blit(texture, drawX, drawY, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
  }

  private ResourceLocation getTexture(boolean hovered) {
    if (pointLeft) {
      return hovered ? LEFT_ARROW_HIGHLIGHTED_TEXTURE : LEFT_ARROW_TEXTURE;
    }
    return hovered ? RIGHT_ARROW_HIGHLIGHTED_TEXTURE : RIGHT_ARROW_TEXTURE;
  }

}