package com.cwjn.skada.damage;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.damage.WeaponStatApplicability;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.generation.attack.ElementSpread;
import com.cwjn.skada.util.KnockbackWeightResistanceUtil;
import com.cwjn.skada.CommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

import java.util.List;
import java.util.Map;

/** Exercises player melee charge, bonus, shield, and resistance behavior through real attacks. */
@GameTestHolder(Skada.MODID)
@PrefixGameTestTemplate(false)
public final class PlayerKnockbackGameTests {
    private static final String TEMPLATE = "damage_handler_5x4x5";

    /** Prevents instantiation of this GameTest collection. */
    private PlayerKnockbackGameTests() {}

    /** Exercises rapid real player attacks without clearing hurt immunity and checks the uncharged second push. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void acceptedPlayerAttacksUseOneDerivedPushAndKeepRapidHitAcceptance(GameTestHelper helper) {
        Player attacker = helper.makeMockPlayer();
        BlockPos attackerPos = helper.absolutePos(new BlockPos(1, 2, 1));
        attacker.setPos(attackerPos.getX() + 0.5, attackerPos.getY(), attackerPos.getZ() + 0.5);
        ItemStack weapon = weapon(1.0);
        attacker.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        helper.assertTrue(weapon.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                        .get(Attributes.ATTACK_KNOCKBACK).stream().anyMatch(modifier -> modifier.getAmount() == 1.0),
                "The selected attack knockback must be exposed through the vanilla attack-knockback attribute");
        attacker.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0);
        Cow target = helper.spawnWithNoFreeWill(EntityType.COW, 1, 2, 3);
        clearWeightResistance(target);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        target.getAttribute(AttackType.none().resistAttribute()).setBaseValue(0.0);

        for (int tick = 0; tick < 25; tick++) attacker.tick();
        helper.assertTrue(attacker.getAttackStrengthScale(0.5F) > 0.9F,
                "The first real attack should be fully charged");
        attacker.attack(target);
        double firstPush = target.getDeltaMovement().z;
        helper.assertTrue(firstPush > 0.45 && firstPush < 0.55,
                "One full-charge accepted hit must apply one 0.5-strength outward impulse; got " + firstPush);
        float healthAfterFirstHit = target.getHealth();

        // The player's attack damage type already bypasses hurt cooldown; do not clear invulnerableTime here.
        attacker.attack(target);
        helper.assertTrue(target.getHealth() < healthAfterFirstHit,
                "A second real player attack must remain accepted during the target hurt window");
        helper.assertTrue(Math.abs(target.getDeltaMovement().z - firstPush) < 1.0e-9,
                "The uncharged second hit must add no ordinary push");
        helper.succeed();
    }

    /** Checks sprint and enchantment bonuses, including their distinct attacker movement costs. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void sprintAndEnchantmentRulesApplyOnlyTheirOwnAttackerCost(GameTestHelper helper) {
        Player attacker = attacker(helper, 1.0);
        ItemStack weapon = attacker.getMainHandItem();
        weapon.enchant(Enchantments.KNOCKBACK, 1);
        Cow target = cow(helper, 1, 2, 3);
        fullyCharge(attacker);
        attacker.setSprinting(true);
        attacker.setDeltaMovement(1.0, 0.0, 0.0);

        attacker.attack(target);

        helper.assertTrue(Math.abs(target.getDeltaMovement().z - 1.5) < 1.0e-9,
                "Generated, Knockback I, and charged sprint components must combine once; got " + target.getDeltaMovement());
        helper.assertTrue(Math.abs(attacker.getDeltaMovement().x - 0.6) < 1.0e-9 && !attacker.isSprinting(),
                "Charged sprint bonus alone must slow and cancel sprinting");

        Player enchantOnly = attacker(helper, 0.0);
        enchantOnly.getMainHandItem().enchant(Enchantments.KNOCKBACK, 1);
        Cow enchantTarget = cow(helper, 1, 2, 3);
        fullyCharge(enchantOnly);
        enchantOnly.setDeltaMovement(1.0, 0.0, 0.0);
        enchantOnly.attack(enchantTarget);
        helper.assertTrue(Math.abs(enchantTarget.getDeltaMovement().z - 0.5) < 1.0e-9,
                "Enchantment knockback must work without generated weapon knockback; got " + enchantTarget.getDeltaMovement());
        helper.assertTrue(Math.abs(enchantOnly.getDeltaMovement().x - 1.0) < 1.0e-9,
                "Enchantment-only knockback must not slow the attacker");
        helper.succeed();
    }

    /** Checks minimum reach scaling and one application of final target resistance. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void minimumReachAndResistanceScaleOnlyTheIntendedImpulse(GameTestHelper helper) {
        Player attacker = attacker(helper, 1.0, 3.0);
        Cow target = cow(helper, 1, 2, 2);
        fullyCharge(attacker);
        attacker.attack(target);
        double reachMultiplier = CommonConfig.INEFFECTIVE_REACH_DAMAGE_MODIFIER.get();
        helper.assertTrue(Math.abs(target.getDeltaMovement().z - reachMultiplier * 0.5) < 1.0e-9,
                "Inside minimum reach must scale generated knockback by the configured multiplier; got " + target.getDeltaMovement());

        Player resistedAttacker = attacker(helper, 1.0);
        Cow resistantTarget = cow(helper, 1, 2, 3, true);
        resistantTarget.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.5);
        double effectiveResistance = resistantTarget.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        fullyCharge(resistedAttacker);
        resistedAttacker.attack(resistantTarget);
        double expectedPush = 0.5 * (1.0 - effectiveResistance);
        helper.assertTrue(Math.abs(resistantTarget.getDeltaMovement().z - expectedPush) < 1.0e-9,
                "Vanilla knockback resistance must be applied once from the final effective value "
                        + effectiveResistance + "; got " + resistantTarget.getDeltaMovement());
        helper.succeed();
    }

    /** Checks absorbed physical contact can push while rejected damage cannot. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void physicalContactCanPushAbsorbedDamageAndRejectedHurtCannotPush(GameTestHelper helper) {
        Player attacker = attacker(helper, 1.0);
        Cow absorbedTarget = cow(helper, 1, 2, 3);
        absorbedTarget.setAbsorptionAmount(20.0F);
        fullyCharge(attacker);
        attacker.attack(absorbedTarget);
        helper.assertTrue(absorbedTarget.getAbsorptionAmount() < 20.0F
                        && Math.abs(absorbedTarget.getDeltaMovement().z - 0.5) < 1.0e-9,
                "Absorption may prevent health loss while accepted contact still pushes once; absorption="
                        + absorbedTarget.getAbsorptionAmount() + ", motion=" + absorbedTarget.getDeltaMovement());

        Player rejectedAttacker = attacker(helper, 1.0);
        Cow invulnerableTarget = cow(helper, 3, 2, 3);
        invulnerableTarget.setInvulnerable(true);
        fullyCharge(rejectedAttacker);
        rejectedAttacker.attack(invulnerableTarget);
        helper.assertTrue(invulnerableTarget.getDeltaMovement().lengthSqr() == 0.0,
                "Rejected hurt must not produce an ordinary impulse");
        helper.succeed();
    }

    /** Checks ordinary shield force and vanilla grounded/airborne vertical motion handling. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void shieldBlocksReceiveOnePushAndVerticalVelocityKeepsVanillaRules(GameTestHelper helper) {
        Player attacker = attacker(helper, 1.0);
        Player blockingTarget = blockingPlayer(helper);
        BlockPos targetPos = helper.absolutePos(new BlockPos(1, 2, 3));
        blockingTarget.setPos(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        blockingTarget.setYRot(0.0F);
        blockingTarget.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        blockingTarget.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0);
        double healthBeforeBlock = blockingTarget.getHealth();
        fullyCharge(attacker);
        attacker.attack(blockingTarget);
        helper.assertTrue(blockingTarget.getHealth() == healthBeforeBlock,
                "A facing shield should prevent health loss; before=" + healthBeforeBlock + ", after=" + blockingTarget.getHealth());
        helper.assertTrue(blockingTarget.getDeltaMovement().z > 0.0,
                "An accepted ordinary shield block must receive an outward derived push; got " + blockingTarget.getDeltaMovement());
        helper.assertTrue(Math.abs(attacker.getDeltaMovement().x) < 1.0e-9
                        && Math.abs(attacker.getDeltaMovement().z) < 1.0e-9,
                "Ordinary shield contact must not retain vanilla's separate attacker counter-shove; motion="
                        + attacker.getDeltaMovement());

        Player groundedAttacker = attacker(helper, 1.0);
        Cow groundedTarget = cow(helper, 1, 2, 3);
        groundedTarget.setDeltaMovement(0.0, 1.0, 0.0);
        groundedTarget.setOnGround(true);
        fullyCharge(groundedAttacker);
        groundedAttacker.attack(groundedTarget);
        helper.assertTrue(Math.abs(groundedTarget.getDeltaMovement().y - 0.4) < 1.0e-9,
                "Grounded upward velocity must retain vanilla's 0.4 cap; got " + groundedTarget.getDeltaMovement());

        Player airborneAttacker = attacker(helper, 1.0);
        Cow airborneTarget = cow(helper, 1, 2, 3);
        airborneTarget.setDeltaMovement(0.0, 0.2, 0.0);
        airborneTarget.setOnGround(false);
        fullyCharge(airborneAttacker);
        airborneAttacker.attack(airborneTarget);
        helper.assertTrue(Math.abs(airborneTarget.getDeltaMovement().y - 0.2) < 1.0e-9,
                "Airborne targets must retain their vertical velocity; got " + airborneTarget.getDeltaMovement());
        helper.succeed();
    }

    /** Creates a configured weapon stack with the requested strike knockback. */
    private static ItemStack weapon(double knockback) {
        return weapon(knockback, 0.0);
    }

