package com.cwjn.skada;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Skada.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue INEFFECTIVE_REACH_DAMAGE_MODIFIER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ACCURACY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ACCURACY_FOR_MELEE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ACCURACY_FOR_RANGED;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LETHALITY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CRITICAL_FAIL;
    public static final ForgeConfigSpec.DoubleValue CRITICAL_FAIL_DURABILITY_LOSS;

    static {
        BUILDER.comment("Combat Settings").push("combat");

        BUILDER.push("Accuracy Settings");
        ENABLE_ACCURACY = BUILDER.comment("Enable precision in general. All the following settings in this section will be ignored if this is false")
                .define("enableAccuracy", true);
        ENABLE_ACCURACY_FOR_MELEE = BUILDER.comment("Enable precision damage modifier for melee attacks")
                .define("enableAccuracyForMelee", true);
        ENABLE_ACCURACY_FOR_RANGED = BUILDER.comment("Enable precision projectile deviation modifier for bow and crossbow attacks")
                .define("enableAccuracyForRanged", true);
        BUILDER.pop();

        BUILDER.push("Critical Fail Settings");
        ENABLE_CRITICAL_FAIL = BUILDER.comment("Enable critical fail mechanic")
                .define("enableCriticalFail", true);
        CRITICAL_FAIL_DURABILITY_LOSS = BUILDER.comment("The durability loss from a critical fail if it is enabled. The item loses this percentage of it's durability on critical fail.")
                .defineInRange("criticalFailDurabilityLoss", 0.5, 0.0, 1.0);
        BUILDER.pop();

        ENABLE_LETHALITY = BUILDER.comment("Enable lethality damage modifier")
                .define("enableLethality", true);
        INEFFECTIVE_REACH_DAMAGE_MODIFIER = BUILDER.comment("The damage modifier applied to attacks that are not in the effective range of an attack")
                .defineInRange("ineffectiveReachDamageModifier", 0.25, 0.0, 1.0);
        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {

    }

}
