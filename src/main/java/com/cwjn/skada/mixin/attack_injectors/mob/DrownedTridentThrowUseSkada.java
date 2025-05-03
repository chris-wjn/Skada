package com.cwjn.skada.mixin.attack_injectors.mob;

import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;
import static com.cwjn.skada.data.SkadaData.WEAPON_INFO_TAG_KEY;

@Mixin(Drowned.class)
public class DrownedTridentThrowUseSkada {

    @SuppressWarnings("all")
    private Drowned thisDrowned() {
        return (Drowned) (Object) this;
    }

    @Inject(
            method = "performRangedAttack",
            locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrownTrident;shoot(DDDFF)V")
    )
    private void inject(LivingEntity pTarget, float pDistanceFactor, CallbackInfo ci, ThrownTrident tr) {
        ItemStack stack = thisDrowned().getItemInHand(ProjectileUtil.getWeaponHoldingHand(thisDrowned(), item -> item instanceof net.minecraft.world.item.BowItem));
        if (stack.getTagElement(WEAPON_INFO_TAG_KEY) != null) {
            WeaponInfo info = WeaponInfo.fromCompoundTag(stack.getTagElement(WEAPON_INFO_TAG_KEY));
            AttackTypeInfo attackInfo =
                    info.getAttackTypes().get(
                            info.getAttackTypes().keySet().toArray(AttackType[]::new)[stack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)]
                    );
            double lethality = attackInfo.lethality();
            double accuracy = attackInfo.accuracy();
            ((AccessProjectileData) tr).setDamageInfo(new DamageInfo(
                    lethality,
                    accuracy,
                    false,
                    info.getAttackTypes().keySet().toArray(AttackType[]::new)[stack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)],
                    info.getSpread().instance()));
        }
        else {
            ((AccessProjectileData) tr).setDamageInfo(new DamageInfo(
                    0.0,
                    SkadaData.DEFAULT_ACCURACY,
                    false,
                    AttackType.thrust(),
                    WeaponInfo.NO_WEAPON.getSpread().instance()));
        }
    }

}
