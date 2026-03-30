package com.cwjn.skada.data.damage;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.attack.AttackTypeJsonInfo;
import com.cwjn.skada.data.gen.attack.ElementSpread;
import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.*;
import java.util.function.Supplier;

public class WeaponInfo {

    private static final WeaponInfo NO_WEAPON_VALUE = new WeaponInfo(Collections.emptyMap(), new ElementSpread(), false);
    private static final double ATTACK_SPEED_SOFTCAP_LOW = 0.5;
    private static final double ATTACK_SPEED_SOFTCAP_HIGH = 2.5;
    private static final double ATTACK_SPEED_LOW_TAIL = 0.04;
    private static final double ATTACK_SPEED_HIGH_TAIL = 0.2;

    private final Map<AttackType, AttackTypeInfo> attackTypes;
    private final ElementSpread spread;
    private final boolean ignoreAttributes;
    public static final WeaponInfo NO_WEAPON = NO_WEAPON_VALUE;

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
        * Construct a new WeaponInfo with a given item by guessing weapon info based on material and name.
        * This is for weapons that are generated from json. The WeaponAssembly will be automatically
        * 
     */
        public static WeaponInfo generate(MaterialInfo material, WeaponAssembly assembly, boolean ignoreAttributes) {
            return generate(material, assembly, ignoreAttributes, Attributes.ATTACK_SPEED.getDefaultValue(), 0.0);
        }

        public static WeaponInfo generate(MaterialInfo material, WeaponAssembly assembly, boolean ignoreAttributes, double damageModifier) {
            return generate(material, assembly, ignoreAttributes, Attributes.ATTACK_SPEED.getDefaultValue(), damageModifier);
        }

        public static WeaponInfo generate(MaterialInfo material, WeaponAssembly assembly, boolean ignoreAttributes, double baseAttackSpeed, double damageModifier) {
      assembly = assembly.withMaterialWoodenHandle(material);
      ElementSpread spread = material.spread();
      Map<AttackType, AttackTypeInfo> retMap = new HashMap<>();
      for (Map.Entry<AttackType, AttackTypeJsonInfo> entry : assembly.getAttackTypes().entrySet()) {
        var generatedStats = entry.getKey().tierStatFunction().generateAll(assembly, entry.getKey());
        retMap.put(entry.getKey(), AttackTypeInfo.of(
            Util.round(generatedStats.lethality()*entry.getValue().lethalityModifier(), 2),
            Util.round(generatedStats.precision()*entry.getValue().precisionModifier(), 2),
                entry.getValue().minReach(),
                entry.getValue().maxReach(),
                        resolveAttackSpeed(baseAttackSpeed, generatedStats.attackSpeed(), entry.getValue().attackSpeed()),
                        Util.round(damageModifier + entry.getValue().damage(), 3),
                        Util.round(generatedStats.criticalFail()*entry.getValue().critFailModifier(), 3),
                        entry.getValue().reticleShapes()));
      }
      return new WeaponInfo(retMap, spread, ignoreAttributes);
    }

    private static double resolveAttackSpeed(double baseAttackSpeed, double generatedAttackSpeedAdjustment, double configuredAttackSpeed) {
        if (Double.isFinite(configuredAttackSpeed)) {
            return Util.round(configuredAttackSpeed, 3);
        }
        return Util.round(softCapGeneratedAttackSpeed(baseAttackSpeed + generatedAttackSpeedAdjustment), 3);
    }

    private static double softCapGeneratedAttackSpeed(double generatedAttackSpeed) {
        if (generatedAttackSpeed < ATTACK_SPEED_SOFTCAP_LOW) {
            return ATTACK_SPEED_SOFTCAP_LOW - ATTACK_SPEED_LOW_TAIL
                    * (1.0 - Math.exp((generatedAttackSpeed - ATTACK_SPEED_SOFTCAP_LOW) / ATTACK_SPEED_LOW_TAIL));
        }
        if (generatedAttackSpeed > ATTACK_SPEED_SOFTCAP_HIGH) {
            return ATTACK_SPEED_SOFTCAP_HIGH + ATTACK_SPEED_HIGH_TAIL
                    * (1.0 - Math.exp((ATTACK_SPEED_SOFTCAP_HIGH - generatedAttackSpeed) / ATTACK_SPEED_HIGH_TAIL));
        }
        return generatedAttackSpeed;
    }

    /*
        * Construct a new WeaponInfo with a given item by guessing weapon info based on only name, these items should be looked at manually
     */
    public static WeaponInfo generate(WeaponAssembly info, boolean ignoreAttributes) {
        return generate(MaterialInfo.getDefault(), info, ignoreAttributes, Attributes.ATTACK_SPEED.getDefaultValue(), 0.0);
    }

    public static WeaponInfo generate(WeaponAssembly info, boolean ignoreAttributes, double damageModifier) {
        return generate(MaterialInfo.getDefault(), info, ignoreAttributes, Attributes.ATTACK_SPEED.getDefaultValue(), damageModifier);
    }

    public static WeaponInfo generate(WeaponAssembly info, boolean ignoreAttributes, double baseAttackSpeed, double damageModifier) {
        return generate(MaterialInfo.getDefault(), info, ignoreAttributes, baseAttackSpeed, damageModifier);
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
