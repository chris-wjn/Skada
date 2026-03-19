package com.cwjn.skada.damage;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.Skada;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.ElementSpreadInstance;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Objects;

@GameTestHolder(Skada.MODID)
public final class DamageHandlerGameTests {

  private static final float BASE_DAMAGE = 10.0F;
  private static final double DAMAGE_TOLERANCE = 0.05;
  private static final double SLASH_RESISTANCE = 10.0D;
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
  public static void armourReductionAppliesInHurtPipeline(GameTestHelper helper) {
    withDeterministicCombatConfig(() -> {
      Zombie target = spawnTarget(helper);
      setAttribute(target, Attributes.ARMOR, 20.0D);
      float startingHealth = target.getHealth();
      double expectedDamage = DamageMath.getDamageAfterArmourReduction(BASE_DAMAGE, 20.0D);

      helper.assertTrue(target.hurt(environmentalSource(helper), BASE_DAMAGE), "Expected armoured hurt call to succeed");
      assertHealthLoss(helper, target, startingHealth, expectedDamage, "armour reduction");
    });
    helper.succeed();
  }

  @PrefixGameTestTemplate(false)
  @GameTest(templateNamespace = Skada.MODID, template = TEST_TEMPLATE)
  public static void elementalResistanceReducesPhysicalDamage(GameTestHelper helper) {
    withDeterministicCombatConfig(() -> {
      Zombie target = spawnTarget(helper);
      setAttribute(target, Element.basic().resist(), 1.0D);
      float startingHealth = target.getHealth();
      double expectedDamage = DamageMath.getDamageAfterElementalResistance(BASE_DAMAGE, 1.0D);

      helper.assertTrue(target.hurt(environmentalSource(helper), BASE_DAMAGE), "Expected elemental hurt call to succeed");
      assertHealthLoss(helper, target, startingHealth, expectedDamage, "elemental resistance");
    });
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
    clearAttribute(target, Element.basic().resist());
    clearAttribute(target, Element.basic().affinity());
    clearAttribute(target, Element.basic().baseDamage());
    setAttribute(target, Attributes.ARMOR, 0.0D);
    setAttribute(target, Attributes.ARMOR_TOUGHNESS, 0.0D);
    setAttribute(target, AttackType.none().resistAttribute(), 0.0D);
    setAttribute(target, AttackType.slash().resistAttribute(), 0.0D);
    setAttribute(target, Element.basic().resist(), 0.0D);
    setAttribute(target, Element.basic().affinity(), 0.0D);
    setAttribute(target, Element.basic().baseDamage(), 0.0D);
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
    return new SkadaDamageSource(damageTypeHolder(helper), DamageInfo.environmental(new ElementSpreadInstance(1.0D)));
  }

  private static SkadaDamageSource slashSource(GameTestHelper helper) {
    DamageInfo info = new DamageInfo(0.0D, 0.0D, false, AttackType.slash(), new ElementSpreadInstance(1.0D));
    return new SkadaDamageSource(damageTypeHolder(helper), info);
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