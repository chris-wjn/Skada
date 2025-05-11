package com.cwjn.skada;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.gen.ArmourMaterialInfo;
import com.cwjn.skada.data.gen.ArmourPieceInfo;
import com.cwjn.skada.data.gen.ExtraTierInfo;
import com.cwjn.skada.data.gen.NamedInfo;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraftforge.common.TierSortingRegistry;
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
import static net.minecraft.commands.Commands.literal;

public class SkadaCommand {

    public SkadaCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx) {
        dispatcher.register(literal("skada")
                .then(literal("get")
                        .then(literal("weaponInfo")
                                .executes(stack -> getWeaponInfo(stack.getSource()))
                        )
                        .then(literal("mobInfo")
                                .then(Commands.argument("entity", ResourceArgument.resource(ctx, Registries.ENTITY_TYPE)).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                        .executes(stack -> displayMobInfo(stack.getSource(), ResourceArgument.getEntityType(stack, "entity")))
                                )
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
        );
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
        HashMap<String, NamedInfo> namedMap = new HashMap<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        source.getServer().getResourceManager().listResources("generator_data/weapon", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
            try (var reader = resource.openAsReader()) {
                String path = rl.getPath();
                if (path.equals("generator_data/weapon/by_item_name.json")) {
                    DataResult<Map<String, NamedInfo>> namedInfo = NamedInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, gson.fromJson(reader, JsonObject.class));
                    namedInfo.result().ifPresent(namedMap::putAll);
                } else if (path.startsWith("generator_data/weapon/tier/")) {
                    String tierName = path.substring("generator_data/weapon/tier/".length()).replace(".json", "");
                    DataResult<ExtraTierInfo> info = ExtraTierInfo.CODEC.parse(JsonOps.INSTANCE, gson.fromJson(reader, JsonObject.class));
                    info.result().ifPresent(tInfo -> {
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
                    NamedInfo nInfo = new NamedInfo();
                    for (String s : namedMap.keySet()) {
                        if (Pattern.compile("\\b" + s + "\\b", Pattern.CASE_INSENSITIVE).matcher(path.replace('_', ' ')).find()) {
                            nInfo = namedMap.get(s);
                            break;
                        }
                    }
                    if (item instanceof TieredItem tItem) {
                        //first check namespace for proper tier
                        String tierName = tItem.getTier().toString().toLowerCase();
                        String nameSpace = Util.getItemNamespace(tItem);
                        if (tierMap.containsKey(nameSpace + "." + tierName)) {
                            info = WeaponInfo.generate(tierMap.get(nameSpace + "." + tierName), nInfo, ignoreAttributes);
                        }
                        //if not check, if any namespace contains the proper tier
                        else {
                            boolean found = false;
                            for (String s : tierMap.keySet()) {
                                if (s.contains(tierName)) {
                                    info = WeaponInfo.generate(tierMap.get(s), nInfo, ignoreAttributes);
                                    found = true;
                                    break;
                                }
                            }
                            //if no match found, use default
                            if (!found) info = WeaponInfo.generate(nInfo, ignoreAttributes);
                        }
                    }
                    else {
                        info = WeaponInfo.generate(nInfo, ignoreAttributes);
                    }
                    if (info == null) {
                        LOGGER.error("Failed to generate weapon info for " + path);
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
                FileUtils.write(new File(path.toFile(), namespace + ".json") ,json);
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
                    DataResult<ArmourMaterialInfo> info = ArmourMaterialInfo.CODEC.parse(JsonOps.INSTANCE, gson.fromJson(reader, JsonObject.class));
                    info.result().ifPresent(mInfo -> {
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
                        //first check namespace for proper material
                        String materialName = aItem.getMaterial().toString().toLowerCase();
                        String nameSpace = Util.getItemNamespace(aItem);
                        if (armourMaterialInfoMap.containsKey(nameSpace + "." + materialName)) {
                            mInfo = armourMaterialInfoMap.get(nameSpace + "." + materialName);
                        }
                        else {
                            //if not check, if any namespace contains the proper material
                            boolean found = false;
                            for (String s : armourMaterialInfoMap.keySet()) {
                                if (s.contains(materialName)) {
                                    mInfo = armourMaterialInfoMap.get(s);
                                    found = true;
                                    break;
                                }
                            }
                            //if no match found, use default
                            if (!found) mInfo = ArmourMaterialInfo.DEFAULT;
                        }
                    }
                    if (mInfo == ArmourMaterialInfo.DEFAULT) {
                        LOGGER.error("Failed to generate armour info for " + path);
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
                FileUtils.write(new File(path.toFile(), namespace + ".json") ,json);
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
                FileUtils.write(new File(path.toFile(), namespace + ".json") ,json);
            } catch (IOException e) {
                player.displayClientMessage(Component.translatable("skada.generate_weapon_info.io_error"), false);
            }
        });
        player.displayClientMessage(Component.translatable("skada.generate_weapon_info.finish", map.size()), false);
        return 1;
    }

}
