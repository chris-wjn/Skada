package com.cwjn.skada.event;

import com.cwjn.skada.SkadaCommand;
import com.cwjn.skada.SkadaRegistry;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.data.registry.Parameter;
import com.cwjn.skada.event.custom.PostMitigationEvent;
import com.cwjn.skada.mixin.PlayerAttackUseSkada;
import com.cwjn.skada.network.SkadaNetwork;
import com.cwjn.skada.network.server_to_client.S2CCreateDamageIndicator;
import com.cwjn.skada.network.server_to_client.S2CSendWeaponInfoMap;
import com.cwjn.skada.network.server_to_client.S2CUpdateWeaponInfo;
import com.cwjn.skada.util.AccessPlayerWeaponInfo;
import com.cwjn.skada.util.Util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.server.command.ConfigCommand;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static com.cwjn.skada.Skada.LOGGER;
import static com.cwjn.skada.data.SkadaData.*;

public class CommonEvent {

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvent {

        @SubscribeEvent
        public static void addSkadaAttributesToLivingEntities(EntityAttributeModificationEvent e) {
            e.getTypes().forEach(entityType -> {
                for (AttackType dc : REGISTRY_ATTACK_TYPE.get().getValues()) {
                    e.add(entityType, dc.resistAttribute());
                }
                for (Element element : REGISTRY_ELEMENT.get().getValues()) {
                    e.add(entityType, element.affinityAttribute());
                    e.add(entityType, element.resistAttribute());
                    e.add(entityType, element.baseDamage());
                }
                for (Parameter parameter : REGISTRY_PARAMETER.get().getValues()) {
                    e.add(entityType, parameter.attribute());
                }
                e.add(entityType, SkadaRegistry.LETHALITY.get());
                e.add(entityType, SkadaRegistry.AIM.get());
                e.add(entityType, SkadaRegistry.EVASIVENESS.get());
            });
        }

        @SubscribeEvent
        public static void createRegisters(NewRegistryEvent e) {
            RegistryBuilder<AttackType> dcBuilder = new RegistryBuilder<>();
            dcBuilder.setName(Util.rl("damage_class"));
            REGISTRY_ATTACK_TYPE = e.create(dcBuilder);
            RegistryBuilder<Element> eBuilder = new RegistryBuilder<>();
            eBuilder.setName(Util.rl("element"));
            REGISTRY_ELEMENT = e.create(eBuilder);
            RegistryBuilder<Parameter> pBuilder = new RegistryBuilder<>();
            pBuilder.setName(Util.rl("parameter"));
            REGISTRY_PARAMETER = e.create(pBuilder);
        }

    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvent {

        /*
         * Here we listen to when the server wants to try and get an itemstack's attribute modifiers so
         * we can add a weapon info tag if it doesn't exist but should exist. We only handle the server side here
         * because the client will request a tag to be added when it needs to be added from mixin.RemoveItemStackModifier
         */
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void addWeaponInfo(ItemAttributeModifierEvent e) {
            if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
                Util.addWeaponInfoTagIfNotExists(e.getItemStack());
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void addWeaponInfoModifiers(ItemAttributeModifierEvent e) {
            if (e.getSlotType() == LivingEntity.getEquipmentSlotForItem(e.getItemStack()) && e.getItemStack().hasTag() && e.getItemStack().getTag().contains(WEAPON_INFO_TAG_KEY)) {
                WeaponInfo info = WeaponInfo.fromCompoundTag(e.getItemStack().getTagElement(WEAPON_INFO_TAG_KEY));
                if (info.getAttackTypes().isEmpty()) return;
                int attackIndex = e.getItemStack().getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY);
                AttackTypeInfo attackInfo = info.getAttackTypes().get(info.getAttackTypes().keySet().toArray(AttackType[]::new)[attackIndex]);
                e.addModifier(ForgeMod.ENTITY_REACH.get(),
                        new AttributeModifier(ATTACK_TYPE_BASE_MOD_UUID, "attack_type_reach_mod", attackInfo.maxReach()-3.0, AttributeModifier.Operation.ADDITION));
                e.addModifier(Attributes.ATTACK_SPEED,
                        new AttributeModifier(ATTACK_TYPE_BASE_MOD_UUID, "attack_type_speed_mod", attackInfo.attackSpeedMod()-1, AttributeModifier.Operation.MULTIPLY_TOTAL));
                e.addModifier(SkadaRegistry.AIM.get(),
                        new AttributeModifier(ATTACK_TYPE_BASE_MOD_UUID, "attack_type_aim_mod", attackInfo.aim(), AttributeModifier.Operation.ADDITION));
                e.addModifier(SkadaRegistry.LETHALITY.get(),
                        new AttributeModifier(ATTACK_TYPE_BASE_MOD_UUID, "attack_type_lethality_mod", attackInfo.lethality(), AttributeModifier.Operation.ADDITION));
                e.addModifier(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ATTACK_TYPE_BASE_MOD_UUID, "attack_type_damage_mod", attackInfo.damageBonus(), AttributeModifier.Operation.ADDITION));
            }
        }

        @SubscribeEvent
        public static void onPlayerJoin(OnDatapackSyncEvent event) {
            ResourceManager manager = event.getPlayer().server.getResourceManager();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            HashMap<String, Map<String, WeaponInfo>> mapToSend = new HashMap<>();
            manager.listResources("weapon_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
                try {
                    BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
                    JsonObject obj = gson.fromJson(reader, JsonObject.class);
                    DataResult<Map<String, WeaponInfo>> info = WeaponInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
                    String[] split = rl.getPath().split("/");
                    String modId = split[split.length-1].substring(0, split[split.length-1].length()-5);
                    info.result().ifPresent((map) -> mapToSend.put(modId, map));
                } catch (Exception e) {
                    LOGGER.error("Failed to read weapon info from " + rl, e);
                }
            });
            SkadaNetwork.serverToPlayer(new S2CSendWeaponInfoMap(mapToSend), event.getPlayer());
        }

