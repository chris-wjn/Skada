package com.cwjn.skada.data.gen;

import com.cwjn.skada.util.TierStatFunctionInterface;

public class AttackTypeGeneratorConfiguration {

    private final TierStatFunctionInterface damage;
    private final TierStatFunctionInterface lethality;
    private final TierStatFunctionInterface aim;

    public AttackTypeGeneratorConfiguration(TierStatFunctionInterface damage, TierStatFunctionInterface lethality, TierStatFunctionInterface aim) {
        this.damage = damage;
        this.lethality = lethality;
        this.aim = aim;
    }

    public double getDamageBonus(double weight, double hardness, double toughness, double flexibility) {
        return damage.apply(weight, hardness, toughness, flexibility);
    }

    public double getLethalityBonus(double weight, double hardness, double toughness, double flexibility) {
        return lethality.apply(weight, hardness, toughness, flexibility);
    }

    public double getAimBonus(double weight, double hardness, double toughness, double flexibility) {
        return aim.apply(weight, hardness, toughness, flexibility);
    }

}
