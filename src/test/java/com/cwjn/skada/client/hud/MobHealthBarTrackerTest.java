package com.cwjn.skada.client.hud;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Mob;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Exercises damage refresh, expiry and cleanup without starting a graphical client. */
class MobHealthBarTrackerTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void undamagedOrHealingMobsStayHidden() {
        Mob mob = mob(10);
        MobHealthBarTracker tracker = new MobHealthBarTracker();
        tracker.tick(mob, 80);
        when(mob.getHealth()).thenReturn(15F);
        tracker.tick(mob, 80);
        assertEquals(0, tracker.remainingTicks(mob));
    }

    @Test
    void synchronizedHealthLossShowsAndRefreshesTheBarWithoutAnAnimation() {
        Mob mob = mob(20);
        MobHealthBarTracker tracker = new MobHealthBarTracker();
        tracker.tick(mob, 3);
        when(mob.getHealth()).thenReturn(18F);
        tracker.tick(mob, 3);
        assertEquals(3, tracker.remainingTicks(mob));
        tracker.tick(mob, 3);
        assertEquals(2, tracker.remainingTicks(mob));
        when(mob.getHealth()).thenReturn(17F);
        tracker.tick(mob, 3);
        assertEquals(3, tracker.remainingTicks(mob));
    }

    @Test
    void aHurtAnimationShowsAbsorbedHitsAndDoesNotExtendVisibilityAsItDecays() {
        Mob mob = mob(20);
        MobHealthBarTracker tracker = new MobHealthBarTracker();
        mob.hurtTime = 10;
        tracker.tick(mob, 2);
        assertEquals(2, tracker.remainingTicks(mob));
        mob.hurtTime = 9;
        tracker.tick(mob, 2);
        assertEquals(1, tracker.remainingTicks(mob));
        mob.hurtTime = 8;
        tracker.tick(mob, 2);
        assertEquals(0, tracker.remainingTicks(mob));
    }

    @Test
    void consecutiveHurtAnimationsRefreshEvenWhenHealthIsUnchanged() {
        Mob mob = mob(20);
        MobHealthBarTracker tracker = new MobHealthBarTracker();
        mob.hurtTime = 10;
        tracker.tick(mob, 80);
        // A second damage packet can reset hurtTime to ten before the next mob tick.
        tracker.tick(mob, 80);
        assertEquals(80, tracker.remainingTicks(mob));
    }

    @Test
    void oneTickDurationExpiresAndLaterDamageCanShowTheBarAgain() {
        Mob mob = mob(20);
        MobHealthBarTracker tracker = new MobHealthBarTracker();
        tracker.tick(mob, 1);
        when(mob.getHealth()).thenReturn(19F);
        tracker.tick(mob, 1);
        assertEquals(1, tracker.remainingTicks(mob));
        tracker.tick(mob, 1);
        assertEquals(0, tracker.remainingTicks(mob));
        when(mob.getHealth()).thenReturn(18F);
        tracker.tick(mob, 1);
        assertEquals(1, tracker.remainingTicks(mob));
    }

    @Test
    void damageAndRemovalAffectOnlyTheRelevantMob() {
        Mob first = mob(20);
        Mob second = mob(20);
        MobHealthBarTracker tracker = new MobHealthBarTracker();
        tracker.tick(first, 80);
        tracker.tick(second, 80);
        when(first.getHealth()).thenReturn(19F);
        tracker.tick(first, 80);
        assertEquals(80, tracker.remainingTicks(first));
        assertEquals(0, tracker.remainingTicks(second));
        second.hurtTime = 10;
        tracker.tick(second, 80);
        tracker.remove(first);
        assertEquals(0, tracker.remainingTicks(first));
        assertEquals(80, tracker.remainingTicks(second));
    }

    @Test
    void deathRemovalAndWorldResetDiscardTimersAndOldHealthSamples() {
        Mob mob = mob(20);
        MobHealthBarTracker tracker = new MobHealthBarTracker();
        mob.hurtTime = 10;
        tracker.tick(mob, 80);
        when(mob.isAlive()).thenReturn(false);
        tracker.tick(mob, 80);
        assertEquals(0, tracker.remainingTicks(mob));
        when(mob.isAlive()).thenReturn(true);
        tracker.tick(mob, 80);
        when(mob.isRemoved()).thenReturn(true);
        tracker.tick(mob, 80);
        assertEquals(0, tracker.remainingTicks(mob));
        when(mob.isRemoved()).thenReturn(false);
        tracker.tick(mob, 80);
        tracker.remove(mob);
        assertEquals(0, tracker.remainingTicks(mob));
        tracker.tick(mob, 80);
        tracker.clear();
        mob.hurtTime = 0;
        when(mob.getHealth()).thenReturn(10F);
        tracker.tick(mob, 80);
        assertEquals(0, tracker.remainingTicks(mob));
    }

    /** Creates a living, undamaged synchronized entity sample. */
    private static Mob mob(float health) {
        Mob mob = mock(Mob.class);
        when(mob.isAlive()).thenReturn(true);
        when(mob.getHealth()).thenReturn(health);
        return mob;
    }
}
