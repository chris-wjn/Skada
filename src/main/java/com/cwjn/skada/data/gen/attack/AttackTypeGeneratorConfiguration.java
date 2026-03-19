package com.cwjn.skada.data.gen.attack;

import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.util.TierStatFunctionInterface;

public record AttackTypeGeneratorConfiguration(TierStatFunctionInterface precision, TierStatFunctionInterface lethality, TierStatFunctionInterface critFail, TierStatFunctionInterface attackSpeed) {

  public double precision(WeaponAssembly profile) {
    return precision.apply(profile);
  }

  public double lethality(WeaponAssembly profile) {
    return lethality.apply(profile);
  }

  public double criticalFail(WeaponAssembly profile) {
    return critFail.apply(profile);
  }

  public double attackSpeed(WeaponAssembly profile) {
    return attackSpeed.apply(profile);
  }

}
