package com.cwjn.skada.data.registry;

import com.cwjn.skada.SkadaRegistry;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.LethalityFunction;
import com.cwjn.skada.data.gen.attack.AttackTypeGeneratorConfiguration;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.jetbrains.annotations.NotNull;

public record AttackType(String name,
                         LethalityFunction type,
                         AttackTypeGeneratorConfiguration tierStatFunction,
                         Attribute resistAttribute) implements Comparable<AttackType> {

    public Attribute getAttribute() {
        return resistAttribute;
    }

    public ResourceLocation rl() {
        return SkadaData.REGISTRY_ATTACK_TYPE.get().getKey(this);
    }

    public static AttackType none() {
        return SkadaRegistry.NONE.get();
    }

    public static AttackType slash() {
        return SkadaRegistry.SLASH.get();
    }

    public static AttackType thrust() {
        return SkadaRegistry.THRUST.get();
    }

    public static AttackType strike() {
        return SkadaRegistry.STRIKE.get();
    }

    public static Codec<AttackType> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("name").forGetter(AttackType::rl)
            ).apply(instance, rl -> SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(rl))
    );

    @Override
    public int compareTo(@NotNull AttackType o) {
        return this.name.compareTo(o.name);
    }

}


