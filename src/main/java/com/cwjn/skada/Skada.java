package com.cwjn.skada;

import com.cwjn.skada.mixin.new_features.AccessRangedAttribute;
import com.cwjn.skada.network.SkadaNetwork;
import com.cwjn.skada.util.Util;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.cwjn.skada.SkadaRegistry.*;
import static com.cwjn.skada.client.Particles.PARTICLES;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Skada.MODID)
public class Skada {
    public static final String MODID = "skada";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Skada() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        ATTACK_TYPES.register(modEventBus);
        ELEMENTS.register(modEventBus);
        PARTICLES.register(modEventBus);
        SkadaRegistry.ATTRIBUTES.register(modEventBus);
        
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        Attributes.ATTACK_DAMAGE.setSyncable(true);
        Attributes.ATTACK_KNOCKBACK.setSyncable(true);
        Attributes.KNOCKBACK_RESISTANCE.setSyncable(true);
        AccessRangedAttribute mixinArmour = (AccessRangedAttribute) Attributes.ARMOR;
        AccessRangedAttribute mixinDefense = (AccessRangedAttribute) Attributes.ARMOR_TOUGHNESS;
        AccessRangedAttribute mixinHealth = (AccessRangedAttribute) Attributes.MAX_HEALTH;
        mixinHealth.setMax(Double.MAX_VALUE);
        mixinArmour.setMax(Double.MAX_VALUE);
        mixinArmour.setMin(-Double.MAX_VALUE);
        mixinDefense.setMax(Double.MAX_VALUE);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        SkadaNetwork.init();
        Path dir = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada");
        Path dirWeapon = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "weapons");
        Path dirArmour = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "armour");
        Path dirMobs = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "mobs");
        Path dirWeaponGenerated = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "weapons", "generated");
        Path dirArmourGenerated = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "armour", "generated");
        Path dirMobsGenerated = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "skada", "mobs", "generated");
        WeaponProfileCodecWriter.writeDefaultByItemNameJson();
        try {
            Files.createDirectories(dir);
            Files.createDirectories(dirWeapon);
            Files.createDirectories(dirArmour);
            Files.createDirectories(dirMobs);
            Files.createDirectories(dirWeaponGenerated);
            Files.createDirectories(dirArmourGenerated);
            Files.createDirectories(dirMobsGenerated);
        }
        catch (IOException e) {
            if (e instanceof FileAlreadyExistsException) {}
            else {
                LOGGER.error("Failed to create skada config directory", e);
            }
        }
    }

    @SubscribeEvent
    public void loadResources(ServerAboutToStartEvent event) {
        Util.updateWeaponInfoItemsFromResources(event.getServer().getResourceManager());
        Util.updateArmourInfoItemsFromResources(event.getServer().getResourceManager());
        Util.updateMobInfoFromResources(event.getServer().getResourceManager());
        Util.updateReticleListFromResources(event.getServer().getResourceManager());
    }

    @SubscribeEvent
    public void loadResourcesOnReload(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            Util.updateWeaponInfoItemsFromResources(event.getPlayerList().getServer().getResourceManager());
            Util.updateArmourInfoItemsFromResources(event.getPlayerList().getServer().getResourceManager());
            Util.updateMobInfoFromResources(event.getPlayerList().getServer().getResourceManager());
            Util.updateReticleListFromResources(event.getPlayerList().getServer().getResourceManager());
        }
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
