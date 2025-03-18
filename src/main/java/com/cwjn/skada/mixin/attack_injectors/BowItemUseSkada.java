package com.cwjn.skada.mixin.attack_injectors;

import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;
import static com.cwjn.skada.data.SkadaData.WEAPON_INFO_TAG_KEY;

@Mixin(BowItem.class)
public class BowItemUseSkada {

    /*
        * When releasing a projectile from a BowItem, inject the projectile with a DamageInfo object to be used later.
     */
    @Inject(
            method = "releaseUsing",
            locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", shift = At.Shift.BEFORE)
    )
    private void onBowRelease(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving, int pTimeLeft, CallbackInfo ci, Player player, boolean flag, ItemStack itemstack, int i, float f, boolean flag1, ArrowItem arrowitem, AbstractArrow projectile, int j, int k) {
        if (pEntityLiving != null) {
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
