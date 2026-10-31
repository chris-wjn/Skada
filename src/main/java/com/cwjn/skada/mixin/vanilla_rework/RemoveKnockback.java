package com.cwjn.skada.mixin.vanilla_rework;

import com.cwjn.skada.damage.OrdinaryMeleeKnockbackUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reconciles vanilla hurt and shield shoves with source-scoped ordinary melee impulses. */
@Mixin(LivingEntity.class)
public class RemoveKnockback {

    @Unique DamageSource skada$lastDamageSource;

    /** Captures the active damage source for later shield and hurt-knockback decisions. */
    @Inject(method = "hurt", at = @At("HEAD"))
    private void captureDamageSourceForShieldBlock(DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        this.skada$lastDamageSource = source;
    }

    @Inject(
            method = "hurt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V",
            shift = At.Shift.BEFORE)
    )
    private void captureLastDamageSource(DamageSource pSource, float pAmount, CallbackInfoReturnable<Boolean> cir) {
        this.skada$lastDamageSource = pSource;
    }

    /** Records a shield contact for the active scoped mob attack. */
    @Inject(method = "blockUsingShield", at = @At("HEAD"))
    private void recordScopedMobShieldContact(LivingEntity attacker, CallbackInfo ci) {
        OrdinaryMeleeKnockbackUtil.recordShieldContact(attacker, (LivingEntity)(Object)this);
    }

    /** Removes the vanilla player-melee shield counter-push replaced by the derived outward impulse. */
    @Inject(method = "blockUsingShield", at = @At("HEAD"), cancellable = true)
    private void suppressPlayerMeleeShieldCounterPush(LivingEntity attacker, CallbackInfo ci) {
        DamageSource source = this.skada$lastDamageSource;
        if (source != null && source.is(DamageTypeTags.BYPASSES_COOLDOWN)
                && source.getEntity() instanceof Player) {
            // Player.attack applies the derived outward push to this shield holder after hurt() completes.
            ci.cancel();
        }
    }

    /** Suppresses only the overlapping default shield shove for attacks handled by the scoped mob path. */
    @Redirect(method = "blockedByShield",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void suppressDefaultShieldKnockback(LivingEntity target, double strength, double x, double z) {
        LivingEntity attacker = (LivingEntity)(Object)this;
        boolean hoglinSpecialSource = attacker instanceof Hoglin || attacker instanceof Zoglin;
        if (!hoglinSpecialSource && !OrdinaryMeleeKnockbackUtil.isCurrentAttack(attacker, target)) {
            target.knockback(strength, x, z);
        }
    }

    /** Replaces ordinary player or mob hurt knockback while preserving unrelated source behavior. */
    @Redirect(method = "hurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void replaceOrdinaryHurtKnockback(LivingEntity target, double strength, double x, double z) {
        DamageSource source = this.skada$lastDamageSource;
        boolean playerOrdinaryAttack = source != null
                && source.is(DamageTypeTags.BYPASSES_COOLDOWN)
                && source.getEntity() instanceof Player;
        boolean scopedMobAttack = source != null
                && OrdinaryMeleeKnockbackUtil.shouldSuppressHurtKnockback(source, target);
        if (playerOrdinaryAttack || scopedMobAttack) return;
        if (source != null) {
            if (source.getEntity() instanceof Hoglin || source.getEntity() instanceof Zoglin) {
                // This vanilla strength is only the signal that hurt() would shove; use authored ATTACK_KNOCKBACK instead.
                if (strength > 0.0 && !target.isDamageSourceBlocked(source)
                        && source.getEntity() instanceof LivingEntity attacker
                        && !attacker.level().isClientSide) {
                    OrdinaryMeleeKnockbackUtil.apply(attacker, target, attackKnockback(attacker));
                }
                return;
            }
            if (source.getEntity() instanceof Slime) {
                // SlimeContactKnockback adds the ordinary impulse only after accepted contact damage.
                return;
            }
        }
        target.knockback(strength, x, z);
    }

    /** Resolves an attacker's authored vanilla knockback attribute, or zero when it is absent. */
    private static double attackKnockback(LivingEntity attacker) {
        var attribute = attacker.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_KNOCKBACK);
        return attribute == null ? 0.0 : attribute.getValue();
    }
}
