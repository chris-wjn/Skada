package com.cwjn.skada.data.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record AttackTypeJsonInfo(double effectiveWeight,
                                 double minReach,
                                 double maxReach,
                                 double attackSpeedMod,
                                 List<String> reticleShapes) {

    public static Codec<AttackTypeJsonInfo> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.DOUBLE.fieldOf("effectiveWeight").forGetter(AttackTypeJsonInfo::effectiveWeight),
                    Codec.DOUBLE.fieldOf("minReach").forGetter(AttackTypeJsonInfo::minReach),
                    Codec.DOUBLE.fieldOf("maxReach").forGetter(AttackTypeJsonInfo::maxReach),
                    Codec.DOUBLE.fieldOf("attackSpeedMod").forGetter(AttackTypeJsonInfo::attackSpeedMod),
                    Codec.list(Codec.STRING).optionalFieldOf("reticleShapes", List.of()).forGetter(AttackTypeJsonInfo::reticleShapes)
            ).apply(instance, AttackTypeJsonInfo::new));

}
