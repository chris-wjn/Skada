package com.cwjn.skada.mixin.new_features;

import com.cwjn.skada.event.Onboarding;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies tutorial immunity without touching vanilla's persisted invulnerability flags. */
@Mixin(Entity.class)
public abstract class TutorialDamageProtection {
    /** Protects even from damage types that normally bypass ordinary invulnerability. */
    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void skada$protected(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (Onboarding.isProtected((Entity) (Object) this)) cir.setReturnValue(true);
    }
}
