package com.cwjn.skada.mixin.attack_injectors.mob;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;
import static com.cwjn.skada.data.SkadaData.WEAPON_INFO_TAG_KEY;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilGetMobArrowUseSkada {

    @Inject(
            method = "getMobArrow",
            at = @At(value = "RETURN")
    )
    private static void inject(LivingEntity pShooter, ItemStack pArrowStack, float pVelocity, CallbackInfoReturnable<AbstractArrow> cir) {
        ItemStack stack = pShooter.getItemInHand(ProjectileUtil.getWeaponHoldingHand(pShooter, item -> item instanceof net.minecraft.world.item.BowItem));
        if (stack.getTagElement(WEAPON_INFO_TAG_KEY) != null) {
            WeaponInfo info = WeaponInfo.fromCompoundTag(stack.getTagElement(WEAPON_INFO_TAG_KEY));
            AttackTypeInfo attackInfo =
                    info.getAttackTypes().get(
                            info.getAttackTypes().keySet().toArray(AttackType[]::new)[stack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)]
                    );
            double lethality = attackInfo.lethality();
            double accuracy = attackInfo.precision();
            ((AccessProjectileData) cir.getReturnValue()).setDamageInfo(new DamageInfo(
                    lethality,
                    accuracy,
                    false,
                    info.getAttackTypes().keySet().toArray(AttackType[]::new)[stack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)],
                    info.getSpread().instance()));
        }
        else {
            ((AccessProjectileData) cir.getReturnValue()).setDamageInfo(new DamageInfo(
                    0.0,
                    SkadaData.DEFAULT_ACCURACY,
                            false,
                            AttackType.thrust(),
                            WeaponInfo.NO_WEAPON.getSpread().instance()));
        }
    }

}
