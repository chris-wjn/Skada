package com.cwjn.skada.data.mob;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.AttackType;
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

public record MobData(List<String> parents, AttackType attackType, Multimap<Attribute, AttributeModifier> extraModifiers) {

    private static final Codec<List<AttributeModifier>> ATTRIBUTE_MODIFIER_LIST_CODEC = Codec.list(Util.ATTRIBUTE_MODIFIER_CODEC);

    public static final Codec<MobData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.optionalField("parents", Codec.list(Codec.STRING)).forGetter(mobData -> Optional.ofNullable(mobData.parents)),
            Codec.STRING.fieldOf("attackType").forGetter(mobData -> String.valueOf(mobData.attackType.rl())),
            Codec.unboundedMap(ForgeRegistries.ATTRIBUTES.getCodec(), ATTRIBUTE_MODIFIER_LIST_CODEC)
                    .fieldOf("extraModifiers").forGetter(mobData -> convertToMap(mobData.extraModifiers))
    ).apply(instance, (parents, attackType, extraModifiers) -> new MobData(
            parents.orElse(null),
            SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(ResourceLocation.tryParse(attackType)),
            convertToMultimap(extraModifiers))));

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

    public static final Codec<Map<String, MobData>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, MobData.CODEC);

}
