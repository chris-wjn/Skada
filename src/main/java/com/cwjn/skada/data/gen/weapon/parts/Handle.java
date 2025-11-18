package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.ExtraTierInfo;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/*
  * Class that represents the handle part of a weapon.
 */
public class Handle {

  private final double length;
  private final double radius;
  private final @Nullable ExtraTierInfo material;

  public Handle(double length, double radius, @Nullable ExtraTierInfo material) {
    this.length = length;
    this.radius = radius;
    this.material = material;
  }

  public double getLength() {
    return length;
  }

  public double getRadius() {
    return radius;
  }

  public double getVolume() {
    return Math.PI * Math.pow(radius, 2) * length;
  }

  public Optional<ExtraTierInfo> getMaterial() {
    return Optional.ofNullable(material);
  }

  public double getPointOfBalance() {
    return length / 2.0;
  }

  public static final Codec<Handle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.DOUBLE.fieldOf("length").forGetter(Handle::getLength),
          Codec.DOUBLE.fieldOf("radius").forGetter(Handle::getRadius),
          ExtraTierInfo.CODEC.optionalFieldOf("material", null).forGetter(h -> h.material)
  ).apply(instance, Handle::new));

}
