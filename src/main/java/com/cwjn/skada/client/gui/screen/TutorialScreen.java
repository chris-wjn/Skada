package com.cwjn.skada.client.gui.screen;

import com.cwjn.skada.ClientConfig;
import com.cwjn.skada.network.SkadaNetwork;
import com.cwjn.skada.network.client_to_server.C2STutorial;
import com.cwjn.skada.util.Keybinds;
import com.cwjn.skada.util.Util;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Fullscreen, pausing tutorial with stable clickable example rows and independent text scrolling. */
public final class TutorialScreen extends Screen {
    private static final int BACKGROUND = 0xFF101820, PANEL = 0xFF192732, ACCENT = 0xFFE3BD79;
    private static final String[] WEAPON = {"equipment_weight", "attack_type", "reach", "lethality", "precision", "knockback", "critical_fail", "attack_speed", "damage", "elements"};
    private static final String[] WEAPON_VALUES = {"1.2 kg", "Slash", "0.5 - 3.0", "12", "10", "0.4", "2%", "1.6", "6.0", "80% Physical / 20% Heat"};
    private static final String[] ARMOUR = {"armour", "toughness", "equipment_weight", "burden", "slash_resist", "thrust_resist", "strike_resist", "element_resist"};
    private static final String[] ARMOUR_VALUES = {"12", "8", "6.0 kg", "4", "20", "10", "30", "Heat: 5"};
    private final UUID session;
    private boolean introduction;
    private boolean confirmation;
    private boolean ended;
    private int page;
    private int selected;
    private int rowOffset;
    private int detailOffset;
    private int left, columnWidth, contentTop, contentBottom;
    private final boolean reticles;

    /** Only server-issued session IDs can enter the protected tutorial. */
    public TutorialScreen(UUID session, boolean introduction) {
        super(text("title"));
        this.session = session;
        this.introduction = introduction;
        this.reticles = ClientConfig.ENABLE_CUSTOM_RETICLES.get();
        this.page = reticles ? 0 : 1;
    }

    /** Identifies the session so stale server acknowledgements cannot close another replay. */
    public UUID session() { return session; }

    /** Uses vanilla pause behaviour in unshared singleplayer; the server separately protects multiplayer. */
    @Override public boolean isPauseScreen() { return true; }

