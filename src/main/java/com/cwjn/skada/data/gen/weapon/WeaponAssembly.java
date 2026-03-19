package com.cwjn.skada.data.gen.weapon;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.attack.AttackTypeJsonInfo;
import com.cwjn.skada.data.gen.attack.ElementSpread;
import com.cwjn.skada.data.gen.weapon.attack_capability.AttackCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.parts.HandlePart;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPart;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartTransform;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.cwjn.skada.data.registry.AttackType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * A weapon assembled from multiple parts (blade, handle, guard, pommel, etc.).
 *
 * Units: centimeters for length, grams for mass, g/cm^3 for density.
 */
public final class WeaponAssembly {

  private final List<WeaponPartEntry> parts;
  private final Map<AttackType, AttackTypeJsonInfo> attackTypes;
  public static final int SMALL_SAMPLE_SIZE = 64;
  public static final int LARGE_SAMPLE_SIZE = 1024;
  private static final int DEFAULT_MASS = 1300; // grams. TODO: set this automatically based on all weapon entries?
  private static final MaterialInfo DEFAULT_WOOD_MATERIAL = new MaterialInfo(0.71, 1.5, 4.0, 6.0, new ElementSpread()); // density
  private static final AttackType THRUST_CONTEXT = new AttackType("thrust", null, null, null, ThrustCapable.class);

  /**
   * Creates an immutable weapon assembly from part entries and attack-type
   * metadata.
   *
   * @param parts       the assembled weapon parts
   * @param attackTypes
   *                    attack-type configuration keyed by attack type
   */
  public WeaponAssembly(List<WeaponPartEntry> parts, Map<AttackType, AttackTypeJsonInfo> attackTypes) {
    Objects.requireNonNull(parts, "parts");
    this.parts = Collections.unmodifiableList(new ArrayList<>(parts));
    this.attackTypes = Collections.unmodifiableMap(new TreeMap<>(attackTypes));
  }

  public static final Codec<WeaponAssembly> CODEC = RecordCodecBuilder.create(instance -> {
    return instance.group(
        WeaponPartEntry.CODEC.listOf().fieldOf("parts").forGetter(WeaponAssembly::parts),
        Codec.unboundedMap(Codec.STRING, AttackTypeJsonInfo.CODEC).fieldOf("attack_types")
            .forGetter(WeaponAssembly::attackTypeStringMap))
        .apply(instance, (parts, stringMap) -> new WeaponAssembly(parts, attackTypesFromStringMap(stringMap)));
  });

  /**
   * Returns attack-type metadata keyed by string resource location.
   *
   * @return attack-type map with string keys suitable for codec serialization
   */
  public Map<String, AttackTypeJsonInfo> attackTypeStringMap() {
    Map<String, AttackTypeJsonInfo> retMap = new TreeMap<>();
    for (Map.Entry<AttackType, AttackTypeJsonInfo> a : attackTypes.entrySet()) {
      retMap.put(a.getKey().rl().toString(), a.getValue());
    }
    return retMap;
  }

  /**
   * Converts a string-keyed attack-type map into a attacktype-keyed map.
   *
   * @param map attack-type metadata keyed by resource-location strings
   * @return attack-type metadata keyed by resolved {@link AttackType}
   */
  private static Map<AttackType, AttackTypeJsonInfo> attackTypesFromStringMap(Map<String, AttackTypeJsonInfo> map) {
    Map<AttackType, AttackTypeJsonInfo> retMap = new TreeMap<>();
    for (Map.Entry<String, AttackTypeJsonInfo> a : map.entrySet()) {
      retMap.put(SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(new ResourceLocation(a.getKey())), a.getValue());
    }
    return retMap;
  }

  /**
   * Creates a copy of this weapon assembly with the material of every part
   * replaced by the provided material.
   *
   * @param newMaterial the material to apply to every part
   * @return new assembly with identical geometry and attack metadata
   */
  public WeaponAssembly withMaterial(MaterialInfo newMaterial) {
    List<WeaponPartEntry> newParts = parts.stream()
        .map(entry -> entry.withMaterial(newMaterial))
        .collect(Collectors.toList());
    return new WeaponAssembly(newParts, attackTypes);
  }

