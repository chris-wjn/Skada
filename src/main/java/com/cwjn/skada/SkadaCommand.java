package com.cwjn.skada;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.gen.ArmourMaterialInfo;
import com.cwjn.skada.data.gen.ArmourPieceInfo;
import com.cwjn.skada.data.gen.ExtraTierInfo;
import com.cwjn.skada.data.gen.WeaponProfile;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.mob.MobData;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.Util;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import java.util.regex.Pattern;

import static com.cwjn.skada.Skada.LOGGER;
import static com.cwjn.skada.data.SkadaData.DEBUG_ENABLED;
import static net.minecraft.commands.Commands.literal;

public class SkadaCommand {

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
            )
            .then(literal("generate")
                    .then(literal("weapons")
                            .then(Commands.argument("namespace", ModIdArgument.modIdArgument())
                                    .executes(stack -> generateWeaponInfoForNamespace(stack.getSource(), stack.getArgument("namespace", String.class)))
                            )
                            .then(literal("all")
                                    .executes(stack -> generateWeaponInfoForAllNamespaces(stack.getSource()))
                            )
                    )
                    .then(literal("armour")
                            .then(Commands.argument("namespace", ModIdArgument.modIdArgument())
                                    .executes(stack -> generateArmourInfoForNamespace(stack.getSource(), stack.getArgument("namespace", String.class)))
                            )
                            .then(literal("all")
                                    .executes(stack -> generateArmourInfoForAllNamespaces(stack.getSource()))
                            )
                    )
                    .then(literal("mobs")
                            .then(Commands.argument("namespace", ModIdArgument.modIdArgument())
                                    .executes(stack -> generateMobInfoForNamespace(stack.getSource(), stack.getArgument("namespace", String.class)))
                            )
                            .then(literal("all")
                                    .executes(stack -> generateMobInfoForAllNamespaces(stack.getSource()))
                            )
                    )
            )
            .then(literal("debug").executes(stack -> toggleDebug(stack.getSource())))
    );
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
    WeaponInfo info = Util.getWeaponInfo(player);
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
    HashMap<String, ExtraTierInfo> tierMap = new HashMap<>();
    HashMap<String, WeaponProfile> profileMap = new HashMap<>();
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    source.getServer().getResourceManager().listResources("generator_data/weapon", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
      try (var reader = resource.openAsReader()) {
        String path = rl.getPath();
        if (path.startsWith("generator_data/weapon/weapon_profile/")) {
          String profileName = path.substring("generator_data/weapon/weapon_profile/".length()).replace(".json", "");
          DataResult<WeaponProfile> info = WeaponProfile.CODEC.parse(JsonOps.INSTANCE, gson.fromJson(reader, JsonObject.class));
          info.result().ifPresent(pInfo -> {
            if (profileMap.containsKey(profileName)) {
              LOGGER.error("Duplicate weapon profile name found: {}", profileName);
            }
            profileMap.put(profileName, pInfo);
          });
        } else if (path.startsWith("generator_data/weapon/tier/")) {
          String tierName = path.substring("generator_data/weapon/tier/".length()).replace(".json", "");
          DataResult<ExtraTierInfo> info = ExtraTierInfo.CODEC.parse(JsonOps.INSTANCE, gson.fromJson(reader, JsonObject.class));
          info.result().ifPresent(tInfo -> {
            if (tierMap.containsKey(tierName)) {
              LOGGER.error("Duplicate tier name found: {}", tierName);
            }
            tierMap.put(tierName, tInfo);
          });
        }
      } catch (IOException e) {
        player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_generator_data"), false);
      }
    });

    for (Item item : ForgeRegistries.ITEMS.getValues()) {
      if (!item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.MAINHAND).isEmpty() ||
              !item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.OFFHAND).isEmpty() ||
              item instanceof ProjectileWeaponItem) {
        if (Util.getItemNamespace(item).equals(namespace)) {
          boolean ignoreAttributes = item instanceof ProjectileWeaponItem;
          String path = Util.getItemPath(item);
          WeaponInfo info = null;
          WeaponProfile profile = new WeaponProfile();
//          for (String s : profileMap.keySet()) {
//            if (Pattern.compile("\\b" + s + "\\b", Pattern.CASE_INSENSITIVE).matcher(path.replace('_', ' ')).find()) {
//              profile = profileMap.get(s);
//              break;
//            }
//          }
          profile = profileMap.get(Util.findClosestMatch(profileMap.keySet().stream().toList(), path));
          if (item instanceof TieredItem tItem) {
            String matName = tItem.getTier().toString().toLowerCase();
            //first we'll check the item's namespace for tiers, then any namespace
            for (String s : tierMap.keySet().stream().filter(s -> s.startsWith(namespace)).toList()) {
              if (s.equals(namespace + "." + matName)) {
                info = WeaponInfo.generate(tierMap.get(s), profile, ignoreAttributes);
                break;
              }
            }
            if (info == null) { //this checks if we didn't find a match in the previous loop
              for (String s : tierMap.keySet()) {
                if (s.contains(matName)) {
                  info = WeaponInfo.generate(tierMap.get(s), profile, ignoreAttributes);
                  break;
                }
              }
              if (info == null) {
                LOGGER.error("No tier info found for {}. Generating on name only.", path);
                info = WeaponInfo.generate(profile, ignoreAttributes);
              }
            }
          } else {
            if (info == null) {
              LOGGER.error("No tier info found for {}. Generating on name only.", path);
              info = WeaponInfo.generate(profile, ignoreAttributes);
            }
          }
          map.put(path, info);
        }
      }
    }
    if (map.isEmpty()) {
      player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_items", namespace), false);
      return 0;
    }
    player.displayClientMessage(Component.translatable("skada.generate_weapon_info.found_items", map.size()), false);
    Path path = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "weapons", "generated");
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

  private int generateArmourInfoForNamespace(@NotNull CommandSourceStack source, String namespace) {
    ServerPlayer player = source.getPlayer();
    if (player == null) {
      return 0;
    }
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    player.displayClientMessage(Component.translatable("skada.generate_armour_info.start", namespace), false);
    Map<String, ArmourPieceInfo> armourPieceNameMap = new HashMap<>();
    Map<String, ArmourMaterialInfo> armourMaterialInfoMap = new HashMap<>();
    source.getServer().getResourceManager().listResources("generator_data/armour", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
      try (var reader = resource.openAsReader()) {
        String path = rl.getPath();
        if (path.equals("generator_data/armour/by_item_name.json")) {
          DataResult<Map<String, ArmourPieceInfo>> namedInfo = ArmourPieceInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, gson.fromJson(reader, JsonObject.class));
          namedInfo.result().ifPresent(armourPieceNameMap::putAll);
        } else if (path.startsWith("generator_data/armour/material/")) {
          String materialName = path.substring("generator_data/armour/material/".length()).replace(".json", "");
          System.out.println(materialName);
          DataResult<ArmourMaterialInfo> info = ArmourMaterialInfo.CODEC.parse(JsonOps.INSTANCE, gson.fromJson(reader, JsonObject.class));
          info.result().ifPresent(mInfo -> {
            if (armourMaterialInfoMap.containsKey(materialName)) {
              LOGGER.error("Duplicate material name found: {}", materialName);
            }
            armourMaterialInfoMap.put(materialName, mInfo);
          });
        }
      } catch (IOException e) {
        player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_generator_data"), false);
      }
    });
    TreeMap<String, ArmourInfo> map = new TreeMap<>();
    for (Item item : ForgeRegistries.ITEMS.getValues()) {
      if (!item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.HEAD).isEmpty() ||
              !item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.CHEST).isEmpty() ||
              !item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.LEGS).isEmpty() ||
              !item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.FEET).isEmpty()) {
        if (Util.getItemNamespace(item).equals(namespace)) {
          ArmourPieceInfo nInfo = ArmourPieceInfo.DEFAULT;
          String path = Util.getItemPath(item);
          for (String s : armourPieceNameMap.keySet()) {
            if (Pattern.compile("\\b" + s + "\\b", Pattern.CASE_INSENSITIVE).matcher(path.replace('_', ' ')).find()) {
              nInfo = armourPieceNameMap.get(s);
              break;
            }
          }
          ArmourMaterialInfo mInfo = ArmourMaterialInfo.DEFAULT;
          if (item instanceof ArmorItem aItem) {
            String matName = aItem.getMaterial().toString().toLowerCase();
            //first we'll check the item's namespace for materials, then any namespace
            for (String s : armourMaterialInfoMap.keySet().stream().filter(s -> s.startsWith(namespace)).toList()) {
              System.out.println("check if " + s + " matches " + namespace + "." + matName);
              if (s.equals(namespace + "." + matName)) {
                mInfo = armourMaterialInfoMap.get(s);
                break;
              }
            }
            if (mInfo == ArmourMaterialInfo.DEFAULT) { //this checks if we didn't find a match in the previous loop
              for (String s : armourMaterialInfoMap.keySet()) {
                if (s.contains(matName)) {
                  mInfo = armourMaterialInfoMap.get(s);
                  break;
                }
              }
            }
          }
          if (mInfo == ArmourMaterialInfo.DEFAULT) {
            LOGGER.error("No armour material info for " + path);
          }
          map.put(path, ArmourInfo.generate(nInfo, mInfo));
        }
      }
    }
    if (map.isEmpty()) {
      player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_items", namespace), false);
      return 0;
    }
    player.displayClientMessage(Component.translatable("skada.generate_weapon_info.found_items", map.size()), false);
    Path path = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "armour", "generated");
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
          map.put(Util.getEntityPath(type), new MobData(null, AttackType.strike(), multimap));
        }
      }
    }
    if (map.isEmpty()) {
      player.displayClientMessage(Component.translatable("skada.generate_mob_info.error.no_mobs", namespace), false);
      return 0;
    }
    player.displayClientMessage(Component.translatable("skada.generate_mob_info.found_mobs", map.size()), false);
    Path path = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "mobs", "generated");
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

}
