package com.cwjn.skada.data.gen;

import com.cwjn.skada.util.TierStatFunctionInterface;

public class AttackTypeGeneratorConfiguration {

    private final TierStatFunctionInterface lethality;
    private final TierStatFunctionInterface accuracy;
    private final TierStatFunctionInterface critFail;

    public AttackTypeGeneratorConfiguration(TierStatFunctionInterface accuracy, TierStatFunctionInterface lethality, TierStatFunctionInterface critFail) {
        this.lethality = lethality;
        this.accuracy = accuracy;
        this.critFail = critFail;
    }

    public double getAccuracyBonus(double weight, double thickness, double hardness, double toughness, double flexibility, double modifier) {
        return accuracy.apply(weight, thickness, hardness, toughness, flexibility, modifier);
    }

    public double getLethalityBonus(double weight, double thickness,double hardness, double toughness, double flexibility, double modifier) {
        return lethality.apply(weight, thickness, hardness, toughness, flexibility, modifier);
    }

    public double getCritFailChance(double weight, double thickness, double hardness, double toughness, double flexibility, double modifier) {
        return critFail.apply(weight, thickness, hardness, toughness, flexibility, modifier);
    }

}
