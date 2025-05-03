package com.cwjn.skada.data.gen;

import com.cwjn.skada.util.TierStatFunctionInterface;

public class AttackTypeGeneratorConfiguration {

    private final TierStatFunctionInterface lethality;
    private final TierStatFunctionInterface accuracy;

    public AttackTypeGeneratorConfiguration(TierStatFunctionInterface accuracy, TierStatFunctionInterface lethality) {
        this.lethality = lethality;
        this.accuracy = accuracy;
    }

    public double getAccuracyBonus(double weight, double hardness, double toughness, double flexibility) {
        return accuracy.apply(weight, hardness, toughness, flexibility);
    }

    public double getLethalityBonus(double weight, double hardness, double toughness, double flexibility) {
        return lethality.apply(weight, hardness, toughness, flexibility);
    }

}
