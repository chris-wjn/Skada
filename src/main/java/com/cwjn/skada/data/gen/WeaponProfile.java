package com.cwjn.skada.data.gen;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.AttackType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

import static com.cwjn.skada.data.SkadaData.BLADE_WEIGHT_DEFAULT;

public record WeaponProfile(
                            boolean singleEdged,
                            double handleLength,
                            double bladeLength,
                            double bladeSpineCrossguardThickness,
                            double bladeSpineTipShoulderThickness,
                            double bladeCrossguardWidth,
                            double bladeTipShoulderWidth,
                            double pointOfBalance,
                            EdgeBevel edgeBevel,
                            Bevel primaryBevel,
                            TipSpecifications tipSpecs,
                            Map<AttackType, AttackTypeJsonInfo> attackTypes) {

    private static final Map<AttackType, AttackTypeJsonInfo> DEFAULT_MAP = new HashMap<>(
            Map.of(AttackType.strike(), AttackTypeJsonInfo.getDefault())
    );

    public static final Codec<WeaponProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("singleEdged").forGetter(WeaponProfile::singleEdged),
            Codec.DOUBLE.fieldOf("handleLength").forGetter(WeaponProfile::handleLength),
            Codec.DOUBLE.fieldOf("bladeLength").forGetter(WeaponProfile::bladeLength),
            Codec.DOUBLE.fieldOf("bladeSpineCrossguardThickness").forGetter(WeaponProfile::bladeSpineCrossguardThickness),
            Codec.DOUBLE.fieldOf("bladeSpineTipShoulderThickness").forGetter(WeaponProfile::bladeSpineTipShoulderThickness),
            Codec.DOUBLE.fieldOf("bladeCrossguardWidth").forGetter(WeaponProfile::bladeCrossguardWidth),
            Codec.DOUBLE.fieldOf("bladeTipShoulderWidth").forGetter(WeaponProfile::bladeTipShoulderWidth),
            Codec.DOUBLE.fieldOf("pointOfBalance").forGetter(WeaponProfile::pointOfBalance),
            EdgeBevel.CODEC.fieldOf("edgeBevel").forGetter(WeaponProfile::edgeBevel),
            Bevel.CODEC.fieldOf("primaryBevel").forGetter(WeaponProfile::primaryBevel),
            TipSpecifications.CODEC.fieldOf("tipSpecifications").forGetter(WeaponProfile::tipSpecs),
            Codec.unboundedMap(Codec.STRING, AttackTypeJsonInfo.CODEC).fieldOf("attackTypes").forGetter(WeaponProfile::attackTypeStringMap)
    ).apply(instance, WeaponProfile::fromStringMap));
    public static final Codec<Map<String, WeaponProfile>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC);
    private Map<String, AttackTypeJsonInfo> attackTypeStringMap() {
        Map<String, AttackTypeJsonInfo> retMap = new HashMap<>();
        for (Map.Entry<AttackType, AttackTypeJsonInfo> a : attackTypes.entrySet()) {
            retMap.put(a.getKey().rl().toString(), a.getValue());
        }
        return retMap;
    }
    private static WeaponProfile fromStringMap(boolean singleEdged,
                                               double handleLength,
                                               double bladeLength,
                                               double bladeSpineCrossguardThickness,
                                               double bladeSpineTipShoulderThickness,
                                               double bladeCrossguardWidth,
                                               double bladeTipShoulderWidth,
                                               double pointOfBalance,
                                               EdgeBevel edgeBevel,
                                               Bevel primaryBevel,
                                               TipSpecifications tipSpecs,
                                               Map<String, AttackTypeJsonInfo> map) {
        Map<AttackType, AttackTypeJsonInfo> retMap = new HashMap<>();
        for (Map.Entry<String, AttackTypeJsonInfo> a : map.entrySet()) {
            retMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
        }
        return new WeaponProfile(singleEdged, handleLength, bladeLength, bladeSpineCrossguardThickness, bladeSpineTipShoulderThickness, bladeCrossguardWidth, bladeTipShoulderWidth, pointOfBalance, edgeBevel, primaryBevel, tipSpecs, retMap);
    }

    /**
     * Default constructor with reasonable defaults for a one-handed sword.
     * Uses a <a href="https://kvetun-armoury.com/assets/images/products/163/caroling.png">Carolingian sword</a> as a model.
     * Measurements in millimetres.
     */
    public WeaponProfile() {
        this(true, 115, 750, 6, 5, 50, 30, 120,
                new EdgeBevel(32.5, 170, BevelType.CONVEX, 5),
                new Bevel(0.66, BevelType.FLAT),
                new TipSpecifications(7500, 40, 150),  DEFAULT_MAP);
    }

    public enum BevelType {
        CONVEX,
        CONCAVE,
        FLAT
    }

    /**
   * Specifications for the tip of the blade. If the blade doesn't have a distinct tip,
     * like for example if the blade is squared off on the end, the tip radius should be set
     * to 0, and the bevel angles should be set to 180 degrees.
   * @param tipRadius the radius of the tip in micrometers (1000 micrometers = 1 millimetre)
   * @param tipBevelAngle the angle of the bevel at the tip in degrees
   * @param tipBevelShoulderAngle the angle of the shoulder where the tip bevel meets the rest of the blade in degrees
   */
    public record TipSpecifications(
            double tipRadius,
            double tipBevelAngle,
            double tipBevelShoulderAngle
    ) {
        public static final Codec<TipSpecifications> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.DOUBLE.fieldOf("tipRadius").forGetter(TipSpecifications::tipRadius),
                        Codec.DOUBLE.fieldOf("tipBevelAngle").forGetter(TipSpecifications::tipBevelAngle),
                        Codec.DOUBLE.fieldOf("tipBevelShoulderAngle").forGetter(TipSpecifications::tipBevelShoulderAngle)
                ).apply(instance, TipSpecifications::new)
        );
    }

  /**
   * Bevel specifications for the primary bevel of the blade.
   * The length of the bevel is measured as a percentage of the blade width.
   * @param percentageOfBladeWidth
   */
  public record Bevel(
         double percentageOfBladeWidth,
         BevelType bevelType
    ) {

      public static final Codec<Bevel> CODEC = RecordCodecBuilder.create(
          instance -> instance.group(
              Codec.DOUBLE.fieldOf("percentageOfBladeWidth").forGetter(Bevel::percentageOfBladeWidth),
              Codec.STRING.fieldOf("bevelType").forGetter(bevel -> bevel.bevelType().name())
          ).apply(instance, (bevelPercentage, type) ->
              new Bevel(bevelPercentage, getBevelType(type)))
      );

      static BevelType getBevelType(String bevel) {
          return BevelType.valueOf(bevel);
      }

    }

  /**
   * Bevel specifications for the edge bevel of the blade.
   * The edge radius is measured in micrometers because it will be very small (1000 micrometers = 1 millimetre).
   * If the shoulder angle is 180 degrees, there is no shoulder and the primary bevel will be ignored.
   *
   * @param angle angle of the bevel in degrees
   * @param shoulderAngle angle of the shoulder where the bevel meets the primary bevel in degrees
   * @param bevelType type of bevel (convex, concave, flat)
   * @param edgeRadius radius of the edge in micrometers (1000 micrometers = 1 millimetre)
   */
    public record EdgeBevel(
            double angle,
            double shoulderAngle,
            BevelType bevelType,
            double edgeRadius
    ) {

      public static final Codec<EdgeBevel> CODEC = RecordCodecBuilder.create(
              instance -> instance.group(
                      Codec.DOUBLE.fieldOf("angle").forGetter(EdgeBevel::angle),
                      Codec.DOUBLE.fieldOf("shoulderAngle").forGetter(EdgeBevel::shoulderAngle),
                      Codec.STRING.fieldOf("bevelType").forGetter(bevel -> bevel.bevelType().name()),
                      Codec.DOUBLE.fieldOf("edgeRadius").forGetter(EdgeBevel::edgeRadius)
              ).apply(instance, (bevelAngle, shoulderAngle, type, edgeRadius) ->
                      new EdgeBevel(bevelAngle, shoulderAngle, getBevelType(type), edgeRadius))
      );

      static BevelType getBevelType(String bevel) {
          return BevelType.valueOf(bevel);
      }

    }

  /**
   * Estimate blade volume using data from WeaponProfile.
   * - Approximate cross-sectional area as width * thickness and assume linear taper along length.
   * - Use trapezoidal rule: volume = length * (A_base + A_tip) / 2.
   * For the default blade profile, this returns 168,750 cubic millimetres (168.75 cm³). Multiplied
   * by the density of iron, which is 7.785 g/cm³, gives a blade weight of ~1.31 kg or 1300 grams.
   * @return estimated blade volume in cubic millimetres.
   */
  public double estimateBladeVolume() {
    double bladeLength = this.bladeLength(); // units from profile (e.g. mm)
    double baseWidth = this.bladeCrossguardWidth();
    double tipWidth = this.bladeTipShoulderWidth();

    // Clamp small/zero values defensively
    bladeLength = Math.max(1e-6, bladeLength);
    baseWidth = Math.max(1e-6, baseWidth);
    tipWidth = Math.max(1e-6, tipWidth);
    double baseThickness = Math.max(1e-6, this.bladeSpineCrossguardThickness());
    double tipThickness = Math.max(1e-6, this.bladeSpineTipShoulderThickness());

    double areaBase = baseWidth * baseThickness;
    double areaTip = tipWidth * tipThickness;

    // Linear taper approximation
    return bladeLength * (areaBase + areaTip) / 2.0;
  }

  /**
   * Calculates the absolute length of the primary bevel in millimetres.
   * @return the absolute length of the primary bevel in millimetres.
   */
  public double absoluteBevelLength() {
    double averageBladeWidth = (this.bladeCrossguardWidth() + this.bladeTipShoulderWidth()) * 0.5;
    if (!this.singleEdged()) averageBladeWidth *= 0.5; //if the blade is double-edged, half the width is used on each edge
    return averageBladeWidth * this.primaryBevel().percentageOfBladeWidth();
  }

  /**
   * Normalize the blade weight to a standard value for use in calculations.
   * We don't want to just divide a default value by the weight here, because
   * that would make very light or very heavy blades have extreme values.
   * Instead, we use a logarithmic scale to keep values within a reasonable range, with
   * an average value of 1.0 for a blade weight of 1300 grams (the default).
   * @return a double representing the normalized blade weight.
   */
  public double normalizeBladeWeight(ExtraTierInfo material) { //in grams, assuming iron density of 7.85 g/cm³
    double weight = this.estimateBladeVolume() * material.density(); //in grams
    return Math.atan((weight/(BLADE_WEIGHT_DEFAULT+200)) - (BLADE_WEIGHT_DEFAULT/(BLADE_WEIGHT_DEFAULT+200)))/2 + 1;
  }

  public double primaryBevelAngle() {
    return -180 + this.edgeBevel().angle() + this.edgeBevel().shoulderAngle();
  }

}
