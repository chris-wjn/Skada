package com.cwjn.skada.event;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure math tests for burden-based food exhaustion constants in CommonEvent.
 * No Forge bootstrap required.
 */
public class BurdenExhaustionMathTest {

    @Test
    public void burdenExhaustionRatePerTickIsNonZero() {
        assertTrue(CommonEvent.ForgeBusEvent.BURDEN_EXHAUSTION_RATE_PER_TICK > 0,
                "Passive exhaustion rate per tick must be positive");
    }

    @Test
    public void burdenJumpExhaustionRateIsNonZero() {
        assertTrue(CommonEvent.ForgeBusEvent.BURDEN_JUMP_EXHAUSTION_RATE > 0,
                "Jump exhaustion rate must be positive");
    }

    @Test
    public void passiveExhaustionScalesWithBurden() {
        float rate = CommonEvent.ForgeBusEvent.BURDEN_EXHAUSTION_RATE_PER_TICK;
        float exhaust10 = 10 * rate;
        float exhaust20 = 20 * rate;
        assertTrue(exhaust10 > 0, "Exhaustion for burden=10 must be positive");
        assertTrue(exhaust20 > exhaust10, "Higher burden must produce more exhaustion");
    }

    @Test
    public void jumpExhaustionIsGreaterThanPassivePerTick() {
        assertTrue(
                CommonEvent.ForgeBusEvent.BURDEN_JUMP_EXHAUSTION_RATE
                        > CommonEvent.ForgeBusEvent.BURDEN_EXHAUSTION_RATE_PER_TICK,
                "Jump exhaustion rate must exceed passive per-tick rate");
    }
}
