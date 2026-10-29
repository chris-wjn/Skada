package com.cwjn.skada.data.damage;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import java.util.*;

/**
 * A coherent selected attack. Owns lazy weapon initialization, legacy migration and order repair.
 * Persisted IDs and order are canonical; index and count are derived from this result.
 */
public record AttackSelection(WeaponInfo weapon, AttackType type, AttackTypeInfo stats,
                              List<AttackType> order, int index, boolean configured) {

    /** Resolves authored stack data, copying item defaults only when stack data is absent. */
    public static AttackSelection resolve(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if ((tag == null || !tag.contains(SkadaData.WEAPON_INFO_TAG_KEY, Tag.TAG_COMPOUND))
                && stack.getItem() instanceof AccessWeaponInfo access && access.skada$hasWeaponInfo()) {
            tag = stack.getOrCreateTag();
            tag.put(SkadaData.WEAPON_INFO_TAG_KEY, access.skada$getWeaponInfo().toCompoundTag());
        }
        if (tag == null || !tag.contains(SkadaData.WEAPON_INFO_TAG_KEY, Tag.TAG_COMPOUND)) {
            return fallback(stack);
        }
        migrateMissingKnockback(stack, tag.getCompound(SkadaData.WEAPON_INFO_TAG_KEY));
        WeaponInfo info = WeaponInfo.fromCompoundTag(tag.getCompound(SkadaData.WEAPON_INFO_TAG_KEY));
        CompoundTag savedWeaponInfo = tag.getCompound(SkadaData.WEAPON_INFO_TAG_KEY);
        if (!savedWeaponInfo.contains("weight_kg", Tag.TAG_ANY_NUMERIC)
                && stack.getItem() instanceof AccessWeaponInfo access && access.skada$hasWeaponInfo()
                && access.skada$getWeaponInfo().weightConfigured()) {
            // Migrate only the absent field so all legacy attack statistics and unknown saved fields survive.
            savedWeaponInfo.putDouble("weight_kg", access.skada$getWeaponInfo().weightKg());
            info = info.withWeightDefault(access.skada$getWeaponInfo());
        }
        LinkedHashMap<ResourceLocation, AttackType> supported = new LinkedHashMap<>();
        for (AttackType type : info.getAttackTypes().keySet()) {
            if (type != null && type.rl() != null) supported.put(type.rl(), type);
        }
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        for (Tag entry : tag.getList(SkadaData.ATTACK_TYPES_ARRAY_TAG_KEY, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            if (supported.containsKey(id)) ids.add(id);
        }
        ids.addAll(supported.keySet());
        List<ResourceLocation> orderIds = List.copyOf(ids);
        if (orderIds.isEmpty()) {
            tag.remove(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY);
            tag.remove(SkadaData.ATTACK_TYPES_ARRAY_TAG_KEY);
            tag.remove(SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY);
            tag.remove(SkadaData.NUM_ATTACK_TYPES_TAG_KEY);
            return new AttackSelection(info, AttackType.strike(), AttackTypeInfo.FIST, List.of(), 0, false);
        }
        ResourceLocation selected = ResourceLocation.tryParse(tag.getString(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY));
        int index = orderIds.indexOf(selected);
        if (index < 0) {
            index = Math.max(0, Math.min(tag.getInt(SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY), orderIds.size() - 1));
            selected = orderIds.get(index);
        }
        ListTag repairedOrder = new ListTag();
        orderIds.forEach(id -> repairedOrder.add(StringTag.valueOf(id.toString())));
        if (!repairedOrder.equals(tag.get(SkadaData.ATTACK_TYPES_ARRAY_TAG_KEY))) {
            tag.put(SkadaData.ATTACK_TYPES_ARRAY_TAG_KEY, repairedOrder);
        }
        if (!selected.toString().equals(tag.getString(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY))) {
            tag.putString(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY, selected.toString());
        }
        tag.remove(SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY);
        tag.remove(SkadaData.NUM_ATTACK_TYPES_TAG_KEY);
        AttackType type = supported.get(selected);
        return new AttackSelection(info, type, info.getAttackTypes().get(type),
                orderIds.stream().map(supported::get).toList(), index, true);
    }

    /** Adds newly introduced attack fields from the current item defaults without replacing authored stats. */
    private static void migrateMissingKnockback(ItemStack stack, CompoundTag savedInfo) {
        if (!(stack.getItem() instanceof AccessWeaponInfo access) || !access.skada$hasWeaponInfo()) return;
        migrateMissingKnockback(savedInfo, access.skada$getWeaponInfo().getAttackTypes());
    }

    /** Fills only absent values so authored numbers, including zero, remain authoritative. */
    static void migrateMissingKnockback(CompoundTag savedInfo, Map<AttackType, AttackTypeInfo> defaults) {
        CompoundTag savedAttacks = savedInfo.getCompound("attack_types");
        for (String key : savedAttacks.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            AttackType type = id == null ? null : SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(id);
            if (type == null) continue;
            CompoundTag savedAttack = savedAttacks.getCompound(key);
            if (savedAttack.contains("knockback", Tag.TAG_DOUBLE)) continue;
            AttackTypeInfo defaultAttack = defaults.get(type);
            savedAttack.putDouble("knockback", defaultAttack == null ? 0.0 : defaultAttack.knockback());
        }
    }

    /** Resolves direct-attack statistics independently of the attribute replacement setting. */
    public static AttackSelection melee(ItemStack stack) {
        AttackSelection selected = resolve(stack);
        return selected.weapon.applicability().melee() ? selected : fallback(stack);
    }

    /** Supplies fist, held-block or generic-item defaults without writing stack NBT. */
    private static AttackSelection fallback(ItemStack stack) {
        boolean empty = stack.isEmpty();
        boolean block = !empty && stack.getItem() instanceof BlockItem;
        AttackType type = AttackType.strike();
        return new AttackSelection(empty ? WeaponInfo.FIST : block ? WeaponInfo.BLOCK : WeaponInfo.RANDOM_ITEM, type,
                empty ? AttackTypeInfo.FIST : block ? AttackTypeInfo.BLOCK : AttackTypeInfo.ITEM, List.of(type), 0, false);
    }

    /** Accepts a supported stable ID only; used for authoritative client requests. */
    public static boolean select(ItemStack stack, ResourceLocation id) {
        AttackSelection current = resolve(stack);
        if (!current.configured || current.order.stream().noneMatch(type -> type.rl().equals(id))) return false;
        CompoundTag tag = stack.getOrCreateTag();
        if (!id.toString().equals(tag.getString(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY))) {
            tag.putString(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY, id.toString());
        }
        return true;
    }

    /** Selects a derived position, wrapping exactly as the existing cycling controls do. */
    public static boolean selectIndex(ItemStack stack, int index) {
        AttackSelection current = resolve(stack);
        if (!current.configured || current.order.isEmpty()) return false;
        ResourceLocation id = current.order.get(Math.floorMod(index, current.order.size())).rl();
        stack.getOrCreateTag().putString(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY, id.toString());
        return true;
    }

    /** Cycles from this resolved order without decoding or reconciling the stack again. */
    public ResourceLocation cycle(ItemStack stack) {
        ResourceLocation id = order.get((index + 1) % order.size()).rl();
        stack.getOrCreateTag().putString(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY, id.toString());
        return id;
    }

    /** Captures this direct attack's combat data for the damage pipeline. */
    public DamageInfo damageInfo() {
        return new DamageInfo(stats.lethality(), stats.precision(), false, type, weapon.getSpread().instance());
    }
}
