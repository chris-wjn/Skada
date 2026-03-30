package com.cwjn.skada.data.gen.armour;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

public record ArmourConstructionInfo(
    double thicknessMm,
    int layerCount,
    double paddingMm,
    double rigidity,
    double seamQuality,
    double gapExposure,
    double curvature,
    double articulationPenalty,
    double burdenMultiplier) {

  public static final ArmourConstructionInfo DEFAULT = new ArmourConstructionInfo(1.0, 1, 0.0, 0.5, 0.5, 0.5,
      0.5, 0.0, 1.0);

  @SuppressWarnings("null")
  public static final Codec<ArmourConstructionInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.DOUBLE.fieldOf("thickness_mm").forGetter(ArmourConstructionInfo::thicknessMm),
      Codec.INT.fieldOf("layer_count").forGetter(ArmourConstructionInfo::layerCount),
      Codec.DOUBLE.fieldOf("padding_mm").forGetter(ArmourConstructionInfo::paddingMm),
      Codec.DOUBLE.fieldOf("rigidity").forGetter(ArmourConstructionInfo::rigidity),
      Codec.DOUBLE.fieldOf("seam_quality").forGetter(ArmourConstructionInfo::seamQuality),
      Codec.DOUBLE.fieldOf("gap_exposure").forGetter(ArmourConstructionInfo::gapExposure),
      Codec.DOUBLE.fieldOf("curvature").forGetter(ArmourConstructionInfo::curvature),
      Codec.DOUBLE.fieldOf("articulation_penalty").forGetter(ArmourConstructionInfo::articulationPenalty),
      Codec.DOUBLE.optionalFieldOf("burden_multiplier", 1.0).forGetter(ArmourConstructionInfo::burdenMultiplier))
      .apply(instance, ArmourConstructionInfo::validated));

  private static ArmourConstructionInfo validated(double thicknessMm, int layerCount, double paddingMm,
      double rigidity, double seamQuality, double gapExposure, double curvature, double articulationPenalty,
      double burdenMultiplier) {
    return new ArmourConstructionInfo(
        Math.max(0.1, thicknessMm),
        Math.max(1, layerCount),
        Math.max(0.0, paddingMm),
        clampUnit(rigidity),
        clampUnit(seamQuality),
        clampUnit(gapExposure),
        clampUnit(curvature),
        clampUnit(articulationPenalty),
        Math.max(0.1, burdenMultiplier));
  }

  private static double clampUnit(double value) {
    return Mth.clamp(value, 0.0, 1.0);
  }
}