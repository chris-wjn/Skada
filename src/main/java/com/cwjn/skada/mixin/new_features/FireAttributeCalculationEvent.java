package com.cwjn.skada.mixin.new_features;

import com.cwjn.skada.event.custom.AttributeCalculationEvent;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class FireAttributeCalculationEvent {

    @Redirect(method = "collectEquipmentChanges",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getAttributeModifiers(Lnet/minecraft/world/entity/EquipmentSlot;)Lcom/google/common/collect/Multimap;"))
    private Multimap<Attribute, AttributeModifier> onCollectEquipmentChanges(ItemStack instance, EquipmentSlot optional) {
        AttributeCalculationEvent evt = new AttributeCalculationEvent((LivingEntity) (Object) this, instance.getAttributeModifiers(optional));
        MinecraftForge.EVENT_BUS.post(evt);
        return evt.getModifiers();
    }

}
