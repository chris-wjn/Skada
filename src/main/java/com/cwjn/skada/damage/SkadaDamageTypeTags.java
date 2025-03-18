package com.cwjn.skada.damage;

import com.cwjn.skada.Skada;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public class SkadaDamageTypeTags {

    public static void init() {}

    public static final TagKey<DamageType> CANCELLED_BY_ARMOUR = create("blocked_by_armour");

    private static TagKey<DamageType> create(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Skada.MODID, name));
    }

}
