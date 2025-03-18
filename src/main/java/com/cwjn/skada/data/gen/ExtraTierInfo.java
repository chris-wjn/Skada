package com.cwjn.skada.data.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ExtraTierInfo(double weight, double hardness, double toughness, double flexibility, ElementSpread spread) {

    public static Codec<ExtraTierInfo> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.DOUBLE.fieldOf("weight").forGetter(ExtraTierInfo::weight),
                    Codec.DOUBLE.fieldOf("hardness").forGetter(ExtraTierInfo::hardness),
                    Codec.DOUBLE.fieldOf("toughness").forGetter(ExtraTierInfo::toughness),
                    Codec.DOUBLE.fieldOf("flexibility").forGetter(ExtraTierInfo::flexibility),
                    ElementSpread.CODEC.fieldOf("spread").forGetter(ExtraTierInfo::spread)
            ).apply(instance, ExtraTierInfo::new)
    );

}
