package com.cwjn.skada.client.gui.screen;

import com.cwjn.skada.client.gui.button.TabButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class JournalPage {

  protected final ResourceLocation icon;
  protected final ResourceLocation pageResource;
  protected final StatScreen screen;
  protected boolean isRender = false;
  protected TabButton button;

  public JournalPage(ResourceLocation icon, ResourceLocation pageResource, StatScreen screen) {
    this.icon = icon;
    this.pageResource = pageResource;
    this.screen = screen;
  }

  public void init() {
    // This method can be overridden to initialize the page.
  }

  public void renderExtraDrawables(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    // This method can be overridden to draw additional elements on the page.
  }

  public boolean handleMouseScrolled(double pMouseX, double pMouseY, double pDelta) {
    // This method can be overridden to handle mouse scrolling.
    return false;
  }

  public void clearButtons() {

  }

  public void setButton(TabButton button) {
    this.button = button;
  }

  public void setTabButtonActive(boolean active) {
    if (this.button != null) {
      this.button.active = active;
    }
  }

  public void setTabButtonVisible(boolean visible) {
    if (this.button != null) {
      this.button.setRenderIcon(visible);
    }
  }

  public void isRender(boolean render) {
    isRender = render;
  }

  public TabButton getButton() {
    return button;
  }

  public ResourceLocation getIcon() {
    return icon;
  }

  public ResourceLocation getPageResource() {
    return pageResource;
  }

}
