package com.cwjn.skada.mixin.attack_injectors.player;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.ProjectileLaunch;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.UtilCombat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/** Captures thrown attacks once while retaining the separate riptide activation path. */
@Mixin(TridentItem.class)
public class TridentThrowUseSkada {
    @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrownTrident;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
    private void captureThrow(ThrownTrident projectile, Entity shooter, float x, float y, float z,
                             float velocity, float inaccuracy, ItemStack source) {
        ProjectileLaunch.resolve(source, AttackType.thrust(), SkadaData.DEFAULT_PRECISION, ProjectileLaunch.Flight.UNCHANGED)
                .attach(projectile, source, (LivingEntity) shooter);
        projectile.shootFromRotation(shooter, x, y, z, velocity, inaccuracy);
    }

    @Inject(method = "releaseUsing", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER))
    private void criticalFailOnRiptide(ItemStack source, Level level, LivingEntity user, int timeLeft,
                                     CallbackInfo ci, Player player, int charge, int riptide) {
        if (riptide > 0 && player instanceof ServerPlayer serverPlayer) {
            ProjectileLaunch launch = ProjectileLaunch.resolve(source, AttackType.thrust(), SkadaData.DEFAULT_PRECISION,
                    ProjectileLaunch.Flight.UNCHANGED);
            if (launch.stats() != null) UtilCombat.rollCriticalFail(source, launch.stats().failChance(), player.getRandom(), serverPlayer);
        }
    }
}
