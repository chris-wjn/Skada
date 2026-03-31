package com.cwjn.skada.damage;

import com.cwjn.skada.data.damage.AccessProjectileData;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against an unguarded cast to AccessProjectileData in DamageHandler.
 * A bare {@code (AccessProjectileData) projectile} without a preceding instanceof
 * check will throw ClassCastException for any projectile not implementing that
 * interface (i.e. any non-Skada projectile), crashing the damage pipeline.
 *
 * Two complementary strategies are used here:
 *
 * 1. Source-text scanning (no Forge bootstrap required): verifies the guard exists in the
 *    source code. This provides a fast, lightweight regression check that is always available.
 *
 * 2. Behavioral test via {@link DamageHandler#resolveProjectileData}: exercises the extracted
 *    helper with {@code null} (which {@code instanceof} always treats as false) to prove the
 *    non-Skada fall-through path returns empty without throwing. Forge bootstrap is not needed
 *    because the helper itself has no Forge dependencies — it only performs an instanceof check
 *    and returns an Optional.
 */
class DamageHandlerProjectileSafetyTest {

    /**
     * Bootstrap vanilla Minecraft registries once for this test class.
     * Entity's static initializer references BuiltInRegistries which requires Bootstrap to have
     * run; without this, Mockito.mock(Entity.class) fails with "Not bootstrapped".
     * Bootstrap.bootStrap() is idempotent — safe to call multiple times across test classes.
     */
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void damageHandlerDoesNotCastToAccessProjectileDataWithoutInstanceofGuard() throws IOException {
        Path source = Path.of("src/main/java/com/cwjn/skada/damage/DamageHandler.java");
        String text = Files.readString(source);

        assertFalse(
                text.contains("(AccessProjectileData) projectile"),
                "DamageHandler must not cast directly to AccessProjectileData without an instanceof " +
                "check — any non-Skada projectile will throw ClassCastException in the damage event"
        );
    }

    @Test
    void damageHandlerProjectilePathUsesInstanceofGuard() throws IOException {
        Path source = Path.of("src/main/java/com/cwjn/skada/damage/DamageHandler.java");
        String text = Files.readString(source);

        if (text.contains("AccessProjectileData")) {
            assertTrue(
                    text.contains("instanceof AccessProjectileData"),
                    "DamageHandler references AccessProjectileData but has no instanceof guard — " +
                    "add a guard so non-Skada projectiles fall through safely"
            );
        }
    }

    @Test
    void resolveProjectileDataReturnsEmptyForNonSkadaProjectile() {
        // null is never instanceof anything — this represents any projectile that does not
        // implement AccessProjectileData (e.g. a vanilla arrow, a modded bolt, etc.).
        // The method must return Optional.empty() without throwing.
        Optional<AccessProjectileData> result = DamageHandler.resolveProjectileData(null);
        assertFalse(result.isPresent(),
                "resolveProjectileData must return Optional.empty() for a non-AccessProjectileData entity");

        // A real non-null Entity that does not implement AccessProjectileData must also return empty.
        // Mockito creates a subclass proxy without triggering the real constructor, so no Forge
        // bootstrap is needed.
        Entity mockEntity = Mockito.mock(Entity.class);
        Optional<AccessProjectileData> resultForMock = DamageHandler.resolveProjectileData(mockEntity);
        assertFalse(resultForMock.isPresent(),
                "resolveProjectileData must return Optional.empty() for a real non-null Entity that does not implement AccessProjectileData");
    }
}
