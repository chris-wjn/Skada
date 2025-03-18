package com.cwjn.skada.mixin.new_features;

import com.cwjn.skada.data.armour.AccessArmourInfo;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.damage.AccessWeaponInfo;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Item.class)
public abstract class ItemInfo implements AccessWeaponInfo, AccessArmourInfo {

    @Shadow public abstract String getDescriptionId();

    @Unique
    private WeaponInfo skada$weaponInfo;
    @Unique
    private ArmourInfo skada$armourInfo;

    public WeaponInfo skada$getWeaponInfo() {
        return skada$weaponInfo;
    }
    public ArmourInfo skada$getArmourInfo() {
        return skada$armourInfo;
    }

    public void skada$setWeaponInfo(WeaponInfo info) {
        skada$weaponInfo = info;
    }
    public void skada$setArmourInfo(ArmourInfo info) {
        skada$armourInfo = info;
    }

    public boolean skada$hasWeaponInfo() {
        return skada$weaponInfo != null;
    }
    public boolean skada$hasArmourInfo() {
        return skada$armourInfo != null;
    }

}
