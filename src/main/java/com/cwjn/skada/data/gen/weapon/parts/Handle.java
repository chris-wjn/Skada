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

  public double getWeight() {
    double density = material != null ? material.density() : 0.7; // density of oak wood in g/cm³
    return getVolume() * density / 1000.0; // volume in mm³ converted to cm³, then multiplied by density to get weight in grams
  }

  /**
   * Calculate the moment of inertia of the handle using the base of the handle as the pivot point.
   * @return Moment of inertia in g·mm².
   */
  public double getMomentOfInertia() {
    double mass = getWeight(); // mass in grams
    // I = 1/2 * M * R^2 + M * (L/2)^2
    return (0.5 * mass * Math.pow(radius, 2)) + (mass * Math.pow(length / 2.0, 2));
  }

  public Optional<ExtraTierInfo> getMaterial() {
    return Optional.ofNullable(material);
  }

  public double getPointOfBalance() {
    return length / 2.0;
  }

  public static final Codec<Handle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.DOUBLE.fieldOf("length").forGetter(Handle::getLength),
          Codec.DOUBLE.fieldOf("radius").forGetter(Handle::getRadius)
  ).apply(instance, (len, rad) -> new Handle(len, rad, null)));

}
