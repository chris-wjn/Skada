package com.cwjn.skada.client.gui.screen.pages;

import com.cwjn.skada.client.gui.button.ElementInfoButton;
import com.cwjn.skada.client.gui.screen.JournalPage;
import com.cwjn.skada.client.gui.screen.StatScreen;
import com.cwjn.skada.client.gui.screen.widget.ElementInfoPanel;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.Element;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.opengl.GL11;

import static com.cwjn.skada.SkadaRegistry.ELEMENTS;
import static com.cwjn.skada.client.gui.screen.StatScreen.*;

@OnlyIn(Dist.CLIENT)
public class DamageInfoPage extends JournalPage {
    private static final int SCROLLBAR_X_OFFSET = 274;
    private static final int SCROLLBAR_TRACK_Y_OFFSET = -2;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_HEIGHT = 23;
    private static final int SCISSOR_Y_OFFSET = -3;
    private static final int SCROLLBAR_TRAVEL = SCROLLBOX_HEIGHT - 25;
    private static final int SCROLLBAR_TRACK_HEIGHT = SCROLLBAR_TRAVEL + SCROLLBAR_HEIGHT;
    private static final int ELEMENT_PANEL_X_OFFSET = 45;
    private final List<ElementInfoButton> elementInfoButtons = new ArrayList<>();
    private ElementInfoPanel currentInfoWidget;
    private int currentScrollPos = 0;
    private int maxScrollPos;
    private int scrollBoxTop, scrollBoxBot;
    private int SCROLLBOX_X_OFFSET;
    private int SCROLLBOX_Y_OFFSET;
    private boolean draggingScrollbar;
    private int scrollbarDragOffsetY;

    public DamageInfoPage(ResourceLocation icon, ResourceLocation pageResource, StatScreen screen) {
        super(icon, pageResource, screen);
    }

    @Override
    public void init() {
        this.scrollBoxTop = screen.getBookLocalY() + 65;
        this.scrollBoxBot = scrollBoxTop + SCROLLBOX_HEIGHT - 3;
        SCROLLBOX_X_OFFSET = screen.getBookLocalX() +51;
        SCROLLBOX_Y_OFFSET = scrollBoxTop-13;

        elementInfoButtons.clear();
        createElementInfoButtons(screen.getBookLocalX(), screen.getWeaponInfo());

        int totalContentHeight = 48 * elementInfoButtons.size() - 4;
        maxScrollPos = Math.max(0, totalContentHeight - SCROLLBOX_HEIGHT) + 6;
    }

    private void createElementInfoButtons(int left, WeaponInfo weaponInfo) {
        for (RegistryObject<Element> element : ELEMENTS.getEntries()) {
            ElementInfoButton button = new ElementInfoButton(0, 0, Component.empty(), element.get(), this::toggleElementInfoWidget, weaponInfo);
            ElementInfoPanel panel = new ElementInfoPanel(screen, left + BOOK_RIGHT_PAGE_OFFSET_X + ELEMENT_PANEL_X_OFFSET, scrollBoxTop-13, element.get(), weaponInfo);
            panel.visible = false;
            button.setWidget(panel);

            screen.addRenderableWidget(button);

            elementInfoButtons.add(button);
        }
        elementInfoButtons.sort(Comparator.comparing(ElementInfoButton::getElement));
    }

    private void toggleElementInfoWidget(Button b) {
        if (b instanceof ElementInfoButton button) {
            if (currentInfoWidget != null) {
                currentInfoWidget.visible = false;
            }
            button.getWidget().visible = true;
            currentInfoWidget = button.getWidget();
            CONTENT_APPEAR_ANIM_RIGHT_PAGE_ONLY.reset();
            screen.anim = CONTENT_APPEAR_ANIM_RIGHT_PAGE_ONLY;
        }
    }

    @Override
    public boolean handleMouseScrolled(double mouseX, double mouseY, double pDelta) {
        if (mouseX > SCROLLBOX_X_OFFSET && mouseX < SCROLLBOX_X_OFFSET + 229
                && mouseY > scrollBoxTop && mouseY < scrollBoxBot) {
            int actualDelta = (int) (pDelta * -5);
            setScrollPosition(currentScrollPos + actualDelta);
            return true;
        }
        else {
            if (currentInfoWidget == null || !currentInfoWidget.visible) return false;
            return currentInfoWidget.handleScroll(mouseX, mouseY, pDelta);
        }
    }

    @Override
    public boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (!isWithin(mouseX, mouseY, scrollbarTrackX(), scrollbarTrackY(), SCROLLBAR_WIDTH, SCROLLBAR_TRACK_HEIGHT)) return false;

