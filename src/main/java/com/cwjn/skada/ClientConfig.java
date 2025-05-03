package com.cwjn.skada;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue USE_MDU_FONT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_RETICLES;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MOB_HEALTHBARS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DAMAGE_INDICATORS;

    static {
        BUILDER.comment("Tooltip Settings").push("tooltips");
        USE_MDU_FONT = BUILDER.comment("Use the custom font for tooltips")
                .define("useNewFont", true);
        BUILDER.pop();
        BUILDER.comment("Reticle Settings").push("reticles");
        ENABLE_CUSTOM_RETICLES = BUILDER.comment("Enable custom reticles. This should match the setting in skada-mixin-config.json! (This file is also in config folder)")
                .define("enableCustomReticles", false);
        BUILDER.pop();
        BUILDER.comment("HUD settings").push("hud");
        ENABLE_MOB_HEALTHBARS = BUILDER.comment("Enable mob healthbars")
                .define("enableMobHealthbars", true);
        ENABLE_DAMAGE_INDICATORS = BUILDER.comment("Enable damage indicators")
                .define("enableDamageIndicators", true);
        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

}
