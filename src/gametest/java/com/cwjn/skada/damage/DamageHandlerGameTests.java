package com.cwjn.skada.damage;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.Skada;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.ElementSpreadInstance;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.damage.WeaponStatApplicability;
import com.cwjn.skada.generation.attack.ElementSpread;
import com.cwjn.skada.data.mob.MobData;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilData;
import com.google.common.collect.ArrayListMultimap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@GameTestHolder(Skada.MODID)
public final class DamageHandlerGameTests {

  private static final float BASE_DAMAGE = 10.0F;
  private static final double DAMAGE_TOLERANCE = 0.05;
  private static final double SLASH_RESISTANCE = 10.0D;
  private static final double ARMOUR_VALUE = 20.0D;
  private static final double SPLIT_DAMAGE_RATIO = 0.5D;
  private static final String TEST_TEMPLATE = "damage_handler_5x4x5";

  private DamageHandlerGameTests() {
  }

  @PrefixGameTestTemplate(false)
  @GameTest(templateNamespace = Skada.MODID, template = TEST_TEMPLATE)
  public static void baselineEnvironmentalDamageMatchesInput(GameTestHelper helper) {
    withDeterministicCombatConfig(() -> {
      Zombie target = spawnTarget(helper);
      float startingHealth = target.getHealth();

      helper.assertTrue(target.hurt(environmentalSource(helper), BASE_DAMAGE), "Expected environmental hurt call to succeed");
      assertHealthLoss(helper, target, startingHealth, BASE_DAMAGE, "baseline environmental damage");
    });
    helper.succeed();
  }

  @PrefixGameTestTemplate(false)
  @GameTest(templateNamespace = Skada.MODID, template = TEST_TEMPLATE)
  public static void attackTypeResistanceReducesDamage(GameTestHelper helper) {
    withDeterministicCombatConfig(() -> {
      Zombie target = spawnTarget(helper);
      setAttribute(target, AttackType.slash().resistAttribute(), SLASH_RESISTANCE);
      float startingHealth = target.getHealth();
      double expectedDamage = DamageMath.getDamageAfterAttackTypeReduction(BASE_DAMAGE, SLASH_RESISTANCE);

      helper.assertTrue(target.hurt(slashSource(helper), BASE_DAMAGE), "Expected slash hurt call to succeed");
      assertHealthLoss(helper, target, startingHealth, expectedDamage, "attack type resistance");
    });
    helper.succeed();
  }

