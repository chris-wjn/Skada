package com.cwjn.skada.data.gen.armour;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record ArmourMaterialInfo(Map<Element, Double> elementResists, Map<AttackType, Double> attackResists,  double armourBonus, double armourToughnessBonus) {

    public static ArmourMaterialInfo DEFAULT = new ArmourMaterialInfo(new HashMap<>(), new HashMap<>(), 0, 0);

    public static Codec<ArmourMaterialInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("element_resists").forGetter(ArmourMaterialInfo::getElementResistsAsStringMap),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("attack_type_resists").forGetter(ArmourMaterialInfo::getAttackResistsAsStringMap),
            Codec.DOUBLE.fieldOf("armour_bonus").forGetter(ArmourMaterialInfo::armourBonus),
            Codec.DOUBLE.fieldOf("armour_toughness_bonus").forGetter(ArmourMaterialInfo::armourToughnessBonus)
    ).apply(instance, ArmourMaterialInfo::fromStringMap));

    private Map<String, Double> getAttackResistsAsStringMap() {
        Map<String, Double> retMap = new HashMap<>();
        for (Map.Entry<AttackType, Double> a : attackResists.entrySet()) {
            retMap.put(a.getKey().rl().toString(), a.getValue());
        }
        return retMap;
    }

    private Map<String, Double> getElementResistsAsStringMap() {
        Map<String, Double> retMap = new HashMap<>();
        for (Map.Entry<Element, Double> a : elementResists.entrySet()) {
            retMap.put(a.getKey().rl().toString(), a.getValue());
        }
        return retMap;
    }

    private static ArmourMaterialInfo fromStringMap(Map<String, Double> elementResists, Map<String, Double> attackResists, double armourBonus, double armourToughness) {
        Map<Element, Double> elementMap = new HashMap<>();
        Map<AttackType, Double> attackMap = new HashMap<>();
        for (Map.Entry<String, Double> a : elementResists.entrySet()) {
            elementMap.put(SkadaData.REGISTRY_ELEMENT.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
        }
        for (Map.Entry<String, Double> a : attackResists.entrySet()) {
            attackMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
        }
        return new ArmourMaterialInfo(elementMap, attackMap, armourBonus, armourToughness);
    }

}
