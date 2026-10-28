package com.cwjn.skada.damage;

import com.cwjn.skada.data.damage.DamageInfo;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/** Keeps damage-source identity intact while attaching an existing combat snapshot. */
class SkadaDamageSourceIdentityTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void indirectSourceRetainsProjectileShooterPositionAndSnapshot() {
        Entity projectile = mock(Entity.class);
        Entity shooter = mock(Entity.class);
        DamageInfo snapshot = mock(DamageInfo.class);
        Vec3 position = new Vec3(1, 2, 3);
        DamageSource original = new DamageSource(Holder.direct(new DamageType("test", 0.0F)), projectile, shooter, position);

        SkadaDamageSource wrapped = new SkadaDamageSource(original, snapshot);

        assertSame(projectile, wrapped.getDirectEntity());
        assertSame(shooter, wrapped.getEntity());
        assertSame(original.typeHolder(), wrapped.typeHolder());
        assertSame(position, wrapped.getSourcePosition());
        assertSame(snapshot, wrapped.getInfo());
    }

    @Test
    void ownerlessProjectileDoesNotBecomeItsOwnShooter() {
        Entity projectile = mock(Entity.class);
        DamageSource original = new DamageSource(Holder.direct(new DamageType("test", 0.0F)), projectile, null);

        SkadaDamageSource wrapped = new SkadaDamageSource(original, mock(DamageInfo.class));

        assertSame(projectile, wrapped.getDirectEntity());
        assertNull(wrapped.getEntity());
    }

    @Test
    void directMeleeAndEntitylessSourcesKeepTheirIdentity() {
        Entity attacker = mock(Entity.class);
        Holder<DamageType> type = Holder.direct(new DamageType("test", 0.0F));
        for (DamageSource original : new DamageSource[] { new DamageSource(type, attacker), new DamageSource(type) }) {
            SkadaDamageSource wrapped = new SkadaDamageSource(original, mock(DamageInfo.class));
            assertSame(original.getDirectEntity(), wrapped.getDirectEntity());
            assertSame(original.getEntity(), wrapped.getEntity());
        }
    }
}
