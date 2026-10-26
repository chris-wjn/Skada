package com.cwjn.skada.data.damage;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.DoubleUnaryOperator;

/**
    This class is used to store the element spread of a SkadaDamageSource.
    Contains two maps: one for the ratios of each element in the spread, and one for the actual damage values of each element after transformation.
    The ratios map is used to calculate the elemental damage values from the total damage of an attack,
    and the values map is used to store the actual elemental damage values after transformation.

    ElementSpreadInstances do not need power budgets, as they are created from ElementSpreads which already have power budgets baked into their ratios.
 */
public class ElementSpreadInstance {

    /*
        * Two maps that represent ratios to convert a total damage value into individual elemental values,
        * and the subsequent map to hold these values.
     */
    private Map<Element, Double> ratios;
    private Map<Element, Float> values = null;
    private boolean transformed = false;

    /*
     * Construct a new ElementSpreadInstance with a power budget and ratio. We start with a physical element taking up the remaining power budget.
     */
    public ElementSpreadInstance(Map<Element, Double> ratios) {
        this.ratios = ratios;
    }

    /*
     * Overload constructor
     */
    public ElementSpreadInstance() {
        this(new HashMap<>());
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
            values = new HashMap<>();
            for (Element element : ratios.keySet()) {
                values.put(element, (float) Util.round(ratios.get(element) * damage, 2));
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
        return damage * ratios.get(e);
    }

    public Map<Element, Float> getElements() {
        if (!transformed) {
            throw new IllegalStateException("Tried to get elements before transformation to actual values!");
        }
        return values;
    }

    /**
     * Creates a defensive copy so projectile snapshots stay immutable across hits.
     */
    public ElementSpreadInstance copy() {
        if (transformed) {
            ElementSpreadInstance copy = new ElementSpreadInstance();
            copy.values = new HashMap<>(values);
            copy.transformed = true;
            return copy;
        }
        return new ElementSpreadInstance(new HashMap<>(ratios));
    }

    public static ElementSpreadInstance fromCompoundTag(CompoundTag tag) {
        CompoundTag ratios = tag.getCompound("ratios");
        Map<Element, Double> ratioMap = new TreeMap<>();
        for (String key : ratios.getAllKeys()) {
            ratioMap.put(SkadaData.REGISTRY_ELEMENT.get().getValue(new ResourceLocation(key)), ratios.getDouble(key));
        }
        return new ElementSpreadInstance(ratioMap);
    }

    public CompoundTag toCompoundTag() {
        if (transformed) {
            throw new IllegalStateException("Tried to serialize a transformed element spread instance!");
        }
        CompoundTag tag = new CompoundTag();
        CompoundTag ratioTag = new CompoundTag();
        for (Element e : ratios.keySet()) {
            ratioTag.putDouble(e.rl().toString(), ratios.get(e));
        }
        tag.put("ratios", ratioTag);
        return tag;
    }

    @Override
    public String toString() {
        if (transformed) {
            return "ElementSpreadInstance{values=" + values + "}";
        }
        return "ElementSpreadInstance{ratios=" + ratios + "}";
    }

}
