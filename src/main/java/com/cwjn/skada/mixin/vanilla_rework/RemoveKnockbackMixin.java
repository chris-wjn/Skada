package com.cwjn.skada.mixin.vanilla_rework;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class RemoveKnockbackMixin {

    @Redirect(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void removeKnockback(LivingEntity instance, double direction, double velocity, double strength, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            if (player.getAttackStrengthScale(0.5F) >= 0.5) {
                instance.knockback(direction, velocity, strength);
            }
        }
    }

}
