package com.cwjn.skada.generation;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.JsonLoader;
import com.cwjn.skada.generation.armour.ArmourConstructionInfo;
import com.cwjn.skada.generation.armour.ArmourPieceInfo;
import com.cwjn.skada.generation.weapon.MaterialInfo;
import com.cwjn.skada.generation.weapon.WeaponAssembly;
import com.cwjn.skada.generation.weapon.parts.WeaponPart;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class GenJsonLoader {

	private static final String WEAPON_DATA_ROOT = "generator_data/weapon";
	private static final String WEAPON_PART_PATH_PREFIX = "generator_data/weapon/part/";
	private static final String WEAPON_ASSEMBLY_PATH_PREFIX = "generator_data/weapon/weapon_assembly/";

	public static JsonObject normalizeWeaponPartDefinitionJson(JsonObject rawPartJson) {
		JsonElement wrappedPart = rawPartJson.get("part");
		if (wrappedPart != null && wrappedPart.isJsonObject()) {
			return wrappedPart.getAsJsonObject().deepCopy();
		}
		return rawPartJson.deepCopy();
	}

	public static JsonObject resolveWeaponAssemblyPartReferences(JsonObject rawAssemblyJson, String assemblyNamespace,
			Map<String, JsonObject> partJsonMap) {
		JsonObject resolvedAssemblyJson = rawAssemblyJson.deepCopy();
		JsonArray parts = resolvedAssemblyJson.getAsJsonArray("parts");
		if (parts == null) {
			return resolvedAssemblyJson;
		}

		for (JsonElement partEntryElement : parts) {
			if (!partEntryElement.isJsonObject()) {
				continue;
			}

			JsonObject partEntryJson = partEntryElement.getAsJsonObject();
			JsonElement partElement = partEntryJson.get("part");
			if (partElement != null && partElement.isJsonPrimitive() && partElement.getAsJsonPrimitive().isString()) {
				String qualifiedPartName = qualifyWeaponPartReference(assemblyNamespace, partElement.getAsString());
				JsonObject referencedPartJson = partJsonMap.get(qualifiedPartName);
				if (referencedPartJson == null) {
					throw new IllegalArgumentException("Unknown weapon part reference: " + qualifiedPartName);
				}
				partEntryJson.add("part", referencedPartJson.deepCopy());
			}

			JsonElement transformElement = partEntryJson.get("transform");
			if (transformElement != null && transformElement.isJsonObject()) {
				partEntryJson.add("transform", normalizeWeaponPartTransformJson(transformElement.getAsJsonObject()));
			}
		}

		return resolvedAssemblyJson;
	}

	private static JsonObject normalizeWeaponPartTransformJson(JsonObject rawTransformJson) {
		JsonObject normalizedTransformJson = rawTransformJson.deepCopy();
		if (normalizedTransformJson.has("x") || normalizedTransformJson.has("y") || normalizedTransformJson.has("z")) {
			JsonObject convertedTransformJson = new JsonObject();
			copyNormalizedAxisMap(normalizedTransformJson, convertedTransformJson, "x", "xMap");
			copyNormalizedAxisMap(normalizedTransformJson, convertedTransformJson, "y", "yMap");
			copyNormalizedAxisMap(normalizedTransformJson, convertedTransformJson, "z", "zMap");
			normalizedTransformJson = convertedTransformJson;
		}

		normalizeAxisMapField(normalizedTransformJson, "xMap");
		normalizeAxisMapField(normalizedTransformJson, "yMap");
		normalizeAxisMapField(normalizedTransformJson, "zMap");
		return normalizedTransformJson;
	}

	private static void copyNormalizedAxisMap(JsonObject sourceTransformJson, JsonObject targetTransformJson,
			String sourceKey, String targetKey) {
		JsonElement axisMapElement = sourceTransformJson.get(sourceKey);
		if (axisMapElement != null && axisMapElement.isJsonObject()) {
			targetTransformJson.add(targetKey, normalizeAxisMapJson(axisMapElement.getAsJsonObject()));
		}
	}

	private static void normalizeAxisMapField(JsonObject transformJson, String axisMapKey) {
		JsonElement axisMapElement = transformJson.get(axisMapKey);
		if (axisMapElement != null && axisMapElement.isJsonObject()) {
			transformJson.add(axisMapKey, normalizeAxisMapJson(axisMapElement.getAsJsonObject()));
		}
	}

	private static JsonObject normalizeAxisMapJson(JsonObject rawAxisMapJson) {
		JsonObject normalizedAxisMapJson = rawAxisMapJson.deepCopy();
		JsonElement axisElement = normalizedAxisMapJson.get("axis");
		if (axisElement != null && axisElement.isJsonPrimitive() && axisElement.getAsJsonPrimitive().isString()) {
			normalizedAxisMapJson.addProperty("localAxis", axisElement.getAsString().toUpperCase(Locale.ROOT));
			normalizedAxisMapJson.remove("axis");
		}

		JsonElement localAxisElement = normalizedAxisMapJson.get("localAxis");
		if (localAxisElement != null && localAxisElement.isJsonPrimitive() && localAxisElement.getAsJsonPrimitive().isString()) {
			normalizedAxisMapJson.addProperty("localAxis", localAxisElement.getAsString().toUpperCase(Locale.ROOT));
		}
		return normalizedAxisMapJson;
	}

	private static String qualifyWeaponPartReference(String assemblyNamespace, String partReference) {
		String resolvedPartReference = partReference.contains(":") ? partReference : assemblyNamespace + ":" + partReference;
		ResourceLocation resourceLocation = ResourceLocation.tryParse(resolvedPartReference);
		if (resourceLocation == null) {
			throw new IllegalArgumentException("Invalid weapon part reference: " + partReference);
		}
		return resourceLocation.toString();
	}

	private static Map<String, JsonObject> loadWeaponParts(ResourceManager resourceManager, @Nullable Player player) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Map<String, JsonObject> partMap = new HashMap<>();
		resourceManager
				.listResources(WEAPON_DATA_ROOT, (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
					try (var reader = resource.openAsReader()) {
						String path = rl.getPath();
						if (path.startsWith(WEAPON_PART_PATH_PREFIX)) {
							String partName = path.substring(WEAPON_PART_PATH_PREFIX.length()).replace(".json", "");
							String qualifiedPartName = rl.getNamespace() + ":" + partName;
							JsonObject normalizedPartJson = normalizeWeaponPartDefinitionJson(gson.fromJson(reader, JsonObject.class));
							DataResult<WeaponPart> info = WeaponPart.CODEC.parse(JsonOps.INSTANCE, normalizedPartJson);
							info.error().ifPresent(error -> Skada.LOGGER.error("Failed to parse weapon part {}: {}", rl, error.message()));
							info.result().ifPresent(pInfo -> {
								if (partMap.containsKey(qualifiedPartName)) {
									Skada.LOGGER.error("Duplicate weapon part name found: {}", qualifiedPartName);
								}
								partMap.put(qualifiedPartName, normalizedPartJson.deepCopy());
							});
						}
					} catch (IOException e) {
						if (player != null) {
							player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_generator_data"),
									false);
						}
					}
				});
		return partMap;
	}

	public static Map<String, WeaponAssembly> loadWeaponAssemblies(ResourceManager resourceManager, @Nullable Player player) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Map<String, WeaponAssembly> assemblyMap = new HashMap<>();
		Map<String, JsonObject> partMap = loadWeaponParts(resourceManager, player);
		resourceManager
				.listResources(WEAPON_DATA_ROOT, (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
					try (var reader = resource.openAsReader()) {
						String path = rl.getPath();
						if (path.startsWith(WEAPON_ASSEMBLY_PATH_PREFIX)) {
							String assemblyName = path.substring(WEAPON_ASSEMBLY_PATH_PREFIX.length()).replace(".json",
									"");
							JsonObject resolvedAssemblyJson = resolveWeaponAssemblyPartReferences(
									gson.fromJson(reader, JsonObject.class),
									rl.getNamespace(),
									partMap);
							DataResult<WeaponAssembly> info = WeaponAssembly.CODEC.parse(JsonOps.INSTANCE, resolvedAssemblyJson);
							info.error().ifPresent(error -> Skada.LOGGER.error("Failed to parse weapon assembly {}: {}", rl, error.message()));
							info.result().ifPresent(pInfo -> {
								if (assemblyMap.containsKey(assemblyName)) {
									Skada.LOGGER.error("Duplicate weapon assembly name found: {}", assemblyName);
								}
								assemblyMap.put(assemblyName, pInfo);
							});
						}
					} catch (IOException | IllegalArgumentException e) {
						Skada.LOGGER.error("Failed to read weapon assembly resource {}", resource, e);
						if (player != null) {
							player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_generator_data"),
									false);
						}
					}
				});
		return assemblyMap;
	}

	public static Map<String, MaterialInfo> loadMaterialInfo(ResourceManager resourceManager, @Nullable Player player) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Map<String, MaterialInfo> materialMap = new HashMap<>();
		resourceManager
					.listResources("generator_data/material", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
					try (var reader = resource.openAsReader()) {
						String path = rl.getPath();
						if (path.startsWith("generator_data/material/")) {
							String tierName = path.substring("generator_data/material/".length()).replace(".json", "");
							DataResult<MaterialInfo> info = MaterialInfo.CODEC.parse(JsonOps.INSTANCE,
									gson.fromJson(reader, JsonObject.class));
							info.result().ifPresent(tInfo -> {
								if (materialMap.containsKey(tierName)) {
									Skada.LOGGER.error("Duplicate material name found: {}", tierName);
								}
								materialMap.put(tierName, tInfo);
							});
						}
					} catch (IOException e) {
						if (player != null) {
							player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_generator_data"),
									false);
						}
					}
				});
		return materialMap;
	}

	public static Map<String, ArmourPieceInfo> loadArmourPieceInfo(ResourceManager resourceManager, ServerPlayer player) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Map<String, ArmourPieceInfo> pieceMap = new HashMap<>();
		resourceManager.listResources("generator_data/armour", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
			try (var reader = resource.openAsReader()) {
				String path = rl.getPath();
				if (path.equals("generator_data/armour/piece.json")) {
					DataResult<Map<String, ArmourPieceInfo>> info = ArmourPieceInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE,
							gson.fromJson(reader, JsonObject.class));
					info.result().ifPresent(pieceMap::putAll);
				}
			} catch (IOException e) {
				player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_generator_data"), false);
			}
		});
		return pieceMap;
	}

	public static Map<String, ArmourConstructionInfo> loadArmourConstructionInfo(ResourceManager resourceManager, ServerPlayer player) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Map<String, ArmourConstructionInfo> constructionMap = new HashMap<>();
		resourceManager.listResources("generator_data/armour/type", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
			try (var reader = resource.openAsReader()) {
				String path = rl.getPath();
				if (path.startsWith("generator_data/armour/type/")) {
					String name = path.substring("generator_data/armour/type/".length()).replace(".json", "");
					DataResult<ArmourConstructionInfo> info = ArmourConstructionInfo.CODEC.parse(JsonOps.INSTANCE,
							gson.fromJson(reader, JsonObject.class));
					info.result().ifPresent(parsed -> constructionMap.put(name, parsed));
				}
			} catch (IOException e) {
				player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_generator_data"), false);
			}
		});
		return constructionMap;
	}

}