  /**
   * Creates a copy of this weapon assembly with the material of all parts except
   * the handle
   * replaced by the provided material. The handle will be converted to wood.
   *
   * @param newMaterial the material to apply to all parts except the handle
   * @return new assembly with identical geometry and attack metadata
   */
  public WeaponAssembly withMaterialWoodenHandle(MaterialInfo newMaterial) {
    List<WeaponPartEntry> newParts = parts.stream()
        .map(entry -> {
          if (entry.part() instanceof HandlePart) {
            return entry.withMaterial(DEFAULT_WOOD_MATERIAL);
          } else {
            return entry.withMaterial(newMaterial);
          }
        })
        .collect(Collectors.toList());
    return new WeaponAssembly(newParts, attackTypes);
  }

  /**
   * Returns the immutable list of part entries in this weapon assembly.
   *
   * @return assembly parts
   */
  public List<WeaponPartEntry> parts() {
    return parts;
  }

  /**
   * Calculates total weapon volume by summing each part volume.
   *
   * @param samples sampling resolution used by part volume estimators
   * @return total volume in cm^3
   */
  public double volume(int samples) {
    return parts.stream()
        .mapToDouble(entry -> entry.part().volumeCm3(samples))
        .sum();
  }

  /**
   * Calculates the total mass of the weapon assembly by summing the mass of all
   * its parts.
   *
   * @param samples the number of samples to use for mass property calculations
   * @return the total mass in grams of all parts in the assembly
   */
  public double mass(int samples) {
    return parts.stream()
        .mapToDouble(entry -> entry.part().massProperties(entry.material().density(), samples).massG())
        .sum();
  }

  /**
   * Calculates the full-assembly center of mass in weapon-space coordinates.
   *
   * @param samples sampling resolution used for part mass properties
   * @return center-of-mass position in a vec3 with units of cm in weapon space
   *         (origin at weapon base/pivot)
   */
  public GeometryUtil.Vec3 centerOfMass(int samples) {
    double totalMass = 0.0;
    GeometryUtil.Vec3 weightedSum = new GeometryUtil.Vec3(0, 0, 0);

    for (WeaponPartEntry entry : parts) {
      MassProperties props = entry.part().massProperties(entry.material().density(), samples);
      Vec3 localCom = props.centerOfMass();
      Vec3 worldCom = entry.transform().apply(localCom).add(entry.position());
      double mass = props.massG();
      totalMass += mass;
      weightedSum = weightedSum.add(worldCom.mul(mass));
    }

    if (totalMass <= 0.0) {
      return new GeometryUtil.Vec3(0, 0, 0);
    }
    return weightedSum.mul(1.0 / totalMass);
  }

  /**
   * Returns the point of balance, i.e. center-of-mass X offset from the weapon
   * base.
   *
   * @param samples sampling resolution used for center-of-mass calculation
   * @return point of balance in cm from pivot/base
   */
  public double pointOfBalance(int samples) {
    return centerOfMass(samples).x();
  }

  /**
   * Computes total moment of inertia about the weapon base for the chosen axis.
   *
   * @param axis    inertia axis in weapon space
   * @param samples sampling resolution used for part inertia calculations
   * @return moment of inertia in g·cm^2
   */
  public double momentOfInertiaAboutBase(WeaponAxis axis, int samples) {
    return parts.stream()
        .mapToDouble(entry -> {
          MassProperties props = entry.part().massProperties(entry.material().density(), samples);
          WeaponAxis localAxis = entry.transform().localAxisForWeaponAxis(axis);
          double localInertia = entry.part().momentOfInertiaAboutCenterOfMass(localAxis, entry.material().density(),
              samples);
          Vec3 worldCom = entry.transform().apply(props.centerOfMass()).add(entry.position());
          return localInertia + props.massG() * distanceSquaredToAxis(worldCom, axis);
        })
        .sum();
  }

  /**
   * Returns the total weapon length along the handle axis (weapon X), in cm.
   */
  public double length() {
    if (parts.isEmpty()) {
      return 0.0;
    }

    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;

    for (WeaponPartEntry entry : parts) {
      WeaponPart part = entry.part();
      GeometryUtil.Bounds bounds = part.localBounds(SMALL_SAMPLE_SIZE);
      WeaponPartTransform transform = entry.transform();
      WeaponAxis localAxis = transform.localAxisForWeaponAxis(WeaponAxis.X);
      int sign = transform.signForWeaponAxis(WeaponAxis.X);

      double localMin = switch (localAxis) {
        case X -> bounds.minX();
        case Y -> bounds.minY();
        case Z -> bounds.minZ();
      };
      double localMax = switch (localAxis) {
        case X -> bounds.maxX();
        case Y -> bounds.maxY();
        case Z -> bounds.maxZ();
      };
      double originX = entry.position().x();

      double partMinX;
      double partMaxX;
      if (sign >= 0) {
        partMinX = originX + localMin;
        partMaxX = originX + localMax;
      } else {
        partMinX = originX - localMax;
        partMaxX = originX - localMin;
      }

      minX = Math.min(minX, partMinX);
      maxX = Math.max(maxX, partMaxX);
    }

    return Math.max(0.0, maxX - minX);
  }

