package com.cwjn.skada;

import com.cwjn.skada.damage.DamageClass;
import com.cwjn.skada.damage.SkadaDamageTags;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Skada.MODID)
public class Skada {
    public static final String MODID = "skada";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Skada() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        SkadaRegistry.ATTRIBUTES.register(modEventBus);
        SkadaDamageTags.init();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        SkadaData.registerDamageClass(new DamageClass("slash", SkadaRegistry.SLASH_RESIST.get()));
        SkadaData.registerDamageClass(new DamageClass("pierce", SkadaRegistry.PIERCE_RESIST.get()));
        SkadaData.registerDamageClass(new DamageClass("blunt", SkadaRegistry.BLUNT_RESIST.get()));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {

        }
    }

}
