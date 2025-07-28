package com.cwjn.skada.client.gui.button;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class TabButton extends Button {

    private ResourceLocation icon;
    private boolean renderIcon = true;
    private boolean wasRenderIcon = true;
    private long fadeStartTime = 0;
    private static final long FADE_DURATION = 200;

    public TabButton(int pX, int pY, Component pMessage, OnPress pOnPress) {
        super(pX, pY, 32, 26, pMessage, pOnPress, Button.DEFAULT_NARRATION);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.active) {
            if (this.isMouseOver(pMouseX, pMouseY)) {
                pGuiGraphics.vLine(this.getX()+32-2, this.getY()+1, this.getY()+this.height-2, 0xFFFFFFFF);
                pGuiGraphics.hLine(this.getX(), this.getX()+32-3, this.getY()+1, 0xFFFFFFFF);
                pGuiGraphics.hLine(this.getX(), this.getX()+32-3, this.getY()+this.height-2, 0xFFFFFFFF);
            }
        }
        if (renderIcon && !wasRenderIcon) {
            fadeStartTime = System.currentTimeMillis();
        }
        wasRenderIcon = renderIcon;

        if (renderIcon && icon != null) {
            float alpha = 1.0f;

            // Calculate fade alpha if we're in fade period
            long currentTime = System.currentTimeMillis();
            if (currentTime - fadeStartTime < FADE_DURATION) {
                alpha = (float)(currentTime - fadeStartTime) / FADE_DURATION;
                alpha = Mth.clamp(alpha, 0.0f, 1.0f);
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            if (active) {
                pGuiGraphics.blit(icon, this.getX()+7, this.getY()+5, 0, 0, 16, 16, 16, 16);
            } else {
                pGuiGraphics.blit(icon, this.getX()+16, this.getY()+5, 0, 0, 16, 16, 16, 16);
            }
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    public void setIcon(ResourceLocation icon) {
        this.icon = icon;
    }

    public void setRenderIcon(boolean newRenderIconState) {
        this.renderIcon = newRenderIconState;
    }

    public boolean isRenderIcon() {
        return renderIcon;
    }

}
