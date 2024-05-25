package com.cwjn.skada.client;

import com.cwjn.skada.client.gui.button.OpenStatScreenButton;
import com.cwjn.skada.client.gui.particles.NumberParticle;
import com.cwjn.skada.client.gui.screen.StatScreen;
import com.cwjn.skada.client.gui.tooltip.ClientIconTooltipComponent;
import com.cwjn.skada.client.gui.tooltip.IconTooltipComponent;
import com.cwjn.skada.client.hud.MobHealthBar;
import com.cwjn.skada.network.SkadaNetwork;
import com.cwjn.skada.network.client_to_server.C2SCycleAttackType;
import com.cwjn.skada.network.client_to_server.C2SCycleAttackTypeFromMenu;
import com.cwjn.skada.util.Keybinds;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.cwjn.skada.data.SkadaData.*;

@OnlyIn(Dist.CLIENT)
public class ClientEvent {

    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvent {

        @SubscribeEvent
        public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent e) {
            e.register(IconTooltipComponent.class, t -> new ClientIconTooltipComponent(t.item));
        }

        @SubscribeEvent
        public static void registerParticleFactories(final RegisterParticleProvidersEvent e) {
            e.registerSpriteSet(Particles.NUMBER_PARTICLE.get(), NumberParticle.Provider::new);
        }

    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvent {

        @SubscribeEvent
        public static void onTooltipEvent(RenderTooltipEvent.GatherComponents e) {
            //Only add to items that we consider weapons
            if (e.getItemStack().hasTag() && e.getItemStack().getTag().contains(WEAPON_INFO_TAG_KEY)) {
                e.getTooltipElements().add(1, Either.right(new IconTooltipComponent(e.getItemStack())));
            }
        }

        @SubscribeEvent
        public static void listenForAttackTypeCycle(InputEvent.Key e) {
            if (Minecraft.getInstance().player == null) return;
            if (Minecraft.getInstance().player.getAttackStrengthScale(0.5f) <= 0.9) return;
            if (e.getKey() == Keybinds.cycleAttackType.getKey().getValue() && e.getAction() == GLFW.GLFW_PRESS) {
                if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getSlotUnderMouse() != null) {
                    if (screen instanceof CreativeModeInventoryScreen) return;
                    ItemStack i = screen.getSlotUnderMouse().getItem();
                    if (i.hasTag() && i.getTag().contains(CURRENT_ATTACK_TYPE_TAG_KEY)) {
                        int currentAttackType = i.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY);
                        int maxAttackTypes = i.getTag().getInt(NUM_ATTACK_TYPES_TAG_KEY);
                        //need to +1 because currentAttackType is an index and maxAttackTypes is a size
                        if (currentAttackType + 1 == maxAttackTypes) {
                            i.getTag().putInt(CURRENT_ATTACK_TYPE_TAG_KEY, 0);
                            SkadaNetwork.playerToServer(new C2SCycleAttackTypeFromMenu(0, screen.getMenu().containerId, screen.getSlotUnderMouse().index));
                        } else {
                            i.getTag().putInt(CURRENT_ATTACK_TYPE_TAG_KEY, currentAttackType + 1);
                            SkadaNetwork.playerToServer(new C2SCycleAttackTypeFromMenu(currentAttackType + 1, screen.getMenu().containerId, screen.getSlotUnderMouse().index));
                        }
                    }
                }
                else {
                    ItemStack i = Minecraft.getInstance().player.getMainHandItem();
                    if (i.hasTag() && i.getTag().contains(CURRENT_ATTACK_TYPE_TAG_KEY)) {
                        int currentAttackType = i.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY);
                        int maxAttackTypes = i.getTag().getInt(NUM_ATTACK_TYPES_TAG_KEY);
                        //need to +1 because currentAttackType is an index and maxAttackTypes is a size
                        if (currentAttackType + 1 == maxAttackTypes) {
                            i.getTag().putInt(CURRENT_ATTACK_TYPE_TAG_KEY, 0);
                            SkadaNetwork.playerToServer(new C2SCycleAttackType(0));
                        } else {
                            i.getTag().putInt(CURRENT_ATTACK_TYPE_TAG_KEY, currentAttackType + 1);
                            SkadaNetwork.playerToServer(new C2SCycleAttackType(currentAttackType + 1));
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onInitGui(ScreenEvent.Init e) {
            Screen s = e.getScreen();
            if (s instanceof InventoryScreen || s instanceof CreativeModeInventoryScreen) {
                int x = (s.width - (s instanceof CreativeModeInventoryScreen ? 195 : 176)) / 2 - 28;
                int y = (s.height - (s instanceof CreativeModeInventoryScreen ? 136 : 166)) / 2;
                e.addListener(new OpenStatScreenButton(x, y, Component.empty(), b -> {
                    Minecraft.getInstance().setScreen(new StatScreen());
                }));
            }
        }

        @SubscribeEvent
        public static void prepareHealthbar(RenderNameTagEvent event) {
            if (event.getEntity() instanceof Mob entity) {
                if (entity.isInvisible()) return;
                if (entity.isVehicle()) return;
                /*if (ClientConfig.BLACKLISTED_HEALTHBAR_ENTITIES.get().contains(Util.getEntityRegistryName(entity.getType()).toString()))
                    return;*/
                /*if (DISPLAY_HEALTHBAR_ONLY_ON_DAMAGE.get()) {
                    if (displayHealthbarTicks.containsKey(entity)) {
                        int unpackedLight = Math.max(LightTexture.sky(event.getPackedLight()) - ClientData.skyDarken, LightTexture.block(event.getPackedLight()));
                        float distance = entity.distanceTo(Minecraft.getInstance().player) - unpackedLight + 15;
                        float minAlpha = distance < MOB_HEALTH_BAR_DISTANCE_FACTOR * 0.5 ? 1 : (1 - (distance / MOB_HEALTH_BAR_DISTANCE_FACTOR));
                        float alpha = (float) Math.min(minAlpha, ((float) displayHealthbarTicks.get(entity) / (HEALTHBAR_ON_DAMAGE_DISPLAY_TIME.get() * 0.5)));
                        MobHealthbar.prepare(entity, alpha);
                    }
                }*/
                //else {
                    //int unpackedLight = Math.max(LightTexture.sky(event.getPackedLight()) - ClientData.skyDarken, LightTexture.block(event.getPackedLight()));
                int unpackedLight = 15;
                    float distance = entity.distanceTo(Minecraft.getInstance().player) - unpackedLight + 15;
                    //float alpha = distance < MOB_HEALTH_BAR_DISTANCE_FACTOR*0.5 ? 1 : (1 - (distance/MOB_HEALTH_BAR_DISTANCE_FACTOR));
                    MobHealthBar.prepare(entity, 1);
                //}
            }
        }

        @SubscribeEvent
        public static void renderHealthbar(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                Camera c = Minecraft.getInstance().gameRenderer.getMainCamera();
                MobHealthBar.renderBars(event.getPartialTick(), event.getPoseStack(), c);
            }
        }

    }

}
