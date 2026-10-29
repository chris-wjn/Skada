package com.cwjn.skada.util;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.armour.AccessArmourInfo;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.damage.*;
import com.cwjn.skada.data.mob.MobData;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/** Compatibility accessors; attack initialization and reconciliation live in AttackSelection. */
public abstract class UtilData {
    /** Lazily initializes equipment data at the existing access opportunities. */
    public static void addWeaponArmourInfoTagIfNotExists(ItemStack stack) {
        AttackSelection.resolve(stack);
        if (stack.getItem() instanceof AccessArmourInfo access && access.skada$hasArmourInfo()
                && stack.getTagElement(SkadaData.ARMOUR_INFO_TAG_KEY) == null) {
            stack.getOrCreateTag().put(SkadaData.ARMOUR_INFO_TAG_KEY, access.skada$getArmourInfo().toCompoundTag());
        } else if (stack.getItem() instanceof AccessArmourInfo access && access.skada$hasArmourInfo()) {
            net.minecraft.nbt.CompoundTag saved = stack.getTagElement(SkadaData.ARMOUR_INFO_TAG_KEY);
            ArmourInfo defaults = access.skada$getArmourInfo();
            if (saved != null && !saved.contains("weight_kg", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
                // Add only the new field; keep the legacy armour values and any future fields intact.
                saved.putDouble("weight_kg", defaults.weightKg());
            }
        }
    }

    /** Returns the effective profile for a direct player attack. */
    public static WeaponInfo getPlayerAttackWeaponInfo(Player player) { return AttackSelection.melee(player.getMainHandItem()).weapon(); }
    /** Returns authored stack data, including projectile profiles. */
    public static WeaponInfo getWeaponInfo(Player player) { return getWeaponInfo(player.getMainHandItem()); }
    /** Returns authored stack data, including projectile profiles. */
    public static WeaponInfo getWeaponInfo(ItemStack stack) { return AttackSelection.resolve(stack).weapon(); }
    /** Returns the effective direct-attack order. */
    public static AttackType[] getAttackTypes(Player player) { return AttackSelection.melee(player.getMainHandItem()).order().toArray(AttackType[]::new); }
    /** Returns the authored order for cycling and inspection. */
    public static AttackType[] getAttackTypes(ItemStack stack) { return AttackSelection.resolve(stack).order().toArray(AttackType[]::new); }
    /** Returns the direct-attack type. */
    public static AttackType getAttackType(Player player) { return AttackSelection.melee(player.getMainHandItem()).type(); }
    /** Returns the authored selected type. */
    public static AttackType getAttackType(ItemStack stack) { return AttackSelection.resolve(stack).type(); }
    /** Returns direct-attack statistics. */
    public static AttackTypeInfo getAttackTypeInfo(Player player) { return AttackSelection.melee(player.getMainHandItem()).stats(); }
    /** Returns authored selected statistics. */
    public static AttackTypeInfo getAttackTypeInfo(ItemStack stack) { return AttackSelection.resolve(stack).stats(); }
    /** Derives the index from the stable selection and current order. */
    public static int getAttackTypeIndex(ItemStack stack) { return AttackSelection.resolve(stack).index(); }
    /** Retains index-based selection for callers that already have a position. */
    public static boolean setAttackTypeIndex(ItemStack stack, int index) { return AttackSelection.selectIndex(stack, index); }

    /** Builds a projectile snapshot respecting applicability. */
    public static DamageInfo getProjectileDamageInfo(ItemStack stack, AttackType fallback, double precision) {
        return ProjectileLaunch.resolve(stack, fallback, precision, ProjectileLaunch.Flight.UNCHANGED).damageInfo();
    }

    /** Resolves held melee data before falling back to the mob's intrinsic attack. */
    public static DamageInfo getMobDirectAttackDamageInfo(LivingEntity mob) {
        return getMobDirectAttackDamageInfo(mob, SkadaData.MOB_DATA.get(mob.getType()));
    }

    /** Resolves held melee data before falling back to explicitly supplied mob data. */
    public static DamageInfo getMobDirectAttackDamageInfo(LivingEntity mob, @Nullable MobData mobData) {
        AttackSelection selected = AttackSelection.melee(mob.getMainHandItem());
        if (!mob.getMainHandItem().isEmpty() && selected.configured()) return selected.damageInfo();
        return mobData != null && mobData.damageInfo() != null ? mobData.damageInfo() : MobData.defaultDamageInfo(null);
    }
}
