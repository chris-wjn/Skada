package com.cwjn.skada.data.gen.weapon.generation_algo.context;

import static com.cwjn.skada.data.SkadaData.MATERIAL_PROPERTY_SOFT_CAP;

import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import net.minecraft.util.Mth;

/**
 * Immutable snapshot of normalized material response used by the generators.
 *
 * <p>This class stores the material traits once, then exposes precomputed
 * factors for the stat formulas that need them.
 */
public final class MaterialResponseSnapshot {

  private static final double SLASH_HARDNESS_SCALE = 0.08;
  private static final double SLASH_FLEXIBILITY_SCALE = 0.08;
  private static final double THRUST_HARDNESS_SCALE = 0.06;
  private static final double THRUST_TOUGHNESS_SCALE = 0.08;
  private static final double THRUST_FLEXIBILITY_SCALE = 0.06;

  private final MaterialInfo material;
  private final double hardnessNorm;
  private final double toughnessNorm;
  private final double flexibilityNorm;
  private final double centeredFlexibility;
  private final double slashMaterialFactor;
  private final double thrustMaterialFactor;

  /**
   * Creates a new normalized material snapshot.
   *
   * @param material source material definition
   * @param hardnessNorm hardness normalized into the shared material range
   * @param toughnessNorm toughness normalized into the shared material range
   * @param flexibilityNorm flexibility normalized into the shared material range
   * @param centeredFlexibility flexibility centered around the neutral midpoint
   * @param slashMaterialFactor precomputed slash material factor
   * @param thrustMaterialFactor precomputed thrust material factor
   */
  private MaterialResponseSnapshot(
      MaterialInfo material,
      double hardnessNorm,
      double toughnessNorm,
      double flexibilityNorm,
      double centeredFlexibility,
      double slashMaterialFactor,
      double thrustMaterialFactor) {
    this.material = material;
    this.hardnessNorm = hardnessNorm;
    this.toughnessNorm = toughnessNorm;
    this.flexibilityNorm = flexibilityNorm;
    this.centeredFlexibility = centeredFlexibility;
    this.slashMaterialFactor = slashMaterialFactor;
    this.thrustMaterialFactor = thrustMaterialFactor;
  }

  /**
   * Normalizes the supplied weapon material into the shared response snapshot.
   *
   * @param material weapon material to normalize
   * @return immutable normalized material snapshot
   */
  public static MaterialResponseSnapshot fromMaterial(MaterialInfo material) {
    double hardnessNorm = normalizeMaterial(material.hardness());
    double toughnessNorm = normalizeMaterial(material.toughness());
    double flexibilityNorm = normalizeMaterial(material.flexibility());
    double centeredFlexibility = Math.abs(flexibilityNorm - 0.5) * 2.0;
    double slashMaterialFactor = 1.0 + SLASH_HARDNESS_SCALE * hardnessNorm - SLASH_FLEXIBILITY_SCALE * centeredFlexibility;
    double thrustMaterialFactor = 1.0 + THRUST_HARDNESS_SCALE * hardnessNorm
      + THRUST_TOUGHNESS_SCALE * toughnessNorm
      - THRUST_FLEXIBILITY_SCALE * centeredFlexibility;

    return new MaterialResponseSnapshot(
      material,
      hardnessNorm,
      toughnessNorm,
      flexibilityNorm,
      centeredFlexibility,
      slashMaterialFactor,
      thrustMaterialFactor);
  }

  private static double normalizeMaterial(double value) {
    return Mth.clamp(value / MATERIAL_PROPERTY_SOFT_CAP, 0.0, 1.0);
  }

  public MaterialInfo material() {
    return material;
  }

  public double hardnessNorm() {
    return hardnessNorm;
  }

  public double toughnessNorm() {
    return toughnessNorm;
  }

  public double flexibilityNorm() {
    return flexibilityNorm;
  }

  public double centeredFlexibility() {
    return centeredFlexibility;
  }

  public double slashMaterialFactor() {
    return slashMaterialFactor;
  }

  public double thrustMaterialFactor() {
    return thrustMaterialFactor;
  }

}