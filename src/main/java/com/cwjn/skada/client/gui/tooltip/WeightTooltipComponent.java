package com.cwjn.skada.client.gui.tooltip;

import com.cwjn.skada.data.damage.AttackSelection;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.util.EquipmentWeightUtil;
import com.cwjn.skada.util.Keybinds;
import com.cwjn.skada.util.Util;
import com.mojang.blaze3d.platform.InputConstants;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.settings.KeyModifier;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/** Shared footer with stationary attack icons, one compact shortcut cue and right-aligned item mass. */
public class WeightTooltipComponent implements TooltipComponent, ClientTooltipComponent {

  private static final int ICON_SLOT = 16;
  private static final int ARROW_SPACE = 4;
  private static final int SHORTCUT_GAP = 5;
  private static final int KEYCAP_HEIGHT = 12;
  private static final int SCROLL_CUE_WIDTH = 26;
  private static final int COLUMN_GAP = 12;
  // Optical adjustments from the shared centre for the bitmap artwork and text.
  private static final int WEIGHT_Y_OFFSET = 1;
  private static final int ATTACK_Y_OFFSET = 2;
  private static final int ARROW_Y_OFFSET = 1;
  private static final AttackSelectionAnimation ANIMATION = new AttackSelectionAnimation();
  private final ItemStack stack;
  private final Component text;
  private final int accent;
  private final List<Component> attackIcons = new ArrayList<>();
  private final List<String> attackOrder;
  private final boolean canCycle;
  private final boolean scrollShortcut;
  private final Component shortcutKey;
  private final Component singleAttackName;
  private final int selectedAttack;
  private int tooltipWidth;

  /** Captures mass and the supported attack icons in their fixed authored order. */
  public WeightTooltipComponent(ItemStack stack) {
    this.stack = stack;
    accent = EquipmentTooltipComponent.accent(stack);
    text = Component.empty()
        .append(Component.translatable("skada.icon.weight").withStyle(Style.EMPTY.withFont(Util.rl("icons"))))
        .append(EquipmentTooltipComponent.text(Component.translatable("skada.tooltip.info.weight_compact",
            new DecimalFormat("#.#").format(EquipmentWeightUtil.getStackWeightKg(stack))), EquipmentTooltipComponent.VALUE));
    if (stack.hasTag() && stack.getTag().contains(SkadaData.WEAPON_INFO_TAG_KEY)) {
      AttackSelection selected = AttackSelection.resolve(stack);
      int count = selected.order().size();
      canCycle = count > 1;
      selectedAttack = selected.index();
      attackOrder = selected.order().stream().map(type -> type.name()).toList();
      for (String type : attackOrder) {
        attackIcons.add(Component.translatable("skada.icon.attack_type." + type)
            .withStyle(EquipmentTooltipComponent.ICONS));
      }
    } else {
      canCycle = false;
      selectedAttack = 0;
      attackOrder = List.of();
    }
    if (attackOrder.size() == 1) {
      String type = attackOrder.get(0);
      String fallback = type.substring(0, 1).toUpperCase(Locale.ROOT) + type.substring(1).replace('_', ' ');
      singleAttackName = EquipmentTooltipComponent.text(
          Component.translatableWithFallback("skada.attack_type." + type, fallback), EquipmentTooltipComponent.LABEL);
    } else {
      singleAttackName = Component.empty();
    }
    KeyMapping binding = Keybinds.attackScrollModifier;
    scrollShortcut = canCycle && binding != null && !binding.isUnbound();
    if (!scrollShortcut) binding = Keybinds.cycleAttackType;
    shortcutKey = canCycle && binding != null && !binding.isUnbound() ? compactKey(binding) : Component.empty();
  }

  /** Uses familiar modifier legends, retaining the complete translated label for other bindings and chords. */
  private static Component compactKey(KeyMapping binding) {
    if (binding.getKey().getType() == InputConstants.Type.KEYSYM && binding.getKeyModifier() == KeyModifier.NONE) {
      String modifier = switch (binding.getKey().getValue()) {
        case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> "alt";
        case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "ctrl";
        case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> "shift";
        case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> "super";
        default -> null;
      };
      if (modifier != null) return Component.translatable("skada.tooltip.key." + modifier);
    }
    return binding.getTranslatedKeyMessage();
  }

