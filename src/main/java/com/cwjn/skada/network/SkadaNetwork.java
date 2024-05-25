package com.cwjn.skada.network;

import com.cwjn.skada.network.client_to_server.C2SAddWeaponTag;
import com.cwjn.skada.network.client_to_server.C2SCycleAttackType;
import com.cwjn.skada.network.client_to_server.C2SCycleAttackTypeFromMenu;
import com.cwjn.skada.network.server_to_client.S2CCreateDamageIndicator;
import com.cwjn.skada.network.server_to_client.S2CSendWeaponInfoMap;
import com.cwjn.skada.network.server_to_client.S2CUpdateWeaponInfo;
import com.cwjn.skada.util.Util;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class SkadaNetwork {

    private static final String VERSION = "0.0.1";
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
                C2SCycleAttackType.class,
                C2SCycleAttackType::encode,
                C2SCycleAttackType::decode,
                C2SCycleAttackType::handle);
        INSTANCE.registerMessage(id++,
                C2SCycleAttackTypeFromMenu.class,
                C2SCycleAttackTypeFromMenu::encode,
                C2SCycleAttackTypeFromMenu::decode,
                C2SCycleAttackTypeFromMenu::handle);
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
                S2CUpdateWeaponInfo.class,
                S2CUpdateWeaponInfo::encode,
                S2CUpdateWeaponInfo::decode,
                S2CUpdateWeaponInfo::handle);
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
