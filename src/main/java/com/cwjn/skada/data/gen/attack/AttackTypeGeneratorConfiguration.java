package com.cwjn.skada.data.gen.attack;

import com.cwjn.skada.data.gen.weapon.ExtraTierInfo;
import com.cwjn.skada.data.gen.weapon.WeaponProfile;
import com.cwjn.skada.util.TierStatFunctionInterface;

public record AttackTypeGeneratorConfiguration(TierStatFunctionInterface precision, TierStatFunctionInterface lethality, TierStatFunctionInterface critFail, TierStatFunctionInterface attackSpeed) {

  public double precision(WeaponProfile profile, ExtraTierInfo tierInfo) {
    return precision.apply(profile, tierInfo);
  }

  public double lethality(WeaponProfile profile, ExtraTierInfo tierInfo) {
    return lethality.apply(profile, tierInfo);
  }

  public double criticalFail(WeaponProfile profile, ExtraTierInfo tierInfo) {
    return critFail.apply(profile, tierInfo);
  }

  public double attackSpeed(WeaponProfile profile, ExtraTierInfo tierInfo) {
    return attackSpeed.apply(profile, tierInfo);
  }

}
