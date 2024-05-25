package com.cwjn.skada.mixin;

import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.util.AccessWeaponInfo;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Item.class)
public abstract class ItemWeaponInfo implements AccessWeaponInfo {

    @Shadow public abstract String getDescriptionId();

    @Unique
    private WeaponInfo skada$weaponInfo;

    @Override
    public WeaponInfo skada$getWeaponInfo() {
        return skada$weaponInfo;
    }

    @Override
    public WeaponInfo skada$getOrCreateWeaponInfo() {
        if (skada$weaponInfo != null) {
            return skada$weaponInfo;
        } else {
            throw new IllegalStateException("Tried to get WeaponInfo object from: " + this.getDescriptionId() + " but it was null!");
        }
    }

    @Override
    public void skada$setWeaponInfo(WeaponInfo info) {
        skada$weaponInfo = info;
    }

    @Override
    public boolean skada$hasWeaponInfo() {
        return skada$weaponInfo != null;
    }

}
