package com.cwjn.skada.util;

@FunctionalInterface
public interface LethalityFunctionInterface {
    double apply(double lethality, double armour, double armourToughness, double healthContext);
}
