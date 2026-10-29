package com.cwjn.skada.client.gui.tooltip;

import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilDefenseDisplay;
import com.cwjn.skada.util.UtilText;
import com.cwjn.skada.util.EquipmentWeightUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.cwjn.skada.data.SkadaData.*;
import static com.cwjn.skada.util.UtilText.getOtherSlotAttributesAsList;
import static com.cwjn.skada.util.UtilText.otherAttributesComponent;

public class ClientArmourTooltipComponent implements ClientTooltipComponent {

  private static final DecimalFormat df = new DecimalFormat("#.#");
  private final Multimap<Attribute, AttributeModifier> mainAttributes;
  private final Multimap<Attribute, AttributeModifier> equippedSlotAttributes = HashMultimap.create();
  private final EquipmentSlot mainSlot;
  private final List<EquipmentSlot> otherSlots;
  private final List<Component> lines = new ArrayList<>();
  private final Player player = Minecraft.getInstance().player;

  private static final Style ICONS = Style.EMPTY.withFont(Util.rl("icons"));
  private static final String PREVIEW_INDENT = "   ";

  public ClientArmourTooltipComponent(ItemStack item) {
    this.mainSlot = LivingEntity.getEquipmentSlotForItem(item);
    EquipmentSlot[] slots = Arrays.stream(EquipmentSlot.values()).filter(s -> !s.equals(mainSlot))
        .toArray(EquipmentSlot[]::new);
    this.mainAttributes = HashMultimap.create(item.getAttributeModifiers(mainSlot));
    if (player != null) {
      equippedSlotAttributes.putAll(player.getItemBySlot(mainSlot).getAttributeModifiers(mainSlot));
    }
    this.otherSlots = Arrays.stream(slots).filter((x) -> !item.getAttributeModifiers(x).isEmpty()).toList();
    lines.addAll(armourComponents());
    lines.add(toughnessComponent(mainAttributes));
    lines.add(weightComponent(item));
    lines.add(burdenComponent(mainAttributes));
    lines.addAll(elementComponents(mainAttributes));
    lines.addAll(damageClassComponent(mainAttributes));
    if (!this.mainAttributes.isEmpty()) {
      lines.add(UtilText.pixelFontComponent(Component.translatable("skada.tooltip.shift_for_other_attributes")));
      if (Screen.hasShiftDown()) {
        lines.addAll(otherAttributesComponent(mainAttributes));
      }
    }
    if (!otherSlots.isEmpty()) {
      lines.add(UtilText.pixelFontComponent(Component.translatable("skada.tooltip.alt_for_other_slot_attributes")));
      if (Screen.hasAltDown()) {
        for (EquipmentSlot slot : otherSlots) {
          lines.addAll(getOtherSlotAttributesAsList(slot, item.getAttributeModifiers(slot)));
        }
      }
    }
  }

  private List<Component> armourComponents() {
    List<Component> list = new ArrayList<>();
    double armourAggregate = takeAdditionTotal(mainAttributes, Attributes.ARMOR);
    list.add(defenceContributionComponent("skada.icon.armour", Component.translatable("skada.tooltip.label.armour"),
        armourAggregate));
    if (player != null && Screen.hasControlDown()) {
      list.addAll(equippedPreviewComponents(UtilDefenseDisplay.previewArmourSwap(
          player.getAttributeValue(Attributes.ARMOR),
          equippedAdditionTotal(Attributes.ARMOR),
          armourAggregate)));
    }
    return list;
  }

