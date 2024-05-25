package com.cwjn.skada.util;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import static com.cwjn.skada.Skada.LOGGER;
import static com.cwjn.skada.data.SkadaData.*;

public abstract class Util {

    private static final Style SPACER = Style.EMPTY.withFont(rl("space"));
    private static final Style PIXEL = Style.EMPTY.withFont(rl("minimal_pixel_bitmap"));
    private static final Style PIXEL_LARGE = Style.EMPTY.withFont(rl("minimal_pixel_16x"));

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(Skada.MODID, path);
    }
    public static String getItemNamespace(Item item) {
        return ForgeRegistries.ITEMS.getKey(item).getNamespace();
    }
    public static String getItemPath(Item item) {
        return ForgeRegistries.ITEMS.getKey(item).getPath();
    }
    public static Attribute attribute(SkadaAttributeHolder holder) {
        return holder.getAttribute();
    }
    public static MutableComponent spacer(int i) {
        return Component.translatable("space." + i).withStyle(SPACER);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static String roundToString(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.toString();
    }

    @OnlyIn(Dist.CLIENT)
    public static MutableComponent pixelFontComponent(String s) {
        return pixelFontComponent(s, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    public static MutableComponent pixelFontComponent(Component comp) {
        return pixelFontComponent(comp, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    public static MutableComponent pixelFontComponent(String s, boolean useBoldNumbers, boolean useLargeFont) {
        if (Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
         || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv")) {
            MutableComponent retComp = Component.empty().withStyle(useLargeFont? PIXEL_LARGE : PIXEL);
            if (useBoldNumbers) s = s.replace("0", "ᙐ").replace("1", "ᙑ").replace("2", "ᙒ").replace("3", "ᙓ").replace("4", "ᙔ").replace("5", "ᙕ").replace("6", "ᙖ").replace("7", "ᙗ").replace("8", "ᙘ").replace("9", "ᙙ").replace('.', '_').replace('(', '<').replace(')', '>');
            for (char c : s.toCharArray()) {
                retComp.append(String.valueOf(c));
                if (c == ' ') {
                    retComp.append(spacer(1));
                    continue;
                }
                retComp.append(spacer(-1));
            }
            return retComp;
        }
        else {
            return Component.literal(s);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static MutableComponent pixelFontComponent(Component comp, boolean useBoldNumbers, boolean useLargeFont) {
        if (Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
         || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv")) {
            MutableComponent retComp = Component.empty().withStyle(useLargeFont? PIXEL_LARGE : PIXEL);
            String s = I18n.exists(comp.getString())? I18n.get(comp.getString()) : comp.getString();
            if (useBoldNumbers) s = s.replace("0", "ᙐ").replace("1", "ᙑ").replace("2", "ᙒ").replace("3", "ᙓ").replace("4", "ᙔ").replace("5", "ᙕ").replace("6", "ᙖ").replace("7", "ᙗ").replace("8", "ᙘ").replace("9", "ᙙ").replace('.', '_').replace('(', '<').replace(')', '>');
            Style currentFormatting = Style.EMPTY;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '§' && i < s.length()-1) {
                    if (s.charAt(i+1) == 'r') {
                        currentFormatting = Style.EMPTY;
                    }
                    else {
                        ChatFormatting cf = ChatFormatting.getByCode(s.charAt(i+1));
                        if (cf != null) currentFormatting = currentFormatting.applyFormat(cf);
                    }
                    i++;
                    continue;
                }
                else {
                    retComp.append(Component.literal(String.valueOf(c)).withStyle(currentFormatting));
                    if (c == ' ') {
                        retComp.append(spacer(1));
                        continue;
                    }
                }
                retComp.append(spacer(-1));
            }
            return retComp;
        }
        else {
            return comp.copy();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static MutableComponent pixelFontComponent(Component... comp) {
        if (Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
         || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv")) {
            MutableComponent retComp = Component.empty();
            for (Component c : comp) {
                Style style = c.getStyle().applyTo(PIXEL);
                String s = I18n.get(c.getString());
                for (int i = 0; i < s.length(); i++) {
                    char ch = s.charAt(i);
                    if (ch == '§') {
                        i++;
                        continue;
                    }
                    else {
                        retComp.append(Component.literal(String.valueOf(ch)).withStyle(style));
                        if (ch == ' ') {
                            retComp.append(spacer(1));
                            continue;
                        }
                    }
                    retComp.append(spacer(-1));
                }
            }
            return retComp;
        }
        else {
            MutableComponent retComp = Component.empty();
            for (Component c : comp) {
                retComp.append(c);
            }
            return retComp;
        }
    }


    public static void addWeaponInfoTagIfNotExists(ItemStack i) {
        if (((AccessWeaponInfo)i.getItem()).skada$hasWeaponInfo()) {
            if (!i.getOrCreateTag().contains(WEAPON_INFO_TAG_KEY)) {
                i.getOrCreateTag().put(WEAPON_INFO_TAG_KEY,
                        ((AccessWeaponInfo)i.getItem()).skada$getWeaponInfo().toCompoundTag());
                i.getOrCreateTag().putInt(CURRENT_ATTACK_TYPE_TAG_KEY, 0);
                i.getOrCreateTag().putInt(NUM_ATTACK_TYPES_TAG_KEY, ((AccessWeaponInfo)i.getItem()).skada$getWeaponInfo().getAttackTypes().size());
            }
        }
    }

    public static void updateWeaponInfoItemsFromResources(ResourceManager manager) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        LOGGER.info("------------------> Reading Weapon Info json files");
        manager.listResources("weapon_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
            LOGGER.info("--------------> " + rl.toString());
            String[] pathSplit = rl.getPath().split("/");
            String modId = pathSplit[pathSplit.length-1].substring(0, pathSplit[pathSplit.length-1].length()-5);
            if (FMLLoader.getLoadingModList().getModFileById(modId)!=null) {
                try {
                    BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
                    JsonObject obj = gson.fromJson(reader, JsonObject.class);
                    DataResult<Map<String, WeaponInfo>> info = WeaponInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
                    info.result().ifPresent((map) -> {
                        map.forEach((key, value) -> {
                            LOGGER.info("----------> " + key);
                            ResourceLocation iRL = new ResourceLocation(modId, key);
                            Item iItem = ForgeRegistries.ITEMS.getValue(iRL);
                            if (iItem != null) {
                                AccessWeaponInfo mItem = (AccessWeaponInfo) iItem;
                                mItem.skada$setWeaponInfo(value);
                            }
                        });
                    });
                } catch (Exception e) {
                    LOGGER.error("Failed to read weapon info from " + rl, e);
                }
            }
            else {
                LOGGER.info("----------> Skipping weapon info file for mod " + modId + " because it is not loaded!");
            }
        });
    }

    public static double percentReduc(double lethality, double armorToughness) {
        if (lethality <= armorToughness) return 1;
        else return 1/(1+(0.04*Math.pow(lethality-armorToughness, (lethality/armorToughness))));
    }

    public static double slashLethalityCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = weight;
        bonus += hardness*0.5;
        return bonus;
    }

    public static double slashDamageCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = 0.0;
        bonus -= toughness*0.5;
        bonus += flexibility;
        return bonus;
    }

    public static double slashAimCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = 0.0;
        bonus += toughness;
        bonus -= flexibility;
        return bonus;
    }

    public static double thrustLethalityCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = weight;
        bonus += hardness*0.5;
        return bonus;
    }

    public static double thrustDamageCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = 0.0;
        bonus += toughness*0.5;
        return bonus;
    }

    public static double thrustAimCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = 0.0;
        bonus += toughness*0.5;
        bonus -= flexibility*1.33;
        return bonus;
    }

    public static double strikeLethalityCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = weight;
        bonus += toughness*0.5;
        return bonus;
    }

    public static double strikeDamageCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = weight*0.5;
        bonus += hardness*0.5;
        return bonus;
    }

    public static double strikeAimCalculation(double weight, double hardness, double toughness, double flexibility) {
        return 0;
    }

    public static double getCriticalFailChance(double weight, double hardness, double toughness, double flexibility) {
        return 0.01*hardness*hardness/toughness;
    }

}
