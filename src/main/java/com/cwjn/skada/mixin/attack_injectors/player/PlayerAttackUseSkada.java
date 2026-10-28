package com.cwjn.skada.mixin.attack_injectors.player;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.damage.SkadaDamageSource;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.AttackSelection;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.UtilCombat;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Player.class, priority = 1100)
public class PlayerAttackUseSkada {

  /** Lets base-only elemental attacks enter vanilla combat without inventing physical damage. */
  @Definition(id = "damage", local = @Local(type = float.class, ordinal = 0))
  @Expression("damage > 0.0")
  @ModifyExpressionValue(method = "attack", at = @At("MIXINEXTRAS:EXPRESSION"), require = 1, allow = 1)
  private boolean allowBaseElementalAttack(boolean original, Entity target) {
    if (original) return true;
    if (!(target instanceof LivingEntity)) return false;
    for (Element element : SkadaData.REGISTRY_ELEMENT.get().getValues()) {
      if (thisPlayer().getAttributeValue(element.baseDamage()) > 0.0) return true;
    }
    return false;
  }

  @SuppressWarnings("all")
  private Player thisPlayer() {
    return (Player) (Object) this;
  }

  @ModifyArg(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
  private DamageSource convertDamageSource(DamageSource source) {
    DamageInfo damageInfo;
    ItemStack heldItem = thisPlayer().getMainHandItem();
    AttackSelection selected = skada$attack;
    AttackTypeInfo attackInfo = selected.stats();

    if (thisPlayer() instanceof ServerPlayer player) {
      UtilCombat.rollCriticalFail(heldItem, attackInfo.failChance(), thisPlayer().getRandom(), player);
    }

    damageInfo = selected.damageInfo();
    return new SkadaDamageSource(source, damageInfo);
  }

  @Unique
  private Entity skada$target;

  @Unique
  private AttackSelection skada$attack;

  @Inject(method = "attack", at = @At("HEAD"))
  private void captureTarget(Entity pTarget, CallbackInfo ci) {
    this.skada$target = pTarget;
    this.skada$attack = AttackSelection.melee(thisPlayer().getMainHandItem());
  }

  @ModifyArg(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
  private float applyMinimumReachModification(float amount) {
    AttackTypeInfo attackInfo = skada$attack.stats();
    double distance = thisPlayer().distanceTo(skada$target);
    if (distance < attackInfo.minReach())
      amount *= CommonConfig.INEFFECTIVE_REACH_DAMAGE_MODIFIER.get();
    return amount;
  }

}
