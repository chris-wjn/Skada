package com.cwjn.skada.client;

import com.cwjn.skada.data.armour.AccessArmourInfo;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.util.SkadaEntity;
import com.cwjn.skada.data.damage.AccessWeaponInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ClientHandler {

    public static HitResult[] hitResults;

    public static void updateWeaponInfos(Map<String, Map<String, WeaponInfo>> map) {
        for (Map.Entry<String,Map<String, WeaponInfo>> submap : map.entrySet()) {
            submap.getValue().forEach((key, value) -> {
                String modId = submap.getKey();
                ResourceLocation iRL = new ResourceLocation(modId, key);
                Item iItem = ForgeRegistries.ITEMS.getValue(iRL);
                if (iItem != null) {
                    AccessWeaponInfo mItem = (AccessWeaponInfo) iItem;
                    mItem.skada$setWeaponInfo(value);
                }
            });
        }
    }

    public static void updateArmourInfos(Map<String, Map<String, ArmourInfo>> map) {
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

    public static void updateClientWeaponInfo(CompoundTag info, int id) {
        if (Minecraft.getInstance().player.getCommandSenderWorld().getEntity(id) instanceof LivingEntity e) {
            ((SkadaEntity)e).setWeaponInfo(WeaponInfo.fromCompoundTag(info));
        }
    }

}