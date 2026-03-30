package com.cwjn.skada.data.damage;

import com.cwjn.skada.data.gen.attack.ElementSpread;
import com.cwjn.skada.data.registry.AttackType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for manually defining weapon infos.
 */
public final class ManualWeaponInfos {

    private static final Map<String, Map<String, WeaponInfo>> BY_NAMESPACE = createByNamespace();

    private ManualWeaponInfos() {
    }

    public static Map<String, Map<String, WeaponInfo>> all() {
        return BY_NAMESPACE;
    }

    public static Map<String, WeaponInfo> byNamespace(String namespace) {
        return BY_NAMESPACE.getOrDefault(namespace, Map.of());
    }

    private static Map<String, Map<String, WeaponInfo>> createByNamespace() {
        Map<String, Map<String, WeaponInfo>> map = new LinkedHashMap<>();
        map.put("minecraft", createMinecraftInfos());
        return Map.copyOf(map);
    }

    private static Map<String, WeaponInfo> createMinecraftInfos() {
        Map<String, WeaponInfo> map = new LinkedHashMap<>();
        map.put("bow", bow());
        map.put("crossbow", crossbow());
        return Map.copyOf(map);
    }

    private static WeaponInfo bow() {
        return new WeaponInfo(singleAttackInfo(
                AttackType.thrust(),
                AttackTypeInfo.of(
                        12.0,
                        8.5,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        0.004,
                        List.of())),
                new ElementSpread(),
                true);
    }

    private static WeaponInfo crossbow() {
        return new WeaponInfo(singleAttackInfo(
                AttackType.thrust(),
                AttackTypeInfo.of(
                        16.0,
                        11.0,
                        0.0,
                        0.0,
                        0.0,
                        0.35,
                        0.002,
                        List.of())),
                new ElementSpread(),
                true);
    }

    private static Map<AttackType, AttackTypeInfo> singleAttackInfo(AttackType attackType, AttackTypeInfo info) {
        Map<AttackType, AttackTypeInfo> map = new LinkedHashMap<>();
        map.put(attackType, info);
        return map;
    }
}