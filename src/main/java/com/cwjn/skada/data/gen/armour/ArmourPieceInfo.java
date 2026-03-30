package com.cwjn.skada.data.gen.armour;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

public record ArmourPieceInfo(
        double elementResistRatio,
        double attackResistRatio,
        double armourRatio,
        double armourToughnessRatio,
        double burdenRatio) {

    public static final ArmourPieceInfo DEFAULT = new ArmourPieceInfo(1, 1, 1, 1, 1);

    @SuppressWarnings("null")
    public static final Codec<ArmourPieceInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("element_resist_ratio", 1.0).forGetter(ArmourPieceInfo::elementResistRatio),
            Codec.DOUBLE.optionalFieldOf("attack_resist_ratio", 1.0).forGetter(ArmourPieceInfo::attackResistRatio),
            Codec.DOUBLE.optionalFieldOf("armour_ratio", 1.0).forGetter(ArmourPieceInfo::armourRatio),
            Codec.DOUBLE.optionalFieldOf("armour_toughness_ratio", 1.0).forGetter(ArmourPieceInfo::armourToughnessRatio),
            Codec.DOUBLE.optionalFieldOf("burden_ratio", 1.0).forGetter(ArmourPieceInfo::burdenRatio))
            .apply(instance, ArmourPieceInfo::new));

    public static final Codec<Map<String, ArmourPieceInfo>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC);

    public ArmourPieceInfo merge(ArmourPieceInfo overrides) {
        return new ArmourPieceInfo(
                overrides.elementResistRatio(),
                overrides.attackResistRatio(),
                overrides.armourRatio(),
                overrides.armourToughnessRatio(),
                overrides.burdenRatio());
    }
}
