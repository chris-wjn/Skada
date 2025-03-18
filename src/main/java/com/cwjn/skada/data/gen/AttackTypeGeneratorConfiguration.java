package com.cwjn.skada.data.gen;

import com.cwjn.skada.util.TierStatFunctionInterface;

public class AttackTypeGeneratorConfiguration {

    private final TierStatFunctionInterface damage;
    private final TierStatFunctionInterface lethality;

    public AttackTypeGeneratorConfiguration(TierStatFunctionInterface damage, TierStatFunctionInterface lethality) {
        this.damage = damage;
        this.lethality = lethality;
    }

    public double getDamageBonus(double weight, double hardness, double toughness, double flexibility) {
        return damage.apply(weight, hardness, toughness, flexibility);
    }

    public double getLethalityBonus(double weight, double hardness, double toughness, double flexibility) {
        return lethality.apply(weight, hardness, toughness, flexibility);
    }

}
