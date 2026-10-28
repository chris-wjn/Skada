package com.cwjn.skada.client.hud;

import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Map;

/** Tracks damage-only visibility from vanilla-synchronized health and hurt animations. */
public final class MobHealthBarTracker {
    private final Map<Mob, DamageState> states = new HashMap<>();

    /** Samples once before each client mob tick, refreshing only when new damage is observed. */
    public void tick(Mob mob, int displayTicks) {
        if (!mob.isAlive() || mob.isRemoved()) {
            remove(mob);
            return;
        }
        DamageState previous = states.get(mob);
        float health = mob.getHealth();
        // Health catches additional damage during hurt immunity; the animation also covers absorbed hits.
        boolean damaged = (previous != null && health < previous.health)
                || (mob.hurtTime > 0 && (previous == null || mob.hurtTime >= previous.hurtTime));
        if (previous == null) {
            previous = new DamageState();
            states.put(mob, previous);
        }
        previous.health = health;
        previous.hurtTime = mob.hurtTime;
        previous.remaining = damaged ? displayTicks : Math.max(0, previous.remaining - 1);
    }

    /** Returns the remaining game ticks, with zero meaning the bar is hidden. */
    public int remainingTicks(Mob mob) {
        DamageState state = states.get(mob);
        return state == null ? 0 : state.remaining;
    }

    /** Releases an entity as soon as it leaves the client level. */
    public void remove(Mob mob) {
        states.remove(mob);
    }

    /** Discards samples and timers when leaving a world or disabling damage-only visibility. */
    public void clear() {
        states.clear();
    }

    /** The last synchronized damage sample and its display countdown for one mob. */
    private static final class DamageState {
        private float health;
        private int hurtTime;
        private int remaining;
    }
}
