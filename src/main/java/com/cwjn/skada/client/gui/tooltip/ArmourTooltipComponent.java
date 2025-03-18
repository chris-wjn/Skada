package com.cwjn.skada.client.gui.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class ArmourTooltipComponent implements TooltipComponent {

    public final ItemStack item;

    public ArmourTooltipComponent(ItemStack item) {
        this.item = item;
    }

}
