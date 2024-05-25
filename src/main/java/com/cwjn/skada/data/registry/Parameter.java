package com.cwjn.skada.data.registry;

import com.cwjn.skada.util.SkadaAttributeHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;

public record Parameter(String name, Attribute attribute) implements SkadaAttributeHolder {

    @Override
    public Attribute getAttribute() {
        return attribute;
    }

}
