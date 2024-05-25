package com.cwjn.skada;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Skada.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue INEFFECTIVE_REACH_DAMAGE_MODIFIER;

    static {
        BUILDER.comment("General settings").push("general");
        INEFFECTIVE_REACH_DAMAGE_MODIFIER = BUILDER.comment("The damage modifier applied to attacks that are not in the effective range of an attack")
                .defineInRange("ineffectiveReachDamageModifier", 0.25, 0.0, 1.0);
        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {

    }

}