    /** Rebuilds native-size widgets after resize, page change or confirmation. */
    @Override protected void init() {
        left = Math.max(12, (width - 760) / 2);
        columnWidth = (width - left * 2 - 16) / 2;
        contentTop = 62;
        contentBottom = height - 66;
        if (introduction || confirmation) {
            int buttonWidth = Math.min(180, (width - 48) / 2);
            int y = Math.min(height - 35, height / 2 + 44);
            addRenderableWidget(Button.builder(text(confirmation ? "skip_confirm" : "experienced"), b -> finish())
                .bounds(width / 2 - buttonWidth - 4, y, buttonWidth, 20).build());
            addRenderableWidget(Button.builder(text(confirmation ? "continue" : "new_player"), b -> {
                if (confirmation) confirmation = false;
                else introduction = false;
                rebuildWidgets();
            }).bounds(width / 2 + 4, y, buttonWidth, 20).build());
            return;
        }
        int buttonWidth = Math.min(95, (width - 48) / 3);
        Button back = Button.builder(text("back"), b -> changePage(-1)).bounds(left, height - 29, buttonWidth, 20).build();
        back.active = page > (reticles ? 0 : 1);
        addRenderableWidget(back);
        addRenderableWidget(Button.builder(text("skip"), b -> confirmExit()).bounds(width / 2 - buttonWidth / 2, height - 29, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(text(page == 2 ? "finish" : "next"), b -> {
            if (page == 2) finish(); else changePage(1);
        }).bounds(width - left - buttonWidth, height - 29, buttonWidth, 20).build());
        if (page > 0) {
            String[] stats = page == 1 ? WEAPON : ARMOUR;
            String[] values = page == 1 ? WEAPON_VALUES : ARMOUR_VALUES;
            int rows = visibleRows();
            rowOffset = Mth.clamp(rowOffset, 0, Math.max(0, stats.length - rows));
            for (int i = rowOffset; i < Math.min(stats.length, rowOffset + rows); i++) {
                final int index = i;
                Component label = Component.translatable("skada.help.name." + stats[i]);
                Component message = Component.translatable("skada.onboarding.stat_row", label, values[i]);
                Button row = new ExampleStatButton(left + 6, contentTop + 37 + (i - rowOffset) * 19,
                    columnWidth - 12, message, stats[i], selected == i,
                    b -> { selected = index; detailOffset = 0; rebuildWidgets(); });
                addRenderableWidget(row);
            }
        }
    }

    /** Reports how many rows fit, retaining full font size on small GUI viewports. */
    private int visibleRows() { return Math.max(1, (contentBottom - contentTop - 42) / 19); }

    /** Navigates without losing protection or replacing the screen session. */
    private void changePage(int direction) {
        page = Mth.clamp(page + direction, reticles ? 0 : 1, 2);
        selected = rowOffset = detailOffset = 0;
        rebuildWidgets();
    }

    /** Requires an explicit second action before a user-requested skip. */
    private void confirmExit() { confirmation = true; rebuildWidgets(); }

    /** Escape is a request to confirm skipping, never an unconfirmed completion. */
    @Override public void onClose() { if (confirmation) { confirmation = false; rebuildWidgets(); } else confirmExit(); }

    /** Completes locally and unpauses so the integrated server can process the completion request. */
    private void finish() {
        if (ended) return;
        ended = true;
        SkadaNetwork.playerToServer(new C2STutorial(session, C2STutorial.COMPLETE));
        minecraft.setScreen(null);
    }

    /** Releases a screen whose server session ended because of a lifecycle transition. */
    public void released() { ended = true; minecraft.setScreen(null); }

    /** Unexpected replacement interrupts rather than silently recording completion. */
    @Override public void removed() {
        if (!ended && minecraft.getConnection() != null) {
            ended = true;
            SkadaNetwork.playerToServer(new C2STutorial(session, C2STutorial.INTERRUPT));
        }
    }

    /** Draws annotated native-size panels and clips long explanation text inside its own column. */
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, BACKGROUND);
        g.fill(left, 16, left + 24, 18, ACCENT);
        g.drawString(font, text("title"), left + 32, 13, ACCENT, false);
        if (introduction || confirmation) {
            int x = Math.max(24, width / 2 - 210);
            int boxWidth = width - x * 2;
            g.drawWordWrap(font, text(confirmation ? "confirm_title" : "question"), x, Math.max(45, height / 2 - 45), boxWidth, 0xFFF4E9D6);
            g.drawWordWrap(font, text(confirmation ? "confirm_body" : "welcome"), x, height / 2 - 12, boxWidth, 0xFFAEBCC6);
        } else {
            String chapter = page == 0 ? "reticles" : page == 1 ? "weapon" : "armour";
            g.drawString(font, text(chapter + ".title"), left, 34, 0xFFF4E9D6, false);
            Component number = Component.translatable("skada.onboarding.page", page + (reticles ? 1 : 0), reticles ? 3 : 2);
            g.drawString(font, number, width - left - font.width(number), 16, 0xFFAEBCC6, false);
            g.fill(left, contentTop, left + columnWidth, contentBottom, page == 0 ? PANEL : 0xFF100B18);
            if (page > 0) g.renderOutline(left, contentTop, columnWidth, contentBottom - contentTop, 0xFF654582);
            int right = left + columnWidth + 16;
            g.fill(right, contentTop, right + columnWidth, contentBottom, PANEL);
            g.drawString(font, text(page == 0 ? "illustration" : "sample_values"), left, 48, 0xFFAEBCC6, false);
            if (page == 0) drawReticles(g, right);
            else drawExample(g, right);
            Component journal = Component.translatable("skada.onboarding.journal", Keybinds.statScreen.getTranslatedKeyMessage());
            g.drawWordWrap(font, journal, left, height - 57, width - left * 2, 0xFFAEBCC6);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Renders a deliberately illustrative targeting diagram with numbered callouts. */
    private void drawReticles(GuiGraphics g, int right) {
        int cx = left + columnWidth / 2;
        int cy = (contentTop + contentBottom) / 2;
        int radius = Math.max(18, Math.min(44, (contentBottom - contentTop - 30) / 2));
        g.fill(cx - 8, cy - 21, cx + 8, cy - 5, 0xFF809B72);
        g.fill(cx - 12, cy - 4, cx + 12, cy + 18, 0xFF4F7777);
        g.fill(cx - radius, cy - radius, cx + radius, cy + radius, 0x448D3D3D);
        g.renderOutline(cx - radius, cy - radius, radius * 2, radius * 2, 0xFFE59B85);
        g.hLine(cx - 4, cx + 4, cy, 0xFFFFFFFF);
        g.vLine(cx, cy - 4, cy + 4, 0xFFFFFFFF);
        g.drawString(font, "1", cx - radius, cy - radius - 11, ACCENT, false);
        g.drawString(font, "2", cx + 15, cy, ACCENT, false);
        g.renderItem(new ItemStack(Items.IRON_SWORD), cx + radius - 5, cy + radius - 5);
        g.drawString(font, "3", cx + radius + 7, cy + radius - 5, ACCENT, false);
        drawDetails(g, right, List.of(text("reticles.outline"), text("reticles.detected"),
            Component.translatable("skada.onboarding.reticles.attack", Keybinds.cycleAttackType.getTranslatedKeyMessage(),
                Keybinds.attackScrollModifier.getTranslatedKeyMessage())));
    }

    /** Uses Minecraft item art and clickable rows, never adding the example item to inventory. */
    private void drawExample(GuiGraphics g, int right) {
        g.renderItem(new ItemStack(page == 1 ? Items.IRON_SWORD : Items.IRON_CHESTPLATE), left + 8, contentTop + 8);
        g.drawString(font, text(page == 1 ? "weapon.example" : "armour.example"), left + 29, contentTop + 8, ACCENT, false);
        String stat = (page == 1 ? WEAPON : ARMOUR)[selected];
        g.drawWordWrap(font, Component.translatable("skada.help.name." + stat), right + 10, contentTop + 9, columnWidth - 20, ACCENT);
        drawDetails(g, right, List.of(Component.translatable("skada.help." + stat), Component.translatable("skada.help.detail." + stat)));
        if ((page == 1 ? WEAPON : ARMOUR).length > visibleRows()) {
            g.drawString(font, text("scroll"), left + 8, contentBottom - 10, 0xFFAEBCC6, false);
        }
    }

    /** Wraps and scrolls prose at the actual GUI font size rather than shrinking it to fit. */
    private void drawDetails(GuiGraphics g, int right, List<Component> paragraphs) {
        java.util.ArrayList<FormattedCharSequence> lines = new java.util.ArrayList<>();
        for (Component paragraph : paragraphs) {
            lines.addAll(font.split(paragraph, columnWidth - 20));
            lines.add(FormattedCharSequence.EMPTY);
        }
        int top = contentTop + 32;
        int available = Math.max(1, (contentBottom - top - 14) / 11);
        detailOffset = Mth.clamp(detailOffset, 0, Math.max(0, lines.size() - available));
        g.enableScissor(right, top, right + columnWidth, contentBottom - 12);
        for (int i = detailOffset; i < Math.min(lines.size(), detailOffset + available); i++)
            g.drawString(font, lines.get(i), right + 10, top + (i - detailOffset) * 11, 0xFFE2E7EA, false);
        g.disableScissor();
        if (lines.size() > available) g.drawString(font, text("scroll"), right + 10, contentBottom - 10, 0xFFAEBCC6, false);
    }

    /** Scrolls the list or explanation according to the pointer column. */
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (introduction || confirmation) return false;
        int step = delta > 0 ? -1 : 1;
        if (mouseX >= left + columnWidth + 16) detailOffset = Math.max(0, detailOffset + step);
        else if (page > 0) { rowOffset += step; rebuildWidgets(); }
        return true;
    }

