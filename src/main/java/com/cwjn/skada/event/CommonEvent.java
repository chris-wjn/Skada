package com.cwjn.skada.event;

import com.cwjn.skada.Skada;
import com.cwjn.skada.damage.DamageClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

import static com.cwjn.skada.SkadaRegistry.*;

public class CommonEvent {

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvent {

        @SubscribeEvent
        public static void addSkadaAttributesToLivingEntities(EntityAttributeModificationEvent e) {
            e.getTypes().forEach(entityType -> {
                e.add(entityType, FIRE_AFFINITY.get());
                e.add(entityType, COLD_AFFINITY.get());
                e.add(entityType, LIGHTNING_AFFINITY.get());
                e.add(entityType, WATER_AFFINITY.get());
                e.add(entityType, LIGHT_AFFINITY.get());
                e.add(entityType, DARK_AFFINITY.get());
                e.add(entityType, WIND_AFFINITY.get());
                e.add(entityType, EARTH_AFFINITY.get());
                e.add(entityType, FIRE_RESIST.get());
                e.add(entityType, COLD_RESIST.get());
                e.add(entityType, LIGHTNING_RESIST.get());
                e.add(entityType, WATER_RESIST.get());
                e.add(entityType, LIGHT_RESIST.get());
                e.add(entityType, DARK_RESIST.get());
                e.add(entityType, WIND_RESIST.get());
                e.add(entityType, EARTH_RESIST.get());
                e.add(entityType, STRENGTH.get());
                e.add(entityType, DEXTERITY.get());
                e.add(entityType, VITALITY.get());
                e.add(entityType, CONSTITUTION.get());
                e.add(entityType, AGILITY.get());
                e.add(entityType, IMPACT.get());
                e.add(entityType, GRIT.get());
                e.add(entityType, MOBILITY.get());
                e.add(entityType, PRECISION.get());
                e.add(entityType, RESILIENCE.get());
                e.add(entityType, FINESSE.get());
            });
        }

    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvent {

        public static void createRegisters(NewRegistryEvent event) {
            event.create(new RegistryBuilder<DamageClass>()
                    .setName(new ResourceLocation(Skada.MODID, "damage_class"))
                    .setDefaultKey(new ResourceLocation(Skada.MODID, "none"))
            );
        }

    }

}
