package com.cwjn.skada.client.gui.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class WeaponTooltipComponent implements TooltipComponent {

    public final ItemStack item;

    public WeaponTooltipComponent(ItemStack item) {
        this.item = item;
    }

}
