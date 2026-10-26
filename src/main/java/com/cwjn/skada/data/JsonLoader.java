package com.cwjn.skada.data;

import com.cwjn.skada.Skada;
import com.cwjn.skada.client.hud.ReticleShape;
import com.cwjn.skada.data.armour.AccessArmourInfo;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.damage.AccessWeaponInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.mob.BestiaryEntryViewBuilder;
import com.cwjn.skada.data.mob.BestiaryInfo;
import com.cwjn.skada.data.mob.MobData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.BufferedReader;
import java.util.Map;

import static com.cwjn.skada.data.SkadaData.*;

public abstract class JsonLoader {

	public static void updateReticleListFromResources(ResourceManager manager) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Skada.LOGGER.info("------------------> Reading reticle json files");
		manager.listResources("reticles", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
			Skada.LOGGER.info("--------------> " + rl.toString());
			try {
				BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
				JsonObject obj = gson.fromJson(reader, JsonObject.class);
				DataResult<ReticleShape> info = ReticleShape.CODEC.parse(JsonOps.INSTANCE, obj);
				info.result().ifPresent((x) -> RETICLES.put(x.getName(), x));
			} catch (Exception e) {
				Skada.LOGGER.error("Failed to read reticle info from " + rl, e);
			}
		});
	}

	public static void updateWeaponInfoItemsFromResources(ResourceManager manager) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Skada.LOGGER.info("------------------> Reading Weapon Info json files");
		manager.listResources("weapon_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
			Skada.LOGGER.info("--------------> " + rl.toString());
			String[] pathSplit = rl.getPath().split("/");
			String modId = pathSplit[pathSplit.length - 1].substring(0, pathSplit[pathSplit.length - 1].length() - 5);
			if (FMLLoader.getLoadingModList().getModFileById(modId) != null) {
				try {
					BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
					JsonObject obj = gson.fromJson(reader, JsonObject.class);
					DataResult<Map<String, WeaponInfo>> info = WeaponInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
					info.result().ifPresent((map) -> applyWeaponInfoMapToItems(modId, map));
				} catch (Exception e) {
					Skada.LOGGER.error("Failed to read weapon info from " + rl, e);
				}
			} else {
				Skada.LOGGER.info("----------> Skipping weapon info file for mod " + modId + " because it is not loaded!");
			}
		});
	}

	private static void applyWeaponInfoMapToItems(String modId, Map<String, WeaponInfo> map) {
		map.forEach((key, value) -> {
			Skada.LOGGER.info("----------> {}:{}", modId, key);
			@SuppressWarnings("null")
			ResourceLocation iRL = new ResourceLocation(modId, key);
			Item iItem = ForgeRegistries.ITEMS.getValue(iRL);
			if (iItem != null) {
				AccessWeaponInfo mItem = (AccessWeaponInfo) iItem;
				if (value.getAttackTypes().isEmpty()) {
					Skada.LOGGER.error("Weapon info for {} has no attack types, skipping", iRL);
				} else {
					mItem.skada$setWeaponInfo(value);
				}
			}
		});
	}

	public static void updateArmourInfoItemsFromResources(ResourceManager manager) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Skada.LOGGER.info("------------------> Reading Armour Info json files");
		manager.listResources("armour_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
			Skada.LOGGER.info("--------------> " + rl.toString());
			String[] pathSplit = rl.getPath().split("/");
			String modId = pathSplit[pathSplit.length - 1].substring(0, pathSplit[pathSplit.length - 1].length() - 5);
			if (FMLLoader.getLoadingModList().getModFileById(modId) != null) {
				try {
					BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
					JsonObject obj = gson.fromJson(reader, JsonObject.class);
					DataResult<Map<String, ArmourInfo>> info = ArmourInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
					info.result().ifPresent((map) -> {
						map.forEach((key, value) -> {
							Skada.LOGGER.info("----------> " + key);
							@SuppressWarnings("null")
							ResourceLocation iRL = new ResourceLocation(modId, key);
							Item iItem = ForgeRegistries.ITEMS.getValue(iRL);
							if (iItem != null) {
								AccessArmourInfo mItem = (AccessArmourInfo) iItem;
								mItem.skada$setArmourInfo(value);
							}
						});
					});
				} catch (Exception e) {
					Skada.LOGGER.error("Failed to read armour info from " + rl, e);
				}
			} else {
				Skada.LOGGER.info("----------> Skipping armour info file for mod " + modId + " because it is not loaded!");
			}
		});
	}

	public static void updateMobInfoFromResources(ResourceManager manager) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		MOB_DATA.clear();
		BESTIARY_ENTRY_VIEWS.clear();
		Skada.LOGGER.info("------------------> Reading Mob Info json files");
		manager.listResources("mob_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
			Skada.LOGGER.info("--------------> " + rl.toString());
			String[] pathSplit = rl.getPath().split("/");
			String modId = pathSplit[pathSplit.length - 1].substring(0, pathSplit[pathSplit.length - 1].length() - 5);
			if (FMLLoader.getLoadingModList().getModFileById(modId) != null) {
				try {
					BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
					JsonObject obj = gson.fromJson(reader, JsonObject.class);
					DataResult<Map<String, MobData>> info = MobData.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
					info.result().ifPresent((map) -> {
						map.forEach((key, value) -> {
							Skada.LOGGER.info("----------> " + key);
							@SuppressWarnings("null")
							ResourceLocation iRL = new ResourceLocation(modId, key);
							EntityType<?> iEntity = getMobEntityType(iRL);
							if (iEntity != null) {
								MOB_DATA.put(iEntity, value);
							}
						});
					});
				} catch (Exception e) {
					Skada.LOGGER.error("Failed to read mob info from " + rl, e);
				}
			} else {
				Skada.LOGGER.info("----------> Skipping mob info file for mod " + modId + " because it is not loaded!");
			}
		});
		Skada.LOGGER.info("-----------> Finished loading mob info, flattening parents");
		MOB_DATA.forEach((key, value) -> {
			if (value.parents() != null) flattenParentModifiers(key, value);
		});
	}

	public static void updateBestiaryInfoFromResources(ResourceManager manager) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		BESTIARY_INFO.clear();
		BESTIARY_ENTRY_VIEWS.clear();
		Skada.LOGGER.info("------------------> Reading Bestiary Info json files");
		manager.listResources("bestiary_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
			Skada.LOGGER.info("--------------> " + rl.toString());
			String[] pathSplit = rl.getPath().split("/");
			String modId = pathSplit[pathSplit.length - 1].substring(0, pathSplit[pathSplit.length - 1].length() - 5);
			if (shouldLoadBestiaryNamespace(modId)) {
				try {
					BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
					JsonObject obj = gson.fromJson(reader, JsonObject.class);
					DataResult<Map<String, BestiaryInfo>> info = BestiaryInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
					info.result().ifPresent((map) -> {
						map.forEach((key, value) -> {
							Skada.LOGGER.info("----------> " + key);
							@SuppressWarnings("null")
							ResourceLocation iRL = new ResourceLocation(modId, key);
							EntityType<?> iEntity = getMobEntityType(iRL);
							if (iEntity != null) {
								BESTIARY_INFO.put(iEntity, value);
							}
						});
					});
				} catch (Exception e) {
					Skada.LOGGER.error("Failed to read bestiary info from " + rl, e);
				}
			} else {
				Skada.LOGGER.info("----------> Skipping bestiary info file for mod " + modId + " because it is not loaded!");
			}
		});
	}

	static boolean shouldLoadBestiaryNamespace(String namespace) {
		if ("minecraft".equals(namespace)) {
			return true;
		}
		return FMLLoader.getLoadingModList() != null
			&& FMLLoader.getLoadingModList().getModFileById(namespace) != null;
	}

	public static void rebuildBestiaryEntryViews() {
		BESTIARY_ENTRY_VIEWS.clear();
		MOB_DATA.forEach((entityType, mobData) -> {
			try {
				BESTIARY_ENTRY_VIEWS.put(entityType,
						BestiaryEntryViewBuilder.build(entityType, mobData, BESTIARY_INFO.get(entityType)));
			} catch (Exception e) {
				Skada.LOGGER.error("Failed to build bestiary entry view for {}", entityType, e);
			}
		});
	}

	private static void flattenParentModifiers(EntityType<?> type, MobData mobData) {
		flattenParentModifiers(type, mobData, new java.util.LinkedHashSet<>());
	}

	private static void flattenParentModifiers(EntityType<?> type, MobData mobData, java.util.LinkedHashSet<EntityType<?>> flatteningChain) {
		if (mobData.parents() == null || mobData.parents().isEmpty()) return;
		if (!flatteningChain.add(type)) {
			Skada.LOGGER.error("Detected cyclic mob parent chain while flattening {}: {}", ForgeRegistries.ENTITY_TYPES.getKey(type),
				flatteningChain.stream().map(ForgeRegistries.ENTITY_TYPES::getKey).toList());
			return;
		}
		@SuppressWarnings("null")
		Multimap<Attribute, AttributeModifier> flattenedModifiers = ArrayListMultimap.create(mobData.extraModifiers());

		try {
			for (String parentPath : mobData.parents()) {
				@SuppressWarnings("null")
				ResourceLocation parentRL = ResourceLocation.tryParse(parentPath);
				if (parentRL != null) {
					EntityType<?> parentEntity = getMobEntityType(parentRL);
					if (parentEntity != null && MOB_DATA.containsKey(parentEntity)) {
						if (flatteningChain.contains(parentEntity)) {
							Skada.LOGGER.error("Detected cyclic mob parent reference while flattening {}: {} -> {}",
								ForgeRegistries.ENTITY_TYPES.getKey(type),
								flatteningChain.stream().map(ForgeRegistries.ENTITY_TYPES::getKey).toList(),
								ForgeRegistries.ENTITY_TYPES.getKey(parentEntity));
							continue;
						}
						MobData parentData = MOB_DATA.get(parentEntity);
						flattenParentModifiers(parentEntity, parentData, flatteningChain);
						MobData flattenedParentData = MOB_DATA.get(parentEntity);
						if (flattenedParentData != null) {
							flattenedParentData.extraModifiers().entries().forEach(entry -> {
								flattenedModifiers.put(entry.getKey(), entry.getValue());
							});
						}
					}
				}
			}

			MOB_DATA.put(type, new MobData(null, mobData.damageInfo(), flattenedModifiers));
		} finally {
			flatteningChain.remove(type);
		}
	}

	static EntityType<?> getMobEntityType(ResourceLocation iRL) {
		return ForgeRegistries.ENTITY_TYPES.getValue(iRL);
	}

}