  /**
   * Calculates the ideal point of balance (PoB) in cm for this weapon for a
   * specific attack type
   * 
   * @return the ideal PoB in cm along the length of the weapon
   */
  public double idealPointOfBalanceForAttackType(AttackType attackType) {
    double totalLength = Math.max(1.0e-6, length());
    double mass = mass(LARGE_SAMPLE_SIZE);
    double inertiaAboutPivot = momentOfInertiaAboutBase(WeaponAxis.Z, LARGE_SAMPLE_SIZE);
    double strikePointNorm = normalizedStrikePointForAttackType(attackType, LARGE_SAMPLE_SIZE);

    double denominator = mass * strikePointNorm * totalLength;
    if (denominator > 1.0e-6 && Double.isFinite(denominator) && Double.isFinite(inertiaAboutPivot)) {
      double ideal = inertiaAboutPivot / denominator;
      if (Double.isFinite(ideal)) {
        return Math.max(0.0, Math.min(totalLength, ideal));
      }
    }

    Optional<WeaponPartEntry> attackHead = primaryPartForAttackType(attackType, SMALL_SAMPLE_SIZE);
    if (attackHead.isPresent()) {
      return attackHead.get().position().x();
    }
    return pointOfBalance(SMALL_SAMPLE_SIZE);
  }

  /**
   * Estimates the normalized impact/strike point for a given attack type.
   *
   * @param attackType attack type whose strike location should be estimated
   * @param samples    sampling resolution used by helper geometry routines
   * @return strike-point position normalized to [0, 1] along weapon length
   */
  public double normalizedStrikePointForAttackType(AttackType attackType, int samples) {
    Optional<WeaponPartEntry> maybePrimary = primaryPartForAttackType(attackType, samples);
    if (maybePrimary.isEmpty()) {
      return centreOfPercussion(samples);
    }

    WeaponPartEntry primary = maybePrimary.get();
    WeaponPart part = primary.part();
    double totalLength = Math.max(1.0e-6, length());

    if (isAttackType(attackType, SlashCapable.class) && part instanceof SlashCapable slashCapable) {
      return normalizedXOnPart(primary, slashCapable.normalizedSlashStrikePointOnPart(samples), samples, totalLength);
    }

    if (isAttackType(attackType, StrikeCapable.class) && part instanceof StrikeCapable strikeCapable) {
      return normalizedXOnPart(primary, strikeCapable.normalizedStrikeContactPointOnPart(samples), samples,
          totalLength);
    }

    if (isAttackType(attackType, ThrustCapable.class) && part instanceof ThrustCapable thrustCapable) {
      return normalizedXOnPart(primary, thrustCapable.normalizedThrustContactPointOnPart(samples), samples,
          totalLength);
    }

    return centreOfPercussion(samples);
  }

  /**
   * Normalizes ideal PoB for an attack type into [0, 1] over current weapon
   * length.
   *
   * @param attackType attack type to evaluate
   * @return normalized ideal point of balance
   */
  public double normalizedIdealPointOfBalanceForAttackType(AttackType attackType) {
    double len = Math.max(1.0e-6, length());
    return Math.max(0.0, Math.min(1.0, idealPointOfBalanceForAttackType(attackType) / len));
  }

  /**
   * Normalize the weapon assembly mass to a standard value for use in
   * calculations.
   * We don't want to just divide a default value by the mass here, because
   * that would make very light or very heavy weapons have extreme values.
   * Instead, we use a logarithmic scale to keep values within a reasonable range,
   * with
   * an average value of 1.0 for a weapon assembly mass of 1300 grams (the
   * default).
   *
   * @return a double representing the normalized weapon assembly mass.
   */
  public double normalizedMass(MaterialInfo material) { // in grams, assuming iron density of 7.85 g/cm³
    double mass = this.mass(LARGE_SAMPLE_SIZE); // in grams
    return Math.atan(
        (mass / (DEFAULT_MASS + 200)) - (DEFAULT_MASS / (DEFAULT_MASS + 200))) / 2 + 1;
  }