  private Component toughnessComponent(Multimap<Attribute, AttributeModifier> mainAttributes) {
    MutableComponent comp = Component.empty();
    comp.append(Component.translatable("skada.icon.toughness").withStyle(ICONS));
    double toughnessAggregate = mainAttributes.get(Attributes.ARMOR_TOUGHNESS).stream()
        .filter((x) -> x.getOperation() == AttributeModifier.Operation.ADDITION)
        .mapToDouble(AttributeModifier::getAmount)
        .sum();
    this.mainAttributes.get(Attributes.ARMOR_TOUGHNESS)
        .removeIf((x) -> x.getOperation() == AttributeModifier.Operation.ADDITION);
    comp.append(UtilText.pixelFontComponent(Component.translatable("skada.tooltip.info.toughness",
        df.format(toughnessAggregate))));
    return comp;
  }

  private Component burdenComponent(Multimap<Attribute, AttributeModifier> mainAttributes) {
    MutableComponent comp = Component.empty();
    double burdenAggregate = mainAttributes.get(Attributes.ATTACK_SPEED).stream()
        .filter((x) -> Arrays.stream(SKADA_ARMOUR_BURDEN_MOD_UUID).anyMatch(uuid -> uuid.equals(x.getId())))
        .mapToDouble(AttributeModifier::getAmount)
        .sum();
    mainAttributes.get(Attributes.ATTACK_SPEED)
      .removeIf((x) -> Arrays.stream(SKADA_ARMOUR_BURDEN_MOD_UUID).anyMatch(uuid -> uuid.equals(x.getId())));
    comp.append(UtilText.pixelFontComponent(Component.translatable("skada.tooltip.info.burden",
        df.format(Math.abs(burdenAggregate / 0.12)))));
    return comp;
  }

  private Component weightComponent(ItemStack stack) {
    MutableComponent line = Component.empty()
        .append(Component.translatable("skada.icon.weight").withStyle(ICONS))
        .append(UtilText.pixelFontComponent(Component.translatable("skada.tooltip.info.weight_kg",
            df.format(EquipmentWeightUtil.getStackWeightKg(stack)))));
    if (player != null) {
      line.append(UtilText.pixelFontComponent("  "))
          .append(UtilText.pixelFontComponent(Component.translatable("skada.tooltip.info.equipped_weight_total",
              df.format(EquipmentWeightUtil.getTotalEquippedWeightKg(player)))));
    }
    return line;
  }

  private List<Component> elementComponents(Multimap<Attribute, AttributeModifier> mainAttributes) {
    List<Component> list = new ArrayList<>();
    for (Element e : REGISTRY_ELEMENT.get().getValues()) {
      if (mainAttributes.get(e.getAttribute()).isEmpty())
        continue;
      double elementAggregate = takeAdditionTotal(mainAttributes, e.getAttribute());
      list.add(defenceContributionComponent(
          "skada.icon.element." + e.name(),
          Component.translatable(e.resist().getDescriptionId()),
          elementAggregate));
      if (player != null) {
        UtilDefenseDisplay.EquippedSwapPreview preview = e.usesArmourAsResistance()
            ? UtilDefenseDisplay.previewArmourSwap(player.getAttributeValue(e.resist()),
                equippedAdditionTotal(e.resist()), elementAggregate)
            : UtilDefenseDisplay.previewElementalResistanceSwap(player.getAttributeValue(e.resist()),
                equippedAdditionTotal(e.resist()), elementAggregate);
        list.addAll(equippedPreviewComponents(preview));
      }
    }
    return list;
  }

  private List<Component> damageClassComponent(Multimap<Attribute, AttributeModifier> mainAttributes) {
    List<Component> list = new ArrayList<>();
    for (AttackType at : REGISTRY_ATTACK_TYPE.get().getValues()) {
      if (mainAttributes.get(at.getAttribute()).isEmpty())
        continue;
      double attackTypeAggregate = takeAdditionTotal(mainAttributes, at.getAttribute());
      list.add(defenceContributionComponent(
          "skada.icon.attack_type." + at.name(),
          Component.translatable(at.getAttribute().getDescriptionId()),
          attackTypeAggregate));
      if (player != null) {
        list.addAll(equippedPreviewComponents(UtilDefenseDisplay.previewAttackTypeResistanceSwap(
            player.getAttributeValue(at.getAttribute()),
            equippedAdditionTotal(at.getAttribute()),
            attackTypeAggregate)));
      }
    }
    return list;
  }

