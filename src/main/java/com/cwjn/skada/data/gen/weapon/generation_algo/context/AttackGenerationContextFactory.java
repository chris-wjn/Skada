package com.cwjn.skada.data.gen.weapon.generation_algo.context;

import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.registry.AttackType;

/**
 * Factory for building shared weapon-generation snapshots and typed contexts.
 *
 * <p>The factory is the single place where assembly physics, attack delivery,
 * contact geometry, and material response are assembled for the generators.
 */
public final class AttackGenerationContextFactory {

  private static final int LARGE_SAMPLE_SIZE = WeaponAssembly.LARGE_SAMPLE_SIZE;

  /*
    Dummy attack types so that we can reference real attack types without relying on registry
  */
  public static final AttackType SLASH_ATTACK_TYPE = new AttackType("slash", null, null, null, SlashCapable.class);
  public static final AttackType THRUST_ATTACK_TYPE = new AttackType("thrust", null, null, null, ThrustCapable.class);
  public static final AttackType STRIKE_ATTACK_TYPE = new AttackType("strike", null, null, null, StrikeCapable.class);

  /**
   * Prevents instantiation.
   */
  private AttackGenerationContextFactory() {}

  /**
   * Builds the shared assembly-physics snapshot used by all generators.
   *
   * @param weapon weapon assembly to measure
   * @return immutable assembly-physics snapshot
   */
  public static AssemblyPhysicsSnapshot buildAssemblyPhysicsSnapshot(WeaponAssembly weapon) {
    return AssemblyPhysicsSnapshot.fromWeapon(weapon, LARGE_SAMPLE_SIZE);
  }

  /**
   * Builds the shared delivery snapshot for an attack type.
   *
   * @param weapon weapon assembly to measure
   * @param assembly precomputed assembly snapshot
   * @param attackType attack type being evaluated
   * @return immutable delivery snapshot
   */
  public static AttackDeliverySnapshot buildDeliverySnapshot(WeaponAssembly weapon, AssemblyPhysicsSnapshot assembly, AttackType attackType) {
    return AttackDeliverySnapshot.fromWeapon(weapon, assembly, attackType, LARGE_SAMPLE_SIZE);
  }

  /**
   * Builds the slash generation context.
   *
   * @param weapon weapon assembly to measure
   * @return slash generation context
   */
  public static AttackGenerationContextSlash buildSlashContext(WeaponAssembly weapon) {
    WeaponPartEntry primaryPartEntry = weapon.primaryPartForAttackType(SLASH_ATTACK_TYPE).orElseThrow(() -> new IllegalStateException("Tried to generate slash lethality for weapon without slash capability"));
    AssemblyPhysicsSnapshot assembly = buildAssemblyPhysicsSnapshot(weapon);
    AttackDeliverySnapshot delivery = buildDeliverySnapshot(weapon, assembly, SLASH_ATTACK_TYPE);
    ContactSnapshotSlash contact = ContactSnapshotSlash.fromPart((SlashCapable) primaryPartEntry.part(), assembly.centreOfPercussionNorm());
    MaterialResponseSnapshot material = MaterialResponseSnapshot.fromMaterial(primaryPartEntry.material());
    return new AttackGenerationContextSlash(weapon, SLASH_ATTACK_TYPE, primaryPartEntry, assembly, delivery, material, contact);
  }

  /**
   * Builds the thrust generation context.
   *
   * @param weapon weapon assembly to measure
   * @return thrust generation context
   */
  public static AttackGenerationContextThrust buildThrustContext(WeaponAssembly weapon) {
    WeaponPartEntry primaryPartEntry = weapon.primaryPartForAttackType(THRUST_ATTACK_TYPE).orElseThrow(() -> new IllegalStateException("Tried to generate thrust lethality for weapon without thrust capability"));
    AssemblyPhysicsSnapshot assembly = buildAssemblyPhysicsSnapshot(weapon);
    AttackDeliverySnapshot delivery = buildDeliverySnapshot(weapon, assembly, THRUST_ATTACK_TYPE);
    ContactSnapshotThrust contact = ContactSnapshotThrust.fromPart((ThrustCapable) primaryPartEntry.part());
    MaterialResponseSnapshot material = MaterialResponseSnapshot.fromMaterial(primaryPartEntry.material());
    return new AttackGenerationContextThrust(weapon, THRUST_ATTACK_TYPE, primaryPartEntry, assembly, delivery, material, contact);
  }

  /**
   * Builds the strike generation context.
   *
   * @param weapon weapon assembly to measure
   * @return strike generation context
   */
  public static AttackGenerationContextStrike buildStrikeContext(WeaponAssembly weapon) {
    WeaponPartEntry primaryPartEntry = weapon.primaryPartForAttackType(STRIKE_ATTACK_TYPE).orElseThrow(() -> new IllegalStateException("Tried to generate strike lethality for weapon without strike capability"));
    AssemblyPhysicsSnapshot assembly = buildAssemblyPhysicsSnapshot(weapon);
    AttackDeliverySnapshot delivery = buildDeliverySnapshot(weapon, assembly, STRIKE_ATTACK_TYPE);
    ContactSnapshotStrike contact = ContactSnapshotStrike.fromPart((StrikeCapable) primaryPartEntry.part());
    MaterialResponseSnapshot material = MaterialResponseSnapshot.fromMaterial(primaryPartEntry.material());
    return new AttackGenerationContextStrike(weapon, STRIKE_ATTACK_TYPE, primaryPartEntry, assembly, delivery, material, contact);
  }

}