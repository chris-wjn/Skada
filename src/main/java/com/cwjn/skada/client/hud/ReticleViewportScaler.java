package com.cwjn.skada.client.hud;

public final class ReticleViewportScaler {

    private static final float REFERENCE_VIEWPORT_EDGE = 1000.0F;

    private ReticleViewportScaler() {
    }

    // Reticle JSON coordinates are authored against a 1000-unit reference viewport, not literal pixels.
    public static float getViewportScale(int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return 1.0F;
        }
        return Math.min(screenWidth, screenHeight) / REFERENCE_VIEWPORT_EDGE;
    }

    public static float scaleToScreen(float coordinate, float viewportScale) {
        return coordinate * viewportScale;
    }

    public static float scaleToGui(float coordinate, float viewportScale, float guiScale) {
        return scaleToScreen(coordinate, viewportScale) / guiScale;
    }
}