package com.cwjn.skada.mixin.new_features;

import com.cwjn.skada.event.Onboarding;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Excludes protected players from vanilla enemy selection, including brain-based targeting. */
@Mixin(LivingEntity.class)
public abstract class TutorialTargetProtection {
    /** Supplements target-change events with the common enemy eligibility check. */
    @Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
    private void skada$enemy(CallbackInfoReturnable<Boolean> cir) {
        if (Onboarding.isProtected((LivingEntity) (Object) this)) cir.setReturnValue(false);
    }
}
