package com.cwjn.skada.mixin.attack_injectors.mob;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.ProjectileLaunch;
import com.cwjn.skada.data.registry.AttackType;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilGetMobArrowUseSkada {

    @Inject(
            method = "getMobArrow",
            at = @At(value = "RETURN")
    )
    private static void inject(LivingEntity pShooter, ItemStack pArrowStack, float pVelocity, CallbackInfoReturnable<AbstractArrow> cir) {
        ItemStack stack = pShooter.getItemInHand(ProjectileUtil.getWeaponHoldingHand(pShooter, item -> item instanceof net.minecraft.world.item.BowItem));
        ProjectileLaunch.resolve(stack, AttackType.thrust(), SkadaData.DEFAULT_PRECISION, ProjectileLaunch.Flight.UNCHANGED)
                .attach(cir.getReturnValue(), stack, pShooter);
    }

}
