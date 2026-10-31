package com.cwjn.skada.client.gui.tooltip;

import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilText;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/** Shared equipment stat columns, section rules and typography for both font settings. */
public abstract class EquipmentTooltipComponent implements ClientTooltipComponent {
  public static final int ACCENT = 0x79B9C6;
  protected static final int LABEL = 0xADB6BF;
  protected static final int VALUE = 0xF1F3F5;
  protected static final int NEGATIVE = 0xEE8585;
  protected static final int HINT = 0x8896A3;
  protected static final Style ICONS = Style.EMPTY.withFont(Util.rl("icons"));
  private static final int INSET = 3;
  private static final int COLUMN_GAP = 14;
  private static final int SECTION_HEIGHT = 7;
  private static final int BRANCH_INDENT = 12;
  private final List<Row> rows = new ArrayList<>();
  private final int accent;
  private int tooltipWidth;
  private boolean hintSection;

  /** A full-width line has an empty value; an empty label and value reserve a divider. */
  private record Row(Component label, Component value, int height, int indent) {}

  /** Captures the item's accent and separates equipment data from vanilla item text. */
  protected EquipmentTooltipComponent(ItemStack stack) {
    accent = accent(stack);
    section();
  }

  /** Matches explicit name colours or rarity colours, with cyan for ordinary white names. */
  public static int accent(ItemStack stack) {
    var colour = stack.getHoverName().getStyle().getColor();
    int rgb = colour == null ? stack.getRarity().color.getColor() : colour.getValue();
    return rgb == 0xFFFFFF ? ACCENT : rgb;
  }

  /** Formats a localized label without forcing the custom font when it is disabled. */
  protected static MutableComponent text(MutableComponent component, int colour) {
    return UtilText.pixelFontComponent(component)
        .withStyle(style -> component.getStyle().applyTo(style.withColor(colour)));
  }

  /** Adds a left-hand icon and label with a separately measured right-hand value. */
  protected void stat(String icon, MutableComponent label, MutableComponent value) {
    stat(icon, label, value, 0);
  }

  /** Nests an elemental damage row below its total, retaining the shared right-hand value column. */
  protected void branchStat(String icon, MutableComponent label, MutableComponent value) {
    stat(icon, label, value, BRANCH_INDENT);
  }

  /** Measures indentation as part of the label column so nested rows cannot overlap their values. */
  private void stat(String icon, MutableComponent label, MutableComponent value, int indent) {
    Component left = Component.empty()
        .append(Component.translatable(icon).withStyle(ICONS))
        .append(text(Component.literal(" ").append(label), LABEL));
    rows.add(new Row(left, text(value, VALUE), Minecraft.getInstance().font.lineHeight + 2, indent));
  }

  /** Adds a full-width line, preserving its own colours and font. */
  protected void line(Component component) {
    rows.add(new Row(component, Component.empty(), Minecraft.getInstance().font.lineHeight + 2, 0));
  }

  /** Inserts one small divider gap, coalescing adjacent section boundaries. */
  protected void section() {
    if (rows.isEmpty() || !rows.get(rows.size() - 1).label().getString().isEmpty()) {
      rows.add(new Row(Component.empty(), Component.empty(), SECTION_HEIGHT, 0));
    }
  }

  /** Appends expanded attributes before the final keyboard-hint section. */
  protected void attributeDetails(ItemStack stack, Multimap<Attribute, AttributeModifier> main,
      List<EquipmentSlot> otherSlots) {
    if (Screen.hasShiftDown() && !main.isEmpty()) {
      section();
      UtilText.otherAttributesComponent(main).forEach(this::attributeLine);
    }
    if (Screen.hasAltDown()) {
      for (EquipmentSlot slot : otherSlots) {
        section();
        // The shared formatter starts with an empty line; the section already supplies that gap.
        UtilText.getOtherSlotAttributesAsList(slot, stack.getAttributeModifiers(slot)).stream()
            .filter(component -> !component.getString().isEmpty()).forEach(this::attributeLine);
      }
    }
  }

  /** Keeps vanilla attribute glyphs while lifting dark positive-modifier blue against the panel. */
  private void attributeLine(Component component) {
    var colour = component.getStyle().getColor();
    line(colour != null && colour.getValue() == ChatFormatting.BLUE.getColor()
        ? component.copy().withStyle(style -> style.withColor(0x91B6E8)) : component);
  }

