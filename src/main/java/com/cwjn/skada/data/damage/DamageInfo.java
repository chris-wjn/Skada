package com.cwjn.skada.data.damage;

import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import static com.cwjn.skada.data.SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY;
import static com.cwjn.skada.data.SkadaData.WEAPON_INFO_TAG_KEY;

public record DamageInfo(double lethality, double accuracy, boolean isEnvironmental, AttackType attackType, ElementSpreadInstance elementSpreadInstance) {

    public static DamageInfo environmental(ElementSpreadInstance spread) {
        return new DamageInfo(0, 0, true, AttackType.none(), spread);
    }

}
