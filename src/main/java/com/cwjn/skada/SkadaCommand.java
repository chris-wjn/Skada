package com.cwjn.skada;

import com.cwjn.skada.data.gen.ExtraTierInfo;
import com.cwjn.skada.data.gen.NamedInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.util.AccessWeaponInfo;
import com.cwjn.skada.util.Util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.command.ModIdArgument;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static net.minecraft.commands.Commands.literal;

public class SkadaCommand {

    public SkadaCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("skada")
                .then(literal("get")
                        .then(literal("weaponInfo")
                                .executes(stack -> getWeaponInfo(stack.getSource()))
                        )
                )
                .then(literal("generate")
                        .then(Commands.argument("namespace", ModIdArgument.modIdArgument())
                                .executes(stack -> generateWeaponInfoForNamespace(stack.getSource(), stack.getArgument("namespace", String.class)))
                        )
                        .then(literal("all")
                                .executes(stack -> generateWeaponInfoForAllNamespaces(stack.getSource()))
                        )
                )
        );
    }

    private int getWeaponInfo(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        if (player.getMainHandItem().isEmpty()) {
            player.displayClientMessage(Component.translatable("skada.command_get_weaponinfo.error.no_item"), false);
            return 0;
        }
        Item item = player.getMainHandItem().getItem();
        AccessWeaponInfo mixinItem = (AccessWeaponInfo) item;
        WeaponInfo info = mixinItem.skada$getWeaponInfo();
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

    private int generateWeaponInfoForNamespace(CommandSourceStack source, String namespace) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        player.displayClientMessage(Component.translatable("skada.generate_weapon_info.start", namespace), false);
        TreeMap<String, WeaponInfo> map = new TreeMap<>();
        HashMap<Tier, ExtraTierInfo> tierMap = new HashMap<>();
        HashMap<String, NamedInfo> namedMap = new HashMap<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path generator_data_item_name = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "generator_data", "by_item_name.json");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(generator_data_item_name.toString()));
            DataResult<Map<String, NamedInfo>> namedInfo = NamedInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, gson.fromJson(reader, JsonObject.class));
            namedInfo.result().ifPresent(namedMap::putAll);
        }
        catch (FileNotFoundException e) {
            player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_generator_data"), false);
            return 0;
        }
        File generator_data_tier_info = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "generator_data", "tier_info").toFile();
        File[] listing = generator_data_tier_info.listFiles();
        if (listing == null) {
            player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_generator_data"), false);
            return 0;
        }
        for (File tier_json : listing) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(tier_json));
                DataResult<ExtraTierInfo> info = ExtraTierInfo.CODEC.parse(JsonOps.INSTANCE, gson.fromJson(reader, JsonObject.class));
                info.result().ifPresent(tInfo -> {
                    String[] nameSplit = tier_json.getName().split("\\.");
                    tierMap.put(TierSortingRegistry.byName(new ResourceLocation(nameSplit[0], nameSplit[1])), tInfo);
                });
            } catch (FileNotFoundException e) {
                player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_generator_data"), false);
            }
        }
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (!item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.MAINHAND).isEmpty() ||
                    !item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.OFFHAND).isEmpty()) {
                if (Util.getItemNamespace(item).equals(namespace)) {
                    String path = Util.getItemPath(item);
                    WeaponInfo info;
                    NamedInfo nInfo = new NamedInfo();
                    for (String s : namedMap.keySet()) {
                        if (Pattern.compile("\\b" + s + "\\b", Pattern.CASE_INSENSITIVE).matcher(path.replace('_', ' ')).find()) {
                            nInfo = namedMap.get(s);
                            break;
                        }
                    }
                    if (item instanceof TieredItem tItem && tierMap.containsKey(tItem.getTier())) {
                        info = WeaponInfo.generate(tierMap.get(tItem.getTier()), nInfo);
                    }
                    else {
                        info = new WeaponInfo();
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
        Path path = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "generated");
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

}
