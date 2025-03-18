package com.cwjn.skada.client;

import com.cwjn.skada.client.gui.button.OpenStatScreenButton;
import com.cwjn.skada.client.gui.particles.NumberParticle;
import com.cwjn.skada.client.gui.screen.StatScreen;
import com.cwjn.skada.client.gui.tooltip.ArmourTooltipComponent;
import com.cwjn.skada.client.gui.tooltip.ClientArmourTooltipComponent;
import com.cwjn.skada.client.gui.tooltip.ClientWeaponTooltipComponent;
import com.cwjn.skada.client.gui.tooltip.WeaponTooltipComponent;
import com.cwjn.skada.client.hud.MobHealthBar;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.network.SkadaNetwork;
import com.cwjn.skada.network.client_to_server.C2SUpdateAttackIndex;
import com.cwjn.skada.network.client_to_server.C2SUpdateAttackIndexFromMenu;
import com.cwjn.skada.util.Keybinds;
import com.cwjn.skada.util.ReticleShapes;
import com.cwjn.skada.util.Util;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import oshi.util.tuples.Pair;

import static com.cwjn.skada.data.SkadaData.*;

@OnlyIn(Dist.CLIENT)
public class ClientEvent {

    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvent {

        @SubscribeEvent
        public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent e) {
            e.register(WeaponTooltipComponent.class, t -> new ClientWeaponTooltipComponent(t.item));
            e.register(ArmourTooltipComponent.class, t -> new ClientArmourTooltipComponent(t.item));
        }

        @SubscribeEvent
        public static void registerParticleFactories(final RegisterParticleProvidersEvent e) {
            e.registerSpriteSet(Particles.NUMBER_PARTICLE.get(), NumberParticle.Provider::new);
        }

    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvent {

        @SubscribeEvent
        public static void onTooltipRender(RenderTooltipEvent.GatherComponents e) {
            //Only add to items that we consider weapons or armour
            if (e.getItemStack().hasTag() && e.getItemStack().getTag().contains(WEAPON_INFO_TAG_KEY)) {
                e.getTooltipElements().add(1, Either.right(new WeaponTooltipComponent(e.getItemStack())));
            }
        }

        @SubscribeEvent
        public static void onTooltipEvent(ItemTooltipEvent e) {
            //if the item has modifiers but isn't a weapon, use the Vanilla tooltip method in Util to add default modifier tooltip lines
            if (e.getItemStack().hasTag() && shouldShowInTooltip(getHideFlags(e.getItemStack()), ItemStack.TooltipPart.MODIFIERS) && !e.getItemStack().getTag().contains(WEAPON_INFO_TAG_KEY)) {
                e.getToolTip().addAll(Util.getVanillaTooltip(Minecraft.getInstance().player, e.getItemStack()));
            }
        }

        private static int getHideFlags(ItemStack i) {
            return i.hasTag() && i.getTag().contains("HideFlags", 99) ? i.getTag().getInt("HideFlags") : i.getItem().getDefaultTooltipHideFlags(i);
        }

        private static boolean shouldShowInTooltip(int pHideFlags, ItemStack.TooltipPart pPart) {
            return (pHideFlags & pPart.getMask()) == 0;
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
                            SkadaNetwork.playerToServer(new C2SUpdateAttackIndexFromMenu(0, screen.getMenu().containerId, screen.getSlotUnderMouse().index));
                        } else {
                            i.getTag().putInt(CURRENT_ATTACK_TYPE_TAG_KEY, currentAttackType + 1);
                            SkadaNetwork.playerToServer(new C2SUpdateAttackIndexFromMenu(currentAttackType + 1, screen.getMenu().containerId, screen.getSlotUnderMouse().index));
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
                            SkadaNetwork.playerToServer(new C2SUpdateAttackIndex(0));
                        } else {
                            i.getTag().putInt(CURRENT_ATTACK_TYPE_TAG_KEY, currentAttackType + 1);
                            SkadaNetwork.playerToServer(new C2SUpdateAttackIndex(currentAttackType + 1));
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
                Player p = Minecraft.getInstance().player;
                e.addListener(new OpenStatScreenButton(x, y, Component.empty(), b -> {
                    Minecraft.getInstance().setScreen(new StatScreen(
                            Util.getWeaponInfo(p),
                            Util.getAttackType(p),
                            Util.getAttackTypeInfo(p)
                    ));
                }));
            }
        }

