package com.cwjn.skada.mixin.vanilla_rework;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class RemoveKnockback {

    @Unique DamageSource skada$lastDamageSource;

    @Inject(
            method = "hurt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V",
            shift = At.Shift.BEFORE)
    )
    private void captureLastDamageSource(DamageSource pSource, float pAmount, CallbackInfoReturnable<Boolean> cir) {
        this.skada$lastDamageSource = pSource;
    }

    @ModifyArg(method = "hurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"),
            index = 0)
    private double modifyKnockbackStrength(double strength) {
        if (this.skada$lastDamageSource != null && this.skada$lastDamageSource.getEntity() instanceof Player player) {
            if (player.getAttackStrengthScale(0.5F) < 0.5F) {
                return 0.0F;
            }
        }
        return strength;
    }

}
