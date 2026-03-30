package com.cwjn.skada.client.gui.screen.widget;

import com.cwjn.skada.util.UtilColour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class InfoPanel {

    private final int x, y;
    private int WIDTH = 234;
    private final String topic;
    public boolean visible = false;

    public InfoPanel(int x, int y, String topic) {
        this.x = x;
        this.y = y;
        this.topic = topic;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        // Render border
        graphics.fill(x-1, y-1, x + WIDTH + 1, y + 293 + 1, UtilColour.UI_BORDER_COLOUR | 0xFF000000);
        // Render panel background
        graphics.fill(x, y, x + WIDTH, y + 293, UtilColour.UI_BACKGROUND_COLOUR | 0xFF000000);


        // Render title
        graphics.drawString(Minecraft.getInstance().font, Component.literal(topic),
                x + 10, y + 10, 0xFFFFFF);

        // Render content
        renderContent(graphics);
    }

    private void renderContent(GuiGraphics graphics) {
        String[] content = getContentForTopic(topic);
        int yOffset = 30;

        for (String line : content) {
            graphics.drawString(Minecraft.getInstance().font, Component.literal(line),
                    x + 10, y + yOffset, 0xCCCCCC);
            yOffset += 12;
        }
    }

    private String[] getContentForTopic(String topic) {
        return switch (topic) {
            case "Combat Basics" -> new String[]{
                    "• placeholder"
            };
            case "Weapon Types" -> new String[]{
                    "• placeholder"
            };
            case "Element System" -> new String[]{
                    "• placeholder"
            };
            case "Status Effects" -> new String[]{
                    "• placeholder"
            };
            case "Damage Mechanics" -> new String[]{
                    "• placeholder"
            };
            case "Defense Stats" -> new String[]{
                    "• placeholder"
            };
            default -> new String[]{"No information available"};
        };
    }

    public boolean handleScroll(double mouseX, double mouseY, double delta) {
        // Implement content scrolling if needed
        return false;
    }
}
