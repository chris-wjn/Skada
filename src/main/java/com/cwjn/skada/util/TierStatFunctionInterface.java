package com.cwjn.skada.util;

@FunctionalInterface
public interface TierStatFunctionInterface {
    double apply(double weight, double thickness, double hardness, double toughness, double flexibility, double modifier);
}
