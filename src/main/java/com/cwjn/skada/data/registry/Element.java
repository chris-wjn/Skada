package com.cwjn.skada.data.registry;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.util.SkadaAttributeHolder;
import com.cwjn.skada.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.jetbrains.annotations.NotNull;

public record Element(String name,
                      Attribute baseDamage,
                      Attribute affinityAttribute,
                      Attribute resistAttribute,
                      int colour,
                      ResourceLocation icon,
                      TagKey<DamageType> tagKey) implements SkadaAttributeHolder, Comparable<Element> {

    @Override
    public Attribute getAttribute() {
        return resistAttribute;
    }

    public ResourceLocation rl() {
        return SkadaData.REGISTRY_ELEMENT.get().getKey(this);
    }

    public Element getElementByRL(ResourceLocation name) {
        return SkadaData.REGISTRY_ELEMENT.get().getValue(name);
    }

    public static Element basic() {
        return SkadaData.REGISTRY_ELEMENT.get().getValue(Util.rl("basic"));
    }

    public TagKey<DamageType> getTagKey() {
        return tagKey;
    }

    public static Codec<Element> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("name").forGetter(Element::rl)
            ).apply(instance, (rl) -> SkadaData.REGISTRY_ELEMENT.get().getValue(rl))
    );

    @Override
    public int compareTo(@NotNull Element element) {
        return this.name.compareTo(element.name());
    }

}
