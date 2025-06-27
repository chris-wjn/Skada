package com.cwjn.skada.client.hud;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Comparator;
import java.util.List;

public record ReticleCoordinate(int place, float x, float y) {

    public static final Codec<ReticleCoordinate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("place").forGetter(ReticleCoordinate::place),
            Codec.FLOAT.fieldOf("x").forGetter(ReticleCoordinate::x),
            Codec.FLOAT.fieldOf("y").forGetter(ReticleCoordinate::y)
    ).apply(instance, ReticleCoordinate::new));

    public static List<ReticleCoordinate> sortByPlace(List<ReticleCoordinate> coordinates) {
        return coordinates.stream()
                .sorted(Comparator.comparingInt(ReticleCoordinate::place))
                .toList();
    }

}
