package com.cwjn.skada;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.generation.GenJsonLoader;
import com.cwjn.skada.generation.armour.ArmourConstructionInfo;
import com.cwjn.skada.generation.armour.ArmourItemMapping;
import com.cwjn.skada.generation.armour.ArmourPieceInfo;
import com.cwjn.skada.generation.attack.ElementSpread;
import com.cwjn.skada.generation.weapon.MaterialInfo;
import com.cwjn.skada.generation.weapon.WeaponAssembly;
import com.cwjn.skada.generation.weapon.WeaponGenerator;
import com.cwjn.skada.generation.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.generation.weapon.util.PhysicsUtil;
import com.cwjn.skada.generation.weapon.util.WeaponAxis;
import com.cwjn.skada.data.damage.ManualWeaponInfos;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.mob.BestiaryEntryView;
import com.cwjn.skada.data.mob.BestiaryUnlockSavedData;
import com.cwjn.skada.data.mob.MobData;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.network.SkadaNetwork;
import com.cwjn.skada.network.server_to_client.S2CSyncBestiaryUnlocks;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.command.ModIdArgument;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.cwjn.skada.Skada.LOGGER;
import static com.cwjn.skada.data.SkadaData.DEBUG_ENABLED;
import static net.minecraft.commands.Commands.literal;

public class SkadaCommand {

  private static final String GENERATION_TAG_NAMESPACE = "skada";
  private static final String ARMOUR_PIECE_TAG_PATH = "generator/armour/piece/";
  private static final String ARMOUR_MATERIAL_TAG_PATH = "generator/armour/material/";
  private static final String ARMOUR_CONSTRUCTION_TAG_PATH = "generator/armour/construction/";

