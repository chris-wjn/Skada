package com.cwjn.skada.data.damage;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.DoubleUnaryOperator;

/*
    * This class is used to store the element spread of a SkadaDamageSource.
 */
public class ElementSpread {

    /*
        * Two maps that represent ratios to convert a total damage value into individual elemental values,
        * and the subsequent map to hold these values.
     */
    private Map<Element, Double> ratios;
    private Map<Element, Float> values = null;
    private final double powerBudget;
    private boolean transformed = false;

    public static final Codec<ElementSpread> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.DOUBLE.fieldOf("powerBudget").forGetter(ElementSpread::powerBudget),
                    Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("ratios").forGetter(ElementSpread::ratios)
            ).apply(instance, ElementSpread::fromStringMap)
    );
    private double powerBudget() {return powerBudget;}
    private Map<String, Double> ratios() {
        Map<String, Double> retMap = new TreeMap<>();
        for (Element e : ratios.keySet()) {
            retMap.put(SkadaData.REGISTRY_ELEMENT.get().getKey(e).toString(), ratios.get(e));
        }
        return retMap;
    }

    /*
     * Construct a new ElementSpread with a power budget and ratio. We start with a basic element taking up the remaining power budget.
     */
    public ElementSpread(double powerBudget, Map<Element, Double> ratios) {
        this.powerBudget = powerBudget;
        this.ratios = ratios;
        double remaining = powerBudget - ratios.values().stream().mapToDouble(aDouble -> aDouble).sum();
        if (remaining > 0) ratios.put(Element.basic(), remaining);
    }

    /*
     * Overload constructor
     */
    public ElementSpread(double powerBudget) {
        this(powerBudget, new HashMap<>());
    }

    /*
     * Overload constructor
     */
    public ElementSpread() {
        this(1);
    }

    /*
        * Construct a new ElementSpread with a power budget and ratio from a string map.
     */
    public static ElementSpread fromStringMap(double powerBudget, Map<String, Double> ratios) {
        Map<Element, Double> retMap = new TreeMap<>();
        for (String s : ratios.keySet()) {
            retMap.put(SkadaData.REGISTRY_ELEMENT.get().getValue(ResourceLocation.tryParse(s)), ratios.get(s));
        }
        return new ElementSpread(powerBudget, retMap);
    }

    public void addRatio(Element element, double ratio) {
        if (!transformed) {
            ratios.put(element, ratio);
        }
        else {
            throw new IllegalStateException("Tried to add ratio to transformed element spread!");
        }
    }

    public void transform(double damage) {
        if (!transformed) {
            double powerRatio = powerBudget/sumRatio();
            values = new HashMap<>();
            for (Element element : ratios.keySet()) {
                values.put(element, (float) Util.round(ratios.get(element) * powerRatio * damage, 2));
            }
            ratios = null;
            transformed = true;
        }
        else {
            throw new IllegalStateException("Tried to transform already transformed element spread!");
        }
    }

    /*
        Applies a function to all elements in the spread.
     */
    public void applyFunctionToAll(DoubleUnaryOperator fn) {
        if (!transformed) {
            throw new IllegalStateException("Tried to apply function to elements before transformation to actual values!");
        }
        values.replaceAll((e, v) -> (float) fn.applyAsDouble(values.get(e)));
    }


    /*
        Applies a function to a specific element in the spread.
     */
    public void applyFunctionToElement(Element e, DoubleUnaryOperator fn) {
        if (!transformed) {
            throw new IllegalStateException("Tried to apply function to element before transformation to a real value!");
        }
        values.put(e, (float) fn.applyAsDouble(values.get(e)));
    }

    public double sum() {
        if (!transformed) {
            throw new IllegalStateException("Tried to sum values before transformation to actual values!");
        }
        return values.values().stream().mapToDouble(Float::doubleValue).sum();
    }

    public double sumRatio() {
        if (transformed) {
            throw new IllegalStateException("Tried to sum ratios after transformation to actual values!");
        }
        return ratios.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public boolean isTransformed() {
        return transformed;
    }

    public double getPowerBudget() {
        return powerBudget;
    }

    public Map<Element, Double> getRatios() {
        if (transformed) {
            throw new IllegalStateException("Tried to get ratios after transformation to actual values!");
        }
        return ratios;
    }

    public double getDamageFromElementRatio(double damage, Element e) {
        if (transformed) {
            throw new IllegalStateException("Tried to get damage from element ratio after transformation to actual values!");
        }
        if (!ratios.containsKey(e)) return 0;
        return damage * ratios.get(e) * powerBudget/sumRatio();
    }

    public Map<Element, Float> getElements() {
        if (!transformed) {
            throw new IllegalStateException("Tried to get elements before transformation to actual values!");
        }
        return values;
    }

    public static ElementSpread fromCompoundTag(CompoundTag tag) {
        double powerBudget = tag.getDouble("powerBudget");
        CompoundTag ratios = tag.getCompound("ratios");
        Map<Element, Double> ratioMap = new TreeMap<>();
        for (String key : ratios.getAllKeys()) {
            ratioMap.put(SkadaData.REGISTRY_ELEMENT.get().getValue(new ResourceLocation(key)), ratios.getDouble(key));
        }
        return new ElementSpread(powerBudget, ratioMap);
    }

    public CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("powerBudget", powerBudget);
        CompoundTag ratioTag = new CompoundTag();
        for (Element e : ratios.keySet()) {
            ratioTag.putDouble(e.rl().toString(), ratios.get(e));
        }
        tag.put("ratios", ratioTag);
        return tag;
    }

}
