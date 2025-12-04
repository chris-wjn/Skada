package com.cwjn.skada.data.damage;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.attack.AttackTypeJsonInfo;
import com.cwjn.skada.data.gen.attack.ElementSpread;
import com.cwjn.skada.data.gen.weapon.ExtraTierInfo;
import com.cwjn.skada.data.gen.weapon.WeaponProfile;
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
    private final boolean ignoreAttributes;
    public static final WeaponInfo NO_WEAPON = new WeaponInfo(
            new HashMap<>(Map.of(AttackType.strike(), AttackTypeInfo.DEFAULT)),
            new ElementSpread(),
            false
    );

    public static Codec<WeaponInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, AttackTypeInfo.CODEC).fieldOf("attack_types").forGetter(WeaponInfo::attackTypeStringMap),
            ElementSpread.CODEC.fieldOf("spread").forGetter(WeaponInfo::getSpread),
            Codec.BOOL.optionalFieldOf("ignore_attributes", false).forGetter(WeaponInfo::ignoreAttributes)
    ).apply(instance, WeaponInfo::fromStringMap));

    private Map<String, AttackTypeInfo> attackTypeStringMap() {
        Map<String, AttackTypeInfo> retMap = new TreeMap<>();
        for (Map.Entry<AttackType, AttackTypeInfo> a : attackTypes.entrySet()) {
            retMap.put(a.getKey().rl().toString(), a.getValue());
        }
        return retMap;
    }

    private static WeaponInfo fromStringMap(Map<String, AttackTypeInfo> map, ElementSpread spread, boolean ignoreAttributes) {
        Map<AttackType, AttackTypeInfo> retMap = new TreeMap<>();
        for (Map.Entry<String, AttackTypeInfo> a : map.entrySet()) {
            retMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
        }
        return new WeaponInfo(retMap, spread, ignoreAttributes);
    }

    public static Codec<Map<String, WeaponInfo>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC);
    public static Codec<Map<String, Map<String, WeaponInfo>>> STRING_STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, STRING_MAP_CODEC);

    public WeaponInfo(Map<AttackType, AttackTypeInfo> attackType, ElementSpread spread, boolean ignoreAttributes) {
        this.attackTypes = attackType;
        this.spread = spread;
        this.ignoreAttributes = ignoreAttributes;
    }

    public WeaponInfo() {
        this(Collections.emptyMap(), new ElementSpread(), false);
    }

    /*
        * Construct a new WeaponInfo with a given item by guessing weapon info based on attributes and name.
     */
    public static WeaponInfo generate(ExtraTierInfo tierInfo, WeaponProfile profile, boolean ignoreAttributes) {
      ElementSpread spread = tierInfo.spread();
      Map<AttackType, AttackTypeInfo> retMap = new HashMap<>();
      for (Map.Entry<AttackType, AttackTypeJsonInfo> entry : profile.getAttackTypes().entrySet()) {
        double lethality = entry.getKey().tierStatFunction().lethality(profile, tierInfo);
        double precision = entry.getKey().tierStatFunction().precision(profile, tierInfo);
        double critFailChance = entry.getKey().tierStatFunction().criticalFail(profile, tierInfo);
        double attackSpeed = entry.getKey().tierStatFunction().attackSpeed(profile, tierInfo);
        retMap.put(entry.getKey(), new AttackTypeInfo(
                Util.round(lethality*entry.getValue().lethalityModifier(), 2),
                Util.round(precision*entry.getValue().precisionModifier(), 2),
                entry.getValue().minReach(),
                entry.getValue().maxReach(),
                Util.round(attackSpeed*entry.getValue().attackSpeedModifier(), 3),
                0.0,
                Util.round(critFailChance*entry.getValue().critFailModifier(), 3),
                entry.getValue().reticleShapes()));
      }
      return new WeaponInfo(retMap, spread, ignoreAttributes);
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

    public boolean ignoreAttributes() {
        return ignoreAttributes;
    }

    public Supplier<Component> toTextComponent() {
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
                .append("ignore_attributes: ").append(String.valueOf(ignoreAttributes)).append("\n")
                .append(attackType).append("\n")
                .append(elements);
    }

    public CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
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
        boolean ignoreAttributes = tag.getBoolean("ignore_attributes");
        ElementSpread spread = ElementSpread.fromCompoundTag(tag.getCompound("spread"));
        CompoundTag attackTypes = tag.getCompound("attack_types");
        Map<AttackType, AttackTypeInfo> attackTypeMap = new TreeMap<>();
        for (String key : attackTypes.getAllKeys()) {
            attackTypeMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(key)), AttackTypeInfo.fromCompoundTag(attackTypes.getCompound(key)));
        }
        return new WeaponInfo(attackTypeMap, spread, ignoreAttributes);
    }

}
