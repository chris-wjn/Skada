package com.cwjn.skada.data.gen.weapon.generation_algo.context;

import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.registry.AttackType;

/**
 * Strike-specific generation context.
 *
 * <p>This wraps the shared attack-generation data with the strike contact
 * snapshot used by the lethality, precision, and fail formulas.
 */
public final class AttackGenerationContextStrike extends AttackGenerationContext {

  private final ContactSnapshotStrike contact;

  /**
   * Creates a strike generation context.
   *
   * @param weapon weapon assembly being analyzed
   * @param attackType strike attack type descriptor
   * @param primaryPartEntry strike-capable primary part entry
   * @param assembly shared assembly snapshot
   * @param delivery shared delivery snapshot
   * @param material shared material snapshot
   * @param contact strike contact snapshot
   */
  public AttackGenerationContextStrike(
      WeaponAssembly weapon,
      AttackType attackType,
      WeaponPartEntry primaryPartEntry,
      AssemblyPhysicsSnapshot assembly,
      AttackDeliverySnapshot delivery,
      MaterialResponseSnapshot material,
      ContactSnapshotStrike contact) {
    super(weapon, attackType, primaryPartEntry, assembly, delivery, material);
    this.contact = contact;
  }

  public ContactSnapshotStrike contact() {
    return contact;
  }

}