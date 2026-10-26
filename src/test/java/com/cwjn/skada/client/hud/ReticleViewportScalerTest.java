package com.cwjn.skada.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReticleViewportScalerTest {

    @Test
    void referenceViewportKeepsCoordinatesUnchanged() {
        float scale = ReticleViewportScaler.getViewportScale(1600, 1000);

        assertEquals(1.0F, scale, 0.0001F);
        assertEquals(167.0F, ReticleViewportScaler.scaleToScreen(167.0F, scale), 0.0001F);
    }

    @Test
    void smallerViewportShrinksCoordinatesProportionally() {
        float scale = ReticleViewportScaler.getViewportScale(1280, 720);

        assertEquals(0.72F, scale, 0.0001F);
        assertEquals(120.24F, ReticleViewportScaler.scaleToScreen(167.0F, scale), 0.0001F);
    }

    @Test
    void guiOffsetsUseTheSameViewportScale() {
        float scale = ReticleViewportScaler.getViewportScale(1280, 720);

        assertEquals(60.12F, ReticleViewportScaler.scaleToGui(167.0F, scale, 2.0F), 0.0001F);
    }
}