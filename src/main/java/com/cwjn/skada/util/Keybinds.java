package com.cwjn.skada.util;

import com.cwjn.skada.Skada;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.event.KeyEvent;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Keybinds {

    public static KeyMapping cycleAttackType;
    public static KeyMapping statScreen;

    @SubscribeEvent
    public static void registerKeyBinding(final RegisterKeyMappingsEvent event) {
        cycleAttackType = create("cycle_attack_type", KeyEvent.VK_K);
        statScreen = create("stat_screen", KeyEvent.VK_O);
        event.register(cycleAttackType);
        event.register(statScreen);
    }

    private static KeyMapping create(String name, int key) {
        return new KeyMapping("key." + Skada.MODID + "." + name, key, "key.category." + Skada.MODID);
    }

}
