package com.cwjn.skada.data.gen;

import com.cwjn.skada.util.TierStatFunctionInterface;

public class AttackTypeGeneratorConfiguration {

    private final TierStatFunctionInterface lethality;
    private final TierStatFunctionInterface accuracy;
    private final TierStatFunctionInterface critFail;
    private final TierStatFunctionInterface attackSpeed;

    public AttackTypeGeneratorConfiguration(TierStatFunctionInterface accuracy, TierStatFunctionInterface lethality, TierStatFunctionInterface critFail, TierStatFunctionInterface attackSpeed) {
        this.lethality = lethality;
        this.accuracy = accuracy;
        this.critFail = critFail;
        this.attackSpeed = attackSpeed;
    }

    public double precision(WeaponProfile profile, ExtraTierInfo tierInfo) {
        return accuracy.apply(profile, tierInfo);
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
