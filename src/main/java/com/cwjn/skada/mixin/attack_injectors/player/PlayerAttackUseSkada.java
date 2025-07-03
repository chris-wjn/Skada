package com.cwjn.skada.mixin.attack_injectors.player;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.damage.SkadaDamageSource;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Player.class, priority = 1100)
public class PlayerAttackUseSkada {

    @SuppressWarnings("all")
    private Player thisPlayer() {
        return (Player) (Object) this;
    }

//    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
//    private boolean addDamageInfoToPlayerAttack(Entity instance, DamageSource source, float amount) {
//        DamageInfo damageInfo;
//        ItemStack heldItem = thisPlayer().getMainHandItem();
//        WeaponInfo weaponInfo = Util.getWeaponInfo(thisPlayer());
//        AttackType attackType = Util.getAttackType(thisPlayer());
//        AttackTypeInfo attackInfo = Util.getAttackTypeInfo(thisPlayer());
//
//        if (weaponInfo.ignoreAttributes()) weaponInfo = WeaponInfo.NO_WEAPON;
//
//        if (thisPlayer() instanceof ServerPlayer player) {
//            Util.rollCriticalFail(heldItem, attackInfo.failChance(), thisPlayer().getRandom(), player);
//        }
//
//        double distance = thisPlayer().distanceTo(instance);
//        if (distance < attackInfo.minReach()) amount *= CommonConfig.INEFFECTIVE_REACH_DAMAGE_MODIFIER.get();
//
//        damageInfo = new DamageInfo(attackInfo.lethality(), attackInfo.accuracy(), false, attackType, weaponInfo.getSpread().instance());
//        SkadaDamageSource skadaSource = new SkadaDamageSource(source, damageInfo);
//
//        return instance.hurt(skadaSource, amount);
//    }

    @ModifyArg(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    )
    private DamageSource convertDamageSource(DamageSource source) {
        DamageInfo damageInfo;
        ItemStack heldItem = thisPlayer().getMainHandItem();
        WeaponInfo weaponInfo = Util.getWeaponInfo(thisPlayer());
        AttackType attackType = Util.getAttackType(thisPlayer());
        AttackTypeInfo attackInfo = Util.getAttackTypeInfo(thisPlayer());

        if (weaponInfo.ignoreAttributes()) weaponInfo = WeaponInfo.NO_WEAPON;

        if (thisPlayer() instanceof ServerPlayer player) {
            Util.rollCriticalFail(heldItem, attackInfo.failChance(), thisPlayer().getRandom(), player);
        }

        damageInfo = new DamageInfo(attackInfo.lethality(), attackInfo.accuracy(), false, attackType, weaponInfo.getSpread().instance());
        return new SkadaDamageSource(source, damageInfo);
    }

    @Unique
    private Entity skada$target;

    @Inject(
            method = "attack",
            at = @At("HEAD")
    )
    private void captureTarget(Entity pTarget, CallbackInfo ci) {
        this.skada$target = pTarget;
    }

    @ModifyArg(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    )
    private float applyMinimumReachModification(float amount) {
        AttackTypeInfo attackInfo = Util.getAttackTypeInfo(thisPlayer());
        double distance = thisPlayer().distanceTo(skada$target);
        if (distance < attackInfo.minReach()) amount *= CommonConfig.INEFFECTIVE_REACH_DAMAGE_MODIFIER.get();
        return amount;
    }

}
