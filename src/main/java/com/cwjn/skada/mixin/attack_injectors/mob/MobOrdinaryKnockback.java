package com.cwjn.skada.mixin.attack_injectors.mob;

import com.cwjn.skada.damage.OrdinaryMeleeKnockbackUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/** Replaces the ordinary default Mob attack shove while leaving each subclass's other effects intact. */
@Mixin(Mob.class)
public abstract class MobOrdinaryKnockback {
    /** Returns the mob instance represented by this mixin. */
    @SuppressWarnings("all")
    private Mob skada$thisMob() {
        return (Mob)(Object)this;
    }

    /** Opens a source-scoped ordinary attack frame before the default mob route. */
    @Inject(method = "doHurtTarget", at = @At("HEAD"))
    private void skada$beginMobAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        OrdinaryMeleeKnockbackUtil.beginAttack(skada$thisMob(), target);
    }

    /** Captures whether the default mob damage call was accepted. */
    @ModifyExpressionValue(method = "doHurtTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean skada$captureMobAttackResult(boolean accepted, Entity target) {
        return OrdinaryMeleeKnockbackUtil.recordResult(skada$thisMob().damageSources().mobAttack(skada$thisMob()), target, accepted);
    }

    /** Replaces the overlapping vanilla target shove, which is applied once after accepted contact. */
    @Redirect(method = "doHurtTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void skada$suppressDefaultAttackKnockback(LivingEntity target, double strength, double x, double z) {
        // The one derived impulse is applied after hurt and the subclass' accepted-hit effects return.
    }

    /** Removes the vanilla attacker slowdown that only accompanied its old knockback formula. */
    @Redirect(method = "doHurtTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void skada$suppressAttackSlowdown(Mob mob, Vec3 movement) {
        // Mob attack knockback no longer imposes vanilla slowdown on the attacker.
    }

    /** Closes the scoped frame and applies its single ordinary impulse when the hit was accepted. */
    @Inject(method = "doHurtTarget", at = @At("RETURN"))
    private void skada$finishMobAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        OrdinaryMeleeKnockbackUtil.finishAttack(skada$thisMob(), target);
    }
}
