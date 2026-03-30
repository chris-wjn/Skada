package com.cwjn.skada.client.gui.tooltip;

import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilText;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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
    private final List<EquipmentSlot> otherSlots;
    private final List<Component> lines = new ArrayList<>();

    private static final Style ICONS = Style.EMPTY.withFont(Util.rl("icons"));

    public ClientArmourTooltipComponent(ItemStack item) {
        EquipmentSlot[] slots = Arrays.stream(EquipmentSlot.values()).filter(s -> !s.equals(LivingEntity.getEquipmentSlotForItem(item))).toArray(EquipmentSlot[]::new);
        this.mainAttributes = HashMultimap.create(item.getAttributeModifiers(LivingEntity.getEquipmentSlotForItem(item)));
        this.otherSlots = Arrays.stream(slots).filter((x) -> !item.getAttributeModifiers(x).isEmpty()).toList();
        lines.add(armourComponent(mainAttributes));
        lines.add(toughnessComponent(mainAttributes));
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

    private Component armourComponent(Multimap<Attribute, AttributeModifier> mainAttributes) {
        MutableComponent comp = Component.empty();
        comp.append(Component.translatable("skada.icon.armour").withStyle(ICONS));
        double armourAggregate = mainAttributes.get(Attributes.ARMOR).stream()
                .filter((x) -> x.getOperation() == AttributeModifier.Operation.ADDITION)
                .mapToDouble(AttributeModifier::getAmount)
                .sum();
        this.mainAttributes.get(Attributes.ARMOR).removeIf((x) -> x.getOperation() == AttributeModifier.Operation.ADDITION);
        comp.append(UtilText.pixelFontComponent(Component.translatable("skada.tooltip.info.armour",
                df.format(armourAggregate))));
        return comp;
    }

    private Component toughnessComponent(Multimap<Attribute, AttributeModifier> mainAttributes) {
        MutableComponent comp = Component.empty();
        comp.append(Component.translatable("skada.icon.toughness").withStyle(ICONS));
        double toughnessAggregate = mainAttributes.get(Attributes.ARMOR_TOUGHNESS).stream()
                .filter((x) -> x.getOperation() == AttributeModifier.Operation.ADDITION)
                .mapToDouble(AttributeModifier::getAmount)
                .sum();
        this.mainAttributes.get(Attributes.ARMOR_TOUGHNESS).removeIf((x) -> x.getOperation() == AttributeModifier.Operation.ADDITION);
        comp.append(UtilText.pixelFontComponent(Component.translatable("skada.tooltip.info.toughness",
                df.format(toughnessAggregate))));
        return comp;
    }

        private Component burdenComponent(Multimap<Attribute, AttributeModifier> mainAttributes) {
        MutableComponent comp = Component.empty();
        comp.append(Component.translatable("skada.icon.weight").withStyle(ICONS));
        double burdenAggregate = mainAttributes.get(Attributes.ATTACK_SPEED).stream()
            .filter((x) -> Arrays.stream(SKADA_ARMOUR_BURDEN_MOD_UUID).anyMatch(uuid -> uuid.equals(x.getId())))
            .mapToDouble(AttributeModifier::getAmount)
            .sum();
        comp.append(UtilText.pixelFontComponent(Component.translatable("skada.tooltip.info.burden",
            df.format(Math.abs(burdenAggregate / 0.12)))));
        return comp;
        }

    private List<Component> elementComponents(Multimap<Attribute, AttributeModifier> mainAttributes) {
        List<Component> list = new ArrayList<>();
        for (Element e : REGISTRY_ELEMENT.get().getValues()) {
            if (mainAttributes.get(e.getAttribute()).isEmpty()) continue;
            double elementAggregate = mainAttributes.get(e.getAttribute()).stream()
                    .filter((x) -> x.getOperation() == AttributeModifier.Operation.ADDITION)
                    .mapToDouble(AttributeModifier::getAmount)
                    .sum();
            this.mainAttributes.get(e.getAttribute()).removeIf((x) -> x.getOperation() == AttributeModifier.Operation.ADDITION);
            MutableComponent comp = Component.empty();
            comp.append(Component.translatable("skada.icon.element." + e.name()).withStyle(ICONS));
            comp.append(UtilText.pixelFontComponent(
                    Component.translatable("skada.tooltip.info.element.resist." + e.name(),
                            df.format(elementAggregate))));
            list.add(comp);
        }
        return list;
    }

    private List<Component> damageClassComponent(Multimap<Attribute, AttributeModifier> mainAttributes) {
        List<Component> list = new ArrayList<>();
        for (AttackType at : REGISTRY_ATTACK_TYPE.get().getValues()) {
            if (mainAttributes.get(at.getAttribute()).isEmpty()) continue;
            double attackTypeAggregate = mainAttributes.get(at.getAttribute()).stream()
                    .filter((x) -> x.getOperation() == AttributeModifier.Operation.ADDITION)
                    .mapToDouble(AttributeModifier::getAmount)
                    .sum();
            this.mainAttributes.get(at.getAttribute()).removeIf((x) -> x.getOperation() == AttributeModifier.Operation.ADDITION);
            MutableComponent comp = Component.empty();
            comp.append(Component.translatable("skada.icon.attack_type." + at.name()).withStyle(ICONS));
            comp.append(UtilText.pixelFontComponent(
                    Component.translatable("skada.tooltip.info.attack_type.resist." + at.name(),
                            df.format(attackTypeAggregate))));
            list.add(comp);
        }
        return list;
    }

    @Override
    public int getHeight() {
        return lines.size()*(Minecraft.getInstance().font.lineHeight+2);
    }

    @Override
    public int getWidth(Font pFont) {
        final int[] longest = {0};
        lines.forEach(l -> {
            if (pFont.width(l) > longest[0]) {
                longest[0] = pFont.width(l);
            }
        });
        return longest[0] +3;
    }

    @Override
    public void renderText(Font pFont, int x, int y, Matrix4f pMatrix, MultiBufferSource.BufferSource pBufferSource) {
        ClientTooltipComponent.super.renderText(pFont, x, y, pMatrix, pBufferSource);
        if (lines.isEmpty()) return;
        for (Component c : lines) {
            pFont.drawInBatch(c, x+2.5f, y, 0xFFFFFF, false, pMatrix, pBufferSource, Font.DisplayMode.NORMAL,0x555555, 15728880);
            y+= pFont.lineHeight+2;
        }
    }

    @Override
    public void renderImage(Font pFont, int pX, int pY, GuiGraphics pGuiGraphics) {
        //todo
    }

}
