package com.cwjn.skada.client.gui.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class IconTooltipComponent implements TooltipComponent {

    public final ItemStack item;

    public IconTooltipComponent(ItemStack item) {
        this.item = item;
    }

}
