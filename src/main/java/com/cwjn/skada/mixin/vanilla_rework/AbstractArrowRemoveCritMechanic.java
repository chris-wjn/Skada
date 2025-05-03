package com.cwjn.skada.mixin.vanilla_rework;

import com.cwjn.skada.Skada;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public class AbstractArrowRemoveCritMechanic {

//    @Inject(
//            method = "onHitEntity",
//            at = @At("HEAD")
//    )
    private void printVelocity(EntityHitResult pResult, CallbackInfo ci) {
        Skada.LOGGER.debug("delta movement length: {}", ((AbstractArrow)(Object)this).getDeltaMovement().length());
    }

    @Redirect(
            method = "setCritArrow",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;setFlag(IZ)V"
            )
    )
    private void removeArrowCritMechanic(AbstractArrow instance, int pId, boolean pValue) {
        /*leave this empty to essentially remove the function*/
    }

}
