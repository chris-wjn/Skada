package com.cwjn.skada.mixin.new_features;

import com.cwjn.skada.client.ClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.phys.HitResult.Type.*;

@Mixin(Minecraft.class)
public class CustomReticlesHitDetectedEntities {

    @SuppressWarnings("all")
    private Minecraft thisMinecraft() {
        return (Minecraft)(Object)this;
    }
    @Shadow
    protected int missTime;
    @Final
    @Shadow
    private static Logger LOGGER;

    /**
     * @author cwJn
     * @reason custom reticles implementation for hitting entities that have been detected.

    @Overwrite
    private boolean startAttack() {
        if (missTime > 0) {
            return false;
        } else if (thisMinecraft().hitResult == null) {
            LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
            if (thisMinecraft().gameMode.hasMissTime()) {
                missTime = 10;
            }
            return false;
        } else if (thisMinecraft().player.isHandsBusy()) {
            return false;
        } else {
            ItemStack itemstack = thisMinecraft().player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!itemstack.isItemEnabled(thisMinecraft().level.enabledFeatures())) {
                return false;
            } else {
                boolean flag = false;
                var inputEvent = net.minecraftforge.client.ForgeHooksClient.onClickInput(0, thisMinecraft().options.keyAttack, InteractionHand.MAIN_HAND);
                if (!inputEvent.isCanceled()) {
                    List<Entity> alreadyHit = new ArrayList<>();
                    for (HitResult hitResult : ClientHandler.hitResults) {
                        if (hitResult.getType() == ENTITY) {
                            if (!alreadyHit.contains(((EntityHitResult) hitResult).getEntity())) {
                                thisMinecraft().gameMode.attack(thisMinecraft().player, ((EntityHitResult) hitResult).getEntity());
                                alreadyHit.add(((EntityHitResult) hitResult).getEntity());
                            }
                        }
                    }
                    switch (thisMinecraft().hitResult.getType()) {
                        case ENTITY:
                            if (!alreadyHit.contains(((EntityHitResult) thisMinecraft().hitResult).getEntity()))
                                thisMinecraft().gameMode.attack(thisMinecraft().player, ((EntityHitResult) thisMinecraft().hitResult).getEntity());
                            break;
                        case BLOCK:
                            BlockHitResult blockhitresult = (BlockHitResult) thisMinecraft().hitResult;
                            BlockPos blockpos = blockhitresult.getBlockPos();
                            if (!thisMinecraft().level.isEmptyBlock(blockpos)) {
                                thisMinecraft().gameMode.startDestroyBlock(blockpos, blockhitresult.getDirection());
                                if (thisMinecraft().level.getBlockState(blockpos).isAir()) {
                                    flag = true;
                                }
                                break;
                            }
                    }
                    if (!alreadyHit.isEmpty()) {
                        if (thisMinecraft().gameMode.hasMissTime()) {
                            missTime = 10;
                        }
                        thisMinecraft().player.resetAttackStrengthTicker();
                        net.minecraftforge.common.ForgeHooks.onEmptyLeftClick(thisMinecraft().player);
                    }
                }
                if (inputEvent.shouldSwingHand())
                    thisMinecraft().player.swing(InteractionHand.MAIN_HAND);
                return flag;
            }
        }
    }
     */

    @Unique
    List<Entity> alreadyHit = new ArrayList<>();
    @Inject(
            method = "startAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/HitResult;getType()Lnet/minecraft/world/phys/HitResult$Type;", shift = At.Shift.BEFORE)
    )
    private void hitDetectedEntities(CallbackInfoReturnable<Boolean> cir) {
        for (HitResult hitResult : ClientHandler.hitResults) {
            if (hitResult.getType() == ENTITY) {
                if (!alreadyHit.contains(((EntityHitResult) hitResult).getEntity())) {
                    thisMinecraft().gameMode.attack(thisMinecraft().player, ((EntityHitResult) hitResult).getEntity());
                    alreadyHit.add(((EntityHitResult) hitResult).getEntity());
                }
            }
        }
    }

    @Redirect(
            method = "startAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V")
    )
    private void hitDetectedEntities1(MultiPlayerGameMode instance, Player pPlayer, Entity pTargetEntity) {
        if (!alreadyHit.contains(((EntityHitResult) thisMinecraft().hitResult).getEntity()))
            thisMinecraft().gameMode.attack(thisMinecraft().player, ((EntityHitResult) thisMinecraft().hitResult).getEntity());
    }

    @Inject(
            method = "startAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/event/InputEvent$InteractionKeyMappingTriggered;shouldSwingHand()Z", shift = At.Shift.BEFORE,
            remap = false)
    )
    private void hitDetectedEntities2(CallbackInfoReturnable<Boolean> cir) {
        if (!alreadyHit.isEmpty()) {
            if (thisMinecraft().gameMode.hasMissTime()) {
                missTime = 10;
            }
            thisMinecraft().player.resetAttackStrengthTicker();
            net.minecraftforge.common.ForgeHooks.onEmptyLeftClick(thisMinecraft().player);
            alreadyHit.clear();
        }
    }

}
