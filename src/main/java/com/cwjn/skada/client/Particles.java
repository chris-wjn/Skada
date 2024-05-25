package com.cwjn.skada.client;

import com.cwjn.skada.Skada;
import com.cwjn.skada.client.gui.particles.NumberParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Particles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Skada.MODID);

    public static final RegistryObject<NumberParticleType> NUMBER_PARTICLE = PARTICLES.register("number", () -> new NumberParticleType(true));
    public static final RegistryObject<SimpleParticleType> MISS_PARTICLE = PARTICLES.register("miss", () -> new SimpleParticleType(true));

}
