package com.cwjn.skada.mixin.attack_injectors;

import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;
import static com.cwjn.skada.data.SkadaData.WEAPON_INFO_TAG_KEY;

@Mixin(CrossbowItem.class)
public class CrossbowItemUseSkada {

    /*
        * When releasing a projectile from a CrossbowItem, inject the projectile with a DamageInfo object to be used later.
     */
    @Inject(
            method = "shootProjectile",
            locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
                    shift = At.Shift.BEFORE
            )
    )
    private static void onShootProjectile(Level pLevel, LivingEntity pShooter, InteractionHand pHand, ItemStack pStack, ItemStack pAmmoStack, float pSoundPitch, boolean pIsCreativeMode, float pVelocity, float pInaccuracy, float pProjectileAngle, CallbackInfo ci, boolean flag, Projectile projectile) {
        if (pShooter != null) {
            if (pStack.getTagElement(WEAPON_INFO_TAG_KEY) != null) {
                WeaponInfo info = WeaponInfo.fromCompoundTag(pStack.getTagElement(WEAPON_INFO_TAG_KEY));
                AttackTypeInfo attackInfo =
                        info.getAttackTypes().get(
                                info.getAttackTypes().keySet().toArray(AttackType[]::new)[pStack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)]
                        );
                double lethality = attackInfo.lethality();
                ((AccessProjectileData) projectile).setDamageInfo(new DamageInfo(
                        lethality,
                        false,
                        info.getAttackTypes().keySet().toArray(AttackType[]::new)[pStack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)],
                        info.getSpread().instance()));
            }
        }
    }

}
