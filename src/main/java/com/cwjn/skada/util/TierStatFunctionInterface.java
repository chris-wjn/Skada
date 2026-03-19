package com.cwjn.skada.util;

import com.cwjn.skada.data.gen.weapon.WeaponAssembly;

@FunctionalInterface
public interface TierStatFunctionInterface {
    double apply(WeaponAssembly profile);
}
