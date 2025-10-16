package com.cwjn.skada.data.damage;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.AttackTypeJsonInfo;
import com.cwjn.skada.data.gen.ElementSpread;
import com.cwjn.skada.data.gen.ExtraTierInfo;
import com.cwjn.skada.data.gen.WeaponProfile;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Supplier;

public class WeaponInfo {

    private final Map<AttackType, AttackTypeInfo> attackTypes;
    private final ElementSpread spread;
    private final double weight;
    private final boolean ignoreAttributes;
    public static final WeaponInfo NO_WEAPON = new WeaponInfo(
            new HashMap<>(Map.of(AttackType.strike(), AttackTypeInfo.DEFAULT)),
            new ElementSpread(),
            0.0,
            false
    );

    public static Codec<WeaponInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, AttackTypeInfo.CODEC).fieldOf("attack_types").forGetter(WeaponInfo::attackTypeStringMap),
            ElementSpread.CODEC.fieldOf("spread").forGetter(WeaponInfo::getSpread),
            Codec.DOUBLE.fieldOf("weight").forGetter(WeaponInfo::getWeight),
            Codec.BOOL.optionalFieldOf("ignore_attributes", false).forGetter(WeaponInfo::ignoreAttributes)
    ).apply(instance, WeaponInfo::fromStringMap));

    private Map<String, AttackTypeInfo> attackTypeStringMap() {
        Map<String, AttackTypeInfo> retMap = new TreeMap<>();
        for (Map.Entry<AttackType, AttackTypeInfo> a : attackTypes.entrySet()) {
            retMap.put(a.getKey().rl().toString(), a.getValue());
        }
        return retMap;
    }

    private static WeaponInfo fromStringMap(Map<String, AttackTypeInfo> map, ElementSpread spread, double weight, boolean ignoreAttributes) {
        Map<AttackType, AttackTypeInfo> retMap = new TreeMap<>();
        for (Map.Entry<String, AttackTypeInfo> a : map.entrySet()) {
            retMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
        }
        return new WeaponInfo(retMap, spread, weight, ignoreAttributes);
    }

    public static Codec<Map<String, WeaponInfo>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC);
    public static Codec<Map<String, Map<String, WeaponInfo>>> STRING_STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, STRING_MAP_CODEC);

    public WeaponInfo(Map<AttackType, AttackTypeInfo> attackType, ElementSpread spread, double weight, boolean ignoreAttributes) {
        this.attackTypes = attackType;
        this.spread = spread;
        this.weight = weight;
        this.ignoreAttributes = ignoreAttributes;
    }

    public WeaponInfo() {
        this(Collections.emptyMap(), new ElementSpread(), 0.0, false);
    }

    /*
        * Construct a new WeaponInfo with a given item by guessing weapon info based on attributes and name.
     */
    public static WeaponInfo generate(ExtraTierInfo info, WeaponProfile nInfo, boolean ignoreAttributes) {
        ElementSpread spread = info.spread();
        Map<AttackType, AttackTypeInfo> retMap = new HashMap<>();
        for (Map.Entry<AttackType, AttackTypeJsonInfo> entry : nInfo.attackTypes().entrySet()) {
            AttackTypeJsonInfo genInfo = entry.getValue();
            double weight = info.weight()*nInfo.area()*nInfo.thickness()*genInfo.effectiveWeight();
            retMap.put(entry.getKey(), new AttackTypeInfo(
                    Util.round(entry.getKey().tierStatFunction().getLethalityBonus(
                            weight, nInfo.thickness(), info.hardness(), info.toughness(), info.flexibility(), genInfo.lethalityModifier()), 1),
                    Util.roundAndClamp(entry.getKey().tierStatFunction().getAccuracyBonus(
                            weight, nInfo.thickness(), info.hardness(), info.toughness(), info.flexibility(), genInfo.accuracyModifier()), 2, 0, 1),
                    genInfo.minReach(),
                    genInfo.maxReach(),
                    genInfo.attackSpeedModifier(),
                    0,
                    Util.roundAndClamp(entry.getKey().tierStatFunction().getCritFailChance(
                            weight, nInfo.thickness(), info.hardness(), info.toughness(), info.flexibility(), genInfo.critFailModifier()), 2, 0, 1),
                    entry.getValue().reticleShapes()
            ));
        }
        return new WeaponInfo(retMap, spread, Util.round(info.weight()*nInfo.area(), 1), ignoreAttributes);
    }

    /*
        * Construct a new WeaponInfo with a given item by guessing weapon info based on only name, these items should be looked at manually
     */
    public static WeaponInfo generate(WeaponProfile info, boolean ignoreAttributes) {
        return generate(ExtraTierInfo.getDefault(), info, ignoreAttributes);
    }

    public Map<AttackType, AttackTypeInfo> getAttackTypes() {
        return attackTypes;
    }

    public ElementSpread getSpread() {
        return spread;
    }

    public double getWeight() {
        return weight;
    }

    public boolean ignoreAttributes() {
        return ignoreAttributes;
    }

    public Supplier<Component> toTextComponent() {
        MutableComponent weight = Component.translatable("skada.weapon_info.weight", getWeight());
        MutableComponent attackType = Component.translatable("skada.weapon_info.attack_types");
        for (Map.Entry<AttackType, AttackTypeInfo> entry : attackTypes.entrySet()) {
            attackType.append("\n");
            attackType.append(Component.translatable("skada.weapon_info.attack_type", entry.getKey().name()));
        }
        MutableComponent elements = Component.translatable("skada.weapon_info.elements", this.spread.getPowerBudget());
        for (Map.Entry<Element, Double> e : spread.getRatios().entrySet()) {
            elements.append("\n");
            elements.append(Component.translatable("skada.weapon_info.spread", e.getKey().name(), e.getValue()));
        }

        return () -> Component.empty()
                .append(weight).append("\n")
                .append("ignore_attributes: ").append(String.valueOf(ignoreAttributes)).append("\n")
                .append(attackType).append("\n")
                .append(elements);
    }

    public CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("weight", weight);
        tag.putBoolean("ignore_attributes", ignoreAttributes);
        tag.put("spread", spread.toCompoundTag());
        CompoundTag attackTypes = new CompoundTag();
        for (Map.Entry<AttackType, AttackTypeInfo> entry : this.attackTypes.entrySet()) {
            attackTypes.put(entry.getKey().rl().toString(), entry.getValue().toCompoundTag());
        }
        tag.put("attack_types", attackTypes);
        return tag;
    }

    public static WeaponInfo fromCompoundTag(CompoundTag tag) {
        double weight = tag.getDouble("weight");
        boolean ignoreAttributes = tag.getBoolean("ignore_attributes");
        ElementSpread spread = ElementSpread.fromCompoundTag(tag.getCompound("spread"));
        CompoundTag attackTypes = tag.getCompound("attack_types");
        Map<AttackType, AttackTypeInfo> attackTypeMap = new TreeMap<>();
        for (String key : attackTypes.getAllKeys()) {
            attackTypeMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(key)), AttackTypeInfo.fromCompoundTag(attackTypes.getCompound(key)));
        }
        return new WeaponInfo(attackTypeMap, spread, weight, ignoreAttributes);
    }

}
