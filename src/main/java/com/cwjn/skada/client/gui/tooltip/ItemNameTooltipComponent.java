package com.cwjn.skada.client.gui.tooltip;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.joml.Matrix4f;

/** Centres the equipment name in a padded header, retaining its font, colour and wrapping. */
public class ItemNameTooltipComponent implements TooltipComponent, ClientTooltipComponent {
  private static final int PADDING = 7;
  private static final int BORDER_PADDING = 3;
  private static final int INSET = PADDING - BORDER_PADDING;
  private final Component name;
  private final int maxWidth;
  private final int followingGap;
  private List<FormattedCharSequence> lines;
  private int lineHeight;
  private int tooltipWidth;

  /** Accounts for vanilla's first-component gap and the following equipment section rule. */
  public ItemNameTooltipComponent(Component name, int maxWidth, boolean dividerFollows) {
    this.name = name;
    this.maxWidth = maxWidth;
    followingGap = 2 + (dividerFollows ? 2 : 0);
    getWidth(Minecraft.getInstance().font);
  }

  /** Matches the width of the complete tooltip after all components have been measured. */
  public void setTooltipWidth(int width) {
    tooltipWidth = width;
  }

  /** Balances padding against the top border and the following divider or vanilla content. */
  @Override
  public int getHeight() {
    return lines.size() * lineHeight + PADDING * 2 - BORDER_PADDING - followingGap;
  }

  /** Wraps unusually long names while reserving equal minimum padding at the left and right. */
  @Override
  public int getWidth(Font font) {
    lines = font.split(name, Math.max(1, maxWidth - INSET * 2));
    if (lines.isEmpty()) lines = List.of(name.getVisualOrderText());
    lineHeight = font.lineHeight;
    int textWidth = lines.stream().mapToInt(font::width).max().orElse(0);
    return Math.max(tooltipWidth, textWidth + INSET * 2);
  }

  /** Centres each name line without changing the item's existing glyphs or styling. */
  @Override
  public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffers) {
    int width = getWidth(font);
    int top = y + INSET;
    for (FormattedCharSequence line : lines) {
      font.drawInBatch(line, x + (width - font.width(line)) / 2, top, 0xFFFFFF, false,
          matrix, buffers, Font.DisplayMode.NORMAL, 0, 15728880);
      top += lineHeight;
    }
  }
}
