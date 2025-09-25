package com.cwjn.skada.data.gen;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.AttackType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record NamedInfo(double area, double thickness, Map<AttackType, AttackTypeJsonInfo> attackTypes) {

    private static final Map<AttackType, AttackTypeJsonInfo> DEFAULT_MAP = new HashMap<>(
            Map.of(AttackType.strike(), AttackTypeJsonInfo.getDefault())
    );

    public static final Codec<NamedInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("area").forGetter(NamedInfo::area),
            Codec.DOUBLE.fieldOf("thickness").forGetter(NamedInfo::thickness),
            Codec.unboundedMap(Codec.STRING, AttackTypeJsonInfo.CODEC).fieldOf("attackTypes").forGetter(NamedInfo::attackTypeStringMap)
    ).apply(instance, NamedInfo::fromStringMap));
    public static final Codec<Map<String, NamedInfo>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC);
    private Map<String, AttackTypeJsonInfo> attackTypeStringMap() {
        Map<String, AttackTypeJsonInfo> retMap = new HashMap<>();
        for (Map.Entry<AttackType, AttackTypeJsonInfo> a : attackTypes.entrySet()) {
            retMap.put(a.getKey().rl().toString(), a.getValue());
        }
        return retMap;
    }
    private static NamedInfo fromStringMap(double area, double thickness, Map<String, AttackTypeJsonInfo> map) {
        Map<AttackType, AttackTypeJsonInfo> retMap = new HashMap<>();
        for (Map.Entry<String, AttackTypeJsonInfo> a : map.entrySet()) {
            retMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
        }
        return new NamedInfo(area, thickness, retMap);
    }

    public NamedInfo() {
        this(1.0, 1.0, DEFAULT_MAP);
    }

}
