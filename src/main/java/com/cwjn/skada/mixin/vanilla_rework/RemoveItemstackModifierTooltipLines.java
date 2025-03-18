package com.cwjn.skada.mixin.vanilla_rework;

import com.cwjn.skada.network.SkadaNetwork;
import com.cwjn.skada.network.client_to_server.C2SAddWeaponTag;
import com.cwjn.skada.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(ItemStack.class)
public abstract class RemoveItemstackModifierTooltipLines {

    @Shadow public abstract Item getItem();

    @Shadow @Nullable public abstract CompoundTag getTag();
    private ItemStack thisItemStack() {
        return (ItemStack) (Object) this;
    }

    @Redirect(
            method = "getTooltipLines",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;shouldShowInTooltip(ILnet/minecraft/world/item/ItemStack$TooltipPart;)Z",
                    ordinal = 4
            )
    )
    public boolean getTooltipLines(int pHideFlags, ItemStack.TooltipPart pPart) {
        if (shouldShowInTooltip(pHideFlags, ItemStack.TooltipPart.MODIFIERS)) {
            //we tell the server to add a weapon tag to the item and then add it on the client for convenience.
            //since we're guaranteed to be in a tooltip, we don't need to null check the screen/item
            if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getSlotUnderMouse() != null) {
                SkadaNetwork.playerToServer(new C2SAddWeaponTag(screen.getMenu().containerId, screen.getSlotUnderMouse().index));
            }
            Util.addWeaponInfoTagIfNotExists((ItemStack) (Object) this);
        }
        return false;
    }

    @Shadow
    private static boolean shouldShowInTooltip(int pHideFlags, ItemStack.TooltipPart pPart) {
        return false;
    }

    @Shadow
    private int getHideFlags() {
        return 0;
    }


}
