package com.cwjn.skada.mixin.attack_injectors.player;

import com.cwjn.skada.data.damage.ProjectileLaunch;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Captures the exact released bow; shared projectile flight code preserves its charge scaling. */
@Mixin(BowItem.class)
public class BowItemUseSkada {
    @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
    private void onBowRelease(AbstractArrow arrow, Entity shooter, float x, float y, float z,
                              float velocity, float inaccuracy, ItemStack source) {
        ProjectileLaunch.resolve(source, AttackType.thrust(), 0.0, ProjectileLaunch.Flight.BOW)
                .attach(arrow, source, (LivingEntity) shooter);
        arrow.shootFromRotation(shooter, x, y, z, velocity, inaccuracy);
    }
}
