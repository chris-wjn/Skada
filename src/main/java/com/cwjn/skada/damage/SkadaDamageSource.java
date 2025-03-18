package com.cwjn.skada.damage;

import com.cwjn.skada.SkadaRegistry;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.ElementSpreadInstance;
import com.cwjn.skada.data.registry.Element;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

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

    public SkadaDamageSource(DamageSource source, DamageInfo damageInfo) {
        super(source.typeHolder(), source.getEntity(), source.getDirectEntity(), source.getSourcePosition());
        this.damageInfo = damageInfo;
    }

    public static SkadaDamageSource environmental(DamageSource source) {
        Map<Element, Double> ratios = new HashMap<>();
        for (RegistryObject<Element> element : SkadaRegistry.ELEMENTS.getEntries()) {
            if (source.is(element.get().getTagKey())) {
                ratios.put(element.get(), 1.0);
            }
        }
        ElementSpreadInstance spread = new ElementSpreadInstance(1.0, ratios);
        return new SkadaDamageSource(source.typeHolder(), source.getEntity(), source.getDirectEntity(), source.getSourcePosition(), DamageInfo.environmental(spread));
    }

}
