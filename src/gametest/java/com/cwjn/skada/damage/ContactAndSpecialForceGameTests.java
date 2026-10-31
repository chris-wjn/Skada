package com.cwjn.skada.damage;

import com.cwjn.skada.Skada;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Exercises ordinary contact force alongside the special mechanics of affected mobs. */
@GameTestHolder(Skada.MODID)
@PrefixGameTestTemplate(false)
public final class ContactAndSpecialForceGameTests {
    private static final String TEMPLATE = "damage_handler_5x4x5";

    /** Prevents instantiation of this GameTest collection. */
    private ContactAndSpecialForceGameTests() {}

    /** Checks slime and magma-cube contact for one derived impulse and resistance application. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void slimeAndMagmaCubeContactUseOneDerivedImpulseAndResistance(GameTestHelper helper) {
        Player slimeTarget = spawnAttackPlayer(helper, 1, 2, 2);
        setAttribute(slimeTarget, Attributes.KNOCKBACK_RESISTANCE, 0.0);
        Slime slime = helper.spawn(EntityType.SLIME, 1, 2, 1);
        slime.setSize(2, true);
        slime.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(1.0);

        slime.playerTouch(slimeTarget);

        helper.assertTrue(slimeTarget.getHealth() < slimeTarget.getMaxHealth(),
                "Slime contact should retain its accepted mobAttack damage");
        helper.assertTrue(horizontalSpeedSqr(slimeTarget) > 0.2,
                "Accepted slime contact should add one ordinary derived outward impulse");
        helper.assertTrue(((ContactPlayer) slimeTarget).ordinaryKnockbackCalls == 1,
                "Accepted slime contact should apply exactly one ordinary knockback call");

        Player magmaTarget = spawnAttackPlayer(helper, 3, 2, 2);
        setAttribute(magmaTarget, Attributes.KNOCKBACK_RESISTANCE, 1.0);
        Slime magmaCube = helper.spawn(EntityType.MAGMA_CUBE, 3, 2, 1);
        magmaCube.setSize(2, true);
        magmaCube.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(1.0);

        magmaCube.playerTouch(magmaTarget);

        helper.assertTrue(magmaTarget.getHealth() < magmaTarget.getMaxHealth(),
                "Magma-cube contact should retain its accepted mobAttack damage at full resistance");
        helper.assertTrue(horizontalSpeedSqr(magmaTarget) < 1.0e-12,
                "Ordinary contact force should respect final knockback resistance once");
        helper.assertTrue(((ContactPlayer) magmaTarget).ordinaryKnockbackCalls == 1,
                "Magma-cube contact should still attempt exactly one ordinary knockback call at full resistance");
        helper.succeed();
    }

    /** Checks the ordinary hoglin impulse remains separate from its adult throw. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void hoglinOrdinaryImpulsePrecedesAndDoesNotReplaceSpecialThrow(GameTestHelper helper) {
        Hoglin hoglin = helper.spawnWithNoFreeWill(EntityType.HOGLIN, 1, 2, 1);
        hoglin.setBaby(false);
        hoglin.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(1.0);
        CountingCow target = spawnCountingCow(helper, 1, 2, 3);
        setAttribute(target, Attributes.KNOCKBACK_RESISTANCE, 0.0);

        helper.assertTrue(hoglin.doHurtTarget(target), "Hoglin's real override should accept the melee hit");

        helper.assertTrue(target.ordinaryKnockbackCalls == 1,
                "Hoglin attack should replace the hurt shove with exactly one derived ordinary impulse");
        helper.assertTrue(target.specialPushCalls == 1,
                "Adult Hoglin throwTarget must remain a separate special push");
        double awayX = target.getX() - hoglin.getX();
        double awayZ = target.getZ() - hoglin.getZ();
        double ordinaryInputDot = target.lastKnockbackX * awayX + target.lastKnockbackZ * awayZ;
        helper.assertTrue(ordinaryInputDot < 0.0,
                "Vanilla knockback receives the direction toward the Hoglin so its subtractive response pushes away; "
                        + "ordinary input=" + target.lastKnockbackX + "," + target.lastKnockbackZ
                        + ", away=" + awayX + "," + awayZ);

        Hoglin resistedHoglin = helper.spawnWithNoFreeWill(EntityType.HOGLIN, 3, 2, 1);
        resistedHoglin.setBaby(false);
        resistedHoglin.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(1.0);
        CountingCow resistantTarget = spawnCountingCow(helper, 3, 2, 3);
        setAttribute(resistantTarget, Attributes.KNOCKBACK_RESISTANCE, 1.0);

        helper.assertTrue(resistedHoglin.doHurtTarget(resistantTarget), "Resistant target still accepts damage");
        helper.assertTrue(resistantTarget.ordinaryKnockbackCalls == 1,
                "Ordinary impulse path should be invoked once and apply resistance through vanilla knockback");
        helper.assertTrue(resistantTarget.specialPushCalls == 0,
                "Hoglin throwTarget's own resistance comparison should suppress its special push at resistance one");
        helper.assertTrue(horizontalSpeedSqr(resistantTarget) < 1.0e-12,
                "Hoglin special and ordinary components are both blocked for this source-specific resistance case");
        helper.succeed();
    }

    /** Checks a shielded hoglin target receives ordinary contact and the special shield reaction. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void hoglinShieldReactionKeepsItsSpecialThrowAlongsideOrdinaryContact(GameTestHelper helper) {
        Hoglin hoglin = helper.spawnWithNoFreeWill(EntityType.HOGLIN, 1, 2, 1);
        hoglin.setBaby(false);
        hoglin.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(1.0);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 3));
        BlockingCountingPlayer target = new BlockingCountingPlayer(helper.getLevel());
        target.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        target.setYRot(180.0F);
        target.setXRot(0.0F);
        target.setYHeadRot(180.0F);
        target.setYBodyRot(180.0F);
        setAttribute(target, Attributes.KNOCKBACK_RESISTANCE, 0.0);
        target.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        double healthBefore = target.getHealth();
        var source = hoglin.damageSources().mobAttack(hoglin);
        Vec3 towardHoglin = hoglin.position().subtract(target.position()).normalize();
        helper.assertTrue(target.getViewVector(1.0F).dot(towardHoglin) > 0.0,
                "The test target must look toward the Hoglin; view=" + target.getViewVector(1.0F)
                        + ", toward=" + towardHoglin);
        helper.assertTrue(target.isDamageSourceBlocked(source),
                "The test target must be facing its raised shield toward the Hoglin; view=" + target.getViewVector(1.0F));

        helper.assertFalse(hoglin.doHurtTarget(target), "The shield should reject Hoglin damage");

        helper.assertTrue(target.getHealth() == healthBefore, "Shield protection must remain intact");
        helper.assertTrue(target.ordinaryKnockbackCalls == 1,
                "Shield contact should receive one derived ordinary impulse before source-specific reactions; calls="
                        + target.ordinaryKnockbackCalls + ", resistance="
                        + target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) + ", attackKnockback="
                        + hoglin.getAttributeValue(Attributes.ATTACK_KNOCKBACK));
        helper.assertTrue(target.specialPushCalls == 1,
                "Hoglin's special blockedByShield throw must still run exactly once");
        helper.succeed();
    }

    /** Checks the iron golem's ordinary melee path preserves its separate upward launch. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void ironGolemLaunchRemainsSeparateFromOrdinaryMelee(GameTestHelper helper) {
        var golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, 1, 2, 1);
        CountingCow target = spawnCountingCow(helper, 1, 2, 3);
        setAttribute(target, Attributes.KNOCKBACK_RESISTANCE, 0.0);

        helper.assertTrue(golem.doHurtTarget(target), "Iron golem's real override should accept the melee hit");

        helper.assertTrue(target.getDeltaMovement().y >= 0.4,
                "With exact zero resistance, vanilla's 0.4 × (1 − resistance) golem launch remains separate");
        helper.succeed();
    }

    /** Spawns a cow that counts ordinary knockback and separate special pushes. */
    private static CountingCow spawnCountingCow(GameTestHelper helper, int x, int y, int z) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
        CountingCow cow = new CountingCow(helper.getLevel());
        cow.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(cow);
        return cow;
    }

    /** Spawns a mock player at a template-relative position for contact testing. */
    private static Player spawnAttackPlayer(GameTestHelper helper, int x, int y, int z) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
        Player player = new ContactPlayer(helper.getLevel(), "skada-contact-knockback-test");
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setInvulnerable(false);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    /** Returns squared horizontal speed for a displacement assertion. */
    private static double horizontalSpeedSqr(LivingEntity entity) {
        return entity.getDeltaMovement().horizontalDistanceSqr();
    }

    /** Replaces an attribute's current modifiers and base value for a controlled test case. */
    private static void setAttribute(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                     double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        for (AttributeModifier modifier : new java.util.ArrayList<>(instance.getModifiers())) {
            instance.removeModifier(modifier);
        }
        instance.setBaseValue(value);
    }

    /** Records ordinary knockback separately from the push used by special forces. */
    private static final class CountingCow extends Cow {
        private int ordinaryKnockbackCalls;
        private int specialPushCalls;
        private double lastKnockbackX;
        private double lastKnockbackZ;

        /** Creates a counting cow in the supplied level. */
        private CountingCow(net.minecraft.world.level.Level level) {
            super(EntityType.COW, level);
        }

        /** Counts positive ordinary knockback calls before delegating to vanilla movement. */
        @Override
        public void knockback(double strength, double x, double z) {
            if (strength > 0.0) {
                ordinaryKnockbackCalls++;
                lastKnockbackX = x;
                lastKnockbackZ = z;
            }
            super.knockback(strength, x, z);
        }

        /** Counts pushes reserved for special source behavior before delegating to vanilla. */
        @Override
        public void push(double x, double y, double z) {
            specialPushCalls++;
            super.push(x, y, z);
        }
    }

    /** Minimal mock player that exposes ordinary knockback call counts to the test. */
    private static class ContactPlayer extends Player {
        int ordinaryKnockbackCalls;

        /** Creates a test player with a unique profile name. */
        private ContactPlayer(Level level, String name) {
            super(level, BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), name));
        }

        /** Counts positive ordinary knockback calls before delegating to vanilla movement. */
        @Override
        public void knockback(double strength, double x, double z) {
            if (strength > 0.0) ordinaryKnockbackCalls++;
            super.knockback(strength, x, z);
        }

        /** Reports that the mock player is not a spectator. */
        @Override public boolean isSpectator() { return false; }
        /** Reports that the mock player is not in creative mode. */
        @Override public boolean isCreative() { return false; }
        /** Reports that the mock player is not the client-side local player. */
        @Override public boolean isLocalPlayer() { return false; }
    }

    /** Mock player that always blocks and counts separate special push calls. */
    private static final class BlockingCountingPlayer extends ContactPlayer {
        private int specialPushCalls;

        /** Creates a shield-reaction test player. */
        private BlockingCountingPlayer(Level level) {
            super(level, "skada-hoglin-shield-test");
        }

        /** Reports that the mock player is actively blocking. */
        @Override public boolean isBlocking() { return true; }

        /** Counts special pushes before delegating to the ordinary player implementation. */
        @Override
        public void push(double x, double y, double z) {
            specialPushCalls++;
            super.push(x, y, z);
        }
    }
}