  /**
   * Returns all attack types supported by at least one part in this assembly.
   */
  public Map<AttackType, AttackTypeJsonInfo> getAttackTypes() {
    return attackTypes;
  }

  /**
   * Returns the largest-volume part that supports the given attack type.
   *
   * @param attackType attack type to query
   * @param samples    sampling resolution used for part-volume comparison
   * @return optional primary part for this attack type
   */
  public Optional<WeaponPartEntry> primaryPartForAttackType(AttackType attackType, int samples) {
    return partsForAttackType(attackType).stream()
        .max((a, b) -> Double.compare(a.part().volumeCm3(samples), b.part().volumeCm3(samples)));
  }

  /**
   * Returns the largest-volume part for the given attack type using large-sample
   * resolution.
   *
   * @param attackType attack type to query
   * @return optional primary part for this attack type
   */
  public Optional<WeaponPartEntry> primaryPartForAttackType(AttackType attackType) {
    return primaryPartForAttackType(attackType, LARGE_SAMPLE_SIZE);
  }

  /**
   * Returns all parts capable of performing the supplied attack type.
   *
   * @param attackType attack type capability filter
   * @return immutable list of matching part entries
   */
  public List<WeaponPartEntry> partsForAttackType(AttackType attackType) {
    Objects.requireNonNull(attackType, "attackType");
    Class<? extends AttackCapable> capableInterface = attackType.capableInterface();
    List<WeaponPartEntry> matchingParts = new ArrayList<>();
    for (WeaponPartEntry entry : parts) {
      if (capableInterface.isInstance(entry.part())) {
        matchingParts.add(entry);
      }
    }
    return List.copyOf(matchingParts);
  }

  /**
   * Returns the primary part's material for an attack type, or a fallback if
   * absent.
   *
   * @param attackType attack type to query
   * @param fallback   fallback material when no supporting part exists
   * @return selected material
   */
  public MaterialInfo primaryPartMaterialOrDefault(AttackType attackType, MaterialInfo fallback) {
    return primaryPartForAttackType(attackType)
        .map(WeaponPartEntry::material)
        .orElse(fallback);
  }

  /**
   * Returns normalized PoB in [0, 1] over current assembly length.
   *
   * @param samples sampling resolution used for PoB calculation
   * @return normalized point of balance
   */
  public double normalizedPointOfBalance(int samples) {
    double len = Math.max(1e-6, length());
    return Math.max(0.0, Math.min(1.0, pointOfBalance(samples) / len));
  }

  /**
   * Returns the normalized center of percussion along the weapon length [0, 1].
   *
   * Computed from rigid-body quantities already available on the assembly:
   * CoP = I_pivot / (m * d_com * L)
   * where I_pivot is about the weapon base around Z (swing axis), d_com is point
   * of balance from the pivot along X, and L is total weapon length.
   */
  public double centreOfPercussion(int samples) {
    double length = Math.max(1.0e-6, length());
    double mass = mass(samples);
    double pointOfBalance = pointOfBalance(samples);
    double inertiaAboutPivot = momentOfInertiaAboutBase(WeaponAxis.Z, samples);

    double denominator = mass * pointOfBalance * length;
    if (denominator <= 1.0e-6 || !Double.isFinite(denominator) || !Double.isFinite(inertiaAboutPivot)) {
      return Math.max(0.0, Math.min(1.0, normalizedPointOfBalance(samples)));
    }

    double normalizedCop = inertiaAboutPivot / denominator;
    return Math.max(0.0, Math.min(1.0, normalizedCop));
  }

  /**
   * Determines whether the primary thrust-capable part performs thrust as
   * rotational motion.
   *
   * @param samples sampling resolution used when selecting the primary thrust
   *                part
   * @return true if thrust motion mode is rotational; otherwise false
   */
  public boolean isThrustRotational(int samples) {
    Optional<WeaponPartEntry> maybePrimary = primaryPartForAttackType(THRUST_CONTEXT, samples);
    if (maybePrimary.isEmpty() || !(maybePrimary.get().part() instanceof ThrustCapable thrustCapable)) {
      return false;
    }
    return thrustCapable.thrustMotionMode() == ThrustCapable.ThrustMotionMode.ROTATIONAL;
  }

