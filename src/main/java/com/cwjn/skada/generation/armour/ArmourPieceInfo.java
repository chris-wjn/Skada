package com.cwjn.skada.generation.armour;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.cwjn.skada.generation.weapon.MaterialInfo;

import java.util.Map;

public record ArmourPieceInfo(
        double elementResistRatio,
        double attackResistRatio,
        double armourRatio,
        double armourToughnessRatio,
        double burdenRatio,
        double coverageAreaCm2,
        double weightKg) {

    public static final ArmourPieceInfo DEFAULT = new ArmourPieceInfo(1, 1, 1, 1, 1, 0, -1);

    /** Retains the legacy constructor; no area or manual mass is inferred for old definitions. */
    public ArmourPieceInfo(double elementResistRatio, double attackResistRatio, double armourRatio,
                           double armourToughnessRatio, double burdenRatio) {
        this(elementResistRatio, attackResistRatio, armourRatio, armourToughnessRatio, burdenRatio, 0, -1);
    }

    @SuppressWarnings("null")
    public static final Codec<ArmourPieceInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("element_resist_ratio", 1.0).forGetter(ArmourPieceInfo::elementResistRatio),
            Codec.DOUBLE.optionalFieldOf("attack_resist_ratio", 1.0).forGetter(ArmourPieceInfo::attackResistRatio),
            Codec.DOUBLE.optionalFieldOf("armour_ratio", 1.0).forGetter(ArmourPieceInfo::armourRatio),
            Codec.DOUBLE.optionalFieldOf("armour_toughness_ratio", 1.0).forGetter(ArmourPieceInfo::armourToughnessRatio),
            Codec.DOUBLE.optionalFieldOf("burden_ratio", 1.0).forGetter(ArmourPieceInfo::burdenRatio),
            Codec.DOUBLE.optionalFieldOf("coverage_area_cm2", 0.0).forGetter(ArmourPieceInfo::coverageAreaCm2),
            Codec.DOUBLE.optionalFieldOf("weight_kg", -1.0).forGetter(ArmourPieceInfo::weightKg))
            .apply(instance, ArmourPieceInfo::new));

    public static final Codec<Map<String, ArmourPieceInfo>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC);

    public ArmourPieceInfo merge(ArmourPieceInfo overrides) {
        return new ArmourPieceInfo(
                overrides.elementResistRatio(),
                overrides.attackResistRatio(),
                overrides.armourRatio(),
                overrides.armourToughnessRatio(),
                overrides.burdenRatio(),
                overrides.coverageAreaCm2() != 0.0 ? overrides.coverageAreaCm2() : coverageAreaCm2,
                overrides.weightKg() >= 0.0 ? overrides.weightKg() : weightKg);
    }

    /** Resolves a manually authored mass or estimates it from covered volume and material density. */
    public double estimateWeightKg(MaterialInfo material, ArmourConstructionInfo construction) {
        if (Double.isFinite(weightKg) && weightKg >= 0.0) return weightKg;
        return ArmourMassUtil.estimateKg(material.density(), construction.thicknessMm(),
                construction.layerCount(), construction.paddingMm(), coverageAreaCm2);
    }
}
