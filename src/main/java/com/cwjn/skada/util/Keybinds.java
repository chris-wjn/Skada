package com.cwjn.skada.util;

import com.cwjn.skada.Skada;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.event.KeyEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Keybinds {

    public static KeyMapping cycleAttackType;
    public static KeyMapping attackScrollModifier;
    public static KeyMapping statScreen;

    @SubscribeEvent
    public static void registerKeyBinding(final RegisterKeyMappingsEvent event) {
        cycleAttackType = create("cycle_attack_type", GLFW.GLFW_KEY_G);
        attackScrollModifier = create("attack_scroll_modifier", GLFW.GLFW_KEY_LEFT_ALT);
        statScreen = create("stat_screen", KeyEvent.VK_O);
        event.register(cycleAttackType);
        event.register(attackScrollModifier);
        event.register(statScreen);
    }

    /** Queries physical input because container screens do not update the held state of key mappings. */
    public static boolean isAttackScrollModifierDown() {
        if (attackScrollModifier == null || attackScrollModifier.isUnbound()
                || !attackScrollModifier.isConflictContextAndModifierActive()) return false;
        InputConstants.Key key = attackScrollModifier.getKey();
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(window, key.getValue());
        }
        return attackScrollModifier.isDown();
    }

    private static KeyMapping create(String name, int key) {
        return new KeyMapping("tagKey." + Skada.MODID + "." + name, key, "tagKey.category." + Skada.MODID);
    }

}
