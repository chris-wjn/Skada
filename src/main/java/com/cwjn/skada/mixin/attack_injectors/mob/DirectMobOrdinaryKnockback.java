package com.cwjn.skada.mixin.attack_injectors.mob;

import com.cwjn.skada.damage.OrdinaryMeleeKnockbackUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Wolf;

/** Direct ordinary animal overrides that do not delegate damage through Mob.doHurtTarget. */
@Mixin(value = {Axolotl.class, Bee.class, Cat.class, Dolphin.class, IronGolem.class, Ocelot.class,
        PolarBear.class, Parrot.class, Rabbit.class, Wolf.class})
public abstract class DirectMobOrdinaryKnockback {
    @Unique
    private Entity skada$directAttackTarget;

    @Unique
    private LivingEntity skada$directAttacker;

    /** Returns the mob instance represented by this mixin. */
    @SuppressWarnings("all")
    private LivingEntity skada$thisAttacker() {
        return (LivingEntity)(Object)this;
    }

    /** Opens a scoped ordinary-attack frame before a direct mob override executes. */
    @Inject(method = "doHurtTarget", at = @At("HEAD"))
    private void skada$beginDirectAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        this.skada$directAttackTarget = target;
        this.skada$directAttacker = skada$thisAttacker();
        OrdinaryMeleeKnockbackUtil.beginAttack(this.skada$directAttacker, target);
    }

    /** Records whether the direct override's damage call was accepted. */
    @ModifyExpressionValue(method = "doHurtTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean skada$captureDirectAttackResult(boolean accepted, Entity target) {
        Mob attacker = (Mob)(Object)this;
        DamageSource source = attacker instanceof Bee bee
                ? attacker.damageSources().sting(bee)
                : attacker.damageSources().mobAttack(attacker);
        return OrdinaryMeleeKnockbackUtil.recordResult(source, target, accepted);
    }

    /** Closes the scoped frame after the direct override has finished its effects. */
    @Inject(method = "doHurtTarget", at = @At("RETURN"))
    private void skada$finishDirectAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (this.skada$directAttacker != null && target == this.skada$directAttackTarget) {
            OrdinaryMeleeKnockbackUtil.finishAttack(this.skada$directAttacker, target);
        }
        this.skada$directAttackTarget = null;
        this.skada$directAttacker = null;
    }
}
