package com.cwjn.skada.client.gui.screen.widget;

import com.cwjn.skada.SkadaRegistry;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.UtilColour;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilText;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class ElementInfoPanel {

    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final Player player = minecraft.player;
    private final ResourceLocation TEXTURE = Util.rl("textures/gui/damage_page/element_info_widget.png");
    private static final int WIDTH = 229, HEIGHT = 293;
    private final WeaponInfo weaponInfo;
    private final int x;
    private final int y;
    public boolean visible = false;
    private int LINE_HEIGHT = 14;
    private int SCROLLBOX_WIDTH = 203;
    private int SCROLLBOX_HEIGHT = 96;
    private int DAMAGE_INFO_MIN_SCROLL_POSITION = -3;
    private int DAMAGE_INFO_MAX_SCROLL_POSITION = 0;
    private int DAMAGE_INFO_CURRENT_SCROLL_POSITION = DAMAGE_INFO_MIN_SCROLL_POSITION;
    private int ARMOUR_INFO_MIN_SCROLL_POSITION = -3;
    private int ARMOUR_INFO_MAX_SCROLL_POSITION = 0;
    private int ARMOUR_INFO_CURRENT_SCROLL_POSITION = ARMOUR_INFO_MIN_SCROLL_POSITION;
    private List<Component> damageInfoLines;
    private List<Component> armourInfoLines;
    private final Element element;

    public ElementInfoPanel(int pX, int pY, Element e, WeaponInfo info) {
        this.x = pX;
        this.y = pY;
        this.element = e;
        this.weaponInfo = info;
        this.damageInfoLines = getDamageInfoLines();
        this.armourInfoLines = getArmourInfoLines();
    }

    public void render(GuiGraphics pGuiGraphics) {
        if (!visible) return;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        int ICON_OFFSET_X = WIDTH/2 - 16;//subtract 16 from x offset because the icon is scaled to 32x32
        int ICON_OFFSET_Y = 14;
        int ICON_X = 0;
        int ICON_Y = 0;
        int ICON_WIDTH = 16;
        int ICON_HEIGHT = 16;
        int DIVIDER_X_START = x+15;
        int DIVIDER_X_END = x + WIDTH - 16;
        int DIVIDER_Y = y + (HEIGHT / 2) +16;
        int ICON_BOX_X = x + ICON_OFFSET_X - 2;
        int ICON_BOX_Y = y + ICON_OFFSET_Y - 2;
        int ICON_BOX_WIDTH = 32 + 4; // 32 for the icon, 2 padding on each side
        int ICON_BOX_HEIGHT = 32 + 4;
        //icon box border
        pGuiGraphics.fill(ICON_BOX_X-1, ICON_BOX_Y-1, ICON_BOX_X + ICON_BOX_WIDTH+1, ICON_BOX_Y + ICON_BOX_HEIGHT+1, UtilColour.UI_BORDER_COLOUR | 0xFF000000);
        //icon box background
        pGuiGraphics.fill(ICON_BOX_X, ICON_BOX_Y, ICON_BOX_X + ICON_BOX_WIDTH, ICON_BOX_Y + ICON_BOX_HEIGHT, UtilColour.UI_BACKGROUND_COLOUR | 0xFF000000);

        //icon
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().scale(2f, 2f, 1f);
        pGuiGraphics.pose().translate(-(x+ICON_OFFSET_X)/2f, -(y+ICON_OFFSET_Y)/2f, 0f);
        if (element.equals(SkadaRegistry.COLD.get()))
            pGuiGraphics.pose().translate(0.5f, 0.5f, 0); // slight offset for cold element icon
        pGuiGraphics.blit(this.element.icon(), x+ICON_OFFSET_X, y+ICON_OFFSET_Y, ICON_X, ICON_Y, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);
        pGuiGraphics.pose().popPose();

        //divider
        pGuiGraphics.hLine(DIVIDER_X_START, DIVIDER_X_END, DIVIDER_Y, UtilColour.UI_BORDER_COLOUR | 0xFF000000);

        //damage info scrollbox
        drawDamageInfo(pGuiGraphics, DIVIDER_Y);
        //armour info scrollbox
        drawArmourInfo(pGuiGraphics, DIVIDER_Y);
    }

    public boolean handleScroll(double mouseX, double mouseY, double pDelta) {
        if (!visible) return false;
        // Check if the mouse is within the scrollbox area
        int DAMAGE_BOX_X = x + 15;
        int DAMAGE_BOX_Y = y + 61;
        int ARMOUR_BOX_X = x + 15;
        int ARMOUR_BOX_Y = y + (HEIGHT / 2) + 16 + 5;
        if (mouseX >= DAMAGE_BOX_X && mouseX <= DAMAGE_BOX_X + SCROLLBOX_WIDTH && mouseY >= DAMAGE_BOX_Y && mouseY <= DAMAGE_BOX_Y + SCROLLBOX_HEIGHT) {
            // Calculate the new scroll position based on the delta
            int actualDelta = (int) (pDelta * -5);
            if (DAMAGE_INFO_CURRENT_SCROLL_POSITION + actualDelta <= DAMAGE_INFO_MIN_SCROLL_POSITION) DAMAGE_INFO_CURRENT_SCROLL_POSITION = DAMAGE_INFO_MIN_SCROLL_POSITION;
            else if (DAMAGE_INFO_CURRENT_SCROLL_POSITION + actualDelta >= DAMAGE_INFO_MAX_SCROLL_POSITION) DAMAGE_INFO_CURRENT_SCROLL_POSITION = DAMAGE_INFO_MAX_SCROLL_POSITION;
            else DAMAGE_INFO_CURRENT_SCROLL_POSITION += actualDelta;
            return true;
        }
        else if (mouseX >= ARMOUR_BOX_X && mouseX <= ARMOUR_BOX_X + SCROLLBOX_WIDTH && mouseY >= ARMOUR_BOX_Y && mouseY <= ARMOUR_BOX_Y + SCROLLBOX_HEIGHT) {
            int actualDelta = (int) (pDelta * -5);
            if (ARMOUR_INFO_CURRENT_SCROLL_POSITION + actualDelta <= ARMOUR_INFO_MIN_SCROLL_POSITION) ARMOUR_INFO_CURRENT_SCROLL_POSITION = ARMOUR_INFO_MIN_SCROLL_POSITION;
            else if (ARMOUR_INFO_CURRENT_SCROLL_POSITION + actualDelta >= ARMOUR_INFO_MAX_SCROLL_POSITION) ARMOUR_INFO_CURRENT_SCROLL_POSITION = ARMOUR_INFO_MAX_SCROLL_POSITION;
            else ARMOUR_INFO_CURRENT_SCROLL_POSITION += actualDelta;
            return true;
        }
        return false;
    }

    private void drawDamageInfo(GuiGraphics pGuiGraphics, int dividerY) {
        //bounds of the where we will write the damage info, if the amount of lines exceeds this, we will scroll
        int X_START = x + 13;
        int Y_START = y + 61;
        double scrollPercentage = DAMAGE_INFO_MAX_SCROLL_POSITION > 0 ? (double) DAMAGE_INFO_CURRENT_SCROLL_POSITION / DAMAGE_INFO_MAX_SCROLL_POSITION : 0;
        //border + background
        pGuiGraphics.fill(X_START-1, Y_START-1, X_START + SCROLLBOX_WIDTH+1, Y_START + SCROLLBOX_HEIGHT+1, UtilColour.UI_BORDER_COLOUR | 0xFF000000);
        pGuiGraphics.fill(X_START, Y_START, X_START + SCROLLBOX_WIDTH, Y_START + SCROLLBOX_HEIGHT, UtilColour.UI_BACKGROUND_COLOUR | 0xFF000000);
        if (damageInfoLines == null || damageInfoLines.isEmpty()) {
            pGuiGraphics.drawString(minecraft.font, UtilText.pixelFontComponent("No damage info available", false, true, false), X_START, Y_START, UtilColour.GRAY, true);
        }
        else {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            int scale = (int) minecraft.getWindow().getGuiScale();
            int SCISSOR_X_START = X_START * scale;
            int SCISSOR_Y_START = (minecraft.getWindow().getGuiScaledHeight() - Y_START - SCROLLBOX_HEIGHT) * scale;
            int SCISSOR_WIDTH = SCROLLBOX_WIDTH * scale;
            int SCISSOR_HEIGHT = (SCROLLBOX_HEIGHT - 3) * scale; // -3 to account for the border
            GL11.glScissor(SCISSOR_X_START, SCISSOR_Y_START, SCISSOR_WIDTH, SCISSOR_HEIGHT);
            for (int i = 0; i < damageInfoLines.size(); i++) {
                // Draw each line of damage info
                int currentY = Y_START + ((i * LINE_HEIGHT) - DAMAGE_INFO_CURRENT_SCROLL_POSITION);
                pGuiGraphics.drawString(minecraft.font, damageInfoLines.get(i), X_START, currentY, UtilColour.LIGHT_GRAY, true);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private void drawArmourInfo(GuiGraphics pGuiGraphics, int dividerY) {
        //bounds of the where we will write the damage info, if the amount of lines exceeds this, we will scroll
        int X_START = x + 13;
        int Y_START = dividerY + 5;
        double scrollPercentage = ARMOUR_INFO_MAX_SCROLL_POSITION > 0 ? (double) ARMOUR_INFO_CURRENT_SCROLL_POSITION / ARMOUR_INFO_MAX_SCROLL_POSITION : 0;
        //border + background
        pGuiGraphics.fill(X_START-1, Y_START-1, X_START + SCROLLBOX_WIDTH+1, Y_START + SCROLLBOX_HEIGHT+1, UtilColour.UI_BORDER_COLOUR | 0xFF000000);
        pGuiGraphics.fill(X_START, Y_START, X_START + SCROLLBOX_WIDTH, Y_START + SCROLLBOX_HEIGHT, UtilColour.UI_BACKGROUND_COLOUR | 0xFF000000);
        if (armourInfoLines == null || armourInfoLines.isEmpty()) {
            pGuiGraphics.drawString(minecraft.font, UtilText.pixelFontComponent("No armour info available", false, true, false), X_START, Y_START, UtilColour.GRAY, true);
        }
        else {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            int scale = (int) minecraft.getWindow().getGuiScale();
            int SCISSOR_X_START = X_START * scale;
            int SCISSOR_Y_START = (minecraft.getWindow().getGuiScaledHeight() - Y_START - SCROLLBOX_HEIGHT) * scale;
            int SCISSOR_WIDTH = SCROLLBOX_WIDTH * scale;
            int SCISSOR_HEIGHT = (SCROLLBOX_HEIGHT - 3) * scale; // -3 to account for the border
            GL11.glScissor(SCISSOR_X_START, SCISSOR_Y_START, SCISSOR_WIDTH, SCISSOR_HEIGHT);
            for (int i = 0; i < armourInfoLines.size(); i++) {
                // Draw each line of damage info
                int currentY = Y_START + ((i * LINE_HEIGHT) - ARMOUR_INFO_CURRENT_SCROLL_POSITION);
                pGuiGraphics.drawString(minecraft.font, armourInfoLines.get(i), X_START, currentY, UtilColour.LIGHT_GRAY, true);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private List<Component> getDamageInfoLines() {
        List<Component> retList = new ArrayList<>();
        double weaponDamage = weaponInfo.getSpread().getDamageFromElementRatio(player.getAttributeValue(Attributes.ATTACK_DAMAGE), this.element);
        if (weaponDamage > 0) {
            retList.add(UtilText.pixelFontComponent("+" + Util.roundToString(weaponDamage, 1) + " - Weapon Damage", false, false, true));
        }
        AttributeInstance baseDamageInstance = player.getAttribute(element.baseDamage());
        if (baseDamageInstance.getValue() != 0) {
            retList.add(UtilText.pixelFontComponent("+" + Util.roundToString(baseDamageInstance.getValue(), 1) + " - Base Damage", false, false, true));
            if (!baseDamageInstance.getModifiers().isEmpty()) {
                for (AttributeModifier modifier : baseDamageInstance.getModifiers()) {
                    switch (modifier.getOperation()) {
                        case ADDITION -> retList.add(additionLine(modifier.getAmount(), I18n.get(modifier.getName())));
                        case MULTIPLY_BASE -> retList.add(baseMultLine(modifier.getAmount() * 100, I18n.get(modifier.getName())));
                        case MULTIPLY_TOTAL -> retList.add(totalMultLine(modifier.getAmount() * 100, I18n.get(modifier.getName())));
                    }
                }
            }
        }
        AttributeInstance affinityInstance = player.getAttribute(element.affinity());
        if (affinityInstance.getValue() != 0) {
            retList.add(UtilText.pixelFontComponent("x" + Util.roundToString(affinityInstance.getValue(), 1) + " - Affinity", false, false, true));
            if (!affinityInstance.getModifiers().isEmpty()) {
                for (AttributeModifier modifier : affinityInstance.getModifiers()) {
                    switch (modifier.getOperation()) {
                        case ADDITION -> retList.add(additionLine(modifier.getAmount(), I18n.get(modifier.getName())));
                        case MULTIPLY_BASE -> retList.add(baseMultLine(modifier.getAmount() * 100, I18n.get(modifier.getName())));
                        case MULTIPLY_TOTAL -> retList.add(totalMultLine(modifier.getAmount() * 100, I18n.get(modifier.getName())));
                    }
                }
            }
        }
        int contentHeight = retList.size() * LINE_HEIGHT;
        DAMAGE_INFO_MAX_SCROLL_POSITION = Math.max(DAMAGE_INFO_MIN_SCROLL_POSITION, contentHeight - SCROLLBOX_HEIGHT);
        return retList;
    }

    private List<Component> getArmourInfoLines() {
        List<Component> retList = new ArrayList<>();
        AttributeInstance resist = player.getAttribute(element.resist());
        if (resist.getValue() != 0) {
            retList.add(UtilText.pixelFontComponent("+" + Util.roundToString(resist.getValue(), 1) + " - Resistance", false, false, true));
            if (!resist.getModifiers().isEmpty()) {
                for (AttributeModifier modifier : resist.getModifiers()) {
                    switch (modifier.getOperation()) {
                        case ADDITION -> retList.add(additionLine(modifier.getAmount(), I18n.get(modifier.getName())));
                        case MULTIPLY_BASE -> retList.add(baseMultLine(modifier.getAmount() * 100, I18n.get(modifier.getName())));
                        case MULTIPLY_TOTAL -> retList.add(totalMultLine(modifier.getAmount() * 100, I18n.get(modifier.getName())));
                    }
                }
            }
        }
        int contentHeight = retList.size() * LINE_HEIGHT;
        ARMOUR_INFO_MAX_SCROLL_POSITION = Math.max(ARMOUR_INFO_MIN_SCROLL_POSITION, contentHeight - SCROLLBOX_HEIGHT);
        return retList;
    }

    private Component additionLine(double number, String reason) {
        return UtilText.pixelFontComponent("        " + number + " - " + reason, false, false, true);
    }

    private Component baseMultLine(double number, String reason) {
        return UtilText.pixelFontComponent("        +" + Util.roundToString(number, 1) + "% - " + reason, false, false, true);
    }

    private Component totalMultLine(double number, String reason) {
        return UtilText.pixelFontComponent("        " + Util.roundToString(number, 1) + "% - " + reason, false, false, true);
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
