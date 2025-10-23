package com.cwjn.skada.util;

import com.cwjn.skada.data.gen.ExtraTierInfo;
import com.cwjn.skada.data.gen.WeaponProfile;

@FunctionalInterface
public interface TierStatFunctionInterface {
    double apply(WeaponProfile profile, ExtraTierInfo tierInfo);
}
