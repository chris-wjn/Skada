package com.cwjn.skada.client.gui.tooltip;

import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.gen.ElementSpread;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.Util;
import com.google.common.collect.HashMultimap;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.text.DecimalFormat;
import java.util.*;

import static com.cwjn.skada.data.SkadaData.*;
import static com.cwjn.skada.util.Util.getOtherSlotAttributesAsList;
import static com.cwjn.skada.util.Util.otherAttributesComponent;

@OnlyIn(Dist.CLIENT)
public class ClientWeaponTooltipComponent implements ClientTooltipComponent {

    private final List<Component> lines = new ArrayList<>();
    private final HashMultimap<Attribute, AttributeModifier> mainAttributes;
    private final List<EquipmentSlot> otherSlots;
    private final Player player = Minecraft.getInstance().player;
    private final WeaponInfo info;
    private final AttackType[] attackTypes;
    private int arrowXCoord = 0; //the coordinate to draw the selector arrow at

    private static final Style ICONS = Style.EMPTY.withFont(Util.rl("icons"));
    private static final ResourceLocation SPRITES = Util.rl("textures/gui/spritesheet.png");
    private static final DecimalFormat df = new DecimalFormat("#.#");

    public ClientWeaponTooltipComponent(ItemStack itemstack) {
        EquipmentSlot[] slots = Arrays.stream(EquipmentSlot.values()).filter(s -> !s.equals(LivingEntity.getEquipmentSlotForItem(itemstack))).toArray(EquipmentSlot[]::new);
        this.mainAttributes = HashMultimap.create(itemstack.getAttributeModifiers(LivingEntity.getEquipmentSlotForItem(itemstack)));
        this.otherSlots = Arrays.stream(slots).filter((x) -> !itemstack.getAttributeModifiers(x).isEmpty()).toList();
        info = Util.getWeaponInfo(itemstack);
        attackTypes = Util.getAttackTypes(itemstack);
        AttackTypeInfo currentInfo = Util.getAttackTypeInfo(itemstack);
        int index = itemstack.getOrCreateTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY);
        lines.add(attackTypesComponent(attackTypes[index]));
        if (!info.ignoreAttributes()) lines.add(attackInfoComponent(currentInfo, WEAPON_INFO_COMPONENT_TYPE.REACH));
        lines.add(attackInfoComponent(currentInfo, WEAPON_INFO_COMPONENT_TYPE.LETHALITY));
        lines.add(attackInfoComponent(currentInfo, WEAPON_INFO_COMPONENT_TYPE.ACCURACY));
        lines.add(attackInfoComponent(currentInfo, WEAPON_INFO_COMPONENT_TYPE.CRITICAL_FAIL));
        if (!info.ignoreAttributes()) lines.add(attackInfoComponent(currentInfo, WEAPON_INFO_COMPONENT_TYPE.ATTACK_SPEED));
        if (itemstack.getItem() instanceof CrossbowItem) {
            lines.add(attackInfoComponent(currentInfo, WEAPON_INFO_COMPONENT_TYPE.VELOCITY_CROSSBOW));
            lines.addAll(attackDamageComponent(info.getSpread(), false));
        }
        else if (itemstack.getItem() instanceof BowItem) {
            lines.add(attackInfoComponent(currentInfo, WEAPON_INFO_COMPONENT_TYPE.VELOCITY_BOW));
            lines.addAll(attackDamageComponent(info.getSpread(), false));
        }
        else {
            lines.addAll(attackDamageComponent(info.getSpread(), true));
        }
        if (!this.mainAttributes.isEmpty()) {
            lines.add(Util.pixelFontComponent(Component.translatable("skada.tooltip.shift_for_other_attributes")));
            if (Screen.hasShiftDown()) {
                lines.addAll(otherAttributesComponent(mainAttributes));
            }
        }
        if (!otherSlots.isEmpty()) {
            lines.add(Util.pixelFontComponent(Component.translatable("skada.tooltip.alt_for_other_slot_attributes")));
            if (Screen.hasAltDown()) {
                for (EquipmentSlot slot : otherSlots) {
                    lines.addAll(getOtherSlotAttributesAsList(slot, itemstack.getAttributeModifiers(slot)));
                }
            }
        }
            /*lines.add(Util.pixelFontComponent(Component.translatable("skada.tooltip.shift_for_weapon_info"), false));
            lines.add(Util.pixelFontComponent(Component.translatable("skada.tooltip.test_numbers",120495), true));
            lines.add(Util.pixelFontComponent(Component.translatable("skada.tooltip.test_swedish_characters"), false));*/
    }

    private List<Component> attackDamageComponent(ElementSpread spread, boolean showFinalDamage) {
        double damage = mainAttributes.get(Attributes.ATTACK_DAMAGE).stream().filter(m -> m.getOperation() == AttributeModifier.Operation.ADDITION).mapToDouble(AttributeModifier::getAmount).sum() + player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        this.mainAttributes.get(Attributes.ATTACK_DAMAGE).removeIf(m -> m.getOperation() == AttributeModifier.Operation.ADDITION);
        List<Component> list = new ArrayList<>();
        MutableComponent comp = Component.empty();
        comp.append(Component.translatable("skada.icon.attack_damage").withStyle(ICONS));
        if (showFinalDamage) comp.append(Util.pixelFontComponent(Component.translatable("skada.tooltip.info.attack_damage", df.format(damage*spread.getPowerBudget()))));
        else comp.append(Util.pixelFontComponent(Component.translatable("skada.tooltip.info.attack_damage_no_sum")));
        list.add(comp);
        double powerRatio = spread.getPowerBudget()/spread.sumRatio();
        if (showFinalDamage) {
            spread.getRatios().keySet().forEach(key -> list.add(Component.literal("   ")
                    .append(Component.translatable("skada.icon.element." + key.name()).withStyle(ICONS))
                    .append(Util.pixelFontComponent(
                                    Component.translatable("skada.tooltip.info.element." + key.name(),
                                            (int) (spread.getRatios().get(key) * 100 / spread.sumRatio()), Util.round(spread.getRatios().get(key) * powerRatio * damage, 1))
                            ).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(key.colour() & 0xFFFFFF)))
                    )
            ));
        }
        else {
            spread.getRatios().keySet().forEach(key -> list.add(Component.literal("   ")
                    .append(Component.translatable("skada.icon.element." + key.name()).withStyle(ICONS))
                    .append(Util.pixelFontComponent(
                                    Component.translatable("skada.tooltip.info.element." + key.name() + "_no_sum",
                                            (int) (spread.getRatios().get(key) * 100 / spread.sumRatio()))
                            ).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(key.colour() & 0xFFFFFF)))
                    )
            ));
        }
        return list;
    }

    /*
        * The height of the tooltip is calculated by multiplying the number of lines by the line height of the font, then
        * adding 2 pixels for each line to account for the padding between lines. If there are multiple attack types,
        * we add 4 pixels to the height to account for the extra space needed for the attack type selector arrow.
     */
    @Override
    public int getHeight() {
        int baseSize = lines.size()*(Minecraft.getInstance().font.lineHeight+2);
        int selectorArrowHeight = attackTypes.length>1 ? 4 : 0;
        return baseSize + selectorArrowHeight;
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
        pFont.drawInBatch(lines.remove(0), x, y, 0xFFFFFF, false, pMatrix, pBufferSource, Font.DisplayMode.NORMAL,0x555555, 15728880);
        if (info.getAttackTypes().size() != 1) y+= pFont.lineHeight+6;
        else y+= pFont.lineHeight+2;
        for (Component c : lines) {
            pFont.drawInBatch(c, x+2.5f, y, 0xFFFFFF, false, pMatrix, pBufferSource, Font.DisplayMode.NORMAL,0x555555, 15728880);
            y+= pFont.lineHeight+2;
        }
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        ClientTooltipComponent.super.renderImage(font, x, y, guiGraphics);
        if (lines.isEmpty()) return;
        //this.drawHorizontalGradient(guiGraphics, x, y+font.lineHeight, x+longest, y+font.lineHeight+0.5f, 0, 0xFFFFFFFF, 0x00FFFFFF);
        //guiGraphics.blit(SPRITES, x+arrowXCoord, y+font.lineHeight-10, 1, 0, 5, 4);
        if (info.getAttackTypes().size() != 1) guiGraphics.blit(SPRITES, x+arrowXCoord, y+font.lineHeight-1, 1, 4, 5, 4);
    }

    private Component attackTypesComponent(AttackType attackType) {
        //start an empty MutableComponent that will be appended to, so we can return a single component
        MutableComponent retComp = Component.empty();
        Font font = Minecraft.getInstance().font;

        /* we create an array of MutableComponents that will be used to create the final component.
         * (attackTypes.length + 2 + attackTypes.length-1) is the area of the array, where the +2 is for the curly braces
         * and the +attackTypes.length-1 is for the commas between the attack types
        */
        MutableComponent[] attackTypeComponents = new MutableComponent[info.getAttackTypes().size() + 2 + info.getAttackTypes().size()-1];
        //attackTypeComponents[0] = Component.literal("[");
        retComp.append(Util.pixelFontComponent(Component.literal("[")));
        //attackTypeComponents[attackTypeComponents.length-1] = Component.literal("]");
        int countArrow = font.width(retComp);

        //iterate over the attack types and create a MutableComponent for each one. We start at i = 1 to skip the opening curly brace,
        //and we set doSlash to false in order to avoid adding a slash before the first attack type.
        boolean doSlash = false;
        int correctAttackIndex = 0;
        for (int i = 1; i <= attackTypeComponents.length-2; i++) {
            MutableComponent thisComp;
            if (doSlash) {
                thisComp = Util.pixelFontComponent(Component.literal("/"));
                countArrow+=font.width(thisComp);
                doSlash = false;
            }
            else if (attackType.equals(attackTypes[correctAttackIndex])) {
                thisComp = Util.pixelFontComponent(Component.literal(attackTypes[correctAttackIndex].name().toUpperCase())).withStyle(ChatFormatting.AQUA);
                correctAttackIndex++;
                arrowXCoord = countArrow+(font.width(thisComp)/2)-2;
                doSlash = true;
            }
            else {
                thisComp = Util.pixelFontComponent(Component.literal(attackTypes[correctAttackIndex].name().toUpperCase())).withStyle(ChatFormatting.DARK_GRAY);
                correctAttackIndex++;
                countArrow+=font.width(thisComp);
                doSlash = true;
            }
            //attackTypeComponents[i] = thisComp;
            retComp.append(thisComp);
        }
        retComp.append(Util.pixelFontComponent(Component.literal("]")));
        //return Util.pixelFontComponent(attackTypeComponents);
        return retComp;
    }

    private Component attackInfoComponent(AttackTypeInfo attackTypeInfo, WEAPON_INFO_COMPONENT_TYPE componentType) {
        MutableComponent retComp = Component.empty();
        retComp.append(Component.translatable("skada.icon." + componentType.toString().toLowerCase()).withStyle(ICONS));
        switch (componentType) {
            case REACH:
                retComp.append(Util.pixelFontComponent(Component.translatable("skada.tooltip.info.reach",
                        df.format(attackTypeInfo.minReach()),
                        df.format(attackTypeInfo.maxReach()))));
                break;
            case LETHALITY:
                retComp.append(Util.pixelFontComponent(Component.translatable("skada.tooltip.info.lethality",
                        df.format(attackTypeInfo.lethality()))));
                break;
            case ATTACK_SPEED:
                double baseSpeed = player.getAttributeBaseValue(Attributes.ATTACK_SPEED)
                        + mainAttributes.get(Attributes.ATTACK_SPEED).stream().filter(m -> m.getOperation()== AttributeModifier.Operation.ADDITION).mapToDouble(AttributeModifier::getAmount).sum();
                this.mainAttributes.get(Attributes.ATTACK_SPEED).removeIf(m -> m.getOperation() == AttributeModifier.Operation.ADDITION);
                baseSpeed *= attackTypeInfo.attackSpeedMod();
                this.mainAttributes.get(Attributes.ATTACK_SPEED).removeIf(m -> m.getId().equals(SKADA_ATTACK_TYPE_SPEED_UUID));
                retComp.append(Util.pixelFontComponent(Component.translatable("skada.tooltip.info.attack_speed",
                        Util.round(baseSpeed, 1))));
                break;
            case ACCURACY:
                retComp.append(Util.pixelFontComponent(Component.translatable("skada.tooltip.info.accuracy",
                        df.format(attackTypeInfo.precision()*100))));
                break;
            case VELOCITY_CROSSBOW:
                retComp.append(Util.pixelFontComponent(Component.translatable("skada.tooltip.info.velocity",
                        df.format(attackTypeInfo.damageBonus()+3.15))));
                break;
            case VELOCITY_BOW:
                retComp.append(Util.pixelFontComponent(Component.translatable("skada.tooltip.info.velocity",
                        df.format(attackTypeInfo.damageBonus()+3.0))));
                break;
            case CRITICAL_FAIL:
                retComp.append(Util.pixelFontComponent(Component.translatable("skada.tooltip.info.critical_fail",
                        df.format(attackTypeInfo.failChance()*100))));
                break;
        }
        return retComp;
    }

    private enum WEAPON_INFO_COMPONENT_TYPE {
        REACH,
        LETHALITY,
        ATTACK_SPEED,
        VELOCITY_CROSSBOW,
        VELOCITY_BOW,
        CRITICAL_FAIL,
        ACCURACY
    }

    private void drawHorizontalGradient(GuiGraphics graphics, float pX1, float pY1, float pX2, float pY2, float pZ, int pColorFrom, int pColorTo) {
        VertexConsumer vertexConsumer = graphics.bufferSource().getBuffer(RenderType.gui());
        float f = (float) FastColor.ARGB32.alpha(pColorFrom) / 255.0F;
        float f1 = (float) FastColor.ARGB32.red(pColorFrom) / 255.0F;
        float f2 = (float) FastColor.ARGB32.green(pColorFrom) / 255.0F;
        float f3 = (float) FastColor.ARGB32.blue(pColorFrom) / 255.0F;
        float f4 = (float) FastColor.ARGB32.alpha(pColorTo) / 255.0F;
        float f5 = (float) FastColor.ARGB32.red(pColorTo) / 255.0F;
        float f6 = (float) FastColor.ARGB32.green(pColorTo) / 255.0F;
        float f7 = (float) FastColor.ARGB32.blue(pColorTo) / 255.0F;
        Matrix4f matrix4f = graphics.pose().last().pose();
        vertexConsumer.vertex(matrix4f, pX1, pY1, pZ).color(f1, f2, f3, f).endVertex();//top left
        vertexConsumer.vertex(matrix4f, pX1, pY2, pZ).color(f1, f2, f3, f).endVertex();//bottom left
        vertexConsumer.vertex(matrix4f, pX2, pY2, pZ).color(f5, f6, f7, f4).endVertex();//bottom right
        vertexConsumer.vertex(matrix4f, pX2, pY1, pZ).color(f5, f6, f7, f4).endVertex();//top right
    }

}
