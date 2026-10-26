package com.cwjn.skada.mixin.new_features;

import com.cwjn.skada.ClientConfig;
import com.cwjn.skada.client.ClientEvent;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Shortens the vanilla crosshair sprite when Skada's custom reticles are active.
 *
 * The attack indicator and the rest of the vanilla crosshair overlay remain untouched.
 */
@Mixin(Gui.class)
public class CustomReticlesShortenVanillaCrosshair {

    private static final int VANILLA_CROSSHAIR_SIZE = 15;

    /**
         * Crops the vanilla 15x15 crosshair sprite down to a centered size derived from the active
         * reticle bounds while custom reticles are enabled.
     *
     * @param guiGraphics the graphics instance rendering the HUD
     * @param atlasLocation the vanilla GUI icon atlas
     * @param x the destination x position
     * @param y the destination y position
     * @param uOffset the source texture x position
     * @param vOffset the source texture y position
     * @param uWidth the requested source width
     * @param vHeight the requested source height
     */
    @Redirect(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
                    ordinal = 0
            )
    )
    private void renderShorterCrosshairSprite(GuiGraphics guiGraphics,
                                              ResourceLocation atlasLocation,
                                              int x,
                                              int y,
                                              int uOffset,
                                              int vOffset,
                                              int uWidth,
                                              int vHeight) {
        if (!ClientConfig.ENABLE_CUSTOM_RETICLES.get()) {
            guiGraphics.blit(atlasLocation, x, y, uOffset, vOffset, uWidth, vHeight);
            return;
        }

        int crosshairSize = ClientEvent.getActiveCrosshairSpriteSize(Minecraft.getInstance());
        if (crosshairSize >= VANILLA_CROSSHAIR_SIZE) {
            guiGraphics.blit(atlasLocation, x, y, uOffset, vOffset, uWidth, vHeight);
            return;
        }

        int cropOffset = (VANILLA_CROSSHAIR_SIZE - crosshairSize) / 2;

        guiGraphics.blit(
                atlasLocation,
                x + cropOffset,
                y + cropOffset,
                uOffset + cropOffset,
                vOffset + cropOffset,
                crosshairSize,
                crosshairSize
        );
    }

    /**
     * Keeps the debug line crosshair proportionally shorter when custom reticles are enabled.
     *
     * @param lineLength the vanilla line length requested by the HUD renderer
     */
    @Redirect(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;renderCrosshair(I)V"
            )
    )
    private void renderShorterDebugCrosshair(int lineLength) {
        RenderSystem.renderCrosshair(
                ClientConfig.ENABLE_CUSTOM_RETICLES.get()
                        ? ClientEvent.getActiveDebugCrosshairLineLength(Minecraft.getInstance())
                        : lineLength);
    }
}