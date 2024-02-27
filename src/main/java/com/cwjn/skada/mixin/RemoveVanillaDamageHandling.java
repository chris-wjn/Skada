package com.cwjn.skada.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

public class RemoveVanillaDamageHandling {

    @Mixin(Player.class)
    public static class MixinPlayer {

        @Redirect(
                method = "actuallyHurt",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
                )
        )
        private float getDamageAfterArmorAbsorb(Player player, net.minecraft.world.damagesource.DamageSource source, float damage) {
            return damage;
        }

        @Redirect(
                method = "actuallyHurt",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
                )
        )
        private float getDamageAfterMagicAbsorb(Player player, net.minecraft.world.damagesource.DamageSource source, float damage) {
            return damage;
        }

    }

    @Mixin(LivingEntity.class)
    public static class MixinLivingEntity {

        @Redirect(
                method = "actuallyHurt",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
                )
        )
        private float getDamageAfterArmorAbsorb(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float damage) {
            return damage;
        }

        @Redirect(
                method = "actuallyHurt",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
                )
        )
        private float getDamageAfterMagicAbsorb(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float damage) {
            return damage;
        }

    }

}
