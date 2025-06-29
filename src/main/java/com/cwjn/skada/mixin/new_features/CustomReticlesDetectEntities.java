package com.cwjn.skada.mixin.new_features;

import com.cwjn.skada.client.ClientHandler;
import com.cwjn.skada.client.hud.ReticleCoordinate;
import com.cwjn.skada.client.hud.ReticleShape;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.ReticleShapes;
import com.cwjn.skada.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import oshi.util.tuples.Pair;

import java.util.Collection;
import java.util.Map;

import static com.cwjn.skada.data.SkadaData.RETICLES;

@Mixin(GameRenderer.class)
public class CustomReticlesDetectEntities {


    /*
        Logical RayTrace side of custom reticles. Does not handle drawing things to the screen, only picking entities inside the reticles.
     */
    @Inject(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", shift = At.Shift.AFTER))
    private void pickEntity(float pPartialTicks, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getProfiler().push("skada reticle extension");
        Player player = minecraft.player;
        Entity entity = minecraft.getCameraEntity();
        AttackType attackType = Util.getAttackType(player);
        ReticleShape shape = RETICLES.get(attackType.name() + "_default");
        float xOffset = minecraft.getWindow().getGuiScaledWidth() * 0.5F;
        float yOffset = minecraft.getWindow().getGuiScaledHeight() * 0.5F;

        /*
            We use the amount of rays we're going to shoot to determine the size of the hitResults array.
         */
        ClientHandler.hitResults = new HitResult[shape.getFilledShape().size()];

        /*
            Start at index 0, and iterate through the shape's coordinates.
            For each coordinate, we calculate the 3D position of the ray trace, and then call doRayTrace to perform the actual ray tracing
            and store the result in ClientHandler.hitResults. Then we increment the index.
            The index is used to store the hit result in the hitResults array. The index should never
            exceed the size of the hitResults array, which is equal to the number of coordinates in the shape.
         */
        int i = 0;
        for (Map.Entry<Float, Collection<Float>> map : shape.getFilledShape().asMap().entrySet()) {
            for (Float coord : map.getValue()) {
                doRayTrace(i, pPartialTicks, minecraft, entity, Util.get3DCoordFrom2D(xOffset + map.getKey(), yOffset + coord, pPartialTicks));
                i++;
            }
        }
        minecraft.getProfiler().pop();
    }

    /*
        * This method is used to ray trace from the player's eye position in the direction of @Vec3 direction.
     */
    private static void doRayTrace(int index, float pPartialTicks, Minecraft minecraft, Entity entity, Vec3 nearPlanePoint) {
        double maxRange = minecraft.player.getEntityReach(); //get player's entity reach distance (range)
        Vec3 eyePosition = entity.getEyePosition(pPartialTicks); //get the player's eye position
        Vec3 directionVector = Util.getMovementVector(eyePosition, nearPlanePoint).normalize(); //get the direction vector from eye position to the near plane point
        Vec3 vectorEndpoint = eyePosition.add(directionVector.scale(maxRange)); //get the vector endpoint
        ClientHandler.hitResults[index] = adjustedPick(entity, eyePosition, vectorEndpoint); //store initial pick result, which is a block HitResult. This is used to check if there might be a block in the way
        double maxDistance = maxRange*maxRange; //square the max range and store as distance, because we'd like operate in squared distances
        if (ClientHandler.hitResults[index] != null && ClientHandler.hitResults[index].getType() != HitResult.Type.MISS) {
            maxDistance = ClientHandler.hitResults[index].getLocation().distanceToSqr(eyePosition); //if there is a block in the way, set the max distance to the distance to the block
        }
        AABB aabb = entity.getBoundingBox().expandTowards(directionVector.scale(maxRange)).inflate(1.0D, 1.0D, 1.0D); //get the bounding box of the entity
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(entity, eyePosition, vectorEndpoint, aabb, (p_234237_) -> { //ray trace for entities and store as entity hit result
            return !p_234237_.isSpectator() && p_234237_.isPickable();
        }, maxDistance);
        if (entityhitresult != null) { //if we found an entity,
            Vec3 entityPosition = entityhitresult.getLocation(); //get the entity's position
            double distanceToEntity = eyePosition.distanceToSqr(entityPosition); //get the distance to the entity
            if (distanceToEntity > maxDistance || distanceToEntity > maxRange * maxRange) { //if the distance to the entity is greater than the max distance or the max range squared,
                ClientHandler.hitResults[index] = BlockHitResult.miss(entityPosition, Direction.getNearest(directionVector.x, directionVector.y, directionVector.z), BlockPos.containing(entityPosition)); //set the hit result to a miss
            } else if (distanceToEntity < maxDistance || ClientHandler.hitResults[index] == null) { //if the distance to the entity is less than the max distance, or we didn't find a block earlier,
                ClientHandler.hitResults[index] = entityhitresult; //set the hit result to the entity hit result
            }
        }
    }

    @SuppressWarnings("all")
    private static HitResult adjustedPick(Entity e, Vec3 start, Vec3 end) {
        return e.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, e));
    }

}
