package com.cwjn.skada.util;

import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import org.jetbrains.annotations.NotNull;

public interface SkadaEntity {

    void setWeaponInfo(WeaponInfo info);
    @NotNull WeaponInfo getWeaponInfo();
    AttackType getCurrentAttackType();
    AttackTypeInfo getCurrentAttackTypeInfo();

}
