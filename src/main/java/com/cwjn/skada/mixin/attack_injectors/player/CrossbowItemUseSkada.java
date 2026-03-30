package com.cwjn.skada.mixin.attack_injectors.player;

import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.UtilCombat;
import com.cwjn.skada.util.UtilData;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemUseSkada {

    @Shadow
    private static float getShootingPower(ItemStack pCrossbowStack) {
        return 0;
    }

    @Unique
    private static ItemStack thisCrossbow;

    @Inject(
            method = "shootProjectile",
            at = @At("HEAD")
    )
    private static void getCrossbow(Level pLevel, LivingEntity pShooter, InteractionHand pHand, ItemStack pCrossbowStack, ItemStack pAmmoStack, float pSoundPitch, boolean pIsCreativeMode, float pVelocity, float pInaccuracy, float pProjectileAngle, CallbackInfo ci) {
        thisCrossbow = pCrossbowStack;
    }

    /*
        * Inject the projectile with a DamageInfo object to be used later. Need to use 2 injectors because of the if statement.
     */
    @Inject(
            method = "shootProjectile",
            locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private static void onShootProjectile(Level pLevel, LivingEntity pShooter, InteractionHand pHand, ItemStack pCrossbowStack, ItemStack pAmmoStack, float pSoundPitch, boolean pIsCreativeMode, float pVelocity, float pInaccuracy, float pProjectileAngle, CallbackInfo ci, boolean flag, Projectile projectile) {
        WeaponInfo info = UtilData.getWeaponInfo(pCrossbowStack);
        if (!info.getAttackTypes().isEmpty()) {
            AttackType attackType = UtilData.getAttackType(pCrossbowStack);
            AttackTypeInfo attackInfo = UtilData.getAttackTypeInfo(pCrossbowStack);
            if (pShooter instanceof ServerPlayer player) {
                UtilCombat.rollCriticalFail(pCrossbowStack, attackInfo.failChance(), pShooter.getRandom(), player);
            }
            ((AccessProjectileData) projectile).setDamageInfo(new DamageInfo(
                    attackInfo.lethality(),
                    attackInfo.precision(),
                    false,
                attackType,
                    info.getSpread().instance()));
        }
    }

    @Redirect(
            method = "shootProjectile",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V")
    )
    private static void velocity(Projectile instance, double pX, double pY, double pZ, float pVelocity, float pInaccuracy) {
        WeaponInfo info = UtilData.getWeaponInfo(thisCrossbow);
        if (!info.getAttackTypes().isEmpty()) {
            AttackTypeInfo attackInfo = UtilData.getAttackTypeInfo(thisCrossbow);
            float inaccuracy = (float) UtilCombat.precisionScoreToProjectileInaccuracy(attackInfo.precision());
            float defaultVelocity = getShootingPower(thisCrossbow);
            float velocity = (float) UtilCombat.projectileVelocityWithDamageBonus(defaultVelocity, attackInfo.damage());
            instance.shoot(pX, pY, pZ, velocity, inaccuracy);
        }
        else {
            instance.shoot(pX, pY, pZ, pVelocity, pInaccuracy);
        }
    }




}
