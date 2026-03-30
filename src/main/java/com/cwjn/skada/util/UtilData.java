package com.cwjn.skada.util;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.armour.AccessArmourInfo;
import com.cwjn.skada.data.damage.AccessWeaponInfo;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public abstract class UtilData {

	@SuppressWarnings("null")
	public static void addWeaponArmourInfoTagIfNotExists(ItemStack i) {
		if (((AccessWeaponInfo) i.getItem()).skada$hasWeaponInfo()) {
			CompoundTag tag = i.getOrCreateTag();
			if (!tag.contains(SkadaData.WEAPON_INFO_TAG_KEY)) {
				WeaponInfo info = ((AccessWeaponInfo) i.getItem()).skada$getWeaponInfo();
				tag.put(SkadaData.WEAPON_INFO_TAG_KEY, info.toCompoundTag());
				syncWeaponAttackSelection(i, info);
			} else {
				syncWeaponAttackSelection(i, WeaponInfo.fromCompoundTag(tag.getCompound(SkadaData.WEAPON_INFO_TAG_KEY)));
			}
		}
		if (((AccessArmourInfo) i.getItem()).skada$hasArmourInfo()) {
			if (!i.getOrCreateTag().contains(SkadaData.ARMOUR_INFO_TAG_KEY)) {
				i.getOrCreateTag().put(SkadaData.ARMOUR_INFO_TAG_KEY,
								((AccessArmourInfo) i.getItem()).skada$getArmourInfo().toCompoundTag());
			}
		}
	}

	public static WeaponInfo getWeaponInfo(Player p) {
		return getWeaponInfo(p.getMainHandItem());
	}

	public static AttackType[] getAttackTypes(Player p) {
		return getAttackTypes(p.getMainHandItem());
	}

	public static AttackType getAttackType(Player p) {
		return getAttackType(p.getMainHandItem());
	}

	public static AttackTypeInfo getAttackTypeInfo(Player p) {
		return getAttackTypeInfo(p.getMainHandItem());
	}

	@SuppressWarnings("null")
	public static WeaponInfo getWeaponInfo(ItemStack i) {
		addWeaponArmourInfoTagIfNotExists(i);
		if (i.hasTag() && i.getTag().contains(SkadaData.WEAPON_INFO_TAG_KEY)) {
			WeaponInfo info = WeaponInfo.fromCompoundTag(i.getTag().getCompound(SkadaData.WEAPON_INFO_TAG_KEY));
			syncWeaponAttackSelection(i, info);
			return info;
		} else {
			return WeaponInfo.NO_WEAPON;
		}
	}

	public static AttackType[] getAttackTypes(ItemStack i) {
		WeaponInfo info = getWeaponInfo(i);
		if (info.getAttackTypes().isEmpty()) {
			return new AttackType[0];
		}

		LinkedHashSet<AttackType> orderedTypes = new LinkedHashSet<>();
		for (ResourceLocation attackTypeId : readStoredAttackTypeOrder(i.getOrCreateTag(), info)) {
			AttackType attackType = SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(attackTypeId);
			if (attackType != null && info.getAttackTypes().containsKey(attackType)) {
				orderedTypes.add(attackType);
			}
		}

		return orderedTypes.toArray(AttackType[]::new);
	}

	public static AttackType getAttackType(ItemStack i) {
		WeaponInfo info = getWeaponInfo(i);
		if (info.getAttackTypes().isEmpty()) {
			return AttackType.strike();
		}

		AttackType[] orderedAttackTypes = getAttackTypes(i);
		if (orderedAttackTypes.length == 0) {
			return AttackType.strike();
		}

		CompoundTag tag = i.getOrCreateTag();
		ResourceLocation selectedAttackTypeId = tryParseAttackTypeId(tag.getString(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY));
		if (selectedAttackTypeId != null) {
			AttackType selectedAttackType = SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(selectedAttackTypeId);
			if (selectedAttackType != null && info.getAttackTypes().containsKey(selectedAttackType)) {
				return selectedAttackType;
			}
		}

		int selectedIndex = Mth.clamp(tag.getInt(SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY), 0, orderedAttackTypes.length - 1);
		setAttackTypeIndex(i, selectedIndex);
		return orderedAttackTypes[selectedIndex];
	}

	public static AttackTypeInfo getAttackTypeInfo(ItemStack i) {
		WeaponInfo info = getWeaponInfo(i);
		AttackType attackType = getAttackType(i);
		if (info.getAttackTypes().containsKey(attackType)) {
			return info.getAttackTypes().get(attackType);
		} else {
			return AttackTypeInfo.DEFAULT;
		}
	}

	public static int getAttackTypeIndex(ItemStack i) {
		AttackType[] orderedAttackTypes = getAttackTypes(i);
		if (orderedAttackTypes.length == 0) {
			return 0;
		}

		ResourceLocation selectedAttackTypeId = tryParseAttackTypeId(i.getOrCreateTag().getString(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY));
		if (selectedAttackTypeId != null) {
			for (int index = 0; index < orderedAttackTypes.length; index++) {
				if (selectedAttackTypeId.equals(orderedAttackTypes[index].rl())) {
					if (i.getOrCreateTag().getInt(SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY) != index) {
						i.getOrCreateTag().putInt(SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY, index);
					}
					return index;
				}
			}
		}

		int selectedIndex = Mth.clamp(i.getOrCreateTag().getInt(SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY), 0, orderedAttackTypes.length - 1);
		setAttackTypeIndex(i, selectedIndex);
		return selectedIndex;
	}

	public static boolean setAttackTypeIndex(ItemStack i, int index) {
		WeaponInfo info = getWeaponInfo(i);
		AttackType[] orderedAttackTypes = getAttackTypes(i);
		if (info.getAttackTypes().isEmpty() || orderedAttackTypes.length == 0) {
			return false;
		}

		int normalizedIndex = Math.floorMod(index, orderedAttackTypes.length);
		AttackType selectedAttackType = orderedAttackTypes[normalizedIndex];
		CompoundTag tag = i.getOrCreateTag();
		tag.putInt(SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY, normalizedIndex);
		tag.putString(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY, selectedAttackType.rl().toString());
		tag.putInt(SkadaData.NUM_ATTACK_TYPES_TAG_KEY, orderedAttackTypes.length);
		return true;
	}

	private static void syncWeaponAttackSelection(ItemStack stack, WeaponInfo info) {
		if (info.getAttackTypes().isEmpty()) {
			return;
		}

		CompoundTag tag = stack.getOrCreateTag();
		List<ResourceLocation> attackOrder = readStoredAttackTypeOrder(tag, info);
		if (attackOrder.isEmpty()) {
			attackOrder = defaultAttackTypeOrder(info);
		}

		ListTag storedOrder = new ListTag();
		for (ResourceLocation attackTypeId : attackOrder) {
			storedOrder.add(StringTag.valueOf(attackTypeId.toString()));
		}

		tag.put(SkadaData.ATTACK_TYPES_ARRAY_TAG_KEY, storedOrder);
		tag.putInt(SkadaData.NUM_ATTACK_TYPES_TAG_KEY, attackOrder.size());

		int selectedIndex = Mth.clamp(tag.getInt(SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY), 0, attackOrder.size() - 1);
		ResourceLocation selectedAttackTypeId = tryParseAttackTypeId(tag.getString(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY));
		if (selectedAttackTypeId == null || !attackOrder.contains(selectedAttackTypeId)) {
			selectedAttackTypeId = attackOrder.get(selectedIndex);
		}

		tag.putString(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY, selectedAttackTypeId.toString());
		tag.putInt(SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY, attackOrder.indexOf(selectedAttackTypeId));
	}

	private static List<ResourceLocation> defaultAttackTypeOrder(WeaponInfo info) {
		List<ResourceLocation> attackOrder = new ArrayList<>();
		for (AttackType attackType : info.getAttackTypes().keySet()) {
			ResourceLocation attackTypeId = attackType.rl();
			if (attackTypeId != null) {
				attackOrder.add(attackTypeId);
			}
		}
		return attackOrder;
	}

	private static List<ResourceLocation> readStoredAttackTypeOrder(CompoundTag tag, WeaponInfo info) {
		LinkedHashSet<ResourceLocation> orderedIds = new LinkedHashSet<>();
		if (tag.contains(SkadaData.ATTACK_TYPES_ARRAY_TAG_KEY, Tag.TAG_LIST)) {
			ListTag attackTypeList = tag.getList(SkadaData.ATTACK_TYPES_ARRAY_TAG_KEY, Tag.TAG_STRING);
			for (Tag attackTypeTag : attackTypeList) {
				if (attackTypeTag instanceof StringTag stringTag) {
					ResourceLocation attackTypeId = tryParseAttackTypeId(stringTag.getAsString());
					if (attackTypeId == null) {
						continue;
					}
					AttackType attackType = SkadaData.REGISTRY_ATTACK_TYPE.get().getValue(attackTypeId);
					if (attackType != null && info.getAttackTypes().containsKey(attackType)) {
						orderedIds.add(attackTypeId);
					}
				}
			}
		}

		for (ResourceLocation defaultAttackTypeId : defaultAttackTypeOrder(info)) {
			orderedIds.add(defaultAttackTypeId);
		}

		return new ArrayList<>(orderedIds);
	}

	private static ResourceLocation tryParseAttackTypeId(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return ResourceLocation.tryParse(value);
	}

  
  
}