        draggingScrollbar = true;
        int thumbY = scrollbarThumbY();
        if (isWithin(mouseX, mouseY, scrollbarTrackX(), thumbY, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT)) {
            scrollbarDragOffsetY = (int) mouseY - thumbY;
        }
        else {
            scrollbarDragOffsetY = SCROLLBAR_HEIGHT / 2;
            updateScrollFromMouse(mouseY);
        }
        return true;
    }

    @Override
    public boolean handleMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!draggingScrollbar || button != 0) return false;
        updateScrollFromMouse(mouseY);
        return true;
    }

    @Override
    public boolean handleMouseReleased(double mouseX, double mouseY, int button) {
        if (!draggingScrollbar || button != 0) return false;
        draggingScrollbar = false;
        return true;
    }

    public void clearButtons() {
        elementInfoButtons.clear();
        draggingScrollbar = false;
    }

    @Override
    public void isRender(boolean render) {
        super.isRender(render);
        draggingScrollbar = false;
        for (ElementInfoButton button : elementInfoButtons) {
            button.visible = render && button.visible;
            button.active = render && button.active;
        }
        if (currentInfoWidget != null) {
            currentInfoWidget.visible = render;
        }
    }

    @Override
    public void renderExtraDrawables(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        int ELEMENT_BUTTON_HEIGHT = 44;

        pGuiGraphics.blit(DAMAGE_PAGE_SCROLLBOX_BORDER, SCROLLBOX_X_OFFSET, SCROLLBOX_Y_OFFSET, 0, 0, 229, 293, 229, 293);
        pGuiGraphics.blit(SCROLLBAR, scrollbarTrackX(), scrollbarThumbY(), 0, 0, SCROLLBAR_WIDTH,
            SCROLLBAR_HEIGHT, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);
        if (currentInfoWidget != null) currentInfoWidget.render(pGuiGraphics);

        screen.enableScissor(screen.getBookLocalX(), scrollBoxTop + SCISSOR_Y_OFFSET, BOOK_WIDTH, SCROLLBOX_HEIGHT);

        for (int i = 0; i < elementInfoButtons.size(); i++) {
            ElementInfoButton button = elementInfoButtons.get(i);
            int itemTop = scrollBoxTop + (i * 48 - currentScrollPos);
            button.setX(screen.getBookLocalX() + 80);
            button.setY(itemTop);

            boolean isVisible = itemTop < scrollBoxBot && itemTop + ELEMENT_BUTTON_HEIGHT > scrollBoxTop;
            button.visible = isVisible;
            button.setIsHovered(isVisible
                    && pMouseY <= scrollBoxBot
                    && pMouseY >= scrollBoxTop
                    && pMouseX >= button.getX()
                    && pMouseY >= button.getY()
                    && pMouseX < button.getX() + button.getWidth()
                    && pMouseY < button.getY() + button.getHeight());

            if (isVisible) {
                button.renderCustom(pGuiGraphics);
                button.active = true;
            } else {
                button.active = false;
            }
        }
        screen.disableScissor();
        for (ElementInfoButton button : elementInfoButtons) {
            if (!button.visible) continue;
            int top = Math.max(scrollBoxTop, button.getY());
            int bottom = Math.min(scrollBoxBot, button.getY() + 44);
            if (bottom <= top) continue;
            var element = button.getElement();
            var player = Minecraft.getInstance().player;
            if (player == null) continue;
            screen.offerTooltip(button.getX(), top, 171, bottom - top, pMouseX, pMouseY,
                com.cwjn.skada.client.gui.StatHelp.lines("element_damage", null));
            int defenceTop = Math.max(top, button.getY() + 22);
            if (bottom <= defenceTop) continue;
            screen.offerTooltip(button.getX() + 100, defenceTop, 71, bottom - defenceTop, pMouseX, pMouseY,
                com.cwjn.skada.client.gui.StatHelp.lines(element.equals(Element.physical()) ? "physical_resist" : "element_resist",
                    player.getAttributeValue(element.resist())));
        }
        if (currentInfoWidget != null) currentInfoWidget.offerHelp(pMouseX, pMouseY);
    }

    private int scrollbarTrackX() {
        return screen.getBookLocalX() + SCROLLBAR_X_OFFSET;
    }

    private int scrollbarTrackY() {
        return scrollBoxTop + SCROLLBAR_TRACK_Y_OFFSET;
    }

    private int scrollbarThumbY() {
        double scrollPercentage = maxScrollPos > 0 ? (double) currentScrollPos / maxScrollPos : 0;
        return scrollbarTrackY() + (int) (scrollPercentage * SCROLLBAR_TRAVEL);
    }

    private void updateScrollFromMouse(double mouseY) {
        int trackTop = scrollBoxTop + SCROLLBAR_TRACK_Y_OFFSET;
        int travel = SCROLLBAR_TRAVEL;
        if (travel <= 0 || maxScrollPos == 0) {
            setScrollPosition(0);
            return;
        }
        int thumbTop = net.minecraft.util.Mth.clamp((int) mouseY - scrollbarDragOffsetY, trackTop, trackTop + travel);
        double progress = (double) (thumbTop - trackTop) / (double) travel;
        setScrollPosition((int) Math.round(progress * maxScrollPos));
    }

    private void setScrollPosition(int scrollPosition) {
        currentScrollPos = net.minecraft.util.Mth.clamp(scrollPosition, 0, maxScrollPos);
    }

    private static boolean isWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
