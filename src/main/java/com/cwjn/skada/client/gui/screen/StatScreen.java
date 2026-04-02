package com.cwjn.skada.client.gui.screen;

import com.cwjn.skada.client.gui.button.TabButton;
import com.cwjn.skada.client.gui.screen.pages.DamageInfoPage;
import com.cwjn.skada.client.gui.screen.pages.InfoPage;
import com.cwjn.skada.client.gui.screen.pages.PlayerPage;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.Keybinds;
import com.cwjn.skada.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import java.awt.event.KeyEvent;

@OnlyIn(Dist.CLIENT)
public class StatScreen extends Screen {

  private final Minecraft minecraft = Minecraft.getInstance();
  private final WeaponInfo weaponInfo;
  private final AttackType attackType;
  private final AttackTypeInfo attackTypeInfo;
  public final static int BOOK_WIDTH = 695;
  public final static int BOOK_HEIGHT = 417;
  public final static int BOOK_TURNING_HEIGHT = 457;
  public final static int BOOK_OPENING_HEIGHT = 507;
  public final static int BOOK_TEXTURE_OFFSET_X = 118;
  public final static int BOOK_RIGHT_PAGE_OFFSET_X = 380;
  public final static int SCROLLBOX_HEIGHT = 273;
  private static final Animation FLIP_LEFT_ANIM = new Animation("textures/gui/book_flip_left/", 9, 500, 140, -41,
      BOOK_TURNING_HEIGHT); // 500ms duration
  private static final Animation FLIP_RIGHT_ANIM = new Animation("textures/gui/book_flip_right/", 9, 500, 140, -41,
      BOOK_TURNING_HEIGHT);
  private static final Animation OPEN_ANIM = new Animation("textures/gui/book_open/", 5, 300, 85, -96,
      BOOK_OPENING_HEIGHT); // 300ms duration
  private static final Animation CONTENT_APPEAR_ANIM = new Animation("textures/gui/book_content_appear/", 36, 800, 181,
      0, BOOK_HEIGHT); // 800ms duration
  public static final Animation CONTENT_APPEAR_ANIM_RIGHT_PAGE_ONLY = new Animation("textures/gui/book_content_appear/",
      36, 800, 181, 0, BOOK_HEIGHT); // 800ms duration
  private static final Animation TABS_APPEAR_ANIM = new Animation("textures/gui/book_tabs_appear/", 17, 500, 181, 0,
      BOOK_HEIGHT); // 600ms duration
  private static final int BOOK_CANVAS_TOP = Math.min(0,
      Math.min(OPEN_ANIM.getYOffset(), Math.min(FLIP_LEFT_ANIM.getYOffset(), FLIP_RIGHT_ANIM.getYOffset())));
  private static final int BOOK_CANVAS_BOTTOM = Math.max(BOOK_HEIGHT,
      Math.max(OPEN_ANIM.getYOffset() + OPEN_ANIM.getHeight(),
          Math.max(FLIP_LEFT_ANIM.getYOffset() + FLIP_LEFT_ANIM.getHeight(),
              FLIP_RIGHT_ANIM.getYOffset() + FLIP_RIGHT_ANIM.getHeight())));
  private static final int BOOK_CANVAS_HEIGHT = BOOK_CANVAS_BOTTOM - BOOK_CANVAS_TOP;
  private static final ResourceLocation BOOK_NO_TAB_SELECTED = Util.rl("textures/gui/book_idle/none_selected.png");
  private static final ResourceLocation BOOK_TAB_1 = Util.rl("textures/gui/book_idle/1.png");
  private static final ResourceLocation BOOK_TAB_2 = Util.rl("textures/gui/book_idle/2.png");
  private static final ResourceLocation BOOK_TAB_3 = Util.rl("textures/gui/book_idle/3.png");
  private static final ResourceLocation BOOK_TAB_4 = Util.rl("textures/gui/book_idle/4.png");
  private static final ResourceLocation BOOK_TAB_5 = Util.rl("textures/gui/book_idle/5.png");
  private static final ResourceLocation BOOK_TAB_6 = Util.rl("textures/gui/book_idle/6.png");
  public static final ResourceLocation DAMAGE_PAGE_SCROLLBOX_BORDER = Util
      .rl("textures/gui/damage_page/scrollbox_border.png");
  public static final ResourceLocation SCROLLBAR = Util.rl("textures/gui/book_idle/scrollbar.png");
  public Animation anim = OPEN_ANIM;
  private static final ResourceLocation PLAYER_PAGE_ICON = Util.rl("textures/gui/book_page_icons/player_page_icon.png");
  private static final ResourceLocation DAMAGE_ICON = Util.rl("textures/gui/book_page_icons/damage_page_icon.png");
  private static final ResourceLocation ARMOUR_ICON = Util.rl("textures/gui/book_page_icons/armour_icon.png");
  private static final ResourceLocation INFO_ICON = Util.rl("textures/gui/book_page_icons/info_page_icon.png");
  private JournalPage[] pages = new JournalPage[] {
      new PlayerPage(PLAYER_PAGE_ICON, BOOK_TAB_1, this),
      new DamageInfoPage(DAMAGE_ICON, BOOK_TAB_2, this),
      new InfoPage(INFO_ICON, BOOK_TAB_3, this)
  }; // max 6 pages, but only 2 for now
  private int DISPLAY_STATE = 0; // index of the currently displayed page
  private static final int BOOK_LOCAL_LEFT = 0;
  private static final int BOOK_LOCAL_TOP = 0;
  private float bookRenderScale = 1.0F;
  private int bookScreenLeft;
  private int bookScreenTop;
  private final Screen savedScreen;

