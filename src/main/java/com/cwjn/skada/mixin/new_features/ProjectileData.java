package com.cwjn.skada.mixin.new_features;

import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.DamageInfo;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Projectile.class)
public class ProjectileData implements AccessProjectileData {

    @Unique
    private DamageInfo skada$damageInfo;

    @Unique
    public DamageInfo getDamageInfo() {
        return skada$damageInfo;
    }

    @Unique
    public void setDamageInfo(DamageInfo skada$damageInfo) {
        this.skada$damageInfo = skada$damageInfo;
    }

}
