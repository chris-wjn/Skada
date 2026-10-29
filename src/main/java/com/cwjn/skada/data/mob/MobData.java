package com.cwjn.skada.data.mob;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.ElementSpreadInstance;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * Data-driven combat and attribute snapshot for a mob entry.
 */
public record MobData(List<String> parents, DamageInfo damageInfo, Multimap<Attribute, AttributeModifier> extraModifiers,
                      Double referenceWeightKg) {

    private static final Codec<List<AttributeModifier>> ATTRIBUTE_MODIFIER_LIST_CODEC = Codec.list(Util.ATTRIBUTE_MODIFIER_CODEC);
    private static final Codec<Map<String, Double>> ELEMENT_SPREAD_CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE);
    private static final Codec<DamageInfo> DAMAGE_INFO_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("attackType").forGetter(info -> info.attackType().rl().toString()),
        Codec.DOUBLE.optionalFieldOf("lethality", 0.0).forGetter(DamageInfo::lethality),
        Codec.DOUBLE.optionalFieldOf("precision", SkadaData.DEFAULT_PRECISION).forGetter(DamageInfo::precision),
        ELEMENT_SPREAD_CODEC.optionalFieldOf("elementSpread", Map.of())
            .forGetter(info -> encodeElementSpread(info.elementSpreadInstance()))
    ).apply(instance, MobData::decodeDamageInfo));

    public static final Codec<MobData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.list(Codec.STRING).optionalFieldOf("parents").forGetter(mobData -> Optional.ofNullable(mobData.parents)),
        Codec.STRING.optionalFieldOf("attackType").forGetter(mobData -> Optional.empty()),
        DAMAGE_INFO_CODEC.optionalFieldOf("damageInfo").forGetter(mobData -> Optional.ofNullable(mobData.damageInfo)),
            Codec.unboundedMap(ForgeRegistries.ATTRIBUTES.getCodec(), ATTRIBUTE_MODIFIER_LIST_CODEC)
            .optionalFieldOf("extraModifiers", Map.of()).forGetter(mobData -> convertToMap(mobData.extraModifiers)),
        Codec.DOUBLE.optionalFieldOf("weight").forGetter(mobData -> Optional.ofNullable(mobData.referenceWeightKg))
    ).apply(instance, (parents, attackType, damageInfo, extraModifiers, referenceWeightKg) -> new MobData(
        parents.orElse(null),
        damageInfo.orElseGet(() -> defaultDamageInfo(resolveAttackType(attackType.orElse(null), AttackType.strike()))),
        convertToMultimap(extraModifiers), referenceWeightKg.orElse(null))));

    public MobData {
        referenceWeightKg = BodyWeightUtil.normalizeReferenceWeight(referenceWeightKg);
    }

    /** Compatibility constructor for data and call sites created before body weight was authored. */
    public MobData(List<String> parents, DamageInfo damageInfo, Multimap<Attribute, AttributeModifier> extraModifiers) {
        this(parents, damageInfo, extraModifiers, null);
    }

    /**
     * Compatibility constructor for legacy call sites that still pass only an attack type.
     */
    public MobData(List<String> parents, AttackType attackType, Multimap<Attribute, AttributeModifier> extraModifiers) {
    this(parents, defaultDamageInfo(attackType), extraModifiers, null);
    }

    /**
     * Returns the direct melee attack type for UI and legacy call sites.
     */
    public AttackType attackType() {
    return damageInfo.attackType();
    }

    /**
     * Creates the deterministic default direct-damage snapshot for a mob attack type.
     */
    public static DamageInfo defaultDamageInfo(AttackType attackType) {
    AttackType resolvedAttackType = attackType == null ? AttackType.strike() : attackType;
    return new DamageInfo(0.0, SkadaData.DEFAULT_PRECISION, false, resolvedAttackType,
        WeaponInfo.FIST.getSpread().instance());
    }

    private static Map<Attribute, List<AttributeModifier>> convertToMap(Multimap<Attribute, AttributeModifier> multimap) {
        HashMap<Attribute, List<AttributeModifier>> returnMap = new HashMap<>();
        for (Map.Entry<Attribute, Collection<AttributeModifier>> entry : multimap.asMap().entrySet()) {
            returnMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return returnMap;
    }

    private static Multimap<Attribute, AttributeModifier> convertToMultimap(Map<Attribute, List<AttributeModifier>> map) {
        Multimap<Attribute, AttributeModifier> returnMap = ArrayListMultimap.create();
        for (Map.Entry<Attribute, List<AttributeModifier>> entry : map.entrySet()) {
            returnMap.putAll(entry.getKey(), entry.getValue());
        }
        return returnMap;
    }

    private static DamageInfo decodeDamageInfo(String attackTypeId, double lethality, double precision,
                                               Map<String, Double> elementSpread) {
        return new DamageInfo(
                lethality,
                precision,
                false,
                resolveAttackType(attackTypeId, AttackType.strike()),
                decodeElementSpread(elementSpread));
    }

    private static AttackType resolveAttackType(String attackTypeId, AttackType fallback) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(attackTypeId);
        if (resourceLocation == null) {
            return fallback;
        }
        AttackType attackType = SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(resourceLocation);
        return attackType != null ? attackType : fallback;
    }

    private static Map<String, Double> encodeElementSpread(ElementSpreadInstance spread) {
        Map<String, Double> encoded = new TreeMap<>();
        for (Map.Entry<Element, Double> entry : spread.getRatios().entrySet()) {
            if (entry.getKey() != null && entry.getKey().rl() != null) {
                encoded.put(entry.getKey().rl().toString(), entry.getValue());
            }
        }
        return encoded;
    }

    private static ElementSpreadInstance decodeElementSpread(Map<String, Double> encoded) {
        Map<Element, Double> decoded = new HashMap<>();
        for (Map.Entry<String, Double> entry : encoded.entrySet()) {
            ResourceLocation elementId = ResourceLocation.tryParse(entry.getKey());
            if (elementId == null) {
                continue;
            }
            Element element = SkadaData.REGISTRY_ELEMENT.get().getValue(elementId);
            if (element != null) {
                decoded.put(element, entry.getValue());
            }
        }
        return decoded.isEmpty() ? WeaponInfo.FIST.getSpread().instance() : new ElementSpreadInstance(decoded);
    }

    public static final Codec<Map<String, MobData>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, MobData.CODEC);

}
