package com.cwjn.skada.generation.weapon;

import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.damage.WeaponStatApplicability;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.generation.attack.AttackTypeJsonInfo;
import com.cwjn.skada.generation.attack.ElementSpread;
import com.cwjn.skada.util.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Generates final weapon statistics, including preset precedence, matching and damage squeezing.
 * Resource loading, command messages and export stay with the caller.
 */
public final class WeaponGenerator {

    private static final String ASSEMBLY_TAG_PATH = "generator/weapon_assembly/";
    private static final double ATTACK_SPEED_SOFTCAP_LOW = 0.5;
    private static final double ATTACK_SPEED_SOFTCAP_HIGH = 2.5;
    private static final double ATTACK_SPEED_LOW_TAIL = 0.04;
    private static final double ATTACK_SPEED_HIGH_TAIL = 0.2;

    private final Map<String, MaterialInfo> materials;
    private final Map<String, WeaponAssembly> assemblies;
    private final Map<String, WeaponInfo> presets;
    private final boolean squeezeDamage;
    private final double retainedDamageDifference;

    /**
     * Captures the item facts generation needs without retaining live registry access.
     * A null tier means the item is not tiered and does not participate in squeezing.
     *
     * @param id the registered item identifier
     * @param tags the item's generation tags
     * @param tier the lower-case tier name, or null for a non-tiered item
     * @param projectile whether the item is a projectile weapon
     * @param hasHandAttributes whether either hand has attribute modifiers
     * @param attackSpeed the item's base attack speed including its default modifier
     * @param baseDamage the item's default base damage modifier
     */
    public record ItemInput(ResourceLocation id, List<ResourceLocation> tags, @Nullable String tier,
                            boolean projectile, boolean hasHandAttributes, double attackSpeed, double baseDamage,
                            boolean thrownHybrid) {

        /** Retains the original fact constructor for integrations without a thrown-item classification. */
        public ItemInput(ResourceLocation id, List<ResourceLocation> tags, @Nullable String tier,
                         boolean projectile, boolean hasHandAttributes, double attackSpeed, double baseDamage) {
            this(id, tags, tier, projectile, hasHandAttributes, attackSpeed, baseDamage,
                    id.equals(new ResourceLocation("minecraft", "trident")));
        }

        /** Reads generation facts from an item after Forge registration and tag loading. */
        public static ItemInput fromItem(Item item) {
            ItemStack stack = item.getDefaultInstance();
            return new ItemInput(Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)),
                    ForgeRegistries.ITEMS.getHolder(item).orElseThrow().tags().map(TagKey::location).toList(),
                    item instanceof TieredItem tiered ? tiered.getTier().toString().toLowerCase(Locale.ROOT) : null,
                    item instanceof ProjectileWeaponItem,
                    !stack.getAttributeModifiers(EquipmentSlot.MAINHAND).isEmpty()
                            || !stack.getAttributeModifiers(EquipmentSlot.OFFHAND).isEmpty(),
                    Util.getAttackSpeedForItem(item), Util.getDamageModifierForItem(item), item instanceof TridentItem);
        }
    }

    /** Final weapon statistics and item-specific explanations of skips or material fallbacks. */
    public record Result(Map<String, WeaponInfo> weapons, Map<ResourceLocation, String> diagnostics) {}

    /** Internal metadata retained until the complete peer group can be squeezed. */
    private record GeneratedWeaponInfoEntry(WeaponInfo weaponInfo, String assemblyKey,
                                           double baseDamageModifier, boolean squeezeEligible) {}

    /**
     * Creates a generator from already loaded definitions and the selected namespace's presets.
     * The retained fraction scales original differences: 0 equalizes base damage; 1 preserves it.
     */
    public WeaponGenerator(Map<String, MaterialInfo> materials, Map<String, WeaponAssembly> assemblies,
                           Map<String, WeaponInfo> presets, boolean squeezeDamage, double retainedDamageDifference) {
        this.materials = materials;
        this.assemblies = assemblies;
        this.presets = presets;
        this.squeezeDamage = squeezeDamage;
        this.retainedDamageDifference = retainedDamageDifference;
    }

    /**
     * Generates a namespace or one selected item using the same complete namespace peer group.
     * Presets bypass procedural eligibility, assembly/material matching and squeezing entirely.
     * Only the selected item's diagnostics are returned for an individual-item request.
     *
     * @param namespace the namespace being generated
     * @param items the candidate item facts, including peers needed for damage squeezing
     * @param onlyPath an item path to select after generation, or null for the whole namespace
     * @return final statistics and diagnostics, without reading or writing output files
     */
    public Result generate(String namespace, Collection<ItemInput> items, @Nullable String onlyPath) {
        if (onlyPath != null && presets.containsKey(onlyPath)) {
            return new Result(Map.of(onlyPath, presets.get(onlyPath)), Map.of());
        }
        Map<String, GeneratedWeaponInfoEntry> generated = new TreeMap<>();
        Map<ResourceLocation, String> diagnostics = new LinkedHashMap<>();
        for (ItemInput item : items) {
            if (!item.id().getNamespace().equals(namespace) || presets.containsKey(item.id().getPath())) {
                continue;
            }
            if (!item.hasHandAttributes() && !item.projectile()) {
                if (item.id().getPath().equals(onlyPath)) {
                    diagnostics.put(item.id(), "Item is not a weapon");
                }
                continue;
            }
            String assemblyKey = resolveAssemblyKey(item);
            if (assemblyKey == null) {
                diagnostics.put(item.id(), "No weapon assembly found; use a generation tag or a supported item name");
                continue;
            }
            MaterialInfo material = resolveMaterial(item);
            if (material == null) {
                diagnostics.put(item.id(), "No material found; using the default material");
                material = MaterialInfo.getDefault();
            }
            WeaponStatApplicability applicability = resolveApplicability(item);
            WeaponInfo info = generateStats(material, assemblies.get(assemblyKey), applicability,
                    item.attackSpeed(), item.baseDamage());
            generated.put(item.id().getPath(), new GeneratedWeaponInfoEntry(info, assemblyKey,
                    item.baseDamage(), item.tier() != null));
        }
        Map<String, WeaponInfo> weapons = new TreeMap<>(squeezeGeneratedWeaponInfos(
                generated, squeezeDamage, retainedDamageDifference));
        weapons.putAll(presets);
        if (onlyPath != null) {
            weapons.keySet().removeIf(path -> !path.equals(onlyPath));
            diagnostics.keySet().removeIf(id -> !id.getPath().equals(onlyPath));
        }
        return new Result(Collections.unmodifiableMap(weapons), Collections.unmodifiableMap(diagnostics));
    }

    /**
     * Explicit generation tags override inheritance. Both wins; separate melee and projectile tags
     * combine to both so datapack tag merging is deterministic. Presets never reach this inference.
     */
    private static WeaponStatApplicability resolveApplicability(ItemInput item) {
        String prefix = "generator/weapon_applicability/";
        boolean both = item.tags().contains(new ResourceLocation("skada", prefix + "both"));
        boolean melee = item.tags().contains(new ResourceLocation("skada", prefix + "melee"));
        boolean projectile = item.tags().contains(new ResourceLocation("skada", prefix + "projectile"));
        if (both || (melee && projectile)) return WeaponStatApplicability.BOTH;
        if (melee) return WeaponStatApplicability.MELEE;
        if (projectile) return WeaponStatApplicability.PROJECTILE;
        return item.thrownHybrid() ? WeaponStatApplicability.BOTH
                : item.projectile() ? WeaponStatApplicability.PROJECTILE : WeaponStatApplicability.MELEE;
    }

    /** Resolves an unmodified assembly for physics inspection, using the same matching as generation. */
    @Nullable
    public WeaponAssembly resolveAssembly(ItemInput item) {
        String key = resolveAssemblyKey(item);
        return key == null ? null : assemblies.get(key);
    }

    /** Resolves a tiered item's material without substituting the generation-only default. */
    @Nullable
    public MaterialInfo resolveMaterial(ItemInput item) {
        if (item.tier() == null) {
            return null;
        }
        String exact = item.tier().contains(".") ? item.tier() : item.id().getNamespace() + "." + item.tier();
        if (materials.containsKey(exact)) {
            return materials.get(exact);
        }
        for (Map.Entry<String, MaterialInfo> entry : materials.entrySet()) {
            if (entry.getKey().endsWith("." + item.tier())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** Preserves tag precedence, normalized matching and the existing fuzzy name fallback. */
    @Nullable
    private String resolveAssemblyKey(ItemInput item) {
        String taggedValue = item.tags().stream()
                .filter(tag -> tag.getNamespace().equals("skada") && tag.getPath().startsWith(ASSEMBLY_TAG_PATH))
                .map(tag -> tag.getPath().substring(ASSEMBLY_TAG_PATH.length()))
                .sorted(Comparator.comparingInt(String::length).reversed()).findFirst().orElse(null);
        String key = null;
        if (taggedValue != null) {
            String normalized = normalizeName(taggedValue);
            key = assemblies.keySet().stream()
                    .sorted(Comparator.comparingInt((String name) -> normalizeName(name).length()).reversed())
                    .filter(name -> normalizeName(name).equals(normalized)
                            || normalizeName(name.substring(name.lastIndexOf('.') + 1)).equals(normalized))
                    .findFirst().orElse(null);
        }
        if (key == null) {
            key = Util.findClosestMatch(new ArrayList<>(assemblies.keySet()), item.id().getPath());
        }
        return key == null || key.isBlank() ? null : key;
    }

    /** Normalizes full and simple assembly names for generation-tag matching. */
    private static String normalizeName(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    /** Applies the existing stat formulas, configured offsets and attack-speed soft caps. */
    private static WeaponInfo generateStats(MaterialInfo material, WeaponAssembly assembly, WeaponStatApplicability applicability,
                                            double baseAttackSpeed, double damageModifier) {
        assembly = assembly.withMaterialWoodenHandle(material);
        ElementSpread spread = material.spread();
        Map<AttackType, AttackTypeInfo> retMap = new HashMap<>();
        for (Map.Entry<AttackType, AttackTypeJsonInfo> entry : assembly.getAttackTypes().entrySet()) {
            var generatedStats = entry.getKey().tierStatFunction().generateAll(assembly);
            retMap.put(entry.getKey(), new AttackTypeInfo(
                    Util.round(generatedStats.lethality() * entry.getValue().lethalityModifier(), 2),
                    Util.round(generatedStats.precision() * entry.getValue().precisionModifier(), 2),
                    entry.getValue().minReach(),
                    entry.getValue().maxReach(),
                    resolveAttackSpeed(baseAttackSpeed, generatedStats.attackSpeed(), entry.getValue().attackSpeed()),
                    Util.round(damageModifier + entry.getValue().damage(), 3),
                    Util.round(generatedStats.criticalFail() * entry.getValue().critFailModifier(), 3),
                    entry.getValue().reticleShapes()));
        }
        return new WeaponInfo(retMap, spread, applicability);
    }

    /** Uses configured speed when present, otherwise soft-caps the generated speed. */
    private static double resolveAttackSpeed(double baseAttackSpeed, double generatedAttackSpeedAdjustment, double configuredAttackSpeed) {
        if (Double.isFinite(configuredAttackSpeed)) {
            return Util.round(configuredAttackSpeed, 3);
        }
        return Util.round(softCapGeneratedAttackSpeed(baseAttackSpeed + generatedAttackSpeedAdjustment), 3);
    }

    /** Preserves the existing exponential tails outside the generated attack-speed range. */
    private static double softCapGeneratedAttackSpeed(double generatedAttackSpeed) {
        if (generatedAttackSpeed < ATTACK_SPEED_SOFTCAP_LOW) {
            return ATTACK_SPEED_SOFTCAP_LOW - ATTACK_SPEED_LOW_TAIL
                    * (1.0 - Math.exp((generatedAttackSpeed - ATTACK_SPEED_SOFTCAP_LOW) / ATTACK_SPEED_LOW_TAIL));
        }
        if (generatedAttackSpeed > ATTACK_SPEED_SOFTCAP_HIGH) {
            return ATTACK_SPEED_SOFTCAP_HIGH + ATTACK_SPEED_HIGH_TAIL
                    * (1.0 - Math.exp((ATTACK_SPEED_SOFTCAP_HIGH - generatedAttackSpeed) / ATTACK_SPEED_HIGH_TAIL));
        }
        return generatedAttackSpeed;
    }

    /** Compresses only procedural tiered peers, preserving their attack-type offsets. */
    private static Map<String, WeaponInfo> squeezeGeneratedWeaponInfos(Map<String, GeneratedWeaponInfoEntry> generatedInfos,
            boolean squeezeDamageValues,
            double damageDifferenceAmount) {
        TreeMap<String, WeaponInfo> squeezedInfos = new TreeMap<>();
        if (!squeezeDamageValues) {
            generatedInfos.forEach((path, generatedInfo) -> squeezedInfos.put(path, generatedInfo.weaponInfo()));
            return squeezedInfos;
        }

        double squeezeAmount = Math.max(0.0, Math.min(1.0, damageDifferenceAmount));
        Map<String, Double> minimumBaseDamageByAssembly = new HashMap<>();
        for (GeneratedWeaponInfoEntry generatedInfo : generatedInfos.values()) {
            if (!generatedInfo.squeezeEligible() || generatedInfo.assemblyKey() == null) {
                continue;
            }
            minimumBaseDamageByAssembly.merge(generatedInfo.assemblyKey(),
                    generatedInfo.baseDamageModifier(),
                    Math::min);
        }

        generatedInfos.forEach((path, generatedInfo) -> {
            if (!generatedInfo.squeezeEligible() || generatedInfo.assemblyKey() == null) {
                squeezedInfos.put(path, generatedInfo.weaponInfo());
                return;
            }

            double minimumBaseDamage = minimumBaseDamageByAssembly.getOrDefault(generatedInfo.assemblyKey(),
                    generatedInfo.baseDamageModifier());
            double squeezedBaseDamage = Util.round(minimumBaseDamage
                    + squeezeAmount * (generatedInfo.baseDamageModifier() - minimumBaseDamage), 3);
            squeezedInfos.put(path,
                    withAdjustedBaseDamage(generatedInfo.weaponInfo(), generatedInfo.baseDamageModifier(), squeezedBaseDamage));
        });
        return squeezedInfos;
    }

    /**
     * Rebuild a generated weapon info after replacing only its vanilla-derived base damage modifier.
     *
     * @param weaponInfo the original generated weapon info
     * @param originalBaseDamage the original vanilla-derived base damage modifier
     * @param adjustedBaseDamage the squeezed vanilla-derived base damage modifier
     * @return a weapon info with updated attack-type damage values
     */
    private static WeaponInfo withAdjustedBaseDamage(WeaponInfo weaponInfo, double originalBaseDamage, double adjustedBaseDamage) {
        double baseDamageDelta = adjustedBaseDamage - originalBaseDamage;
        Map<AttackType, AttackTypeInfo> adjustedAttackTypes = new LinkedHashMap<>();
        for (Map.Entry<AttackType, AttackTypeInfo> entry : weaponInfo.getAttackTypes().entrySet()) {
            AttackTypeInfo attackTypeInfo = entry.getValue();
            adjustedAttackTypes.put(entry.getKey(), new AttackTypeInfo(
                    attackTypeInfo.lethality(),
                    attackTypeInfo.precision(),
                    attackTypeInfo.minReach(),
                    attackTypeInfo.maxReach(),
                    attackTypeInfo.attackSpeed(),
                    Util.round(attackTypeInfo.damage() + baseDamageDelta, 3),
                    attackTypeInfo.failChance(),
                    attackTypeInfo.reticleShapes()));
        }
        return new WeaponInfo(adjustedAttackTypes, weaponInfo.getSpread(), weaponInfo.applicability());
    }

}
