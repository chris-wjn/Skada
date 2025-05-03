package com.cwjn.skada.mixin.attack_injectors.player;

import com.cwjn.skada.data.damage.*;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;
import static com.cwjn.skada.data.SkadaData.WEAPON_INFO_TAG_KEY;

@Mixin(BowItem.class)
public class BowItemUseSkada {

    /*
     * Use the bow's accuracy and damage bonus to affect the projectile's deviation from crosshair and velocity, and inject the projectile's damage info.
     * @param entity is never null because it is checked to be a Player in the original method
     */
    @Redirect(
            method = "releaseUsing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V")
    )
    private void onBowRelease(AbstractArrow instance, Entity entity, float x, float y, float z, float pVelocity, float inaccuracy, ItemStack pStack) {
        if (pStack.getTagElement(WEAPON_INFO_TAG_KEY) != null) {
            float chargePower = pVelocity / 3; //get the charge strength (how long the player has held right click) by dividing out the original velocity multiplier
            inaccuracy = 1.0F; //inaccuracy parameter is always 1.0F in the original method, so this is just for clarity
            WeaponInfo info = WeaponInfo.fromCompoundTag(pStack.getTagElement(WEAPON_INFO_TAG_KEY));
            AttackTypeInfo attackInfo =
                    info.getAttackTypes().get(
                            info.getAttackTypes().keySet().toArray(AttackType[]::new)[pStack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)]
                    );
            inaccuracy = (float) (15 * (1 - attackInfo.accuracy()));
            float velocity = (float) (chargePower * (3 + attackInfo.damageBonus()));
            Util.rollCriticalFail(pStack, attackInfo.failChance(), ((ServerPlayer)entity).getRandom(), ((ServerPlayer)entity));
            ((AccessProjectileData) instance).setDamageInfo(new DamageInfo(
                    attackInfo.lethality(),
                    attackInfo.accuracy(),
                    false,
                    info.getAttackTypes().keySet().toArray(AttackType[]::new)[pStack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)],
                    info.getSpread().instance()));
            instance.shootFromRotation(entity, x, y, z, velocity, inaccuracy);
        }
        else {
            ((AccessProjectileData) instance).setDamageInfo(new DamageInfo(
                    0,
                    0,
                    false,
                    AttackType.thrust(),
                    new ElementSpreadInstance()));
            instance.shootFromRotation(entity, x, y, z, pVelocity, inaccuracy);
        }
    }

}
