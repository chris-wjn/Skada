package com.cwjn.skada.util;

import static net.minecraft.world.item.ItemStack.ATTRIBUTE_MODIFIER_FORMAT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import com.cwjn.skada.ClientConfig;
import com.cwjn.skada.data.SkadaData;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Utility class for text-related functions, such as creating pixel font
 * components and formatting attribute modifiers for tooltips.
 * Class is not entirely clientsided. Some methods are marked as client-only.
 */
public abstract class UtilText {
  private static final float COMPASS9_GLYPH_HEIGHT = 18F;

  /**
   * Creates a pixel font component with the given string. Only one of usePixel16
   * or usePixel12 should be true,
   * but usePixel16 is used if both are true.
   */
  @SuppressWarnings("null")
  @OnlyIn(Dist.CLIENT)
  public static MutableComponent pixelFontComponent(String s, boolean useBoldNumbers, boolean usePixel16,
      boolean usePixel12) {
    if ((Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
        || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv"))
        && ClientConfig.USE_MDU_FONT.get()) {
      @SuppressWarnings("null")
      MutableComponent retComp = Component.empty()
          .withStyle(usePixel16 ? UtilText.PIXEL_16 : usePixel12 ? UtilText.PIXEL_12 : UtilText.PIXEL);
      if (useBoldNumbers)
        s = s.replace("0", "ᙐ").replace("1", "ᙑ").replace("2", "ᙒ").replace("3", "ᙓ").replace("4", "ᙔ")
            .replace("5", "ᙕ").replace("6", "ᙖ").replace("7", "ᙗ").replace("8", "ᙘ").replace("9", "ᙙ").replace('.', '_')
            .replace('(', '<').replace(')', '>');
      Style currentFormatting = Style.EMPTY;
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c == '§' && i < s.length() - 1) {
          if (s.charAt(i + 1) == 'r') {
            currentFormatting = Style.EMPTY;
          } else {
            ChatFormatting cf = ChatFormatting.getByCode(s.charAt(i + 1));
            if (cf != null)
              currentFormatting = currentFormatting.applyFormat(cf);
          }
          i++;
          continue;
        } else {
          retComp.append(Component.literal(String.valueOf(c)).withStyle(currentFormatting));
          if (c == ' ') {
            retComp.append(UtilText.spacer(1));
            continue;
          }
        }
        retComp.append(UtilText.spacer(-1));
        if (useBoldNumbers && c == '_')
          retComp.append(UtilText.spacer(1));
      }
      return retComp;
    } else {
      return getComponentWithFormatting(Component.literal(s));
    }
  }

  /**
   * Creates a pixel font component with the given MutableComponent. Only one of
   * usePixel16 or usePixel12 should be true,
   * but usePixel16 is used if both are true.
   */
  @OnlyIn(Dist.CLIENT)
  public static MutableComponent pixelFontComponent(MutableComponent comp) {
    return ClientConfig.USE_MDU_FONT.get() ? UtilText.pixelFontComponent(comp, false, false, false) : getComponentWithFormatting(comp);
  }

  /**
   * Creates a pixel font component with the given string. Only one of usePixel16
   * or usePixel12 should be true,
   * but usePixel16 is used if both are true.
   */
  @OnlyIn(Dist.CLIENT)
  public static MutableComponent pixelFontComponent(String s) {
    return pixelFontComponent(s, false, false, false);
  }

  /**
   * Creates a pixel font component with the given MutableComponent. Only one of
   * usePixel16 or usePixel12 should be true,
   * but usePixel16 is used if both are true.
   */
  @SuppressWarnings("null")
  @OnlyIn(Dist.CLIENT)
  @Nonnull
  public static MutableComponent pixelFontComponent(MutableComponent comp, boolean useBoldNumbers, boolean usePixel16,
      boolean usePixel12) {
    if ((Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
        || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv"))
        && ClientConfig.USE_MDU_FONT.get()) {
      @SuppressWarnings("null")
      MutableComponent retComp = Component.empty()
          .withStyle(usePixel16 ? UtilText.PIXEL_16 : usePixel12 ? UtilText.PIXEL_12 : UtilText.PIXEL);
      @SuppressWarnings("null")
      String s = I18n.exists(comp.getString()) ? I18n.get(comp.getString()) : comp.getString();
      if (useBoldNumbers)
        s = s.replace("0", "ᙐ").replace("1", "ᙑ").replace("2", "ᙒ").replace("3", "ᙓ").replace("4", "ᙔ")
            .replace("5", "ᙕ").replace("6", "ᙖ").replace("7", "ᙗ").replace("8", "ᙘ").replace("9", "ᙙ").replace('.', '_')
            .replace('(', '<').replace(')', '>');
      Style currentFormatting = Style.EMPTY;
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c == '§' && i < s.length() - 1) {
          if (s.charAt(i + 1) == 'r') {
            currentFormatting = Style.EMPTY;
          } else {
            ChatFormatting cf = ChatFormatting.getByCode(s.charAt(i + 1));
            if (cf != null)
              currentFormatting = currentFormatting.applyFormat(cf);
          }
          i++;
          continue;
        } else {
          retComp.append(Component.literal(String.valueOf(c)).withStyle(currentFormatting));
          if (c == ' ') {
            retComp.append(UtilText.spacer(1));
            continue;
          }
        }
        retComp.append(UtilText.spacer(-1));
        if (useBoldNumbers && c == '_')
          retComp.append(UtilText.spacer(1));
      }
      return retComp;
    } else {
      return getComponentWithFormatting(comp);
    }
  }

  /*
   * Manually apply formatting for when the custom font is disabled.
   */
  public static MutableComponent getComponentWithFormatting(MutableComponent comp) {
    String s = I18n.exists(comp.getString()) ? I18n.get(comp.getString()) : comp.getString();
    MutableComponent retComp = Component.empty();
    Style currentFormatting = Style.EMPTY;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '§' && i < s.length() - 1) {
        if (s.charAt(i + 1) == 'r') {
          currentFormatting = Style.EMPTY;
        } else {
          ChatFormatting cf = ChatFormatting.getByCode(s.charAt(i + 1));
          if (cf != null)
            currentFormatting = currentFormatting.applyFormat(cf);
        }
        i++;
        continue;
      } else {
        retComp.append(Component.literal(String.valueOf(c)).withStyle(currentFormatting));
      }
    }
    return retComp;
  }

  @SuppressWarnings("null")
  @OnlyIn(Dist.CLIENT)
  public static MutableComponent pixelFontComponent(Component... comp) {
    if (Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
        || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv")) {
      MutableComponent retComp = Component.empty();
      for (Component c : comp) {
        @SuppressWarnings("null")
        Style style = c.getStyle().applyTo(UtilText.PIXEL);
        @SuppressWarnings("null")
        String s = I18n.get(c.getString());
        for (int i = 0; i < s.length(); i++) {
          char ch = s.charAt(i);
          if (ch == '§') {
            i++;
            continue;
          } else {
            retComp.append(Component.literal(String.valueOf(ch)).withStyle(style));
            if (ch == ' ') {
              retComp.append(UtilText.spacer(1));
              continue;
            }
          }
          retComp.append(UtilText.spacer(-1));
        }
      }
      return retComp;
    } else {
      MutableComponent retComp = Component.empty();
      for (Component c : comp) {
        retComp.append(c);
      }
      return retComp;
    }
  }

  public static MutableComponent compassNineFontComponent(MutableComponent comp) {
    return comp.withStyle(Style.EMPTY.withFont(Util.rl("compass9_ttf")));
  }

  public static MutableComponent compassNineFontComponent(FormattedCharSequence comp) {
    StringBuilder builder = new StringBuilder();
    comp.accept((index, style, codePoint) -> {
      builder.appendCodePoint(codePoint);
      return true;
    });
    return Component.literal(builder.toString()).withStyle(Style.EMPTY.withFont(Util.rl("compass9_ttf")));
  }

  /**
   * Returns the actual rendered height of compass9 text after scaling.
   * The visible glyph height is constant for this font, so this uses a fixed
   * measured value instead of recomputing font bounds at runtime.
   *
   * @param scale the render scale that will be applied
   * @return the scaled ink height in pixels
   */
  @OnlyIn(Dist.CLIENT)
  public static int compass9ScaledHeight(float scale) {
    return Math.round(scale * COMPASS9_GLYPH_HEIGHT);
  }

  /**
   * Returns the width a component will occupy after it is rendered at the given
   * scale.
   *
   * @param font  the font used for rendering
   * @param text  the text to measure
   * @param scale the render scale that will be applied
   * @return the scaled width in pixels
   */
  @OnlyIn(Dist.CLIENT)
  public static int scaledWidth(Font font, Component text, float scale) {
    return Math.round(font.width(text.getVisualOrderText()) * scale);
  }

  /**
   * Returns the scale factor needed to make compass9 text a desired pixel height.
   *
   * @param desiredPixelHeight
   * @return the scale factor to apply to compass9 text to make it the desired
   *         pixel height
   */
  public static float getScaleForDesiredPixelHeightCompass9(int desiredPixelHeight) {
    return desiredPixelHeight / COMPASS9_GLYPH_HEIGHT;
  }

  /**
   * Draws text while keeping the supplied top-left anchor fixed as the pose is
   * scaled.
   *
   * @param graphics the graphics context used for rendering
   * @param font     the font used for the text
   * @param text     the text to render
   * @param x        the anchored x position
   * @param y        the anchored y position
   * @param scale    the render scale to apply
   * @param colour   the ARGB colour used for the text
   */
  @OnlyIn(Dist.CLIENT)
  public static void drawStringScaled(GuiGraphics graphics, Font font, Component text, int x, int y, float scale,
      int colour, boolean dropShadow) {
    graphics.pose().pushPose();

    graphics.pose().translate(x, y, 0.0F);
    graphics.pose().scale(scale, scale, 1.0F);
    graphics.pose().translate(-x, -y, 0.0F);
    graphics.drawString(font, text, x, y, colour, dropShadow);

    graphics.pose().popPose();
  }

  /**
   * Draws centered text while keeping the center anchor fixed as the pose is
   * scaled.
   *
   * @param graphics the graphics context used for rendering
   * @param font     the font used for the text
   * @param text     the text to render
   * @param x        the horizontal center anchor
   * @param y        the top anchor
   * @param scale    the render scale to apply
   * @param colour   the ARGB colour used for the text
   */
  @OnlyIn(Dist.CLIENT)
  public static void drawCenteredStringScaled(GuiGraphics graphics, Font font, Component text, int x, int y,
      float scale, int colour, boolean dropShadow) {
    drawStringScaled(graphics, font, text, Math.round(x - scaledWidth(font, text, scale) / 2.0F), y, scale, colour,
        dropShadow);
  }

  @SuppressWarnings("null")
  public static List<Component> getVanillaTooltip(Player pPlayer, ItemStack stack) {
    List<Component> list = new ArrayList<>();
    for (EquipmentSlot equipmentslot : EquipmentSlot.values()) {
      @SuppressWarnings("null")
      Multimap<Attribute, AttributeModifier> multimap = stack.getAttributeModifiers(equipmentslot);
      if (!multimap.isEmpty()) {
        list.add(CommonComponents.EMPTY);
        list.add(Component.translatable("item.modifiers." + equipmentslot.getName()).withStyle(ChatFormatting.GRAY));

        for (Map.Entry<Attribute, AttributeModifier> entry : multimap.entries()) {
          AttributeModifier attributemodifier = entry.getValue();
          double d0 = attributemodifier.getAmount();
          boolean flag = false;
          if (pPlayer != null) {
            if (SkadaData.BASE_ATTACK_DAMAGE_UUID.equals(attributemodifier.getId())
                || SkadaData.SKADA_ATTACK_TYPE_DAMAGE_UUID.equals(attributemodifier.getId())) {
              d0 += pPlayer.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
              d0 += (double) EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);
              flag = true;
            } else if (SkadaData.BASE_ATTACK_SPEED_UUID.equals(attributemodifier.getId())
                || SkadaData.SKADA_ATTACK_TYPE_SPEED_UUID.equals(attributemodifier.getId())) {
              d0 += pPlayer.getAttributeBaseValue(Attributes.ATTACK_SPEED);
              flag = true;
            }
          }

          double d1;
          if (attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE
              && attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
            if (entry.getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
              d1 = d0 * 10.0D;
            } else {
              d1 = d0;
            }
          } else {
            d1 = d0 * 100.0D;
          }

          if (flag) {
            list.add(CommonComponents.space()
                .append(Component.translatable(
                    "attribute.modifier.equals." + attributemodifier.getOperation().toValue(),
                    ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(entry.getKey().getDescriptionId())))
                .withStyle(ChatFormatting.DARK_GREEN));
          } else if (d0 > 0.0D) {
            list.add(Component
                .translatable("attribute.modifier.plus." + attributemodifier.getOperation().toValue(),
                    ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(entry.getKey().getDescriptionId()))
                .withStyle(ChatFormatting.BLUE));
          } else if (d0 < 0.0D) {
            d1 *= -1.0D;
            list.add(Component
                .translatable("attribute.modifier.take." + attributemodifier.getOperation().toValue(),
                    ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(entry.getKey().getDescriptionId()))
                .withStyle(ChatFormatting.RED));
          }
        }
      }
    }
    return list;
  }

  @SuppressWarnings("null")
  public static List<Component> otherAttributesComponent(Multimap<Attribute, AttributeModifier> mainAttributes) {
    List<Component> list = new ArrayList<>();
    for (Map.Entry<Attribute, AttributeModifier> attributeAttributeModifierEntry : mainAttributes.entries()) {
      AttributeModifier attributemodifier = attributeAttributeModifierEntry.getValue();
      double d0 = attributemodifier.getAmount();
      double d1;
      if (attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE
          && attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
        if ((attributeAttributeModifierEntry).getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
          d1 = d0 * 10.0;
        } else {
          d1 = d0;
        }
      } else {
        d1 = d0 * 100.0;
      }
      if (d0 > 0.0) {
        list.add(Component
            .translatable("attribute.modifier.plus." + attributemodifier.getOperation().toValue(),
                ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                Component.translatable((attributeAttributeModifierEntry).getKey().getDescriptionId()))
            .withStyle(ChatFormatting.BLUE));
      } else if (d0 < 0.0) {
        d1 *= -1.0;
        list.add(Component
            .translatable("attribute.modifier.take." + attributemodifier.getOperation().toValue(),
                ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                Component.translatable((attributeAttributeModifierEntry).getKey().getDescriptionId()))
            .withStyle(ChatFormatting.RED));
      }
    }
    return list;
  }

  @SuppressWarnings("null")
  public static List<Component> getOtherSlotAttributesAsList(EquipmentSlot slot,
      Multimap<Attribute, AttributeModifier> multimap) {
    List<Component> list = new ArrayList<>();
    list.add(CommonComponents.EMPTY);
    list.add(Component.translatable("item.modifiers." + slot.getName()).withStyle(ChatFormatting.GRAY));
    if (!multimap.isEmpty()) {
      for (Map.Entry<Attribute, AttributeModifier> attributeAttributeModifierEntry : multimap.entries()) {
        AttributeModifier attributemodifier = attributeAttributeModifierEntry.getValue();
        double d0 = attributemodifier.getAmount();
        boolean flag = false;
        double d1;
        if (attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE
            && attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
          if ((attributeAttributeModifierEntry).getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
            d1 = d0 * 10.0;
          } else {
            d1 = d0;
          }
        } else {
          d1 = d0 * 100.0;
        }
        if (d0 > 0.0) {
          list.add(Component
              .translatable("attribute.modifier.plus." + attributemodifier.getOperation().toValue(),
                  ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                  Component.translatable((attributeAttributeModifierEntry).getKey().getDescriptionId()))
              .withStyle(ChatFormatting.BLUE));
        } else if (d0 < 0.0) {
          d1 *= -1.0;
          list.add(Component
              .translatable("attribute.modifier.take." + attributemodifier.getOperation().toValue(),
                  ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                  Component.translatable((attributeAttributeModifierEntry).getKey().getDescriptionId()))
              .withStyle(ChatFormatting.RED));
        }
      }
    }
    return list;
  }

  static final Style SPACER = Style.EMPTY.withFont(Util.rl("space"));
  static final Style PIXEL = Style.EMPTY.withFont(Util.rl("minimal_pixel_bitmap"));
  static final Style PIXEL_16 = Style.EMPTY.withFont(Util.rl("minimal_pixel_16x"));
  static final Style PIXEL_12 = Style.EMPTY.withFont(Util.rl("minimal_pixel_12x"));

  @SuppressWarnings("null")
  public static MutableComponent spacer(int i) {
    return Component.translatable("space." + i).withStyle(SPACER);
  }

}
