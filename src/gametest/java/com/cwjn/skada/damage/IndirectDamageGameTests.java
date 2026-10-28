package com.cwjn.skada.damage;

import com.cwjn.skada.Skada;
import com.cwjn.skada.SkadaRegistry;
import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.ElementSpreadInstance;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Exercises indirect identity and its damage/statistic consequences with the real hurt pipeline. */
@GameTestHolder(Skada.MODID)
@PrefixGameTestTemplate(false)
public final class IndirectDamageGameTests {
    private static final String TEMPLATE = "damage_handler_5x4x5";

    private IndirectDamageGameTests() {}

    /** Covers both wrapper entry points with actual vanilla source factories and ownerless sources. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void wrappersPreserveVanillaDirectAndCausingEntities(GameTestHelper helper) {
        Cow shooter = helper.spawnWithNoFreeWill(EntityType.COW, 1, 1, 1);
        Arrow arrow = new Arrow(helper.getLevel(), shooter);
        ThrownTrident trident = new ThrownTrident(helper.getLevel(), shooter, new ItemStack(Items.TRIDENT));
        ThrownPotion potion = new ThrownPotion(helper.getLevel(), shooter);
        PrimedTnt tnt = new PrimedTnt(helper.getLevel(), 1, 1, 1, shooter);
        List<DamageSource> sources = List.of(
                shooter.damageSources().arrow(arrow, shooter),
                shooter.damageSources().trident(trident, shooter),
                shooter.damageSources().indirectMagic(potion, shooter),
                shooter.damageSources().explosion(tnt, shooter),
                shooter.damageSources().arrow(arrow, null),
                shooter.damageSources().mobAttack(shooter),
                shooter.damageSources().generic());

        for (DamageSource original : sources) {
            for (SkadaDamageSource wrapped : List.of(
                    new SkadaDamageSource(original, DamageInfo.environmental(spread(Element.physical()))),
                    SkadaDamageSource.environmental(original))) {
                helper.assertTrue(wrapped.getDirectEntity() == original.getDirectEntity(),
                        original.getMsgId() + " must retain its immediate source");
                helper.assertTrue(wrapped.getEntity() == original.getEntity(),
                        original.getMsgId() + " must retain its causing entity, including null");
                helper.assertTrue(wrapped.typeHolder() == original.typeHolder(), "Damage type must remain unchanged");
                helper.assertTrue(Objects.equals(wrapped.getSourcePosition(), original.getSourcePosition()),
                        "Damage source position must remain unchanged");
            }
        }
        helper.succeed();
    }

    /** A projectile snapshot must still receive its living shooter's elemental contributions. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void projectileHurtUsesShooterBaseDamageAndAffinity(GameTestHelper helper) {
        Cow shooter = helper.spawnWithNoFreeWill(EntityType.COW, 1, 1, 1);
        Cow target = target(helper);
        setAttribute(shooter, Element.physical().baseDamage(), 2.0);
        setAttribute(shooter, Element.physical().affinity(), 0.5);
        Arrow arrow = new Arrow(helper.getLevel(), shooter);
        ((AccessProjectileData) arrow).setDamageInfo(
                DamageInfo.projectileFallback(AttackType.none(), 0.0, spread(Element.physical())));
        float before = target.getHealth();

        helper.assertTrue(target.hurt(shooter.damageSources().arrow(arrow, shooter), 4.0F), "Arrow hurt must succeed");

        assertDamage(helper, before - target.getHealth(), 9.0, "Arrow must apply (4 + shooter base 2) * affinity 1.5");
        helper.succeed();
    }

    /** The environmental wrapper must preserve the caster of an unsnapshotted magic projectile. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void indirectMagicHurtUsesCasterBaseDamageAndAffinity(GameTestHelper helper) {
        Cow caster = helper.spawnWithNoFreeWill(EntityType.COW, 1, 1, 1);
        Cow target = target(helper);
        Element ender = SkadaRegistry.ENDER.get();
        setAttribute(caster, ender.baseDamage(), 2.0);
        setAttribute(caster, ender.affinity(), 0.5);
        ThrownPotion potion = new ThrownPotion(helper.getLevel(), caster);
        float before = target.getHealth();

        helper.assertTrue(target.hurt(caster.damageSources().indirectMagic(potion, caster), 4.0F), "Magic hurt must succeed");

        assertDamage(helper, before - target.getHealth(), 9.0, "Magic must apply (4 + caster base 2) * affinity 1.5");
        helper.succeed();
    }

    /** An attacking player receives dealt-resisted statistics instead of victim resistance statistics. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void resistedHurtAwardsAttackerDealtResistedStatistic(GameTestHelper helper) {
        Map<Stat<?>, Integer> awarded = new HashMap<>();
        // The player need not join a network session to observe vanilla statistic callbacks.
        ServerPlayer attacker = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "skada-stat-test")) {
            @Override
            public void awardStat(Stat<?> statistic, int amount) {
                awarded.merge(statistic, amount, Integer::sum);
            }
        };
        Cow target = target(helper);
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
        float before = target.getHealth();

        helper.assertTrue(target.hurt(attacker.damageSources().playerAttack(attacker), 10.0F), "Resisted hurt must succeed");

        assertDamage(helper, before - target.getHealth(), 8.0, "Resistance I must still remove two damage");
        helper.assertTrue(awarded.getOrDefault(Stats.CUSTOM.get(Stats.DAMAGE_DEALT_RESISTED), 0) == 20,
                "Attacker must receive 20 dealt-resisted statistic units");
        helper.assertTrue(awarded.getOrDefault(Stats.CUSTOM.get(Stats.DAMAGE_RESISTED), 0) == 0,
                "Attacker must not receive the victim's damage-resisted statistic");
        helper.succeed();
    }

    /** Supplies a neutral live target so the test isolates attacker identity, not tuning policy. */
    private static Cow target(GameTestHelper helper) {
        Cow target = helper.spawnWithNoFreeWill(EntityType.COW, 3, 1, 3);
        setAttribute(target, Attributes.ARMOR, 0.0);
        setAttribute(target, AttackType.none().resistAttribute(), 0.0);
        setAttribute(target, SkadaRegistry.ENDER.get().resist(), 0.0);
        target.invulnerableTime = 0;
        return target;
    }

    /** Removes resource modifiers before assigning a deterministic attribute value. */
    private static void setAttribute(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = Objects.requireNonNull(entity.getAttribute(attribute));
        for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) instance.removeModifier(modifier);
        instance.setBaseValue(value);
    }

    /** Builds the single-channel snapshot used by the identity regression. */
    private static ElementSpreadInstance spread(Element element) {
        return new ElementSpreadInstance(Map.of(element, 1.0));
    }

    /** Compares actual health loss with the unchanged deterministic damage calculation. */
    private static void assertDamage(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.01, message + "; expected " + expected + " but was " + actual);
    }
}
