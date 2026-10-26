package com.cwjn.skada.damage;

import com.cwjn.skada.data.damage.AccessProjectileData;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifies that only projectiles with a captured Skada snapshot enter its damage path. */
class DamageHandlerProjectileSafetyTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void missingProjectileFallsThrough() {
        assertTrue(DamageHandler.resolveProjectileData(null).isEmpty());
    }

    @Test
    void unrelatedEntityFallsThroughWithoutCastFailure() {
        assertTrue(DamageHandler.resolveProjectileData(mock(Entity.class)).isEmpty());
    }

    @Test
    void projectileWithoutSnapshotFallsThrough() {
        Entity projectile = mock(Entity.class, Mockito.withSettings().extraInterfaces(AccessProjectileData.class));
        assertTrue(DamageHandler.resolveProjectileData(projectile).isEmpty());
    }

    @Test
    void projectileWithSnapshotRetainsItsData() {
        Entity projectile = mock(Entity.class, Mockito.withSettings().extraInterfaces(AccessProjectileData.class));
        AccessProjectileData data = (AccessProjectileData) projectile;
        when(data.hasDamageInfo()).thenReturn(true);
        assertSame(data, DamageHandler.resolveProjectileData(projectile).orElseThrow());
    }
}
