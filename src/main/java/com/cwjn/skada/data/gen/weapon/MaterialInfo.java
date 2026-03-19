package com.cwjn.skada.data.gen.weapon;

import static com.cwjn.skada.data.SkadaData.MATERIAL_PROPERTY_SOFT_CAP;

import com.cwjn.skada.data.gen.attack.ElementSpread;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;

/**
 * Record representing information about a material.
 * Any instance of MaterialInfo must be associated with a valid Minecraft Tier.
 * Contains material properties and elemental spread data.
 *
 * @param density     density in g/cm^3, default 7.78 (steel), minimum 0.01
 * @param hardness    hardness according to Mohs scale, 0-10. Can go over 10 for
 *                    fictional materials.
 * @param toughness   toughness on a scale of 0-10, where 0 is very brittle and
 *                    10 is very tough. Can go over 10 for fictional materials.
 * @param flexibility flexibility on a scale of 0-10, where 0 is very rigid and
 *                    10 is very flexible. Can go over 10 for fictional
 *                    materials.
 * @param spread      the elemental spread properties associated with this
 *                    material
 */
public record MaterialInfo(double density, double hardness, double toughness, double flexibility,
    ElementSpread spread) {

  // Default material info for steel, with no elemental spread
  public static MaterialInfo getDefault() {
    return new MaterialInfo(7.78, 5.0, 7.0, 4.5, new ElementSpread());
  }

  @SuppressWarnings("null")
  public static Codec<MaterialInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.DOUBLE.fieldOf("density").forGetter(MaterialInfo::density),
      Codec.DOUBLE.fieldOf("hardness").forGetter(MaterialInfo::hardness),
      Codec.DOUBLE.fieldOf("toughness").forGetter(MaterialInfo::toughness),
      Codec.DOUBLE.fieldOf("flexibility").forGetter(MaterialInfo::flexibility),
      ElementSpread.CODEC.fieldOf("spread").forGetter(MaterialInfo::spread))
      .apply(instance, MaterialInfo::validateExtraTierInfo));

  public static MaterialInfo validateExtraTierInfo(double density, double hardness, double toughness,
      double flexibility, ElementSpread spread) {
    return new MaterialInfo(
        Math.max(0.01, density),
        Math.max(0.01, hardness),
        Math.max(0.01, toughness),
        Math.max(0.01, flexibility),
        spread);
  }

  public static double normalizeMaterial(double value) {
    return Mth.clamp(value / MATERIAL_PROPERTY_SOFT_CAP, 0.0, 1.0);
  }

}
