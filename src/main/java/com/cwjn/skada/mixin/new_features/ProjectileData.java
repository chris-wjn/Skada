package com.cwjn.skada.mixin.new_features;

import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.ElementSpreadInstance;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import static com.cwjn.skada.Skada.LOGGER;

@Mixin(Projectile.class)
public class ProjectileData implements AccessProjectileData {

    private Projectile thisProjectile() {
        return (Projectile) (Object) this;
    }

    @Unique
    private DamageInfo skada$damageInfo;

    @Unique
    public DamageInfo getDamageInfo() {
        if (!hasDamageInfo()) {
            LOGGER.error("ProjectileData.getDamageInfo() called before setDamageInfo() was called for projectile: {}", thisProjectile().toString());
            return new DamageInfo(0.0, 1.0, false, AttackType.strike(), new ElementSpreadInstance());
        }
        return skada$damageInfo;
    }

    @Unique
    public void setDamageInfo(DamageInfo skada$damageInfo) {
        this.skada$damageInfo = skada$damageInfo;
    }

    @Unique
    public boolean hasDamageInfo() {
        return skada$damageInfo != null;
    }

}
