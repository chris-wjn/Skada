package com.cwjn.skada.mixin.attack_injectors.mob;

import com.cwjn.skada.damage.OrdinaryMeleeKnockbackUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the ordinary component before Hoglin/Zoglin retain their separate shield throw response. */
@Mixin({Hoglin.class, Zoglin.class})
public abstract class HoglinShieldKnockback {
    /** Applies ordinary contact force before the entity's separate shield throw reaction. */
    @Inject(method = "blockedByShield", at = @At("HEAD"))
    private void applyOrdinaryKnockbackBeforeSpecialShieldThrow(LivingEntity target, CallbackInfo ci) {
        LivingEntity attacker = (LivingEntity) (Object) this;
        if (!attacker.level().isClientSide
                && target.isDamageSourceBlocked(attacker.damageSources().mobAttack(attacker))) {
            OrdinaryMeleeKnockbackUtil.apply(attacker, target,
                    OrdinaryMeleeKnockbackUtil.boundedKnockback(
                            attacker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_KNOCKBACK)));
        }
    }
}