  static {
    OPEN_ANIM.setSoundEffect(SoundEvents.BOOK_PAGE_TURN);
    FLIP_LEFT_ANIM.setSoundEffect(SoundEvents.BOOK_PAGE_TURN);
    FLIP_LEFT_ANIM.setNextAnim(TABS_APPEAR_ANIM);
    FLIP_RIGHT_ANIM.setNextAnim(TABS_APPEAR_ANIM);
    FLIP_RIGHT_ANIM.setSoundEffect(SoundEvents.BOOK_PAGE_TURN);
    OPEN_ANIM.setNextAnim(TABS_APPEAR_ANIM);
    TABS_APPEAR_ANIM.setNextAnim(CONTENT_APPEAR_ANIM);
    OPEN_ANIM.addDelay(250);
  }

  public StatScreen(WeaponInfo info, AttackType type, AttackTypeInfo attackTypeInfo) {
    super(Component.literal("Stat Screen"));
    this.savedScreen = minecraft.screen;
    this.weaponInfo = info;
    this.attackType = type;
    this.attackTypeInfo = attackTypeInfo;
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void onClose() {
    super.onClose();
    clearPageButtons();
    resetAnimations();
    minecraft.setScreen(savedScreen);
  }

  @Override
  public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
    if (pKeyCode == Keybinds.statScreen.getKey().getValue() || pKeyCode == KeyEvent.VK_E) {
      clearPageButtons();
      resetAnimations();
      minecraft.setScreen(savedScreen);
      return true;
    }
    return super.keyPressed(pKeyCode, pScanCode, pModifiers);
  }

  @Override
  protected void init() {
    super.init();

    updateBookLayout();

    createTabButtons();
    for (JournalPage page : pages) {
      page.init();
      page.isRender(false);
    }
    pages[DISPLAY_STATE].isRender(true);
  }

  /**
   * Calculate the screen-space placement of the book. Page content is drawn in
   * book-local coordinates with a fixed origin, then translated and scaled into
   * place during rendering.
   */
  private void updateBookLayout() {
    bookRenderScale = Math.min(1.0F,
        Math.min((float) this.width / BOOK_WIDTH, (float) this.height / BOOK_CANVAS_HEIGHT));
    if (bookRenderScale <= 0.0F) {
      bookRenderScale = 1.0F;
    }

    bookScreenLeft = Mth.floor((this.width - BOOK_WIDTH * bookRenderScale) / 2.0F);
    bookScreenTop = Mth.floor((this.height - BOOK_HEIGHT * bookRenderScale) / 2.0F);
  }