  /** Adds a localized, subdued input hint; bindings are resolved by the caller. */
  protected void hint(String key, Object... arguments) {
    if (!hintSection) {
      section();
      hintSection = true;
    }
    line(text(Component.translatable(key, arguments), HINT));
  }

  /** Uses the full measured tooltip width so columns and rules also respect long item names. */
  public void setTooltipWidth(int width) {
    tooltipWidth = width;
  }

  /** Includes every row and divider in vanilla's tooltip positioning calculation. */
  @Override
  public int getHeight() {
    return rows.stream().mapToInt(Row::height).sum();
  }

  /** Measures label and value columns independently, avoiding overlap between different rows. */
  @Override
  public int getWidth(Font font) {
    int labelWidth = 0;
    int valueWidth = 0;
    int lineWidth = 0;
    for (Row row : rows) {
      if (row.value().getString().isEmpty()) {
        lineWidth = Math.max(lineWidth, font.width(row.label()));
      } else {
        labelWidth = Math.max(labelWidth, row.indent() + font.width(row.label()));
        valueWidth = Math.max(valueWidth, font.width(row.value()));
      }
    }
    int columns = valueWidth == 0 ? labelWidth : labelWidth + COLUMN_GAP + valueWidth;
    return Math.max(tooltipWidth, Math.max(lineWidth, columns) + INSET * 2);
  }

  /** Draws compact rows with values aligned to the shared right edge. */
  @Override
  public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffers) {
    int right = x + getWidth(font) - INSET;
    for (Row row : rows) {
      font.drawInBatch(row.label(), x + INSET + row.indent(), y, VALUE, false, matrix, buffers,
          Font.DisplayMode.NORMAL, 0, 15728880);
      font.drawInBatch(row.value(), right - font.width(row.value()), y, VALUE, false, matrix, buffers,
          Font.DisplayMode.NORMAL, 0, 15728880);
      y += row.height();
    }
  }

  /** Draws section rules and quiet branches connecting nested damage rows to their total. */
  @Override
  public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
    for (int i = 0; i < rows.size(); i++) {
      Row row = rows.get(i);
      if (row.label().getString().isEmpty() && row.value().getString().isEmpty()) {
        drawDivider(graphics, x + INSET, x + getWidth(font) - INSET, y + 2, accent);
      }
      if (row.indent() > 0) {
        int trunkX = x + INSET + 3;
        int branchY = y + font.lineHeight / 2;
        boolean first = i == 0 || rows.get(i - 1).indent() == 0;
        boolean last = i + 1 == rows.size() || rows.get(i + 1).indent() == 0;
        // Continue the trunk between children, ending level with the final child's icon.
        graphics.fill(trunkX, first ? y - 2 : y, trunkX + 1,
            last ? branchY + 1 : y + row.height(), 0x608896A3);
        graphics.fill(trunkX + 1, branchY, x + INSET + row.indent() - 3, branchY + 1, 0x608896A3);
      }
      y += row.height();
    }
  }

  /** Draws a one-pixel rule whose accent fades from a quiet centre to transparent ends. */
  protected static void drawDivider(GuiGraphics graphics, int left, int right, int y, int colour) {
    if (right <= left) return;
    VertexConsumer vertices = graphics.bufferSource().getBuffer(RenderType.gui());
    Matrix4f matrix = graphics.pose().last().pose();
    float middle = (left + right) / 2.0F;
    int red = colour >> 16 & 0xFF;
    int green = colour >> 8 & 0xFF;
    int blue = colour & 0xFF;
    for (int side = 0; side < 2; side++) {
      float start = side == 0 ? left : middle;
      float end = side == 0 ? middle : right;
      int startAlpha = side == 0 ? 0 : 0x48;
      int endAlpha = side == 0 ? 0x48 : 0;
      vertices.vertex(matrix, start, y, 0).color(red, green, blue, startAlpha).endVertex();
      vertices.vertex(matrix, start, y + 1, 0).color(red, green, blue, startAlpha).endVertex();
      vertices.vertex(matrix, end, y + 1, 0).color(red, green, blue, endAlpha).endVertex();
      vertices.vertex(matrix, end, y, 0).color(red, green, blue, endAlpha).endVertex();
    }
  }
}
