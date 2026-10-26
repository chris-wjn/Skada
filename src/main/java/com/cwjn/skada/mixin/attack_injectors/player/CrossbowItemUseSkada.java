package com.cwjn.skada.mixin.attack_injectors.player;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.ProjectileLaunch;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/** Seeds both crossbow aiming branches before flight, including mobs, rockets and multishot. */
@Mixin(CrossbowItem.class)
public class CrossbowItemUseSkada {
    @Inject(method = "shootProjectile", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getUpVector(F)Lnet/minecraft/world/phys/Vec3;"))
    private static void capturePlayerLaunch(Level level, LivingEntity shooter, InteractionHand hand, ItemStack source,
            ItemStack ammo, float soundPitch, boolean creative, float velocity, float inaccuracy, float angle,
            CallbackInfo ci, boolean rocket, Projectile projectile) {
        ProjectileLaunch.resolve(source, AttackType.thrust(), SkadaData.DEFAULT_PRECISION, ProjectileLaunch.Flight.CROSSBOW)
                .attach(projectile, source, shooter);
    }

    @Inject(method = "shootProjectile", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/CrossbowAttackMob;shootCrossbowProjectile(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/projectile/Projectile;F)V"))
    private static void captureMobLaunch(Level level, LivingEntity shooter, InteractionHand hand, ItemStack source,
            ItemStack ammo, float soundPitch, boolean creative, float velocity, float inaccuracy, float angle,
            CallbackInfo ci, boolean rocket, Projectile projectile) {
        ProjectileLaunch.resolve(source, AttackType.thrust(), SkadaData.DEFAULT_PRECISION, ProjectileLaunch.Flight.CROSSBOW)
                .attach(projectile, source, shooter);
    }
}
