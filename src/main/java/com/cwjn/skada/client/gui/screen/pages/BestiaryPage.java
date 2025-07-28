package com.cwjn.skada.client.gui.screen.pages;

import com.cwjn.skada.client.gui.button.EntityButton;
import com.cwjn.skada.client.gui.screen.JournalPage;
import com.cwjn.skada.client.gui.screen.StatScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class BestiaryPage extends JournalPage {

  private List<EntityType<? extends LivingEntity>> entities;

  public BestiaryPage(ResourceLocation icon, ResourceLocation pageResource, StatScreen screen) {
    super(icon, pageResource, screen);
  }

  @Override
  public void init() {
    for (EntityType<? extends LivingEntity> entityType : entities) {

    }
  }

}
