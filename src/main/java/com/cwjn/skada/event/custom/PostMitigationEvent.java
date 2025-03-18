package com.cwjn.skada.event.custom;

import com.cwjn.skada.data.registry.Element;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.util.Map;

public class PostMitigationEvent extends LivingEvent {

    private final Map<Element, Float> damage;

    public PostMitigationEvent(LivingEntity entity, Map<Element, Float> damage) {
        super(entity);
        this.damage = damage;
    }

    public Map<Element, Float> getDamage() {
        return damage;
    }

    public float getTotalDamage() {
        return (float) damage.values().stream().mapToDouble(Float::doubleValue).sum();
    }

}
