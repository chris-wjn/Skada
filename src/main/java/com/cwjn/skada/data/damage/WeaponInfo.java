package com.cwjn.skada.data.damage;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.generation.attack.ElementSpread;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.codecs.OptionalFieldCodec;
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
    private final WeaponStatApplicability applicability;
    public static final WeaponInfo FIST = createDefaultWeaponInfo(AttackTypeInfo.FIST);
    public static final WeaponInfo RANDOM_ITEM = createDefaultWeaponInfo(AttackTypeInfo.ITEM);

    /**
     * Create a default weapon info singleton without failing class initialization in lightweight test environments.
     *
     * @param attackTypeInfo the default attack info to attach when strike is available
     * @return the default weapon info singleton
     */
    private static WeaponInfo createDefaultWeaponInfo(AttackTypeInfo attackTypeInfo) {
        try {
            return new WeaponInfo(Map.of(AttackType.strike(), attackTypeInfo), new ElementSpread(), WeaponStatApplicability.BOTH);
        } catch (RuntimeException exception) {
            return new WeaponInfo(Collections.emptyMap(), new ElementSpread(), WeaponStatApplicability.BOTH);
        }
    }

    // DFU 6's ordinary optionalFieldOf discards parse errors. Only an absent field is legacy data.
    private static final OptionalFieldCodec<WeaponStatApplicability> APPLICABILITY_FIELD =
            new OptionalFieldCodec<>("applicability", WeaponStatApplicability.CODEC) {
                @Override
                public <T> DataResult<Optional<WeaponStatApplicability>> decode(DynamicOps<T> ops, MapLike<T> input) {
                    T value = input.get("applicability");
                    return value == null ? DataResult.success(Optional.empty())
                            : WeaponStatApplicability.CODEC.parse(ops, value).map(Optional::of);
                }
            };

    public static Codec<WeaponInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, AttackTypeInfo.CODEC).fieldOf("attack_types").forGetter(WeaponInfo::attackTypeStringMap),
            ElementSpread.CODEC.fieldOf("spread").forGetter(WeaponInfo::getSpread),
            // Read legacy data, but omit this obsolete setting from every new write.
            Codec.BOOL.optionalFieldOf("ignore_attributes", false).forGetter(info -> false),
            APPLICABILITY_FIELD.forGetter(info -> Optional.of(info.applicability()))
    ).apply(instance, WeaponInfo::fromStringMap));

    private Map<String, AttackTypeInfo> attackTypeStringMap() {
        Map<String, AttackTypeInfo> retMap = new TreeMap<>();
        for (Map.Entry<AttackType, AttackTypeInfo> a : attackTypes.entrySet()) {
            retMap.put(a.getKey().rl().toString(), a.getValue());
        }
        return retMap;
    }

    private static WeaponInfo fromStringMap(Map<String, AttackTypeInfo> map, ElementSpread spread, boolean ignoreAttributes,
                                            Optional<WeaponStatApplicability> applicability) {
        Map<AttackType, AttackTypeInfo> retMap = new TreeMap<>();
        for (Map.Entry<String, AttackTypeInfo> a : map.entrySet()) {
            retMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
        }
        return new WeaponInfo(retMap, spread, applicability.orElse(WeaponStatApplicability.legacy(ignoreAttributes)));
    }

    public static Codec<Map<String, WeaponInfo>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC);
    public static Codec<Map<String, Map<String, WeaponInfo>>> STRING_STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, STRING_MAP_CODEC);

    /** Creates a profile whose applicability also determines melee attribute replacement. */
    public WeaponInfo(Map<AttackType, AttackTypeInfo> attackType, ElementSpread spread, WeaponStatApplicability applicability) {
        this.attackTypes = attackType;
        this.spread = spread;
        this.applicability = Objects.requireNonNull(applicability);
    }

    public WeaponInfo() {
        this(Collections.emptyMap(), new ElementSpread(), WeaponStatApplicability.BOTH);
    }

    public Map<AttackType, AttackTypeInfo> getAttackTypes() {
        return attackTypes;
    }

    public ElementSpread getSpread() {
        return spread;
    }

    /** Returns the uses allowed to consume this profile. */
    public WeaponStatApplicability applicability() { return applicability; }

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
                .append("applicability: ").append(applicability.serializedName()).append("\n")
                .append(attackType).append("\n")
                .append(elements);
    }

    public CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("applicability", applicability.serializedName());
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
            ResourceLocation id = ResourceLocation.tryParse(key);
            AttackType type = id == null ? null : SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(id);
            if (type != null) attackTypeMap.put(type, AttackTypeInfo.fromCompoundTag(attackTypes.getCompound(key)));
        }
        WeaponStatApplicability applicability = WeaponStatApplicability.legacy(ignoreAttributes);
        if (tag.contains("applicability", net.minecraft.nbt.Tag.TAG_STRING)) {
            applicability = WeaponStatApplicability.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("applicability"))
                    .result().orElse(applicability);
        }
        return new WeaponInfo(attackTypeMap, spread, applicability);
    }

}
