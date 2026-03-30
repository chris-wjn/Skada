package com.cwjn.skada.data.damage;

public class LethalityFunction {

    private final LethalityFunctionInterface func;
    private final Operation op;

    public LethalityFunction(LethalityFunctionInterface func, Operation op) {
        this.func = func;
        this.op = op;
    }

    public Operation getOperation() {
        return op;
    }

    public double apply(double lethality, double armour, double healthContext) {
        return func.apply(lethality, armour, healthContext);
    }

    public enum Operation {
        SUM_WITH_DAMAGE,
        MULTIPLY_WITH_DAMAGE,
        SUM_WITH_ARMOUR,
        MULTIPLY_WITH_ARMOUR
    }

}
