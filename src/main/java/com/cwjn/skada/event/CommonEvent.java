package com.cwjn.skada.event;

import com.cwjn.skada.SkadaCommand;
import com.cwjn.skada.client.hud.ReticleShape;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.mob.MobData;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.event.custom.PostMitigationEvent;
import com.cwjn.skada.network.SkadaNetwork;
import com.cwjn.skada.network.server_to_client.S2CCreateDamageIndicator;
import com.cwjn.skada.network.server_to_client.S2CSendArmourInfoMap;
import com.cwjn.skada.network.server_to_client.S2CSendWeaponInfoMap;
import com.cwjn.skada.util.Util;
import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.server.command.ConfigCommand;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
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
            Util.addWeaponArmourInfoTagIfNotExists(e.getItemStack());
        }

        /*
        This is where we add the attribute modifiers to items based on their weapon or armour info
         */
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void addWeaponInfoModifiers(ItemAttributeModifierEvent e) {
            ItemStack stack = e.getItemStack();
            if (!stack.hasTag()) return;
            if (LivingEntity.getEquipmentSlotForItem(stack) != e.getSlotType()) return;
            if (stack.getTagElement(WEAPON_INFO_TAG_KEY) != null) {
                WeaponInfo info = WeaponInfo.fromCompoundTag(stack.getTagElement(WEAPON_INFO_TAG_KEY));
                if (info.ignoreAttributes()) return;
                AttackTypeInfo attackInfo =
                        info.getAttackTypes().get(
                                info.getAttackTypes().keySet().toArray(AttackType[]::new)[stack.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)]
                        );
                if (attackInfo.maxReach() - 3.0 != 0) e.addModifier(ForgeMod.ENTITY_REACH.get(),
                        new AttributeModifier(SKADA_ATTACK_TYPE_BASE_MOD_UUID, "attack_type_reach_mod", attackInfo.maxReach() - 3.0, AttributeModifier.Operation.ADDITION));
                if (attackInfo.attackSpeedMod() - 1 != 0) e.addModifier(Attributes.ATTACK_SPEED,
                        new AttributeModifier(SKADA_ATTACK_TYPE_BASE_MOD_UUID, "attack_type_speed_mod", attackInfo.attackSpeedMod() - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
                if (attackInfo.damageBonus() != 0) e.addModifier(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(SKADA_ATTACK_TYPE_BASE_MOD_UUID, "attack_type_damage_mod", attackInfo.damageBonus(), AttributeModifier.Operation.ADDITION));
            }
            if (stack.getTagElement(ARMOUR_INFO_TAG_KEY) != null) {
                ArmourInfo info = ArmourInfo.fromCompoundTag(stack.getTagElement(ARMOUR_INFO_TAG_KEY));
                for (Map.Entry<Element, Double> entry : info.elementalResists().entrySet()) {
                    e.addModifier(entry.getKey().resistAttribute(),
                            new AttributeModifier(SKADA_ARMOUR_BASE_MOD_UUID, entry.getKey() + "_resist_mod", entry.getValue(), AttributeModifier.Operation.ADDITION));
                }
                for (Map.Entry<AttackType, Double> entry : info.attackResists().entrySet()) {
                    e.addModifier(entry.getKey().resistAttribute(),
                            new AttributeModifier(SKADA_ARMOUR_BASE_MOD_UUID, entry.getKey() + "_resist_mod", entry.getValue(), AttributeModifier.Operation.ADDITION));
                }
                if (info.armourBonus() != 0) e.addModifier(Attributes.ARMOR,
                        new AttributeModifier(SKADA_ARMOUR_BASE_MOD_UUID, "armour_bonus_mod", info.armourBonus(), AttributeModifier.Operation.ADDITION));
                if (info.armourToughnessBonus() != 0) e.addModifier(Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(SKADA_ARMOUR_BASE_MOD_UUID, "armour_toughness_bonus_mod", info.armourToughnessBonus(), AttributeModifier.Operation.ADDITION));
            }
        }

        /*
        Add attribute modifiers to mobs when they join the world.
         */
        @SubscribeEvent
        public static void onMobJoinWorld(EntityJoinLevelEvent e)  {
            if (e.getEntity() instanceof Mob mob) {
                if (mob.getPersistentData().contains("skada_has_persistent_bonus")) return;
                MobData data = MOB_DATA.get(mob.getType());
                if (data == null) {
                    LOGGER.error("No mob data found for {}", mob.getType().getDescriptionId());
                }
                else {
                    for (Map.Entry<Attribute, AttributeModifier> entry : data.extraModifiers().entries()) {
                        if (mob.getAttribute(entry.getKey()) == null) {
                            continue;
                        }
                        mob.getAttribute(entry.getKey()).addPermanentModifier(
                                new AttributeModifier(
                                        "skada.mob_mod." + entry.getKey().getDescriptionId() + ".OP" + entry.getValue().getOperation().toValue(),
                                        entry.getValue().getAmount(),
                                        entry.getValue().getOperation()
                                )
                        );
                    }
                    mob.getPersistentData().putBoolean("skada_has_persistent_bonus", true);
                }
            }
        }

        /*
            * Here we listen to when a player joins the server or the reload command and update their config maps
         */
        @SubscribeEvent
        public static void onPlayerJoin(OnDatapackSyncEvent event) {
            ResourceManager manager = event.getPlayerList().getServer().getResourceManager();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            HashMultimap<String, Map<String, WeaponInfo>> weaponMapToSend = HashMultimap.create();
            HashMultimap<String, Map<String, ArmourInfo>> armourMapToSend = HashMultimap.create();
            manager.listResources("weapon_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
                try {
                    BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
                    JsonObject obj = gson.fromJson(reader, JsonObject.class);
                    DataResult<Map<String, WeaponInfo>> info = WeaponInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
                    String[] split = rl.getPath().split("/");
                    String modId = split[split.length-1].substring(0, split[split.length-1].length()-5);
                    info.result().ifPresent((map) -> {
                        if (map.size() > 40) {
                            LOGGER.info("Weapon info from {} has more than 40 entries, splitting into multiple maps", rl);
                            Map<String, WeaponInfo> subMap = new HashMap<>();
                            for (Map.Entry<String, WeaponInfo> entry : map.entrySet()) {
                                subMap.put(entry.getKey(), entry.getValue());
                                if (subMap.size() == 40) {
                                    weaponMapToSend.put(modId, new HashMap<>(subMap));
                                    subMap.clear();
                                }
                            }
                            if (!subMap.isEmpty()) {
                                weaponMapToSend.put(modId, subMap);
                            }
                        }
                        else {
                            weaponMapToSend.put(modId, map);
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Failed to read weapon info from " + rl, e);
                }
            });
            manager.listResources("armour_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
                try {
                    BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
                    JsonObject obj = gson.fromJson(reader, JsonObject.class);
                    DataResult<Map<String, ArmourInfo>> info = ArmourInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
                    String[] split = rl.getPath().split("/");
                    String modId = split[split.length-1].substring(0, split[split.length-1].length()-5);
                    info.result().ifPresent((map) -> {
                        if (map.size() > 40) {
                            LOGGER.info("Weapon info from {} has more than 40 entries, splitting into multiple maps", rl);
                            Map<String, ArmourInfo> subMap = new HashMap<>();
                            for (Map.Entry<String, ArmourInfo> entry : map.entrySet()) {
                                subMap.put(entry.getKey(), entry.getValue());
                                if (subMap.size() == 40) {
                                    armourMapToSend.put(modId, new HashMap<>(subMap));
                                    subMap.clear();
                                }
                            }
                            if (!subMap.isEmpty()) {
                                armourMapToSend.put(modId, subMap);
                            }
                        }
                        else {
                            armourMapToSend.put(modId, map);
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Failed to read armour info from " + rl, e);
                }
            });
            manager.listResources("reticles", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
                try {
                    BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
                    JsonObject obj = gson.fromJson(reader, JsonObject.class);
                    DataResult<ReticleShape> info = ReticleShape.CODEC.parse(JsonOps.INSTANCE, obj);
                    info.result().ifPresent((rs) -> {

                    });
                } catch (Exception e) {
                    LOGGER.error("Failed to read armour info from " + rl, e);
                }
            });
            // If the player isn't null, it means a player is joining the server, so we send the maps to that player.
            if (event.getPlayer() != null) {
                weaponMapToSend.forEach((key, value) ->
                        SkadaNetwork.serverToPlayer(new S2CSendWeaponInfoMap(Map.of(key, value)), event.getPlayer())
                );
                armourMapToSend.forEach((key, value) ->
                        SkadaNetwork.serverToPlayer(new S2CSendArmourInfoMap(Map.of(key, value)), event.getPlayer())
                );
            }
            // If the player is null, it means the reload command was run, so we send the maps to all players.
            else {
                weaponMapToSend.forEach((key, value) ->
                        SkadaNetwork.serverToAll(new S2CSendWeaponInfoMap(Map.of(key, value)))
                );
                armourMapToSend.forEach((key, value) ->
                        SkadaNetwork.serverToAll(new S2CSendArmourInfoMap(Map.of(key, value)))
                );
            }

        }

//        @SubscribeEvent
//        public static void onLivingEntityEquip(LivingEquipmentChangeEvent e) {
//            if (e.getSlot() != EquipmentSlot.MAINHAND && e.getSlot() != EquipmentSlot.OFFHAND) return;
//            if (e.getTo().hasTag() && e.getTo().getTag().contains(WEAPON_INFO_TAG_KEY)) {
//                WeaponInfo info = WeaponInfo.fromCompoundTag(e.getTo().getTagElement(WEAPON_INFO_TAG_KEY));
//                SkadaNetwork.serverToAll(new S2CUpdateWeaponInfo(e.getTo().getTagElement(WEAPON_INFO_TAG_KEY), e.getEntity().getId()));
//                ((SkadaEntity) e.getEntity()).setWeaponInfo(info);
//            }
//            else {
//                ((SkadaEntity) e.getEntity()).setWeaponInfo(WeaponInfo.NO_WEAPON);
//                SkadaNetwork.serverToAll(new S2CUpdateWeaponInfo(WeaponInfo.NO_WEAPON.toCompoundTag(), e.getEntity().getId()));
//            }
//        }

        @SubscribeEvent
        public static void onCommandsRegister(RegisterCommandsEvent e) {
            new SkadaCommand(e.getDispatcher(), e.getBuildContext());
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
                            Math.round(entry.getValue()),
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
