package com.cwjn.skada.client.gui.screen;

import com.cwjn.skada.SkadaRegistry;
import com.cwjn.skada.client.gui.button.TabButton;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.AccessPlayerWeaponInfo;
import com.cwjn.skada.util.Keybinds;
import com.cwjn.skada.util.Util;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static com.cwjn.skada.SkadaRegistry.ELEMENTS;

public class StatScreen extends Screen {

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Player player = Minecraft.getInstance().player;
    private final WeaponInfo weaponInfo = ((AccessPlayerWeaponInfo) player).getWeaponInfo();
    private final AttackType attackType = weaponInfo.getAttackTypes().keySet().toArray(AttackType[]::new)[((AccessPlayerWeaponInfo) player).getAttackTypeIndex()];
    private final AttackTypeInfo attackTypeInfo = weaponInfo.getAttackTypes().get(attackType);
    private final static int BOOK_WIDTH = 695;
    private final static int BOOK_HEIGHT = 417;
    private final static int BOOK_TURNING_HEIGHT = 457;
    private final static int BOOK_OPENING_HEIGHT = 507;
    private final static int BOOK_TEXTURE_OFFSET_X = 118;
    private static final Animation FLIP_LEFT_ANIM = new Animation("textures/gui/book_flip_left/", 8, 24, 140, -41, BOOK_TURNING_HEIGHT);
    private static final Animation FLIP_RIGHT_ANIM = new Animation("textures/gui/book_flip_right/", 8, 24, 140, -41, BOOK_TURNING_HEIGHT);
    private static final Animation OPEN_ANIM = new Animation("textures/gui/book_open/", 5, 24, 85, -96, BOOK_OPENING_HEIGHT);
    private static final Animation CONTENT_APPEAR_ANIM = new Animation("textures/gui/book_content_appear/", 24, 6, 181, 0, BOOK_HEIGHT);
    private static final Animation TABS_APPEAR_ANIM = new Animation("textures/gui/book_tabs_appear/", 13, 16, 181, 0, BOOK_HEIGHT);
    private static final ResourceLocation[] GUI_BOOK = new ResourceLocation[]{
            Util.rl("textures/gui/book_idle/none_selected.png"),
            Util.rl("textures/gui/book_idle/1.png"),
            Util.rl("textures/gui/book_idle/2.png"),
            Util.rl("textures/gui/book_idle/3.png"),
            Util.rl("textures/gui/book_idle/4.png"),
            Util.rl("textures/gui/book_idle/5.png"),
            Util.rl("textures/gui/book_idle/6.png")
    };
    private static final ResourceLocation ELEMENT_INFO_PANEL = Util.rl("textures/gui/book_idle/element_info_panel.png");
    private static final ResourceLocation SCROLLBAR = Util.rl("textures/gui/book_idle/scrollbar.png");
    private Animation anim = OPEN_ANIM;
    private ResourceLocation currentFrame;
    private int DISPLAY_STATE = 0; //0=stats, 1=offensive, 2=defensive, 3=info
    private int w, h, left, top, timer = 0;
    private final int SCROLLBOX_HEIGHT = 273;
    private int scrollBoxTop, scrollBoxBot;
    private static final List<Element> elements = new ArrayList<>();
    static {
        for (RegistryObject<Element> element : ELEMENTS.getEntries()) {
            elements.add(element.get());
        }
        elements.sort(Element::compareTo);
    }
    private final int damageInfoHeight = 48*elements.size()-4;
    private int currentScrollPos = 0; //btwn 0 and maxScrollPos
    private int maxScrollPos;
    private final double savedScale;
    private final Screen savedScreen;
    private final TabButton[] tabs = new TabButton[4];
    private int vitPreview = (int) player.getAttributeBaseValue(SkadaRegistry.VITALITY.get().attribute());
    private int strPreview = (int) player.getAttributeBaseValue(SkadaRegistry.STRENGTH.get().attribute());
    private int dexPreview = (int) player.getAttributeBaseValue(SkadaRegistry.DEXTERITY.get().attribute());
    private int aglPreview = (int) player.getAttributeBaseValue(SkadaRegistry.AGILITY.get().attribute());
    private int intPreview = (int) player.getAttributeBaseValue(SkadaRegistry.INTELLIGENCE.get().attribute());
    private int wisPreview = (int) player.getAttributeBaseValue(SkadaRegistry.WISDOM.get().attribute());
    private int fthPreview = (int) player.getAttributeBaseValue(SkadaRegistry.FAITH.get().attribute());