    /** Creates a configured weapon stack with custom knockback and minimum reach. */
    private static ItemStack weapon(double knockback, double minReach) {
        AttackTypeInfo profile = new AttackTypeInfo(8, 1000, minReach, 4, 1.6, 2, 0, knockback, List.of());
        WeaponInfo info = new WeaponInfo(Map.of(AttackType.strike(), profile), new ElementSpread(),
                WeaponStatApplicability.MELEE);
        ItemStack stack = new ItemStack(Items.STICK);
        stack.getOrCreateTag().put(SkadaData.WEAPON_INFO_TAG_KEY, info.toCompoundTag());
        return stack;
    }

    /** Creates a mock attacker holding the configured weapon. */
    private static Player attacker(GameTestHelper helper, double knockback) {
        return attacker(helper, knockback, 0.0);
    }

    /** Creates a mock attacker holding a weapon with the requested reach and knockback. */
    private static Player attacker(GameTestHelper helper, double knockback, double minReach) {
        Player player = helper.makeMockPlayer();
        BlockPos playerPos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon(knockback, minReach));
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0);
        return player;
    }

    /** Spawns a controlled cow and clears its weight contribution for combat measurements. */
    private static Cow cow(GameTestHelper helper, int x, int y, int z) {
        return cow(helper, x, y, z, false);
    }

    /** Spawns a controlled cow, optionally retaining its derived weight resistance. */
    private static Cow cow(GameTestHelper helper, int x, int y, int z, boolean preserveWeightResistance) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, x, y, z);
        if (!preserveWeightResistance) clearWeightResistance(cow);
        cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0);
        cow.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        cow.getAttribute(AttackType.none().resistAttribute()).setBaseValue(0.0);
        return cow;
    }

    /** Removes the cow's derived resistance modifier for an isolated attribute measurement. */
    private static void clearWeightResistance(Cow cow) {
        var resistance = cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance.getModifier(KnockbackWeightResistanceUtil.MODIFIER_ID) != null) {
            resistance.removeModifier(KnockbackWeightResistanceUtil.MODIFIER_ID);
        }
    }

    /** Advances the mock player long enough to restore full vanilla attack charge. */
    private static void fullyCharge(Player player) {
        for (int tick = 0; tick < 25; tick++) player.tick();
    }

    /** Creates a mock player whose shield callback always reports a block. */
    private static Player blockingPlayer(GameTestHelper helper) {
        return new Player(helper.getLevel(), BlockPos.ZERO, 0.0F,
                new GameProfile(UUID.randomUUID(), "skada-shield-knockback-test")) {
            /** Reports that this mock player is not a spectator. */
            @Override public boolean isSpectator() { return false; }
            /** Reports that this mock player is not in creative mode. */
            @Override public boolean isCreative() { return false; }
            /** Reports that this mock player is not the client-side local player. */
            @Override public boolean isLocalPlayer() { return false; }
            /** Reports that this mock player is actively blocking. */
            @Override public boolean isBlocking() { return true; }
            /** Treats the supplied damage source as blocked by the test shield. */
            @Override public boolean isDamageSourceBlocked(DamageSource source) { return true; }
        };
    }
}
