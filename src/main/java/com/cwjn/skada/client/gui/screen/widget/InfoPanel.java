package com.cwjn.skada.client.gui.screen.widget;

import com.cwjn.skada.util.ColourLibrary;
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
        graphics.fill(x-1, y-1, x + WIDTH + 1, y + 293 + 1, ColourLibrary.UI_BORDER_COLOUR | 0xFF000000);
        // Render panel background
        graphics.fill(x, y, x + WIDTH, y + 293, ColourLibrary.UI_BACKGROUND_COLOUR | 0xFF000000);


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
                    "• Right-click to attack",
                    "• Hold to charge attacks",
                    "• Time attacks for criticals"
            };
            case "Weapon Types" -> new String[]{
                    "• Swords: Balanced damage/speed",
                    "• Axes: High damage, slower",
                    "• Spears: Long reach attacks"
            };
            case "Element System" -> new String[]{
                    "• Fire: Burns over time",
                    "• Ice: Slows enemies",
                    "• Lightning: Chain damage"
            };
            case "Status Effects" -> new String[]{
                    "• Poison: Damage over time",
                    "• Weakness: Reduced damage",
                    "• Strength: Increased damage"
            };
            case "Damage Mechanics" -> new String[]{
                    "• Base damage from weapon",
                    "• Elements modify damage",
                    "• Armor reduces incoming damage"
            };
            case "Defense Stats" -> new String[]{
                    "• Armor reduces physical damage",
                    "• Resistance affects elements",
                    "• Toughness prevents knockback"
            };
            default -> new String[]{"No information available"};
        };
    }

    public boolean handleScroll(double mouseX, double mouseY, double delta) {
        // Implement content scrolling if needed
        return false;
    }
}