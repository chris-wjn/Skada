package com.cwjn.skada.util;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.armour.AccessArmourInfo;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.damage.AccessWeaponInfo;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.mob.MobData;
import com.cwjn.skada.data.registry.AttackType;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.ForgeRegistries;
import oshi.util.tuples.Pair;

import java.io.BufferedReader;
import java.lang.Math;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.cwjn.skada.Skada.LOGGER;
import static com.cwjn.skada.data.SkadaData.*;
import static net.minecraft.world.item.ItemStack.ATTRIBUTE_MODIFIER_FORMAT;

public abstract class Util {

    private static final Style SPACER = Style.EMPTY.withFont(rl("space"));
    private static final Style PIXEL = Style.EMPTY.withFont(rl("minimal_pixel_bitmap"));
    private static final Style PIXEL_LARGE = Style.EMPTY.withFont(rl("minimal_pixel_16x"));

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(Skada.MODID, path);
    }
    public static String getItemNamespace(Item item) {
        return ForgeRegistries.ITEMS.getKey(item).getNamespace();
    }
    public static String getItemPath(Item item) {
        return ForgeRegistries.ITEMS.getKey(item).getPath();
    }
    public static String getEntityNamespace(EntityType<?> entity) {
        return ForgeRegistries.ENTITY_TYPES.getKey(entity).getNamespace();
    }
    public static String getEntityPath(EntityType<?> entity) {
        return ForgeRegistries.ENTITY_TYPES.getKey(entity).getPath();
    }
    public static Attribute attribute(SkadaAttributeHolder holder) {
        return holder.getAttribute();
    }
    public static MutableComponent spacer(int i) {
        return Component.translatable("space." + i).withStyle(SPACER);
    }

    public static final Codec<AttributeModifier> ATTRIBUTE_MODIFIER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("amount").forGetter(AttributeModifier::getAmount),
            Codec.INT.fieldOf("operation").forGetter(Util::getOperation)
    ).apply(instance, Util::attrMod));


    private static int getOperation(AttributeModifier mod) {
        return mod.getOperation().toValue();
    }
    private static AttributeModifier attrMod(double amount, int operation) {
        //The first parameter of AttributeModifier is supposed to be a "name," but since this will be
        //used to generate attributemodifiers dynamically, there's no need for a name, we just need
        //to ensure they are unique.
        return new AttributeModifier(UUID.randomUUID().toString(), amount, AttributeModifier.Operation.fromValue(operation));
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static String roundToString(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.toString();
    }

    @OnlyIn(Dist.CLIENT)
    public static MutableComponent pixelFontComponent(String s) {
        return pixelFontComponent(s, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    public static MutableComponent pixelFontComponent(Component comp) {
        return pixelFontComponent(comp, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    public static MutableComponent pixelFontComponent(String s, boolean useBoldNumbers, boolean useLargeFont) {
        if (Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
         || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv")) {
            MutableComponent retComp = Component.empty().withStyle(useLargeFont? PIXEL_LARGE : PIXEL);
            if (useBoldNumbers) s = s.replace("0", "ᙐ").replace("1", "ᙑ").replace("2", "ᙒ").replace("3", "ᙓ").replace("4", "ᙔ").replace("5", "ᙕ").replace("6", "ᙖ").replace("7", "ᙗ").replace("8", "ᙘ").replace("9", "ᙙ").replace('.', '_').replace('(', '<').replace(')', '>');
            for (char c : s.toCharArray()) {
                retComp.append(String.valueOf(c));
                if (c == ' ') {
                    retComp.append(spacer(1));
                    continue;
                }
                retComp.append(spacer(-1));
            }
            return retComp;
        }
        else {
            return Component.literal(s);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static MutableComponent pixelFontComponent(Component comp, boolean useBoldNumbers, boolean useLargeFont) {
        if (Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
         || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv")) {
            MutableComponent retComp = Component.empty().withStyle(useLargeFont? PIXEL_LARGE : PIXEL);
            String s = I18n.exists(comp.getString())? I18n.get(comp.getString()) : comp.getString();
            if (useBoldNumbers) s = s.replace("0", "ᙐ").replace("1", "ᙑ").replace("2", "ᙒ").replace("3", "ᙓ").replace("4", "ᙔ").replace("5", "ᙕ").replace("6", "ᙖ").replace("7", "ᙗ").replace("8", "ᙘ").replace("9", "ᙙ").replace('.', '_').replace('(', '<').replace(')', '>');
            Style currentFormatting = Style.EMPTY;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '§' && i < s.length()-1) {
                    if (s.charAt(i+1) == 'r') {
                        currentFormatting = Style.EMPTY;
                    }
                    else {
                        ChatFormatting cf = ChatFormatting.getByCode(s.charAt(i+1));
                        if (cf != null) currentFormatting = currentFormatting.applyFormat(cf);
                    }
                    i++;
                    continue;
                }
                else {
                    retComp.append(Component.literal(String.valueOf(c)).withStyle(currentFormatting));
                    if (c == ' ') {
                        retComp.append(spacer(1));
                        continue;
                    }
                }
                retComp.append(spacer(-1));
            }
            return retComp;
        }
        else {
            return comp.copy();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static MutableComponent pixelFontComponent(Component... comp) {
        if (Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en")
         || Minecraft.getInstance().getLanguageManager().getSelected().startsWith("sv")) {
            MutableComponent retComp = Component.empty();
            for (Component c : comp) {
                Style style = c.getStyle().applyTo(PIXEL);
                String s = I18n.get(c.getString());
                for (int i = 0; i < s.length(); i++) {
                    char ch = s.charAt(i);
                    if (ch == '§') {
                        i++;
                        continue;
                    }
                    else {
                        retComp.append(Component.literal(String.valueOf(ch)).withStyle(style));
                        if (ch == ' ') {
                            retComp.append(spacer(1));
                            continue;
                        }
                    }
                    retComp.append(spacer(-1));
                }
            }
            return retComp;
        }
        else {
            MutableComponent retComp = Component.empty();
            for (Component c : comp) {
                retComp.append(c);
            }
            return retComp;
        }
    }

    public static List<Component> getVanillaTooltip(Player pPlayer, ItemStack stack) {
        List<Component> list = new ArrayList<>();
        for(EquipmentSlot equipmentslot : EquipmentSlot.values()) {
            Multimap<Attribute, AttributeModifier> multimap = stack.getAttributeModifiers(equipmentslot);
            if (!multimap.isEmpty()) {
                list.add(CommonComponents.EMPTY);
                list.add(Component.translatable("item.modifiers." + equipmentslot.getName()).withStyle(ChatFormatting.GRAY));

                for(Map.Entry<Attribute, AttributeModifier> entry : multimap.entries()) {
                    AttributeModifier attributemodifier = entry.getValue();
                    double d0 = attributemodifier.getAmount();
                    boolean flag = false;
                    if (pPlayer != null) {
                        if (attributemodifier.getId() == SkadaData.BASE_ATTACK_DAMAGE_UUID) {
                            d0 += pPlayer.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                            d0 += (double) EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);
                            flag = true;
                        } else if (attributemodifier.getId() == SkadaData.BASE_ATTACK_SPEED_UUID) {
                            d0 += pPlayer.getAttributeBaseValue(Attributes.ATTACK_SPEED);
                            flag = true;
                        }
                    }

                    double d1;
                    if (attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
                        if (entry.getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
                            d1 = d0 * 10.0D;
                        } else {
                            d1 = d0;
                        }
                    } else {
                        d1 = d0 * 100.0D;
                    }

                    if (flag) {
                        list.add(CommonComponents.space().append(Component.translatable("attribute.modifier.equals." + attributemodifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(entry.getKey().getDescriptionId()))).withStyle(ChatFormatting.DARK_GREEN));
                    } else if (d0 > 0.0D) {
                        list.add(Component.translatable("attribute.modifier.plus." + attributemodifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(entry.getKey().getDescriptionId())).withStyle(ChatFormatting.BLUE));
                    } else if (d0 < 0.0D) {
                        d1 *= -1.0D;
                        list.add(Component.translatable("attribute.modifier.take." + attributemodifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(entry.getKey().getDescriptionId())).withStyle(ChatFormatting.RED));
                    }
                }
            }
        }
        return list;
    }

    public static void addWeaponInfoTagIfNotExists(ItemStack i) {
        if (((AccessWeaponInfo)i.getItem()).skada$hasWeaponInfo()) {
            if (!i.getOrCreateTag().contains(WEAPON_INFO_TAG_KEY)) {
                i.getOrCreateTag().put(WEAPON_INFO_TAG_KEY,
                        ((AccessWeaponInfo)i.getItem()).skada$getWeaponInfo().toCompoundTag());
                i.getOrCreateTag().putInt(CURRENT_ATTACK_TYPE_TAG_KEY, 0);
                i.getOrCreateTag().putInt(NUM_ATTACK_TYPES_TAG_KEY, ((AccessWeaponInfo)i.getItem()).skada$getWeaponInfo().getAttackTypes().size());
            }
        }
        if (((AccessArmourInfo)i.getItem()).skada$hasArmourInfo()) {
            if (!i.getOrCreateTag().contains(ARMOUR_INFO_TAG_KEY)) {
                i.getOrCreateTag().put(ARMOUR_INFO_TAG_KEY,
                        ((AccessArmourInfo)i.getItem()).skada$getArmourInfo().toCompoundTag());
            }
        }
    }

    public static WeaponInfo getWeaponInfo(Player p) {
        ItemStack i = p.getMainHandItem();
        if (i.hasTag() && i.getTag().contains(WEAPON_INFO_TAG_KEY)) {
            return WeaponInfo.fromCompoundTag(i.getTag().getCompound(WEAPON_INFO_TAG_KEY));
        }
        else {
            return WeaponInfo.NO_WEAPON;
        }
    }

    public static AttackType[] getAttackTypes(Player p) {
        WeaponInfo info = getWeaponInfo(p);
        return info.getAttackTypes().keySet().toArray(AttackType[]::new);
    }

    public static AttackType getAttackType(Player p) {
        if (p.getMainHandItem().hasTag() && p.getMainHandItem().getTag().contains(CURRENT_ATTACK_TYPE_TAG_KEY)) {
            return getAttackTypes(p)[p.getMainHandItem().getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)];
        }
        else {
            return AttackType.strike();
        }
    }

    public static AttackTypeInfo getAttackTypeInfo(Player p) {
        if (p.getMainHandItem().hasTag() && p.getMainHandItem().getTag().contains(CURRENT_ATTACK_TYPE_TAG_KEY)) {
            return getWeaponInfo(p).getAttackTypes().get(getAttackType(p));
        }
        else {
            return AttackTypeInfo.DEFAULT;
        }
    }

    public static WeaponInfo getWeaponInfo(ItemStack i) {
        if (i.hasTag() && i.getTag().contains(WEAPON_INFO_TAG_KEY)) {
            return WeaponInfo.fromCompoundTag(i.getTag().getCompound(WEAPON_INFO_TAG_KEY));
        }
        else {
            return WeaponInfo.NO_WEAPON;
        }
    }

    public static AttackType[] getAttackTypes(ItemStack i) {
        WeaponInfo info = getWeaponInfo(i);
        return info.getAttackTypes().keySet().toArray(AttackType[]::new);
    }

    public static AttackType getAttackType(ItemStack i) {
        if (i.hasTag() && i.getTag().contains(CURRENT_ATTACK_TYPE_TAG_KEY)) {
            return getAttackTypes(i)[i.getTag().getInt(CURRENT_ATTACK_TYPE_TAG_KEY)];
        }
        else {
            return AttackType.strike();
        }
    }

    public static AttackTypeInfo getAttackTypeInfo(ItemStack i) {
        if (i.hasTag() && i.getTag().contains(CURRENT_ATTACK_TYPE_TAG_KEY)) {
            return getWeaponInfo(i).getAttackTypes().get(getAttackType(i));
        }
        else {
            return AttackTypeInfo.DEFAULT;
        }
    }

    public static void updateWeaponInfoItemsFromResources(ResourceManager manager) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        LOGGER.info("------------------> Reading Weapon Info json files");
        manager.listResources("weapon_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
            LOGGER.info("--------------> " + rl.toString());
            String[] pathSplit = rl.getPath().split("/");
            String modId = pathSplit[pathSplit.length-1].substring(0, pathSplit[pathSplit.length-1].length()-5);
            if (FMLLoader.getLoadingModList().getModFileById(modId)!=null) {
                try {
                    BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
                    JsonObject obj = gson.fromJson(reader, JsonObject.class);
                    DataResult<Map<String, WeaponInfo>> info = WeaponInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
                    info.result().ifPresent((map) -> {
                        map.forEach((key, value) -> {
                            LOGGER.info("----------> " + key);
                            ResourceLocation iRL = new ResourceLocation(modId, key);
                            Item iItem = ForgeRegistries.ITEMS.getValue(iRL);
                            if (iItem != null) {
                                AccessWeaponInfo mItem = (AccessWeaponInfo) iItem;
                                mItem.skada$setWeaponInfo(value);
                            }
                        });
                    });
                } catch (Exception e) {
                    LOGGER.error("Failed to read weapon info from " + rl, e);
                }
            }
            else {
                LOGGER.info("----------> Skipping weapon info file for mod " + modId + " because it is not loaded!");
            }
        });
    }

    public static void updateArmourInfoItemsFromResources(ResourceManager manager) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        LOGGER.info("------------------> Reading Armour Info json files");
        manager.listResources("armour_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
            LOGGER.info("--------------> " + rl.toString());
            String[] pathSplit = rl.getPath().split("/");
            String modId = pathSplit[pathSplit.length-1].substring(0, pathSplit[pathSplit.length-1].length()-5);
            if (FMLLoader.getLoadingModList().getModFileById(modId)!=null) {
                try {
                    BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
                    JsonObject obj = gson.fromJson(reader, JsonObject.class);
                    DataResult<Map<String, ArmourInfo>> info = ArmourInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
                    info.result().ifPresent((map) -> {
                        map.forEach((key, value) -> {
                            LOGGER.info("----------> " + key);
                            ResourceLocation iRL = new ResourceLocation(modId, key);
                            Item iItem = ForgeRegistries.ITEMS.getValue(iRL);
                            if (iItem != null) {
                                AccessArmourInfo mItem = (AccessArmourInfo) iItem;
                                mItem.skada$setArmourInfo(value);
                            }
                        });
                    });
                } catch (Exception e) {
                    LOGGER.error("Failed to read armour info from " + rl, e);
                }
            }
            else {
                LOGGER.info("----------> Skipping armour info file for mod " + modId + " because it is not loaded!");
            }
        });
    }

    public static void updateMobInfoFromResources(ResourceManager manager) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        LOGGER.info("------------------> Reading Mob Info json files");
        manager.listResources("mob_info", (rl) -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
            LOGGER.info("--------------> " + rl.toString());
            String[] pathSplit = rl.getPath().split("/");
            String modId = pathSplit[pathSplit.length-1].substring(0, pathSplit[pathSplit.length-1].length()-5);
            if (FMLLoader.getLoadingModList().getModFileById(modId)!=null) {
                try {
                    BufferedReader reader = new BufferedReader(manager.openAsReader(rl));
                    JsonObject obj = gson.fromJson(reader, JsonObject.class);
                    DataResult<Map<String, MobData>> info = MobData.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, obj);
                    info.result().ifPresent((map) -> {
                        map.forEach((key, value) -> {
                            LOGGER.info("----------> " + key);
                            ResourceLocation iRL = new ResourceLocation(modId, key);
                            EntityType<?> iEntity = getMobEntityType(iRL);
                            if (iEntity != null) {
                                MOB_DATA.put(iEntity, value);
                            }
                        });
                    });
                } catch (Exception e) {
                    LOGGER.error("Failed to read mob info from " + rl, e);
                }
            }
            else {
                LOGGER.info("----------> Skipping mob info file for mod " + modId + " because it is not loaded!");
            }
        });
        LOGGER.info("-----------> Finished loading mob info, flattening parents");
        MOB_DATA.forEach((key, value) -> {
            if (value.parent() != null) flattenParentModifiers(value);
        });
    }

    private static void flattenParentModifiers(MobData mobData) {
        if (mobData.parent() != null) {
            MobData parent = MOB_DATA.get(getMobEntityType(ResourceLocation.tryParse(mobData.parent())));
            if (parent != null) {
                flattenParentModifiers(parent);
                Multimap<Attribute, AttributeModifier> parentMods = parent.extraModifiers();
                Multimap<Attribute, AttributeModifier> childMods = mobData.extraModifiers();
                for (Attribute a : parentMods.keySet()) {
                    childMods.putAll(a, parentMods.get(a));
                }
            }
            else {
                LOGGER.error("Tried to retrieve mob data for requested parent: {} but result was null", mobData.parent());
            }
        }
    }

    private static EntityType<?> getMobEntityType(ResourceLocation iRL) {
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(iRL);
        if (entityType != null && entityType.getCategory() != MobCategory.MISC) {
            return entityType;
        }
        return null; // or handle the case where it's not a Mob
    }

    private static Vec3 getFirstViewPlayerHandPos(Player player, boolean isLeftHand, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        double d4 = 960.0 / mc.options.fov().get();
        Vec3 vec3 = mc.gameRenderer.getMainCamera().getNearPlane().getPointOnPlane(isLeftHand ? -0.525F : 0.525F, -0.3F).scale(d4);
        return player.getEyePosition(partialTick).add(vec3);
    }

    public static Vec3 get3DCoordFrom2D(float x, float y, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        double d4 = 960.0 / mc.options.fov().get(); // Magic number for scaling

        // Get the screen dimensions
        float width = mc.getWindow().getGuiScaledWidth();
        float height = mc.getWindow().getGuiScaledHeight();

        // Convert screen coordinates to normalized device coordinates (NDC)
        float leftScale = (x / width) * 2 - 1; // NDC x-coordinate
        float upScale = 1 - (y / height) * 2; // NDC y-coordinate (inverted)

        // Get the point on the near plane
        Vec3 vec3 = mc.gameRenderer.getMainCamera().getNearPlane().getPointOnPlane(leftScale, upScale).scale(d4);

        // Add the player's eye position to get the correct 3D coordinate
        return mc.cameraEntity.getEyePosition(partialTick).add(vec3);
    }

    public static Vec3 getMovementVector(Vec3 start, Vec3 end) {
        return end.subtract(start);
    }

    //given a pair of numbers, x, y, return 4 vertices that represent a tiny square centered at x, y
    public static void drawPixel(BufferBuilder buffer, PoseStack stack, float x, float y) {
        //buffer.vertex(stack.last().pose(), pair.getA(), pair.getB(), 0).color(0.4f, 0.4f, 0.4f, 1f).endVertex();
        buffer.vertex(x+0.5f, y-0.5f, 0).color(0.7f, 0.4f, 0.4f, 1f).endVertex();
        buffer.vertex(x+0.5f, y+0.5f, 0).color(0.7f, 0.4f, 0.4f, 1f).endVertex();
        buffer.vertex(x-0.5f, y+0.5f, 0).color(0.7f, 0.4f, 0.4f, 1f).endVertex();
        buffer.vertex(x-0.5f, y-0.5f, 0).color(0.7f, 0.4f, 0.4f, 1f).endVertex();
    }

    /*
    function to calculate the percentage reduction in armour for strike attack type. Returns a value between 0 and 1 that should
    be multiplied with the target's armour. When lethality is 5x armour toughness, returns 0.5.
    */
    public static double percentReduc(double lethality, double armorToughness, double targetHP) {
        if (armorToughness == 0) return 0;
        double ratio = lethality / armorToughness;
        double effectiveness = 1 / (1 + ratio / 5);
        return Math.max(0, Math.min(1, effectiveness));
    }

    /*
    function to calculate percentage health damage for thrust attack type. Returns a real damage number to be summed with
    the current damage total. When lethality is 2x armour toughness, returns 15% of the target's hp.
     */
    public static double percentHealthDamage(double lethality, double armorToughness, double targetHP) {
        return 0.075*lethality/(armorToughness+1)*targetHP;
    }

    /*
    function to calculate bonus damage for slash attack type. Returns a real damage number to be multiplied with
    the current damage total. When lethality is 2x armour toughness, returns 1.35 (35% bonus damage).
     */
    public static double percentBonusDamage(double lethality, double armorToughness, double targetHP) {
        if (lethality == 0) return 1;
        return 1+(1/Math.pow(2, 3*armorToughness/lethality));
    }

    public static double slashLethalityCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = weight;
        bonus += hardness*0.5;
        return bonus;
    }

    public static double slashDamageCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = 0.0;
        bonus -= toughness*0.5;
        bonus += flexibility;
        return bonus;
    }

    public static double thrustLethalityCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = weight;
        bonus += hardness*0.5;
        return bonus;
    }

    public static double thrustDamageCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = 0.0;
        bonus += toughness*0.5;
        return bonus;
    }

    public static double strikeLethalityCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = weight;
        bonus += toughness*0.5;
        return bonus;
    }

    public static double strikeDamageCalculation(double weight, double hardness, double toughness, double flexibility) {
        double bonus = weight*0.5;
        bonus += hardness*0.5;
        return bonus;
    }

    public static double getCriticalFailChance(double weight, double hardness, double toughness, double flexibility) {
        return 0.01*hardness*hardness/toughness;
    }

}
