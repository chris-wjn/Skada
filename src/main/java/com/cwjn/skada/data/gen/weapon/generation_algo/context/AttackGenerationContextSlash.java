package com.cwjn.skada.data.gen.weapon.generation_algo.context;

import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.registry.AttackType;

/**
 * Slash-specific generation context.
 *
 * <p>This wraps the shared attack-generation data with the slash contact
 * snapshot used by the lethality, precision, and fail formulas.
 */
public final class AttackGenerationContextSlash extends AttackGenerationContext {

  private final ContactSnapshotSlash contact;

  /**
   * Creates a slash generation context.
   *
   * @param weapon weapon assembly being analyzed
   * @param attackType slash attack type descriptor
   * @param primaryPartEntry slash-capable primary part entry
   * @param assembly shared assembly snapshot
   * @param delivery shared delivery snapshot
   * @param material shared material snapshot
   * @param contact slash contact snapshot
   */
  public AttackGenerationContextSlash(
      WeaponAssembly weapon,
      AttackType attackType,
      WeaponPartEntry primaryPartEntry,
      AssemblyPhysicsSnapshot assembly,
      AttackDeliverySnapshot delivery,
      MaterialResponseSnapshot material,
      ContactSnapshotSlash contact) {
    super(weapon, attackType, primaryPartEntry, assembly, delivery, material);
    this.contact = contact;
  }

  public ContactSnapshotSlash contact() {
    return contact;
  }

}