package com.cwjn.skada.mixin;

import com.cwjn.skada.Config;
import com.cwjn.skada.damage.SkadaDamageSource;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.ElementSpread;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.AccessPlayerWeaponInfo;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public class PlayerAttackUseSkada implements AccessPlayerWeaponInfo {

    @Unique private WeaponInfo skada$weaponInfo;
    @Unique private int skada$attackTypeIndex;

    public WeaponInfo getWeaponInfo() {
        return skada$weaponInfo;
    }

    public void setWeaponInfo(WeaponInfo info) {
        skada$weaponInfo = info;
    }

    @Override
    public void setAttackTypeIndex(int index) {
        skada$attackTypeIndex = index;
    }

    @Override
    public int getAttackTypeIndex() {
        return skada$attackTypeIndex;
    }

    private Player thisPlayer() {
        return (Player) (Object) this;
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean addDamageInfoToPlayerAttack(Entity instance, DamageSource source, float amount) {
        DamageInfo damageInfo;
        if (skada$weaponInfo == null) {
            damageInfo = new DamageInfo(0, 0, false, AttackType.strike(), new ElementSpread());
        } else {
            AttackType[] attackTypes = skada$weaponInfo.getAttackTypes().keySet().toArray(AttackType[]::new);
            AttackTypeInfo attackInfo = skada$weaponInfo.getAttackTypes().get(attackTypes[skada$attackTypeIndex]);
            double distance = thisPlayer().distanceTo(instance);
            if (distance < attackInfo.minReach()) amount *= Config.INEFFECTIVE_REACH_DAMAGE_MODIFIER.get();
            damageInfo = new DamageInfo(attackInfo.aim(), attackInfo.lethality(), false, attackTypes[skada$attackTypeIndex], skada$weaponInfo.getSpread());
        }
        SkadaDamageSource skadaSource = new SkadaDamageSource(source, damageInfo);
        return instance.hurt(skadaSource, amount);
    }

}
