package com.cwjn.skada.util;

import com.cwjn.skada.ClientConfig;
import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.Skada;
import com.cwjn.skada.client.hud.ReticleShape;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.armour.AccessArmourInfo;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.damage.AccessWeaponInfo;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPart;
import com.cwjn.skada.data.mob.MobData;
import com.cwjn.skada.data.registry.AttackType;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Math;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import static com.cwjn.skada.Skada.LOGGER;
import static com.cwjn.skada.data.SkadaData.*;
import static net.minecraft.world.item.ItemStack.ATTRIBUTE_MODIFIER_FORMAT;

public abstract class Util {

  private static final String WEAPON_DATA_ROOT = "generator_data/weapon";
  private static final String WEAPON_PART_PATH_PREFIX = "generator_data/weapon/part/";
  private static final String WEAPON_ASSEMBLY_PATH_PREFIX = "generator_data/weapon/weapon_profile/";

  private static final Style SPACER = Style.EMPTY.withFont(rl("space"));
  private static final Style PIXEL = Style.EMPTY.withFont(rl("minimal_pixel_bitmap"));
  private static final Style PIXEL_16 = Style.EMPTY.withFont(rl("minimal_pixel_16x"));
  private static final Style PIXEL_12 = Style.EMPTY.withFont(rl("minimal_pixel_12x"));

  @SuppressWarnings("null")
  public static ResourceLocation rl(String path) {
    return new ResourceLocation(Skada.MODID, path);
  }

  public static String getItemNamespace(Item item) {
    return ForgeRegistries.ITEMS.getKey(item).getNamespace();
  }

  public static String getItemPath(Item item) {
    return ForgeRegistries.ITEMS.getKey(item).getPath();
  }

  public static String getEntityNamespace(EntityType<?> entity) {
    return ForgeRegistries.ENTITY_TYPES.getKey(entity).getNamespace();
  }

  public static String getEntityPath(EntityType<?> entity) {
    return ForgeRegistries.ENTITY_TYPES.getKey(entity).getPath();
  }

  @SuppressWarnings("null")
  public static MutableComponent spacer(int i) {
    return Component.translatable("space." + i).withStyle(SPACER);
  }

