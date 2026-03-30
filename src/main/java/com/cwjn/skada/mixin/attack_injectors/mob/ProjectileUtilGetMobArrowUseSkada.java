package com.cwjn.skada.mixin.attack_injectors.mob;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.Util;
import com.cwjn.skada.util.UtilData;

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
        WeaponInfo info = UtilData.getWeaponInfo(stack);
        if (!info.getAttackTypes().isEmpty()) {
            AttackType attackType = UtilData.getAttackType(stack);
            AttackTypeInfo attackInfo = UtilData.getAttackTypeInfo(stack);
            double lethality = attackInfo.lethality();
            double precision = attackInfo.precision();
            ((AccessProjectileData) cir.getReturnValue()).setDamageInfo(new DamageInfo(
                    lethality,
                    precision,
                    false,
                    attackType,
                    info.getSpread().instance()));
        }
        else {
            ((AccessProjectileData) cir.getReturnValue()).setDamageInfo(new DamageInfo(
                    0.0,
                    SkadaData.DEFAULT_PRECISION,
                            false,
                            AttackType.thrust(),
                            WeaponInfo.NO_WEAPON.getSpread().instance()));
        }
    }

}
