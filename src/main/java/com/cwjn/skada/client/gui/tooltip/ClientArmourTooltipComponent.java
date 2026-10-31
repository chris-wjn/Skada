package com.cwjn.skada.client.gui.tooltip;

import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilDefenseDisplay;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static com.cwjn.skada.data.SkadaData.*;

/** Armour ratings and optional equipped-swap comparisons in the shared equipment layout. */
public class ClientArmourTooltipComponent extends EquipmentTooltipComponent {
  private static final DecimalFormat DF = new DecimalFormat("#.#");
  private final Multimap<Attribute, AttributeModifier> mainAttributes;
  private final Multimap<Attribute, AttributeModifier> equippedSlotAttributes = HashMultimap.create();
  private final Player player = Minecraft.getInstance().player;

  /** Separates core defences, elemental ratings, attack-type ratings and input hints. */
  public ClientArmourTooltipComponent(ItemStack stack) {
    super(stack);
    EquipmentSlot mainSlot = LivingEntity.getEquipmentSlotForItem(stack);
    mainAttributes = HashMultimap.create(stack.getAttributeModifiers(mainSlot));
    if (player != null) {
      equippedSlotAttributes.putAll(player.getItemBySlot(mainSlot).getAttributeModifiers(mainSlot));
    }
    List<EquipmentSlot> otherSlots = Arrays.stream(EquipmentSlot.values())
        .filter(slot -> slot != mainSlot && !stack.getAttributeModifiers(slot).isEmpty()).toList();
    double armour = takeAdditionTotal(mainAttributes, Attributes.ARMOR);
    defence("skada.icon.armour", Component.translatable("skada.tooltip.label.armour"), armour);
    if (player != null && Screen.hasControlDown()) {
      preview(UtilDefenseDisplay.previewArmourSwap(player.getAttributeValue(Attributes.ARMOR),
          equippedAdditionTotal(Attributes.ARMOR), armour));
    }
    stat("skada.icon.toughness", Component.translatable("skada.tooltip.label.toughness"),
        Component.literal(DF.format(takeAdditionTotal(mainAttributes, Attributes.ARMOR_TOUGHNESS))));
    double burden = mainAttributes.get(Attributes.ATTACK_SPEED).stream()
        .filter(modifier -> Arrays.stream(SKADA_ARMOUR_BURDEN_MOD_UUID).anyMatch(id -> id.equals(modifier.getId())))
        .mapToDouble(AttributeModifier::getAmount).sum();
    mainAttributes.get(Attributes.ATTACK_SPEED)
        .removeIf(modifier -> Arrays.stream(SKADA_ARMOUR_BURDEN_MOD_UUID).anyMatch(id -> id.equals(modifier.getId())));
    stat("skada.icon.burden", Component.translatable("skada.tooltip.label.burden"),
        Component.literal(DF.format(Math.abs(burden / 0.12))));
    boolean first = true;
    for (Element element : REGISTRY_ELEMENT.get().getValues()) {
      if (mainAttributes.get(element.getAttribute()).isEmpty()) continue;
      if (first) section();
      first = false;
      double rating = takeAdditionTotal(mainAttributes, element.getAttribute());
      defence("skada.icon.element." + element.name(), Component.translatable(element.resist().getDescriptionId()),
          rating, element.colour() & 0xFFFFFF);
      if (player != null && Screen.hasControlDown()) {
        preview(element.usesArmourAsResistance()
            ? UtilDefenseDisplay.previewArmourSwap(player.getAttributeValue(element.resist()),
                equippedAdditionTotal(element.resist()), rating)
            : UtilDefenseDisplay.previewElementalResistanceSwap(player.getAttributeValue(element.resist()),
                equippedAdditionTotal(element.resist()), rating));
      }
    }
    first = true;
    for (AttackType type : REGISTRY_ATTACK_TYPE.get().getValues()) {
      if (mainAttributes.get(type.getAttribute()).isEmpty()) continue;
      if (first) section();
      first = false;
      double rating = takeAdditionTotal(mainAttributes, type.getAttribute());
      defence("skada.icon.attack_type." + type.name(), Component.translatable(type.getAttribute().getDescriptionId()), rating);
      if (player != null && Screen.hasControlDown()) {
        preview(UtilDefenseDisplay.previewAttackTypeResistanceSwap(player.getAttributeValue(type.getAttribute()),
            equippedAdditionTotal(type.getAttribute()), rating));
      }
    }
    attributeDetails(stack, mainAttributes, otherSlots);
    if (player != null) hint("skada.tooltip.hint.ctrl_comparison");
    if (!mainAttributes.isEmpty()) hint("skada.tooltip.hint.shift_details");
    if (!otherSlots.isEmpty()) hint("skada.tooltip.hint.alt_attributes");
  }

  /** Displays signed defence ratings without changing the resistance label. */
  private void defence(String icon, Component label, double value) {
    defence(icon, label, value, VALUE);
  }

  /** Uses elemental colours for resistance values, with red taking precedence for negative ratings. */
  private void defence(String icon, Component label, double value, int colour) {
    stat(icon, label.copy(), Component.literal(DF.format(value))
        .withStyle(style -> style.withColor(value < 0 ? NEGATIVE : colour)));
  }

  /** Uses the existing swap calculation, reserving green/red solely for a visible mitigation change. */
  private void preview(UtilDefenseDisplay.EquippedSwapPreview preview) {
    line(text(Component.literal("   ").append(Component.translatable(
        "skada.tooltip.info.defence.equipped_total_preview", DF.format(preview.currentTotal()),
        DF.format(preview.newTotal()))), LABEL));
    double delta = Util.round(preview.percentagePointDelta(), 1);
    ChatFormatting colour = delta > 0 ? ChatFormatting.GREEN : delta < 0 ? ChatFormatting.RED : ChatFormatting.GRAY;
    String change = colour + (delta > 0 ? "+" : "") + DF.format(delta) + ChatFormatting.RESET;
    line(text(Component.literal("   ").append(Component.translatable(
        "skada.tooltip.info.defence.mitigation_preview", mitigation(preview.currentMitigationPercent()),
        mitigation(preview.newMitigationPercent()), change)), LABEL));
  }

  /** Retains the distinction between damage reduction and vulnerability in comparison text. */
  private Component mitigation(double percent) {
    return Component.translatable(percent < 0 ? "skada.tooltip.info.defence.vulnerability_percent"
        : "skada.tooltip.info.defence.reduction_percent", DF.format(Math.abs(percent)));
  }

  /** Consumes only additive contributions, leaving other modifiers for the expanded attribute section. */
  private double takeAdditionTotal(Multimap<Attribute, AttributeModifier> attributes, Attribute attribute) {
    double total = attributes.get(attribute).stream()
        .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
        .mapToDouble(AttributeModifier::getAmount).sum();
    attributes.get(attribute).removeIf(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION);
    return total;
  }

  /** Reads the equipped piece's additive contribution for the unchanged swap-preview calculation. */
  private double equippedAdditionTotal(Attribute attribute) {
    return equippedSlotAttributes.get(attribute).stream()
        .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
        .mapToDouble(AttributeModifier::getAmount).sum();
  }
}
