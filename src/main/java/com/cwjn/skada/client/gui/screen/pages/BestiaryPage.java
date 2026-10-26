package com.cwjn.skada.client.gui.screen.pages;

import com.cwjn.skada.client.ClientBestiaryCache;
import com.cwjn.skada.client.gui.button.EntityButton;
import com.cwjn.skada.client.gui.screen.JournalPage;
import com.cwjn.skada.client.gui.screen.StatScreen;
import com.cwjn.skada.client.render.BestiaryEntitySketchRenderer;
import com.cwjn.skada.data.mob.BestiaryEntryView;
import com.cwjn.skada.data.mob.BestiaryPreviewSettings;
import com.cwjn.skada.data.mob.BestiaryTag;
import com.cwjn.skada.util.UtilColour;
import com.cwjn.skada.util.UtilText;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class BestiaryPage extends JournalPage {

  private static final List<BestiaryTag> FILTER_TAGS = List.of(
      BestiaryTag.OVERWORLD,
      BestiaryTag.NETHER,
      BestiaryTag.END,
      BestiaryTag.AQUATIC,
      BestiaryTag.PASSIVE,
      BestiaryTag.NEUTRAL,
      BestiaryTag.HOSTILE);
  private static final int FILTER_CHIP_COUNT = 1 + FILTER_TAGS.size();
  private static final int FILTER_CHIP_COLUMNS = 3;
  private static final int FILTER_CHIP_ROWS = (FILTER_CHIP_COUNT + FILTER_CHIP_COLUMNS - 1) / FILTER_CHIP_COLUMNS;

  private static final int LEFT_VIEWPORT_X = 60;
  private static final int LEFT_VIEWPORT_Y = 115;
  private static final int LEFT_VIEWPORT_WIDTH = 204;
  private static final int LEFT_VIEWPORT_HEIGHT = 223;
  private static final int LEFT_FILTER_X = 60;
  private static final int LEFT_FILTER_Y = 68;
  private static final int LEFT_FILTER_CHIP_WIDTH = 64;
  private static final int LEFT_FILTER_CHIP_HEIGHT = 12;
  private static final int LEFT_FILTER_COLUMN_GAP = 6;
  private static final int LEFT_FILTER_ROW_GAP = 4;
  private static final int LEFT_ENTRY_X = 60;
  private static final int LEFT_ENTRY_Y = LEFT_FILTER_Y
      + FILTER_CHIP_ROWS * LEFT_FILTER_CHIP_HEIGHT
      + Math.max(0, FILTER_CHIP_ROWS - 1) * LEFT_FILTER_ROW_GAP
      + 6;
  private static final int LEFT_ENTRY_WIDTH = 94;
  private static final int LEFT_ENTRY_HEIGHT = 24;
  private static final int LEFT_ENTRY_COLUMN_GAP = 10;
  private static final int LEFT_ENTRY_ROW_GAP = 10;
  private static final int LEFT_ENTRY_TOP_PADDING = LEFT_ENTRY_Y - LEFT_VIEWPORT_Y;
  private static final int LEFT_ENTRY_BOTTOM_PADDING = 7;
  private static final int LEFT_SCROLLBAR_WIDTH = 6;
  private static final int LEFT_SCROLL_STEP = 20;
  private static final int SCROLLBAR_THUMB_HEIGHT = 20;

  // Offsets/dimensions for the entire detail panel, which includes the preview, stats, and manual sections. These are relative to the top-left of book
  // No ui elements should extend past the dimension of the detail panel bounds, or they will be off the edge of the book
  private static final int DETAIL_PANEL_X_OFFSET = StatScreen.BOOK_RIGHT_PAGE_OFFSET_X + 38;
  private static final int DETAIL_PANEL_Y_OFFSET = 36;
  private static final int DETAIL_PANEL_WIDTH = 254;
  private static final int DETAIL_PANEL_HEIGHT = 322;

  // define the bounding box for the sepia sketch of the entity that is selected. Goes outside the detail panel bounds a bit
  private static final int DETAIL_PREVIEW_X = -14;
  private static final int DETAIL_PREVIEW_Y = -14;
  private static final int DETAIL_PREVIEW_WIDTH = 149;
  private static final int DETAIL_PREVIEW_HEIGHT = 229;

  private static final int DETAIL_STAT_X = 124;
  private static final int DETAIL_STAT_Y = 0;
  private static final int DETAIL_STAT_WIDTH = 128;
  private static final int DETAIL_STAT_HEIGHT = 210;
  private static final int DETAIL_MANUAL_X = -14;
  private static final int DETAIL_MANUAL_Y = 212;
  private static final int DETAIL_MANUAL_WIDTH = 282;
  private static final int DETAIL_MANUAL_HEIGHT = 72;
  private static final int DETAIL_SCROLL_STEP = 6;
  private static final int STAT_TEXT_INSET_X = 4;
  private static final int MANUAL_TEXT_INSET_X = 8;
  private static final int MANUAL_LOWER_TAPER_START_Y = 52;
  private static final int MANUAL_LOWER_TAPER_INSET_X = 18;
  private static final float MANUAL_TEXT_SCALE = 0.5F;
  private static final int HEADER_UNDERLINE_Y = 18;
  private static final int PREVIEW_TICK_SEED = 40;
  private static final int PREVIEW_ENTITY_ID_BASE = 1_000_000;

  private final Minecraft minecraft = Minecraft.getInstance();
  private final BestiaryEntitySketchRenderer entitySketchRenderer = new BestiaryEntitySketchRenderer();
  private final List<EntityButton> entityButtons = new ArrayList<>();
  private final Set<BestiaryTag> selectedTags = EnumSet.noneOf(BestiaryTag.class);

  private List<BestiaryEntryView> entries = List.of();
  private int scrollOffset;
  private int statScroll;
  private int manualScroll;
  private boolean newFilterSelected;

  @Nullable
  private BestiaryEntryView selectedEntry;
  @Nullable
  private ResourceLocation previewEntityId;
  @Nullable
  private ClientLevel previewLevel;
  @Nullable
  private LivingEntity previewEntity;
  private boolean draggingScrollbar;
  private int scrollbarDragOffsetY;

  public BestiaryPage(ResourceLocation icon, ResourceLocation pageResource, StatScreen screen) {
    super(icon, pageResource, screen);
  }

  @Override
  public void init() {
    refreshEntriesFromCache();
    updateWidgetState(0, 0);
    setWidgetsVisible(false);
  }

  @Override
  public boolean handleMouseScrolled(double mouseX, double mouseY, double delta) {
    if (isWithin(mouseX, mouseY, leftViewportX(), leftViewportY(), LEFT_VIEWPORT_WIDTH, LEFT_VIEWPORT_HEIGHT)) {
      setScrollOffset(scrollOffset + (int) (delta * -LEFT_SCROLL_STEP));
      return true;
    }

    if (!isRender) {
      return false;
    }

    int scrollAmount = (int) (delta * -DETAIL_SCROLL_STEP);
    int panelX = detailPanelX();
    int panelY = detailPanelY();
    if (isWithin(mouseX, mouseY, panelX + DETAIL_STAT_X, panelY + DETAIL_STAT_Y, DETAIL_STAT_WIDTH,
        DETAIL_STAT_HEIGHT)) {
      statScroll = Mth.clamp(statScroll + scrollAmount, 0, maxStatScroll());
      return true;
    }
    if (isWithin(mouseX, mouseY, panelX + DETAIL_MANUAL_X, panelY + DETAIL_MANUAL_Y, DETAIL_MANUAL_WIDTH,
        DETAIL_MANUAL_HEIGHT)) {
      manualScroll = Mth.clamp(manualScroll + scrollAmount, 0, maxManualScroll());
      return true;
    }

    return false;
  }

  @Override
  public boolean handleMouseClicked(double mouseX, double mouseY, int button) {
    if (button != 0) {
      return false;
    }

    if (toggleFilterAt(mouseX, mouseY)) {
      return true;
    }

    int trackX = scrollbarTrackX();
    int trackY = scrollbarTrackY();
    int trackHeight = scrollbarTrackHeight();
    if (isWithin(mouseX, mouseY, trackX, trackY, LEFT_SCROLLBAR_WIDTH, trackHeight)) {
      draggingScrollbar = true;
      int thumbY = scrollbarThumbY();
      if (isWithin(mouseX, mouseY, trackX, thumbY, LEFT_SCROLLBAR_WIDTH, SCROLLBAR_THUMB_HEIGHT)) {
        scrollbarDragOffsetY = (int) mouseY - thumbY;
      } else {
        scrollbarDragOffsetY = SCROLLBAR_THUMB_HEIGHT / 2;
        updateScrollFromMouse(mouseY);
      }
      return true;
    }

    if (!isWithin(mouseX, mouseY, leftViewportX(), leftViewportY(), LEFT_VIEWPORT_WIDTH, LEFT_VIEWPORT_HEIGHT)) {
      return false;
    }

    updateWidgetState((int) mouseX, (int) mouseY);
    for (EntityButton entityButton : entityButtons) {
      if (!entityButton.visible) {
        continue;
      }

      if (entityButton.mouseClicked(mouseX, mouseY, button)) {
        markEntryViewed(entityButton.getEntry(), mouseX, mouseY);
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean handleMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    if (!draggingScrollbar || button != 0) {
      return false;
    }
    updateScrollFromMouse(mouseY);
    return true;
  }

  @Override
  public boolean handleMouseReleased(double mouseX, double mouseY, int button) {
    if (!draggingScrollbar || button != 0) {
      return false;
    }
    draggingScrollbar = false;
    return true;
  }

  @Override
  public void clearButtons() {
    entityButtons.clear();
    entitySketchRenderer.close();
    entries = List.of();
    selectedTags.clear();
    newFilterSelected = false;
    selectedEntry = null;
    scrollOffset = 0;
    statScroll = 0;
    manualScroll = 0;
    draggingScrollbar = false;
    clearPreviewEntity();
  }

  @Override
  public void isRender(boolean render) {
    super.isRender(render);
    if (!render) {
      draggingScrollbar = false;
    }
    setWidgetsVisible(render);
  }

  @Override
  public void renderExtraDrawables(GuiGraphics graphics, int mouseX, int mouseY) {
    refreshEntriesFromCache();
    updateWidgetState(mouseX, mouseY);

    renderFilterChips(graphics, mouseX, mouseY);

    int trackX = scrollbarTrackX();
    int trackY = scrollbarTrackY();
    int trackHeight = scrollbarTrackHeight();
    int thumbY = scrollbarThumbY();
    graphics.fill(trackX + 2, trackY, trackX + 4, trackY + trackHeight, 0x887A5A45);
    graphics.fill(trackX, thumbY, trackX + LEFT_SCROLLBAR_WIDTH, thumbY + SCROLLBAR_THUMB_HEIGHT,
        draggingScrollbar ? 0xFFB68E69 : 0xCC8E6C50);
    graphics.fill(trackX, thumbY, trackX + LEFT_SCROLLBAR_WIDTH, thumbY + 1, 0xFFE9C397);
    graphics.fill(trackX, thumbY + SCROLLBAR_THUMB_HEIGHT - 1, trackX + LEFT_SCROLLBAR_WIDTH,
        thumbY + SCROLLBAR_THUMB_HEIGHT, 0xFF5B4030);

    if (isRender) {
      renderDetailPanel(graphics, mouseX, mouseY);
    }

    screen.enableScissor(leftViewportX(), leftViewportY(), LEFT_VIEWPORT_WIDTH, LEFT_VIEWPORT_HEIGHT);
    if (entries.isEmpty()) {
      renderEmptyListState(graphics);
    } else {
      for (EntityButton button : entityButtons) {
        if (button.visible) {
          button.renderTile(graphics);
        }
      }
    }
    screen.disableScissor();
  }

  private void refreshEntriesFromCache() {
    List<BestiaryEntryView> filteredEntries = buildFilteredEntries();
    if (filteredEntries.equals(entries) && filteredEntries.size() == entityButtons.size()) {
      scrollOffset = Mth.clamp(scrollOffset, 0, maxLeftScroll(entries.size()));
      return;
    }

    ResourceLocation selectedEntityId = selectedEntry == null ? null : selectedEntry.entityId();
    entries = filteredEntries;
    scrollOffset = Mth.clamp(scrollOffset, 0, maxLeftScroll(entries.size()));

    rebuildButtons();

    BestiaryEntryView retainedSelection = findEntry(selectedEntityId);
    if (retainedSelection != null) {
      selectEntry(retainedSelection, false);
    } else if (entries.isEmpty()) {
      selectEntry(null, false);
    } else {
      selectEntry(entries.get(0), false);
    }
  }

  private void rebuildButtons() {
    entityButtons.clear();
    for (BestiaryEntryView entry : entries) {
      EntityButton button = new EntityButton(0, 0, entry, pressed -> selectEntry(entry, true));
      button.visible = false;
      button.active = false;
      entityButtons.add(button);
    }
  }

  private void updateWidgetState(int mouseX, int mouseY) {
    int viewportX = leftViewportX();
    int viewportY = leftViewportY();
    for (int index = 0; index < entityButtons.size(); index++) {
      EntityButton button = entityButtons.get(index);
      int row = index / 2;
      int column = index % 2;
      int buttonX = screen.getBookLocalX() + LEFT_ENTRY_X + column * (LEFT_ENTRY_WIDTH + LEFT_ENTRY_COLUMN_GAP);
      int buttonY = screen.getBookLocalY() + LEFT_ENTRY_Y + row * (LEFT_ENTRY_HEIGHT + LEFT_ENTRY_ROW_GAP)
          - scrollOffset;
      boolean isVisible = intersects(buttonX, buttonY, LEFT_ENTRY_WIDTH, LEFT_ENTRY_HEIGHT, viewportX, viewportY,
          LEFT_VIEWPORT_WIDTH, LEFT_VIEWPORT_HEIGHT);
      boolean isHovered = isVisible
          && isWithin(mouseX, mouseY, buttonX, buttonY, LEFT_ENTRY_WIDTH, LEFT_ENTRY_HEIGHT)
          && isWithin(mouseX, mouseY, viewportX, viewportY, LEFT_VIEWPORT_WIDTH, LEFT_VIEWPORT_HEIGHT);

      button.setX(buttonX);
      button.setY(buttonY);
      button.visible = isRender && isVisible;
      button.active = isRender && isVisible;
      button.setHoveredWithinViewport(isHovered);
      button.setNewlyUnlocked(ClientBestiaryCache.isNewlyUnlocked(button.getEntry().entityId()));
      button.setSelected(selectedEntry != null && Objects.equals(selectedEntry.entityId(), button.getEntry().entityId()));
    }
  }

  private void setWidgetsVisible(boolean visible) {
    for (EntityButton button : entityButtons) {
      if (!visible) {
        button.visible = false;
        button.active = false;
        button.setHoveredWithinViewport(false);
      }
    }
  }

  private void selectEntry(@Nullable BestiaryEntryView entry, boolean animateRightPage) {
    if (Objects.equals(selectedEntry, entry)) {
      return;
    }

    boolean shouldAnimate = animateRightPage && selectedEntry != null && entry != null;
    selectedEntry = entry;
    statScroll = 0;
    manualScroll = 0;
    clearPreviewEntity();
    if (shouldAnimate) {
      StatScreen.CONTENT_APPEAR_ANIM_RIGHT_PAGE_ONLY.reset();
      screen.anim = StatScreen.CONTENT_APPEAR_ANIM_RIGHT_PAGE_ONLY;
    }
  }

  private List<BestiaryEntryView> buildFilteredEntries() {
    return filterEntries(ClientBestiaryCache.entries(), ClientBestiaryCache.unlockedEntryIds(),
        ClientBestiaryCache.newlyUnlockedEntryIds(), selectedTags, newFilterSelected);
  }

  private void markEntryViewed(BestiaryEntryView entry, double mouseX, double mouseY) {
    if (!ClientBestiaryCache.markEntryViewed(entry.entityId())) {
      return;
    }

    refreshEntriesFromCache();
    updateWidgetState((int) mouseX, (int) mouseY);
  }

  private boolean toggleFilterAt(double mouseX, double mouseY) {
    int filterIndex = filterChipIndexAt((int) mouseX, (int) mouseY);
    if (filterIndex < 0) {
      return false;
    }

    if (filterIndex == 0) {
      newFilterSelected = !newFilterSelected;
    } else {
      BestiaryTag tag = FILTER_TAGS.get(filterIndex - 1);
      if (!selectedTags.add(tag)) {
        selectedTags.remove(tag);
      }
    }

    refreshEntriesFromCache();
    updateWidgetState((int) mouseX, (int) mouseY);
    return true;
  }

  private void renderFilterChips(GuiGraphics graphics, int mouseX, int mouseY) {
    for (int index = 0; index < FILTER_CHIP_COUNT; index++) {
      int chipX = filterChipX(index);
      int chipY = filterChipY(index);
      boolean selected = isFilterSelected(index);
      boolean hovered = isRender && isWithin(mouseX, mouseY, chipX, chipY, LEFT_FILTER_CHIP_WIDTH, LEFT_FILTER_CHIP_HEIGHT);
      int borderColour = selected ? 0xFF8F634A : hovered ? 0xFF8B6A58 : 0xAA7A5A45;
      int fillColour = selected ? 0xFFCD9E6B : hovered ? 0xD9E6C39F : 0xB8E7C59A;
      int highlightColour = selected ? 0xFFF3D7A6 : 0xCCEFD3A9;

      graphics.fill(chipX, chipY, chipX + LEFT_FILTER_CHIP_WIDTH, chipY + LEFT_FILTER_CHIP_HEIGHT, borderColour);
      graphics.fill(chipX + 1, chipY + 1, chipX + LEFT_FILTER_CHIP_WIDTH - 1, chipY + LEFT_FILTER_CHIP_HEIGHT - 1,
          fillColour);
      graphics.fill(chipX + 1, chipY + 1, chipX + LEFT_FILTER_CHIP_WIDTH - 1, chipY + 2, highlightColour);

      Component label = filterLabel(index);
      int textX = chipX + LEFT_FILTER_CHIP_WIDTH / 2 - minecraft.font.width(label) / 2;
      int textY = chipY + Math.max(1, (LEFT_FILTER_CHIP_HEIGHT - minecraft.font.lineHeight) / 2);
      graphics.drawString(minecraft.font, label, textX, textY,
          selected ? UtilColour.WHITE : UtilColour.UI_TEXT_COLOUR, false);
    }
  }

  private void renderEmptyListState(GuiGraphics graphics) {
    Component emptyText = Component.translatable("skada.journal.bestiary.placeholder.no_entries");
    int drawX = leftViewportX() + LEFT_VIEWPORT_WIDTH / 2 - minecraft.font.width(emptyText) / 2;
    int drawY = leftViewportY() + LEFT_VIEWPORT_HEIGHT / 2 - minecraft.font.lineHeight / 2;
    graphics.drawString(minecraft.font, emptyText, drawX, drawY, UtilColour.UI_BORDER_COLOUR, false);
  }

  private boolean isFilterSelected(int filterIndex) {
    if (filterIndex == 0) {
      return newFilterSelected;
    }
    return selectedTags.contains(FILTER_TAGS.get(filterIndex - 1));
  }

  private Component filterLabel(int filterIndex) {
    if (filterIndex == 0) {
      return Component.translatable("skada.journal.bestiary.filter.new");
    }
    return Component.translatable("skada.journal.bestiary.filter."
        + FILTER_TAGS.get(filterIndex - 1).serializedName());
  }

  private int filterChipIndexAt(int mouseX, int mouseY) {
    for (int index = 0; index < FILTER_CHIP_COUNT; index++) {
      if (isWithin(mouseX, mouseY, filterChipX(index), filterChipY(index), LEFT_FILTER_CHIP_WIDTH,
          LEFT_FILTER_CHIP_HEIGHT)) {
        return index;
      }
    }
    return -1;
  }

  private int filterChipX(int filterIndex) {
    int column = filterIndex % FILTER_CHIP_COLUMNS;
    return screen.getBookLocalX() + LEFT_FILTER_X + column * (LEFT_FILTER_CHIP_WIDTH + LEFT_FILTER_COLUMN_GAP);
  }

  private int filterChipY(int filterIndex) {
    int row = filterIndex / FILTER_CHIP_COLUMNS;
    return screen.getBookLocalY() + LEFT_FILTER_Y + row * (LEFT_FILTER_CHIP_HEIGHT + LEFT_FILTER_ROW_GAP);
  }

  /**
   * Filters the cache-backed bestiary list down to unlocked entries that satisfy the local page filters.
   */
  static List<BestiaryEntryView> filterEntries(List<BestiaryEntryView> sourceEntries,
                                               Set<ResourceLocation> unlockedEntryIds,
                                               Set<ResourceLocation> newlyUnlockedEntryIds,
                                               Set<BestiaryTag> requiredTags,
                                               boolean onlyNewEntries) {
    Set<ResourceLocation> unlockedIds = unlockedEntryIds == null ? Set.of() : unlockedEntryIds;
    Set<ResourceLocation> newIds = newlyUnlockedEntryIds == null ? Set.of() : newlyUnlockedEntryIds;
    Set<BestiaryTag> tagFilters = requiredTags == null || requiredTags.isEmpty() ? Set.of() : Set.copyOf(requiredTags);
    List<BestiaryEntryView> filteredEntries = new ArrayList<>();

    for (BestiaryEntryView entry : sourceEntries) {
      ResourceLocation entityId = entry.entityId();
      if (entityId == null || !unlockedIds.contains(entityId)) {
        continue;
      }
      if (onlyNewEntries && !newIds.contains(entityId)) {
        continue;
      }
      if (!tagFilters.isEmpty() && !entry.tags().containsAll(tagFilters)) {
        continue;
      }
      filteredEntries.add(entry);
    }

    return List.copyOf(filteredEntries);
  }

  @Nullable
  private BestiaryEntryView findEntry(@Nullable ResourceLocation entityId) {
    if (entityId == null) {
      return null;
    }
    for (BestiaryEntryView entry : entries) {
      if (entityId.equals(entry.entityId())) {
        return entry;
      }
    }
    return null;
  }

  private void renderDetailPanel(GuiGraphics graphics, int mouseX, int mouseY) {
    int panelX = detailPanelX();
    int panelY = detailPanelY();
    int previewX = panelX + DETAIL_PREVIEW_X;
    int previewY = panelY + DETAIL_PREVIEW_Y;
    int statX = panelX + DETAIL_STAT_X;
    int statY = panelY + DETAIL_STAT_Y;
    int manualX = panelX + DETAIL_MANUAL_X;
    int manualY = panelY + DETAIL_MANUAL_Y;

    renderPreviewSection(graphics, previewX, previewY, mouseX, mouseY);
    renderStatSection(graphics, statX, statY);
    renderManualSection(graphics, manualX, manualY);
  }

  private void renderPreviewSection(GuiGraphics graphics, int previewX, int previewY, int mouseX, int mouseY) {
    if (selectedEntry != null && renderPreview(graphics, previewX, previewY, DETAIL_PREVIEW_WIDTH,
        DETAIL_PREVIEW_HEIGHT, selectedEntry, mouseX, mouseY)) {
      return;
    }

    Component previewText = Component.translatable(selectedEntry == null
        ? "skada.journal.bestiary.placeholder.no_selection"
        : "skada.journal.bestiary.placeholder.preview");
    int drawX = previewX + DETAIL_PREVIEW_WIDTH / 2 - minecraft.font.width(previewText) / 2;
    int drawY = previewY + DETAIL_PREVIEW_HEIGHT / 2 - minecraft.font.lineHeight / 2;
    graphics.drawString(minecraft.font, previewText, drawX, drawY, UtilColour.UI_BORDER_COLOUR, false);
  }

  private void renderStatSection(GuiGraphics graphics, int statX, int statY) {
    int lineHeight = minecraft.font.lineHeight + 2;
    int drawX = statX + STAT_TEXT_INSET_X;
    int drawWidth = DETAIL_STAT_WIDTH - STAT_TEXT_INSET_X * 2;
    int drawCenterX = statX + DETAIL_STAT_WIDTH / 2;
    statScroll = Mth.clamp(statScroll, 0, maxStatScroll());
    screen.enableScissor(drawX, statY, drawWidth, DETAIL_STAT_HEIGHT);

    int lineIndex = 0;
    if (selectedEntry == null) {
      drawStatLine(graphics, drawCenterX, statY, lineHeight, lineIndex,
          UtilText.pixelFontComponent(Component.translatable("skada.journal.bestiary.placeholder.no_selection"), false,
              false, true),
          UtilColour.UI_BORDER_COLOUR);
    } else {
      lineIndex = drawStatLine(graphics, drawCenterX, statY, lineHeight, lineIndex,
          UtilText.pixelFontComponent(Component.translatable("skada.journal.bestiary.attack_type_line",
              Component.translatable(selectedEntry.attackTypeKey())), false, false, true),
          UtilColour.UI_TEXT_COLOUR);

      for (BestiaryEntryView.StatRow statRow : selectedEntry.statRows()) {
        lineIndex = drawStatLine(graphics, drawCenterX, statY, lineHeight, lineIndex,
            statComponent(statRow.labelKey(), statRow.formattedValue()), UtilColour.UI_TEXT_COLOUR);
      }

      if (!selectedEntry.resistanceRows().isEmpty()) {
        lineIndex = drawStatLine(graphics, drawCenterX, statY, lineHeight, lineIndex,
            UtilText.pixelFontComponent(Component.translatable("skada.journal.bestiary.section.resistances"), false,
                false, true),
            UtilColour.UI_BORDER_COLOUR);
        for (BestiaryEntryView.ResistanceRow resistanceRow : selectedEntry.resistanceRows()) {
          lineIndex = drawStatLine(graphics, drawCenterX, statY, lineHeight, lineIndex,
              statComponent(resistanceRow.labelKey(), resistanceRow.formattedValue()), UtilColour.UI_TEXT_COLOUR);
        }
      }
    }

    screen.disableScissor();
  }

  private int drawStatLine(GuiGraphics graphics, int drawCenterX, int statY, int lineHeight, int lineIndex,
                           Component text, int colour) {
    int drawY = statY + lineIndex * lineHeight - statScroll;
    int drawX = drawCenterX - minecraft.font.width(text) / 2;
    graphics.drawString(minecraft.font, text, drawX, drawY, colour, false);
    return lineIndex + 1;
  }

  private Component statComponent(String labelKey, String formattedValue) {
    return UtilText.pixelFontComponent(Component.translatable("skada.journal.bestiary.stat_line",
        Component.translatable(labelKey), formattedValue), false, false, true);
  }

  private void renderManualSection(GuiGraphics graphics, int manualX, int manualY) {
    List<Component> manualLines = buildManualLines();
    int lineHeight = manualLineHeight();
    int drawCenterX = manualX + DETAIL_MANUAL_WIDTH / 2;
    manualScroll = Mth.clamp(manualScroll, 0, maxManualScroll(manualLines));

    for (int i = 0; i < manualLines.size(); i++) {
      int contentY = i * lineHeight;
      int drawY = manualY + contentY - manualScroll;
      if (drawY + lineHeight <= manualY || drawY >= manualY + DETAIL_MANUAL_HEIGHT) {
        continue;
      }

      int drawX = manualLineLeft(manualX, contentY);
      int drawWidth = manualLineWidth(contentY);
      int scissorY = Math.max(drawY, manualY);
      int scissorHeight = Math.min(drawY + lineHeight, manualY + DETAIL_MANUAL_HEIGHT) - scissorY;
      if (scissorHeight <= 0) {
        continue;
      }

      screen.enableScissor(drawX, scissorY, drawWidth, scissorHeight);
      UtilText.drawCenteredStringScaled(graphics, minecraft.font, manualLines.get(i), drawCenterX, drawY,
          MANUAL_TEXT_SCALE, UtilColour.UI_TEXT_COLOUR, false);
      screen.disableScissor();
    }
  }

  /**
   * Draws the raw snapshot across the full right page while renderer capture is
   * still under investigation.
   *
   * This deliberately bypasses the normal preview frame so the page can show
   * the entire cropped texture returned by the snapshot renderer.
   *
   * @param graphics the active GUI graphics context
   * @param entry the currently selected bestiary entry
   * @return {@code true} when a snapshot was drawn
   */
  private boolean renderRawSnapshotDebug(GuiGraphics graphics, BestiaryEntryView entry) {
    LivingEntity livingEntity = previewEntity(entry);
    if (livingEntity == null) {
      return false;
    }

    BestiaryEntitySketchRenderer.SketchSnapshot snapshot = entitySketchRenderer.getSnapshot(livingEntity);
    if (snapshot == null) {
      return false;
    }

    drawRawSnapshotDebug(graphics, snapshot);
    return true;
  }

  private List<Component> buildManualLines() {
    List<Component> lines = new ArrayList<>();
    if (selectedEntry == null || selectedEntry.description().isEmpty()) {
      addWrappedManualLines(lines, Component.translatable("skada.journal.bestiary.placeholder.no_field_notes")
          .getString());
      return lines;
    }

    for (int i = 0; i < selectedEntry.description().size(); i++) {
      addTranslatedManualLines(lines, selectedEntry.description().get(i));
      if (i < selectedEntry.description().size() - 1) {
        lines.add(Component.empty());
      }
    }
    return lines;
  }

  private int maxStatScroll() {
    int lineCount = 1;
    if (selectedEntry != null) {
      lineCount += selectedEntry.statRows().size();
      if (!selectedEntry.resistanceRows().isEmpty()) {
        lineCount += 1 + selectedEntry.resistanceRows().size();
      }
    }
    return Math.max(0, lineCount * (minecraft.font.lineHeight + 2) - DETAIL_STAT_HEIGHT);
  }

  private int maxManualScroll() {
    return maxManualScroll(buildManualLines());
  }

  /**
   * Splits translated field-note paragraphs by explicit line breaks before applying
   * the content-region-aware manual wrapping.
   */
  private void addTranslatedManualLines(List<Component> lines, String descriptionKey) {
    String[] translatedLines = Component.translatable(descriptionKey).getString().split("\\R", -1);
    for (String translatedLine : translatedLines) {
      if (translatedLine.isEmpty()) {
        lines.add(Component.empty());
        continue;
      }
      addWrappedManualLines(lines, translatedLine);
    }
  }

  /**
   * Wraps manual text using the same width profile the tapered notes region uses during rendering.
   */
  private void addWrappedManualLines(List<Component> lines, String text) {
    String remaining = text.stripLeading();
    while (!remaining.isEmpty()) {
      int wrapWidth = manualWrapWidth(lines.size() * manualLineHeight());
      String nextLine = nextManualLine(remaining, wrapWidth);
      lines.add(UtilText.compassNineFontComponent(Component.literal(nextLine)));
      remaining = remaining.substring(nextLine.length()).stripLeading();
    }
  }

  private int maxManualScroll(List<Component> manualLines) {
    return Math.max(0, manualLines.size() * manualLineHeight() - DETAIL_MANUAL_HEIGHT);
  }

  /**
   * Resolves the next wrapped manual line while preserving the existing paragraph text.
   */
  private String nextManualLine(String text, int wrapWidth) {
    String trimmedText = text.stripLeading();
    if (trimmedText.isEmpty()) {
      return "";
    }

    int bestBreakIndex = trimmedText.length();
    int lastWhitespaceIndex = -1;
    for (int i = 0; i < trimmedText.length(); i++) {
      if (Character.isWhitespace(trimmedText.charAt(i))) {
        lastWhitespaceIndex = i;
      }

      String candidate = trimmedText.substring(0, i + 1);
      if (manualLinePixelWidth(candidate) <= wrapWidth) {
        continue;
      }

      if (lastWhitespaceIndex >= 0) {
        bestBreakIndex = lastWhitespaceIndex;
      } else {
        bestBreakIndex = Math.max(1, i);
      }
      return trimmedText.substring(0, bestBreakIndex).stripTrailing();
    }

    return trimmedText.substring(0, bestBreakIndex).stripTrailing();
  }

  /**
   * Returns the rendered notes line height, including the small spacing between compass text rows.
   */
  private int manualLineHeight() {
    return UtilText.compass9ScaledHeight(MANUAL_TEXT_SCALE) + 1;
  }

  private int manualWrapWidth(int contentY) {
    return Math.max(24, Math.round(manualLineWidth(contentY) / MANUAL_TEXT_SCALE));
  }

  private int manualLineLeft(int manualX, int contentY) {
    return manualX + MANUAL_TEXT_INSET_X + manualTaperInset(contentY);
  }

  private int manualLineWidth(int contentY) {
    int taperInset = manualTaperInset(contentY);
    return Math.max(24, DETAIL_MANUAL_WIDTH - (MANUAL_TEXT_INSET_X + taperInset) * 2);
  }

  private int manualTaperInset(int contentY) {
    return contentY >= MANUAL_LOWER_TAPER_START_Y ? MANUAL_LOWER_TAPER_INSET_X : 0;
  }

  private int manualLinePixelWidth(String text) {
    return minecraft.font.width(UtilText.compassNineFontComponent(Component.literal(text)));
  }

  private void drawHorizontalGradient(GuiGraphics graphics, float x1, float y1, float x2, float y2, float z,
                                      int colorFrom, int colorTo) {
    VertexConsumer vertexConsumer = graphics.bufferSource().getBuffer(RenderType.gui());
    float alphaFrom = (float) FastColor.ARGB32.alpha(colorFrom) / 255.0F;
    float redFrom = (float) FastColor.ARGB32.red(colorFrom) / 255.0F;
    float greenFrom = (float) FastColor.ARGB32.green(colorFrom) / 255.0F;
    float blueFrom = (float) FastColor.ARGB32.blue(colorFrom) / 255.0F;
    float alphaTo = (float) FastColor.ARGB32.alpha(colorTo) / 255.0F;
    float redTo = (float) FastColor.ARGB32.red(colorTo) / 255.0F;
    float greenTo = (float) FastColor.ARGB32.green(colorTo) / 255.0F;
    float blueTo = (float) FastColor.ARGB32.blue(colorTo) / 255.0F;
    Matrix4f pose = graphics.pose().last().pose();
    vertexConsumer.vertex(pose, x1, y1, z).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
    vertexConsumer.vertex(pose, x1, y2, z).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
    vertexConsumer.vertex(pose, x2, y2, z).color(redTo, greenTo, blueTo, alphaTo).endVertex();
    vertexConsumer.vertex(pose, x2, y1, z).color(redTo, greenTo, blueTo, alphaTo).endVertex();
  }

  private boolean renderPreview(GuiGraphics graphics, int previewX, int previewY, int previewWidth, int previewHeight,
                                BestiaryEntryView entry, double mouseX, double mouseY) {
    LivingEntity livingEntity = previewEntity(entry);
    if (livingEntity == null) {
      return false;
    }

    BestiaryPreviewSettings previewSettings = entry.preview();
    BestiaryEntitySketchRenderer.SketchSnapshot snapshot = entitySketchRenderer.getSnapshot(livingEntity);
    if (snapshot != null) {
      drawPreviewSnapshot(graphics, snapshot, previewX, previewY, previewWidth, previewHeight, previewSettings);
      return true;
    }

    int renderScale = Math.max(18, Mth.floor(Math.min(previewWidth, previewHeight) * 0.8D * previewSettings.scale()));
    int renderX = previewX + previewWidth / 2 + Mth.floor(previewSettings.offsetX());
    int renderY = previewY + previewHeight + Mth.floor(renderScale * 0.75D) + Mth.floor(previewSettings.offsetY());
    float lookX = (float) (renderX - mouseX);
    float lookY = (float) (renderY - mouseY - renderScale * 0.85F);

    InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, renderX, renderY, renderScale, lookX, lookY,
        livingEntity);
    return true;
  }

  /**
   * Draws the cropped snapshot texture into the preview frame using page-side
   * scale and offset settings.
   */
  private void drawPreviewSnapshot(GuiGraphics graphics, BestiaryEntitySketchRenderer.SketchSnapshot snapshot,
                                   int previewX, int previewY, int previewWidth, int previewHeight,
                                   BestiaryPreviewSettings previewSettings) {
    float fitScale = Math.min(1.0F, Math.min((float) previewWidth / (float) snapshot.width(),
      (float) previewHeight / (float) snapshot.height()));
    float scale = fitScale * (float) previewSettings.scale();
    float drawWidth = snapshot.width() * scale;
    float drawHeight = snapshot.height() * scale;
    float drawX = previewX + (previewWidth - drawWidth) * 0.5F + (float) previewSettings.offsetX();
    float drawY = previewY + (previewHeight - drawHeight) * 0.5F + (float) previewSettings.offsetY();

    graphics.flush();
    screen.enableScissor(previewX, previewY, previewWidth, previewHeight);
    try {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      graphics.blit(snapshot.textureLocation(), Mth.floor(drawX), Mth.floor(drawY), Math.max(1, Mth.floor(drawWidth)),
          Math.max(1, Mth.floor(drawHeight)), 0.0F, 0.0F, snapshot.width(), snapshot.height(), snapshot.width(),
          snapshot.height());
    } finally {
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      screen.disableScissor();
    }
  }

  /**
   * Draws the current raw snapshot centered across the entire right page.
   *
   * The full-region blit overload keeps the source texture dimensions explicit,
   * which avoids the misleading partial-fragment draw that can happen with the
   * simpler overload when the snapshot is larger than the destination area.
   *
   * @param graphics the active GUI graphics context
   * @param snapshot the raw snapshot texture to inspect
   */
  private void drawRawSnapshotDebug(GuiGraphics graphics, BestiaryEntitySketchRenderer.SketchSnapshot snapshot) {
    int pageX = screen.getBookLocalX() + StatScreen.BOOK_RIGHT_PAGE_OFFSET_X;
    int pageY = screen.getBookLocalY();
    int pageWidth = StatScreen.BOOK_WIDTH / 2;
    int pageHeight = StatScreen.BOOK_HEIGHT;
    float scale = Math.min((float) pageWidth / (float) snapshot.width(), (float) pageHeight / (float) snapshot.height());
    float scaleDown = Math.min(1, scale);
    int drawWidth = Math.max(1, Mth.floor(snapshot.width() * scaleDown));
    int drawHeight = Math.max(1, Mth.floor(snapshot.height() * scaleDown));
    int drawX = pageX + (pageWidth - drawWidth) / 2;
    int drawY = pageY + (pageHeight - drawHeight) / 2;

    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    graphics.blit(snapshot.textureLocation(), drawX, drawY, drawWidth, drawHeight, 0.0F, 0.0F, snapshot.width(),
        snapshot.height(), snapshot.width(), snapshot.height());
    RenderSystem.disableBlend();
  }

  private void updateScrollFromMouse(double mouseY) {
    int trackY = scrollbarTrackY();
    int travel = Math.max(0, scrollbarTrackHeight() - SCROLLBAR_THUMB_HEIGHT);
    int maxScroll = maxLeftScroll(entries.size());
    if (travel == 0 || maxScroll == 0) {
      setScrollOffset(0);
      return;
    }

    int thumbTop = Mth.clamp((int) mouseY - scrollbarDragOffsetY, trackY, trackY + travel);
    double progress = (double) (thumbTop - trackY) / (double) travel;
    setScrollOffset((int) Math.round(progress * maxScroll));
  }

  private void setScrollOffset(int newScrollOffset) {
    scrollOffset = Mth.clamp(newScrollOffset, 0, maxLeftScroll(entries.size()));
  }

  private int leftViewportX() {
    return screen.getBookLocalX() + LEFT_VIEWPORT_X;
  }

  private int leftViewportY() {
    return screen.getBookLocalY() + LEFT_VIEWPORT_Y;
  }

  private int detailPanelX() {
    return screen.getBookLocalX() + DETAIL_PANEL_X_OFFSET;
  }

  private int detailPanelY() {
    return screen.getBookLocalY() + DETAIL_PANEL_Y_OFFSET;
  }

  private int scrollbarTrackX() {
    return leftViewportX() + LEFT_VIEWPORT_WIDTH + 6;
  }

  private int scrollbarTrackY() {
    return leftViewportY() + 1;
  }

  private int scrollbarTrackHeight() {
    return LEFT_VIEWPORT_HEIGHT - 2;
  }

  private int scrollbarThumbY() {
    int travel = Math.max(0, scrollbarTrackHeight() - SCROLLBAR_THUMB_HEIGHT);
    int maxScroll = maxLeftScroll(entries.size());
    if (travel == 0 || maxScroll == 0) {
      return scrollbarTrackY();
    }

    double progress = (double) scrollOffset / (double) maxScroll;
    return scrollbarTrackY() + (int) Math.round(progress * travel);
  }

  private int maxLeftScroll(int entryCount) {
    return Math.max(0, leftContentHeight(entryCount) - LEFT_VIEWPORT_HEIGHT);
  }

  private int leftContentHeight(int entryCount) {
    if (entryCount <= 0) {
      return 0;
    }
    int rows = (entryCount + 1) / 2;
    return LEFT_ENTRY_TOP_PADDING
        + rows * LEFT_ENTRY_HEIGHT
        + Math.max(0, rows - 1) * LEFT_ENTRY_ROW_GAP
        + LEFT_ENTRY_BOTTOM_PADDING;
  }

  private static boolean isWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
    return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
  }

  private static boolean intersects(int x, int y, int width, int height, int otherX, int otherY, int otherWidth,
                                    int otherHeight) {
    return x < otherX + otherWidth && x + width > otherX && y < otherY + otherHeight && y + height > otherY;
  }

  @Nullable
  private LivingEntity previewEntity(BestiaryEntryView entry) {
    ClientLevel currentLevel = minecraft.level;
    if (currentLevel == null || entry.entityId() == null) {
      clearPreviewEntity();
      return null;
    }

    if (previewEntity != null && Objects.equals(previewEntityId, entry.entityId()) && previewLevel == currentLevel) {
      return previewEntity;
    }

    EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entry.entityId());
    if (entityType == null) {
      clearPreviewEntity();
      return null;
    }

    if (!(entityType.create(currentLevel) instanceof LivingEntity livingEntity)) {
      clearPreviewEntity();
      return null;
    }

    initializePreviewEntity(livingEntity);
    previewEntityId = entry.entityId();
    previewLevel = currentLevel;
    previewEntity = livingEntity;
    return livingEntity;
  }

  /**
   * Seeds one freshly created preview entity with stable first-frame transform
   * state so vanilla renderers that interpolate from prior frame values do not
   * start from uninitialized positions or rotations.
   *
   * @param entity the newly created preview entity
   */
  private void initializePreviewEntity(LivingEntity entity) {
    ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
    int seededId = PREVIEW_ENTITY_ID_BASE + Math.floorMod(Objects.hashCode(entityKey), 1_000_000);
    entity.setId(seededId);
    entity.moveTo(0.0D, 0.0D, 0.0D, entity.getYRot(), entity.getXRot());
    entity.setDeltaMovement(Vec3.ZERO);
    entity.tickCount = PREVIEW_TICK_SEED;
    entity.yBodyRot = entity.getYRot();
    entity.yBodyRotO = entity.getYRot();
    entity.yHeadRot = entity.getYRot();
    entity.yHeadRotO = entity.getYRot();
  }

  private void clearPreviewEntity() {
    previewEntityId = null;
    previewLevel = null;
    previewEntity = null;
  }
}