package com.cwjn.skada.client.hud;

import com.cwjn.skada.util.Util;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public class MobHealthBar {

    private static final int MAX_BAR_WIDTH = 75;

    private static final ResourceLocation HEALTH_GUI = Util.rl("textures/gui/spritesheet.png");

    private static final Minecraft client = Minecraft.getInstance();

    private static final Map<LivingEntity, Float> renderables = new HashMap<>();

    public static void prepare(LivingEntity entity, float alpha) {
        renderables.put(entity, alpha);
    }

    public static void renderBars(float pTick, PoseStack stack, Camera cam) {

        if (cam == null) {
            cam = client.getEntityRenderDispatcher().camera;
        }
        if (cam == null) {
            renderables.clear();
            return;
        }
        if (renderables.isEmpty()) {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE,
                GL11.GL_ZERO);

        for (LivingEntity entity : renderables.keySet()) {
            float alpha = renderables.get(entity);
            if (alpha <= 0.05) continue;
            float scaleToGui = 0.025f;
            boolean sneaking = entity.isCrouching();
            float height = entity.getBbHeight() + 0.73F - (sneaking ? 0.25F : 0.0F);

            double x = Mth.lerp(pTick, entity.xo, entity.getX());
            double y = Mth.lerp(pTick, entity.yo, entity.getY());
            double z = Mth.lerp(pTick, entity.zo, entity.getZ());

            Vec3 camPos = cam.getPosition();
            double camX = camPos.x();
            double camY = camPos.y();
            double camZ = camPos.z();

            stack.pushPose();
            stack.translate(x - camX, (y + height) - camY, z - camZ);
            stack.mulPose(Axis.YP.rotationDegrees(-cam.getYRot()));
            stack.mulPose(Axis.XP.rotationDegrees(cam.getXRot()));
            stack.scale(-scaleToGui, -scaleToGui, scaleToGui);

            renderBar(stack, entity, renderables.get(entity));

            stack.popPose();
        }

        RenderSystem.disableBlend();
        renderables.clear();
    }

    private static void renderBar(PoseStack stack, LivingEntity entity, float alpha) {
        //get values from entity
        float currentHealth = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        boolean isHundreds = maxHealth>=1000;
        float percent = Math.min(1, Math.min(currentHealth, maxHealth) / maxHealth);
        float percentOfMax = Math.min((isHundreds? 100:10)/maxHealth, 1);

        //draw bars
        Matrix4f m4f = stack.last().pose();
        drawBar(m4f, 1, 0, true, alpha, 0, false);
        drawBar(m4f, percent, 1, false, alpha, percentOfMax, isHundreds);
    }

    private static void drawBar(Matrix4f matrix, float percent, int zOffset, boolean background, float alpha, float percentOfMax, boolean isHundreds) {
        float CONSTANT = 0.00390625F;
        int xTexOffset = 75;
        int yTexOffset = background ? 34 : 30;
        int width = Mth.ceil(81 * percent);
        int height = 4;
        float half = (float) MAX_BAR_WIDTH / 2;
        float zOffsetAmount = -0.1F;
        double size = percent * (float) MAX_BAR_WIDTH;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, HEALTH_GUI);
        RenderSystem.enableBlend();
        /*if (ClientData.shadersLoaded) {
            if (isHundreds) RenderSystem.setShaderColor(0.4f, 0.4f, 0f, alpha);
            else RenderSystem.setShaderColor(0.4f, 0f, 0f, alpha);
        }
        else {
            if (isHundreds) RenderSystem.setShaderColor(1f, 1f, 0f, alpha);
            else RenderSystem.setShaderColor(1f, 0f, 0f, alpha);
        }*/

        if (isHundreds) RenderSystem.setShaderColor(1f, 1f, 0f, alpha);
        else RenderSystem.setShaderColor(1f, 0f, 0f, alpha);
        if (background) RenderSystem.setShaderColor(0f, 0f, 0f, alpha);

        //Red bar
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, (float) (-half + (double) 0), (float) (double) -0.1, zOffset * zOffsetAmount)
                .uv(xTexOffset * CONSTANT, yTexOffset * CONSTANT).endVertex();
        buffer.vertex(matrix, (float) (-half + (double) 0), (float) (height + (double) 0.1), zOffset * zOffsetAmount)
                .uv(xTexOffset * CONSTANT, (yTexOffset + height) * CONSTANT).endVertex();
        buffer.vertex(matrix, (float) (-half + size + (double) 0), (float) (height + (double) 0.1), zOffset * zOffsetAmount)
                .uv((xTexOffset + width) * CONSTANT, (yTexOffset + height) * CONSTANT).endVertex();
        buffer.vertex(matrix, (float) (-half + size + (double) 0), (float) (double) -0.1, zOffset * zOffsetAmount)
                .uv(((xTexOffset + width) * CONSTANT), yTexOffset * CONSTANT).endVertex();
        tesselator.end();

        /*if (ClientData.shadersLoaded) {
            if (alpha >= 0.96) RenderSystem.setShaderColor(0.2f, 0.4f, 0.4f, alpha);
            else RenderSystem.setShaderColor(0.35f, 0.4f, 0.4f, alpha);
        }
        else {
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        }*/
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        if (!background) {
            //Border
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(matrix, -half - 2.7f, -3, (zOffset+1)*zOffsetAmount) //top left
                    .uv(72*CONSTANT, 38*CONSTANT).endVertex();
            buffer.vertex(matrix, -half - 2.7f, -3 + 10, (zOffset+1)*zOffsetAmount) //bottom left
                    .uv(72*CONSTANT, 48*CONSTANT).endVertex();
            buffer.vertex(matrix, -half + MAX_BAR_WIDTH + 2.6f, -3 + 10, (zOffset+1)*zOffsetAmount) //bottom right
                    .uv(159*CONSTANT, 48*CONSTANT).endVertex();
            buffer.vertex(matrix, -half + MAX_BAR_WIDTH + 2.6f, -3, (zOffset+1)*zOffsetAmount) //top right
                    .uv(159*CONSTANT, 38*CONSTANT).endVertex();
            tesselator.end();

            //Dividers
            float dividerSpacing = MAX_BAR_WIDTH*percentOfMax;
            float f = -half + dividerSpacing;
            float dividerWidth = (float) Mth.clamp(dividerSpacing*0.1, 0.25f, 0.5f);
            while (f < (MAX_BAR_WIDTH - half)) {
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                buffer.vertex(matrix, f, 0, (zOffset + 1) * zOffsetAmount) //top left
                        .uv(74 * CONSTANT, 41 * CONSTANT).endVertex();
                buffer.vertex(matrix, f, 4, (zOffset + 1) * zOffsetAmount) //bottom left
                        .uv(74 * CONSTANT, 43 * CONSTANT).endVertex();
                buffer.vertex(matrix, f + dividerWidth, 4, (zOffset + 1) * zOffsetAmount) //bottom right
                        .uv(75 * CONSTANT, 43 * CONSTANT).endVertex();
                buffer.vertex(matrix, f + dividerWidth, 0, (zOffset + 1) * zOffsetAmount) //top right
                        .uv(75 * CONSTANT, 41 * CONSTANT).endVertex();
                tesselator.end();
                f += dividerSpacing;
            }
        }

        RenderSystem.disableBlend();

    }

}
