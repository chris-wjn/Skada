package com.cwjn.skada.data.armour;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.generation.armour.ArmourConstructionInfo;
import com.cwjn.skada.generation.armour.ArmourGenerationContext;
import com.cwjn.skada.generation.armour.ArmourGenerationContextFactory;
import com.cwjn.skada.generation.armour.ArmourPieceInfo;
import com.cwjn.skada.generation.weapon.MaterialInfo;
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
                         double armour,
                         double armourToughness,
                         double burden) {

    private static final double ELEMENTAL_RESISTANCE_RATING_SCALE = 100.0;
    private static final double ATTACK_RESISTANCE_RATING_SCALE = 250.0;

    public static Codec<ArmourInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("elemental_resists").forGetter(ArmourInfo::getElementalResistsAsStringMap),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("attack_type_resists").forGetter(ArmourInfo::getAttackResistsAsStringMap),
            Codec.DOUBLE.fieldOf("armour").forGetter(ArmourInfo::armour),
            Codec.DOUBLE.fieldOf("armour_toughness").forGetter(ArmourInfo::armourToughness),
            Codec.DOUBLE.optionalFieldOf("burden", 0.0).forGetter(ArmourInfo::burden)
    ).apply(instance, ArmourInfo::fromStringMap));

    public static ArmourInfo generate(ArmourPieceInfo info, MaterialInfo material, ArmourConstructionInfo construction, double baseArmour, double baseToughness) {
        ArmourGenerationContext context = ArmourGenerationContextFactory.create(material, construction, info);
        Map<Element, Double> elementResists = new HashMap<>();
        Map<AttackType, Double> attackResists = new HashMap<>();

        double elementalScale = Util.round(
                (0.2 * context.normalizedFlexibility())
                        + (0.3 * context.constructionResponse().paddingStrength())
                        + (0.2 * context.constructionResponse().continuityQuality())
                        + (0.3 * context.constructionResponse().effectiveThickness() / 10.0),
                3);
        for (Map.Entry<Element, Double> e : material.spread().getRatios().entrySet()) {
            if (e.getKey().equals(Element.physical())) {
                continue;
            }
            double value = e.getValue()
                * material.spread().getPowerBudget()
                * elementalScale
                * info.elementResistRatio()
                * ELEMENTAL_RESISTANCE_RATING_SCALE;
            if (value > 0.0) {
                elementResists.put(e.getKey(), Util.round(value, 2));
            }
        }

        AttackType slashType = SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation("skada", "slash"));
        AttackType thrustType = SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation("skada", "thrust"));
        AttackType strikeType = SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation("skada", "strike"));

        double effectiveThicknessNorm = Math.min(1.0, context.constructionResponse().effectiveThickness() / 10.0);
        double slashResist = ATTACK_RESISTANCE_RATING_SCALE * 0.4 + (0.9 * context.normalizedHardness()) + (0.8 * context.constructionResponse().continuityQuality())
                + (0.35 * context.constructionResponse().deflectionQuality()) - (0.35 * context.constructionResponse().gapExposure());
        double thrustResist = ATTACK_RESISTANCE_RATING_SCALE * 0.35 + (0.75 * context.normalizedToughness()) + (0.95 * effectiveThicknessNorm)
                + (0.55 * (1.0 - context.constructionResponse().seamWeakness())) - (0.7 * context.constructionResponse().gapExposure());
        double strikeResist = ATTACK_RESISTANCE_RATING_SCALE * 0.2 + (0.7 * context.normalizedDensity()) + (0.8 * context.constructionResponse().paddingStrength())
                + (0.45 * context.constructionResponse().rigidityQuality()) + (0.3 * context.constructionResponse().deflectionQuality());

        attackResists.put(slashType, (double) (int) Math.max(0.0, slashResist * info.attackResistRatio()));
        attackResists.put(thrustType, (double) (int) Math.max(0.0, thrustResist * info.attackResistRatio()));
        attackResists.put(strikeType, (double) (int) Math.max(0.0, strikeResist * info.attackResistRatio()));

        double armour = baseArmour * (1 + (1.15 * context.normalizedDensity()) + (0.65 * context.normalizedHardness())
            + (1.25 * effectiveThicknessNorm) + (0.85 * context.constructionResponse().continuityQuality())) * info.armourRatio();
        double materialProtection = (0.95 * context.normalizedHardness()) + (0.55 * context.normalizedToughness())
            + (0.25 * context.normalizedDensity());
        double constructionProtection = (0.75 * effectiveThicknessNorm) + (0.65 * context.constructionResponse().rigidityQuality())
            + (0.45 * context.constructionResponse().continuityQuality()) + (0.35 * context.constructionResponse().deflectionQuality())
            + (0.2 * context.constructionResponse().paddingStrength()) - (0.35 * context.constructionResponse().gapExposure());
        double toughness = Math.max(0.0, 0.75 + (4.8 * materialProtection) + (2.8 * constructionProtection))
            * info.armourToughnessRatio();
        double burden = context.constructionResponse().burdenFactor() * info.burdenRatio();

        return new ArmourInfo(
                elementResists,
                attackResists,
                Util.round(armour, 2),
                Util.round(toughness, 2),
                Util.round(burden, 2));
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
        tag.putDouble("armour", armour);
        tag.putDouble("armour_toughness", armourToughness);
        tag.putDouble("burden", burden);
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
        return new ArmourInfo(elementMap, attackMap, tag.getDouble("armour"), tag.getDouble("armour_toughness"), tag.getDouble("burden"));
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
    private static ArmourInfo fromStringMap(Map<String, Double> elementalResists, Map<String, Double> attackResists, double armourBonus, double armourToughnessBonus, double burden) {
        //convert the maps of String, Double to maps of Element/AttackType, Double
        Map<Element, Double> elementMap = new HashMap<>();
        Map<AttackType, Double> attackMap = new HashMap<>();
        for (Map.Entry<String, Double> a : elementalResists.entrySet()) {
            elementMap.put(SkadaData.REGISTRY_ELEMENT.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
        }
        for (Map.Entry<String, Double> a : attackResists.entrySet()) {
            attackMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
        }
        return new ArmourInfo(elementMap, attackMap, armourBonus, armourToughnessBonus, burden);
    }
    public static Codec<Map<String, ArmourInfo>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC);
    public static Codec<Map<String, Map<String, ArmourInfo>>> STRING_STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, STRING_MAP_CODEC);

    public static ArmourInfo DEFAULT = new ArmourInfo(Map.of(), Map.of(), 0, 0, 0);
}