  private void clearPageButtons() {
    for (JournalPage page : pages) {
      page.clearButtons();
    }
  }

  private void resetAnimations() {
    OPEN_ANIM.reset();
    FLIP_LEFT_ANIM.reset();
    FLIP_RIGHT_ANIM.reset();
    TABS_APPEAR_ANIM.reset();
    CONTENT_APPEAR_ANIM.reset();
    CONTENT_APPEAR_ANIM_RIGHT_PAGE_ONLY.reset();
    anim = OPEN_ANIM;
  }

  private void createTabButtons() {
    int yOffset = 0;
    for (int i = 0; i < pages.length; i++) {
      int finalI = i;
      TabButton bt = new TabButton(BOOK_LOCAL_LEFT + 654, BOOK_LOCAL_TOP + 66 + yOffset, Component.empty(),
          b -> setDisplayState(finalI));
      addRenderableWidget(bt);
      bt.setIcon(pages[i].getIcon());
      pages[i].setButton(bt);
      yOffset += 38;
    }
    syncTabButtonState();
  }

  private void syncTabButtonState() {
    boolean tabsVisible = areTabsVisible();
    for (int i = 0; i < pages.length; i++) {
      pages[i].setTabButtonVisible(tabsVisible);
      pages[i].setTabButtonActive(tabsVisible && i != DISPLAY_STATE);
    }
  }

  private boolean areTabsVisible() {
    return anim == null || anim == CONTENT_APPEAR_ANIM || anim == CONTENT_APPEAR_ANIM_RIGHT_PAGE_ONLY || anim == TABS_APPEAR_ANIM;
  }

  private void setDisplayState(int newState) {
    CONTENT_APPEAR_ANIM.reset();
    if (DISPLAY_STATE == newState)
      return;
    for (JournalPage page : pages) {
      page.setTabButtonActive(false);
      page.setTabButtonVisible(false);
    }
    anim = DISPLAY_STATE < newState ? FLIP_RIGHT_ANIM : FLIP_LEFT_ANIM;
    pages[DISPLAY_STATE].isRender(false);
    pages[newState].isRender(true);
    DISPLAY_STATE = newState;
    syncTabButtonState();
  }

  @Override
  public void renderBackground(GuiGraphics pGuiGraphics) {
    super.renderBackground(pGuiGraphics);
  }

  @Override
  public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
    int localMouseX = toBookLocalX(pMouseX);
    int localMouseY = toBookLocalY(pMouseY);