    static {
        OPEN_ANIM.setSoundEffect(SoundEvents.BOOK_PAGE_TURN);
        FLIP_LEFT_ANIM.setSoundEffect(SoundEvents.BOOK_PAGE_TURN);
        FLIP_LEFT_ANIM.setNextAnim(CONTENT_APPEAR_ANIM);
        FLIP_RIGHT_ANIM.setNextAnim(CONTENT_APPEAR_ANIM);
        FLIP_RIGHT_ANIM.setSoundEffect(SoundEvents.BOOK_PAGE_TURN);
        OPEN_ANIM.setNextAnim(TABS_APPEAR_ANIM);
        TABS_APPEAR_ANIM.setNextAnim(CONTENT_APPEAR_ANIM);
        OPEN_ANIM.addDelay(160);
        TABS_APPEAR_ANIM.addDelay(130);
    }

    public StatScreen() {
        super(Component.literal("Stat Screen"));
        savedScale = minecraft.getWindow().getGuiScale();
        savedScreen = minecraft.screen;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        minecraft.getWindow().setGuiScale(savedScale);
        minecraft.setScreen(savedScreen);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == Keybinds.statScreen.getKey().getValue() || pKeyCode == KeyEvent.VK_E) {
            minecraft.getWindow().setGuiScale(savedScale);
            minecraft.setScreen(savedScreen);
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    protected void init() {
        super.init();
        minecraft.getWindow().setGuiScale(2);
        w = minecraft.getWindow().getGuiScaledWidth();
        h = minecraft.getWindow().getGuiScaledHeight();
        left = (w - BOOK_WIDTH)/2;
        top = (h - BOOK_HEIGHT)/2;
        scrollBoxTop = top+65;
        scrollBoxBot = scrollBoxTop+SCROLLBOX_HEIGHT-3;
        maxScrollPos = Math.max(0,damageInfoHeight-SCROLLBOX_HEIGHT)+6;
        tabs[0] = new TabButton(left+654, top+66, Component.empty(), b -> setDisplayState(0));
        tabs[1] = new TabButton(left+654, top+66+38, Component.empty(), b -> setDisplayState(1));
        tabs[2] = new TabButton(left+654, top+66+38+38, Component.empty(), b -> setDisplayState(2));
        tabs[3] = new TabButton(left+654, top+66+38+38+38, Component.empty(), b -> setDisplayState(3));
        for (TabButton tab : tabs) addRenderableWidget(tab);
        tabs[DISPLAY_STATE].active = false;
    }

    private void setDisplayState(int newState) {
        if (DISPLAY_STATE == newState) return;
        tabs[DISPLAY_STATE].active = true;
        tabs[newState].active = false;
        anim = DISPLAY_STATE < newState? FLIP_RIGHT_ANIM : FLIP_LEFT_ANIM;
        DISPLAY_STATE = newState;
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics) {
        super.renderBackground(pGuiGraphics);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        timer++;
        if (anim == null) {
            drawFromState(pGuiGraphics);
        }
        else if (anim == CONTENT_APPEAR_ANIM) {
            drawFromState(pGuiGraphics);
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate(0, 0, 1);
            pGuiGraphics.blit(anim.getCurrentFrameThenIterate(timer), left, top+anim.getYOffset(), BOOK_TEXTURE_OFFSET_X, anim.getVOffset(), BOOK_WIDTH, anim.getHeight(), 896, 720);
            pGuiGraphics.pose().popPose();
            if (anim.isComplete()) {
                timer = 0;
                anim.reset();
                if (anim.getNextAnim() != null) anim = anim.getNextAnim();
                else anim = null;
            }
        }
        else {
            pGuiGraphics.blit(anim.getCurrentFrameThenIterate(timer), left, top+anim.getYOffset(), BOOK_TEXTURE_OFFSET_X, anim.getVOffset(), BOOK_WIDTH, anim.getHeight(), 896, 720);
            if (anim.isComplete()) {
                timer = 0;
                anim.reset();
                if (anim.getNextAnim() != null) anim = anim.getNextAnim();
                else anim = null;
            }
        }
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    private void drawFromState(GuiGraphics pGuiGraphics) {
        switch (DISPLAY_STATE) {
            case 0:
                statPage(pGuiGraphics);
                break;
            case 1:
                damagePage(pGuiGraphics);
                break;
            case 2:
                pGuiGraphics.blit(GUI_BOOK[3], left, top, BOOK_TEXTURE_OFFSET_X, 181, BOOK_WIDTH, BOOK_HEIGHT, 896, 720);
                break;
            case 3:
                pGuiGraphics.blit(GUI_BOOK[4], left, top, BOOK_TEXTURE_OFFSET_X, 181, BOOK_WIDTH, BOOK_HEIGHT, 896, 720);
                break;
        }
    }

    private void statPage(GuiGraphics pGuiGraphics) {
        pGuiGraphics.blit(GUI_BOOK[1], left, top, BOOK_TEXTURE_OFFSET_X, 181, BOOK_WIDTH, BOOK_HEIGHT, 896, 720);
        int y = top+47;
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent(String.format("%02d", vitPreview), true, true), left+256, y, 0xCA8E61, false);
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent("Vitality", false, true), left+50, y+1, 0xCA8E61);
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent(String.format("%02d", strPreview), true, true), left+256, y+=24, 0xCA8E61, false);
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent("Strength", false, true), left+50, y+1, 0xCA8E61);
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent(String.format("%02d", dexPreview), true, true), left+256, y+=24, 0xCA8E61, false);
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent("Dexterity", false, true), left+50, y+1, 0xCA8E61);
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent(String.format("%02d", aglPreview), true, true), left+256, y+=24, 0xCA8E61, false);
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent("Agility", false, true), left+50, y+1, 0xCA8E61);
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent(String.format("%02d", intPreview), true, true), left+256, y+=24, 0xCA8E61, false);
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent("Intelligence", false, true), left+50, y+1, 0xCA8E61);
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent(String.format("%02d", wisPreview), true, true), left+256, y+=24, 0xCA8E61, false);
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent("Wisdom", false, true), left+50, y+1, 0xCA8E61);
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent(String.format("%02d", fthPreview), true, true), left+256, y+=24, 0xCA8E61, false);
        pGuiGraphics.drawString(minecraft.font, Util.pixelFontComponent("Faith", false, true), left+50, y+1, 0xCA8E61);
    }

    private void damagePage(GuiGraphics pGuiGraphics) {
        pGuiGraphics.blit(GUI_BOOK[2], left, top, BOOK_TEXTURE_OFFSET_X, 181, BOOK_WIDTH, BOOK_HEIGHT, 896, 720);
        int startIndex = currentScrollPos/44;
        int endIndex = Math.min(startIndex+(SCROLLBOX_HEIGHT+44)/44, elements.size());
        double scrollPercentage = (double) currentScrollPos/maxScrollPos;
        pGuiGraphics.blit(SCROLLBAR, left+274, (scrollBoxTop-2) + (int)(scrollPercentage*(SCROLLBOX_HEIGHT-25)), 0, 0, 5, 23, 5, 23);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scale = (int) minecraft.getWindow().getGuiScale();
        GL11.glScissor(left*scale, minecraft.getWindow().getScreenHeight() - scrollBoxBot*scale, BOOK_WIDTH*scale, SCROLLBOX_HEIGHT*scale);
        for (int i = startIndex; i < endIndex; i++) {
            int itemTop = scrollBoxTop+(i*48-currentScrollPos);
            pGuiGraphics.blit(ELEMENT_INFO_PANEL, left+80, itemTop, 0, 0, 171, 44, 171, 44);
            pGuiGraphics.blit(elements.get(i).icon(), left+80+10, itemTop+14, 0, 0, 16, 16, 16, 16);
            pGuiGraphics.drawString(minecraft.font,
                    Util.pixelFontComponent(StringUtils.capitalize(elements.get(i).name()), false, true),
                    left+80+28, itemTop+15, elements.get(i).colour(), true);
            pGuiGraphics.drawString(minecraft.font,
                    Util.pixelFontComponent(Util.roundToString(
                            player.getAttributeValue(elements.get(i).baseDamage()) + weaponInfo.getSpread().getDamageFromElementRatio(player.getAttributeValue(Attributes.ATTACK_DAMAGE), elements.get(i)),
                                    1),
                            false, true), left+80+130, itemTop+15, elements.get(i).colour(), true);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        int actualDelta = (int) (pDelta * -10);
        if (currentScrollPos + actualDelta <= 0) currentScrollPos = 0;
        else if (currentScrollPos + actualDelta >= maxScrollPos) currentScrollPos = maxScrollPos;
        else currentScrollPos += actualDelta;
        return true;
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
