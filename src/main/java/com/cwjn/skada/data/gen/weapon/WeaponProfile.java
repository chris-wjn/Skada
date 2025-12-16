package com.cwjn.skada.data.gen.weapon;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.gen.attack.AttackTypeJsonInfo;
import com.cwjn.skada.data.gen.attack.ElementSpread;
import com.cwjn.skada.data.gen.weapon.parts.*;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.SlashCapable;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.ThrustCapable;
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

  private Optional<WeaponHeadEntry> slashHead = Optional.empty();
  private Optional<WeaponHeadEntry> thrustHead = Optional.empty();
  private Optional<WeaponHeadEntry> strikeHead = Optional.empty();
  private static final double BLADE_WEIGHT_DEFAULT = 1300.0; // Default blade weight in grams

  /**
   * Enum to describe how a weapon head is oriented relative to the handle axis.
   */
  public enum HeadOrientation {
    /**
     * Head extends along the handle axis (e.g., blade, spear point)
     */
    PARALLEL,
    /**
     * Head extends perpendicular to the handle axis (e.g., axe, mace, pick)
     */
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
            Codec.STRING.fieldOf("orientation").forGetter((WeaponHeadEntry e) -> e.getOrientation().name())
    ).apply(instance, (head, pos, oriStr) -> new WeaponHeadEntry(head, pos, HeadOrientation.valueOf(oriStr), null)));
  }

  private final Handle handle;
  private final List<WeaponHeadEntry> weaponHeads;
  private final Map<AttackType, AttackTypeJsonInfo> attackTypes;

  public List<WeaponHeadEntry> getWeaponHeads() {
    return weaponHeads;
  }

  /**
   * Constructor for WeaponProfile.
   *
   * @param handle      The handle of the weapon
   * @param weaponHeads List of weapon heads with their positions and orientations on the handle
   * @param attackTypes Map of attack types for this weapon
   */
  public WeaponProfile(Handle handle,
                       List<WeaponHeadEntry> weaponHeads,
                       Map<AttackType, AttackTypeJsonInfo> attackTypes) {
    this.handle = handle;
    this.weaponHeads = weaponHeads != null ? weaponHeads : new ArrayList<>();
    this.attackTypes = attackTypes != null ? attackTypes : new HashMap<>();
    /*
      We'll take note of the "main" weapon head for each attack type for quick access later.
      My best idea right now is to just take the one with the largest volume for each type.
      TODO: maybe improve this selection logic later?
     */
    for (WeaponHeadEntry entry : this.weaponHeads) {
      if (entry.head instanceof SlashCapable) {
        if (slashHead.isEmpty()) slashHead = Optional.of(entry);
        else {
          if (slashHead.get().head.getVolume() < entry.head.getVolume()) {
            slashHead = Optional.of(entry);
          }
        }
      }
      if (entry.head instanceof ThrustCapable) {
        if (thrustHead.isEmpty()) thrustHead = Optional.of(entry);
        else {
          if (thrustHead.get().head.getVolume() < entry.head.getVolume()) {
            thrustHead = Optional.of(entry);
          }
        }
      }
      if (entry.head instanceof StrikeCapable) {
        if (strikeHead.isEmpty()) strikeHead = Optional.of(entry);
        else {
          if (strikeHead.get().head.getVolume() < entry.head.getVolume()) {
            strikeHead = Optional.of(entry);
          }
        }
      }
    }
  }

  /**
   * Default constructor with reasonable defaults for a one-handed sword.
   * Uses a <a href="https://kvetun-armoury.com/assets/images/products/163/caroling.png">Carolingian sword</a> as a model.
   * Measurements in millimetres.
   */
  public static WeaponProfile defaultSword() {
    Handle handle = new Handle(115, 15, null); // 115mm length, 15mm radius
    Blade blade = new Blade(
            false, 50, 30, null,
            8, 6, null, 750,
            new Blade.Bevel(60, 1.5),
            new Blade.EdgeBevel(22.5, 180, 5),
            new Blade.TipSpecifications(1000, 40, 150, 0.5),
            new Blade.Fuller(true, 0.4, 0.1)
    );
    List<WeaponHeadEntry> weaponHeads = new ArrayList<>();
    weaponHeads.add(new WeaponHeadEntry(blade, 115, HeadOrientation.PARALLEL, null)); // Blade starts at 115mm from base of handle
    return new WeaponProfile(handle, weaponHeads, new HashMap<>(
            Map.of(AttackType.slash(), new AttackTypeJsonInfo(
                    0.2, 3.0, 1.0, 1.0, 1.0, 1.0, List.of()
            ))
    ));
  }

  public static WeaponProfile axeTest() {
    Handle handle = new Handle(500.0, 15.0, null);

    AxeHead axe = new AxeHead(
            300.0, // eyeLength
            100.0, // eyeHeight
            300.0, // cheekLength
            100.0, // cheekHeight
            50.0,  // beardHeight
            150.0, // beardTipDistance
            50.0,  // toeHeight
            150.0, // toeTipDistance
            15.0,  // eyeThickness
            15.0,  // eyeHoleSemiMajorAxis
            15.0,  // eyeHoleSemiMinorAxis
            new Blade.Bevel(0.33, 1.3),
            new Blade.EdgeBevel(22.5, 180.0, 5.0)
    );

    List<WeaponHeadEntry> heads = new ArrayList<>();
    heads.add(new WeaponHeadEntry(axe, 450.0, HeadOrientation.PERPENDICULAR, new ExtraTierInfo(1.0, 1.0, 1.0, 1.0, new ElementSpread())));

    Map<AttackType, AttackTypeJsonInfo> attacks = new HashMap<>();
    attacks.put(AttackType.slash(), new AttackTypeJsonInfo(0.25, 2.0, 1.0, 1.0, 1.0, 1.0, List.of()));

    return new WeaponProfile(handle, heads, attacks);
  }

  public boolean canSlash() {
    return slashHead.isPresent();
  }

  public boolean canThrust() {
    return thrustHead.isPresent();
  }

  public boolean canStrike() {
    return strikeHead.isPresent();
  }

  public WeaponHeadEntry getSlashHead() {
    return this.slashHead.orElseThrow(() ->
            new IllegalStateException("Tried to get slash head of a weapon that cannot slash!")
    );
  }

  public WeaponHeadEntry getThrustHead() {
    return thrustHead.orElseThrow(() ->
            new IllegalStateException("Tried to get thrust head of a weapon that cannot thrust!")
    );
  }

  public WeaponHeadEntry getStrikeHead() {
    return strikeHead.orElseThrow(() ->
            new IllegalStateException("Tried to get strike head of a weapon that cannot strike!")
    );
  }

  /**
   * Calculate the total volume of the weapon, using
   * the volumes of the handle and weapon heads.
   *
   * @return total volume in mm³
   */
  public double getVolume() {
    return handle.getVolume() + weaponHeads.stream()
            .mapToDouble(entry -> entry.getHead().getVolume())
            .sum();
  }

  /**
   * Get the total weight of the weapon assuming a given material density for the
   * weapon heads. Assume the handle is made of oak wood with density 0.7 g/cm³.
   * @param material the material info for the weapon heads
   */
  public double getWeight(ExtraTierInfo material) {
    double handleWeight = handle.getWeight(); // Oak wood density in g/cm³
    double headsWeight = weaponHeads.stream()
            .mapToDouble(entry -> entry.getHead().getVolume() * material.density() / 1000.0)
            .sum();
    return handleWeight + headsWeight; // Total weight in grams
  }

  /**
   * Calculate the point of balance of the weapon in millimeters from the base of the handle
   * by taking a weighted average of each component's point of balance, using their masses as weights.
   *
   * @param material the fallback material for heads without an explicit material
   * @return point of balance in millimeters from the base of the handle
   */
  public double getPointOfBalance(ExtraTierInfo material) {
    double handleWeight = handle.getWeight(); // Oak wood density in g/cm³
    double handlePoB = handle.getPointOfBalance();

    double totalWeightedPoB = handlePoB * handleWeight;
    double totalWeight = handleWeight;

    for (WeaponHeadEntry entry : weaponHeads) {
      ExtraTierInfo headMaterial = entry.getMaterial().orElse(material);
      double headWeight = entry.getHead().getVolume() * headMaterial.density() / 1000.0; // Convert from g/cm³ to g/mm³
      double headPoB;
      if (entry.getOrientation() == HeadOrientation.PERPENDICULAR) {
        // if the head is perpendicular, we don't use its PoB, just the centre of the head width
        headPoB = entry.getPositionOnHandle() + (entry.getHead().getSecondaryAxisLength() / 2.0);
      }
      else {
        // if the head is parallel, we include its PoB
        headPoB = entry.getPositionOnHandle() + entry.getHead().getPointOfBalance();
      }
      totalWeightedPoB += headPoB * headWeight;
      totalWeight += headWeight;
    }

    return totalWeightedPoB / totalWeight;
  }

  /**
   * Get the ideal point of balance along the total weapon length when accounting for a given weapon head
   * and attack type.
   * @param headEntry the weapon head
   * @param attackType the attack type to calculate ideal PoB for
   * @return the ideal point of balance in mm from the base of the handle
   */
  public double getIdealPointOfBalanceWithHead(WeaponHeadEntry headEntry, AttackType attackType) {
    double bladeStart = headEntry.getPositionOnHandle();
    if (headEntry.getOrientation() == HeadOrientation.PERPENDICULAR) {
      return bladeStart;
    }
    else {
      double headIdealPoB = 0.0;
      // Determine the normalized ideal PoB based on attack type and head capabilities
      WeaponHead head = headEntry.getHead();
      if (attackType.equals(AttackType.slash()) && head instanceof SlashCapable slashHead) {
        headIdealPoB = slashHead.getSlashNormalizedIdealPointOfBalance();
      } else if (attackType.equals(AttackType.thrust()) && head instanceof ThrustCapable thrustHead) {
        headIdealPoB = thrustHead.getThrustNormalizedIdealPointOfBalance();
      } else if (attackType.equals(AttackType.strike()) && head instanceof StrikeCapable strikeHead) {
        headIdealPoB = strikeHead.getStrikeNormalizedIdealPointOfBalance();
      }
      return bladeStart + headEntry.getHead().getPrimaryAxisLength() * headIdealPoB;
    }
  }

  /**
   * Get the mass moment of inertia of the weapon about the handle base.
   * @param material the material info for the weapon heads
   * @return mass moment of inertia in kg·m²
   */
  public double getMomentOfInertia(ExtraTierInfo material) {
    double gramMillimeterSquared = handle.getMomentOfInertia() + weaponHeads.stream()
            .mapToDouble(entry -> {
              ExtraTierInfo headMaterial = entry.getMaterial().orElse(material);
              double distanceFromPivot = entry.getPositionOnHandle();
              return entry.getHead().getMomentOfInertia(distanceFromPivot, headMaterial.density(), entry.getOrientation());
            })
            .sum();
    // Convert from g·mm² to kg·m²: divide by 1,000,000,000 (1 billion)
    // 1 kg = 1000 g, 1 m² = 1,000,000 mm², so 1 kg·m² = 10^9 g·mm²
    return gramMillimeterSquared / 1_000_000_000.0;
  }

  /**
   * Get the distance that the head(s) extend beyond the handle.
   * Most likely caused by just 1 head, but technically could be multiple.
   * @return length of head beyond handle in mm
   */
  public double getHeadExtension() {
    double maxHeadEnd = 0.0;
    for (WeaponHeadEntry entry : weaponHeads) {
      double headEnd = entry.getPositionOnHandle();
      if (entry.getOrientation() == HeadOrientation.PARALLEL) {
        headEnd += entry.getHead().getPrimaryAxisLength();
      }
      else {
        headEnd += entry.getHead().getSecondaryAxisLength();
      }
      if (headEnd > maxHeadEnd) {
        maxHeadEnd = headEnd;
      }
    }
    double headLength = maxHeadEnd - handle.getLength();
    return Math.max(0.0, headLength);
  }

  public double getTotalLength() {
    double maxLength = handle.getLength();
    for (WeaponHeadEntry entry : weaponHeads) {
      double headEnd = entry.getPositionOnHandle();
      if (entry.getOrientation() == HeadOrientation.PARALLEL) {
        headEnd += entry.getHead().getPrimaryAxisLength();
      }
      else {
        headEnd += entry.getHead().getSecondaryAxisLength();
      }
      if (headEnd > maxLength) {
        maxLength = headEnd;
      }
    }
    return maxLength;
  }

  public Handle getHandle() {
    return handle;
  }

  public Map<AttackType, AttackTypeJsonInfo> getAttackTypes() {
    return attackTypes;
  }

  /**
   * Estimate the total volume of all weapon heads (blades).
   *
   * @return total blade volume in mm³
   */
  private double getBladeVolume() {
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
   *
   * @return a double representing the normalized blade weight.
   */
  public double normalizeBladeWeight(ExtraTierInfo material) { //in grams, assuming iron density of 7.85 g/cm³
    double weight = this.getBladeVolume() * material.density(); //in grams
    return Math.atan((weight / (BLADE_WEIGHT_DEFAULT + 200)) - (BLADE_WEIGHT_DEFAULT / (BLADE_WEIGHT_DEFAULT + 200))) / 2 + 1;
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

  public static final Codec<WeaponProfile> CODEC = RecordCodecBuilder.create(instance -> {
    return instance.group(
            Handle.CODEC.fieldOf("handle").forGetter(WeaponProfile::getHandle),
            Codec.list(WeaponHeadEntry.CODEC).fieldOf("weapon_heads").forGetter(wp -> wp.weaponHeads),
            Codec.unboundedMap(Codec.STRING, AttackTypeJsonInfo.CODEC).fieldOf("attack_types").forGetter(WeaponProfile::attackTypeStringMap)
    ).apply(instance, (handle, heads, attackMap) -> new WeaponProfile(handle, heads, fromStringMap(attackMap)));
  });

}
