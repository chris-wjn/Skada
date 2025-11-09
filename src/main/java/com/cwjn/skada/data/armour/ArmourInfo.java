package com.cwjn.skada.data.armour;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.armour.ArmourMaterialInfo;
import com.cwjn.skada.data.gen.armour.ArmourPieceInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/*
 Stores information about an armour's resistances and bonuses. Used to attach modifiers to armour pieces.
 Stored in the armour's NBT data in game, in resources out of game. Loaded on server start.
 */
public record ArmourInfo(Map<Element, Double> elementalResists,
                         Map<AttackType, Double> attackResists,
                         double armourBonus,
                         double armourToughnessBonus) {

    public static Codec<ArmourInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("elemental_resists").forGetter(ArmourInfo::getElementalResistsAsStringMap),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("attack_type_resists").forGetter(ArmourInfo::getAttackResistsAsStringMap),
            Codec.DOUBLE.fieldOf("armour_bonus").forGetter(ArmourInfo::armourBonus),
            Codec.DOUBLE.fieldOf("armour_toughness_bonus").forGetter(ArmourInfo::armourToughnessBonus)
    ).apply(instance, ArmourInfo::fromStringMap));

    public static ArmourInfo generate(ArmourPieceInfo info, ArmourMaterialInfo material) {
        Map<Element, Double> elementResists = new HashMap<>();
        Map<AttackType, Double> attackResists = new HashMap<>();
        for (Map.Entry<Element, Double> e : material.elementResists().entrySet()) {
            elementResists.put(e.getKey(), Util.round(e.getValue() * info.elementResistRatio(), 2));
        }
        for (Map.Entry<AttackType, Double> e : material.attackResists().entrySet()) {
            attackResists.put(e.getKey(), Util.round(e.getValue() * info.attackResistRatio(), 2));
        }
        return new ArmourInfo(
                elementResists,
                attackResists,
                Util.round(material.armourBonus() * info.armourRatio(), 2),
                Util.round(material.armourToughnessBonus() * info.armourToughnessRatio(), 2));
    }

    public CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
        CompoundTag elementTag = new CompoundTag();
        CompoundTag attackTag = new CompoundTag();
        for (Map.Entry<Element, Double> a : elementalResists.entrySet()) {
            elementTag.putDouble(a.getKey().rl().toString(), a.getValue());
        }
        for (Map.Entry<AttackType, Double> a : attackResists.entrySet()) {
            attackTag.putDouble(a.getKey().rl().toString(), a.getValue());
        }
        tag.putDouble("armour_bonus", armourBonus);
        tag.putDouble("armour_toughness_bonus", armourToughnessBonus);
        tag.put("elemental_resists", elementTag);
        tag.put("attack_resists", attackTag);
        return tag;
    }

    public static ArmourInfo fromCompoundTag(CompoundTag tag) {
        Map<Element, Double> elementMap = new HashMap<>();
        Map<AttackType, Double> attackMap = new HashMap<>();
        CompoundTag elementTag = tag.getCompound("elemental_resists");
        CompoundTag attackTag = tag.getCompound("attack_resists");
        for (String key : elementTag.getAllKeys()) {
            elementMap.put(SkadaData.REGISTRY_ELEMENT.get().getValue(new ResourceLocation(key)), elementTag.getDouble(key));
        }
        for (String key : attackTag.getAllKeys()) {
            attackMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(key)), attackTag.getDouble(key));
        }
        return new ArmourInfo(elementMap, attackMap, tag.getDouble("armour_bonus"), tag.getDouble("armour_toughness_bonus"));
    }

    private Map<String, Double> getAttackResistsAsStringMap() {
        //convert attackResists to a map of String, Double that uses the attackType's registry name as the key
        Map<String, Double> retMap = new HashMap<>();
        for (Map.Entry<AttackType, Double> a : attackResists.entrySet()) {
            retMap.put(a.getKey().rl().toString(), a.getValue());
        }
        return retMap;
    }
    private Map<String, Double> getElementalResistsAsStringMap() {
        //convert elementalResists to a map of String, Double that uses the element's registry name as the key
        Map<String, Double> retMap = new HashMap<>();
        for (Map.Entry<Element, Double> a : elementalResists.entrySet()) {
            retMap.put(a.getKey().rl().toString(), a.getValue());
        }
        return retMap;
    }
    private static ArmourInfo fromStringMap(Map<String, Double> elementalResists, Map<String, Double> attackResists, double armourBonus, double armourToughnessBonus) {
        //convert the maps of String, Double to maps of Element/AttackType, Double
        Map<Element, Double> elementMap = new HashMap<>();
        Map<AttackType, Double> attackMap = new HashMap<>();
        for (Map.Entry<String, Double> a : elementalResists.entrySet()) {
            elementMap.put(SkadaData.REGISTRY_ELEMENT.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
        }
        for (Map.Entry<String, Double> a : attackResists.entrySet()) {
            attackMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
        }
        return new ArmourInfo(elementMap, attackMap, armourBonus, armourToughnessBonus);
    }
    public static Codec<Map<String, ArmourInfo>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC);
    public static Codec<Map<String, Map<String, ArmourInfo>>> STRING_STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, STRING_MAP_CODEC);

    public static ArmourInfo DEFAULT = new ArmourInfo(Map.of(), Map.of(), 0, 0);
}
