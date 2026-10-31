package com.cwjn.skada.client.gui.tooltip;

import com.cwjn.skada.client.gui.StatHelp;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AttackSelection;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.generation.attack.ElementSpread;
import com.cwjn.skada.util.Util;
import com.google.common.collect.HashMultimap;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;

/** Weapon handling, damage breakdown and input hints in the shared equipment layout. */
@OnlyIn(Dist.CLIENT)
public class ClientWeaponTooltipComponent extends EquipmentTooltipComponent {
  private static final DecimalFormat DF = new DecimalFormat("#.#");
  private static final double BOW_BASE_VELOCITY = 3.0;
  private static final double CROSSBOW_BASE_VELOCITY = 3.15;
  private final HashMultimap<Attribute, AttributeModifier> mainAttributes;
  private final Player player = Minecraft.getInstance().player;

  /** Captures the selected attack without changing any equipment or combat calculations. */
  public ClientWeaponTooltipComponent(ItemStack stack) {
    super(stack);
    EquipmentSlot mainSlot = LivingEntity.getEquipmentSlotForItem(stack);
    mainAttributes = HashMultimap.create(stack.getAttributeModifiers(mainSlot));
    List<EquipmentSlot> otherSlots = Arrays.stream(EquipmentSlot.values())
        .filter(slot -> slot != mainSlot && !stack.getAttributeModifiers(slot).isEmpty()).toList();
    AttackSelection selected = AttackSelection.resolve(stack);
    AttackTypeInfo stats = selected.stats();
    boolean melee = selected.weapon().applicability().melee();
    if (melee) {
      mainAttributes.get(ForgeMod.ENTITY_REACH.get())
          .removeIf(modifier -> modifier.getId().equals(SkadaData.SKADA_ATTACK_TYPE_REACH_UUID));
      weaponStat("reach", Component.translatable("skada.tooltip.value.reach",
          DF.format(stats.minReach()), DF.format(stats.maxReach())));
    }
    weaponStat("lethality", Component.literal(DF.format(stats.lethality())));
    weaponStat("precision", Component.literal(DF.format(stats.precision())));
    if (melee) {
      mainAttributes.get(Attributes.ATTACK_KNOCKBACK)
          .removeIf(modifier -> modifier.getId().equals(SkadaData.SKADA_ATTACK_TYPE_KNOCKBACK_UUID));
      MutableComponent category = Component.translatable(StatHelp.knockbackCategoryKey(stats.knockback()));
      weaponStat("knockback", Screen.hasShiftDown()
          ? Component.translatable("skada.tooltip.value.knockback_detailed", category, Double.toString(stats.knockback()))
          : category);
    }
    weaponStat("critical_fail", Component.translatable("skada.tooltip.value.percent", DF.format(stats.failChance() * 100)));
    if (melee) {
      mainAttributes.get(Attributes.ATTACK_SPEED)
          .removeIf(modifier -> modifier.getId().equals(SkadaData.SKADA_ATTACK_TYPE_SPEED_UUID));
      weaponStat("attack_speed", Component.literal(DF.format(Util.round(stats.attackSpeed(), 1))));
    }
    boolean rangedDamage = false;
    if (selected.weapon().applicability().projectile()) {
      if (stack.getItem() instanceof CrossbowItem) {
        weaponStat("velocity", Component.translatable("skada.tooltip.value.velocity",
            DF.format(stats.damage() + CROSSBOW_BASE_VELOCITY)));
        rangedDamage = true;
      } else if (stack.getItem() instanceof BowItem) {
        weaponStat("velocity", Component.translatable("skada.tooltip.value.velocity",
            DF.format(stats.damage() + BOW_BASE_VELOCITY)));
        rangedDamage = true;
      }
    }
    section();
    attackDamage(selected.weapon().getSpread(), !rangedDamage);
    attributeDetails(stack, mainAttributes, otherSlots);
    if (melee || !mainAttributes.isEmpty()) hint("skada.tooltip.hint.shift_details");
    if (!otherSlots.isEmpty()) hint("skada.tooltip.hint.alt_attributes");
  }

  /** Adds a localized weapon label and the existing stat icon. */
  private void weaponStat(String name, MutableComponent value) {
    stat("skada.icon." + name, Component.translatable("skada.tooltip.label." + name), value);
  }

  /** Keeps the current damage calculation and element colours while aligning each breakdown value. */
  private void attackDamage(ElementSpread spread, boolean showFinalDamage) {
    double damage = mainAttributes.get(Attributes.ATTACK_DAMAGE).stream()
        .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
        .mapToDouble(AttributeModifier::getAmount).sum()
        + (player == null ? 1.0 : player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
    mainAttributes.get(Attributes.ATTACK_DAMAGE)
        .removeIf(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION);
    if (showFinalDamage) {
      weaponStat("attack_damage", Component.literal(DF.format(damage * spread.getPowerBudget())));
    } else {
      line(text(Component.translatable("skada.tooltip.label.element_split"), LABEL));
    }
    spread.getRatios().forEach((element, ratio) -> {
      int percent = (int) (ratio * 100 / spread.getPowerBudget());
      MutableComponent value = showFinalDamage
          ? Component.translatable("skada.tooltip.value.element_damage", percent, Util.round(ratio * damage, 1))
          : Component.translatable("skada.tooltip.value.percent", percent);
      branchStat("skada.icon.element." + element.name(),
          Component.translatable("skada.journal.player.element." + element.name()),
          value.withStyle(style -> style.withColor(element.colour() & 0xFFFFFF)));
    });
  }

}
