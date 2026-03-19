package com.cwjn.skada;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Skada.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue INEFFECTIVE_REACH_DAMAGE_MODIFIER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PRECISION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PRECISION_FOR_MELEE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PRECISION_FOR_RANGED;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LETHALITY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CRITICAL_FAIL;
    public static final ForgeConfigSpec.DoubleValue CRITICAL_FAIL_DURABILITY_LOSS;
    public static final ForgeConfigSpec.BooleanValue SQUEEZE_DAMAGE_VALUES;

    static {

        BUILDER.comment("Gameplay Settings").push("gameplay");
        SQUEEZE_DAMAGE_VALUES = BUILDER.comment("When generating, assign damage modifiers to minimize the difference in damage between weapons of the same type but different material")
                .define("squeezeDamageValues", true);
        BUILDER.pop();

        BUILDER.comment("Combat Settings").push("combat");

        BUILDER.push("Precision Settings");
        ENABLE_PRECISION = BUILDER.comment("Enable precision in general. All the following settings in this section will be ignored if this is false")
                .define("enablePrecision", true);
        ENABLE_PRECISION_FOR_MELEE = BUILDER.comment("Enable precision damage modifier for melee attacks")
                .define("enablePrecisionForMelee", true);
        ENABLE_PRECISION_FOR_RANGED = BUILDER.comment("Enable precision projectile deviation modifier for bow and crossbow attacks")
                .define("enablePrecisionForRanged", true);
        BUILDER.pop();

        BUILDER.push("Critical Fail Settings");
        ENABLE_CRITICAL_FAIL = BUILDER.comment("Enable critical fail mechanic")
                .define("enableCriticalFail", true);
        CRITICAL_FAIL_DURABILITY_LOSS = BUILDER.comment("The durability loss for the deformation tier of a critical fail. Edge damage uses a small fixed loss and catastrophic failure breaks the item.")
                .defineInRange("criticalFailDurabilityLoss", 0.15, 0.0, 1.0);
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
