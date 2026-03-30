package com.cwjn.skada.mixin.attack_injectors.player;

import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.UtilCombat;
import com.cwjn.skada.util.UtilData;

import net.minecraft.server.level.ServerPlayer;
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

@Mixin(TridentItem.class)
public class TridentThrowUseSkada {

  @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrownTrident;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
  private void onTridentShootFromRotation(ThrownTrident thrownTrident, net.minecraft.world.entity.Entity shooter,
      float xRot, float yRot, float zRot, float velocity, float inaccuracy, ItemStack pStack) {
    WeaponInfo info = UtilData.getWeaponInfo(pStack);
    if (!info.getAttackTypes().isEmpty()) {
      AttackTypeInfo attackInfo = UtilData.getAttackTypeInfo(pStack);
      float adjustedVelocity = (float) UtilCombat.tridentProjectileVelocity(velocity, attackInfo.damage());
      float adjustedInaccuracy = (float) UtilCombat.precisionScoreToProjectileInaccuracy(attackInfo.precision());
      thrownTrident.shootFromRotation(shooter, xRot, yRot, zRot, adjustedVelocity, adjustedInaccuracy);
      return;
    }
    thrownTrident.shootFromRotation(shooter, xRot, yRot, zRot, velocity, inaccuracy);
  }

  /*
   * When releasing a thrown trident, inject the trident projectile with a
   * DamageInfo object to be used later.
   */
  @Inject(method = "releaseUsing", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", shift = At.Shift.BEFORE))
  private void onTridentRelease(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving, int pTimeLeft,
      CallbackInfo ci, Player player, int i, int j, ThrownTrident throwntrident) {
    WeaponInfo info = UtilData.getWeaponInfo(pStack);
    if (!info.getAttackTypes().isEmpty()) {
      AttackType attackType = UtilData.getAttackType(pStack);
      AttackTypeInfo attackInfo = UtilData.getAttackTypeInfo(pStack);
      double lethality = attackInfo.lethality();
      double precision = attackInfo.precision();
      ((AccessProjectileData) throwntrident).setDamageInfo(new DamageInfo(
          lethality,
          precision,
          false,
          attackType,
          info.getSpread().instance()));
    }
  }

  @Inject(method = "releaseUsing", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER))
  private void criticalFailOnThrow(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving, int pTimeLeft,
      CallbackInfo ci, Player player, int i, int j) {
    WeaponInfo info = UtilData.getWeaponInfo(pStack);
    if (!info.getAttackTypes().isEmpty()) {
      AttackTypeInfo attackInfo = UtilData.getAttackTypeInfo(pStack);
      UtilCombat.rollCriticalFail(pStack, attackInfo.failChance(), player.getRandom(), (ServerPlayer) player);
    }
  }

}
