package com.cwjn.skada.data.mob;

import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BestiaryEntryViewBuilder {

  private static final Map<String, Integer> RESISTANCE_SORT_ORDER = createResistanceSortOrder();

  private BestiaryEntryViewBuilder() {
  }

  public static BestiaryEntryView build(EntityType<?> entityType, MobData mobData, @Nullable BestiaryInfo manualInfo) {
    return build(entityType, mobData, manualInfo, BestiaryEntryViewBuilder::resolveVanillaBaseValue);
  }

  static BestiaryEntryView build(EntityType<?> entityType,
                                 MobData mobData,
                                 @Nullable BestiaryInfo manualInfo,
                                 AttributeBaseValueResolver baseValueResolver) {
    ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
    if (entityId == null) {
      throw new IllegalArgumentException("Entity type is not registered: " + entityType);
    }

    List<BestiaryEntryView.StatRow> statRows = buildStatRows(entityType, mobData, baseValueResolver);
    List<BestiaryEntryView.ResistanceRow> resistanceRows = buildResistanceRows(entityType, mobData, baseValueResolver);
    BestiaryInfo resolvedInfo = manualInfo == null
      ? new BestiaryInfo(List.of(), List.of(), BestiaryPreviewSettings.DEFAULT)
        : manualInfo;

    return new BestiaryEntryView(
        entityId,
        entityType.getDescriptionId(),
        attackTypeKey(mobData),
        statRows,
        resistanceRows,
        resolvedInfo.description(),
      resolvedInfo.tags(),
        resolvedInfo.preview());
  }

  private static List<BestiaryEntryView.StatRow> buildStatRows(EntityType<?> entityType,
                                                               MobData mobData,
                                                               AttributeBaseValueResolver baseValueResolver) {
    List<BestiaryEntryView.StatRow> rows = new ArrayList<>();
    addStatRow(rows, entityType, mobData, Attributes.MAX_HEALTH, true, baseValueResolver);
    addStatRow(rows, entityType, mobData, Attributes.ARMOR, false, baseValueResolver);
    addStatRow(rows, entityType, mobData, Attributes.ARMOR_TOUGHNESS, false, baseValueResolver);
    return rows;
  }

  private static List<BestiaryEntryView.ResistanceRow> buildResistanceRows(EntityType<?> entityType,
                                                                           MobData mobData,
                                                                           AttributeBaseValueResolver baseValueResolver) {
    List<Attribute> resistanceAttributes = mobData.extraModifiers().keySet().stream()
        .filter(BestiaryEntryViewBuilder::isResistanceAttribute)
        .sorted(Comparator
            .comparingInt(BestiaryEntryViewBuilder::resistanceSortIndex)
            .thenComparing(Attribute::getDescriptionId))
        .toList();

    List<BestiaryEntryView.ResistanceRow> rows = new ArrayList<>();
    for (Attribute attribute : resistanceAttributes) {
      double value = resolveAttributeValue(entityType, mobData.extraModifiers(), attribute, baseValueResolver);
      if (isEffectivelyZero(value)) {
        continue;
      }
      rows.add(new BestiaryEntryView.ResistanceRow(attribute.getDescriptionId(), value, formatValue(value)));
    }
    return rows;
  }

  private static void addStatRow(List<BestiaryEntryView.StatRow> rows,
                                 EntityType<?> entityType,
                                 MobData mobData,
                                 Attribute attribute,
                                 boolean includeZero,
                                 AttributeBaseValueResolver baseValueResolver) {
    double value = resolveAttributeValue(entityType, mobData.extraModifiers(), attribute, baseValueResolver);
    if (!includeZero && isEffectivelyZero(value)) {
      return;
    }
    rows.add(new BestiaryEntryView.StatRow(attribute.getDescriptionId(), value, formatValue(value)));
  }

  private static double resolveAttributeValue(EntityType<?> entityType,
                                              Multimap<Attribute, AttributeModifier> modifiers,
                                              Attribute attribute,
                                              AttributeBaseValueResolver baseValueResolver) {
    AttributeInstance instance = new AttributeInstance(attribute, ignored -> {
    });
    instance.setBaseValue(baseValueResolver.resolve(entityType, attribute));
    for (AttributeModifier modifier : modifiers.get(attribute)) {
      instance.addTransientModifier(modifier);
    }
    return instance.getValue();
  }

  private static double resolveVanillaBaseValue(EntityType<?> entityType, Attribute attribute) {
    if (DefaultAttributes.hasSupplier(entityType)) {
      @SuppressWarnings("unchecked")
      EntityType<? extends LivingEntity> livingEntityType = (EntityType<? extends LivingEntity>) entityType;
      AttributeSupplier supplier = DefaultAttributes.getSupplier(livingEntityType);
      if (supplier != null && supplier.hasAttribute(attribute)) {
        return supplier.getBaseValue(attribute);
      }
    }
    return attribute.getDefaultValue();
  }

  private static boolean isResistanceAttribute(Attribute attribute) {
    String descriptionId = attribute.getDescriptionId();
    return descriptionId.startsWith("attribute.skada.") && descriptionId.endsWith("_resist");
  }

  private static int resistanceSortIndex(Attribute attribute) {
    String descriptionId = attribute.getDescriptionId();
    int lastDot = descriptionId.lastIndexOf('.');
    String key = lastDot >= 0 ? descriptionId.substring(lastDot + 1) : descriptionId;
    return RESISTANCE_SORT_ORDER.getOrDefault(key, Integer.MAX_VALUE);
  }

  private static boolean isEffectivelyZero(double value) {
    return Math.abs(value) < 1.0e-9;
  }

  private static String attackTypeKey(MobData mobData) {
    String attackTypeName = mobData.damageInfo() == null || mobData.damageInfo().attackType() == null
      ? "strike"
      : mobData.damageInfo().attackType().name();
    return "skada.journal.player.core." + attackTypeName;
  }

  private static String formatValue(double value) {
    return BigDecimal.valueOf(value)
        .setScale(1, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString();
  }

  private static Map<String, Integer> createResistanceSortOrder() {
    Map<String, Integer> order = new HashMap<>();
    order.put("slash_resist", 0);
    order.put("thrust_resist", 1);
    order.put("strike_resist", 2);
    order.put("heat_resist", 3);
    order.put("cold_resist", 4);
    order.put("lightning_resist", 5);
    order.put("ender_resist", 6);
    order.put("wither_resist", 7);
    order.put("aether_resist", 8);
    return order;
  }

  @FunctionalInterface
  interface AttributeBaseValueResolver {
    double resolve(EntityType<?> entityType, Attribute attribute);
  }
}