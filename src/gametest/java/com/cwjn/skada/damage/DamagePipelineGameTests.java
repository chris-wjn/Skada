package com.cwjn.skada.damage;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.Skada;
import com.cwjn.skada.SkadaRegistry;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.event.custom.PostMitigationEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Exercises the real damage hooks, player override and base-only elemental attacks. */
@GameTestHolder(Skada.MODID)
@PrefixGameTestTemplate(false)
public final class DamagePipelineGameTests {
    private static final String TEMPLATE = "damage_handler_5x4x5";

    private DamagePipelineGameTests() {}

    /** A real player swing can deal multiple base-only elements without physical attack damage. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void zeroAttackDamageStillDealsBaseElementsOutsideWeaponSpread(GameTestHelper helper) {
        withDeterministicCombat(() -> {
            Player attacker = player(helper);
            LivingEntity target = target(helper);
            Element heat = SkadaRegistry.HEAT.get();
            Element ender = SkadaRegistry.ENDER.get();
            setAttribute(attacker, heat.baseDamage(), 4.0);
            setAttribute(attacker, heat.affinity(), 0.5);
            setAttribute(attacker, ender.baseDamage(), 2.0);
            setAttribute(target, heat.resist(), 20.0);
            int[] calls = {0, 0};
            Consumer<LivingHurtEvent> hurt = event -> {
                if (event.getEntity() != target) return;
                calls[0]++;
                assertAmount(helper, event.getAmount(), 0.0, "Vanilla input must remain zero");
            };
            Consumer<PostMitigationEvent> post = event -> {
                if (event.getEntity() != target) return;
                calls[1]++;
                assertAmount(helper, event.getDamage().get(Element.physical()), 0.0, "No physical damage may be invented");
                assertAmount(helper, event.getDamage().get(heat), DamageMath.getDamageAfterElementalResistance(6.0, 20.0), "Heat base damage must use affinity and resistance");
                assertAmount(helper, event.getDamage().get(ender), 2.0, "An additional base-only element must be included");
            };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingHurtEvent.class, hurt);
            MinecraftForge.EVENT_BUS.addListener(post);
            float before = target.getHealth();
            try {
                attacker.attack(target);
                assertAmount(helper, before - target.getHealth(), DamageMath.getDamageAfterElementalResistance(6.0, 20.0) + 2.0, "Base-only elemental swing");
                helper.assertTrue(calls[0] == 1 && calls[1] == 1, "A player swing must calculate damage exactly once");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(hurt);
                MinecraftForge.EVENT_BUS.unregister(post);
            }
        });
        helper.succeed();
    }

    /** With neither vanilla damage nor base elemental damage, the attack gate stays closed. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void zeroAttackWithoutBaseElementsDoesNotEnterDamagePipeline(GameTestHelper helper) {
        withDeterministicCombat(() -> {
            Player attacker = player(helper);
            LivingEntity target = target(helper);
            int[] calls = {0};
            Consumer<LivingHurtEvent> hurt = event -> {
                if (event.getEntity() == target) calls[0]++;
            };
            MinecraftForge.EVENT_BUS.addListener(hurt);
            float before = target.getHealth();
            try {
                attacker.attack(target);
                assertAmount(helper, target.getHealth(), before, "An empty attack must not damage its target");
                helper.assertTrue(calls[0] == 0, "An empty attack must not enter the hurt pipeline");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(hurt);
            }
        });
        helper.succeed();
    }

    /** Even the last hurt listener can cancel a base-only attack before Skada or absorption runs. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void cancelledElementalAttackNeverRunsSkadaCalculation(GameTestHelper helper) {
        withDeterministicCombat(() -> {
            Player attacker = player(helper);
            LivingEntity target = target(helper);
            setAttribute(attacker, SkadaRegistry.HEAT.get().baseDamage(), 4.0);
            target.setAbsorptionAmount(3.0F);
            int[] calls = {0, 0, 0};
            Consumer<LivingHurtEvent> hurt = event -> {
                if (event.getEntity() != target) return;
                calls[0]++;
                event.setCanceled(true);
            };
            Consumer<PostMitigationEvent> post = event -> {
                if (event.getEntity() == target) calls[1]++;
            };
            Consumer<LivingDamageEvent> damage = event -> {
                if (event.getEntity() == target) calls[2]++;
            };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingHurtEvent.class, hurt);
            MinecraftForge.EVENT_BUS.addListener(post);
            MinecraftForge.EVENT_BUS.addListener(damage);
            float before = target.getHealth();
            try {
                attacker.attack(target);
                helper.assertTrue(calls[0] == 1 && calls[1] == 0 && calls[2] == 0, "Cancellation must skip Skada and the final damage event");
                assertAmount(helper, target.getHealth(), before, "Cancellation must preserve health");
                assertAmount(helper, target.getAbsorptionAmount(), 3.0, "Cancellation must preserve absorption");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(hurt);
                MinecraftForge.EVENT_BUS.unregister(post);
                MinecraftForge.EVENT_BUS.unregister(damage);
            }
        });
        helper.succeed();
    }

    /** Other mods may reduce vanilla damage to zero without cancelling independent base damage. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void uncancelledZeroFromHurtListenerRetainsBaseElementalDamage(GameTestHelper helper) {
        withDeterministicCombat(() -> {
            Player attacker = player(helper);
            LivingEntity target = target(helper);
            setAttribute(attacker, Attributes.ATTACK_DAMAGE, 5.0);
            setAttribute(attacker, SkadaRegistry.HEAT.get().baseDamage(), 3.0);
            Consumer<LivingHurtEvent> hurt = event -> {
                if (event.getEntity() == target) event.setAmount(0.0F);
            };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingHurtEvent.class, hurt);
            float before = target.getHealth();
            try {
                attacker.attack(target);
                assertAmount(helper, before - target.getHealth(), 3.0, "Uncancelled zero must retain the attacker's base damage");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(hurt);
            }
        });
        helper.succeed();
    }

    /** Living-entity mitigation runs after all hurt listeners and before absorption and final hooks. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void mobDamagePreservesEventOrderAndAppliesMitigationOnce(GameTestHelper helper) {
        withDeterministicCombat(() -> assertDamageStages(helper, target(helper)));
        helper.succeed();
    }

    /** The independent player damage method must have the same replacement and event ordering. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void playerDamagePreservesEventOrderAndAppliesMitigationOnce(GameTestHelper helper) {
        withDeterministicCombat(() -> assertDamageStages(helper, player(helper)));
        helper.succeed();
    }

    /** A zero result must return before absorption and final hooks can revive a mob hit. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void zeroMitigatedDamageStopsMobDamagePipeline(GameTestHelper helper) {
        withDeterministicCombat(() -> assertZeroDamageStops(helper, target(helper)));
        helper.succeed();
    }

    /** A zero result must also stop the separate player damage method. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void zeroMitigatedDamageStopsPlayerDamagePipeline(GameTestHelper helper) {
        withDeterministicCombat(() -> assertZeroDamageStops(helper, player(helper)));
        helper.succeed();
    }

    /** Verifies mutable extension points, absorption, potion/enchantment handling and durability. */
    private static void assertDamageStages(GameTestHelper helper, LivingEntity target) {
        ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);
        chestplate.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
        target.setItemSlot(EquipmentSlot.CHEST, chestplate);
        setAttribute(target, Attributes.ARMOR, 20.0);
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
        target.setAbsorptionAmount(3.0F);
        List<String> stages = new ArrayList<>();
        double expectedMitigated = DamageMath.getDamageAfterArmourReduction(CombatRules.getDamageAfterMagicAbsorb(8.0F, 4.0F), 20.0);
        Consumer<LivingHurtEvent> hurt = event -> {
            if (event.getEntity() != target) return;
            stages.add("hurt");
            assertAmount(helper, event.getAmount(), 1.0, "Hurt listeners must see the original input");
            event.setAmount(10.0F);
        };
        Consumer<PostMitigationEvent> post = event -> {
            if (event.getEntity() != target) return;
            stages.add("post");
            assertAmount(helper, event.getTotalDamage(), expectedMitigated, "Skada must use the last hurt listener's amount and mitigate once");
            event.getDamage().put(Element.physical(), 8.0F);
        };
        Consumer<LivingDamageEvent> damage = event -> {
            if (event.getEntity() != target) return;
            stages.add("damage");
            assertAmount(helper, event.getAmount(), 5.0, "Final hooks must see mutable Skada damage after absorption");
            event.setAmount(4.0F);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingHurtEvent.class, hurt);
        MinecraftForge.EVENT_BUS.addListener(post);
        MinecraftForge.EVENT_BUS.addListener(damage);
        float before = target.getHealth();
        try {
            target.hurt(target.damageSources().generic(), 1.0F);
            helper.assertTrue(stages.equals(List.of("hurt", "post", "damage")), "Expected each damage stage exactly once, got " + stages);
            assertAmount(helper, before - target.getHealth(), 4.0, "Final damage modifications must affect health");
            assertAmount(helper, target.getAbsorptionAmount(), 0.0, "Absorption must be consumed exactly once");
            helper.assertTrue(chestplate.getDamageValue() == 0, "The existing suppression of ordinary armour durability must remain");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(hurt);
            MinecraftForge.EVENT_BUS.unregister(post);
            MinecraftForge.EVENT_BUS.unregister(damage);
        }
    }

    /** Distinguishes a zero Skada result from a zero input with independent elemental damage. */
    private static void assertZeroDamageStops(GameTestHelper helper, LivingEntity target) {
        target.setAbsorptionAmount(4.0F);
        int[] calls = {0, 0};
        Consumer<PostMitigationEvent> post = event -> {
            if (event.getEntity() != target) return;
            calls[0]++;
            event.getDamage().replaceAll((element, amount) -> 0.0F);
        };
        Consumer<LivingDamageEvent> damage = event -> {
            if (event.getEntity() == target) {
                calls[1]++;
                event.setAmount(10.0F);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(post);
        MinecraftForge.EVENT_BUS.addListener(damage);
        float before = target.getHealth();
        try {
            target.hurt(target.damageSources().generic(), 5.0F);
            helper.assertTrue(calls[0] == 1 && calls[1] == 0, "A zero calculation must stop before the final damage event");
            assertAmount(helper, target.getHealth(), before, "A zero result must preserve health");
            assertAmount(helper, target.getAbsorptionAmount(), 4.0, "A zero result must preserve absorption");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(post);
            MinecraftForge.EVENT_BUS.unregister(damage);
        }
    }

    /** Creates an unarmed player with explicitly zero vanilla and Skada base damage. */
    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        prepareEntity(player);
        setAttribute(player, Attributes.ATTACK_DAMAGE, 0.0);
        return player;
    }

    /** Creates a durable target with no resource-derived combat modifiers. */
    private static LivingEntity target(GameTestHelper helper) {
        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.COW, 3, 1, 3);
        prepareEntity(target);
        return target;
    }

    /** Removes unrelated combat tuning so tests exercise hook semantics deterministically. */
    private static void prepareEntity(LivingEntity entity) {
        for (Element element : SkadaData.REGISTRY_ELEMENT.get().getValues()) {
            setAttribute(entity, element.baseDamage(), 0.0);
            setAttribute(entity, element.affinity(), 0.0);
            setAttribute(entity, element.resist(), 0.0);
        }
        for (AttackType type : SkadaData.REGISTRY_ATTACK_TYPE.get().getValues()) {
            setAttribute(entity, type.resistAttribute(), 0.0);
        }
        setAttribute(entity, Attributes.ARMOR_TOUGHNESS, 0.0);
        setAttribute(entity, Attributes.MAX_HEALTH, 100.0);
        entity.setHealth(100.0F);
        entity.invulnerableTime = 0;
    }

    /** Sets an attribute after removing item and resource modifiers. */
    private static void setAttribute(LivingEntity entity, Attribute attribute, double amount) {
        AttributeInstance instance = Objects.requireNonNull(entity.getAttribute(attribute));
        for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) instance.removeModifier(modifier);
        instance.setBaseValue(amount);
    }

    /** Allows float rounding without hiding a changed damage stage or duplicated calculation. */
    private static void assertAmount(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.02, message + "; expected " + expected + " but was " + actual);
    }

    /** Disables random precision and lethality only for the synchronous action under test. */
    private static void withDeterministicCombat(Runnable action) {
        boolean precision = CommonConfig.ENABLE_PRECISION.get();
        boolean meleePrecision = CommonConfig.ENABLE_PRECISION_FOR_MELEE.get();
        boolean lethality = CommonConfig.ENABLE_LETHALITY.get();
        CommonConfig.ENABLE_PRECISION.set(false);
        CommonConfig.ENABLE_PRECISION_FOR_MELEE.set(false);
        CommonConfig.ENABLE_LETHALITY.set(false);
        try {
            action.run();
        } finally {
            CommonConfig.ENABLE_PRECISION.set(precision);
            CommonConfig.ENABLE_PRECISION_FOR_MELEE.set(meleePrecision);
            CommonConfig.ENABLE_LETHALITY.set(lethality);
        }
    }
}
