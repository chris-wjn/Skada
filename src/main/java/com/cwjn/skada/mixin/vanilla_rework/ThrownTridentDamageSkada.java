package com.cwjn.skada.mixin.vanilla_rework;

import com.cwjn.skada.data.damage.AttackSelection;
import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Replaces the vanilla thrown-trident base damage with the trident's configured damage value.
 */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentDamageSkada {

    @Shadow
    private ItemStack tridentItem;

    @ModifyConstant(method = "onHitEntity", constant = @Constant(floatValue = 8.0F))
    private float useConfiguredTridentDamage(float originalDamage) {
        AccessProjectileData projectile = (AccessProjectileData) this;
        if (projectile.hasDamageInfo()) {
            Double damage = projectile.getDamageInfo().projectileDamage();
            if (damage != null) return damage.floatValue();
        }
        // Old saves may have no snapshot, or an older snapshot without captured base damage.
        AttackSelection selected = AttackSelection.resolve(this.tridentItem);
        AttackTypeInfo stats = projectile.hasDamageInfo()
                ? selected.weapon().getAttackTypes().get(projectile.getDamageInfo().attackType()) : selected.stats();
        return selected.configured() && selected.weapon().applicability().projectile() && stats != null
                ? (float) stats.damage() : originalDamage;
    }

}
