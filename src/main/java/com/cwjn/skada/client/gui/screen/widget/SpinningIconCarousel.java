package com.cwjn.skada.client.gui.screen.widget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.cwjn.skada.util.UtilColour;
import com.cwjn.skada.util.UtilText;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpinningIconCarousel {

  private static final float TAU = (float) (Math.PI * 2.0D);

  private final List<Entry> entries;
  private final Options options;
  private float autoRotation;
  private float manualRotation;
  private long lastRenderNanos = -1L;

  public SpinningIconCarousel(List<Entry> entries, Options options) {
    this.entries = List.copyOf(entries);
    this.options = options;
    this.autoRotation = options.startAngleRadians;
  }

  public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    if (entries.isEmpty()) {
      return;
    }

    advanceAnimation();
    boolean hovered = isMouseOver(mouseX, mouseY);
    List<RenderEntry> renderEntries = new ArrayList<>(entries.size());
    float rotationStep = TAU / entries.size();
    float baseRotation = autoRotation + manualRotation;
    RenderEntry selectedEntry = null;

    for (int i = 0; i < entries.size(); i++) {
      float angle = baseRotation + (rotationStep * i);
      float depth = (Mth.cos(angle) + 1.0F) * 0.5F;
      float scale = Mth.lerp(depth * depth, options.minScale, options.maxScale);
      float x = options.centerX + Mth.sin(angle) * options.radius;
      float y = options.baseY - ((1.0F - depth) * options.verticalLift);
      RenderEntry renderEntry = new RenderEntry(entries.get(i), x, y, depth, scale);
      renderEntries.add(renderEntry);
      if (selectedEntry == null || renderEntry.depth() > selectedEntry.depth()) {
        selectedEntry = renderEntry;
      }
    }

    renderEntries.sort(Comparator.comparingDouble(RenderEntry::depth));
    for (RenderEntry renderEntry : renderEntries) {
      drawEntry(guiGraphics, renderEntry, hovered && renderEntry == selectedEntry);
    }

    if (selectedEntry != null) {
      drawSelectedText(guiGraphics, selectedEntry);
    }
  }

  public boolean isMouseOver(double mouseX, double mouseY) {
    int horizontalReach = options.radius + Math.round((options.iconSize * options.maxScale) / 2.0F) + 8;
    int top = options.baseY - options.verticalLift - Math.round((options.iconSize * options.maxScale) / 2.0F) - 8;
    int bottom = options.baseY + options.valueYOffset + 20;
    return mouseX >= options.centerX - horizontalReach && mouseX <= options.centerX + horizontalReach
        && mouseY >= top && mouseY <= bottom;
  }

  public void nudgeRotation(float scrollAmount) {
    manualRotation += scrollAmount * options.scrollStepRadians;
  }

  private void advanceAnimation() {
    long now = System.nanoTime();
    if (lastRenderNanos < 0L) {
      lastRenderNanos = now;
      return;
    }

    float elapsedSeconds = Math.min((now - lastRenderNanos) / 1_000_000_000.0F, 0.1F);
    lastRenderNanos = now;
    autoRotation += elapsedSeconds * options.spinSpeedRadians * options.spinDirection;
  }

  private void drawEntry(GuiGraphics guiGraphics, RenderEntry renderEntry, boolean highlightSelected) {
    int halfSize = Math.round((options.iconSize * renderEntry.scale()) / 2.0F);
    int padding = options.platePadding;
    int left = Math.round(renderEntry.x()) - halfSize - padding;
    int top = Math.round(renderEntry.y()) - halfSize - padding;
    int right = Math.round(renderEntry.x()) + halfSize + padding;
    int bottom = Math.round(renderEntry.y()) + halfSize + padding;
    int alpha = 70 + Math.round(renderEntry.depth() * 90.0F);
    int fillColour = withAlpha(UtilColour.UI_BACKGROUND_COLOUR, alpha);
    int borderColour = highlightSelected
        ? withAlpha(renderEntry.entry().colour().getAsInt(), 255)
        : withAlpha(UtilColour.UI_BORDER_COLOUR, 110 + Math.round(renderEntry.depth() * 70.0F));

    guiGraphics.fill(left - 1, top - 1, right + 1, bottom + 1, borderColour);
    guiGraphics.fill(left, top, right, bottom, fillColour);

    guiGraphics.pose().pushPose();
    guiGraphics.pose().translate(renderEntry.x(), renderEntry.y(), 200.0F + (renderEntry.depth() * 25.0F));
    guiGraphics.pose().scale(renderEntry.scale(), renderEntry.scale(), 1.0F);
    guiGraphics.blit(renderEntry.entry().icon(), -options.iconSize / 2, -options.iconSize / 2, 0, 0,
        options.iconSize, options.iconSize, options.iconSize, options.iconSize);
    guiGraphics.pose().popPose();
  }

  private void drawSelectedText(GuiGraphics guiGraphics, RenderEntry selectedEntry) {
    Font font = Minecraft.getInstance().font;
    MutableComponent label = UtilText.pixelFontComponent(selectedEntry.entry().label().get(), false, false, true);
    MutableComponent value = UtilText.pixelFontComponent(selectedEntry.entry().value().get(), false, true, false);
    drawCenteredString(guiGraphics, font, label, options.centerX, options.baseY + options.labelYOffset, UtilColour.UI_TEXT_COLOUR);
    drawCenteredString(guiGraphics, font, value, options.centerX, options.baseY + options.valueYOffset,
        selectedEntry.entry().colour().getAsInt());
  }

  private void drawCenteredString(GuiGraphics guiGraphics, Font font, MutableComponent component, int centerX, int y, int colour) {
    guiGraphics.drawString(font, component, centerX - font.width(component.getVisualOrderText()) / 2, y, colour, false);
  }

  private static int withAlpha(int rgb, int alpha) {
    return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
  }

  public record Entry(Supplier<MutableComponent> label, ResourceLocation icon, IntSupplier colour,
      Supplier<MutableComponent> value) {
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
    private int labelYOffset = 22;
    private int valueYOffset = 37;
    private float minScale = 0.72F;
    private float maxScale = 1.48F;
    private float spinSpeedRadians = 0.85F;
    private float spinDirection = 1.0F;
    private float startAngleRadians = 0.0F;
    private float scrollStepRadians = 0.45F;

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

    public Options labelYOffset(int labelYOffset) {
      this.labelYOffset = labelYOffset;
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

    public Options spinSpeedRadians(float spinSpeedRadians) {
      this.spinSpeedRadians = spinSpeedRadians;
      return this;
    }

    public Options spinDirection(float spinDirection) {
      this.spinDirection = spinDirection;
      return this;
    }

    public Options startAngleRadians(float startAngleRadians) {
      this.startAngleRadians = startAngleRadians;
      return this;
    }

    public Options scrollStepRadians(float scrollStepRadians) {
      this.scrollStepRadians = scrollStepRadians;
      return this;
    }
  }
}