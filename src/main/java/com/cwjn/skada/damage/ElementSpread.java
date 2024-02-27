package com.cwjn.skada.damage;

import java.util.function.DoubleUnaryOperator;

/*
    * This class is used to store the element spread of a SkadaDamageSource.
 */
public class ElementSpread {

    /*
        * The elements. 1.0 represents 100% conversion. Once converted they represent real values.
     */
    private double fire, cold, lightning, water, earth, wind, dark, light;
    private boolean transformed = false;

    /*
        * Creates a new ElementSpread with the given values.
        * @param fire The fire ratio.
        * @param cold The cold ratio.
        * @param lightning The lightning ratio.
        * @param water The water ratio.
        * @param earth The earth ratio.
        * @param wind The wind ratio.
        * @param dark The dark ratio.
        * @param light The light ratio.
        * @param powerBudget The power budget to scale the elements by. All element conversion ratios will be scaled
        * so that the sum of all elements is equal to the power budget.
     */
    public ElementSpread(double fire, double cold, double lightning, double water, double earth, double wind, double dark, double light, double powerBudget) {
        double ratio = powerBudget / (fire + cold + lightning + water + earth + wind + dark + light);
        this.fire = fire *ratio;
        this.cold = cold *ratio;
        this.lightning = lightning *ratio;
        this.water = water *ratio;
        this.earth = earth *ratio;
        this.wind = wind *ratio;
        this.dark = dark *ratio;
        this.light = light *ratio;
    }

    public void transform(double damage) {
        transformed = true;
        fire *= damage;
        cold *= damage;
        lightning *= damage;
        water *= damage;
        earth *= damage;
        wind *= damage;
        dark *= damage;
        light *= damage;
    }

    public void applyFunctionToAll(DoubleUnaryOperator fn) {
        fire = fn.applyAsDouble(fire);
        cold = fn.applyAsDouble(cold);
        lightning = fn.applyAsDouble(lightning);
        water = fn.applyAsDouble(water);
        earth = fn.applyAsDouble(earth);
        wind = fn.applyAsDouble(wind);
        dark = fn.applyAsDouble(dark);
        light = fn.applyAsDouble(light);
    }

    //functions that apply a function to a specific element
    public void applyFunctionToFire(DoubleUnaryOperator fn) {
        fire = fn.applyAsDouble(fire);
    }
    public void applyFunctionToCold(DoubleUnaryOperator fn) {
        cold = fn.applyAsDouble(cold);
    }
    public void applyFunctionToLightning(DoubleUnaryOperator fn) {
        lightning = fn.applyAsDouble(lightning);
    }
    public void applyFunctionToWater(DoubleUnaryOperator fn) {
        water = fn.applyAsDouble(water);
    }
    public void applyFunctionToEarth(DoubleUnaryOperator fn) {
        earth = fn.applyAsDouble(earth);
    }
    public void applyFunctionToWind(DoubleUnaryOperator fn) {
        wind = fn.applyAsDouble(wind);
    }
    public void applyFunctionToDark(DoubleUnaryOperator fn) {
        dark = fn.applyAsDouble(dark);
    }
    public void applyFunctionToLight(DoubleUnaryOperator fn) {
        light = fn.applyAsDouble(light);
    }

    public double sum() {
        return fire + cold + lightning + water + earth + wind + dark + light;
    }

    public boolean isTransformed() {
        return transformed;
    }

    public double[] getElements() {
        if (transformed) {
            return new double[] {fire, cold, lightning, water, earth, wind, dark, light};
        } else {
            throw new IllegalStateException("Tried to get element spread before transformation to actual!");
        }
    }

}
