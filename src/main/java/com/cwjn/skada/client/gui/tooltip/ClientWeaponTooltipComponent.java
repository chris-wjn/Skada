package com.cwjn.skada.client.gui.tooltip;

import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.gen.ElementSpread;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.Util;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.text.DecimalFormat;
import java.util.*;

import static com.cwjn.skada.data.SkadaData.*;
import static net.minecraft.world.item.ItemStack.ATTRIBUTE_MODIFIER_FORMAT;

@OnlyIn(Dist.CLIENT)
public class ClientWeaponTooltipComponent implements ClientTooltipComponent {

    private final List<Component> lines = new ArrayList<>();
    private final Multimap<Attribute, AttributeModifier> mainAttributes;
    private final List<Multimap<Attribute, AttributeModifier>> otherAttributes;
    private final Player player = Minecraft.getInstance().player;
    private final WeaponInfo info;
    private final AttackType[] attackTypes;
    private int longest = 0; //the length of the longest line in lines
    private int arrowXCoord = 0; //the coordinate to draw the selector arrow at

    private static final Style ICONS = Style.EMPTY.withFont(Util.rl("icons"));
    private static final ResourceLocation SPRITES = Util.rl("textures/gui/spritesheet.png");
    private static final DecimalFormat df = new DecimalFormat("#.#");

    public ClientWeaponTooltipComponent(ItemStack itemstack) {
        EquipmentSlot[] slots = Arrays.stream(EquipmentSlot.values()).filter(s -> !s.equals(LivingEntity.getEquipmentSlotForItem(itemstack))).toArray(EquipmentSlot[]::new);
        this.mainAttributes = itemstack.getAttributeModifiers(LivingEntity.getEquipmentSlotForItem(itemstack));
        this.otherAttributes = Arrays.stream(slots).map(itemstack::getAttributeModifiers).toList();
        info = Util.getWeaponInfo(itemstack);
        attackTypes = Util.getAttackTypes(itemstack);
        AttackTypeInfo currentInfo = Util.getAttackTypeInfo(itemstack);
        int index = itemstack.getOrCreateTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY);
        if (attackTypes.length <= index) {
            //debug
            System.out.println("item: " + itemstack.getDisplayName());
            System.out.println("attackTypes.length: " + attackTypes.length);
            System.out.println("index: " + index);
            return;
        }
        ;
        lines.add(attackTypesComponent(attackTypes[index]));
        lines.add(attackInfoComponent(currentInfo, WEAPON_INFO_COMPONENT_TYPE.REACH));
        lines.add(attackInfoComponent(currentInfo, WEAPON_INFO_COMPONENT_TYPE.LETHALITY));
        lines.add(attackInfoComponent(currentInfo, WEAPON_INFO_COMPONENT_TYPE.ATTACK_SPEED));
        lines.addAll(attackDamageComponent(info.getSpread()));
        //lines.addAll(getAttributeComponents(mainAttributes));
            /*lines.add(Util.pixelFontComponent(Component.translatable("skada.tooltip.shift_for_weapon_info"), false));
            lines.add(Util.pixelFontComponent(Component.translatable("skada.tooltip.test_numbers",120495), true));
            lines.add(Util.pixelFontComponent(Component.translatable("skada.tooltip.test_swedish_characters"), false));*/
    }

    private List<Component> attackDamageComponent(ElementSpread spread) {
        double damage = mainAttributes.get(Attributes.ATTACK_DAMAGE).stream().filter(m -> m.getOperation() == AttributeModifier.Operation.ADDITION).mapToDouble(AttributeModifier::getAmount).sum() + player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        List<Component> list = new ArrayList<>();
        MutableComponent comp = Component.empty();
        comp.append(Component.translatable("skada.icon.attack_damage").withStyle(ICONS));
        comp.append(Util.pixelFontComponent(Component.translatable("skada.tooltip.info.attack_damage", df.format(damage*spread.getPowerBudget()))));
        list.add(comp);
        double powerRatio = spread.getPowerBudget()/spread.sumRatio();
        spread.getRatios().keySet().forEach(key -> list.add(Component.literal("   ")
                .append(Component.translatable("skada.icon.element." + key.name()).withStyle(ICONS))
                .append(Util.pixelFontComponent(
                                Component.translatable("skada.tooltip.info.element." + key.name(),
                                        (int)(spread.getRatios().get(key)*100/spread.sumRatio()), Util.round(spread.getRatios().get(key)*powerRatio*damage, 1))
                        ).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(key.colour() & 0xFFFFFF)))
                )
        ));
        return list;
    }

    @Override
    public int getHeight() {
        return lines.size()*(Minecraft.getInstance().font.lineHeight+2)+4;
    }

    @Override
    public int getWidth(Font pFont) {
        final int[] longest = {0};
        lines.forEach(l -> {
            if (pFont.width(l) > longest[0]) {
                longest[0] = pFont.width(l);
            }
        });
        this.longest = longest[0];
        return longest[0]+3;
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

    private List<Component> getAttributeComponents(Multimap<Attribute, AttributeModifier> multimap) {
        List<Component> list = new ArrayList<>();
        if (!multimap.isEmpty()) {
            Iterator var11 = multimap.entries().iterator();
            while (var11.hasNext()) {
                Map.Entry<Attribute, AttributeModifier> entry = (Map.Entry) var11.next();
                AttributeModifier attributemodifier = entry.getValue();
                double d0 = attributemodifier.getAmount();
                boolean flag = false;
                if (player != null) {
                    if (attributemodifier.getId() == BASE_ATTACK_DAMAGE_UUID) {
                        d0 += player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                        flag = true;
                    } else if (attributemodifier.getId() == BASE_ATTACK_SPEED_UUID) {
                        d0 += player.getAttributeBaseValue(Attributes.ATTACK_SPEED);
                        flag = true;
                    }
                }
                double d1;
                if (attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
                    if (entry.getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
                        d1 = d0 * 10.0;
                    } else {
                        d1 = d0;
                    }
                } else {
                    d1 = d0 * 100.0;
                }
                if (flag) {
                    list.add(CommonComponents.space().append(Component.translatable("attribute.modifier.equals." + attributemodifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(entry.getKey().getDescriptionId()))).withStyle(ChatFormatting.DARK_GREEN));
                } else if (d0 > 0.0) {
                    list.add(Component.translatable("attribute.modifier.plus." + attributemodifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(entry.getKey().getDescriptionId())).withStyle(ChatFormatting.BLUE));
                } else if (d0 < 0.0) {
                    d1 *= -1.0;
                    list.add(Component.translatable("attribute.modifier.take." + attributemodifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(entry.getKey().getDescriptionId())).withStyle(ChatFormatting.RED));
                }
            }
        }
        return list;
    }

    private Component attackTypesComponent(AttackType attackType) {
        //start an empty MutableComponent that will be appended to, so we can return a single component
        MutableComponent retComp = Component.empty();
        Font font = Minecraft.getInstance().font;

        /* we create an array of MutableComponents that will be used to create the final component.
         * (attackTypes.length + 2 + attackTypes.length-1) is the size of the array, where the +2 is for the curly braces
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
                retComp.append(Util.pixelFontComponent(Component.translatable("skada.tooltip.info.attack_speed",
                        Util.round(baseSpeed*attackTypeInfo.attackSpeedMod(), 1))));
                break;
        }
        return retComp;
    }

    private enum WEAPON_INFO_COMPONENT_TYPE {
        REACH,
        LETHALITY,
        ATTACK_SPEED
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