    pGuiGraphics.pose().pushPose();
    pGuiGraphics.pose().translate(bookScreenLeft, bookScreenTop, 0);
    pGuiGraphics.pose().scale(bookRenderScale, bookRenderScale, 1.0F);
    if (anim == null) {
      drawFromState(pGuiGraphics, localMouseX, localMouseY);
    } else if (anim == CONTENT_APPEAR_ANIM) {
      drawFromState(pGuiGraphics, localMouseX, localMouseY);
      pGuiGraphics.pose().pushPose();
      pGuiGraphics.pose().translate(0, 0, 201);
      pGuiGraphics.blit(anim.getCurrentFrame(), BOOK_LOCAL_LEFT, BOOK_LOCAL_TOP + anim.getYOffset(), BOOK_TEXTURE_OFFSET_X,
          anim.getVOffset(), BOOK_WIDTH, anim.getHeight(), 896, 720);
      pGuiGraphics.pose().popPose();
      if (anim.isComplete()) {
        anim.reset();
        if (anim.getNextAnim() != null)
          anim = anim.getNextAnim();
        else
          anim = null;
      }
    } else if (anim == CONTENT_APPEAR_ANIM_RIGHT_PAGE_ONLY) {
      drawFromState(pGuiGraphics, localMouseX, localMouseY);
      pGuiGraphics.pose().pushPose();
      pGuiGraphics.pose().translate(0, 0, 201);
      pGuiGraphics.blit(anim.getCurrentFrame(), BOOK_LOCAL_LEFT + BOOK_RIGHT_PAGE_OFFSET_X, BOOK_LOCAL_TOP + anim.getYOffset(),
          BOOK_TEXTURE_OFFSET_X + BOOK_RIGHT_PAGE_OFFSET_X, anim.getVOffset(), BOOK_WIDTH / 2, anim.getHeight(), 896,
          720);
      pGuiGraphics.pose().popPose();
      if (anim.isComplete()) {
        anim.reset();
        if (anim.getNextAnim() != null)
          anim = anim.getNextAnim();
        else
          anim = null;
      }
    } else {
      pGuiGraphics.blit(anim.getCurrentFrame(), BOOK_LOCAL_LEFT, BOOK_LOCAL_TOP + anim.getYOffset(), BOOK_TEXTURE_OFFSET_X,
          anim.getVOffset(), BOOK_WIDTH, anim.getHeight(), 896, 720);
      if (anim.isComplete()) {
        anim.reset();
        if (anim == TABS_APPEAR_ANIM) {
          syncTabButtonState();
        }
        if (anim.getNextAnim() != null)
          anim = anim.getNextAnim();
        else
          anim = null;
      }
    }
    super.render(pGuiGraphics, localMouseX, localMouseY, pPartialTick);
    pGuiGraphics.pose().popPose();
  }

  private void drawFromState(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    pGuiGraphics.blit(pages[DISPLAY_STATE].getPageResource(), BOOK_LOCAL_LEFT, BOOK_LOCAL_TOP, BOOK_TEXTURE_OFFSET_X, 181,
        BOOK_WIDTH, BOOK_HEIGHT, 896, 720);
    if (pages[DISPLAY_STATE].isRender)
      pages[DISPLAY_STATE].renderExtraDrawables(pGuiGraphics, pMouseX, pMouseY);
  }

  @Override
  public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
    return pages[DISPLAY_STATE].handleMouseScrolled(toBookLocalX(pMouseX), toBookLocalY(pMouseY), pDelta);
  }

  @Override
  public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
    return super.mouseClicked(toBookLocalX(pMouseX), toBookLocalY(pMouseY), pButton);
  }

  @Override
  public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
    return super.mouseReleased(toBookLocalX(pMouseX), toBookLocalY(pMouseY), pButton);
  }

  @Override
  public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
    return super.mouseDragged(toBookLocalX(pMouseX), toBookLocalY(pMouseY), pButton, pDragX / bookRenderScale,
        pDragY / bookRenderScale);
  }

  @Override
  public void mouseMoved(double pMouseX, double pMouseY) {
    super.mouseMoved(toBookLocalX(pMouseX), toBookLocalY(pMouseY));
  }

  public void addRenderableWidget(AbstractWidget widget) {
    super.addRenderableWidget(widget);
  }

  public Renderable addRenderableOnly(Renderable renderable) {
    return super.addRenderableOnly(renderable);
  }

  /**
   * Get the X coordinate of the book-local origin used by page widgets and
   * drawables.
   * 
   * @return X coordinate in book-local space
   */
  public int getBookLocalX() {
    return BOOK_LOCAL_LEFT;
  }

  public int getBookLocalY() {
    return BOOK_LOCAL_TOP;
  }

  public WeaponInfo getWeaponInfo() {
    return weaponInfo;
  }

  public void enableScissor(int x, int y, int width, int height) {
    double guiScale = minecraft.getWindow().getGuiScale();
    int scissorX = Mth.floor((bookScreenLeft + x * bookRenderScale) * guiScale);
    int scissorY = Mth
        .floor(minecraft.getWindow().getScreenHeight() - (bookScreenTop + (y + height) * bookRenderScale) * guiScale);
    int scissorWidth = Math.max(0, (int) Math.ceil(width * bookRenderScale * guiScale));
    int scissorHeight = Math.max(0, (int) Math.ceil(height * bookRenderScale * guiScale));
    GL11.glEnable(GL11.GL_SCISSOR_TEST);
    GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
  }

  public void disableScissor() {
    GL11.glDisable(GL11.GL_SCISSOR_TEST);
  }

  private int toBookLocalX(double mouseX) {
    return Mth.floor((mouseX - bookScreenLeft) / bookRenderScale);
  }

  private int toBookLocalY(double mouseY) {
    return Mth.floor((mouseY - bookScreenTop) / bookRenderScale);
  }

}
