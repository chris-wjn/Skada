package com.cwjn.skada.client.gui.button;


import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class EntityButton extends Button {

  protected EntityButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress, CreateNarration pCreateNarration) {
    super(pX, pY, pWidth, pHeight, pMessage, pOnPress, pCreateNarration);
  }

}