  /** Sets the full tooltip width after all text has been wrapped and measured. */
  public void setTooltipWidth(int width) {
    tooltipWidth = width;
  }

  /** Reserves the divider gap and room for the selection marker beneath the centred text. */
  @Override
  public int getHeight() {
    Font font = Minecraft.getInstance().font;
    return 7 + (rowHeight(font) - font.lineHeight) / 2 + font.lineHeight + 2;
  }

  /** Keeps the footer wide enough for its contents and the enclosing tooltip. */
  @Override
  public int getWidth(Font font) {
    int selector = attackIcons.isEmpty() ? 0 : selectorWidth(font) + COLUMN_GAP;
    return Math.max(tooltipWidth, selector + font.width(text) + 6);
  }

  /** Reserves a permanent slot per attack and measures the single displayed shortcut beside them. */
  private int selectorWidth(Font font) {
    if (attackIcons.size() == 1) {
      return singleAttackIconX(0) + font.width(attackIcons.get(0)) + 4 + font.width(singleAttackName) - 3;
    }
    int shortcut = shortcutKey.getString().isEmpty() ? 0
        : SHORTCUT_GAP + keycapWidth(font) + (scrollShortcut ? SCROLL_CUE_WIDTH : 0);
    return attackIcons.size() * ICON_SLOT + (canCycle ? ARROW_SPACE * 2 : 0) + shortcut;
  }

  /** Allows remapped keys and translated labels to fit without colliding with the mass column. */
  private int keycapWidth(Font font) {
    return Math.max(12, font.width(shortcutKey) + 6);
  }

  /** Keeps armour's compact mass row while giving weapon icons their own breathing room. */
  private int rowHeight(Font font) {
    return attackIcons.isEmpty() ? font.lineHeight : Math.max(font.lineHeight, ICON_SLOT);
  }

  /** Centres each visible element between the divider's lower edge and the tooltip's bottom frame. */
  private int centeredTop(int y, int height) {
    return y + 3 + (getHeight() - 3 - height) / 2;
  }

  /** Places the lone icon one pixel right of its equal-padding position. */
  private int singleAttackIconX(int x) {
    return x - 1 + (getHeight() - 3 - 8) / 2;
  }

