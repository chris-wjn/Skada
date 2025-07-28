package com.cwjn.skada.client.gui.button;

import com.cwjn.skada.client.gui.screen.widget.InfoPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class InfoTileButton extends Button {

    private static final Minecraft minecraft = Minecraft.getInstance();
    private InfoPanel infoPanel;
    private boolean isHovered = false;

    public InfoTileButton(int x, int y, Component message, OnPress onPress) {
        super(x, y, 171, 44, message, onPress, DEFAULT_NARRATION);
    }

    public void setInfoPanel(InfoPanel panel) {
        this.infoPanel = panel;
    }

    public InfoPanel getInfoPanel() {
        return infoPanel;
    }

    public void setIsHovered(boolean hovered) {
        this.isHovered = hovered;
    }

    public void renderTile(GuiGraphics graphics) {
        // Render tile background
        int bgColor = isHovered ? 0x99FFFFFF : 0x55FFFFFF;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);

        // Render border
        int borderColor = isHovered ? 0xFFFFFFFF : 0x99FFFFFF;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 1, borderColor);
        graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), borderColor);
        graphics.fill(getX(), getY(), getX() + 1, getY() + getHeight(), borderColor);
        graphics.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), borderColor);

        // Render text
        int textColor = isHovered ? 0xFFFFFF : 0xCCCCCC;
        graphics.drawCenteredString(minecraft.font, getMessage(),
                getX() + getWidth() / 2, getY() + getHeight() / 2 - 4, textColor);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Override to prevent default button rendering
    }
}