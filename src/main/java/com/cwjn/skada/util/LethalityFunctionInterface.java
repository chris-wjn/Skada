package com.cwjn.skada.util;

@FunctionalInterface
public interface LethalityFunctionInterface {
    double apply(double lethality, double armorToughness);
}