  /**
   * Computes the effective-mass ratio for an attack type at its estimated strike
   * point.
   *
   * @param attackType attack type whose impact point is used
   * @param samples    sampling resolution for mass/inertia calculations
   * @return effective-mass ratio in [0, 1]
   */
  public double effectiveMassRatioForAttackType(AttackType attackType, int samples) {
    if (isAttackType(attackType, ThrustCapable.class) && !isThrustRotational(samples)) {
      return 1.0;
    }

    double totalLength = Math.max(1.0e-6, length());
    double impactNorm = normalizedStrikePointForAttackType(attackType, samples);
    double impactX = impactNorm * totalLength;
    return effectiveMassRatioAtPoint(impactX, samples);
  }

  /**
   * Computes effective-mass ratio at a concrete impact X position.
   *
   * Formula:
   * m_eff = 1 / (1/m + (x - PoB)^2 / I_com), ratio = m_eff / m.
   *
   * @param impactPointX impact point in cm from weapon base
   * @param samples      sampling resolution for mass/inertia calculations
   * @return effective-mass ratio in [0, 1]
   */
  public double effectiveMassRatioAtPoint(double impactPointX, int samples) {
    double mass = mass(samples);
    double pointOfBalance = pointOfBalance(samples);
    double inertiaAboutPivot = momentOfInertiaAboutBase(WeaponAxis.Z, samples);

    if (mass <= 1.0e-6 || !Double.isFinite(mass) || !Double.isFinite(pointOfBalance)
        || !Double.isFinite(inertiaAboutPivot)) {
      return 1.0;
    }

    double inertiaAboutCenter = inertiaAboutPivot - mass * pointOfBalance * pointOfBalance;
    if (inertiaAboutCenter <= 1.0e-6 || !Double.isFinite(inertiaAboutCenter)) {
      return 1.0;
    }

    double delta = impactPointX - pointOfBalance;
    double inverseEffectiveMass = (1.0 / mass) + ((delta * delta) / inertiaAboutCenter);
    if (inverseEffectiveMass <= 1.0e-6 || !Double.isFinite(inverseEffectiveMass)) {
      return 1.0;
    }

    double effectiveMass = 1.0 / inverseEffectiveMass;
    double ratio = effectiveMass / mass;
    return Math.max(0.0, Math.min(1.0, ratio));
  }

  private static boolean isAttackType(AttackType attackType, Class<? extends AttackCapable> capableInterface) {
    return attackType != null
      && attackType.capableInterface() != null
      && capableInterface.isAssignableFrom(attackType.capableInterface());
  }

  /**
   * Maps a local normalized X on a part's bounds to assembly-normalized X.
   *
   * @param partEntry   part entry whose bounds are sampled
   * @param localNormX  normalized local position along weapon-X mapped axis
   * @param samples     sampling resolution used for bounds
   * @param totalLength total assembly length in cm
   * @return normalized X position in [0, 1]
   */
  private static double normalizedXOnPart(WeaponPartEntry partEntry, double localNormX, int samples,
      double totalLength) {
    GeometryUtil.Bounds bounds = partEntry.part().localBounds(samples);
    WeaponPartTransform transform = partEntry.transform();
    WeaponAxis localAxis = transform.localAxisForWeaponAxis(WeaponAxis.X);
    int sign = transform.signForWeaponAxis(WeaponAxis.X);

    double localMin = switch (localAxis) {
      case X -> bounds.minX();
      case Y -> bounds.minY();
      case Z -> bounds.minZ();
    };
    double localMax = switch (localAxis) {
      case X -> bounds.maxX();
      case Y -> bounds.maxY();
      case Z -> bounds.maxZ();
    };

    double clampedNorm = Math.max(0.0, Math.min(1.0, localNormX));
    double localPoint = localMin + (localMax - localMin) * clampedNorm;
    double worldX = sign >= 0
      ? partEntry.position().x() + localPoint
      : partEntry.position().x() - localPoint;
    return Math.max(0.0, Math.min(1.0, worldX / totalLength));
  }

  private static double distanceSquaredToAxis(Vec3 pos, WeaponAxis axis) {
    return switch (axis) {
      case X -> (pos.y() * pos.y()) + (pos.z() * pos.z());
      case Y -> (pos.x() * pos.x()) + (pos.z() * pos.z());
      case Z -> (pos.x() * pos.x()) + (pos.y() * pos.y());
    };
  }

}
