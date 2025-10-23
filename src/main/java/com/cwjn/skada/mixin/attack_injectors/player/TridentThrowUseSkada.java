package com.cwjn.skada.mixin.attack_injectors.player;

import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;
import static com.cwjn.skada.data.SkadaData.WEAPON_INFO_TAG_KEY;

@Mixin(TridentItem.class)
public class TridentThrowUseSkada {

    /*
     * When releasing a thrown trident, inject the trident projectile with a DamageInfo object to be used later.
     */
    @Inject(
            method = "releaseUsing",
            locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", shift = At.Shift.BEFORE)
    )
    private void onTridentRelease(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving, int pTimeLeft, CallbackInfo ci, Player player, int i, int j, ThrownTrident throwntrident) {
        if (pStack.getTagElement(WEAPON_INFO_TAG_KEY) != null) {
            WeaponInfo info = WeaponInfo.fromCompoundTag(pStack.getTagElement(WEAPON_INFO_TAG_KEY));
            AttackTypeInfo attackInfo =
                    info.getAttackTypes().get(
                            info.getAttackTypes().keySet().toArray(AttackType[]::new)[pStack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)]
                    );
            double lethality = attackInfo.lethality();
            double accuracy = attackInfo.precision();
            ((AccessProjectileData) throwntrident).setDamageInfo(new DamageInfo(
                    lethality,
                    accuracy,
                    false,
                    info.getAttackTypes().keySet().toArray(AttackType[]::new)[pStack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)],
                    info.getSpread().instance()));
        }
    }

    @Inject(
            method = "releaseUsing",
            locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
                    shift = At.Shift.AFTER)
    )
    private void criticalFailOnThrow(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving, int pTimeLeft, CallbackInfo ci, Player player, int i, int j) {
        if (pStack.getTagElement(WEAPON_INFO_TAG_KEY) != null) {
            WeaponInfo info = WeaponInfo.fromCompoundTag(pStack.getTagElement(WEAPON_INFO_TAG_KEY));
            AttackTypeInfo attackInfo =
                    info.getAttackTypes().get(
                            info.getAttackTypes().keySet().toArray(AttackType[]::new)[pStack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)]
                    );
            Util.rollCriticalFail(pStack, attackInfo.failChance(), player.getRandom(), (ServerPlayer) player);
        }
    }

}
