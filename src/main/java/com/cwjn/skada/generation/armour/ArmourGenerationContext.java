package com.cwjn.skada.generation.armour;

import com.cwjn.skada.generation.weapon.MaterialInfo;

public record ArmourGenerationContext(
    MaterialInfo material,
    ArmourConstructionInfo construction,
    ArmourPieceInfo piece,
    double normalizedDensity,
    double normalizedHardness,
    double normalizedToughness,
    double normalizedFlexibility,
    ArmourConstructionResponseSnapshot constructionResponse) {
}