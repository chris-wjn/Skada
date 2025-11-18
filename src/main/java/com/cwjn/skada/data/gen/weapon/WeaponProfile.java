package com.cwjn.skada.data.gen.weapon;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.gen.attack.AttackTypeJsonInfo;
import com.cwjn.skada.data.gen.weapon.parts.*;
import com.cwjn.skada.data.registry.AttackType;

import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * WeaponProfile describes the geometry of a melee weapon.
 * It consists of a Handle and one or more WeaponHeads positioned along the handle.
 */
public class WeaponProfile {

    private static final double BLADE_WEIGHT_DEFAULT = 1300.0; // Default blade weight in grams

    /**
     * Enum to describe how a weapon head is oriented relative to the handle axis.
     */
    public enum HeadOrientation {
        /** Head extends along the handle axis (e.g., blade, spear point) */
        PARALLEL,
        /** Head extends perpendicular to the handle axis (e.g., axe, mace, pick) */
        PERPENDICULAR
    }

    /**
     * Entry class to store a weapon head with its position and orientation on the handle.
     */
    public static class WeaponHeadEntry {
      private final WeaponHead head;
      private final @Nullable ExtraTierInfo material;
      private final double positionOnHandle; // Distance from base of handle in mm
      private final HeadOrientation orientation;

      public WeaponHeadEntry(WeaponHead head, double positionOnHandle, HeadOrientation orientation, @Nullable ExtraTierInfo material) {
        this.head = head;
        this.positionOnHandle = positionOnHandle;
        this.orientation = orientation;
        this.material = material;
      }

      public WeaponHead getHead() {
        return head;
      }

      public double getPositionOnHandle() {
        return positionOnHandle;
      }

      public HeadOrientation getOrientation() {
        return orientation;
      }

      public Optional<ExtraTierInfo> getMaterial() {
        return Optional.ofNullable(material);
      }

