package com.cwjn.skada.util;

import com.cwjn.skada.data.damage.WeaponInfo;

public interface AccessPlayerWeaponInfo {

    void setWeaponInfo(WeaponInfo info);
    WeaponInfo getWeaponInfo();
    void setAttackTypeIndex(int index);
    int getAttackTypeIndex();

}
