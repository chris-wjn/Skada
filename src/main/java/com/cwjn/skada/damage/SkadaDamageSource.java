package com.cwjn.skada.damage;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SkadaDamageSource extends DamageSource {

    private final DamageInfo damageInfo;

    public DamageInfo getInfo() {
        return damageInfo;
    }

    public SkadaDamageSource(Holder<DamageType> pType, @Nullable Entity pDirectEntity, @Nullable Entity pCausingEntity, @Nullable Vec3 pDamageSourcePosition, DamageInfo damageInfo) {
        super(pType, pDirectEntity, pCausingEntity, pDamageSourcePosition);
        this.damageInfo = damageInfo;
    }

    public SkadaDamageSource(Holder<DamageType> pType, @Nullable Entity pDirectEntity, @Nullable Entity pCausingEntity, DamageInfo damageInfo) {
        super(pType, pDirectEntity, pCausingEntity);
        this.damageInfo = damageInfo;
    }

    public SkadaDamageSource(Holder<DamageType> pType, Vec3 pDamageSourcePosition, DamageInfo damageInfo) {
        super(pType, pDamageSourcePosition);
        this.damageInfo = damageInfo;
    }

    public SkadaDamageSource(Holder<DamageType> pType, @Nullable Entity pEntity, DamageInfo damageInfo) {
        super(pType, pEntity);
        this.damageInfo = damageInfo;
    }

    public SkadaDamageSource(Holder<DamageType> pType, DamageInfo damageInfo) {
        super(pType);
        this.damageInfo = damageInfo;
    }

    public static SkadaDamageSource convert(DamageSource source) {
        ElementSpread spread = new ElementSpread(
                source.is(SkadaDamageTags.CONVERT_FIRE) ? 1:0,
                source.is(SkadaDamageTags.CONVERT_COLD) ? 1:0,
                source.is(SkadaDamageTags.CONVERT_LIGHTNING) ? 1:0,
                source.is(SkadaDamageTags.CONVERT_WATER) ? 1:0,
                source.is(SkadaDamageTags.CONVERT_EARTH) ? 1:0,
                source.is(SkadaDamageTags.CONVERT_WIND) ? 1:0,
                source.is(SkadaDamageTags.CONVERT_DARK) ? 1:0,
                source.is(SkadaDamageTags.CONVERT_LIGHT) ? 1:0,
                1.0);
        DamageInfo newInfo = new DamageInfo(0, 0, 0, 0, 0, 0, false, true, spread);
        return new SkadaDamageSource(source.typeHolder(), source.getEntity(), source.getDirectEntity(), source.getSourcePosition(), newInfo);
    }

}
