package com.cwjn.skada.damage;

import com.cwjn.skada.Skada;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public class SkadaDamageTags {

    public static void init() {}

    public static final TagKey<DamageType> CONVERT_FIRE = create("convert_fire");
    public static final TagKey<DamageType> CONVERT_COLD = create("convert_cold");
    public static final TagKey<DamageType> CONVERT_LIGHTNING = create("convert_lightning");
    public static final TagKey<DamageType> CONVERT_WATER = create("convert_water");
    public static final TagKey<DamageType> CONVERT_EARTH = create("convert_earth");
    public static final TagKey<DamageType> CONVERT_WIND = create("convert_wind");
    public static final TagKey<DamageType> CONVERT_DARK = create("convert_dark");
    public static final TagKey<DamageType> CONVERT_LIGHT = create("convert_light");

    private static TagKey<DamageType> create(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Skada.MODID, name));
    }

}
