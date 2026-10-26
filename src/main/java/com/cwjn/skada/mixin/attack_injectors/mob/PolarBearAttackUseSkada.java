package com.cwjn.skada.mixin.attack_injectors.mob;

import com.cwjn.skada.damage.SkadaDamageSource;
import com.cwjn.skada.util.UtilData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.PolarBear;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PolarBear.class)
public class PolarBearAttackUseSkada {

  @SuppressWarnings("all")
  private PolarBear thisMob() {
    return (PolarBear) (Object) this;
  }

  @ModifyArg(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
  private DamageSource convertDamageSource(DamageSource source) {
    return new SkadaDamageSource(source, UtilData.getMobDirectAttackDamageInfo(thisMob()));
  }
}