package com.cwjn.skada.client.gui.screen.pages;

import com.cwjn.skada.client.gui.button.InfoTileButton;
import com.cwjn.skada.client.gui.screen.JournalPage;
import com.cwjn.skada.client.gui.screen.StatScreen;
import com.cwjn.skada.client.gui.screen.widget.InfoPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class InfoPage extends JournalPage {

    private final List<InfoTileButton> infoButtons = new ArrayList<>();
    private InfoPanel currentInfoPanel;
    private int currentScrollPos = 0;
    private int maxScrollPos;
    private int scrollBoxTop, scrollBoxBot;
    private int SCROLLBOX_X_OFFSET;
    private int SCROLLBOX_Y_OFFSET;

    public InfoPage(ResourceLocation icon, ResourceLocation pageResource, StatScreen screen) {
        super(icon, pageResource, screen);
    }

    @Override
    public void init() {
        this.scrollBoxTop = screen.getBookY() + 65;
        this.scrollBoxBot = scrollBoxTop + StatScreen.SCROLLBOX_HEIGHT - 3;
        SCROLLBOX_X_OFFSET = screen.getBookX() + 51;
        SCROLLBOX_Y_OFFSET = scrollBoxTop - 13;

        infoButtons.clear();
        createInfoTiles();

        int totalContentHeight = 48 * infoButtons.size() - 4;
        maxScrollPos = Math.max(0, totalContentHeight - StatScreen.SCROLLBOX_HEIGHT) + 6;
    }

    private void createInfoTiles() {
        String[] infoTopics = {
                "Combat Basics",
                "Weapon Types",
                "Element System",
                "Status Effects",
                "Damage Mechanics",
                "Defense Stats"
        };

        for (String topic : infoTopics) {
            InfoTileButton button = new InfoTileButton(0, 0, Component.literal(topic), this::showInfoPanel);
            InfoPanel panel = new InfoPanel(screen.getBookX() + StatScreen.BOOK_RIGHT_PAGE_OFFSET_X+1,
                    scrollBoxTop - 13, topic);
            panel.visible = false;
            button.setInfoPanel(panel);

            screen.addRenderableWidget(button);

            infoButtons.add(button);
        }
    }

    private void showInfoPanel(Button button) {
        if (button instanceof InfoTileButton tileButton) {
            if (currentInfoPanel != null) {
                currentInfoPanel.visible = false;
            }
            tileButton.getInfoPanel().visible = true;
            currentInfoPanel = tileButton.getInfoPanel();
            StatScreen.CONTENT_APPEAR_ANIM_RIGHT_PAGE_ONLY.reset();
            screen.anim = StatScreen.CONTENT_APPEAR_ANIM_RIGHT_PAGE_ONLY;
        }
    }

    @Override
    public boolean handleMouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX > SCROLLBOX_X_OFFSET && mouseX < SCROLLBOX_X_OFFSET + 229
                && mouseY > scrollBoxTop && mouseY < scrollBoxBot) {
            int scrollAmount = (int) (delta * -5);
            if (currentScrollPos + scrollAmount <= 0) currentScrollPos = 0;
            else if (currentScrollPos + scrollAmount >= maxScrollPos) currentScrollPos = maxScrollPos;
            else currentScrollPos += scrollAmount;
            return true;
        } else if (currentInfoPanel != null) {
            return currentInfoPanel.handleScroll(mouseX, mouseY, delta);
        }
        return false;
    }

    @Override
    public void clearButtons() {
        infoButtons.clear();
        if (currentInfoPanel != null) {
            currentInfoPanel.visible = false;
            currentInfoPanel = null;
        }
    }

    @Override
    public void renderExtraDrawables(GuiGraphics graphics, int mouseX, int mouseY) {
        int TILE_HEIGHT = 44;
        double scrollPercentage = maxScrollPos > 0 ? (double) currentScrollPos / maxScrollPos : 0;

        // Render scrollbox border and scrollbar
        graphics.blit(StatScreen.DAMAGE_PAGE_SCROLLBOX_BORDER, SCROLLBOX_X_OFFSET, SCROLLBOX_Y_OFFSET,
                0, 0, 229, 293, 229, 293);
        graphics.blit(StatScreen.SCROLLBAR, screen.getBookX() + 274,
                (scrollBoxTop - 2) + (int)(scrollPercentage * (StatScreen.SCROLLBOX_HEIGHT - 25)),
                0, 0, 5, 23, 5, 23);
        if (currentInfoPanel != null) {
            currentInfoPanel.render(graphics, mouseX, mouseY, 0);
        }

        // Enable scissor test for scrollable area
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scale = (int) Minecraft.getInstance().getWindow().getGuiScale();
        GL11.glScissor(screen.getBookX() * scale,
                Minecraft.getInstance().getWindow().getScreenHeight() - scrollBoxBot * scale,
                StatScreen.BOOK_WIDTH * scale, StatScreen.SCROLLBOX_HEIGHT * scale);

        // Render visible tiles
        for (int i = 0; i < infoButtons.size(); i++) {
            InfoTileButton button = infoButtons.get(i);
            int itemTop = scrollBoxTop + (i * 48 - currentScrollPos);
            button.setX(screen.getBookX() + 80);
            button.setY(itemTop);

            boolean isVisible = itemTop < scrollBoxBot && itemTop + TILE_HEIGHT > scrollBoxTop;
            button.visible = isVisible;
            button.setIsHovered(isVisible
                    && mouseY <= scrollBoxBot
                    && mouseY >= scrollBoxTop
                    && mouseX >= button.getX()
                    && mouseY >= button.getY()
                    && mouseX < button.getX() + button.getWidth()
                    && mouseY < button.getY() + button.getHeight());

            if (isVisible) {
                button.renderTile(graphics);
                button.active = true;
            } else {
                button.active = false;
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}