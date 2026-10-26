package com.cwjn.skada.data.damage;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.UtilCombat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/** One resolved launch profile shared by snapshot storage, flight and critical-fail handling. */
public record ProjectileLaunch(DamageInfo damageInfo, @Nullable AttackTypeInfo stats, Flight flight) {
    /** Delivery mechanics remain explicit; thrown tridents do not gain arrow velocity scaling. */
    public enum Flight { UNCHANGED, BOW, CROSSBOW }

    /** Resolves the exact firing stack supplied by a launch adapter, never the shooter's guessed hand. */
    public static ProjectileLaunch resolve(ItemStack source, AttackType fallbackType, double fallbackPrecision, Flight flight) {
        AttackSelection selected = AttackSelection.resolve(source);
        if (selected.configured() && selected.weapon().applicability().projectile()) {
            return new ProjectileLaunch(DamageInfo.projectile(selected.stats(), selected.type(), selected.weapon().getSpread().instance()),
                    selected.stats(), flight);
        }
        return new ProjectileLaunch(DamageInfo.projectileFallback(fallbackType, fallbackPrecision, WeaponInfo.FIST.getSpread().instance()),
                null, flight);
    }

    /** Seeds a newly captured launch and preserves any snapshot supplied by an earlier integration. */
    public void attach(Projectile projectile, ItemStack source, LivingEntity shooter) {
        AccessProjectileData data = (AccessProjectileData) projectile;
        if (data.hasDamageInfo()) return;
        // Capture vanilla trident damage even for an explicit fallback. A missing value can then
        // identify old trident snapshots that still need their carried-item compatibility lookup.
        DamageInfo snapshot = projectile instanceof ThrownTrident && stats == null
                ? new DamageInfo(damageInfo.lethality(), damageInfo.precision(), damageInfo.isEnvironmental(),
                    damageInfo.attackType(), damageInfo.elementSpreadInstance(), 8.0)
                : damageInfo;
        data.setDamageInfo(snapshot);
        data.setPendingLaunch(this);
        if (stats != null && shooter instanceof ServerPlayer player) {
            UtilCombat.rollCriticalFail(source, stats.failChance(), player.getRandom(), player);
        }
    }

    /** Applies the selected accuracy only when ranged precision is enabled. */
    public float inaccuracy(float original) {
        return stats != null && CommonConfig.ENABLE_PRECISION.get() && CommonConfig.ENABLE_PRECISION_FOR_RANGED.get()
                ? (float) UtilCombat.precisionScoreToProjectileInaccuracy(stats.precision()) : original;
    }

    /** Preserves bow charge scaling and the caller's crossbow/firework/mob velocity baseline. */
    public float velocity(float original) {
        if (stats == null || flight == Flight.UNCHANGED) return original;
        return flight == Flight.BOW
                ? (float) (original / 3.0 * UtilCombat.projectileVelocityWithDamageBonus(3.0, stats.damage()))
                : (float) UtilCombat.projectileVelocityWithDamageBonus(original, stats.damage());
    }
}
