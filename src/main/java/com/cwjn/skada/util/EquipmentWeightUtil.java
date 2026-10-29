package com.cwjn.skada.util;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.armour.AccessArmourInfo;
import com.cwjn.skada.data.damage.AccessWeaponInfo;
import com.cwjn.skada.data.damage.AttackSelection;
import com.cwjn.skada.data.damage.WeaponInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Reads equipment mass without treating the rest of an entity's inventory as worn or held gear. */
public final class EquipmentWeightUtil {
    private static final List<EquipmentSlot> EQUIPPED_SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);

    private EquipmentWeightUtil() {}

    /** Returns physical mass in kilograms for each worn or held stack, excluding inventory contents. */
    public static double getTotalEquippedWeightKg(LivingEntity entity) {
        return sumEquippedStacks(EQUIPPED_SLOTS.stream().map(entity::getItemBySlot).toList());
    }

    /** Sums exactly the supplied equipped stacks; callers must not pass unequipped inventory slots. */
    public static double sumEquippedStacks(Iterable<ItemStack> stacks) {
        double total = 0.0;
        for (ItemStack stack : stacks) {
            total += getStackWeightKg(stack);
            if (!Double.isFinite(total)) return Double.MAX_VALUE;
        }
        return total;
    }

    /** Returns mass from the stack's saved profile or its current item default, if configured. */
    public static double getStackWeightKg(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0;
        if (stack.getItem() instanceof AccessWeaponInfo || stack.getItem() instanceof AccessArmourInfo) {
            UtilData.addWeaponArmourInfoTagIfNotExists(stack);
        }
        CompoundTag root = stack.getTag();
        if (root == null) return 0.0;
        double weight = 0.0;
        CompoundTag weaponTag = root.getCompound(SkadaData.WEAPON_INFO_TAG_KEY);
        if (root.contains(SkadaData.WEAPON_INFO_TAG_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            AttackSelection.resolve(stack);
            weight += WeaponInfo.fromCompoundTag(weaponTag).weightKg();
        }
        CompoundTag armourTag = root.getCompound(SkadaData.ARMOUR_INFO_TAG_KEY);
        if (root.contains(SkadaData.ARMOUR_INFO_TAG_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            weight += ArmourInfo.fromCompoundTag(armourTag).weightKg();
        }
        return Double.isFinite(weight) && weight > 0.0 ? weight : 0.0;
    }
}
