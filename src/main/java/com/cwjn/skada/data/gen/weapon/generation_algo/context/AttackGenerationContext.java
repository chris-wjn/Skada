package com.cwjn.skada.data.gen.weapon.generation_algo.context;

import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.registry.AttackType;

import java.util.Objects;

/**
 * Base immutable context for weapon-stat generation.
 *
 * <p>It bundles the shared weapon, attack, assembly, delivery, and material
 * descriptors that the per-stat generators interpret differently.
 */
public abstract class AttackGenerationContext {

  private final WeaponAssembly weapon;
  private final AttackType attackType;
  private final WeaponPartEntry primaryPartEntry;
  private final AssemblyPhysicsSnapshot assembly;
  private final AttackDeliverySnapshot delivery;
  private final MaterialResponseSnapshot material;

  /**
   * Creates a new generation context.
   *
   * @param weapon weapon assembly being analyzed
   * @param attackType attack type being evaluated
   * @param primaryPartEntry attack-capable primary part entry
   * @param assembly shared assembly-physics snapshot
   * @param delivery shared attack-delivery snapshot
   * @param material shared material-response snapshot
   */
  protected AttackGenerationContext(
      WeaponAssembly weapon,
      AttackType attackType,
      WeaponPartEntry primaryPartEntry,
      AssemblyPhysicsSnapshot assembly,
      AttackDeliverySnapshot delivery,
      MaterialResponseSnapshot material) {
    this.weapon = Objects.requireNonNull(weapon, "weapon");
    this.attackType = Objects.requireNonNull(attackType, "attackType");
    this.primaryPartEntry = Objects.requireNonNull(primaryPartEntry, "primaryPartEntry");
    this.assembly = Objects.requireNonNull(assembly, "assembly");
    this.delivery = Objects.requireNonNull(delivery, "delivery");
    this.material = Objects.requireNonNull(material, "material");
  }

  public WeaponAssembly weapon() {
    return weapon;
  }

  public AttackType attackType() {
    return attackType;
  }

  public WeaponPartEntry primaryPartEntry() {
    return primaryPartEntry;
  }

  public AssemblyPhysicsSnapshot assembly() {
    return assembly;
  }

  public AttackDeliverySnapshot delivery() {
    return delivery;
  }

  public MaterialResponseSnapshot material() {
    return material;
  }

}