        @SubscribeEvent
        public static void onPlayerEquipItem(LivingEquipmentChangeEvent e) {
            if (!(e.getEntity() instanceof Player)) return;
            if (e.getSlot() != EquipmentSlot.MAINHAND && e.getSlot() != EquipmentSlot.OFFHAND) return;
            if (e.getTo().hasTag() && e.getTo().getTag().contains(WEAPON_INFO_TAG_KEY)) {
                WeaponInfo info = WeaponInfo.fromCompoundTag(e.getTo().getTagElement(WEAPON_INFO_TAG_KEY));
                SkadaNetwork.serverToPlayer(new S2CUpdateWeaponInfo(e.getTo().getTagElement(WEAPON_INFO_TAG_KEY)), (ServerPlayer) e.getEntity());
                ((AccessPlayerWeaponInfo) e.getEntity()).setWeaponInfo(info);
                ((AccessPlayerWeaponInfo) e.getEntity()).setAttackTypeIndex(e.getTo().getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY));
            }
        }

        @SubscribeEvent
        public static void onCommandsRegister(RegisterCommandsEvent e) {
            new SkadaCommand(e.getDispatcher());
            ConfigCommand.register(e.getDispatcher());
        }

        @SubscribeEvent
        public static void onPostMitigationEvent(PostMitigationEvent e) {
            LivingEntity ent = e.getEntity();
            RandomSource rand = ent.getRandom();
            Level level = ent.level();
            if (level.isClientSide()) return;
            UUID id = net.minecraft.Util.NIL_UUID;
            if (ent instanceof Player) {
                id = ent.getUUID();
            }
            float step = 0.2f;
            float min = 0;
            float max = 0.2f;
            for (Map.Entry<Element, Float> entry : e.getDamage().entrySet()) {
                SkadaNetwork.serverToNearPoint(new S2CCreateDamageIndicator(
                            ent.getX(),
                            ent.getEyeY(),
                            ent.getZ(),
                            entry.getValue(),
                            min + rand.nextFloat()*(max-min),
                            entry.getKey().colour(),
                            id),
                        ent.getX(),
                        ent.getY(),
                        ent.getZ(),
                        15,
                        ent.getCommandSenderWorld().dimension()
                );
                min = (min+step)*-1;
                max = (max+step)*-1;
                step*=-1;
            }
        }

    }

}