    /** Clickable tooltip row using the same icon font as real Skada tooltips. */
    private static final class ExampleStatButton extends Button {
        private final String stat;
        private final boolean selected;

        /** Keeps the full stat label available to keyboard navigation and narration. */
        private ExampleStatButton(int x, int y, int width, Component message, String stat, boolean selected, OnPress press) {
            super(x, y, width, 18, message, press, DEFAULT_NARRATION);
            this.stat = stat;
            this.selected = selected;
        }

        /** Draws a stat line with a selection rail instead of ordinary grey menu-button chrome. */
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            if (selected || isHoveredOrFocused()) g.fill(getX(), getY(), getX() + getWidth(), getY() + height, 0xFF30243F);
            if (selected) g.fill(getX(), getY() + 2, getX() + 2, getY() + height - 2, ACCENT);
            String icon = switch (stat) {
                case "equipment_weight", "burden" -> "weight";
                case "attack_type" -> "attack_type.slash";
                case "damage" -> "attack_damage";
                case "elements" -> "element.physical";
                case "element_resist" -> "element.heat";
                case "slash_resist" -> "attack_type.slash";
                case "thrust_resist" -> "attack_type.thrust";
                case "strike_resist" -> "attack_type.strike";
                default -> stat;
            };
            g.drawString(font, Component.translatable("skada.icon." + icon).withStyle(Style.EMPTY.withFont(Util.rl("icons"))),
                getX() + 5, getY() + 5, selected ? ACCENT : 0xFFE2E7EA, false);
            renderScrollingString(g, font, getMessage(), getX() + 20, getY(), getX() + getWidth() - 4,
                getY() + height, selected ? ACCENT : 0xFFE2E7EA);
        }
    }

    /** Resolves tutorial UI text from the resource pack translation table. */
    private static Component text(String name) { return Component.translatable("skada.onboarding." + name); }
}