  public SkadaCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx) {
    dispatcher.register(literal("skada").requires(source -> source.hasPermission(2))
            .then(literal("get")
                    .then(literal("weaponInfo")
                            .executes(stack -> getWeaponInfo(stack.getSource()))
                    )
                    .then(literal("mobInfo")
                            .then(Commands.argument("entity", ResourceArgument.resource(ctx, Registries.ENTITY_TYPE)).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                    .executes(stack -> displayMobInfo(stack.getSource(), ResourceArgument.getEntityType(stack, "entity")))
                            )
                    )
                    .then(literal("materialName")
                            .executes(stack -> getTieredItemOrArmourItemMaterialName(stack.getSource()))
                    )
                    .then(literal("physics")
                          .executes(stack -> printWeaponPhysics(stack.getSource()))
            ))
            .then(literal("generate")
                    .then(literal("weapons")
                            .then(Commands.literal("item"))
                                  .then(Commands.argument("item", ResourceLocationArgument.id())
                                    .executes(stack -> generateWeaponInfoForItem(stack.getSource(), ResourceLocationArgument.getId(stack, "item")))
                            )
                            .then(Commands.argument("namespace", ModIdArgument.modIdArgument())
                                    .executes(stack -> generateWeaponInfoForNamespace(stack.getSource(), stack.getArgument("namespace", String.class)))
                            )
                            .then(literal("all")
                                    .executes(stack -> generateWeaponInfoForAllNamespaces(stack.getSource()))
                            )
                    )
                    .then(literal("armour")
                            .then(Commands.literal("item"))
                                  .then(Commands.argument("item", ResourceLocationArgument.id())
                                    .executes(stack -> generateArmourInfoForItem(stack.getSource(), ResourceLocationArgument.getId(stack, "item")))
                            )
                            .then(Commands.argument("namespace", ModIdArgument.modIdArgument())
                                    .executes(stack -> generateArmourInfoForNamespace(stack.getSource(), stack.getArgument("namespace", String.class)))
                            )
                            .then(literal("all")
                                    .executes(stack -> generateArmourInfoForAllNamespaces(stack.getSource()))
                            )
                    )
                    .then(literal("mobs")
                            .then(literal("entity")
                                    .then(Commands.argument("entity", ResourceLocationArgument.id())
                                            .executes(stack -> generateMobInfoForEntity(stack.getSource(), ResourceLocationArgument.getId(stack, "entity")))
                                    )
                            )
                            .then(Commands.argument("namespace", ModIdArgument.modIdArgument())
                                    .executes(stack -> generateMobInfoForNamespace(stack.getSource(), stack.getArgument("namespace", String.class)))
                            )
                            .then(literal("all")
                                    .executes(stack -> generateMobInfoForAllNamespaces(stack.getSource()))
                            )
                    )
            )
                                  .then(literal("bestiary")
                                    .then(literal("unlockAll")
                                      .executes(stack -> unlockAllBestiaryEntriesForSelf(stack.getSource()))
                                      .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(stack -> unlockAllBestiaryEntries(stack.getSource(),
                                          EntityArgument.getPlayers(stack, "targets"))))
                                    )
                                  )
            .then(literal("debug").executes(stack -> toggleDebug(stack.getSource())))
            //.then(literal("test").executes(stack -> testCommand(stack.getSource())))
    );
  }

  // private int testCommand(CommandSourceStack source) {
  //   try {
  //   } catch (IOException e) {
  //     e.printStackTrace();
  //   }
  //   return 1;
  // }

  /**
   * Print the volume, mass, point of balance, CoM, and inertia of the held weapon. Or, print an error message if the held
   * item is not a weapon.
   * @param source the command source
   * @return 1 if the command executed successfully, 0 if the player is null or not holding a weapon
   */
  private int printWeaponPhysics(CommandSourceStack source) {
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      return 0;
    }
    Map<String, WeaponAssembly> profileMap = new HashMap<>(GenJsonLoader.loadWeaponAssemblies(source.getServer().getResourceManager(), player));
    Map<String, MaterialInfo> tierMap = new HashMap<>(GenJsonLoader.loadMaterialInfo(source.getServer().getResourceManager(), player));
    
    Item held = player.getMainHandItem().getItem();
    LOGGER.debug("Held item: {}", held);
    WeaponGenerator generator = new WeaponGenerator(tierMap, profileMap, Map.of(), false, 1.0);
    WeaponGenerator.ItemInput input = WeaponGenerator.ItemInput.fromItem(held);
    WeaponAssembly profile = generator.resolveAssembly(input);
    LOGGER.debug("Using profile: {}", profile);
    if (profile == null) {
      player.displayClientMessage(Component.translatable("skada.command_get_physics.error.not_tiered"), false);
      return 0;
    }
    if (held instanceof TieredItem) {
      MaterialInfo tier = generator.resolveMaterial(input);
      if (tier == null) {
        player.displayClientMessage(Component.translatable("skada.command_get_physics.error.not_tiered"), false);
        return 0;
      }
      profile = profile.withMaterialWoodenHandle(tier);
      LOGGER.debug("Using tier: {}", tier);
      double volume = profile.volume(WeaponAssembly.LARGE_SAMPLE_SIZE);
      LOGGER.debug("volume: {}", volume);
      double mass = profile.mass(WeaponAssembly.LARGE_SAMPLE_SIZE);
      LOGGER.debug("mass: {}", mass);
      double PoB = profile.pointOfBalance(WeaponAssembly.LARGE_SAMPLE_SIZE);
      LOGGER.debug("PoB: {}", PoB);
      Vec3 CoM = profile.centerOfMass(WeaponAssembly.LARGE_SAMPLE_SIZE);
      LOGGER.debug("CoM: {}", CoM);
      double inertia = PhysicsUtil.toKgM2(profile.momentOfInertiaAboutBase(WeaponAxis.Z, WeaponAssembly.LARGE_SAMPLE_SIZE));
      LOGGER.debug("inertia: {}", inertia);
      player.displayClientMessage(Component.translatable("skada.command_get_physics", volume, mass, PoB, CoM, inertia), false);
    }
    else {
      player.displayClientMessage(Component.translatable("skada.command_get_physics.error.not_tiered"), false);
    }
    return 1;
  }

  private int toggleDebug(CommandSourceStack source) {
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      return 0;
    }
    DEBUG_ENABLED = !DEBUG_ENABLED;
    player.sendSystemMessage(Component.translatable("skada.command_debug.toggle", DEBUG_ENABLED ? "enabled" : "disabled"));
    return 1;
  }

  private int unlockAllBestiaryEntriesForSelf(CommandSourceStack source) throws CommandSyntaxException {
    return unlockAllBestiaryEntries(source, List.of(source.getPlayerOrException()));
  }

  private int unlockAllBestiaryEntries(CommandSourceStack source, Collection<ServerPlayer> targets) {
    Set<ResourceLocation> allEntryIds = SkadaData.BESTIARY_ENTRY_VIEWS.values().stream()
        .map(BestiaryEntryView::entityId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (allEntryIds.isEmpty()) {
      source.sendFailure(Component.translatable("skada.command_bestiary.unlock_all.error.no_entries"));
      return 0;
    }

    int newlyUnlockedCount = 0;
    for (ServerPlayer target : targets) {
      BestiaryUnlockSavedData unlockData = BestiaryUnlockSavedData.get(target.serverLevel());
      Set<ResourceLocation> newlyUnlockedEntries = unlockData.unlockAll(target.getUUID(), allEntryIds);
      newlyUnlockedCount += newlyUnlockedEntries.size();
      SkadaNetwork.serverToPlayer(
          new S2CSyncBestiaryUnlocks(unlockData.unlockedEntries(target.getUUID()), newlyUnlockedEntries),
          target
      );
    }

    int unlockedEntriesTotal = newlyUnlockedCount;
    int targetCount = targets.size();
    int trackedEntryCount = allEntryIds.size();
    source.sendSuccess(() -> Component.translatable("skada.command_bestiary.unlock_all.success",
        unlockedEntriesTotal, targetCount, trackedEntryCount), true);
    return 1;
  }

  private int getTieredItemOrArmourItemMaterialName(CommandSourceStack source) {
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      return 0;
    }
    ItemStack stack = player.getMainHandItem();
    if (stack.isEmpty()) {
      player.displayClientMessage(Component.translatable("skada.command_get_material_name.error.no_item"), false);
    } else if (stack.getItem() instanceof TieredItem) {
      String name = ((TieredItem) stack.getItem()).getTier().toString().toLowerCase();
      player.displayClientMessage(Component.translatable("skada.command_get_material_name.result", name), false);
    } else if (stack.getItem() instanceof ArmorItem) {
      String name = ((ArmorItem) stack.getItem()).getMaterial().toString().toLowerCase();
      player.displayClientMessage(Component.translatable("skada.command_get_material_name.result", name), false);
    } else {
      player.displayClientMessage(Component.translatable("skada.command_get_material_name.error.not_tiered_or_armour"), false);
    }
    return 1;
  }

  private int displayMobInfo(CommandSourceStack source, Holder.Reference<EntityType<?>> entity) {
    ServerPlayer player = source.getPlayer();
    EntityType<?> type = entity.get();
    if (player == null) {
      return 0;
    }
    MobData data = SkadaData.MOB_DATA.get(type);
    if (data == null) {
      player.displayClientMessage(Component.translatable("skada.command_get_mobinfo.error.no_info", type.getDescriptionId()), false);
      return 0;
    }
    StringBuilder attributes = new StringBuilder("Attributes for " + type.getDescriptionId() + ":\n");
    attributes.append("attackType: ")
      .append(data.damageInfo().attackType().name())
      .append("\nlethality: ")
      .append(data.damageInfo().lethality())
      .append("\nprecision: ")
      .append(data.damageInfo().precision())
      .append("\n");
    for (Map.Entry<Attribute, AttributeModifier> entry : data.extraModifiers().entries()) {
      attributes.append(entry.getKey().getDescriptionId())
              .append(": ")
              .append(entry.getValue().getAmount())
              .append(" (")
              .append(entry.getValue().getOperation())
              .append(")\n");
    }
    player.displayClientMessage(Component.literal(attributes.toString()), false);
    return 1;
  }

  private int getWeaponInfo(CommandSourceStack source) {
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      return 0;
    }
    WeaponInfo info = UtilData.getWeaponInfo(player);
    if (info != null) {
      player.displayClientMessage(info.toTextComponent().get(), false);
    } else {
      player.displayClientMessage(Component.translatable("skada.command_get_weaponinfo.error.no_info"), false);
    }
    return 1;
  }

  private int generateWeaponInfoForAllNamespaces(CommandSourceStack stack) {
    FMLLoader.getLoadingModList().getMods().forEach(mod -> generateWeaponInfoForNamespace(stack, mod.getModId()));
    return 1;
  }

  private int generateArmourInfoForAllNamespaces(CommandSourceStack stack) {
    FMLLoader.getLoadingModList().getMods().forEach(mod -> generateArmourInfoForNamespace(stack, mod.getModId()));
    return 1;
  }

  private int generateMobInfoForAllNamespaces(CommandSourceStack stack) {
    FMLLoader.getLoadingModList().getMods().forEach(mod -> generateMobInfoForNamespace(stack, mod.getModId()));
    return 1;
  }

  private int generateWeaponInfoForNamespace(CommandSourceStack source, String namespace) {
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      return 0;
    }
    player.displayClientMessage(Component.translatable("skada.generate_weapon_info.start", namespace), false);
    TreeMap<String, WeaponInfo> map = new TreeMap<>();
    HashMap<String, MaterialInfo> tierMap = new HashMap<>(GenJsonLoader.loadMaterialInfo(source.getServer().getResourceManager(), player));
    HashMap<String, WeaponAssembly> profileMap = new HashMap<>(GenJsonLoader.loadWeaponAssemblies(source.getServer().getResourceManager(), player));
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    WeaponGenerator.Result result = generateWeaponInfos(namespace, tierMap, profileMap, null);
    result.diagnostics().forEach((id, message) -> LOGGER.warn("{}: {}", id, message));
    map.putAll(result.weapons());
    if (map.isEmpty()) {
      player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_items", namespace), false);
      return 0;
    }
    player.displayClientMessage(Component.translatable("skada.generate_weapon_info.found_items", map.size()), false);
    Path path = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "weapons");
    WeaponInfo.STRING_MAP_CODEC.encodeStart(JsonOps.INSTANCE, map).result().ifPresent(jsonElement -> {
      String json = gson.toJson(jsonElement);
      try {
        FileUtils.write(new File(path.toFile(), namespace + ".json"), json);
      } catch (IOException e) {
        player.displayClientMessage(Component.translatable("skada.generate_weapon_info.io_error"), false);
      }
    });
    player.displayClientMessage(Component.translatable("skada.generate_weapon_info.finish", map.size()), false);
    return 1;
  }

  private int generateWeaponInfoForItem(CommandSourceStack source, ResourceLocation itemId) {
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      return 0;
    }
    Item item = ForgeRegistries.ITEMS.getValue(itemId);
    if (item == null || item == Items.AIR) {
      player.displayClientMessage(Component.literal("No item found for id: " + itemId), false);
      return 0;
    }
    String namespace = itemId.getNamespace();
    player.displayClientMessage(Component.translatable("skada.generate_weapon_info.start", itemId), false);

    HashMap<String, MaterialInfo> tierMap = new HashMap<>(GenJsonLoader.loadMaterialInfo(source.getServer().getResourceManager(), player));
    HashMap<String, WeaponAssembly> profileMap = new HashMap<>(GenJsonLoader.loadWeaponAssemblies(source.getServer().getResourceManager(), player));
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    String path = Util.getItemPath(item);
    TreeMap<String, WeaponInfo> map = new TreeMap<>();
    Path outputPath = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "weapons", namespace + ".json");
    if (outputPath.toFile().exists()) {
      try (Reader reader = new FileReader(outputPath.toFile())) {
        JsonObject existingObj = gson.fromJson(reader, JsonObject.class);
        if (existingObj != null) {
          DataResult<Map<String, WeaponInfo>> existingInfo = WeaponInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, existingObj);
          existingInfo.result().ifPresent(map::putAll);
        }
      } catch (IOException e) {
        player.displayClientMessage(Component.translatable("skada.generate_weapon_info.io_error"), false);
      }
    }

    WeaponGenerator.Result result = generateWeaponInfos(namespace, tierMap, profileMap, path);
    result.diagnostics().forEach((id, message) -> LOGGER.warn("{}: {}", id, message));
    if (result.weapons().isEmpty()) {
      player.displayClientMessage(Component.literal(result.diagnostics().getOrDefault(itemId,
          "No weapon info generated") + ": " + itemId), false);
      return 0;
    }
    map.putAll(result.weapons());
    WeaponInfo.STRING_MAP_CODEC.encodeStart(JsonOps.INSTANCE, map).result().ifPresent(jsonElement -> {
      String json = gson.toJson(jsonElement);
      try {
        FileUtils.write(outputPath.toFile(), json);
      } catch (IOException e) {
        player.displayClientMessage(Component.translatable("skada.generate_weapon_info.io_error"), false);
      }
    });
    player.displayClientMessage(Component.translatable("skada.generate_weapon_info.finish", 1), false);
    return 1;
  }

  /** Supplies loaded definitions, current item facts and settings to the weapon generation module. */
  private WeaponGenerator.Result generateWeaponInfos(String namespace, Map<String, MaterialInfo> materials,
      Map<String, WeaponAssembly> assemblies, String onlyPath) {
    List<WeaponGenerator.ItemInput> items = ForgeRegistries.ITEMS.getValues().stream()
        .filter(item -> Util.getItemNamespace(item).equals(namespace))
        .map(WeaponGenerator.ItemInput::fromItem)
        .toList();
    return new WeaponGenerator(materials, assemblies, ManualWeaponInfos.byNamespace(namespace),
        CommonConfig.SQUEEZE_DAMAGE_VALUES.get(), CommonConfig.DAMAGE_DIFFERENCE_AMOUNT.get())
        .generate(namespace, items, onlyPath);
  }

  private int generateArmourInfoForNamespace(@NotNull CommandSourceStack source, String namespace) {
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      return 0;
    }
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    player.displayClientMessage(Component.translatable("skada.generate_armour_info.start", namespace), false);
    ResourceManager resourceManager = source.getServer().getResourceManager();
    Map<String, ArmourPieceInfo> armourPieceNameMap = GenJsonLoader.loadArmourPieceInfo(resourceManager, player);
    Map<String, ArmourConstructionInfo> constructionInfoMap = GenJsonLoader.loadArmourConstructionInfo(resourceManager, player);
    Map<String, MaterialInfo> sharedMaterials = GenJsonLoader.loadMaterialInfo(resourceManager, player);
    TreeMap<String, ArmourInfo> map = new TreeMap<>();
    for (Item item : ForgeRegistries.ITEMS.getValues()) {
      if (!item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.HEAD).isEmpty() ||
              !item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.CHEST).isEmpty() ||
              !item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.LEGS).isEmpty() ||
              !item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.FEET).isEmpty()) {
        if (Util.getItemNamespace(item).equals(namespace)) {
          String path = Util.getItemPath(item);
          ArmourItemMapping mapping = resolveArmourItemMapping(item, armourPieceNameMap, constructionInfoMap, sharedMaterials);
          if (mapping == null) {
            LOGGER.error("No armour generation tags or fallback mapping found for {}", path);
            continue;
          }
          ArmourPieceInfo pieceInfo = mapping.resolvePiece(armourPieceNameMap);
          MaterialInfo materialInfo = resolveSharedMaterial(sharedMaterials, namespace, mapping.material(), item);
          ArmourConstructionInfo constructionInfo = resolveArmourConstruction(constructionInfoMap, namespace, mapping.construction());
          double baseArmour = item.getDefaultAttributeModifiers(LivingEntity.getEquipmentSlotForItem(item.getDefaultInstance())).get(Attributes.ARMOR).stream()
          .filter(m -> m.getOperation().equals(Operation.ADDITION)).mapToDouble(m ->m.getAmount()).sum();
          double baseToughness = item.getDefaultAttributeModifiers(LivingEntity.getEquipmentSlotForItem(item.getDefaultInstance())).get(Attributes.ARMOR_TOUGHNESS).stream()
          .filter(m -> m.getOperation().equals(Operation.ADDITION)).mapToDouble(m ->m.getAmount()).sum();
          if (materialInfo == null || constructionInfo == null) {
            LOGGER.error("Failed to resolve material or construction for {} using {}", path, mapping);
            continue;
          }
          map.put(path, ArmourInfo.generate(pieceInfo, materialInfo, constructionInfo, baseArmour, baseToughness));
        }
      }
    }
    if (map.isEmpty()) {
      player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_items", namespace), false);
      return 0;
    }
    player.displayClientMessage(Component.translatable("skada.generate_weapon_info.found_items", map.size()), false);
    Path path = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "armour");
    ArmourInfo.STRING_MAP_CODEC.encodeStart(JsonOps.INSTANCE, map).result().ifPresent(jsonElement -> {
      String json = gson.toJson(jsonElement);
      try {
        FileUtils.write(new File(path.toFile(), namespace + ".json"), json);
      } catch (IOException e) {
        player.displayClientMessage(Component.translatable("skada.generate_weapon_info.io_error"), false);
      }
    });
    player.displayClientMessage(Component.translatable("skada.generate_weapon_info.finish", map.size()), false);
    return 1;
  }

  private int generateArmourInfoForItem(CommandSourceStack source, ResourceLocation itemId) {
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      return 0;
    }

    Item item = ForgeRegistries.ITEMS.getValue(itemId);
    if (item == null || item == Items.AIR) {
      player.displayClientMessage(Component.literal("No item found for id: " + itemId), false);
      return 0;
    }
    if (item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.HEAD).isEmpty() &&
            item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.CHEST).isEmpty() &&
            item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.LEGS).isEmpty() &&
            item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.FEET).isEmpty()) {
      player.displayClientMessage(Component.literal("Item is not armour: " + itemId), false);
      return 0;
    }

    String namespace = itemId.getNamespace();
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    player.displayClientMessage(Component.translatable("skada.generate_armour_info.start", itemId), false);

    ResourceManager resourceManager = source.getServer().getResourceManager();
    Map<String, ArmourPieceInfo> armourPieceNameMap = GenJsonLoader.loadArmourPieceInfo(resourceManager, player);
    Map<String, ArmourConstructionInfo> constructionInfoMap = GenJsonLoader.loadArmourConstructionInfo(resourceManager, player);
    Map<String, MaterialInfo> sharedMaterials = GenJsonLoader.loadMaterialInfo(resourceManager, player);

    String path = Util.getItemPath(item);
    ArmourItemMapping mapping = resolveArmourItemMapping(item, armourPieceNameMap, constructionInfoMap, sharedMaterials);
    if (mapping == null) {
      player.displayClientMessage(Component.literal("No armour generation tags or fallback mapping found for: " + itemId), false);
      return 0;
    }
    ArmourPieceInfo pieceInfo = mapping.resolvePiece(armourPieceNameMap);
    MaterialInfo materialInfo = resolveSharedMaterial(sharedMaterials, namespace, mapping.material(), item);
    ArmourConstructionInfo constructionInfo = resolveArmourConstruction(constructionInfoMap, namespace, mapping.construction());
    if (materialInfo == null || constructionInfo == null) {
      player.displayClientMessage(Component.literal("Failed to resolve armour material or construction for: " + itemId), false);
      return 0;
    }

    TreeMap<String, ArmourInfo> map = new TreeMap<>();
    Path outputPath = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "armour", namespace + ".json");
    if (outputPath.toFile().exists()) {
      try (Reader reader = new FileReader(outputPath.toFile())) {
        JsonObject existingObj = gson.fromJson(reader, JsonObject.class);
        if (existingObj != null) {
          DataResult<Map<String, ArmourInfo>> existingInfo = ArmourInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, existingObj);
          existingInfo.result().ifPresent(map::putAll);
        }
      } catch (IOException e) {
        player.displayClientMessage(Component.translatable("skada.generate_weapon_info.io_error"), false);
      }
    }

    double baseArmour = item
        .getDefaultAttributeModifiers(LivingEntity.getEquipmentSlotForItem(item.getDefaultInstance()))
        .get(Attributes.ARMOR).stream()
        .filter(m -> m.getOperation().equals(Operation.ADDITION)).mapToDouble(m -> m.getAmount()).sum();
    double baseToughness = item
        .getDefaultAttributeModifiers(LivingEntity.getEquipmentSlotForItem(item.getDefaultInstance()))
        .get(Attributes.ARMOR_TOUGHNESS).stream()
        .filter(m -> m.getOperation().equals(Operation.ADDITION)).mapToDouble(m -> m.getAmount()).sum();

    map.put(path, ArmourInfo.generate(pieceInfo, materialInfo, constructionInfo, baseArmour, baseToughness));
    ArmourInfo.STRING_MAP_CODEC.encodeStart(JsonOps.INSTANCE, map).result().ifPresent(jsonElement -> {
      String json = gson.toJson(jsonElement);
      try {
        FileUtils.write(outputPath.toFile(), json);
      } catch (IOException e) {
        player.displayClientMessage(Component.translatable("skada.generate_weapon_info.io_error"), false);
      }
    });
    player.displayClientMessage(Component.translatable("skada.generate_weapon_info.finish", 1), false);
    return 1;
  }

  /**
   * Resolve armour generation data from item tags and fallback heuristics.
   *
   * Piece, material, and construction can each be supplied through tags. When
   * tags are absent, the method falls back to slot inference, armor material
   * inference, and a material-family construction default.
   *
   * @param item the item being generated
   * @param pieceMap loaded armour piece definitions
   * @param constructionMap loaded armour construction definitions
   * @param materialMap loaded shared material definitions
   * @return a resolved mapping, or null if the item cannot be classified
   */
  private ArmourItemMapping resolveArmourItemMapping(Item item,
      Map<String, ArmourPieceInfo> pieceMap,
      Map<String, ArmourConstructionInfo> constructionMap,
      Map<String, MaterialInfo> materialMap) {
    String piece = resolveTaggedGenerationName(item, ARMOUR_PIECE_TAG_PATH, pieceMap.keySet());
    if (piece == null) {
      piece = resolveDefaultArmourPiece(item);
    }

    String material = resolveTaggedGenerationName(item, ARMOUR_MATERIAL_TAG_PATH, materialMap.keySet());
    if (material == null) {
      material = resolveDefaultArmourMaterial(item);
    }

    String construction = resolveTaggedGenerationName(item, ARMOUR_CONSTRUCTION_TAG_PATH, constructionMap.keySet());
    if (construction == null) {
      construction = resolveDefaultArmourConstruction(material);
    }

    if (piece == null || material == null || construction == null) {
      return null;
    }

    return new ArmourItemMapping(material, construction, piece, ArmourPieceInfo.DEFAULT);
  }

  /**
  * Resolve a generation name from an item tag under the Skada generation tag namespace.
   *
   * @param item the item to inspect
   * @param tagPath the tag path to match, for example generator/armour/piece/
   * @param availableNames the set of known names for the requested category
   * @return a matching name, or null if no matching tag is present
   */
  private String resolveTaggedGenerationName(Item item, String tagPath, Collection<String> availableNames) {
    String taggedValue = findGenerationTagValue(item, tagPath);
    if (taggedValue == null) {
      return null;
    }
    return matchGenerationName(availableNames, taggedValue);
  }

  /**
   * Find the raw value encoded in the first matching Skada generation tag on the item.
   *
   * @param item the item to inspect
     * @param tagPath the expected tag path
   * @return the encoded tag suffix, or null when the item is untagged for that category
   */
    private String findGenerationTagValue(Item item, String tagPath) {
    return ForgeRegistries.ITEMS.getHolder(item).orElseThrow().tags()
        .map(TagKey::location)
        .filter(location -> location.getNamespace().equals(GENERATION_TAG_NAMESPACE))
        .map(ResourceLocation::getPath)
      .filter(path -> path.startsWith(tagPath))
      .map(path -> path.substring(tagPath.length()))
        .sorted(Comparator.comparingInt(String::length).reversed())
        .findFirst()
        .orElse(null);
  }

  /**
  * Match a category name against a normalized candidate value.
   *
     * @param availableNames the loaded names for the category
     * @param candidateName the desired name or tag-derived value
     * @return the matching name, or null if no category value matches
   */
    private String matchGenerationName(Collection<String> availableNames, String candidateName) {
    String normalizedCandidate = normalizeGenerationName(candidateName);
    return availableNames.stream()
      .sorted(Comparator.comparingInt((String value) -> normalizeGenerationName(value).length()).reversed())
      .filter(availableName -> normalizeGenerationName(availableName).equals(normalizedCandidate)
        || normalizeGenerationName(simpleGenerationName(availableName)).equals(normalizedCandidate))
        .findFirst()
        .orElse(null);
  }

  /**
   * Resolve the armour piece name from the item's equipment slot.
   *
   * @param item the item to inspect
   * @return helmet, chestplate, leggings, boots, or null when the slot cannot be inferred
   */
  private String resolveDefaultArmourPiece(Item item) {
    EquipmentSlot slot = getArmourEquipmentSlot(item);
    if (slot == null) {
      return null;
    }
    return switch (slot) {
      case HEAD -> "helmet";
      case CHEST -> "chestplate";
      case LEGS -> "leggings";
      case FEET -> "boots";
      default -> null;
    };
  }

  /**
   * Determine the armour slot used by the item.
   *
   * @param item the item to inspect
   * @return the matching equipment slot, or null if the item does not behave like armour
   */
  private EquipmentSlot getArmourEquipmentSlot(Item item) {
    if (item instanceof ArmorItem armourItem) {
      return armourItem.getEquipmentSlot();
    }
    for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
      if (!item.getDefaultInstance().getAttributeModifiers(slot).isEmpty()) {
        return slot;
      }
    }
    return null;
  }

  /**
   * Resolve the armour material name from an ArmorItem.
   *
   * @param item the item to inspect
   * @return the lower-case armour material name, or null if the item is not ArmorItem
   */
  private String resolveDefaultArmourMaterial(Item item) {
    if (item instanceof ArmorItem armourItem) {
      return armourItem.getMaterial().toString().toLowerCase(Locale.ROOT);
    }
    return null;
  }

  /**
   * Map a material family to a default armour construction id.
   *
   * @param materialId the material identifier or family name
   * @return the default construction id for that material family
   */
  static String resolveDefaultArmourConstruction(String materialId) {
    if (materialId == null || materialId.isBlank()) {
      return null;
    }
    return switch (simpleGenerationName(materialId)) {
      case "leather" -> "gambeson";
      case "turtle" -> "scale";
      default -> "plate";
    };
  }

  /**
  * Normalize a name for tag/profile matching.
   *
   * @param value the raw name string
   * @return a lower-case alphanumeric underscore-form identifier
   */
  static String normalizeGenerationName(String value) {
    return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
  }

  /**
   * Strip the namespace from a category name when matching a simple local name.
   *
   * @param value the raw name string
   * @return the local id portion after the last dot, or the original string when no namespace separator exists
   */
  static String simpleGenerationName(String value) {
    int separatorIndex = value.lastIndexOf('.');
    return separatorIndex >= 0 ? value.substring(separatorIndex + 1) : value;
  }

  /**
   * Resolve a shared material definition using a configured material name and item namespace.
   *
   * @param materialMap loaded shared material definitions
   * @param namespace the namespace for the generated item
   * @param configuredMaterial the material id from tags or fallback inference
   * @param item the item being generated
   * @return the resolved material definition, or null if none matches
   */
  private MaterialInfo resolveSharedMaterial(Map<String, MaterialInfo> materialMap, String namespace, String configuredMaterial,
      Item item) {
    String materialName = configuredMaterial;
    if (item instanceof ArmorItem armourItem && materialName.isBlank()) {
      materialName = armourItem.getMaterial().toString().toLowerCase();
    }
    String exact = materialName.contains(".") ? materialName : namespace + "." + materialName;
    if (materialMap.containsKey(exact)) {
      return materialMap.get(exact);
    }
    for (Map.Entry<String, MaterialInfo> entry : materialMap.entrySet()) {
      if (entry.getKey().endsWith("." + materialName)) {
        return entry.getValue();
      }
    }
    return null;
  }

  /**
   * Resolve a construction definition using a configured construction id and item namespace.
   *
   * @param constructionMap loaded armour construction definitions
   * @param namespace the namespace for the generated item
   * @param configuredConstruction the construction id from tags or fallback inference
   * @return the resolved construction definition, or null if none matches
   */
  private ArmourConstructionInfo resolveArmourConstruction(Map<String, ArmourConstructionInfo> constructionMap,
      String namespace, String configuredConstruction) {
    String exact = configuredConstruction.contains(".") ? configuredConstruction : namespace + "." + configuredConstruction;
    if (constructionMap.containsKey(exact)) {
      return constructionMap.get(exact);
    }
    if (constructionMap.containsKey(configuredConstruction)) {
      return constructionMap.get(configuredConstruction);
    }
    for (Map.Entry<String, ArmourConstructionInfo> entry : constructionMap.entrySet()) {
      if (entry.getKey().endsWith("." + configuredConstruction)) {
        return entry.getValue();
      }
    }
    return null;
  }

  private int generateMobInfoForNamespace(CommandSourceStack source, String namespace) {
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      return 0;
    }
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    TreeMap<String, MobData> map = new TreeMap<>();
    player.displayClientMessage(Component.translatable("skada.generate_mob_info.start", namespace), false);
    for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
      if (type.create(player.level()) instanceof LivingEntity && !(type.create(player.level()) instanceof Projectile)) {
        if (Util.getEntityNamespace(type).equals(namespace)) {
          Multimap<Attribute, AttributeModifier> multimap = ArrayListMultimap.create();
          map.put(Util.getEntityPath(type), new MobData(null, MobData.defaultDamageInfo(AttackType.strike()), multimap));
        }
      }
    }
    if (map.isEmpty()) {
      player.displayClientMessage(Component.translatable("skada.generate_mob_info.error.no_mobs", namespace), false);
      return 0;
    }
    player.displayClientMessage(Component.translatable("skada.generate_mob_info.found_mobs", map.size()), false);
    Path path = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "mobs");
    MobData.STRING_MAP_CODEC.encodeStart(JsonOps.INSTANCE, map).result().ifPresent(jsonElement -> {
      String json = gson.toJson(jsonElement);
      try {
        FileUtils.write(new File(path.toFile(), namespace + ".json"), json);
      } catch (IOException e) {
        player.displayClientMessage(Component.translatable("skada.generate_weapon_info.io_error"), false);
      }
    });
    player.displayClientMessage(Component.translatable("skada.generate_weapon_info.finish", map.size()), false);
    return 1;
  }

  private int generateMobInfoForEntity(CommandSourceStack source, ResourceLocation entityId) {
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      return 0;
    }

    // This registry defaults unknown IDs to pig, so a null check alone cannot validate an ID.
    if (!ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
      player.displayClientMessage(Component.literal("No entity found for id: " + entityId), false);
      return 0;
    }
    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
    if (type == null) {
      player.displayClientMessage(Component.literal("No entity found for id: " + entityId), false);
      return 0;
    }
    if (!(type.create(player.level()) instanceof LivingEntity) || type.create(player.level()) instanceof Projectile) {
      player.displayClientMessage(Component.literal("Entity is not a valid mob: " + entityId), false);
      return 0;
    }

    String namespace = entityId.getNamespace();
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    TreeMap<String, MobData> map = new TreeMap<>();
    Path outputPath = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "mobs", namespace + ".json");
    if (outputPath.toFile().exists()) {
      try (Reader reader = new FileReader(outputPath.toFile())) {
        JsonObject existingObj = gson.fromJson(reader, JsonObject.class);
        if (existingObj != null) {
          DataResult<Map<String, MobData>> existingInfo = MobData.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, existingObj);
          existingInfo.result().ifPresent(map::putAll);
        }
      } catch (IOException e) {
        player.displayClientMessage(Component.translatable("skada.generate_weapon_info.io_error"), false);
      }
    }

    player.displayClientMessage(Component.translatable("skada.generate_mob_info.start", entityId), false);
    Multimap<Attribute, AttributeModifier> multimap = ArrayListMultimap.create();
    map.put(Util.getEntityPath(type), new MobData(null, MobData.defaultDamageInfo(AttackType.strike()), multimap));

    MobData.STRING_MAP_CODEC.encodeStart(JsonOps.INSTANCE, map).result().ifPresent(jsonElement -> {
      String json = gson.toJson(jsonElement);
      try {
        FileUtils.write(outputPath.toFile(), json);
      } catch (IOException e) {
        player.displayClientMessage(Component.translatable("skada.generate_weapon_info.io_error"), false);
      }
    });
    player.displayClientMessage(Component.translatable("skada.generate_weapon_info.finish", 1), false);
    return 1;
  }

}
