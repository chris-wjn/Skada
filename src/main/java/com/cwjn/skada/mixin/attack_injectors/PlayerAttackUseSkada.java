package com.cwjn.skada.mixin.attack_injectors;

import com.cwjn.skada.Config;
import com.cwjn.skada.damage.SkadaDamageSource;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;
import static com.cwjn.skada.data.SkadaData.WEAPON_INFO_TAG_KEY;

@Mixin(Player.class)
public class PlayerAttackUseSkada {

    @SuppressWarnings("all")
    private Player thisPlayer() {
        return (Player) (Object) this;
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean addDamageInfoToPlayerAttack(Entity instance, DamageSource source, float amount) {
        DamageInfo damageInfo;
        ItemStack heldItem = thisPlayer().getMainHandItem();
        WeaponInfo weaponInfo;
        if (heldItem.hasTag() && heldItem.getTagElement(WEAPON_INFO_TAG_KEY) != null) {
            weaponInfo = WeaponInfo.fromCompoundTag(heldItem.getTagElement(WEAPON_INFO_TAG_KEY));
        }
        else {
            damageInfo = new DamageInfo(0.0, false, AttackType.strike(), WeaponInfo.NO_WEAPON.getSpread().instance());
            SkadaDamageSource skadaSource = new SkadaDamageSource(source, damageInfo);
            return instance.hurt(skadaSource, amount);
        }
        AttackType attackType = weaponInfo.getAttackTypes().keySet().toArray(AttackType[]::new)[heldItem.getOrCreateTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)];
        AttackTypeInfo attackInfo = weaponInfo.getAttackTypes().get(attackType);
        double distance = thisPlayer().distanceTo(instance);
        if (distance < attackInfo.minReach()) amount *= Config.INEFFECTIVE_REACH_DAMAGE_MODIFIER.get();
        damageInfo = new DamageInfo(attackInfo.lethality(), false, attackType, weaponInfo.getSpread().instance());
        SkadaDamageSource skadaSource = new SkadaDamageSource(source, damageInfo);
        return instance.hurt(skadaSource, amount);
    }

}
