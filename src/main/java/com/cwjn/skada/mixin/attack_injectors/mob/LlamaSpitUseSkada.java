package com.cwjn.skada.mixin.attack_injectors.mob;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.ProjectileLaunch;
import net.minecraft.world.item.ItemStack;
import com.cwjn.skada.data.registry.AttackType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.projectile.LlamaSpit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Llama.class)
public class LlamaSpitUseSkada {

    /*
     * When a llama spits at an entity, inject the spit projectile with a default DamageInfo object
     * since llamas don't have equipment to pull weapon info from.
     */
    @Inject(
            method = "spit",
            locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/LlamaSpit;shoot(DDDFF)V")
    )
    private void onLlamaSpitAttack(LivingEntity pTarget, CallbackInfo ci, LlamaSpit llama_spit) {
        ProjectileLaunch.resolve(ItemStack.EMPTY, AttackType.thrust(), SkadaData.DEFAULT_PRECISION, ProjectileLaunch.Flight.UNCHANGED)
                .attach(llama_spit, ItemStack.EMPTY, (Llama) (Object) this);
    }

}
