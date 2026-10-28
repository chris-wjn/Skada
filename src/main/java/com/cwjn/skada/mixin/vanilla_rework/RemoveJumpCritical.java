package com.cwjn.skada.mixin.vanilla_rework;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Removes the automatic jump-crit trigger while preserving the critical-hit extension and feedback path. */
@Mixin(Player.class)
public class RemoveJumpCritical {

    /** Supplies neutral defaults before the event; its vanilla-critical flag cannot be changed by a listener. */
    @ModifyArg(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/ForgeHooks;getCriticalHit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;ZF)Lnet/minecraftforge/event/entity/player/CriticalHitEvent;",
            remap = false), index = 2, require = 1, allow = 1)
    private boolean removeJumpCriticalTrigger(boolean vanillaCritical) {
        return false;
    }

    /** Removes the jump bonus from the initial modifier while allowing a critical rule to supply its own. */
    @ModifyArg(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/ForgeHooks;getCriticalHit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;ZF)Lnet/minecraftforge/event/entity/player/CriticalHitEvent;",
            remap = false), index = 3, require = 1, allow = 1)
    private float removeJumpCriticalMultiplier(float damageModifier) {
        return 1.0F;
    }
}
