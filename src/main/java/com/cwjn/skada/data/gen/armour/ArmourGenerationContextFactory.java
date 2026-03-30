package com.cwjn.skada.data.gen.armour;

import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import net.minecraft.util.Mth;

public final class ArmourGenerationContextFactory {

  private ArmourGenerationContextFactory() {
  }

  public static ArmourGenerationContext create(MaterialInfo material, ArmourConstructionInfo construction,
      ArmourPieceInfo piece) {
    double normalizedDensity = MaterialInfo.normalizeMaterial(material.density());
    double normalizedHardness = MaterialInfo.normalizeMaterial(material.hardness());
    double normalizedToughness = MaterialInfo.normalizeMaterial(material.toughness());
    double normalizedFlexibility = MaterialInfo.normalizeMaterial(material.flexibility());

    double effectiveThickness = construction.thicknessMm() * Math.sqrt(construction.layerCount());
    double paddingStrength = clampUnit((construction.paddingMm() / 12.0) * (0.45 + (0.55 * normalizedFlexibility)));
    double continuityQuality = clampUnit(
        (construction.seamQuality() * 0.55) + ((1.0 - construction.gapExposure()) * 0.45));
    double seamWeakness = clampUnit(1.0 - construction.seamQuality());
    double deflectionQuality = clampUnit((construction.curvature() * 0.6) + (construction.rigidity() * 0.4));
    double burdenFactor = Math.max(0.05,
        (effectiveThickness / 10.0) * (0.55 + normalizedDensity) * (0.7 + construction.burdenMultiplier())
            * (1.0 + (construction.articulationPenalty() * 0.75)));

    return new ArmourGenerationContext(
        material,
        construction,
        piece,
        normalizedDensity,
        normalizedHardness,
        normalizedToughness,
        normalizedFlexibility,
        new ArmourConstructionResponseSnapshot(
            effectiveThickness,
            paddingStrength,
            continuityQuality,
            construction.rigidity(),
            seamWeakness,
            construction.gapExposure(),
            deflectionQuality,
            burdenFactor));
  }

  private static double clampUnit(double value) {
    return Mth.clamp(value, 0.0, 1.0);
  }
}