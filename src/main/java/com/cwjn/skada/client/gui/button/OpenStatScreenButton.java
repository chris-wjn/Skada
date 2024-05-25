package com.cwjn.skada.client.gui.button;

import com.cwjn.skada.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class OpenStatScreenButton extends Button {

    private static final ResourceLocation SPRITESHEET = Util.rl("textures/gui/spritesheet.png");

    public OpenStatScreenButton(int pX, int pY, Component pMessage, OnPress pOnPress) {
        super(pX, pY, 20, 20, pMessage, pOnPress, Button.DEFAULT_NARRATION);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.blit(SPRITESHEET, getX()+2, getY()+2, 8, 0, 16, 16);
    }

}
