package com.cwjn.skada;

import com.cwjn.skada.damage.DamageClass;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import static com.cwjn.skada.Skada.MODID;

public class SkadaRegistry {

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, MODID);

    public static final RegistryObject<Attribute> FIRE_RESIST = resistAttribute("fire");
    public static final RegistryObject<Attribute> COLD_RESIST = resistAttribute("frost");
    public static final RegistryObject<Attribute> LIGHTNING_RESIST = resistAttribute("lightning");
    public static final RegistryObject<Attribute> WATER_RESIST = resistAttribute("water");
    public static final RegistryObject<Attribute> LIGHT_RESIST = resistAttribute("light");
    public static final RegistryObject<Attribute> DARK_RESIST = resistAttribute("dark");
    public static final RegistryObject<Attribute> WIND_RESIST = resistAttribute("wind");
    public static final RegistryObject<Attribute> EARTH_RESIST = resistAttribute("earth");

    public static final RegistryObject<Attribute> SLASH_RESIST = resistAttribute("slash");
    public static final RegistryObject<Attribute> PIERCE_RESIST = resistAttribute("pierce");
    public static final RegistryObject<Attribute> BLUNT_RESIST = resistAttribute("blunt");

    public static final RegistryObject<Attribute> FIRE_AFFINITY = affinityAttribute("fire");
    public static final RegistryObject<Attribute> COLD_AFFINITY = affinityAttribute("cold");
    public static final RegistryObject<Attribute> LIGHTNING_AFFINITY = affinityAttribute("lightning");
    public static final RegistryObject<Attribute> WATER_AFFINITY = affinityAttribute("water");
    public static final RegistryObject<Attribute> LIGHT_AFFINITY = affinityAttribute("light");
    public static final RegistryObject<Attribute> DARK_AFFINITY = affinityAttribute("dark");
    public static final RegistryObject<Attribute> WIND_AFFINITY = affinityAttribute("wind");
    public static final RegistryObject<Attribute> EARTH_AFFINITY = affinityAttribute("earth");

    public static final RegistryObject<Attribute> STRENGTH = statAttribute("strength");
    public static final RegistryObject<Attribute> DEXTERITY = statAttribute("dexterity");
    public static final RegistryObject<Attribute> VITALITY = statAttribute("vitality");
    public static final RegistryObject<Attribute> CONSTITUTION = statAttribute("constitution");
    public static final RegistryObject<Attribute> AGILITY = statAttribute("agility");

    public static final RegistryObject<Attribute> IMPACT = equipmentAttribute("impact");
    public static final RegistryObject<Attribute> GRIT = equipmentAttribute("grit");
    public static final RegistryObject<Attribute> FINESSE = equipmentAttribute("finesse");
    public static final RegistryObject<Attribute> MOBILITY = equipmentAttribute("mobility");
    public static final RegistryObject<Attribute> PRECISION = equipmentAttribute("precision");
    public static final RegistryObject<Attribute> RESILIENCE = equipmentAttribute("resilience");

    private static RegistryObject<Attribute> resistAttribute(String name) {
        return ATTRIBUTES.register(name + "_resist",
                () -> new RangedAttribute("attribute.skada." + name + "_resist", 1.0D, 0.0D, 10.0D).setSyncable(true));
    }

    private static RegistryObject<Attribute> affinityAttribute(String name) {
        return ATTRIBUTES.register(name + "_affinity",
                () -> new RangedAttribute("attribute.skada" + name + "_affinity", 1.0D, 0.0D, 10.D).setSyncable(true));
    }

    private static RegistryObject<Attribute> statAttribute(String name) {
        return ATTRIBUTES.register("stat_" + name,
                () -> new RangedAttribute("attribute.skada." + name, 0.0D, 0.0D, 99.0D).setSyncable(true));
    }

    private static RegistryObject<Attribute> equipmentAttribute(String name) {
            return ATTRIBUTES.register("equipment_" + name,
                    () -> new RangedAttribute("attribute.skada." + name, 0.0D, 0.0D, 8192.0D).setSyncable(true));
    }

}
