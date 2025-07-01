package com.cwjn.skada;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class ClientConfig {


    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue USE_MDU_FONT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_RETICLES;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MOB_HEALTHBARS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DAMAGE_INDICATORS;
    public static final ForgeConfigSpec.BooleanValue HEALTHBAR_ONLY_ON_DAMAGE;
    public static final ForgeConfigSpec.IntValue HEALTHBAR_ON_DAMAGE_DISPLAY_TIME;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_HEALTHBAR_ENTITIES;

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
        HEALTHBAR_ONLY_ON_DAMAGE = BUILDER.comment("Show healthbars only when the entity takes damage")
                .define("healthbarOnlyOnDamage", false);
        HEALTHBAR_ON_DAMAGE_DISPLAY_TIME = BUILDER.comment("How long to display mob's health bar when hit if the previous option is true.")
                .defineInRange("ticks", 80, 1, Integer.MAX_VALUE);
        BLACKLISTED_HEALTHBAR_ENTITIES = BUILDER.comment("Blacklisted entities for healthbars. Enter mob registry names!")
                .define("blacklistedHealthbarEntities", List.of("minecraft:example"), s -> s instanceof String);
        ENABLE_DAMAGE_INDICATORS = BUILDER.comment("Enable damage indicators")
                .define("enableDamageIndicators", true);
        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

}
