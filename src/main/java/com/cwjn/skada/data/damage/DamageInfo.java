package com.cwjn.skada.data.damage;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * Immutable combat snapshot stored on a launched attack or projectile.
 */
public record DamageInfo(double lethality, double precision, boolean isEnvironmental, AttackType attackType,
                         ElementSpreadInstance elementSpreadInstance, @javax.annotation.Nullable Double projectileDamage) {

    /** Retains the direct/environmental snapshot constructor and legacy callers. */
    public DamageInfo(double lethality, double precision, boolean isEnvironmental, AttackType attackType, ElementSpreadInstance spread) {
        this(lethality, precision, isEnvironmental, attackType, spread, null);
    }

    private static final String LETHALITY_TAG = "lethality";
    private static final String PRECISION_TAG = "precision";
    private static final String ENVIRONMENTAL_TAG = "environmental";
    private static final String ATTACK_TYPE_TAG = "attack_type";
    private static final String ELEMENT_SPREAD_TAG = "element_spread";

    /**
     * Creates the projectile snapshot for a launched weapon attack.
     */
    public static DamageInfo projectile(AttackTypeInfo attackInfo, AttackType attackType, ElementSpreadInstance elementSpreadInstance) {
        return new DamageInfo(
                attackInfo.lethality(),
                attackInfo.precision(),
                false,
                attackType,
                elementSpreadInstance, attackInfo.damage());
    }

    /**
     * Creates a fallback projectile snapshot when no weapon-specific attack data exists.
     */
    public static DamageInfo projectileFallback(AttackType attackType, double precision, ElementSpreadInstance elementSpreadInstance) {
        return new DamageInfo(0.0, precision, false, attackType, elementSpreadInstance);
    }

    public static DamageInfo environmental(ElementSpreadInstance spread) {
        return new DamageInfo(0, 0, true, AttackType.strike(), spread);
    }

    /**
     * Serializes this snapshot for persistent projectile storage.
     */
    public CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(LETHALITY_TAG, lethality);
        tag.putDouble(PRECISION_TAG, precision);
        tag.putBoolean(ENVIRONMENTAL_TAG, isEnvironmental);
        tag.putString(ATTACK_TYPE_TAG, attackType.rl().toString());
        tag.put(ELEMENT_SPREAD_TAG, elementSpreadInstance.toCompoundTag());
        if (projectileDamage != null) tag.putDouble("projectile_damage", projectileDamage);
        return tag;
    }

    /**
     * Restores a projectile snapshot from persistent storage.
     */
    public static DamageInfo fromCompoundTag(CompoundTag tag) {
        ElementSpreadInstance spread = tag.contains(ELEMENT_SPREAD_TAG, Tag.TAG_COMPOUND)
                ? ElementSpreadInstance.fromCompoundTag(tag.getCompound(ELEMENT_SPREAD_TAG))
                : new ElementSpreadInstance();
        return new DamageInfo(
                tag.getDouble(LETHALITY_TAG),
                tag.getDouble(PRECISION_TAG),
                tag.getBoolean(ENVIRONMENTAL_TAG),
                attackTypeFromTag(tag),
                spread, tag.contains("projectile_damage", Tag.TAG_ANY_NUMERIC) ? tag.getDouble("projectile_damage") : null);
    }

    private static AttackType attackTypeFromTag(CompoundTag tag) {
        ResourceLocation attackTypeId = ResourceLocation.tryParse(tag.getString(ATTACK_TYPE_TAG));
        if (attackTypeId == null) {
            return AttackType.none();
        }
        AttackType attackType = SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(attackTypeId);
        return attackType != null ? attackType : AttackType.none();
    }

    @Override
    public String toString() {
        return "DamageInfo{" +
                "lethality=" + lethality +
                ", precision=" + precision +
                ", isEnvironmental=" + isEnvironmental +
                ", attackType=" + attackType +
                ", elementSpreadInstance=" + elementSpreadInstance +
                '}';
    }

}
