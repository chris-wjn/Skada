package com.cwjn.skada.data.mob;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BestiaryPreviewSettings(double scale, double offsetX, double offsetY) {

  public static final BestiaryPreviewSettings DEFAULT = new BestiaryPreviewSettings(1.0, 0.0, 0.0);

  public static final Codec<BestiaryPreviewSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.DOUBLE.optionalFieldOf("scale", DEFAULT.scale()).forGetter(BestiaryPreviewSettings::scale),
      Codec.DOUBLE.optionalFieldOf("offset_x", DEFAULT.offsetX()).forGetter(BestiaryPreviewSettings::offsetX),
      Codec.DOUBLE.optionalFieldOf("offset_y", DEFAULT.offsetY()).forGetter(BestiaryPreviewSettings::offsetY)
  ).apply(instance, BestiaryPreviewSettings::new));

  public BestiaryPreviewSettings {
    if (scale <= 0.0D) {
      throw new IllegalArgumentException("Bestiary preview scale must be positive");
    }
  }
}