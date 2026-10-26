package com.cwjn.skada.client.gui.screen.pages;

import com.cwjn.skada.client.gui.button.CarouselArrowButton;
import com.cwjn.skada.client.gui.screen.JournalPage;
import com.cwjn.skada.client.gui.screen.StatScreen;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class TutorialPage extends JournalPage {

    private static final ResourceLocation TUTORIAL_ICON = Util.rl("textures/gui/book_page_icons/tutorial_page_icon.png");

    private static final int LEFT_ARROW_X_OFFSET = 55;
    private static final int RIGHT_ARROW_X_OFFSET = 618;
    private static final int ARROW_Y_OFFSET = 397;
    private static final int TITLE_Y_OFFSET = 35;
    private static final int BODY_Y_OFFSET = 58;
    private static final int BODY_WIDTH = 640;
    private static final int BODY_LINE_HEIGHT = 10;
    private static final int PAGE_NUMBER_X = 330;
    private static final int PAGE_NUMBER_Y = 393;
    private static final float TITLE_SCALE = 1.2f;
    private static final float BODY_SCALE = 0.55f;
    private static final int BODY_HEIGHT = 330;

    private int currentSection;
    private CarouselArrowButton leftArrow;
    private CarouselArrowButton rightArrow;

    private final List<TutorialSection> sections = List.of(
        new TutorialSection("skada.tutorial.section.1.title", List.of(
            "skada.tutorial.section.1.body.1",
            "skada.tutorial.section.1.body.2",
            "skada.tutorial.section.1.body.3"
        )),
        new TutorialSection("skada.tutorial.section.2.title", List.of(
            "skada.tutorial.section.2.body.1",
            "skada.tutorial.section.2.body.2",
            "skada.tutorial.section.2.body.3",
            "skada.tutorial.section.2.body.4",
            "skada.tutorial.section.2.body.5"
        )),
        new TutorialSection("skada.tutorial.section.3.title", List.of(
            "skada.tutorial.section.3.body.1",
            "skada.tutorial.section.3.body.2",
            "skada.tutorial.section.3.body.3",
            "skada.tutorial.section.3.body.4"
        )),
        new TutorialSection("skada.tutorial.section.4.title", List.of(
            "skada.tutorial.section.4.body.1",
            "skada.tutorial.section.4.body.2",
            "skada.tutorial.section.4.body.3",
            "skada.tutorial.section.4.body.4",
            "skada.tutorial.section.4.body.5"
        )),
        new TutorialSection("skada.tutorial.section.5.title", List.of(
            "skada.tutorial.section.5.body.1",
            "skada.tutorial.section.5.body.2",
            "skada.tutorial.section.5.body.3",
            "skada.tutorial.section.5.body.4",
            "skada.tutorial.section.5.body.5",
            "skada.tutorial.section.5.body.6"
        ))
    );

    public TutorialPage(ResourceLocation icon, ResourceLocation pageResource, StatScreen screen) {
        super(icon, pageResource, screen);
    }

    public static ResourceLocation getTutorialIcon() {
        return TUTORIAL_ICON;
    }

    @Override
    public void init() {
        int bookX = screen.getBookLocalX();
        int bookY = screen.getBookLocalY();

        leftArrow = new CarouselArrowButton(
            bookX + LEFT_ARROW_X_OFFSET,
            bookY + ARROW_Y_OFFSET,
            true,
            b -> previousSection()
        );
        rightArrow = new CarouselArrowButton(
            bookX + RIGHT_ARROW_X_OFFSET,
            bookY + ARROW_Y_OFFSET,
            false,
            b -> nextSection()
        );

        screen.addRenderableWidget(leftArrow);
        screen.addRenderableWidget(rightArrow);
        syncArrowState();
    }

    @Override
    public void clearButtons() {
        if (leftArrow != null) {
            screen.removeWidget(leftArrow);
            leftArrow = null;
        }
        if (rightArrow != null) {
            screen.removeWidget(rightArrow);
            rightArrow = null;
        }
    }

    @Override
    public void isRender(boolean render) {
        super.isRender(render);
        if (leftArrow != null) {
            leftArrow.visible = render;
            leftArrow.active = render;
        }
        if (rightArrow != null) {
            rightArrow.visible = render;
            rightArrow.active = render;
        }
    }

    @Override
    public void renderExtraDrawables(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        Font font = Minecraft.getInstance().font;
        int bookX = screen.getBookLocalX();
        int bookY = screen.getBookLocalY();

        TutorialSection section = sections.get(currentSection);

        Component title = Component.translatable(section.titleKey);
        Component scaledTitle = UtilText.compassNineFontComponent(title.copy());
        int titleWidth = UtilText.scaledWidth(font, scaledTitle, TITLE_SCALE);
        UtilText.drawCenteredStringScaled(pGuiGraphics, font, scaledTitle,
            bookX + StatScreen.BOOK_WIDTH / 2, bookY + TITLE_Y_OFFSET, TITLE_SCALE, 0xFFEAD8A0, false);

        int bodyTop = bookY + BODY_Y_OFFSET;
        int bodyLeft = bookX + (StatScreen.BOOK_WIDTH - BODY_WIDTH) / 2;
        int lineCount = section.bodyKeys.size();
        int totalBodyHeight = lineCount * BODY_LINE_HEIGHT;
        int availableHeight = ARROW_Y_OFFSET - BODY_Y_OFFSET - 10;

        if (totalBodyHeight > availableHeight) {
            screen.enableScissor(bookX + 25, bodyTop, StatScreen.BOOK_WIDTH - 50, availableHeight);
        }

        for (int i = 0; i < section.bodyKeys.size(); i++) {
            Component line = Component.translatable(section.bodyKeys.get(i));
            UtilText.drawStringScaled(pGuiGraphics, font,
                UtilText.compassNineFontComponent(line.copy()),
                bodyLeft, bodyTop + i * BODY_LINE_HEIGHT, BODY_SCALE, 0xFFC8C8C8, false);
        }

        if (totalBodyHeight > availableHeight) {
            screen.disableScissor();
        }

        Component pageNumber = Component.translatable("skada.tutorial.page_number",
            currentSection + 1, sections.size());
        UtilText.drawCenteredStringScaled(pGuiGraphics, font,
            UtilText.compassNineFontComponent(pageNumber.copy()),
            bookX + PAGE_NUMBER_X, bookY + PAGE_NUMBER_Y, 0.5f, 0xFF888888, false);
    }

    private void nextSection() {
        if (currentSection < sections.size() - 1) {
            currentSection++;
        } else {
            currentSection = 0;
        }
        syncArrowState();
    }

    private void previousSection() {
        if (currentSection > 0) {
            currentSection--;
        } else {
            currentSection = sections.size() - 1;
        }
        syncArrowState();
    }

    private void syncArrowState() {
        if (leftArrow != null) {
            leftArrow.active = true;
        }
        if (rightArrow != null) {
            rightArrow.active = true;
        }
    }

    private record TutorialSection(String titleKey, List<String> bodyKeys) {}

}