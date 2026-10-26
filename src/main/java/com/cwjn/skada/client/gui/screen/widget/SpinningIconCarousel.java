package com.cwjn.skada.client.gui.screen.widget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.cwjn.skada.util.UtilColour;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpinningIconCarousel {

  private static final float TAU = (float) (Math.PI * 2.0D);

  private final List<Entry> entries;
  private final Options options;
  private float currentRotation;
  private float targetRotation;
  private long lastRenderNanos = -1L;

  public SpinningIconCarousel(List<Entry> entries, Options options) {
    this.entries = List.copyOf(entries);
    this.options = options;
    this.currentRotation = options.startAngleRadians;
    this.targetRotation = options.startAngleRadians;
  }

  public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    if (entries.isEmpty()) {
      return;
    }

    advanceAnimation();
    List<RenderEntry> renderEntries = new ArrayList<>(entries.size());
    float rotationStep = getRotationStep();
    float baseRotation = currentRotation;
    RenderEntry selectedEntry = null;

    for (int i = 0; i < entries.size(); i++) {
      float angle = baseRotation + (rotationStep * i);
      float depth = (Mth.cos(angle) + 1.0F) * 0.5F;
      float warpedAngle = angle + (Mth.sin(angle) * options.frontSeparationRadians * depth * depth);
      depth = (Mth.cos(warpedAngle) + 1.0F) * 0.5F;
      float scale = Mth.lerp(depth * depth, options.minScale, options.maxScale);
      float x = options.centerX + Mth.sin(warpedAngle) * options.radius;
      float y = options.baseY - ((1.0F - depth) * options.verticalLift);
      RenderEntry renderEntry = new RenderEntry(entries.get(i), x, y, depth, scale);
      renderEntries.add(renderEntry);
      if (selectedEntry == null || renderEntry.depth() > selectedEntry.depth()) {
        selectedEntry = renderEntry;
      }
    }

    renderEntries.sort(Comparator.comparingDouble(RenderEntry::depth));
    for (RenderEntry renderEntry : renderEntries) {
      drawEntry(guiGraphics, renderEntry);
    }
  }

  public boolean isMouseOver(double mouseX, double mouseY) {
    int horizontalReach = options.radius + Math.round(options.radius * options.frontSeparationRadians)
      + Math.round((options.iconSize * options.maxScale) / 2.0F) + 8;
    int top = options.baseY - options.verticalLift - Math.round((options.iconSize * options.maxScale) / 2.0F) - 8;
    int bottom = options.baseY + options.valueYOffset + 20;
    return mouseX >= options.centerX - horizontalReach && mouseX <= options.centerX + horizontalReach
        && mouseY >= top && mouseY <= bottom;
  }

  public void rotateByEntries(int entryDelta) {
    if (entryDelta == 0 || entries.isEmpty()) {
      return;
    }

    targetRotation += entryDelta * getRotationStep();
  }

  private float getRotationStep() {
    return TAU / entries.size();
  }

  private void advanceAnimation() {
    long now = System.nanoTime();
    if (lastRenderNanos < 0L) {
      lastRenderNanos = now;
      return;
    }

    float elapsedSeconds = Math.min((now - lastRenderNanos) / 1_000_000_000.0F, 0.1F);
    lastRenderNanos = now;
    float remainingRotation = targetRotation - currentRotation;
    if (Math.abs(remainingRotation) <= 0.0001F) {
      currentRotation = targetRotation;
      return;
    }

    float stepRotation = elapsedSeconds * options.stepAnimationSpeedRadians;
    if (Math.abs(remainingRotation) <= stepRotation) {
      currentRotation = targetRotation;
      return;
    }

    currentRotation += Math.copySign(stepRotation, remainingRotation);
  }

  private void drawEntry(GuiGraphics guiGraphics, RenderEntry renderEntry) {
    guiGraphics.pose().pushPose();
    guiGraphics.pose().translate(renderEntry.x(), renderEntry.y(), (renderEntry.depth() * 25.0F));
    guiGraphics.pose().scale(renderEntry.scale(), renderEntry.scale(), 1.0F);
    RenderSystem.enableBlend();
    drawShadow(guiGraphics, renderEntry);
    float iconAlpha = 0.35F + (renderEntry.depth() * 0.65F);
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, iconAlpha);
    guiGraphics.blit(renderEntry.entry().icon(), -options.iconSize / 2, -options.iconSize / 2, 0, 0,
        options.iconSize, options.iconSize, options.iconSize, options.iconSize);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.disableBlend();
    guiGraphics.pose().popPose();
  }

  private void drawShadow(GuiGraphics guiGraphics, RenderEntry renderEntry) {
    int shadowWidth = Math.round(options.iconSize * (0.88F + (renderEntry.depth() * 0.22F)));
    int shadowTop = Math.round(options.iconSize * 0.34F);
    int shadowHeight = Math.max(2, Math.round(options.iconSize * 0.16F));
    int shadowAlpha = Math.round((42.0F) * (0.65F + (renderEntry.depth() * 0.35F)));
    guiGraphics.fill(-(shadowWidth / 2) - 4, shadowTop + 2, (shadowWidth / 2) + 4, shadowTop + shadowHeight + 2,
        withAlpha(0x000000, shadowAlpha / 4));
    guiGraphics.fill(-(shadowWidth / 2) - 2, shadowTop + 1, (shadowWidth / 2) + 2, shadowTop + shadowHeight + 1,
        withAlpha(0x000000, shadowAlpha / 2));
    guiGraphics.fill(-(shadowWidth / 2), shadowTop, shadowWidth / 2, shadowTop + shadowHeight,
        withAlpha(0x000000, shadowAlpha));
  }

  private static int withAlpha(int rgb, int alpha) {
    return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
  }

  public record Entry(ResourceLocation icon, int borderColour) {
  }

  private record RenderEntry(Entry entry, float x, float y, float depth, float scale) {
  }

  public static final class Options {

    private final int centerX;
    private final int baseY;
    private int radius = 38;
    private int verticalLift = 18;
    private int iconSize = 16;
    private int platePadding = 2;
    private int valueYOffset = 37;
    private float minScale = 0.72F;
    private float maxScale = 1.48F;
    private float stepAnimationSpeedRadians = 6.0F;
    private float startAngleRadians = 0.0F;
    private float frontSeparationRadians = 0.30F;

    private Options(int centerX, int baseY) {
      this.centerX = centerX;
      this.baseY = baseY;
    }

    public static Options defaults(int centerX, int baseY) {
      return new Options(centerX, baseY);
    }

    public Options radius(int radius) {
      this.radius = radius;
      return this;
    }

    public Options verticalLift(int verticalLift) {
      this.verticalLift = verticalLift;
      return this;
    }

    public Options iconSize(int iconSize) {
      this.iconSize = iconSize;
      return this;
    }

    public Options valueYOffset(int valueYOffset) {
      this.valueYOffset = valueYOffset;
      return this;
    }

    public Options minScale(float minScale) {
      this.minScale = minScale;
      return this;
    }

    public Options maxScale(float maxScale) {
      this.maxScale = maxScale;
      return this;
    }

    public Options stepAnimationSpeedRadians(float stepAnimationSpeedRadians) {
      this.stepAnimationSpeedRadians = stepAnimationSpeedRadians;
      return this;
    }

    public Options startAngleRadians(float startAngleRadians) {
      this.startAngleRadians = startAngleRadians;
      return this;
    }

    public Options frontSeparationRadians(float frontSeparationRadians) {
      this.frontSeparationRadians = frontSeparationRadians;
      return this;
    }
  }
}