  /** Draws the icons in fixed positions, highlighting the selected type immediately alongside its stats. */
  @Override
  public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffers) {
    // The weight glyph begins one pixel above the numeric text's origin.
    int weightTop = centeredTop(y, font.lineHeight + 1) + 1 + WEIGHT_Y_OFFSET;
    font.drawInBatch(text, x + getWidth(font) - 3 - font.width(text), weightTop, 0xF1F3F5, false,
        matrix, buffers, Font.DisplayMode.NORMAL, 0x555555, 15728880);
    // Centre the eight-pixel bitmap and its underline together, including the bitmap's ascent offset.
    int top = canCycle ? centeredTop(y, 11) + 1 + ATTACK_Y_OFFSET : centeredTop(y, 8) + 1;
    int start = x + 3 + (canCycle ? ARROW_SPACE : 0);
    for (int i = 0; i < attackIcons.size(); i++) {
      Component icon = attackIcons.get(i);
      int iconX = start + i * ICON_SLOT + (ICON_SLOT - font.width(icon)) / 2;
      if (!canCycle) iconX = singleAttackIconX(x);
      font.drawInBatch(icon, iconX, top, i == selectedAttack ? 0xFFFFFFFF : 0x66FFFFFF, false,
          matrix, buffers, Font.DisplayMode.NORMAL, 0, 15728880);
      if (!canCycle) {
        font.drawInBatch(singleAttackName, iconX + font.width(icon) + 4, top - 1, 0xFFFFFFFF, false,
            matrix, buffers, Font.DisplayMode.NORMAL, 0, 15728880);
      }
    }
  }

  /** Uses the hovered slot as a stable owner even if a creative/server update replaces its stack instance. */
  private Object animationOwner() {
    if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
        && screen.getSlotUnderMouse() != null && screen.getSlotUnderMouse().getItem() == stack) {
      return screen.getSlotUnderMouse();
    }
    return stack;
  }

  /** Frames stationary attack icons with compact chevrons and centres the shortcut below the divider. */
  @Override
  public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
    EquipmentTooltipComponent.drawDivider(graphics, x + 3, x + getWidth(font) - 3, y + 2, accent);
    if (!canCycle) return;
    int start = x + 3 + (canCycle ? ARROW_SPACE : 0);
    double position = canCycle ? ANIMATION.position(animationOwner(), stack.getItem(), attackOrder,
        selectedAttack, System.nanoTime()) : selectedAttack;
    int selectedX = start + (int) Math.round(position * ICON_SLOT);
    int top = centeredTop(y, 11) + 1 + ATTACK_Y_OFFSET;
    // Icon glyphs sit one pixel above the text origin because their bitmap-font ascent is eight.
    graphics.fill(selectedX + 3, top + 9, selectedX + ICON_SLOT - 5, top + 10, 0xC0000000 | accent);
    if (canCycle) {
      int arrowCentreY = centeredTop(y, 5) + 2 + ARROW_Y_OFFSET;
      drawArrow(graphics, x + 3, arrowCentreY, true);
      drawArrow(graphics, start + attackIcons.size() * ICON_SLOT + 1, arrowCentreY, false);
    }
    if (!shortcutKey.getString().isEmpty()) {
      int shortcutY = centeredTop(y, KEYCAP_HEIGHT);
      drawShortcut(font, graphics, start + attackIcons.size() * ICON_SLOT + ARROW_SPACE + SHORTCUT_GAP, shortcutY);
    }
  }

  /** Fits a three-pixel chevron into a narrow reserved gutter beside the attack icons. */
  private void drawArrow(GuiGraphics graphics, int x, int centreY, boolean left) {
    for (int i = 0; i < 3; i++) {
      int pixelX = x + (left ? i : 2 - i);
      graphics.fill(pixelX, centreY - i, pixelX + 1, centreY - i + 1, 0x99000000 | accent);
      if (i > 0) graphics.fill(pixelX, centreY + i, pixelX + 1, centreY + i + 1, 0x99000000 | accent);
    }
  }

  /** Draws a restrained keycap and a mouse with a highlighted wheel, avoiding duplicate shortcut labels. */
  private void drawShortcut(Font font, GuiGraphics graphics, int x, int y) {
    int width = keycapWidth(font);
    graphics.fill(x, y, x + width, y + KEYCAP_HEIGHT, 0xFF202730);
    graphics.renderOutline(x, y, width, KEYCAP_HEIGHT, 0xFF667582);
    graphics.fill(x + 1, y + KEYCAP_HEIGHT - 2, x + width - 1, y + KEYCAP_HEIGHT - 1, 0xFF12171D);
    graphics.drawString(font, shortcutKey, x + (width - font.width(shortcutKey)) / 2,
        y + (KEYCAP_HEIGHT - font.lineHeight) / 2 + 1, 0xADB6BF, false);
    if (!scrollShortcut) return;
    graphics.drawString(font, "+", x + width + 3, y + (KEYCAP_HEIGHT - font.lineHeight) / 2 + 1, 0x8896A3, false);
    int mouseX = x + width + 11;
    int outline = 0xFF8896A3;
    graphics.fill(mouseX + 2, y, mouseX + 8, y + 1, outline);
    graphics.fill(mouseX + 1, y + 1, mouseX + 2, y + 2, outline);
    graphics.fill(mouseX + 8, y + 1, mouseX + 9, y + 2, outline);
    graphics.fill(mouseX, y + 2, mouseX + 1, y + 10, outline);
    graphics.fill(mouseX + 9, y + 2, mouseX + 10, y + 10, outline);
    graphics.fill(mouseX + 1, y + 10, mouseX + 2, y + 11, outline);
    graphics.fill(mouseX + 8, y + 10, mouseX + 9, y + 11, outline);
    graphics.fill(mouseX + 2, y + 11, mouseX + 8, y + 12, outline);
    graphics.fill(mouseX + 4, y + 2, mouseX + 6, y + 6, 0xFF000000 | accent);
    graphics.fill(mouseX + 13, y + 1, mouseX + 14, y + 2, outline);
    graphics.fill(mouseX + 12, y + 2, mouseX + 15, y + 3, outline);
    graphics.fill(mouseX + 12, y + 9, mouseX + 15, y + 10, outline);
    graphics.fill(mouseX + 13, y + 10, mouseX + 14, y + 11, outline);
  }
}
