package com.cwjn.skada.client.gui.button;

import com.cwjn.skada.client.gui.screen.widget.ElementInfoPanel;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilText;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.StringUtils;

public class ElementInfoButton extends Button {

    private static final int BUTTON_WIDTH = 171, BUTTON_HEIGHT = 44;
    private static final ResourceLocation TEXTURE = Util.rl("textures/gui/damage_page/element_info_panel.png");
    private static final ResourceLocation DAMAGE_ICON = Util.rl("textures/gui/book_page_icons/damage_page_icon.png");
    private static final ResourceLocation ARMOUR_ICON = Util.rl("textures/gui/book_page_icons/armour_icon.png");
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final Player player = minecraft.player;

    private final Element element;
    private ElementInfoPanel widget;
    private final WeaponInfo weaponInfo;

    public ElementInfoButton(int x, int y, Component message, Element e, OnPress onPress, WeaponInfo weaponInfo) {
        super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, message, onPress, Button.DEFAULT_NARRATION);
        this.element = e;
        this.weaponInfo = weaponInfo;
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {

    }

    public void renderCustom(GuiGraphics pGuiGraphics) {
        int TEXTURE_WIDTH = 171, TEXTURE_HEIGHT = 88;
        if (this.isHovered) {
            // Same texture but border is highlighted white
            int HIGHLIGHTED_TEXTURE_X = 0, HIGHLIGHTED_TEXTURE_Y = 44;
            pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), HIGHLIGHTED_TEXTURE_X, HIGHLIGHTED_TEXTURE_Y, BUTTON_WIDTH, BUTTON_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        } else {
            // normal texture
            int TEXTURE_X = 0, TEXTURE_Y = 0;
            pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), TEXTURE_X, TEXTURE_Y, BUTTON_WIDTH, BUTTON_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        //icon
        int ICON_OFFSET_X = 10, ICON_OFFSET_Y = 14;
        int ICON_X = 0, ICON_Y = 0;
        int ICON_WIDTH = 16, ICON_HEIGHT = 16;
        pGuiGraphics.blit(this.element.icon(), getX()+ICON_OFFSET_X, getY()+ICON_OFFSET_Y, ICON_X, ICON_Y, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);

        //name
        int NAME_OFFSET_X = 28, NAME_OFFSET_Y = 15;
        pGuiGraphics.drawString(minecraft.font,
                UtilText.pixelFontComponent(StringUtils.capitalize(this.element.name()), false, true, false),
                getX()+NAME_OFFSET_X, getY()+NAME_OFFSET_Y, this.element.colour(), true);

        //damage and armour icons
        float ICON_SCALE = 0.9f;
        int DAMAGE_ICON_OFFSET_X = 110, DAMAGE_ICON_OFFSET_Y = 5;
        int ARMOUR_ICON_OFFSET_X = 125, ARMOUR_ICON_OFFSET_Y = 26;
        int TRANSLATE_OFFSET_Y = 15;
        int ICON_SIZE = 16;
        int TEXTURE_U = 0, TEXTURE_V = 0;
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().scale(ICON_SCALE, ICON_SCALE, 1.0f);
        pGuiGraphics.pose().translate((getX() + DAMAGE_ICON_OFFSET_X) / ICON_SCALE - (getX() + DAMAGE_ICON_OFFSET_X), (getY() + TRANSLATE_OFFSET_Y) / ICON_SCALE - (getY() + TRANSLATE_OFFSET_Y), 0);
        pGuiGraphics.blit(DAMAGE_ICON, getX() + DAMAGE_ICON_OFFSET_X, getY() + DAMAGE_ICON_OFFSET_Y, TEXTURE_U, TEXTURE_V, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        pGuiGraphics.blit(ARMOUR_ICON, getX() + ARMOUR_ICON_OFFSET_X, getY() + ARMOUR_ICON_OFFSET_Y, TEXTURE_U, TEXTURE_V, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        pGuiGraphics.pose().popPose();

        // damage and armour values
        int DAMAGE_TEXT_OFFSET_X = 125, DAMAGE_TEXT_OFFSET_Y = 5;
        int ARMOUR_TEXT_OFFSET_X = 140, ARMOUR_TEXT_OFFSET_Y = 24;
        int ROUNDING_PRECISION = 1;
        int AFFINITY_DEFAULT = 1;
        pGuiGraphics.drawString(minecraft.font,
                UtilText.pixelFontComponent(Util.roundToString(
                        (player.getAttributeValue(this.element.baseDamage()) +
                                weaponInfo.getSpread().getDamageFromElementRatio(player.getAttributeValue(Attributes.ATTACK_DAMAGE), this.element))
                                * (player.getAttributeValue(this.element.affinity())+AFFINITY_DEFAULT), ROUNDING_PRECISION), false, true, false),
                getX()+DAMAGE_TEXT_OFFSET_X, getY()+DAMAGE_TEXT_OFFSET_Y, this.element.colour(), true);
        pGuiGraphics.drawString(minecraft.font,
                UtilText.pixelFontComponent(Util.roundToString(
                        player.getAttributeValue(this.element.resist()),
                        ROUNDING_PRECISION), false, true, false),
                getX()+ARMOUR_TEXT_OFFSET_X, getY()+ARMOUR_TEXT_OFFSET_Y, this.element.colour(), true);
    }

    public Element getElement() {
        return element;
    }

    public void setWidget(ElementInfoPanel widget) {
        this.widget = widget;
    }

    public ElementInfoPanel getWidget() {
        return widget;
    }

    public void setIsHovered(boolean isHovered) {
        this.isHovered = isHovered;
    }

    @Override
    protected boolean clicked(double pMouseX, double pMouseY) {
        return this.active && this.visible && this.isHovered;
    }

}