  @SuppressWarnings("null")
  public static final Codec<AttributeModifier> ATTRIBUTE_MODIFIER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.DOUBLE.fieldOf("amount").forGetter(AttributeModifier::getAmount),
          Codec.INT.fieldOf("operation").forGetter(Util::getOperation)
  ).apply(instance, Util::attrMod));


  private static int getOperation(AttributeModifier mod) {
    return mod.getOperation().toValue();
  }

  @SuppressWarnings("null")
  private static AttributeModifier attrMod(double amount, int operation) {
    //The first parameter of AttributeModifier is supposed to be a "name," but since this will be
    //used to generate attributemodifiers dynamically, there's no need for a name, we just need
    //to ensure they are unique.
    return new AttributeModifier(UUID.randomUUID().toString(), amount, AttributeModifier.Operation.fromValue(operation));
  }

  public static double roundAndClamp(double value, int places, double min, double max) {
    return Math.min(max, Math.max(min, round(value, places)));
  }

  public static double round(double value, int places) {
    if (places < 0) throw new IllegalArgumentException();
    BigDecimal bd = new BigDecimal(Double.toString(value));
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }

  public static float round(float value, int places) {
    if (places < 0) throw new IllegalArgumentException();
    BigDecimal bd = new BigDecimal(Float.toString(value));
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.floatValue();
  }

  public static String roundToString(double value, int places) {
    if (places < 0) throw new IllegalArgumentException();
    BigDecimal bd = new BigDecimal(Double.toString(value));
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.toString();
  }

  /**
   * Gets a colour from red to green based the difference between the value and defaultValue,
   * where if the value is worse than the expected value, it is red, and if it is better, it is green.
   */
  public static int getColourByPercentage(double value, double defaultValue, boolean higherIsBetter) {
    double difference = value - defaultValue;
    int red = 128, green = 128, blue = 128; //start at #808080 so that when we add colour it'll be brighter

    float percentOfDefault;
    if (defaultValue == 0) percentOfDefault = (float) difference;
    else percentOfDefault = (float) (difference / defaultValue);
    int mainHex = Math.round(128 * mainToOtherHexRatio(percentOfDefault));
    int otherHex = 128 - mainHex;
    blue += otherHex;

    if (percentOfDefault < 0.05) {
      if (higherIsBetter) {
        red += mainHex;
        green += otherHex;
      } else {
        red += otherHex;
        green += mainHex;
      }
    } else {
      if (higherIsBetter) {
        red += otherHex;
        green += mainHex;
      } else {
        red += mainHex;
        green += otherHex;
      }
    }
    red = Math.min(255, red);
    green = Math.min(255, green);
    blue = Math.min(255, blue);

    return (red << 16) | (green << 8) | blue;
  }

  /*
      Get the ratio of main hex colour to other hex colours based on percentage.
      At 500% (5.0), the main hex colour is 100% and the other hex colours are 0%.
   */
  private static float mainToOtherHexRatio(float percent) {
    float hexPercent = percent / 3.0f;
    return Math.max(0.5f, hexPercent);
  }

  /**
   * Creates a pixel font component with the given string. Only one of usePixel16 or usePixel12 should be true,
   * but usePixel16 is used if both are true.
   */
  @OnlyIn(Dist.CLIENT)
  public static MutableComponent pixelFontComponent(String s) {
    return pixelFontComponent(s, false, false, false);
  }

  /**
   * Creates a pixel font component with the given MutableComponent. Only one of usePixel16 or usePixel12 should be true,
   * but usePixel16 is used if both are true.
   */
  @OnlyIn(Dist.CLIENT)
  public static MutableComponent pixelFontComponent(MutableComponent comp) {
    return ClientConfig.USE_MDU_FONT.get() ? pixelFontComponent(comp, false, false, false) : comp.copy();
  }

  /**
   * Creates a pixel font component with the given string. Only one of usePixel16 or usePixel12 should be true,
   * but usePixel16 is used if both are true.
   */
  @SuppressWarnings("null")
  @OnlyIn(Dist.CLIENT)
  public static MutableComponent pixelFontComponent(String s, boolean useBoldNumbers, boolean usePixel16, boolean usePixel12) {
    if ((Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
            || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv"))
            && ClientConfig.USE_MDU_FONT.get()) {
      @SuppressWarnings("null")
      MutableComponent retComp = Component.empty().withStyle(usePixel16 ? PIXEL_16 : usePixel12 ? PIXEL_12 : PIXEL);
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
    } else {
      return Component.literal(s);
    }
  }

  /**
   * Creates a pixel font component with the given MutableComponent. Only one of usePixel16 or usePixel12 should be true,
   * but usePixel16 is used if both are true.
   */
  @SuppressWarnings("null")
  @OnlyIn(Dist.CLIENT)
  public static MutableComponent pixelFontComponent(MutableComponent comp, boolean useBoldNumbers, boolean usePixel16, boolean usePixel12) {
    if ((Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
            || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv"))
            && ClientConfig.USE_MDU_FONT.get()) {
      @SuppressWarnings("null")
      MutableComponent retComp = Component.empty().withStyle(usePixel16 ? PIXEL_16 : usePixel12 ? PIXEL_12 : PIXEL);
      @SuppressWarnings("null")
      String s = I18n.exists(comp.getString()) ? I18n.get(comp.getString()) : comp.getString();
      if (useBoldNumbers) s = s.replace("0", "ᙐ").replace("1", "ᙑ").replace("2", "ᙒ").replace("3", "ᙓ").replace("4", "ᙔ").replace("5", "ᙕ").replace("6", "ᙖ").replace("7", "ᙗ").replace("8", "ᙘ").replace("9", "ᙙ").replace('.', '_').replace('(', '<').replace(')', '>');
      Style currentFormatting = Style.EMPTY;
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c == '§' && i < s.length() - 1) {
          if (s.charAt(i + 1) == 'r') {
            currentFormatting = Style.EMPTY;
          } else {
            ChatFormatting cf = ChatFormatting.getByCode(s.charAt(i + 1));
            if (cf != null) currentFormatting = currentFormatting.applyFormat(cf);
          }
          i++;
          continue;
        } else {
          retComp.append(Component.literal(String.valueOf(c)).withStyle(currentFormatting));
          if (c == ' ') {
            retComp.append(spacer(1));
            continue;
          }
        }
        retComp.append(spacer(-1));
      }
      return retComp;
    } else {
      return comp.copy();
    }
  }

  @SuppressWarnings("null")
  @OnlyIn(Dist.CLIENT)
  public static MutableComponent pixelFontComponent(Component... comp) {
    if (Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
            || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv")) {
      MutableComponent retComp = Component.empty();
      for (Component c : comp) {
        @SuppressWarnings("null")
        Style style = c.getStyle().applyTo(PIXEL);
        @SuppressWarnings("null")
        String s = I18n.get(c.getString());
        for (int i = 0; i < s.length(); i++) {
          char ch = s.charAt(i);
          if (ch == '§') {
            i++;
            continue;
          } else {
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
    } else {
      MutableComponent retComp = Component.empty();
      for (Component c : comp) {
        retComp.append(c);
      }
      return retComp;
    }
  }

  @SuppressWarnings("null")
  public static List<Component> getVanillaTooltip(Player pPlayer, ItemStack stack) {
    List<Component> list = new ArrayList<>();
    for (EquipmentSlot equipmentslot : EquipmentSlot.values()) {
      @SuppressWarnings("null")
      Multimap<Attribute, AttributeModifier> multimap = stack.getAttributeModifiers(equipmentslot);
      if (!multimap.isEmpty()) {
        list.add(CommonComponents.EMPTY);
        list.add(Component.translatable("item.modifiers." + equipmentslot.getName()).withStyle(ChatFormatting.GRAY));

        for (Map.Entry<Attribute, AttributeModifier> entry : multimap.entries()) {
          AttributeModifier attributemodifier = entry.getValue();
          double d0 = attributemodifier.getAmount();
          boolean flag = false;
          if (pPlayer != null) {
            if (attributemodifier.getId() == SkadaData.BASE_ATTACK_DAMAGE_UUID) {
              d0 += pPlayer.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
              d0 += (double) EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);
              flag = true;
            } else if (attributemodifier.getId() == SkadaData.BASE_ATTACK_SPEED_UUID) {
              d0 += pPlayer.getAttributeBaseValue(Attributes.ATTACK_SPEED);
              flag = true;
            }
          }

          double d1;
          if (attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
            if (entry.getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
              d1 = d0 * 10.0D;
            } else {
              d1 = d0;
            }
          } else {
            d1 = d0 * 100.0D;
          }

          if (flag) {
            list.add(CommonComponents.space().append(Component.translatable("attribute.modifier.equals." + attributemodifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(entry.getKey().getDescriptionId()))).withStyle(ChatFormatting.DARK_GREEN));
          } else if (d0 > 0.0D) {
            list.add(Component.translatable("attribute.modifier.plus." + attributemodifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(entry.getKey().getDescriptionId())).withStyle(ChatFormatting.BLUE));
          } else if (d0 < 0.0D) {
            d1 *= -1.0D;
            list.add(Component.translatable("attribute.modifier.take." + attributemodifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(entry.getKey().getDescriptionId())).withStyle(ChatFormatting.RED));
          }
        }
      }
    }
    return list;
  }

  @SuppressWarnings("null")
  public static List<Component> otherAttributesComponent(Multimap<Attribute, AttributeModifier> mainAttributes) {
    List<Component> list = new ArrayList<>();
    for (Map.Entry<Attribute, AttributeModifier> attributeAttributeModifierEntry : mainAttributes.entries()) {
      AttributeModifier attributemodifier = attributeAttributeModifierEntry.getValue();
      double d0 = attributemodifier.getAmount();
      boolean flag = false;
      double d1;
      if (attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
        if ((attributeAttributeModifierEntry).getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
          d1 = d0 * 10.0;
        } else {
          d1 = d0;
        }
      } else {
        d1 = d0 * 100.0;
      }
      if (d0 > 0.0) {
        list.add(Component.translatable("attribute.modifier.plus." + attributemodifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable((attributeAttributeModifierEntry).getKey().getDescriptionId())).withStyle(ChatFormatting.BLUE));
      } else if (d0 < 0.0) {
        d1 *= -1.0;
        list.add(Component.translatable("attribute.modifier.take." + attributemodifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable((attributeAttributeModifierEntry).getKey().getDescriptionId())).withStyle(ChatFormatting.RED));
      }
    }
    return list;
  }

  @SuppressWarnings("null")
  public static List<Component> getOtherSlotAttributesAsList(EquipmentSlot slot, Multimap<Attribute, AttributeModifier> multimap) {
    List<Component> list = new ArrayList<>();
    list.add(CommonComponents.EMPTY);
    list.add(Component.translatable("item.modifiers." + slot.getName()).withStyle(ChatFormatting.GRAY));
    if (!multimap.isEmpty()) {
      for (Map.Entry<Attribute, AttributeModifier> attributeAttributeModifierEntry : multimap.entries()) {
        AttributeModifier attributemodifier = attributeAttributeModifierEntry.getValue();
        double d0 = attributemodifier.getAmount();
        boolean flag = false;
        double d1;
        if (attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
          if ((attributeAttributeModifierEntry).getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
            d1 = d0 * 10.0;
          } else {
            d1 = d0;
          }
        } else {
          d1 = d0 * 100.0;
        }
        if (d0 > 0.0) {
          list.add(Component.translatable("attribute.modifier.plus." + attributemodifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable((attributeAttributeModifierEntry).getKey().getDescriptionId())).withStyle(ChatFormatting.BLUE));
        } else if (d0 < 0.0) {
          d1 *= -1.0;
          list.add(Component.translatable("attribute.modifier.take." + attributemodifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable((attributeAttributeModifierEntry).getKey().getDescriptionId())).withStyle(ChatFormatting.RED));
        }
      }
    }
    return list;
  }

  @SuppressWarnings("null")
  public static void addWeaponArmourInfoTagIfNotExists(ItemStack i) {
    if (((AccessWeaponInfo) i.getItem()).skada$hasWeaponInfo()) {
      if (!i.getOrCreateTag().contains(WEAPON_INFO_TAG_KEY)) {
        i.getOrCreateTag().put(WEAPON_INFO_TAG_KEY,
                ((AccessWeaponInfo) i.getItem()).skada$getWeaponInfo().toCompoundTag());
        i.getOrCreateTag().putInt(CURRENT_ATTACK_TYPE_TAG_KEY, 0);
        i.getOrCreateTag().putInt(NUM_ATTACK_TYPES_TAG_KEY, ((AccessWeaponInfo) i.getItem()).skada$getWeaponInfo().getAttackTypes().size());
      }
    }
    if (((AccessArmourInfo) i.getItem()).skada$hasArmourInfo()) {
      if (!i.getOrCreateTag().contains(ARMOUR_INFO_TAG_KEY)) {
        i.getOrCreateTag().put(ARMOUR_INFO_TAG_KEY,
                ((AccessArmourInfo) i.getItem()).skada$getArmourInfo().toCompoundTag());
      }
    }
  }

  @SuppressWarnings("null")
  public static WeaponInfo getWeaponInfo(Player p) {
    ItemStack i = p.getMainHandItem();
    if (i.hasTag() && i.getTag().contains(WEAPON_INFO_TAG_KEY)) {
      return WeaponInfo.fromCompoundTag(i.getTag().getCompound(WEAPON_INFO_TAG_KEY));
    } else {
      return WeaponInfo.NO_WEAPON;
    }
  }

  public static AttackType[] getAttackTypes(Player p) {
    WeaponInfo info = getWeaponInfo(p);
    return info.getAttackTypes().keySet().toArray(AttackType[]::new);
  }

  @SuppressWarnings("null")
  public static AttackType getAttackType(Player p) {
    if (p.getMainHandItem().hasTag() && p.getMainHandItem().getTag().contains(CURRENT_ATTACK_TYPE_TAG_KEY)) {
      return getAttackTypes(p)[p.getMainHandItem().getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)];
    } else {
      return AttackType.strike();
    }
  }

  @SuppressWarnings("null")
  public static AttackTypeInfo getAttackTypeInfo(Player p) {
    if (p.getMainHandItem().hasTag() && p.getMainHandItem().getTag().contains(CURRENT_ATTACK_TYPE_TAG_KEY)) {
      return getWeaponInfo(p).getAttackTypes().get(getAttackType(p));
    } else {
      return AttackTypeInfo.DEFAULT;
    }
  }

  @SuppressWarnings("null")
  public static WeaponInfo getWeaponInfo(ItemStack i) {
    if (i.hasTag() && i.getTag().contains(WEAPON_INFO_TAG_KEY)) {
      return WeaponInfo.fromCompoundTag(i.getTag().getCompound(WEAPON_INFO_TAG_KEY));
    } else {
      return WeaponInfo.NO_WEAPON;
    }
  }

  public static AttackType[] getAttackTypes(ItemStack i) {
    WeaponInfo info = getWeaponInfo(i);
    return info.getAttackTypes().keySet().toArray(AttackType[]::new);
  }

  @SuppressWarnings("null")
  public static AttackType getAttackType(ItemStack i) {
    if (i.hasTag() && i.getTag().contains(CURRENT_ATTACK_TYPE_TAG_KEY)) {
      return getAttackTypes(i)[i.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)];
    } else {
      return AttackType.strike();
    }
  }

  @SuppressWarnings("null")
  public static AttackTypeInfo getAttackTypeInfo(ItemStack i) {
    if (i.hasTag() && i.getTag().contains(CURRENT_ATTACK_TYPE_TAG_KEY)) {
      return getWeaponInfo(i).getAttackTypes().get(getAttackType(i));
    } else {
      return AttackTypeInfo.DEFAULT;
    }
  }

  /**
   * Calculates the player's speed in blocks per second based on the movespeed attribute.
   * The formula is based on the fact that 0.1 movement speed is equal to 4.3 blocks per second.
   *
   * @param movespeed_attribute the player's movespeed attribute value, which by default is 0.1
   * @return the player's speed in blocks per second, rounded to 1 decimal place
   */
  public static double getPlayerSpeedInBlocksPerSecond(double movespeed_attribute) {
    BigDecimal x = BigDecimal.valueOf(movespeed_attribute * 43.178);
    return x.setScale(1, RoundingMode.HALF_UP).doubleValue();
  }

  public static void updateReticleListFromResources(ResourceManager manager) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    LOGGER.info("------------------> Reading reticle json files");
    manager.listResources("reticles", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
      LOGGER.info("--------------> " + rl.toString());
      try {
        BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
        JsonObject obj = gson.fromJson(reader, JsonObject.class);
        DataResult<ReticleShape> info = ReticleShape.CODEC.parse(JsonOps.INSTANCE, obj);
        info.result().ifPresent((x) -> RETICLES.put(x.getName(), x));
      } catch (Exception e) {
        LOGGER.error("Failed to read reticle info from " + rl, e);
      }
    });
  }

  public static void updateWeaponInfoItemsFromResources(ResourceManager manager) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    LOGGER.info("------------------> Reading Weapon Info json files");
    manager.listResources("weapon_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
      LOGGER.info("--------------> " + rl.toString());
      String[] pathSplit = rl.getPath().split("/");
      String modId = pathSplit[pathSplit.length - 1].substring(0, pathSplit[pathSplit.length - 1].length() - 5);
      if (FMLLoader.getLoadingModList().getModFileById(modId) != null) {
        try {
          BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
          JsonObject obj = gson.fromJson(reader, JsonObject.class);
          DataResult<Map<String, WeaponInfo>> info = WeaponInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
          info.result().ifPresent((map) -> {
            map.forEach((key, value) -> {
              LOGGER.info("----------> " + key);
              @SuppressWarnings("null")
              ResourceLocation iRL = new ResourceLocation(modId, key);
              Item iItem = ForgeRegistries.ITEMS.getValue(iRL);
              if (iItem != null) {
                AccessWeaponInfo mItem = (AccessWeaponInfo) iItem;
                if (value.getAttackTypes().isEmpty()) {
                  LOGGER.error("Weapon info for {} has no attack types, skipping", iRL);
                } else {
                  mItem.skada$setWeaponInfo(value);
                }
              }
            });
          });
        } catch (Exception e) {
          LOGGER.error("Failed to read weapon info from " + rl, e);
        }
      } else {
        LOGGER.info("----------> Skipping weapon info file for mod " + modId + " because it is not loaded!");
      }
    });
  }

  public static void updateArmourInfoItemsFromResources(ResourceManager manager) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    LOGGER.info("------------------> Reading Armour Info json files");
    manager.listResources("armour_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
      LOGGER.info("--------------> " + rl.toString());
      String[] pathSplit = rl.getPath().split("/");
      String modId = pathSplit[pathSplit.length - 1].substring(0, pathSplit[pathSplit.length - 1].length() - 5);
      if (FMLLoader.getLoadingModList().getModFileById(modId) != null) {
        try {
          BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
          JsonObject obj = gson.fromJson(reader, JsonObject.class);
          DataResult<Map<String, ArmourInfo>> info = ArmourInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
          info.result().ifPresent((map) -> {
            map.forEach((key, value) -> {
              LOGGER.info("----------> " + key);
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
          LOGGER.error("Failed to read armour info from " + rl, e);
        }
      } else {
        LOGGER.info("----------> Skipping armour info file for mod " + modId + " because it is not loaded!");
      }
    });
  }

  public static void updateMobInfoFromResources(ResourceManager manager) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    LOGGER.info("------------------> Reading Mob Info json files");
    manager.listResources("mob_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
      LOGGER.info("--------------> " + rl.toString());
      String[] pathSplit = rl.getPath().split("/");
      String modId = pathSplit[pathSplit.length - 1].substring(0, pathSplit[pathSplit.length - 1].length() - 5);
      if (FMLLoader.getLoadingModList().getModFileById(modId) != null) {
        try {
          BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
          JsonObject obj = gson.fromJson(reader, JsonObject.class);
          DataResult<Map<String, MobData>> info = MobData.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
          info.result().ifPresent((map) -> {
            map.forEach((key, value) -> {
              LOGGER.info("----------> " + key);
              @SuppressWarnings("null")
              ResourceLocation iRL = new ResourceLocation(modId, key);
              EntityType<?> iEntity = getMobEntityType(iRL);
              if (iEntity != null) {
                MOB_DATA.put(iEntity, value);
              }
            });
          });
        } catch (Exception e) {
          LOGGER.error("Failed to read mob info from " + rl, e);
        }
      } else {
        LOGGER.info("----------> Skipping mob info file for mod " + modId + " because it is not loaded!");
      }
    });
    LOGGER.info("-----------> Finished loading mob info, flattening parents");
    MOB_DATA.forEach((key, value) -> {
      if (value.parents() != null) flattenParentModifiers(key, value);
    });
  }

  @SuppressWarnings("null")
  private static void flattenParentModifiers(EntityType<?> type, MobData mobData) {
    if (mobData.parents() == null || mobData.parents().isEmpty()) return;
    @SuppressWarnings("null")
    Multimap<Attribute, AttributeModifier> flattenedModifiers = ArrayListMultimap.create(mobData.extraModifiers());

    for (String parentPath : mobData.parents()) {
      @SuppressWarnings("null")
      ResourceLocation parentRL = ResourceLocation.tryParse(parentPath);
      if (parentRL != null) {
        EntityType<?> parentEntity = getMobEntityType(parentRL);
        if (parentEntity != null && MOB_DATA.containsKey(parentEntity)) {
          MobData parentData = MOB_DATA.get(parentEntity);
          // Recursively flatten parent's modifiers first
          flattenParentModifiers(type, parentData);
          // Add parent's modifiers to our flattened set
          parentData.extraModifiers().entries().forEach(entry -> {
            flattenedModifiers.put(entry.getKey(), entry.getValue());
          });
        }
      }
    }

    // Replace the original MobData with a new instance containing flattened modifiers
    MOB_DATA.put(type, new MobData(null, mobData.attackType(), flattenedModifiers));
  }

  private static EntityType<?> getMobEntityType(ResourceLocation iRL) {
    EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(iRL);
    if (entityType != null && entityType.getCategory() != MobCategory.MISC) {
      return entityType;
    }
    return null; // or handle the case where it's not a Mob
  }

  public static String getEntityRegistryName(EntityType<?> type) {
    ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(type);
    if (rl != null) {
      return rl.toString();
    } else {
      return "unknown_entity";
    }
  }

  @SuppressWarnings("null")
  private static Vec3 getFirstViewPlayerHandPos(Player player, boolean isLeftHand, float partialTick) {
    Minecraft mc = Minecraft.getInstance();
    double d4 = 960.0 / mc.options.fov().get();
    Vec3 vec3 = mc.gameRenderer.getMainCamera().getNearPlane().getPointOnPlane(isLeftHand ? -0.525F : 0.525F, -0.3F).scale(d4);
    return player.getEyePosition(partialTick).add(vec3);
  }

  @SuppressWarnings("null")
  public static Vec3 get3DCoordFrom2D(float x, float y, float partialTick) {
    Minecraft mc = Minecraft.getInstance();
    double d4 = 960.0 / mc.options.fov().get(); // Magic number for scaling

    // Get the screen dimensions
    float width = mc.getWindow().getGuiScaledWidth();
    float height = mc.getWindow().getGuiScaledHeight();

    // Convert screen coordinates to normalized device coordinates (NDC)
    float leftScale = (x / width) * 2 - 1; // NDC x-coordinate
    float upScale = 1 - (y / height) * 2; // NDC y-coordinate (inverted)

    // Get the point on the near plane
    Vec3 vec3 = mc.gameRenderer.getMainCamera().getNearPlane().getPointOnPlane(leftScale, upScale).scale(d4);

    // Add the player's eye position to get the correct 3D coordinate
    return mc.cameraEntity.getEyePosition(partialTick).add(vec3);
  }

  @SuppressWarnings("null")
  public static Vec3 getMovementVector(Vec3 start, Vec3 end) {
    return end.subtract(start);
  }

  private static final double PRECISION_SCORE_OFFSET = 5.0;
  private static final double PRECISION_TOUGHNESS_WEIGHT = 0.5;
  private static final double PRECISION_CONSISTENCY_MIN = 0.0;
  private static final double PRECISION_CONSISTENCY_MAX = 0.98;
  private static final double PROJECTILE_INACCURACY_MAX = 15.0;
  private static final double ANGULAR_VELOCITY_BASE = 10.0;
  private static final double ANGULAR_VELOCITY_REFERENCE_INERTIA = 0.00015;
  private static final double ANGULAR_VELOCITY_INERTIA_EXPONENT = 0.4;
  private static final double ANGULAR_VELOCITY_STRENGTH_REFERENCE = 50.0;
  private static final double ANGULAR_VELOCITY_MAX = 200.0;

  public static double precisionScoreToConsistency(double rawPrecision) {
    return precisionScoreToConsistency(rawPrecision, 0.0);
  }

  public static double precisionScoreToConsistency(double rawPrecision, double targetToughness) {
    double safePrecision = Math.max(0.0, rawPrecision);
    double safeToughness = Math.max(0.0, targetToughness);
    double denominator = safePrecision + PRECISION_SCORE_OFFSET + PRECISION_TOUGHNESS_WEIGHT * safeToughness;
    if (denominator <= 0.0) {
      return PRECISION_CONSISTENCY_MIN;
    }
    double consistency = safePrecision / denominator;
    return Math.max(PRECISION_CONSISTENCY_MIN, Math.min(PRECISION_CONSISTENCY_MAX, consistency));
  }

  public static double precisionScoreToProjectileInaccuracy(double rawPrecision) {
    return PROJECTILE_INACCURACY_MAX * (1.0 - precisionScoreToConsistency(rawPrecision));
  }

  public static double precisionScoreToDisplayPercent(double rawPrecision) {
    return precisionScoreToConsistency(rawPrecision) * 100.0;
  }

  public static double projectileVelocityWithDamageBonus(double baseVelocity, double damageBonus) {
    return Math.max(0.0, baseVelocity + damageBonus);
  }

  public enum CriticalFailSeverity {
    NONE,
    EDGE_DAMAGE,
    DEFORMATION,
    CATASTROPHIC
  }

  private static final String CRITICAL_FAIL_EDGE_DAMAGE_TAG = "skadaCriticalFailEdgeDamage";
  private static final String CRITICAL_FAIL_DEFORMATION_TAG = "skadaCriticalFailDeformation";
  private static final double EDGE_DAMAGE_DURABILITY_LOSS = 0.01;
  private static final double BASE_DEFORMATION_SEVERITY_SHARE = 0.18;
  private static final double BASE_CATASTROPHIC_SEVERITY_SHARE = 0.02;
  private static final double EDGE_DAMAGE_TO_DEFORMATION_WEIGHT = 0.03;
  private static final double EDGE_DAMAGE_TO_CATASTROPHIC_WEIGHT = 0.01;
  private static final double DEFORMATION_TO_DEFORMATION_WEIGHT = 0.05;
  private static final double DEFORMATION_TO_CATASTROPHIC_WEIGHT = 0.08;
  private static final double FAIL_CHANCE_TO_DEFORMATION_WEIGHT = 0.80;
  private static final double FAIL_CHANCE_TO_CATASTROPHIC_WEIGHT = 0.35;

  /*
   * Roll for a critical fail and apply a severity tier.
   * Edge damage is the common light outcome, deformation is the moderate outcome,
   * and catastrophic failure breaks the item outright.
   */
  @SuppressWarnings("null")
  public static void rollCriticalFail(ItemStack item, double chance, RandomSource random, @NotNull ServerPlayer player) {
    if (!CommonConfig.ENABLE_CRITICAL_FAIL.get()) return;
    if (player.getAbilities().instabuild) return;
    CriticalFailSeverity severity = sampleCriticalFailSeverity(item, chance, random);
    if (severity == CriticalFailSeverity.NONE) {
      return;
    }
    applyCriticalFail(item, severity, player);
  }

  public static CriticalFailSeverity sampleCriticalFailSeverity(ItemStack item, double chance, RandomSource random) {
    int edgeDamage = criticalFailEdgeDamageCount(item);
    int deformation = criticalFailDeformationCount(item);
    return criticalFailSeverity(chance, random.nextDouble(), random.nextDouble(), edgeDamage, deformation);
  }

  static CriticalFailSeverity criticalFailSeverity(double chance, double triggerRoll, double severityRoll, int edgeDamage, int deformation) {
    if (triggerRoll >= chance) {
      return CriticalFailSeverity.NONE;
    }

    double catastrophicShare = Mth.clamp(
      BASE_CATASTROPHIC_SEVERITY_SHARE
        + EDGE_DAMAGE_TO_CATASTROPHIC_WEIGHT * Math.max(0, edgeDamage)
        + DEFORMATION_TO_CATASTROPHIC_WEIGHT * Math.max(0, deformation)
        + FAIL_CHANCE_TO_CATASTROPHIC_WEIGHT * Math.max(0.0, chance),
      0.02,
      0.35);
    double deformationShare = Mth.clamp(
      BASE_DEFORMATION_SEVERITY_SHARE
        + EDGE_DAMAGE_TO_DEFORMATION_WEIGHT * Math.max(0, edgeDamage)
        + DEFORMATION_TO_DEFORMATION_WEIGHT * Math.max(0, deformation)
        + FAIL_CHANCE_TO_DEFORMATION_WEIGHT * Math.max(0.0, chance),
      0.18,
      0.78 - catastrophicShare);

    if (severityRoll < catastrophicShare) {
      return CriticalFailSeverity.CATASTROPHIC;
    }
    if (severityRoll < catastrophicShare + deformationShare) {
      return CriticalFailSeverity.DEFORMATION;
    }
    return CriticalFailSeverity.EDGE_DAMAGE;
  }

  private static void applyCriticalFail(ItemStack item, CriticalFailSeverity severity, ServerPlayer player) {
    CompoundTag tag = item.getOrCreateTag();
    switch (severity) {
      case EDGE_DAMAGE -> {
        tag.putInt(CRITICAL_FAIL_EDGE_DAMAGE_TAG, criticalFailEdgeDamageCount(item) + 1);
        hurtItemByFraction(item, EDGE_DAMAGE_DURABILITY_LOSS, player);
      }
      case DEFORMATION -> {
        tag.putInt(CRITICAL_FAIL_DEFORMATION_TAG, criticalFailDeformationCount(item) + 1);
        hurtItemByFraction(item, CommonConfig.CRITICAL_FAIL_DURABILITY_LOSS.get(), player);
      }
      case CATASTROPHIC -> item.hurtAndBreak(
        item.getDamageValue(),
        player,
        p -> p.broadcastBreakEvent(player.getUsedItemHand()));
      case NONE -> {
      }
    }
  }

  private static void hurtItemByFraction(ItemStack item, double durabilityLossFraction, ServerPlayer player) {
    int amount = criticalFailDurabilityLoss(item.getMaxDamage(), item.getDamageValue(), durabilityLossFraction);
    item.hurtAndBreak(amount, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
  }

  static int criticalFailDurabilityLoss(int maxDamage, int remainingDurability, double durabilityLossFraction) {
    if (maxDamage <= 0 || remainingDurability <= 0) {
      return 0;
    }
    int scaledLoss = (int) Math.ceil(maxDamage * Math.max(0.0, durabilityLossFraction));
    return Math.max(1, Math.min(remainingDurability, scaledLoss));
  }

  static int criticalFailEdgeDamageCount(ItemStack item) {
    return item.getOrCreateTag().getInt(CRITICAL_FAIL_EDGE_DAMAGE_TAG);
  }

  static int criticalFailDeformationCount(ItemStack item) {
    return item.getOrCreateTag().getInt(CRITICAL_FAIL_DEFORMATION_TAG);
  }

  public static double tridentProjectileVelocity(double baseVelocity, double damageBonus) {
    return projectileVelocityWithDamageBonus(baseVelocity, damageBonus);
  }

  /**
   * Calculates an armour multiplier for strike lethality.
   * Lower returned values mean more of the target's armour is bypassed.
   *
   * @param lethality the lethality value of the attack
   * @param armour the target's current armour
   * @param toughness the target's toughness
   * @param targetHP the current health of the target (unused in calculation)
   * @return a number between 0.25 and 1.0 to be multiplied with the target's armour value
   */
  public static double percentReduc(double lethality, double armour, double toughness, double targetHP) {
    if (lethality <= 0.0) return 1.0;
    double safeArmour = Math.max(0.0, armour);
    double safeToughness = Math.max(0.0, toughness);
    double penetrationShare = 0.75 * lethality / (lethality + 4.0 * safeArmour + 2.0 * safeToughness + 8.0);
    return Math.max(0.25, 1.0 - penetrationShare);
  }

  /**
   * Calculates the amount of damage based on lethality, armour toughness, and target HP.
   * Returns a number that is a percentage of the target's current health, which should be
   * summed with the running total of damage to be dealt.<br>
   * L = A -> 5% cHP<br>
   * L = 2A -> 10% cHP<br>
   * L = 3A -> 15% cHP<br>
   * cap is 25% cHP at L = 5A<br>
   * etc...
   *
   * @param lethality the lethality value of the attack
   * @param armour the target's armour (unused in calculation)
   * @param toughness the target's toughness
   * @param targetHP the current health of the target
   * @return a damage value to be summed with the current damage total
   */
  public static double percentHealthDamage(double lethality, double armour, double toughness, double targetHP) {
    if (toughness == 0) return 0.25 * targetHP;
    double percentage = (0.05 * lethality) / toughness;
    return Math.min(percentage * targetHP, 0.25 * targetHP);
  }

  /**
   * Calculates a bounded slash damage multiplier from lethality.
   * The curve is primarily lethality-driven with diminishing returns.
   *
   * @param lethality the lethality value of the attack
   * @param armour the target's armour (unused in calculation)
   * @param toughness the target's armour toughness (unused in calculation)
   * @param targetHP the current health of the target (unused in calculation)
   * @return the bonus damage multiplier
   */
  public static double percentBonusDamage(double lethality, double armour, double toughness, double targetHP) {
    if (lethality <= 0.0) return 1.0;
    return 1.0 + 0.75 * (1.0 - Math.exp(-lethality / 35.0));
  }

  /**
   * Given a cross-section of a bevel, calculate the bevel angle
   * in degrees. The bevel cross-section is considered a right triangle,
   * where width is the base and thickness is the height. The bevel angle
   * is the angle between the base and the hypotenuse.
   * 
   * @param width width of the bevel cross-section
   * @param thickness height of the bevel cross-section
   * @return the bevel angle in degrees
   */
  public static double findBevelAngleByDimensions(double width, double thickness) {
    if (Double.isNaN(width) || Double.isNaN(thickness) || Double.isInfinite(width) || Double.isInfinite(thickness)) {
      LOGGER.warn("findBevelAngleByDimensions received non-finite dimensions: width={}, height={}", width, thickness);
      return 0.0;
    }

    if (width < 0 || thickness < 0) {
      LOGGER.debug("findBevelAngleByDimensions received negative dimension(s); using absolute values (was width={}, height={})", width, thickness);
      width = Math.abs(width);
      thickness = Math.abs(thickness);
    }

    final double EPS = 1e-9;
    if (width < EPS && thickness < EPS) {
      return 0.0;
    }
    double safeWidth = Math.max(width, EPS);

    double angleDeg = Math.toDegrees(Math.atan2(thickness, safeWidth));

    if (Double.isNaN(angleDeg) || Double.isInfinite(angleDeg)) angleDeg = 0.0;
    angleDeg = Math.max(0.0, Math.min(90.0, angleDeg));
    return angleDeg;
  }

  /**
   * Normalizes a bevel angle for use in damage calculations,
   * according to the "average" bevel angle, which we take to be
   * 22.5 degrees. If the angle is less than 22.5, the normalized
   * value will be greater than 1, indicating a more effective bevel.
   * If the angle is greater than 22.5, the normalized value will be
   * less than 1, indicating a less effective bevel.
   * @param angle bevel angle in degrees
   * @return the bevel angle normalized according to the default bevel angle.
   */
  public static double normalizeBevelAngle(double angle) {
    if (angle <= 0) return 1.0;
    else return BEVEL_ANGLE_DEFAULT / angle;
  }

  /*
    * Calculate the moment of inertia for a rectangular prism (a weapon)
   */
  public static double momentOfInertia(double mass, double length, double centreOfMass) {
    double term1 = (mass * length * length) / 12.0;
    double term2_1 = 1;
    double term2_2 = 6 * (centreOfMass/length);
    double term2_3 = 12 * (centreOfMass/length) * (centreOfMass/length);
    double term2 = term2_1 + term2_2 - term2_3;
    return term1 * term2;
  }

  /**
   * Calculate angular velocity from moment of inertia and player strength.
   *
   * Uses a HEMA-validated empirical formula calibrated to historical European martial arts
   * sword swing data. The formula produces realistic angular velocities across the full
   * spectrum of weapon weights.
   *
   * Empirical basis:
   * - HEMA video analysis shows trained sword fighters achieve 5-20 rad/s peak angular velocity
   * - Light weapons (daggers, ~0.00005 kg·m²): 12-18 rad/s
   * - Medium weapons (longswords, ~0.00015 kg·m²): 8-12 rad/s
   * - Heavy weapons (greatswords, ~0.0003 kg·m²): 5-8 rad/s
   *
   * The formula incorporates:
   * - Base velocity: 10.0 rad/s for average trained human
   * - Inertia penalty: Diminishing returns (0.4 exponent) as weapon gets heavier
   * - Strength multiplier: Scales with player strength (normalized to 50.0 = realistic human)
   *
   * Formula: ω = 10.0 × √(S/50.0) / (I/0.00015)^0.4
   * where S = player strength, I = moment of inertia
   *
   * @param inertia the moment of inertia of the weapon in kg·m²
   * @param playerStrength the player's swing strength (typically 50.0 for average human)
   * @return the angular velocity in radians per second, typically 5-20 rad/s
   */
  public static double angularVelocity(double inertia, double playerStrength) {
    // Guard against non-positive inertia
    if (inertia <= 0 || Double.isNaN(inertia) || Double.isInfinite(inertia)) {
      return 0.0;
    }

    // Strength-dependent factor: normalized to 50.0 = average trained human
    double strengthFactor = 1.0;
    if (playerStrength > 0 && !Double.isNaN(playerStrength) && !Double.isInfinite(playerStrength)) {
      strengthFactor = Math.sqrt(playerStrength / ANGULAR_VELOCITY_STRENGTH_REFERENCE);
    }

    // Inertia penalty: heavier weapons swing slower, but with diminishing effect
    double inertiaPenalty = Math.pow(inertia / ANGULAR_VELOCITY_REFERENCE_INERTIA, ANGULAR_VELOCITY_INERTIA_EXPONENT);
    if (inertiaPenalty <= 0 || Double.isNaN(inertiaPenalty) || Double.isInfinite(inertiaPenalty)) {
      inertiaPenalty = 1.0;
    }

    double result = ANGULAR_VELOCITY_BASE * strengthFactor / inertiaPenalty;

    // Clamp to reasonable physical bounds to avoid absurd values
    result = Math.max(0.0, Math.min(result, ANGULAR_VELOCITY_MAX));
    return result;
  }

  /**
   * Finds the closest matching string from a list to the given path using exact matching, word boundary matching,
   * and Levenshtein distance as fallback.
   * The method first attempts exact match, then word boundary match with all tokens present,
   * and finally falls back to Levenshtein distance for fuzzy matching.
   * This takes a long time. O(big number). Try to keep usage to a minimum if possible.
   * @param strings list of strings that are to be compared to in order to find a match
   * @param path the string to be matched
   * @return the string that matches, or empty string if input list is empty
   */
  public static String findClosestMatch(List<String> strings, String path) {
    if (strings == null || strings.isEmpty()) {
      return "";
    }

    // Normalize the path: lowercase, replace underscores/non-word chars with spaces, trim
    String normalizedPath = path.toLowerCase().replaceAll("[_\\W]+", " ").trim();

    // First pass: Try exact match (case-insensitive, normalized)
    for (String profileKey : strings) {
      String normalizedKey = profileKey.toLowerCase().replaceAll("[_\\W]+", " ").trim();
      if (normalizedPath.equals(normalizedKey)) {
        return profileKey;
      }
    }

    // Create a sorted copy for prioritizing longer (more specific) matches
    List<String> sortedStrings = new ArrayList<>(strings);
    sortedStrings.sort(Comparator.comparingInt(String::length).reversed()
            .thenComparing(String::compareTo));

    // Second pass: Try word boundary matching with ALL tokens present
    for (String profileKey : sortedStrings) {
      String normalizedKey = profileKey.toLowerCase().replaceAll("[_\\W]+", " ").trim();
      String[] keyTokens = normalizedKey.split("\\s+");
      if (keyTokens.length == 0) continue;

      // Check if ALL tokens from the key are present as complete words in the path
      boolean allTokensPresent = true;
      for (String token : keyTokens) {
        // Use word boundaries to match complete words only
        String wordBoundaryPattern = "\\b" + Pattern.quote(token) + "\\b";
        if (!Pattern.compile(wordBoundaryPattern, Pattern.CASE_INSENSITIVE).matcher(normalizedPath).find()) {
          allTokensPresent = false;
          break;
        }
      }

      if (allTokensPresent) {
        // Verify the tokens appear in order (allows words between them)
        StringBuilder orderedRegex = new StringBuilder();
        for (int i = 0; i < keyTokens.length; i++) {
          orderedRegex.append("\\b").append(Pattern.quote(keyTokens[i])).append("\\b");
          if (i < keyTokens.length - 1) {
            orderedRegex.append(".*?"); // non-greedy match allowing any text between tokens
          }
        }
        Pattern orderedPattern = Pattern.compile(orderedRegex.toString(), Pattern.CASE_INSENSITIVE);
        if (orderedPattern.matcher(normalizedPath).find()) {
          return profileKey;
        }
      }
    }

    // Third pass: Fallback to Levenshtein distance for fuzzy matching
    int bestDistance = Integer.MAX_VALUE;
    String bestKey = "";
    for (String profileKey : sortedStrings) {
      String normalizedKey = profileKey.toLowerCase().replaceAll("[_\\W]+", " ").trim();
      int dist = levenshteinDistance(normalizedPath, normalizedKey);
      // Prefer shorter distance; if equal distance, prefer longer key (more specific match)
      if (dist < bestDistance || (dist == bestDistance && profileKey.length() > bestKey.length())) {
        bestDistance = dist;
        bestKey = profileKey;
      }
    }

    return bestKey;
  }

  private static int levenshteinDistance(String str1, String str2) {
    int m = str1.length();
    int n = str2.length();

    // Initializing two arrays to store the current and previous row values
    int[] prevRow = new int[n + 1];
    int[] currRow = new int[n + 1];

    // Initializing the first row with increasing integers
    for (int j = 0; j <= n; j++) {
      prevRow[j] = j;
    }

    // Looping through each character of str1
    for (int i = 1; i <= m; i++) {
      // Initializing the first element of the current row with the row number
      currRow[0] = i;

      // Looping through each character of str2
      for (int j = 1; j <= n; j++) {
        // If characters are equal, no operation needed, take the diagonal value
        if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
          currRow[j] = prevRow[j - 1];
        } else {
          // If characters are not equal, find the minimum value of insert, delete, or replace
          currRow[j] = 1 + Math.min(currRow[j - 1], Math.min(prevRow[j], prevRow[j - 1]));
        }
      }

      // Update prevRow with currRow values
      prevRow = Arrays.copyOf(currRow, currRow.length);
    }

    // Return the final Levenshtein distance stored in the bottom-right corner of the matrix
    return currRow[n];
  }

  /**
   * Normalizes a standalone weapon-part definition into the direct part-object
   * shape expected by {@link WeaponPart#CODEC}.
   *
   * Accepts either the preferred direct payload or a legacy/documented wrapper of
   * the form {@code {"part": {...}}}.
   *
   * @param rawPartJson raw json read from a part resource file
   * @return a deep-copied json object containing the direct part definition
   */
  static JsonObject normalizeWeaponPartDefinitionJson(JsonObject rawPartJson) {
    JsonElement wrappedPart = rawPartJson.get("part");
    if (wrappedPart != null && wrappedPart.isJsonObject()) {
      return wrappedPart.getAsJsonObject().deepCopy();
    }
    return rawPartJson.deepCopy();
  }

  /**
   * Rewrites assembly part entries so named part references are replaced with the
   * referenced inline part definition before codec decoding.
   *
   * Unqualified part names are resolved within the same namespace as the assembly
   * resource. This pass also normalizes schema-style transform fields into the
   * existing codec field names.
   *
   * @param rawAssemblyJson raw weapon-assembly json from a resource file
   * @param assemblyNamespace namespace that owns the assembly resource
   * @param partJsonMap normalized named-part definitions keyed by qualified id
   * @return a deep-copied assembly json object with references resolved inline
   * @throws IllegalArgumentException if a part reference is invalid or missing
   */
  static JsonObject resolveWeaponAssemblyPartReferences(JsonObject rawAssemblyJson, String assemblyNamespace,
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

  /**
   * Normalizes schema-style transform objects into the field layout consumed by
   * {@code WeaponPartTransform.CODEC}.
   *
   * Supports both the documented {@code x/y/z} mapping keys and the internal
   * {@code xMap/yMap/zMap} keys.
   *
   * @param rawTransformJson raw transform json from an assembly part entry
   * @return a deep-copied transform json matching codec expectations
   */
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

  /**
   * Copies one axis-map entry from a schema-style transform object into the
   * normalized transform object.
   *
   * @param sourceTransformJson transform json using external schema keys
   * @param targetTransformJson destination json using codec keys
   * @param sourceKey source field name such as {@code x}
   * @param targetKey destination field name such as {@code xMap}
   */
  private static void copyNormalizedAxisMap(JsonObject sourceTransformJson, JsonObject targetTransformJson,
      String sourceKey, String targetKey) {
    JsonElement axisMapElement = sourceTransformJson.get(sourceKey);
    if (axisMapElement != null && axisMapElement.isJsonObject()) {
      targetTransformJson.add(targetKey, normalizeAxisMapJson(axisMapElement.getAsJsonObject()));
    }
  }

  /**
   * Normalizes a single axis-map field in-place if it exists on the provided
   * transform object.
   *
   * @param transformJson transform json being normalized
   * @param axisMapKey axis-map field name to normalize
   */
  private static void normalizeAxisMapField(JsonObject transformJson, String axisMapKey) {
    JsonElement axisMapElement = transformJson.get(axisMapKey);
    if (axisMapElement != null && axisMapElement.isJsonObject()) {
      transformJson.add(axisMapKey, normalizeAxisMapJson(axisMapElement.getAsJsonObject()));
    }
  }

  /**
   * Normalizes an individual axis-map object into the codec field names and enum
   * casing expected by {@code WeaponPartTransform.AxisMap.CODEC}.
   *
   * @param rawAxisMapJson raw axis-map json containing either {@code axis} or
   *                       {@code localAxis}
   * @return a deep-copied axis-map json ready for codec parsing
   */
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

  /**
   * Resolves a weapon-part reference to a fully qualified resource-location
   * string.
   *
   * Bare part names are interpreted in the same namespace as the assembly that
   * references them.
   *
   * @param assemblyNamespace namespace that owns the referencing assembly
   * @param partReference raw part reference from assembly json
   * @return normalized resource-location string for map lookup
   * @throws IllegalArgumentException if the reference is not a valid resource id
   */
  private static String qualifyWeaponPartReference(String assemblyNamespace, String partReference) {
    String resolvedPartReference = partReference.contains(":") ? partReference : assemblyNamespace + ":" + partReference;
    ResourceLocation resourceLocation = ResourceLocation.tryParse(resolvedPartReference);
    if (resourceLocation == null) {
      throw new IllegalArgumentException("Invalid weapon part reference: " + partReference);
    }
    return resourceLocation.toString();
  }

  /**
   * Loads named weapon-part resource files into a map of normalized json
   * definitions keyed by qualified resource id.
   *
   * Part files are validated against {@link WeaponPart#CODEC} during loading so
   * assembly resolution can safely inline them later.
   *
   * @param resourceManager resource manager used to enumerate generator data
   * @param player optional player used for surfacing user-facing load failures
   * @return map of qualified part ids to normalized part-definition json
   */
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
              info.error().ifPresent(error -> LOGGER.error("Failed to parse weapon part {}: {}", rl, error.message()));
              info.result().ifPresent(pInfo -> {
                if (partMap.containsKey(qualifiedPartName)) {
                  LOGGER.error("Duplicate weapon part name found: {}", qualifiedPartName);
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

  /**
   * Loads weapon assemblies from resources and returns a map of weapon assemblies.
   * @param resourceManager The resource manager.
   * @param player The player instance. Used for displaying error messages if resource loading fails. Can be null if not needed.
   * @return A map of String-keyed weapon assemblies.
   */
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
              info.error().ifPresent(error -> LOGGER.error("Failed to parse weapon assembly {}: {}", rl, error.message()));
              info.result().ifPresent(pInfo -> {
                if (assemblyMap.containsKey(assemblyName)) {
                  LOGGER.error("Duplicate weapon assembly name found: {}", assemblyName);
                }
                assemblyMap.put(assemblyName, pInfo);
              });
            }
          } catch (IOException | IllegalArgumentException e) {
            LOGGER.error("Failed to read weapon assembly resource {}", resource, e);
            if (player != null) {
              player.displayClientMessage(Component.translatable("skada.generate_weapon_info.error.no_generator_data"),
                  false);
            }
          }
        });
    return assemblyMap;
  }

  /**
   * Loads material info from resources and returns a map of material info.
   * @param resourceManager The resource manager.
   * @param player The player instance. Used for displaying error messages if resource loading fails. Can be null if not needed.
   * @return A map of String-keyed material info.
   */
  public static Map<String, MaterialInfo> loadMaterialInfo(ResourceManager resourceManager, @Nullable Player player) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Map<String, MaterialInfo> materialMap = new HashMap<>();
    resourceManager
          .listResources("generator_data/weapon/tier", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
          try (var reader = resource.openAsReader()) {
            String path = rl.getPath();
            if (path.startsWith("generator_data/weapon/tier/")) {
              String tierName = path.substring("generator_data/weapon/tier/".length()).replace(".json", "");
              DataResult<MaterialInfo> info = MaterialInfo.CODEC.parse(JsonOps.INSTANCE,
                  gson.fromJson(reader, JsonObject.class));
              info.result().ifPresent(tInfo -> {
                if (materialMap.containsKey(tierName)) {
                  LOGGER.error("Duplicate tier name found: {}", tierName);
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

}
