package com.cwjn.skada.mixin.attack_injectors.player;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.damage.PlayerKnockbackUtil;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
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

  @Unique
  private double skada$attackCharge;

  @Unique
  private boolean skada$insideMinimumReach;

  @Unique
  private boolean skada$shieldContact;

  @Unique
  private boolean skada$attackAccepted;

  @Inject(method = "attack", at = @At("HEAD"))
  private void captureTarget(Entity pTarget, CallbackInfo ci) {
    this.skada$target = pTarget;
    this.skada$attack = AttackSelection.melee(thisPlayer().getMainHandItem());
    this.skada$attackCharge = thisPlayer().getAttackStrengthScale(0.5F);
    this.skada$insideMinimumReach = pTarget instanceof LivingEntity
        && thisPlayer().distanceTo(pTarget) < this.skada$attack.stats().minReach();
    this.skada$shieldContact = pTarget instanceof LivingEntity target
        && target.isDamageSourceBlocked(thisPlayer().damageSources().playerAttack(thisPlayer()));
    this.skada$attackAccepted = false;
  }

  /** Captures the accepted primary melee contact; the final impulse waits until vanilla velocity handling ends. */
  @ModifyExpressionValue(method = "attack", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
  private boolean captureAcceptedAttack(boolean accepted, Entity pTarget) {
    // A shield can block before the hurt cooldown gate and make hurt() return false; the contact still transfers force.
    this.skada$attackAccepted = (accepted || this.skada$shieldContact) && pTarget == this.skada$target;
    return accepted;
  }

  /** Applies the derived impulse after vanilla has finished its player-velocity and packet handling. */
  @Inject(method = "attack", at = @At("RETURN"))
  private void applyDerivedKnockback(CallbackInfo ci) {
    Player attacker = thisPlayer();
    if (!this.skada$attackAccepted || attacker.level().isClientSide
        || !(this.skada$target instanceof LivingEntity target)) return;

    double generated = this.skada$attack.stats().knockback();
    double otherAttribute = attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK) - generated;
    double distance = attacker.distanceTo(this.skada$target);
    boolean insideMinimumReach = this.skada$insideMinimumReach || distance < this.skada$attack.stats().minReach();
    int enchantmentBonus = EnchantmentHelper.getKnockbackBonus(attacker);
    boolean sprintBonusUsed = attacker.isSprinting() && this.skada$attackCharge > 0.9;
    double combined = PlayerKnockbackUtil.combinedKnockback(generated, otherAttribute, enchantmentBonus,
        attacker.isSprinting(), this.skada$attackCharge, insideMinimumReach,
        CommonConfig.INEFFECTIVE_REACH_DAMAGE_MODIFIER.get());

    if (combined > 0.0) {
      // LivingEntity.knockback subtracts its direction arguments from velocity, so pass the vector toward the attacker.
      target.knockback(combined * 0.5, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
    }
    if (sprintBonusUsed) {
      Vec3 movement = attacker.getDeltaMovement();
      attacker.setDeltaMovement(movement.multiply(0.6D, 1.0D, 0.6D));
      attacker.setSprinting(false);
    }
  }

  /** Removes the overlapping vanilla attack-bonus shove for the accepted primary living target. */
  @Redirect(method = "attack", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
  private void suppressVanillaAttackKnockback(LivingEntity target, double strength, double x, double z) {
    if (this.skada$attackAccepted && target == this.skada$target) return;
    target.knockback(strength, x, z);
  }

  /** Vanilla slows the attacker for enchantment-only knockback too; charge this cost only for sprint bonus use. */
  @Redirect(method = "attack", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
  private void suppressVanillaAttackSlowdown(Player player, Vec3 movement) {
    if (this.skada$attackAccepted && this.skada$target instanceof LivingEntity) return;
    player.setDeltaMovement(movement);
  }

  @Redirect(method = "attack", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
  private void suppressVanillaSprintCancellation(Player player, boolean sprinting) {
    if (this.skada$attackAccepted && this.skada$target instanceof LivingEntity) return;
    player.setSprinting(sprinting);
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
