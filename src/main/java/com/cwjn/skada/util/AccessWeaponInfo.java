package com.cwjn.skada.util;

import com.cwjn.skada.data.damage.WeaponInfo;

public interface AccessWeaponInfo {

    WeaponInfo skada$getWeaponInfo();

    WeaponInfo skada$getOrCreateWeaponInfo();

    void skada$setWeaponInfo(WeaponInfo info);

    boolean skada$hasWeaponInfo();

}
