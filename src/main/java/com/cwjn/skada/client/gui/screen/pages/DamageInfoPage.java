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
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.cwjn.skada.SkadaRegistry.ELEMENTS;
import static com.cwjn.skada.client.gui.screen.StatScreen.*;

@OnlyIn(Dist.CLIENT)
public class DamageInfoPage extends JournalPage {
    private final List<ElementInfoButton> elementInfoButtons = new ArrayList<>();
    private ElementInfoPanel currentInfoWidget;
    private int currentScrollPos = 0;
    private int maxScrollPos;
    private int scrollBoxTop, scrollBoxBot;
    private int SCROLLBOX_X_OFFSET;
    private int SCROLLBOX_Y_OFFSET;

    public DamageInfoPage(ResourceLocation icon, ResourceLocation pageResource, StatScreen screen) {
        super(icon, pageResource, screen);
    }

    @Override
    public void init() {
        this.scrollBoxTop = screen.getBookY() + 65;
        this.scrollBoxBot = scrollBoxTop + SCROLLBOX_HEIGHT - 3;
        SCROLLBOX_X_OFFSET = screen.getBookX() +51;
        SCROLLBOX_Y_OFFSET = scrollBoxTop-13;

        elementInfoButtons.clear();
        createElementInfoButtons(screen.getBookX(), screen.getWeaponInfo());

        int totalContentHeight = 48 * elementInfoButtons.size() - 4;
        maxScrollPos = Math.max(0, totalContentHeight - SCROLLBOX_HEIGHT) + 6;
    }

    private void createElementInfoButtons(int left, WeaponInfo weaponInfo) {
        for (RegistryObject<Element> element : ELEMENTS.getEntries()) {
            ElementInfoButton button = new ElementInfoButton(0, 0, Component.empty(), element.get(), this::toggleElementInfoWidget, weaponInfo);
            ElementInfoPanel panel = new ElementInfoPanel(left + BOOK_RIGHT_PAGE_OFFSET_X, scrollBoxTop-13, element.get(), weaponInfo);
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
            if (currentScrollPos + actualDelta <= 0) currentScrollPos = 0;
            else if (currentScrollPos + actualDelta >= maxScrollPos) currentScrollPos = maxScrollPos;
            else currentScrollPos += actualDelta;
            return true;
        }
        else {
            if (currentInfoWidget == null || !currentInfoWidget.visible) return false;
            return currentInfoWidget.handleScroll(mouseX, mouseY, pDelta);
        }
    }

    public void clearButtons() {
        elementInfoButtons.clear();
    }

    @Override
    public void renderExtraDrawables(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        int ELEMENT_BUTTON_HEIGHT = 44;
        double scrollPercentage = maxScrollPos > 0 ? (double) currentScrollPos / maxScrollPos : 0;

        pGuiGraphics.blit(DAMAGE_PAGE_SCROLLBOX_BORDER, SCROLLBOX_X_OFFSET, SCROLLBOX_Y_OFFSET, 0, 0, 229, 293, 229, 293);
        pGuiGraphics.blit(SCROLLBAR, screen.getBookX() + 274, (scrollBoxTop - 2) + (int)(scrollPercentage * (SCROLLBOX_HEIGHT - 25)), 0, 0, 5, 23, 5, 23);
        if (currentInfoWidget != null) currentInfoWidget.render(pGuiGraphics);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scale = (int) Minecraft.getInstance().getWindow().getGuiScale();
        GL11.glScissor(screen.getBookX() * scale, Minecraft.getInstance().getWindow().getScreenHeight() - scrollBoxBot * scale, BOOK_WIDTH * scale, SCROLLBOX_HEIGHT * scale);

        for (int i = 0; i < elementInfoButtons.size(); i++) {
            ElementInfoButton button = elementInfoButtons.get(i);
            int itemTop = scrollBoxTop + (i * 48 - currentScrollPos);
            button.setX(screen.getBookX() + 80);
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

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
