package com.cwjn.skada.data.gen;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.ElementSpreadInstance;
import com.cwjn.skada.data.registry.Element;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ElementSpread {

    private final Map<Element, Double> ratios;
    private final double powerBudget;
    private final double sumRatio;

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
    public static ElementSpread fromStringMap(double powerBudget, Map<String, Double> ratios) {
        Map<Element, Double> retMap = new TreeMap<>();
        for (String s : ratios.keySet()) {
            retMap.put(SkadaData.REGISTRY_ELEMENT.get().getValue(ResourceLocation.tryParse(s)), ratios.get(s));
        }
        return new ElementSpread(powerBudget, retMap);
    }

    public ElementSpread(double powerBudget, Map<Element, Double> ratios) {
        this.powerBudget = powerBudget;
        this.ratios = ratios;
        double remaining = powerBudget - ratios.values().stream().mapToDouble(aDouble -> aDouble).sum();
        if (remaining > 0) ratios.put(Element.basic(), remaining);
        sumRatio = ratios.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public ElementSpread(double powerBudget) {
        this(powerBudget, new HashMap<>());
    }

    public ElementSpread() {
        this(1);
    }

    public double getPowerBudget() {
        return powerBudget;
    }

    public Map<Element, Double> getRatios() {
        return ratios;
    }

    public double sumRatio() {
        return sumRatio;
    }

    public ElementSpreadInstance instance() {
        return new ElementSpreadInstance(powerBudget, new HashMap<>(ratios));
    }

    public double getDamageFromElementRatio(double damage, Element e) {
        if (!ratios.containsKey(e)) return 0;
        return damage * ratios.get(e) * powerBudget/sumRatio;
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
