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
import com.cwjn.skada.data.gen.ExtraTierInfo;
import com.cwjn.skada.data.gen.WeaponProfile;
import com.cwjn.skada.data.mob.MobData;
import com.cwjn.skada.data.registry.AttackType;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
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
import java.lang.Math;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.cwjn.skada.Skada.LOGGER;
import static com.cwjn.skada.data.SkadaData.*;
import static net.minecraft.world.item.ItemStack.ATTRIBUTE_MODIFIER_FORMAT;

public abstract class Util {

  private static final Style SPACER = Style.EMPTY.withFont(rl("space"));
  private static final Style PIXEL = Style.EMPTY.withFont(rl("minimal_pixel_bitmap"));
  private static final Style PIXEL_16 = Style.EMPTY.withFont(rl("minimal_pixel_16x"));
  private static final Style PIXEL_12 = Style.EMPTY.withFont(rl("minimal_pixel_12x"));

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

  public static MutableComponent spacer(int i) {
    return Component.translatable("space." + i).withStyle(SPACER);
  }

  public static final Codec<AttributeModifier> ATTRIBUTE_MODIFIER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.DOUBLE.fieldOf("amount").forGetter(AttributeModifier::getAmount),
          Codec.INT.fieldOf("operation").forGetter(Util::getOperation)
  ).apply(instance, Util::attrMod));


  private static int getOperation(AttributeModifier mod) {
    return mod.getOperation().toValue();
  }

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
  @OnlyIn(Dist.CLIENT)
  public static MutableComponent pixelFontComponent(String s, boolean useBoldNumbers, boolean usePixel16, boolean usePixel12) {
    if ((Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
            || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv"))
            && ClientConfig.USE_MDU_FONT.get()) {
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
  @OnlyIn(Dist.CLIENT)
  public static MutableComponent pixelFontComponent(MutableComponent comp, boolean useBoldNumbers, boolean usePixel16, boolean usePixel12) {
    if ((Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
            || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv"))
            && ClientConfig.USE_MDU_FONT.get()) {
      MutableComponent retComp = Component.empty().withStyle(usePixel16 ? PIXEL_16 : usePixel12 ? PIXEL_12 : PIXEL);
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

  public static List<Component> getVanillaTooltip(Player pPlayer, ItemStack stack) {
    List<Component> list = new ArrayList<>();
    for (EquipmentSlot equipmentslot : EquipmentSlot.values()) {
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

  public static AttackType getAttackType(Player p) {
    if (p.getMainHandItem().hasTag() && p.getMainHandItem().getTag().contains(CURRENT_ATTACK_TYPE_TAG_KEY)) {
      return getAttackTypes(p)[p.getMainHandItem().getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)];
    } else {
      return AttackType.strike();
    }
  }

  public static AttackTypeInfo getAttackTypeInfo(Player p) {
    if (p.getMainHandItem().hasTag() && p.getMainHandItem().getTag().contains(CURRENT_ATTACK_TYPE_TAG_KEY)) {
      return getWeaponInfo(p).getAttackTypes().get(getAttackType(p));
    } else {
      return AttackTypeInfo.DEFAULT;
    }
  }

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

  public static AttackType getAttackType(ItemStack i) {
    if (i.hasTag() && i.getTag().contains(CURRENT_ATTACK_TYPE_TAG_KEY)) {
      return getAttackTypes(i)[i.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)];
    } else {
      return AttackType.strike();
    }
  }

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

  private static void flattenParentModifiers(EntityType<?> type, MobData mobData) {
    if (mobData.parents() == null || mobData.parents().isEmpty()) return;
    Multimap<Attribute, AttributeModifier> flattenedModifiers = ArrayListMultimap.create(mobData.extraModifiers());

    for (String parentPath : mobData.parents()) {
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

  private static Vec3 getFirstViewPlayerHandPos(Player player, boolean isLeftHand, float partialTick) {
    Minecraft mc = Minecraft.getInstance();
    double d4 = 960.0 / mc.options.fov().get();
    Vec3 vec3 = mc.gameRenderer.getMainCamera().getNearPlane().getPointOnPlane(isLeftHand ? -0.525F : 0.525F, -0.3F).scale(d4);
    return player.getEyePosition(partialTick).add(vec3);
  }

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

  public static Vec3 getMovementVector(Vec3 start, Vec3 end) {
    return end.subtract(start);
  }

  /*
   * standard function to calculate the chance of a critical fail given a chance 0.0 - 1.0 and a random number generator.
   * If the critical fail is true, reduce the item's durability by half. Item durability is calculated as the max durability minus the current damage value.
   */
  public static void rollCriticalFail(ItemStack item, double chance, RandomSource random, @NotNull ServerPlayer player) {
    if (!CommonConfig.ENABLE_CRITICAL_FAIL.get()) return;
    if (player.getAbilities().instabuild) return;
    if (random.nextDouble() < chance) item.hurtAndBreak(
            (int) ((item.getMaxDamage() - item.getDamageValue()) * (CommonConfig.CRITICAL_FAIL_DURABILITY_LOSS.get())),
            player,
            (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
  }

  /**
   * Calculates the percentage reduction in target's armour based on lethality and toughness.
   * Returns a number between 0.0 and 0.5 representing the percentage reduction in target's armour.<br>
   * L = A -> 9.09% armour reduction<br>
   * L = 2A -> 16.67% armour reduction<br>
   * L = 3A -> 23.08% armour reduction<br>
   * L = 4A -> 28.57% armour reduction<br>
   * L = 5A -> 33.33% armour reduction<br>
   * cap is 50% armour reduction at L = 10A
   *
   * @param lethality the lethality value of the attack
   * @param toughness the target's toughness
   * @param targetHP  the current health of the target
   * @return a number between 0.0 and 0.5 to be multiplied with the target's armour value
   */
  public static double percentReduc(double lethality, double toughness, double targetHP) {
    if (toughness == 0) return 0.5;
    double ratio = lethality / (10 * toughness);
    double denominator = 1 + ratio;
    return (-1 / denominator) + 1;
  }

  /**
   * Calculates the amount of damage based on lethality, armor toughness, and target HP.
   * Returns a number that is a percentage of the target's current health, which should be
   * summed with the running total of damage to be dealt.<br>
   * L = A -> 5% cHP<br>
   * L = 2A -> 10% cHP<br>
   * L = 3A -> 15% cHP<br>
   * cap is 25% cHP at L = 5A<br>
   * etc...
   *
   * @param lethality the lethality value of the attack
   * @param toughness the target's toughness
   * @param targetHP  the current health of the target
   * @return a damage value to be summed with the current damage total
   */
  public static double percentHealthDamage(double lethality, double toughness, double targetHP) {
    if (toughness == 0) return 0.25 * targetHP;
    double percentage = (0.05 * lethality) / toughness;
    return Math.min(percentage * targetHP, 0.25 * targetHP);
  }

  /**
   * Calculates a bonus damage multiplier based on lethality and toughness.
   * The formula increases the bonus as lethality increases relative to toughness.
   * The bonus damage multiplier approaches 2.0 as lethality becomes much greater than toughness,
   * and approaches 1.0 as lethality becomes much less than toughness. <br>
   * L = A -> 1.125x damage<br>
   * L = 2A -> 1.35x damage<br>
   * L = 3A -> 1.5x damage<br>
   * L = 4A -> 1.6x damage<br>
   * no need for hard cap cause of diminishing returns
   *
   * @param lethality the lethality value of the attack
   * @param toughness the target's armor toughness
   * @param targetHP  the current health of the target (unused in calculation)
   * @return the bonus damage multiplier
   */
  public static double percentBonusDamage(double lethality, double toughness, double targetHP) {
    if (lethality == 0) return 1.0;
    double exponent = (3 * toughness) / lethality;
    double denominator = Math.pow(2, exponent);
    double secondTerm = 1 / denominator;
    return 1 + secondTerm;
  }

  /**
   * Calculates the lethality value for a slash attack based on weapon properties.
   * Each property is normalized and weighted: weight (0.25), hardness (0.25), flexibility (0.5). Toughness is ignored.
   * The result is scaled to a range from 1 to 20.
   *
   * @param weight      the weapon's weight
   * @param thickness   the weapon's thickness
   * @param hardness    the weapon's material hardness
   * @param toughness   the weapon's material toughness
   * @param flexibility the weapon's material flexibility
   * @param modifier    a modifier based on the weapon's shape
   * @return the calculated lethality value for slash attack type
   */
  public static double slashLethalityCalculation(double weight, double thickness, double hardness, double toughness, double flexibility, double modifier) {
    double normWeight = Math.log(10*weight - WEAPON_WEIGHT_MINIMUM + 1);
    double normHardness = (hardness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normToughness = (toughness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normFlexibility = (flexibility - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    return 1 + 10 * (0.25 * normWeight + 0.25 * normHardness + 0.0 * normToughness + 0.5 * normFlexibility);
  }

  /**
   * Calculates the lethality value for a thrust attack based on weapon properties.
   * Weight and hardness are weighted at (0.5) each; toughness and flexibility are ignored.
   * The result is scaled to a range from 1 to 20.
   *
   * @param weight      the weapon's weight
   * @param hardness    the weapon's material hardness
   * @param toughness   the weapon's material toughness
   * @param flexibility the weapon's material flexibility
   * @return the calculated lethality value for thrust attack type
   */
  public static double thrustLethalityCalculation(double weight, double hardness, double toughness, double flexibility) {
    double normWeight = Math.log(10*weight - WEAPON_WEIGHT_MINIMUM + 1);
    double normHardness = (hardness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normToughness = (toughness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normFlexibility = (flexibility - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    return 1 + 10 * (0.5 * normWeight + 0.5 * normHardness + 0.0 * normToughness + 0.0 * normFlexibility);
  }

  /**
   * Calculates the lethality value for a strike attack based on weapon properties.
   * Weight is weighted at (0.7), toughness at (0.3); hardness and flexibility are ignored.
   * The result is scaled to a range from 1 to 20.
   *
   * @param weight      the weapon's weight
   * @param hardness    the weapon's material hardness
   * @param toughness   the weapon's material toughness
   * @param flexibility the weapon's material flexibility
   * @return the calculated lethality value for strike attack type
   */
  public static double strikeLethalityCalculation(double weight, double hardness, double toughness, double flexibility) {
    double normWeight = Math.log(10*weight - WEAPON_WEIGHT_MINIMUM + 1);
    double normHardness = (hardness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normToughness = (toughness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normFlexibility = (flexibility - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    return 1 + 10 * (0.7 * normWeight + 0.0 * normHardness + 0.3 * normToughness + 0.0 * normFlexibility);
  }

  /**
   * Calculates the accuracy for a slash attack based on weapon properties.
   * Accuracy is determined by normalized weight, hardness, toughness, and flexibility,
   * with weight and hardness weighted at 0.6 each, flexibility at -0.2, and toughness ignored.
   * The result is scaled to a range from 0.01 to 1.0.
   *
   * @param weight the weapon's weight
   * @param hardness the weapon's material hardness
   * @param toughness the weapon's material toughness
   * @param flexibility the weapon's material flexibility
   * @return the calculated accuracy value for slash attack type
   */
  public static double slashAccuracyCalculation(double weight, double hardness, double toughness, double flexibility) {
    double normWeight = Math.log(10*weight - WEAPON_WEIGHT_MINIMUM + 1);
    double normHardness = (hardness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normToughness = (toughness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normFlexibility = (flexibility - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    return 0.33 + 0.67*(0.5 * normWeight + 0.5 * normHardness + 0.0 * normToughness + 0.0 * normFlexibility);
  }

  /**
   * Calculates the accuracy for a thrust attack based on weapon properties.
   * Accuracy is determined by normalized hardness (0.8), toughness (0.5), flexibility (-0.3),
   * and ignores weight. The result is scaled to a range from 0.01 to 1.0.
   *
   * @param weight the weapon's weight
   * @param hardness the weapon's material hardness
   * @param toughness the weapon's material toughness
   * @param flexibility the weapon's material flexibility
   * @return the calculated accuracy value for thrust attack type
   */
  public static double thrustAccuracyCalculation(double weight, double hardness, double toughness, double flexibility) {
    double normWeight = Math.log(10*weight - WEAPON_WEIGHT_MINIMUM + 1);
    double normHardness = (hardness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normToughness = (toughness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normFlexibility = (flexibility - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    return 0.33 + 0.67*(0.0 * normWeight + 0.5 * normHardness + 0.5 * normToughness + 0.0 * normFlexibility);
  }

  /**
   * Calculates the accuracy for a strike attack based on weapon properties.
   * Accuracy is determined by normalized weight (1.5) and flexibility (-0.5),
   * with hardness and toughness ignored. The result is scaled to a range from 0.01 to 1.0.
   *
   * @param weight the weapon's weight
   * @param hardness the weapon's material hardness
   * @param toughness the weapon's material toughness
   * @param flexibility the weapon's material flexibility
   * @return the calculated accuracy value for strike attack type
   */
  public static double strikeAccuracyCalculation(double weight, double hardness, double toughness, double flexibility) {
    double normWeight = Math.log(10*weight - WEAPON_WEIGHT_MINIMUM + 1);
    double normHardness = (hardness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normToughness = (toughness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normFlexibility = (flexibility - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    return 0.33 + 0.67*(1.0 * normWeight + 0.0 * normHardness + 0.0 * normToughness + 0.0 * normFlexibility);
  }

  /**
   * Calculates the critical fail chance for a slash attack based on weapon properties.
   * Toughness and flexibility are weighted most heavily; weight and hardness are ignored.
   * The result is scaled to a range from 0.01 to 1.0.
   *
   * @param weight the weapon's weight
   * @param hardness the weapon's material hardness
   * @param toughness the weapon's material toughness
   * @param flexibility the weapon's material flexibility
   * @return the calculated critical fail chance for slash attack type
   */
  public static double slashCriticalFailCalculation(double weight, double hardness, double toughness, double flexibility) {
    double normWeight = Math.log(10*weight - WEAPON_WEIGHT_MINIMUM + 1);
    double normHardness = (hardness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normToughness = (toughness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normFlexibility = (flexibility - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    return 0.25 - 0.25*(0.0 * normWeight + 0.0 * normHardness + 0.5 * normToughness + 0.5 * normFlexibility);
  }

  /**
   * Calculates the critical fail chance for a thrust attack based on weapon properties.
   * Toughness is weighted most heavily, hardness is weighted negatively; weight and flexibility are ignored.
   * The result is scaled to a range from 0.01 to 1.0.
   *
   * @param weight the weapon's weight
   * @param hardness the weapon's material hardness
   * @param toughness the weapon's material toughness
   * @param flexibility the weapon's material flexibility
   * @return the calculated critical fail chance for thrust attack type
   */
  public static double thrustCriticalFailCalculation(double weight, double hardness, double toughness, double flexibility) {
    double normWeight = Math.log(10*weight - WEAPON_WEIGHT_MINIMUM + 1);
    double normHardness = (hardness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normToughness = (toughness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normFlexibility = (flexibility - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    return 0.25 - 0.25*(0.0 * normWeight + 0.0 * normHardness + 0.75 * normToughness + 0.25  * normFlexibility);
  }

  /**
   * Calculates the critical fail chance for a strike attack based on weapon properties.
   * Toughness is weighted most heavily; weight, hardness, and flexibility are ignored.
   * The result is scaled to a range from 0.01 to 1.0.
   *
   * @param weight the weapon's weight
   * @param hardness the weapon's material hardness
   * @param toughness the weapon's material toughness
   * @param flexibility the weapon's material flexibility
   * @return the calculated critical fail chance for strike attack type
   */
  public static double strikeCriticalFailCalculation(double weight, double hardness, double toughness, double flexibility) {
    double normWeight = Math.log(10*weight - WEAPON_WEIGHT_MINIMUM + 1);
    double normHardness = (hardness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normToughness = (toughness - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    double normFlexibility = (flexibility - MATERIAL_PROPERTY_MINIMUM) / (MATERIAL_PROPERTY_SOFT_CAP - MATERIAL_PROPERTY_MINIMUM);
    return 0.25 - 0.25*(0.0 * normWeight + 0.0 * normHardness + 1.0 * normToughness + 0.0 * normFlexibility);
  }

  public static void generateWeaponInfo(WeaponProfile profile, ExtraTierInfo tierInfo, boolean ignoreAttributes) {
    if (profile.attackTypes().containsKey(AttackType.slash())) {
      if (profile.edgeBevel().edgeRadius() <= 0)
        throw new IllegalArgumentException("Edge radius for Weapon Profile {} was {}. Must be greater than 0!", profile.);
      double accuracyBase = profile.edgeBevel().edgeRadius();
    }
    if (profile.attackTypes().containsKey(AttackType.thrust())) {

    }
    if (profile.attackTypes().containsKey(AttackType.strike())) {

    }
  }

}
