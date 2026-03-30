package com.cwjn.skada.util;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.Skada;
import com.cwjn.skada.client.hud.ReticleShape;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.armour.AccessArmourInfo;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.damage.AccessWeaponInfo;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.gen.armour.ArmourConstructionInfo;
import com.cwjn.skada.data.gen.armour.ArmourPieceInfo;
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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
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
import net.minecraft.world.phys.Vec3;
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

  public static double getDamageModifierForItem(Item item) {
    return item.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream()
            .filter(modifier -> modifier.getId().equals(SkadaData.BASE_ATTACK_DAMAGE_UUID))
            .mapToDouble(AttributeModifier::getAmount)
            .findFirst()
            .orElse(0.0);
  }

  public static double getAttackSpeedForItem(Item item) {
    return Attributes.ATTACK_SPEED.getDefaultValue() + item.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_SPEED).stream()
            .filter(modifier -> modifier.getId().equals(SkadaData.BASE_ATTACK_SPEED_UUID))
            .mapToDouble(AttributeModifier::getAmount)
            .findFirst()
            .orElse(0.0);
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

}
