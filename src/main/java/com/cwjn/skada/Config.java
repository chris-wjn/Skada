package com.cwjn.skada;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Skada.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_IMPACT_GRIT_CHECK = BUILDER
            .comment("Whether or not to factor attacker impact vs defender grit. If disabled, impact and grit will be removed.")
            .define("Impact vs Grit Check Enabled?", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_FINESSE_MOBILITY_CHECK = BUILDER
            .comment("Whether ot not to factor attacker finesse vs defender mobility. If disabled, finesse and mobility will be removed.")
            .define("Finesse vs Mobility Check Enabled?", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_DEFTNESS_RESILIENCE_CHECK = BUILDER
            .comment("Whether or not to factor attacker deftness vs defender resilience. If disabled, deftness and resilience will be removed.")
            .define("Deftness vs Resilience Check Enabled?", true);


    static final ForgeConfigSpec SPEC = BUILDER.build();

    private static boolean gritCheck, mobilityCheck, resilienceCheck;

    public static boolean isArmourPenEnabled() {
        return gritCheck;
    }

    public static boolean isGlancingBlowEnabled() {
        return mobilityCheck;
    }

    public static boolean isCritDamageEnabled() {
        return resilienceCheck;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        gritCheck = ENABLE_IMPACT_GRIT_CHECK.get();
        mobilityCheck = ENABLE_FINESSE_MOBILITY_CHECK.get();
        resilienceCheck = ENABLE_DEFTNESS_RESILIENCE_CHECK.get();
    }

}
