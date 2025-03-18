package com.cwjn.skada.data.damage;

import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;
import static com.cwjn.skada.data.SkadaData.WEAPON_INFO_TAG_KEY;

public record DamageInfo(double lethality, boolean isEnvironmental, AttackType attackType, ElementSpreadInstance elementSpreadInstance) {

    public static DamageInfo environmental(ElementSpreadInstance spread) {
        return new DamageInfo(0, true, AttackType.none(), spread);
    }

    public static DamageInfo fromLivingEntity(LivingEntity livingEntity) {
        ItemStack i = livingEntity.getMainHandItem();
        if (i == ItemStack.EMPTY) {
            return new DamageInfo(0, false, AttackType.none(), new ElementSpreadInstance());
        }
        else {
            if (!i.hasTag() || !i.getTag().contains(WEAPON_INFO_TAG_KEY)) {
                return new DamageInfo(0, false, AttackType.none(), new ElementSpreadInstance());
            }
            WeaponInfo currWeaponInfo = WeaponInfo.fromCompoundTag(i.getTagElement(WEAPON_INFO_TAG_KEY));
            AttackType[] attackTypes = currWeaponInfo.getAttackTypes().keySet().toArray(AttackType[]::new);
            AttackTypeInfo attackInfo = currWeaponInfo.getAttackTypes().get(attackTypes[i.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)]);
            return new DamageInfo(attackInfo.lethality(), false, attackTypes[i.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)], currWeaponInfo.getSpread().instance());
        }
    }

}
