package com.cwjn.skada.client.gui.button;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class TabButton extends Button {

    public TabButton(int pX, int pY, Component pMessage, OnPress pOnPress) {
        super(pX, pY, 32, 26, pMessage, pOnPress, Button.DEFAULT_NARRATION);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.isMouseOver(pMouseX, pMouseY)) {
            pGuiGraphics.vLine(this.getX()+32-2, this.getY()+1, this.getY()+this.height-2, 0xFFFFFFFF);
            pGuiGraphics.hLine(this.getX(), this.getX()+32-3, this.getY()+1, 0xFFFFFFFF);
            pGuiGraphics.hLine(this.getX(), this.getX()+32-3, this.getY()+this.height-2, 0xFFFFFFFF);
        }
    }

}
