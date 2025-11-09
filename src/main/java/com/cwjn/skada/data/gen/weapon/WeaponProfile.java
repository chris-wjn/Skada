package com.cwjn.skada.data.gen.weapon;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.attack.AttackTypeJsonInfo;
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
                            Map<AttackType, AttackTypeJsonInfo> attackTypes) {

    private static final Map<AttackType, AttackTypeJsonInfo> DEFAULT_MAP = new HashMap<>(
            Map.of(AttackType.strike(), AttackTypeJsonInfo.getDefault())
    );

    private static final Map<AttackType, AttackTypeJsonInfo> DEFAULT_SWORD_SLASH_MAP = new HashMap<>(
            Map.of(AttackType.slash(), new AttackTypeJsonInfo(
                    0.2,
                    3.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    null
            ))
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
                                               Map<String, AttackTypeJsonInfo> map) {
        Map<AttackType, AttackTypeJsonInfo> retMap = new HashMap<>();
        for (Map.Entry<String, AttackTypeJsonInfo> a : map.entrySet()) {
            retMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
        }
        return new WeaponProfile(singleEdged, handleLength, bladeLength, bladeSpineCrossguardThickness, bladeSpineTipShoulderThickness, bladeCrossguardWidth, bladeTipShoulderWidth, pointOfBalance, retMap);
    }

    /**
     * Default constructor with reasonable defaults for a one-handed sword.
     * Uses a <a href="https://kvetun-armoury.com/assets/images/products/163/caroling.png">Carolingian sword</a> as a model.
     * Measurements in millimetres.
     */
    public WeaponProfile() {
        this(false, 115, 750, 6, 5, 50, 30, 120,
                DEFAULT_SWORD_SLASH_MAP
                );
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
   * Calculates the absolute length of the primary bevel in millimetres at a given distance
   * from the start of the blade.
   * @param pointOnBlade distance from the start of the blade in millimetres
   * @return the absolute length of the primary bevel in millimetres.
   */
//  public double absoluteBevelLength(double pointOnBlade) {
//    double bladeLen = Math.max(1e-6, this.bladeLength());
//    double t = Math.max(0.0, Math.min(1.0, pointOnBlade / bladeLen)); // normalized position [0,1]
//    double baseWidth = this.bladeCrossguardWidth();
//    double tipWidth = this.bladeTipShoulderWidth();
//    double widthAtPoint = baseWidth + (tipWidth - baseWidth) * t; // linear interpolation along blade
//    if (!this.singleEdged()) widthAtPoint *= 0.5; // if double-edged, use half the width per edge
//    return widthAtPoint * this.primaryBevel().percentageOfBladeWidth();
//  }

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

//  public double primaryBevelAngle() {
//    return -180 + this.edgeBevel().angle() + this.edgeBevel().shoulderAngle();
//  }

}
