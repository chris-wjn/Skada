package com.cwjn.skada.data.gen.weapon.generation_algo.context;

import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.registry.AttackType;

/**
 * Thrust-specific generation context.
 *
 * <p>This wraps the shared attack-generation data with the thrust contact
 * snapshot used by the lethality, precision, and fail formulas.
 */
public final class AttackGenerationContextThrust extends AttackGenerationContext {

  private final ContactSnapshotThrust contact;

  /**
   * Creates a thrust generation context.
   *
   * @param weapon weapon assembly being analyzed
   * @param attackType thrust attack type descriptor
   * @param primaryPartEntry thrust-capable primary part entry
   * @param assembly shared assembly snapshot
   * @param delivery shared delivery snapshot
   * @param material shared material snapshot
   * @param contact thrust contact snapshot
   */
  public AttackGenerationContextThrust(
      WeaponAssembly weapon,
      AttackType attackType,
      WeaponPartEntry primaryPartEntry,
      AssemblyPhysicsSnapshot assembly,
      AttackDeliverySnapshot delivery,
      MaterialResponseSnapshot material,
      ContactSnapshotThrust contact) {
    super(weapon, attackType, primaryPartEntry, assembly, delivery, material);
    this.contact = contact;
  }

  public ContactSnapshotThrust contact() {
    return contact;
  }

}