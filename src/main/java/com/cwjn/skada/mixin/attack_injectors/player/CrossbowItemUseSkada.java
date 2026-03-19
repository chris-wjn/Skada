package com.cwjn.skada.mixin.attack_injectors.player;

import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.Util;
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

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;
import static com.cwjn.skada.data.SkadaData.WEAPON_INFO_TAG_KEY;

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
        if (pCrossbowStack.getTagElement(WEAPON_INFO_TAG_KEY) != null) {
            WeaponInfo info = WeaponInfo.fromCompoundTag(pCrossbowStack.getTagElement(WEAPON_INFO_TAG_KEY));
            AttackTypeInfo attackInfo =
                    info.getAttackTypes().get(
                            info.getAttackTypes().keySet().toArray(AttackType[]::new)[pCrossbowStack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)]
                    );
            if (pShooter instanceof ServerPlayer player) {
                Util.rollCriticalFail(pCrossbowStack, attackInfo.failChance(), pShooter.getRandom(), player);
            }
            ((AccessProjectileData) projectile).setDamageInfo(new DamageInfo(
                    attackInfo.lethality(),
                    attackInfo.precision(),
                    false,
                    info.getAttackTypes().keySet().toArray(AttackType[]::new)[pCrossbowStack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)],
                    info.getSpread().instance()));
        }
    }

    @Redirect(
            method = "shootProjectile",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V")
    )
    private static void velocity(Projectile instance, double pX, double pY, double pZ, float pVelocity, float pInaccuracy) {
        if (thisCrossbow.getTagElement(WEAPON_INFO_TAG_KEY) != null) {
            WeaponInfo info = WeaponInfo.fromCompoundTag(thisCrossbow.getTagElement(WEAPON_INFO_TAG_KEY));
            AttackTypeInfo attackInfo =
                    info.getAttackTypes().get(
                            info.getAttackTypes().keySet().toArray(AttackType[]::new)[thisCrossbow.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)]
                    );
                float inaccuracy = (float) Util.precisionScoreToProjectileInaccuracy(attackInfo.precision());
            float defaultVelocity = getShootingPower(thisCrossbow);
            float velocity = (float) Util.projectileVelocityWithDamageBonus(defaultVelocity, attackInfo.damageBonus());
            instance.shoot(pX, pY, pZ, velocity, inaccuracy);
        }
        else {
            instance.shoot(pX, pY, pZ, pVelocity, pInaccuracy);
        }
    }




}
