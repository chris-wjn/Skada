package com.cwjn.skada.mixin.new_features;

import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.ElementSpreadInstance;
import com.cwjn.skada.data.damage.ProjectileLaunch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.cwjn.skada.Skada.LOGGER;

@Mixin(Projectile.class)
public class ProjectileData implements AccessProjectileData {

    private static final String SKADA_DAMAGE_INFO_TAG = "SkadaDamageInfo";

    private Projectile thisProjectile() {
        return (Projectile) (Object) this;
    }

    @Unique
    private DamageInfo skada$damageInfo;

    @Unique
    private ProjectileLaunch skada$pendingLaunch;

    /** Returns the exact-source launch context until its first flight calculation completes. */
    public ProjectileLaunch getPendingLaunch() { return skada$pendingLaunch; }

    /** Sets transient launch context without changing an already persistent damage snapshot. */
    public void setPendingLaunch(ProjectileLaunch launch) { skada$pendingLaunch = launch; }

    @ModifyVariable(method = "shoot", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float skada$launchVelocity(float original) {
        return skada$pendingLaunch == null ? original : skada$pendingLaunch.velocity(original);
    }

    @ModifyVariable(method = "shoot", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float skada$launchInaccuracy(float original) {
        return skada$pendingLaunch == null ? original : skada$pendingLaunch.inaccuracy(original);
    }

    @Inject(method = "shoot", at = @At("RETURN"))
    private void skada$finishLaunch(double x, double y, double z, float velocity, float inaccuracy, CallbackInfo ci) {
        skada$pendingLaunch = null;
    }

    @Unique
    public DamageInfo getDamageInfo() {
        if (!hasDamageInfo()) {
            LOGGER.error("ProjectileData.getDamageInfo() called before setDamageInfo() was called for projectile: {}", thisProjectile().toString());
            return DamageInfo.environmental(new ElementSpreadInstance());
        }
        return skada$damageInfo;
    }

    @Unique
    public void setDamageInfo(DamageInfo skada$damageInfo) {
        this.skada$damageInfo = skada$damageInfo;
    }

    @Unique
    public boolean hasDamageInfo() {
        return skada$damageInfo != null;
    }

    /**
     * Saves the projectile's Skada combat snapshot with the entity.
     */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveSkadaDamageInfo(CompoundTag tag, CallbackInfo ci) {
        if (hasDamageInfo()) {
            tag.put(SKADA_DAMAGE_INFO_TAG, skada$damageInfo.toCompoundTag());
        }
    }

    /**
     * Restores the projectile's Skada combat snapshot after entity load.
     */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadSkadaDamageInfo(CompoundTag tag, CallbackInfo ci) {
        skada$pendingLaunch = null;
        if (tag.contains(SKADA_DAMAGE_INFO_TAG, Tag.TAG_COMPOUND)) {
            skada$damageInfo = DamageInfo.fromCompoundTag(tag.getCompound(SKADA_DAMAGE_INFO_TAG));
            return;
        }
        skada$damageInfo = null;
    }

}
