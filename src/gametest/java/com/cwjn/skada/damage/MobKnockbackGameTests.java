package com.cwjn.skada.damage;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.damage.WeaponStatApplicability;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.generation.attack.ElementSpread;
import com.cwjn.skada.util.KnockbackWeightResistanceUtil;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Exercises mob default and direct-override ordinary melee attack routes. */
@GameTestHolder(Skada.MODID)
@PrefixGameTestTemplate(false)
public final class MobKnockbackGameTests {
    private static final String TEMPLATE = "damage_handler_5x4x5";

    /** Prevents instantiation of this GameTest collection. */
    private MobKnockbackGameTests() {}

    /** Zombie overrides doHurtTarget but delegates to Mob.doHurtTarget; one accepted hit receives one derived push. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void defaultMobRouteUsesAuthoredAttackKnockbackAndRetainsRejectedHitPolicy(GameTestHelper helper) {
        Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 1);
        attacker.setNoAi(true);
        ItemStack heldWeapon = weapon(1.0);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, heldWeapon);
        var heldKnockback = heldWeapon.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_KNOCKBACK);
        helper.assertTrue(heldKnockback.size() == 1,
                "The configured held weapon must expose an ATTACK_KNOCKBACK modifier");
        var modifier = heldKnockback.iterator().next();
        var attackKnockback = attacker.getAttribute(Attributes.ATTACK_KNOCKBACK);
        if (!attackKnockback.hasModifier(modifier)) attackKnockback.addTransientModifier(modifier);
        helper.assertTrue(attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK) >= 1.0,
                "The equipped mob must resolve its held weapon modifier through vanilla ATTACK_KNOCKBACK");
        attacker.setDeltaMovement(1.0, 0.0, 0.0);
        Cow target = cow(helper, 1, 2, 3);
        target.setDeltaMovement(0.8, 0.0, 0.0);

        helper.assertTrue(attacker.doHurtTarget(target), "The first mob attack should be accepted");
        double firstPush = target.getDeltaMovement().z;
        helper.assertTrue(firstPush > 0.49 && firstPush < 0.51,
                "One derived ordinary impulse should replace the default hurt and attack shoves; got " + target.getDeltaMovement());
        helper.assertTrue(Math.abs(target.getDeltaMovement().x - 0.4) < 1.0e-9,
                "Existing horizontal velocity should be handled once by vanilla knockback; got " + target.getDeltaMovement());
        helper.assertTrue(Math.abs(attacker.getDeltaMovement().x - 1.0) < 1.0e-9,
                "A generated/held attack value must not slow the mob attacker");
        helper.assertTrue(!attacker.doHurtTarget(target), "The mob should retain its normal hurt cooldown policy");
        helper.assertTrue(Math.abs(target.getDeltaMovement().z - firstPush) < 1.0e-9,
                "A rejected repeated mob attack must not add another impulse");
        helper.succeed();
    }

    /** Direct animal overrides use the shared path without requiring a default Mob.doHurtTarget delegation. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void overridingWolfRouteUsesAttackKnockbackWithoutAttackerSlowdown(GameTestHelper helper) {
        Wolf attacker = helper.spawnWithNoFreeWill(EntityType.WOLF, 1, 2, 1);
        attacker.setNoAi(true);
        attacker.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(1.0);
        attacker.setDeltaMovement(1.0, 0.0, 0.0);
        Cow target = cow(helper, 1, 2, 3);

        helper.assertTrue(attacker.doHurtTarget(target), "Wolf's overriding direct hurt route should accept contact");
        helper.assertTrue(Math.abs(target.getDeltaMovement().z - 0.5) < 1.0e-9,
                "The overriding route should apply one bounded derived impulse; got " + target.getDeltaMovement());
        helper.assertTrue(Math.abs(attacker.getDeltaMovement().x - 1.0) < 1.0e-9,
                "A positive ordinary mob knockback value must not add attacker slowdown");
        helper.succeed();
    }

    /** Subclass side effects remain attached to the accepted route while force is applied once. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void caveSpiderDelegationPreservesPoisonAndUsesOneOrdinaryImpulse(GameTestHelper helper) {
        net.minecraft.world.Difficulty previousDifficulty = helper.getLevel().getDifficulty();
        helper.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.HARD, true);
        CaveSpider attacker = helper.spawnWithNoFreeWill(EntityType.CAVE_SPIDER, 1, 2, 1);
        attacker.setNoAi(true);
        attacker.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(1.0);
        Cow target = cow(helper, 1, 2, 3);

        helper.assertTrue(attacker.doHurtTarget(target), "Cave spider's inherited ordinary attack should be accepted");
        helper.assertTrue(target.hasEffect(net.minecraft.world.effect.MobEffects.POISON),
                "The ordinary force replacement must preserve Cave Spider poison");
        helper.assertTrue(Math.abs(target.getDeltaMovement().z - 0.5) < 1.0e-9,
                "The delegated route should apply one impulse with poison intact; got " + target.getDeltaMovement());
        helper.getLevel().getServer().setDifficulty(previousDifficulty, true);
        helper.succeed();
    }


    /** Checks resistance and ordinary shield contact through the mob attack path. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void mobResistanceAndOrdinaryShieldContactUseOneDerivedImpulse(GameTestHelper helper) {
        Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 1);
        attacker.setNoAi(true);
        attacker.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(1.0);
        Cow resistantTarget = cow(helper, 1, 2, 3);
        resistantTarget.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.5);
        double effectiveResistance = resistantTarget.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        helper.assertTrue(attacker.doHurtTarget(resistantTarget), "A resistant target should still accept the mob attack");
        double expectedPush = 0.5 * (1.0 - effectiveResistance);
        helper.assertTrue(Math.abs(resistantTarget.getDeltaMovement().z - expectedPush) < 1.0e-9,
                "Mob knockback resistance should be applied once from its final effective value "
                        + effectiveResistance + "; got " + resistantTarget.getDeltaMovement());

        Zombie shieldAttacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 1);
        shieldAttacker.setNoAi(true);
        shieldAttacker.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(1.0);
        Player shieldTarget = blockingPlayer(helper);
        BlockPos targetPos = helper.absolutePos(new BlockPos(1, 2, 3));
        shieldTarget.setPos(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        shieldTarget.setYRot(180.0F);
        shieldTarget.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        shieldTarget.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0);
        float healthBeforeBlock = shieldTarget.getHealth();
        helper.assertTrue(!shieldAttacker.doHurtTarget(shieldTarget), "Mob melee should retain vanilla blocked-hit acceptance results");
        helper.assertTrue(shieldTarget.getHealth() == healthBeforeBlock,
                "A facing shield should prevent health loss");
        helper.assertTrue(shieldTarget.getDeltaMovement().z > 0.45,
                "An ordinary mob shield block must receive one outward impulse; got " + shieldTarget.getDeltaMovement());
        helper.succeed();
    }

    /** Creates a held weapon with a configured strike knockback value. */
    private static ItemStack weapon(double knockback) {
        AttackTypeInfo profile = new AttackTypeInfo(8, 1000, 0, 4, 1.6, 2, 0, knockback, List.of());
        WeaponInfo info = new WeaponInfo(Map.of(AttackType.strike(), profile), new ElementSpread(),
                WeaponStatApplicability.MELEE);
        ItemStack stack = new ItemStack(Items.STICK);
        stack.getOrCreateTag().put(SkadaData.WEAPON_INFO_TAG_KEY, info.toCompoundTag());
        return stack;
    }

    /** Creates a mock player whose shield callback always reports a block. */
    private static Player blockingPlayer(GameTestHelper helper) {
        return new Player(helper.getLevel(), BlockPos.ZERO, 0.0F,
                new GameProfile(UUID.randomUUID(), "skada-mob-shield-test")) {
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

    /** Spawns a cow with its initial weight-derived resistance removed for a controlled combat setup. */
    /** Spawns a cow with no derived weight contribution for isolated knockback checks. */
    private static Cow cow(GameTestHelper helper, int x, int y, int z) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, x, y, z);
        var resistance = cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance.getModifier(KnockbackWeightResistanceUtil.MODIFIER_ID) != null) {
            resistance.removeModifier(KnockbackWeightResistanceUtil.MODIFIER_ID);
        }
        resistance.setBaseValue(0.0);
        return cow;
    }
}