      public static final Codec<WeaponHeadEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
              WeaponHead.DISPATCH_CODEC.fieldOf("head").forGetter(WeaponHeadEntry::getHead),
              Codec.DOUBLE.fieldOf("position_on_handle").forGetter(WeaponHeadEntry::getPositionOnHandle),
              Codec.STRING.fieldOf("orientation").forGetter((WeaponHeadEntry e) -> e.getOrientation().name()),
              ExtraTierInfo.CODEC.optionalFieldOf("material").forGetter(WeaponHeadEntry::getMaterial)
      ).apply(instance, (head, pos, oriStr, materialOpt) ->
              new WeaponHeadEntry(head, pos, HeadOrientation.valueOf(oriStr), materialOpt.orElse(null))
      ));
    }

    private final Handle handle;
    private final List<WeaponHeadEntry> weaponHeads;
    private final Map<AttackType, AttackTypeJsonInfo> attackTypes;

  public List<WeaponHeadEntry> getWeaponHeads() {
    return weaponHeads;
  }

  /**
     * Constructor for WeaponProfile.
     * @param handle The handle of the weapon
     * @param weaponHeads List of weapon heads with their positions and orientations on the handle
     * @param attackTypes Map of attack types for this weapon
     */
    public WeaponProfile(Handle handle,
                        List<WeaponHeadEntry> weaponHeads,
                        Map<AttackType, AttackTypeJsonInfo> attackTypes) {
        this.handle = handle;
        this.weaponHeads = weaponHeads != null ? weaponHeads : new ArrayList<>();
        this.attackTypes = attackTypes != null ? attackTypes : new HashMap<>();
    }

    /**
     * Default constructor with reasonable defaults for a one-handed sword.
     * Uses a <a href="https://kvetun-armoury.com/assets/images/products/163/caroling.png">Carolingian sword</a> as a model.
     * Measurements in millimetres.
     */
    public WeaponProfile() {
      this.handle = new Handle(115, 15, null); // 115mm length, 15mm radius
      Blade blade = new Blade(
              false, 50, 30, null,
              8, 6, null, 750,
              new Blade.Bevel(60, 1.5),
              new Blade.EdgeBevel(22.5, 180, 5),
              new Blade.TipSpecifications(1000, 40, 150),
              new Blade.Fuller(true, 40, 4)
      );
      this.weaponHeads = new ArrayList<>();
      weaponHeads.add(new WeaponHeadEntry(blade, 115, HeadOrientation.PARALLEL, null)); // Blade starts at 115mm from base of handle
      this.attackTypes = new HashMap<>(
              Map.of(AttackType.slash(), new AttackTypeJsonInfo(
                      0.2, 3.0, 1.0, 1.0, 1.0, 1.0, List.of()
              ))
      );
    }

    /**
     * Calculate the total volume of the weapon, using
     * the volumes of the handle and weapon heads.
     * @return total volume in mm³
     */
    public double getVolume() {
        return handle.getVolume() + weaponHeads.stream()
                .mapToDouble(entry -> entry.getHead().getVolume())
                .sum();
    }

  /**
   * Get the point of balance (center of mass) from the base of the handle.
   * Assumes uniform density for all components.
   * For perpendicular heads (axe, mace, pick), the head's internal PoB doesn't shift
   * the balance along the handle axis - only its mounting position matters.
   * @return point of balance in millimeters from the base of the handle
   */
  public double getPointOfBalance() {
    double totalMoment = 0.0;
    double totalVolume = 0.0;

    // Handle contribution (assuming uniform density)
    double handleVolume = handle.getVolume();
    double handlePoB = handle.getPointOfBalance();
    totalMoment += handleVolume * handlePoB;
    totalVolume += handleVolume;

    // Weapon heads contribution
    for (WeaponHeadEntry entry : weaponHeads) {
      double headVolume = entry.getHead().getVolume();
      double headPoBAlongHandle;

      if (entry.getOrientation() == HeadOrientation.PERPENDICULAR) {
        // For perpendicular heads, the center of mass is at the mounting position
        // The head's internal PoB is perpendicular to the handle axis, so it doesn't
        // affect the balance along the handle
        headPoBAlongHandle = entry.getPositionOnHandle();
      } else {
        // For parallel heads, add the mounting position + the head's internal PoB
        headPoBAlongHandle = entry.getPositionOnHandle() + entry.getHead().getPointOfBalance();
      }

      totalMoment += headVolume * headPoBAlongHandle;
      totalVolume += headVolume;
    }

    if (totalVolume < 1e-6) {
      return handle.getLength() / 2.0;
    }

    return totalMoment / totalVolume;
  }

    public Handle getHandle() {
    return handle;
  }

  /**
   * Estimate the total volume of all weapon heads (blades).
   * @return total blade volume in mm³
   */
  private double estimateBladeVolume() {
    return weaponHeads.stream()
            .mapToDouble(entry -> entry.getHead().getVolume())
            .sum();
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

  public WeaponInfo generate(ExtraTierInfo material) {
    // We'll check the best stats for each weapon head and use the best one
        return WeaponInfo.generate(material, this, false);
  }

  public Map<String, AttackTypeJsonInfo> attackTypeStringMap() {
    Map<String, AttackTypeJsonInfo> retMap = new TreeMap<>();
    for (Map.Entry<AttackType, AttackTypeJsonInfo> a : attackTypes.entrySet()) {
      retMap.put(a.getKey().rl().toString(), a.getValue());
    }
    return retMap;
  }

  private static Map<AttackType, AttackTypeJsonInfo> fromStringMap(Map<String, AttackTypeJsonInfo> map) {
    Map<AttackType, AttackTypeJsonInfo> retMap = new TreeMap<>();
    for (Map.Entry<String, AttackTypeJsonInfo> a : map.entrySet()) {
      retMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
    }
    return retMap;
  }

  public static final Codec<WeaponProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Handle.CODEC.fieldOf("handle").forGetter(WeaponProfile::getHandle),
          Codec.list(WeaponHeadEntry.CODEC).fieldOf("weapon_heads").forGetter(wp -> wp.weaponHeads),
          Codec.unboundedMap(Codec.STRING, AttackTypeJsonInfo.CODEC).fieldOf("attack_types").forGetter(WeaponProfile::attackTypeStringMap)
  ).apply(instance, (handle, heads, attackMap) -> new WeaponProfile(handle, heads, fromStringMap(attackMap))));

}
