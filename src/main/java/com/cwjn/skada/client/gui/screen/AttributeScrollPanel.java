package com.cwjn.skada.client.gui.screen;

import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraftforge.client.gui.widget.ScrollPanel;

public class AttributeScrollPanel extends ScrollPanel {

    public AttributeScrollPanel(Minecraft client, int width, int height, int top, int left) {
        super(client, width, height, top, left);
    }

    @Override
    protected int getContentHeight() {
        return 50;
    }

    @Override
    protected void drawPanel(GuiGraphics guiGraphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {

    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {

    }

}
