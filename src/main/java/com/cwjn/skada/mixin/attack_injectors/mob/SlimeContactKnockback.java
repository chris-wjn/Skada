package com.cwjn.skada.mixin.attack_injectors.mob;

import com.cwjn.skada.damage.OrdinaryMeleeKnockbackUtil;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Adds one ordinary derived impulse to accepted slime and magma-cube contact attacks. */
@Mixin(Slime.class)
public abstract class SlimeContactKnockback {
    /** Adds one derived impulse after accepted damage or a real shield block. */
    @ModifyExpressionValue(method = "dealDamage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean applyAcceptedContactKnockback(boolean accepted, LivingEntity target) {
        Slime attacker = (Slime) (Object) this;
        boolean shieldContact = target.isDamageSourceBlocked(attacker.damageSources().mobAttack(attacker));
        if ((accepted || shieldContact) && !attacker.level().isClientSide && target.isAlive()) {
            var attackKnockback = attacker.getAttribute(Attributes.ATTACK_KNOCKBACK);
            double value = attackKnockback == null ? 0.0 : attackKnockback.getValue();
            OrdinaryMeleeKnockbackUtil.apply(attacker, target, value);
        }
        return accepted;
    }
}
