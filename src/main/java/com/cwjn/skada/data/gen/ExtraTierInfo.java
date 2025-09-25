package com.cwjn.skada.data.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ExtraTierInfo(double weight, double hardness, double toughness, double flexibility,
                            ElementSpread spread) {

  public static ExtraTierInfo getDefault() {
    return new ExtraTierInfo(0.05, 1.0, 1.0, 1.0, new ElementSpread());
  }

  public static Codec<ExtraTierInfo> CODEC = RecordCodecBuilder.create(
          instance -> instance.group(
                  Codec.DOUBLE.fieldOf("weight").forGetter(ExtraTierInfo::weight),
                  Codec.DOUBLE.fieldOf("hardness").forGetter(ExtraTierInfo::hardness),
                  Codec.DOUBLE.fieldOf("toughness").forGetter(ExtraTierInfo::toughness),
                  Codec.DOUBLE.fieldOf("flexibility").forGetter(ExtraTierInfo::flexibility),
                  ElementSpread.CODEC.fieldOf("spread").forGetter(ExtraTierInfo::spread)
          ).apply(instance, ExtraTierInfo::validateExtraTierInfo)
  );

  public static ExtraTierInfo validateExtraTierInfo(double weight, double hardness, double toughness, double flexibility, ElementSpread spread) {
    return new ExtraTierInfo(
            Math.max(0.01, weight),
            Math.max(0.01, hardness),
            Math.max(0.01, toughness),
            Math.max(0.01, flexibility),
            spread
    );
  }

}
