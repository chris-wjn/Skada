package com.cwjn.skada.network;

import com.cwjn.skada.network.client_to_server.C2SAddWeaponTag;
import com.cwjn.skada.network.client_to_server.C2SSelectAttackType;
import com.cwjn.skada.network.client_to_server.C2SSelectAttackTypeFromMenu;
import com.cwjn.skada.network.server_to_client.*;
import com.cwjn.skada.util.Util;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class SkadaNetwork {

    private static final String VERSION = "0.0.4";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            Util.rl("main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals
    );

    public static void init() {
        int id = 0;
        INSTANCE.registerMessage(id++,
                S2CSendWeaponInfoMap.class,
                S2CSendWeaponInfoMap::encode,
                S2CSendWeaponInfoMap::decode,
                S2CSendWeaponInfoMap::handle);
        INSTANCE.registerMessage(id++,
                S2CSendArmourInfoMap.class,
                S2CSendArmourInfoMap::encode,
                S2CSendArmourInfoMap::decode,
                S2CSendArmourInfoMap::handle);
        INSTANCE.registerMessage(id++,
                C2SSelectAttackType.class,
                C2SSelectAttackType::encode,
                C2SSelectAttackType::decode,
                C2SSelectAttackType::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++,
                C2SSelectAttackTypeFromMenu.class,
                C2SSelectAttackTypeFromMenu::encode,
                C2SSelectAttackTypeFromMenu::decode,
                C2SSelectAttackTypeFromMenu::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++,
                C2SAddWeaponTag.class,
                C2SAddWeaponTag::encode,
                C2SAddWeaponTag::decode,
                C2SAddWeaponTag::handle);
        INSTANCE.registerMessage(id++,
                S2CCreateDamageIndicator.class,
                S2CCreateDamageIndicator::encode,
                S2CCreateDamageIndicator::decode,
                S2CCreateDamageIndicator::handle);
        INSTANCE.registerMessage(id++,
                S2CPlayCriticalFailSound.class,
                S2CPlayCriticalFailSound::encode,
                S2CPlayCriticalFailSound::decode,
                S2CPlayCriticalFailSound::handle);
        INSTANCE.registerMessage(id++,
                S2CSendReticles.class,
                S2CSendReticles::encode,
                S2CSendReticles::decode,
                S2CSendReticles::handle);
        INSTANCE.registerMessage(id++,
                S2CSyncSkyDarken.class,
                S2CSyncSkyDarken::encode,
                S2CSyncSkyDarken::decode,
                S2CSyncSkyDarken::handle);
        INSTANCE.registerMessage(id++,
                S2CSendBestiaryEntries.class,
                S2CSendBestiaryEntries::encode,
                S2CSendBestiaryEntries::decode,
                S2CSendBestiaryEntries::handle);
        INSTANCE.registerMessage(id++,
                S2CSyncBestiaryUnlocks.class,
                S2CSyncBestiaryUnlocks::encode,
                S2CSyncBestiaryUnlocks::decode,
                S2CSyncBestiaryUnlocks::handle);
        INSTANCE.registerMessage(id++, S2CTutorial.class, S2CTutorial::encode, S2CTutorial::decode, S2CTutorial::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, com.cwjn.skada.network.client_to_server.C2STutorial.class,
                com.cwjn.skada.network.client_to_server.C2STutorial::encode,
                com.cwjn.skada.network.client_to_server.C2STutorial::decode,
                com.cwjn.skada.network.client_to_server.C2STutorial::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++,
                S2CSyncPlayerBodyWeight.class,
                S2CSyncPlayerBodyWeight::encode,
                S2CSyncPlayerBodyWeight::decode,
                S2CSyncPlayerBodyWeight::handle);
    }

    public static void serverToPlayer(Object packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void serverToAll(Object packet) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void playerToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void serverToNearPoint(Object packet, double x, double y, double z, double range, ResourceKey<Level> dim) {
        INSTANCE.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(x, y, z, range, dim)), packet);
    }

}
