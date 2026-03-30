package com.cwjn.skada.data.damage;

import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public record DamageInfo(double lethality, double precision, boolean isEnvironmental, AttackType attackType, ElementSpreadInstance elementSpreadInstance) {

    public static DamageInfo environmental(ElementSpreadInstance spread) {
        return new DamageInfo(0, 0, true, AttackType.none(), spread);
    }

}