  @PrefixGameTestTemplate(false)
  @GameTest(templateNamespace = Skada.MODID, template = TEST_TEMPLATE)
  public static void mobConfiguredDirectDamageUsesSkadaAttackTypeMitigation(GameTestHelper helper) {
    withDeterministicCombatConfig(() -> {
      Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 2);
      Zombie target = spawnTarget(helper);
      MobData mobData = new MobData(null,
        new DamageInfo(2.0D, 0.75D, false, AttackType.slash(), physicalOnlySpread()),
        ArrayListMultimap.create());
      float startingHealth = target.getHealth();
      double expectedDamage = DamageMath.getDamageAfterAttackTypeReduction(BASE_DAMAGE, SLASH_RESISTANCE);

      setAttribute(target, AttackType.slash().resistAttribute(), SLASH_RESISTANCE);
      SkadaDamageSource source = directMobSource(helper, attacker, mobData);

      helper.assertTrue(!source.getInfo().isEnvironmental(), "Expected mob direct attack source to be non-environmental");
      helper.assertTrue(target.hurt(source, BASE_DAMAGE), "Expected mob-configured slash hurt call to succeed");
      assertHealthLoss(helper, target, startingHealth, expectedDamage, "mob-configured attack type resistance");
    });
    helper.succeed();
  }

  @PrefixGameTestTemplate(false)
  @GameTest(templateNamespace = Skada.MODID, template = TEST_TEMPLATE)
  public static void mobHeldWeaponDataOverridesMobConfiguredFallback(GameTestHelper helper) {
    withDeterministicCombatConfig(() -> {
      Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 2);
      Zombie target = spawnTarget(helper);
      ItemStack weaponStack = new ItemStack(Items.STICK);
      weaponStack.getOrCreateTag().put(SkadaData.WEAPON_INFO_TAG_KEY, weaponInfo(4.5D, 2.25D, AttackType.thrust()).toCompoundTag());
      attacker.setItemSlot(EquipmentSlot.MAINHAND, weaponStack);

      MobData mobData = new MobData(null,
        new DamageInfo(2.0D, 0.75D, false, AttackType.strike(), physicalOnlySpread()),
        ArrayListMultimap.create());
      float startingHealth = target.getHealth();
      SkadaDamageSource source = directMobSource(helper, attacker, mobData);

      setAttribute(target, AttackType.strike().resistAttribute(), SLASH_RESISTANCE);
      setAttribute(target, AttackType.thrust().resistAttribute(), 0.0D);

      helper.assertTrue(!source.getInfo().isEnvironmental(), "Expected held-weapon mob source to be non-environmental");
      helper.assertTrue(source.getInfo().attackType() == AttackType.thrust(), "Expected held weapon attack type to override mob fallback");
      helper.assertTrue(target.hurt(source, BASE_DAMAGE), "Expected held-weapon mob hurt call to succeed");
      assertHealthLoss(helper, target, startingHealth, BASE_DAMAGE, "held weapon precedence over mob fallback");
    });
    helper.succeed();
  }

  @PrefixGameTestTemplate(false)
  @GameTest(templateNamespace = Skada.MODID, template = TEST_TEMPLATE)
  public static void armourReductionAppliesInHurtPipeline(GameTestHelper helper) {
    withDeterministicCombatConfig(() -> {
      Zombie target = spawnTarget(helper);
      setAttribute(target, Attributes.ARMOR, ARMOUR_VALUE);
      float startingHealth = target.getHealth();
      double expectedDamage = DamageMath.getDamageAfterArmourReduction(BASE_DAMAGE, ARMOUR_VALUE);

      helper.assertTrue(target.hurt(environmentalSource(helper), BASE_DAMAGE), "Expected armoured hurt call to succeed");
      assertHealthLoss(helper, target, startingHealth, expectedDamage, "armour reduction");
    });
    helper.succeed();
  }

  @PrefixGameTestTemplate(false)
  @GameTest(templateNamespace = Skada.MODID, template = TEST_TEMPLATE)
  public static void armourOnlyReducesPhysicalDamage(GameTestHelper helper) {
    withDeterministicCombatConfig(() -> {
      Zombie target = spawnTarget(helper);
      Element heat = requiredElement("heat");
      setAttribute(target, Attributes.ARMOR, ARMOUR_VALUE);
      clearAttribute(target, heat.resist());
      setAttribute(target, heat.resist(), 0.0D);
      float startingHealth = target.getHealth();
      double expectedDamage = DamageMath.getDamageAfterArmourReduction(BASE_DAMAGE * SPLIT_DAMAGE_RATIO, ARMOUR_VALUE)
        + (BASE_DAMAGE * SPLIT_DAMAGE_RATIO);

      helper.assertTrue(target.hurt(environmentalSource(helper, physicalAndHeatSpread()), BASE_DAMAGE), "Expected mixed-element hurt call to succeed");
      assertHealthLoss(helper, target, startingHealth, expectedDamage, "physical-only armour mitigation");
    });
    helper.succeed();
  }

  @PrefixGameTestTemplate(false)
  @GameTest(templateNamespace = Skada.MODID, template = TEST_TEMPLATE)
  public static void nonPhysicalElementalResistanceUsesArmourScaleCurve(GameTestHelper helper) {
    withDeterministicCombatConfig(() -> {
      Zombie target = spawnTarget(helper);
      Element heat = requiredElement("heat");
      setAttribute(target, Attributes.ARMOR, ARMOUR_VALUE);
      setAttribute(target, heat.resist(), 20.0D);
      float startingHealth = target.getHealth();
      double expectedDamage = DamageMath.getDamageAfterArmourReduction(BASE_DAMAGE * SPLIT_DAMAGE_RATIO, ARMOUR_VALUE)
        + DamageMath.getDamageAfterElementalResistance(BASE_DAMAGE * SPLIT_DAMAGE_RATIO, 20.0D);

      helper.assertTrue(target.hurt(environmentalSource(helper, physicalAndHeatSpread()), BASE_DAMAGE), "Expected mixed armour and heat hurt call to succeed");
      assertHealthLoss(helper, target, startingHealth, expectedDamage, "mixed physical and elemental mitigation");
    });
    helper.succeed();
  }

  @PrefixGameTestTemplate(false)
  @GameTest(templateNamespace = Skada.MODID, template = TEST_TEMPLATE)
  public static void physicalElementResistanceUsesArmourAttribute(GameTestHelper helper) {
    helper.assertTrue(Element.physical().usesArmourAsResistance(), "Expected physical resistance to use armour");
    helper.assertTrue(Element.physical().resist() == Attributes.ARMOR, "Expected physical resistance attribute to resolve to armour");
    helper.succeed();
  }

  private static Zombie spawnTarget(GameTestHelper helper) {
    buildStonePad(helper);
    helper.killAllEntities();

    Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 1, 2);
    target.setHealth(target.getMaxHealth());
    target.invulnerableTime = 0;
    clearAttribute(target, Attributes.ARMOR);
    clearAttribute(target, Attributes.ARMOR_TOUGHNESS);
    clearAttribute(target, AttackType.none().resistAttribute());
    clearAttribute(target, AttackType.slash().resistAttribute());
    if (!Element.physical().usesArmourAsResistance()) {
      clearAttribute(target, Element.physical().resist());
    }
    clearAttribute(target, Element.physical().affinity());
    clearAttribute(target, Element.physical().baseDamage());
    clearAttribute(target, requiredElement("heat").resist());
    setAttribute(target, Attributes.ARMOR, 0.0D);
    setAttribute(target, Attributes.ARMOR_TOUGHNESS, 0.0D);
    setAttribute(target, AttackType.none().resistAttribute(), 0.0D);
    setAttribute(target, AttackType.slash().resistAttribute(), 0.0D);
    if (!Element.physical().usesArmourAsResistance()) {
      setAttribute(target, Element.physical().resist(), 0.0D);
    }
    setAttribute(target, Element.physical().affinity(), 0.0D);
    setAttribute(target, Element.physical().baseDamage(), 0.0D);
    setAttribute(target, requiredElement("heat").resist(), 0.0D);
    return target;
  }

  private static void buildStonePad(GameTestHelper helper) {
    for (int x = 1; x <= 3; x++) {
      for (int z = 1; z <= 3; z++) {
        helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
      }
    }
  }

  private static void assertHealthLoss(GameTestHelper helper, LivingEntity target, float startingHealth, double expectedDamage, String label) {
    double actualDamage = startingHealth - target.getHealth();
    helper.assertTrue(
      Math.abs(actualDamage - expectedDamage) <= DAMAGE_TOLERANCE,
      label + " expected damage " + expectedDamage + " but was " + actualDamage
    );
  }

  private static SkadaDamageSource environmentalSource(GameTestHelper helper) {
    return environmentalSource(helper, physicalOnlySpread());
  }

  private static SkadaDamageSource environmentalSource(GameTestHelper helper, ElementSpreadInstance spread) {
    return new SkadaDamageSource(damageTypeHolder(helper), DamageInfo.environmental(spread));
  }

  private static SkadaDamageSource slashSource(GameTestHelper helper) {
    DamageInfo info = new DamageInfo(0.0D, 0.0D, false, AttackType.slash(), physicalOnlySpread());
    return new SkadaDamageSource(damageTypeHolder(helper), info);
  }

  private static SkadaDamageSource directMobSource(GameTestHelper helper, LivingEntity attacker, MobData mobData) {
    return new SkadaDamageSource(damageTypeHolder(helper), attacker, UtilData.getMobDirectAttackDamageInfo(attacker, mobData));
  }

  private static WeaponInfo weaponInfo(double lethality, double precision, AttackType attackType) {
    return new WeaponInfo(
      Map.of(attackType, AttackTypeInfo.of(lethality, precision, 0.0D, 3.0D, 1.6D, 2.0D, 0.0D, List.of())),
      new ElementSpread(1.0D, Map.of(Element.physical(), 1.0D)),
      WeaponStatApplicability.MELEE);
  }

  private static ElementSpreadInstance physicalOnlySpread() {
    ElementSpreadInstance spread = new ElementSpreadInstance();
    spread.addRatio(Element.physical(), 1.0D);
    return spread;
  }

  private static ElementSpreadInstance physicalAndHeatSpread() {
    ElementSpreadInstance spread = new ElementSpreadInstance();
    spread.addRatio(Element.physical(), SPLIT_DAMAGE_RATIO);
    spread.addRatio(requiredElement("heat"), SPLIT_DAMAGE_RATIO);
    return spread;
  }

  private static Element requiredElement(String elementId) {
    return Objects.requireNonNull(SkadaData.REGISTRY_ELEMENT.get().getValue(Util.rl(elementId)), () -> "Missing element " + elementId);
  }

  private static Holder<DamageType> damageTypeHolder(GameTestHelper helper) {
    return helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC);
  }

  private static void setAttribute(LivingEntity entity, Attribute attribute, double value) {
    AttributeInstance instance = Objects.requireNonNull(entity.getAttribute(attribute), () -> "Missing attribute " + attribute.getDescriptionId());
    instance.setBaseValue(value);
  }

  private static void clearAttribute(LivingEntity entity, Attribute attribute) {
    AttributeInstance instance = Objects.requireNonNull(entity.getAttribute(attribute), () -> "Missing attribute " + attribute.getDescriptionId());
    for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
      instance.removeModifier(modifier);
    }
  }

  private static void withDeterministicCombatConfig(Runnable action) {
    boolean originalPrecision = CommonConfig.ENABLE_PRECISION.get();
    boolean originalPrecisionForMelee = CommonConfig.ENABLE_PRECISION_FOR_MELEE.get();
    boolean originalLethality = CommonConfig.ENABLE_LETHALITY.get();

    CommonConfig.ENABLE_PRECISION.set(false);
    CommonConfig.ENABLE_PRECISION_FOR_MELEE.set(false);
    CommonConfig.ENABLE_LETHALITY.set(false);

    try {
      action.run();
    } finally {
      CommonConfig.ENABLE_PRECISION.set(originalPrecision);
      CommonConfig.ENABLE_PRECISION_FOR_MELEE.set(originalPrecisionForMelee);
      CommonConfig.ENABLE_LETHALITY.set(originalLethality);
    }
  }
}