        //@SubscribeEvent
        public static void onRenderGuii(RenderGuiOverlayEvent event) {
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            PoseStack stack = event.getGuiGraphics().pose();
            stack.pushPose();
            float middleX = event.getWindow().getGuiScaledWidth()*0.5F;
            float middleY = event.getWindow().getGuiScaledHeight()*0.5F;
            stack.translate(middleX-0.5f, middleY, 0);
            buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            for (Pair<Float, Float> pair : ReticleShapes.CirclePerfectCrosshair) {
                buffer.vertex(stack.last().pose(), pair.getA(), pair.getB(), 0).color(0.4f, 0.4f, 0.4f, 1f).endVertex();
            }
            tesselator.end();
            /*Entity e = Minecraft.getInstance().getCameraEntity();
            Vec3 rayOrigin = Util.get3DCoordFrom2D(middleX+10f, middleY+10f, event.getPartialTick());
            Vec3 rayMovement = Util.getMovementVector(e.getEyePosition(event.getPartialTick()), rayOrigin);
            Vec3 rayEndpoint = rayOrigin.add(rayMovement.x * 10, rayMovement.y * 10, rayMovement.z * 10);
            AABB aabb = e.getBoundingBox().expandTowards(rayMovement.scale(10)).inflate(1.0D, 1.0D, 1.0D);
            EntityHitResult secondRayResult = ProjectileUtil.getEntityHitResult(e, rayOrigin, rayEndpoint, aabb, (p_234237_) -> {
                return !p_234237_.isSpectator() && p_234237_.isPickable();
            }, 100);
            if (secondRayResult != null) {
                //print what entity we hit
                System.out.println("Second Ray Hit: " + secondRayResult.getEntity().getDisplayName().getString());
            }*/
            stack.popPose();
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiOverlayEvent event) {
            ItemStack i = Minecraft.getInstance().player.getMainHandItem();
            WeaponInfo weaponInfo = i.hasTag()? i.getTagElement(WEAPON_INFO_TAG_KEY) != null?
                    WeaponInfo.fromCompoundTag(i.getTagElement(WEAPON_INFO_TAG_KEY)) : WeaponInfo.NO_WEAPON : WeaponInfo.NO_WEAPON;
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            PoseStack stack = event.getGuiGraphics().pose();
            stack.pushPose();
            float middleX = event.getWindow().getGuiScaledWidth() * 0.5F;
            float middleY = event.getWindow().getGuiScaledHeight() * 0.5F;
            stack.translate(middleX - 0.5f, middleY, 0);
            buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            AttackType[] types = weaponInfo.getAttackTypes().keySet().toArray(AttackType[]::new);
            AttackType attackType = weaponInfo == WeaponInfo.NO_WEAPON? AttackType.strike() : types[i.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)];
                    switch (attackType.name()) {
                        case "slash" -> {
                            for (Pair<Float, Float> pair : ReticleShapes.getDrawable(ReticleShapes.SlashDefault)) {
                                buffer.vertex(stack.last().pose(), pair.getA(), pair.getB(), 0).color(0.4f, 0.4f, 0.4f, 1f).endVertex();
                            }
                        }
                        case "strike" -> {
                            for (Pair<Float, Float> pair : ReticleShapes.getDrawable(ReticleShapes.CircleRad15)) {
                                buffer.vertex(stack.last().pose(), pair.getA(), pair.getB(), 0).color(0.4f, 0.4f, 0.4f, 1f).endVertex();
                            }
                        }
                        case "thrust" -> {
                            for (Pair<Float, Float> pair : ReticleShapes.getDrawable(ReticleShapes.CirclePerfectCrosshair)) {
                                buffer.vertex(stack.last().pose(), pair.getA(), pair.getB(), 0).color(0.4f, 0.4f, 0.4f, 1f).endVertex();
                            }
                        }
                    }
//            Pair<Float, Float>[] filledShape = new Pair[0];
//            switch (attackType.name()) {
//                case "slash" -> filledShape = ReticleShapes.getFilledShape(ReticleShapes.SlashDefault, 5.0f);
//                case "strike" -> filledShape = ReticleShapes.getFilledShape(ReticleShapes.CircleRad15, 5.0f);
//                case "thrust" -> filledShape = ReticleShapes.getFilledShape(ReticleShapes.CirclePerfectCrosshair, 5.0f);
//            }
//            for (Pair<Float, Float> pair : filledShape) {
//                buffer.vertex(stack.last().pose(), pair.getA(), pair.getB(), 0).color(0.4f, 0.4f, 0.4f, 1f).endVertex();
//            }
            tesselator.end();
            stack.popPose();
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
                    //int unpackedLight = 15;
                    //float distance = entity.distanceTo(Minecraft.getInstance().player) - unpackedLight + 15;
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
