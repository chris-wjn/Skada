package com.cwjn.skada.data.damage;

@FunctionalInterface
public interface LethalityFunctionInterface {
    double apply(double lethality, double armour, double healthContext);
}
