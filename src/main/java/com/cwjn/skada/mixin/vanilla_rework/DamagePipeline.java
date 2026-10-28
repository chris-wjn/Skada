package com.cwjn.skada.mixin.vanilla_rework;

import com.cwjn.skada.damage.DamageHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Owns Skada's calculation entry point and replacement of vanilla mitigation. */
public final class DamagePipeline {

    private DamagePipeline() {}

    /** Runs after all hurt listeners, only on Forge's uncancelled return branch. */
    @Mixin(value = ForgeHooks.class, remap = false)
    public static class MixinForgeHooks {

        /** Retains legitimate zero input without turning cancelled events into hits. */
        @ModifyExpressionValue(method = "onLivingHurt", at = @At(value = "INVOKE",
                target = "Lnet/minecraftforge/event/entity/living/LivingHurtEvent;getAmount()F", remap = false))
        private static float calculateDamage(float amount, LivingEntity target, DamageSource source, float originalAmount) {
            return DamageHandler.calculateDamage(target, source, amount);
        }
    }

    /** Living entities retain absorption, final damage events and vanilla health bookkeeping. */
    @Mixin(LivingEntity.class)
    public static class MixinLivingEntity {

        /** Skips both vanilla formulas and their side effects, as the previous redirects did. */
        @WrapOperation(method = "actuallyHurt", at = {
                @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"),
                @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
        }, require = 2)
        private float skipVanillaMitigation(LivingEntity target, DamageSource source, float amount, Operation<Float> original) {
            return amount;
        }
    }

    /** Players override actuallyHurt and therefore need the same mitigation replacement. */
    @Mixin(Player.class)
    public static class MixinPlayer {

        /** Applies Skada mitigation exactly once while retaining player statistics and exhaustion. */
        @WrapOperation(method = "actuallyHurt", at = {
                @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"),
                @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
        }, require = 2)
        private float skipVanillaMitigation(Player target, DamageSource source, float amount, Operation<Float> original) {
            return amount;
        }
    }
}
