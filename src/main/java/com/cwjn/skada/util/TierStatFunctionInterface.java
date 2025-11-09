package com.cwjn.skada.util;

import com.cwjn.skada.data.gen.weapon.ExtraTierInfo;
import com.cwjn.skada.data.gen.weapon.WeaponProfile;

@FunctionalInterface
public interface TierStatFunctionInterface {
    double apply(WeaponProfile profile, ExtraTierInfo tierInfo);
}
