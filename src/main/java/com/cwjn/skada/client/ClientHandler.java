package com.cwjn.skada.client;

import com.cwjn.skada.ClientConfig;
import com.cwjn.skada.client.ClientBestiaryCache;
import com.cwjn.skada.client.hud.ReticleShape;
import com.cwjn.skada.data.armour.AccessArmourInfo;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.damage.AccessWeaponInfo;
import com.cwjn.skada.data.mob.BestiaryEntryView;
import com.cwjn.skada.util.UtilCombat.CriticalFailSeverity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.cwjn.skada.Skada.LOGGER;
import static com.cwjn.skada.data.SkadaData.RETICLES;

@OnlyIn(Dist.CLIENT)
public class ClientHandler {

    public static HitResult[] hitResults;
    public static int skyDarken;
    //storing as NaN is fine because we check isFinite later
    public static double playerBodyWeightKg = Double.NaN;
    public static double playerTotalWeightKg = Double.NaN;

    public static void updateWeaponInfos(Map<String, Map<String, WeaponInfo>> map) {
        LOGGER.info("Syncing {} weapon info maps from server.", map.size());
        for (Map.Entry<String,Map<String, WeaponInfo>> submap : map.entrySet()) {
            submap.getValue().forEach((key, value) -> {
                String modId = submap.getKey();
                ResourceLocation iRL = new ResourceLocation(modId, key);
                Item iItem = ForgeRegistries.ITEMS.getValue(iRL);
                if (iItem != null) {
                    AccessWeaponInfo mItem = (AccessWeaponInfo) iItem;
                    if (value.getAttackTypes().isEmpty()) {
                        LOGGER.error("Weapon info for {} has no attack types, skipping", iRL);
                    } else {
                        mItem.skada$setWeaponInfo(value);
                    }
                }
            });
        }
    }

    public static void updateArmourInfos(Map<String, Map<String, ArmourInfo>> map) {
        LOGGER.info("Syncing {} armour info maps from server.", map.size());
        for (Map.Entry<String,Map<String, ArmourInfo>> submap : map.entrySet()) {
            submap.getValue().forEach((key, value) -> {
                String modId = submap.getKey();
                ResourceLocation iRL = new ResourceLocation(modId, key);
                Item iItem = ForgeRegistries.ITEMS.getValue(iRL);
                if (iItem != null) {
                    AccessArmourInfo mItem = (AccessArmourInfo) iItem;
                    mItem.skada$setArmourInfo(value);
                }
            });
        }
    }

    public static void createDamageIndicator(double x, double y, double z, float f, int col, double horizontalOffset, UUID id) {
        if (!ClientConfig.ENABLE_DAMAGE_INDICATORS.get()) return;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (id.equals(player.getUUID())) return;
        Level world = Minecraft.getInstance().level;
        Vector3f lookVector = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
        float newZ = -1*lookVector.x();
        float newX = lookVector.z();
        if (world != null) {
            world.addParticle(Particles.NUMBER_PARTICLE.get().setNumber(f).setColour(col), x, y, z, (newX*horizontalOffset)*0.25, 0.35+(player.getRandom().nextDouble()*0.1)+((1-Math.abs(horizontalOffset))*0.1), (newZ*horizontalOffset)*0.25);
        }
    }

    /**
     * Plays the client-local sound cue for a server-reported critical fail.
     *
     * @param severity the critical fail severity that determines the sound variant
     */
    public static void playCriticalFailSound(CriticalFailSeverity severity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        switch (severity) {
            case EDGE_DAMAGE -> minecraft.getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.GRINDSTONE_USE, 0.9F, 0.7F));
            case DEFORMATION -> minecraft.getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_HIT, 0.9F, 0.8F));
            case CATASTROPHIC -> minecraft.getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.ITEM_BREAK, 0.9F, 0.9F));
            case NONE -> {
            }
        }
    }

    public static void updateReticles(List<ReticleShape> reticles) {
        LOGGER.info("Received {} reticle shapes from server.", reticles.size());
        for (ReticleShape r : reticles) {
            RETICLES.put(r.getName(), r);
        }
    }

    public static void updateBestiaryEntries(List<BestiaryEntryView> entries) {
        LOGGER.info("Received {} bestiary entries from server.", entries.size());
        ClientBestiaryCache.replaceEntries(entries);
    }

    /** Stores the latest server-authoritative weight for the local player journal. */
    public static void updatePlayerWeights(double bodyWeightKg, double totalWeightKg) {
        if (Double.isFinite(bodyWeightKg) && bodyWeightKg >= 0.0) playerBodyWeightKg = bodyWeightKg;
        if (Double.isFinite(totalWeightKg) && totalWeightKg >= 0.0) playerTotalWeightKg = totalWeightKg;
    }
    public static void updateBestiaryUnlocks(Collection<ResourceLocation> unlockedEntryIds,
                                             Collection<ResourceLocation> newlyUnlockedEntryIds) {
        LOGGER.info("Received bestiary unlock state from server: {} unlocked, {} newly unlocked.",
                unlockedEntryIds.size(), newlyUnlockedEntryIds.size());
        ClientBestiaryCache.replaceUnlockState(unlockedEntryIds, newlyUnlockedEntryIds);
    }

}
