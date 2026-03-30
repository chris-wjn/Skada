package com.cwjn.skada.data.gen.armour;

import com.cwjn.skada.data.gen.weapon.MaterialInfo;

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