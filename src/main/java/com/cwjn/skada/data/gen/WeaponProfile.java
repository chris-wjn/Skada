package com.cwjn.skada.data.gen;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.AttackType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

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
   * @param type type of bevel (convex, concave, flat)
   * @param edgeRadius radius of the edge in micrometers (1000 micrometers = 1 millimetre)
   */
    public record EdgeBevel(
            double angle,
            double shoulderAngle,
            BevelType type,
            double edgeRadius
    ) {

      public static final Codec<EdgeBevel> CODEC = RecordCodecBuilder.create(
              instance -> instance.group(
                      Codec.DOUBLE.fieldOf("angle").forGetter(EdgeBevel::angle),
                      Codec.DOUBLE.fieldOf("shoulderAngle").forGetter(EdgeBevel::shoulderAngle),
                      Codec.STRING.fieldOf("bevelType").forGetter(bevel -> bevel.type().name()),
                      Codec.DOUBLE.fieldOf("edgeRadius").forGetter(EdgeBevel::edgeRadius)
              ).apply(instance, (bevelAngle, shoulderAngle, type, edgeRadius) ->
                      new EdgeBevel(bevelAngle, shoulderAngle, getBevelType(type), edgeRadius))
      );

      static BevelType getBevelType(String bevel) {
          return BevelType.valueOf(bevel);
      }

    }

}
