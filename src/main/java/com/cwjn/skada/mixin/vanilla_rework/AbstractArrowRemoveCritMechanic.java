package com.cwjn.skada.mixin.vanilla_rework;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractArrow.class)
public class AbstractArrowRemoveCritMechanic {

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
