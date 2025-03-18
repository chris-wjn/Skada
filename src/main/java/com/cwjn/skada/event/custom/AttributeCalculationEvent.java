package com.cwjn.skada.event.custom;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.entity.living.LivingEvent;

public class AttributeCalculationEvent extends LivingEvent {

    private final Multimap<Attribute, AttributeModifier> modifiers;

    public AttributeCalculationEvent(LivingEntity entity, Multimap<Attribute, AttributeModifier> modifiers) {
        super(entity);
        this.modifiers = HashMultimap.create(modifiers);
    }

    public Multimap<Attribute, AttributeModifier> getModifiers() {
        return modifiers;
    }

    //add modifier
    public void addModifier(Attribute attribute, AttributeModifier modifier) {
        modifiers.put(attribute, modifier);
    }

    //remove modifier
    public void removeModifier(Attribute attribute, AttributeModifier modifier) {
        modifiers.remove(attribute, modifier);
    }

    //set modifiers
    public void setModifiers(Multimap<Attribute, AttributeModifier> modifiers) {
        this.modifiers.clear();
        this.modifiers.putAll(modifiers);
    }

}