  private Component defenceContributionComponent(String iconKey, Component label, double value) {
    MutableComponent comp = Component.empty();
    comp.append(Component.translatable(iconKey).withStyle(ICONS));
    MutableComponent infoLine = value < 0
        ? Component.translatable("skada.tooltip.info.defence.vulnerability_rating", label, df.format(Math.abs(value)))
        : Component.translatable("skada.tooltip.info.defence.rating", label, df.format(value));
    comp.append(UtilText.pixelFontComponent(infoLine));
    return comp;
  }

  private List<Component> equippedPreviewComponents(UtilDefenseDisplay.EquippedSwapPreview preview) {
    if (!Screen.hasControlDown()) {
      return List.of();
    }
    List<Component> list = new ArrayList<>();
    list.add(previewLine(Component.translatable(
        "skada.tooltip.info.defence.equipped_total_preview",
        df.format(preview.currentTotal()),
        df.format(preview.newTotal()))));
    list.add(previewLine(Component.translatable(
        "skada.tooltip.info.defence.mitigation_preview",
        mitigationTextComponent(preview.currentMitigationPercent()),
        mitigationTextComponent(preview.newMitigationPercent()),
        signedNumber(preview.percentagePointDelta()))));
    return list;
  }

  private Component previewLine(MutableComponent line) {
    return UtilText.pixelFontComponent(PREVIEW_INDENT)
        .append(UtilText.pixelFontComponent(line))
        .withStyle(ChatFormatting.DARK_GRAY);
  }

  private MutableComponent mitigationTextComponent(double mitigationPercent) {
    return Component.translatable(
        mitigationPercent < 0 ? "skada.tooltip.info.defence.vulnerability_percent"
            : "skada.tooltip.info.defence.reduction_percent",
        df.format(Math.abs(mitigationPercent)));
  }

  private double takeAdditionTotal(Multimap<Attribute, AttributeModifier> attributes, Attribute attribute) {
    double aggregate = attributes.get(attribute).stream()
        .filter((modifier) -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
        .mapToDouble(AttributeModifier::getAmount)
        .sum();
    attributes.get(attribute).removeIf((modifier) -> modifier.getOperation() == AttributeModifier.Operation.ADDITION);
    return aggregate;
  }

  private double equippedAdditionTotal(Attribute attribute) {
    return equippedSlotAttributes.get(attribute).stream()
        .filter((modifier) -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
        .mapToDouble(AttributeModifier::getAmount)
        .sum();
  }

  private String signedNumber(double value) {
    return value > 0 ? "+" + df.format(value) : df.format(value);
  }

  @Override
  public int getHeight() {
    return lines.size() * (Minecraft.getInstance().font.lineHeight + 2);
  }

  @Override
  public int getWidth(Font pFont) {
    final int[] longest = { 0 };
    lines.forEach(l -> {
      if (pFont.width(l) > longest[0]) {
        longest[0] = pFont.width(l);
      }
    });
    return longest[0] + 3;
  }

  @Override
  public void renderText(Font pFont, int x, int y, Matrix4f pMatrix, MultiBufferSource.BufferSource pBufferSource) {
    ClientTooltipComponent.super.renderText(pFont, x, y, pMatrix, pBufferSource);
    if (lines.isEmpty())
      return;
    for (Component c : lines) {
      pFont.drawInBatch(c, x + 2.5f, y, 0xFFFFFF, false, pMatrix, pBufferSource, Font.DisplayMode.NORMAL, 0x555555,
          15728880);
      y += pFont.lineHeight + 2;
    }
  }

  @Override
  public void renderImage(Font pFont, int pX, int pY, GuiGraphics pGuiGraphics) {
    // todo
  }

}
