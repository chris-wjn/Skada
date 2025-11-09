package com.cwjn.skada.data.gen.armour;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

public record ArmourPieceInfo(double elementResistRatio, double attackResistRatio, double armourRatio, double armourToughnessRatio) {

    public static ArmourPieceInfo DEFAULT = new ArmourPieceInfo(1, 1, 1, 1);

    public static Codec<ArmourPieceInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("element_resist_ratio").forGetter(ArmourPieceInfo::elementResistRatio),
            Codec.DOUBLE.fieldOf("attack_resist_ratio").forGetter(ArmourPieceInfo::attackResistRatio),
            Codec.DOUBLE.fieldOf("armour_ratio").forGetter(ArmourPieceInfo::armourRatio),
            Codec.DOUBLE.fieldOf("armour_toughness_ratio").forGetter(ArmourPieceInfo::armourToughnessRatio)
    ).apply(instance, ArmourPieceInfo::new
    ));

    public static Codec<Map<String, ArmourPieceInfo>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC);

}
