package com.cwjn.skada.generation.weapon;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.damage.WeaponStatApplicability;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.generation.attack.AttackTypeGeneratorConfiguration;
import com.cwjn.skada.generation.GenJsonLoader;
import com.google.gson.JsonObject;
import java.util.concurrent.atomic.AtomicInteger;
import com.cwjn.skada.generation.attack.AttackTypeJsonInfo;
import com.cwjn.skada.generation.attack.ElementSpread;
import com.cwjn.skada.generation.weapon.attack_capability.NoneCapable;
import com.cwjn.skada.generation.weapon.attack_capability.AttackCapable;
import com.cwjn.skada.generation.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.generation.weapon.generation_algo.AttackSpeedGenerationUtil;
import com.cwjn.skada.generation.weapon.generation_algo.CriticalFailGenerationUtil;
import com.cwjn.skada.generation.weapon.generation_algo.LethalityGenerationUtil;
import com.cwjn.skada.generation.weapon.generation_algo.KnockbackGenerationUtil;
import com.cwjn.skada.generation.weapon.generation_algo.PrecisionGenerationUtil;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextFactory;
import com.cwjn.skada.generation.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.generation.weapon.attack_capability.ThrustCapable;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Exercises final generation results rather than private matching or squeezing helpers. */
class WeaponGeneratorTest {

    private static final double EPS = 1.0e-9;
    private static Supplier<IForgeRegistry<Element>> previousElements;
    private static Supplier<IForgeRegistry<AttackType>> previousAttacks;
    private static Element physical;
    private static AttackType primary;
    private static AttackType secondary;
    private static AttackType slash;
    private static AttackType thrust;
    private static AttackType strike;

    /** Supplies the registry data used by codecs and default element spreads without starting Forge. */
    @BeforeAll
    @SuppressWarnings("unchecked")
    static void setUpRegistries() {
        previousElements = SkadaData.REGISTRY_ELEMENT;
        previousAttacks = SkadaData.REGISTRY_ATTACK_TYPE;
        physical = new Element("physical", null, null, null, 0, id("skada:physical"), null);
        IForgeRegistry<Element> elements = mock(IForgeRegistry.class);
        when(elements.getValue(id("skada:physical"))).thenReturn(physical);
        when(elements.getKey(physical)).thenReturn(id("skada:physical"));
        SkadaData.REGISTRY_ELEMENT = () -> elements;

        AttackTypeGeneratorConfiguration calculations = new AttackTypeGeneratorConfiguration(
                assembly -> new AttackTypeGeneratorConfiguration.GeneratedStats(11.111, 22.222, 0.12345, -0.6));
        primary = new AttackType("primary", null, calculations, null, NoneCapable.class);
        secondary = new AttackType("secondary", null, calculations, null, NoneCapable.class);
        slash = new AttackType("slash", null, SkadaData.SLASH_GENERATOR_CONFIG, null, SlashCapable.class);
        thrust = new AttackType("thrust", null, SkadaData.THRUST_GENERATOR_CONFIG, null, ThrustCapable.class);
        strike = new AttackType("strike", null, SkadaData.STRIKE_GENERATOR_CONFIG, null, StrikeCapable.class);
        IForgeRegistry<AttackType> attacks = mock(IForgeRegistry.class);
        for (AttackType type : List.of(primary, secondary, slash, thrust, strike)) {
            when(attacks.getValue(id("skada:" + type.name()))).thenReturn(type);
            when(attacks.getKey(type)).thenReturn(id("skada:" + type.name()));
        }
        SkadaData.REGISTRY_ATTACK_TYPE = () -> attacks;
        // Initialize the runtime defaults without DeferredRegister's game startup lifecycle.
        try (MockedStatic<AttackType> defaults = mockStatic(AttackType.class, CALLS_REAL_METHODS)) {
            defaults.when(AttackType::strike).thenReturn(primary);
            new WeaponInfo();
        }
    }

    /** Restores global registry suppliers so this suite cannot affect later tests. */
    @AfterAll
    static void restoreRegistries() {
        SkadaData.REGISTRY_ELEMENT = previousElements;
        SkadaData.REGISTRY_ATTACK_TYPE = previousAttacks;
    }

    @Test
    void singleItemMatchesNamespaceUsingAllPeersAndPreservesAttackOffsets() {
        WeaponGenerator generator = generator(Map.of("sword", assembly(1.25)), Map.of(), true, 0.5);
        List<WeaponGenerator.ItemInput> items = List.of(item("wood_sword", 4), item("iron_sword", 7), item("netherite_sword", 9));

        WeaponGenerator.Result all = generator.generate("example", items, null);
        WeaponGenerator.Result single = generator.generate("example", items, "iron_sword");

        assertEquals(Set.of("iron_sword"), single.weapons().keySet());
        assertEquals(all.weapons().get("iron_sword").getAttackTypes(), single.weapons().get("iron_sword").getAttackTypes());
        assertDamage(all, "wood_sword", primary, 5.25);
        assertDamage(all, "iron_sword", primary, 6.75);
        assertDamage(all, "netherite_sword", primary, 7.75);
        assertDamage(all, "iron_sword", secondary, 4.75);
        AttackTypeInfo attack = all.weapons().get("iron_sword").getAttackTypes().get(primary);
        assertEquals(11.11, attack.lethality(), EPS);
        assertEquals(22.22, attack.precision(), EPS);
        assertEquals(0.247, attack.failChance(), EPS);
        assertEquals(1.0, attack.attackSpeed(), EPS);
        assertEquals(0.25, attack.minReach(), EPS);
        assertEquals(2.75, attack.maxReach(), EPS);
        assertEquals(List.of("test_reticle"), attack.reticleShapes());
        assertEquals(1.876, all.weapons().get("iron_sword").getAttackTypes().get(secondary).attackSpeed(), EPS);
    }

    @Test
    void retainedFractionScalesUnevenGapsRatherThanAssigningFixedTierIncrements() {
        List<WeaponGenerator.ItemInput> items = List.of(item("wood_sword", 4), item("iron_sword", 8), item("netherite_sword", 12));
        for (double fraction : List.of(0.0, 0.25, 0.5, 1.0)) {
            WeaponGenerator.Result result = generator(Map.of("sword", assembly(0)), Map.of(), true, fraction)
                    .generate("example", items, null);
            assertDamage(result, "wood_sword", primary, 4);
            assertDamage(result, "iron_sword", primary, 4 + 4 * fraction);
            assertDamage(result, "netherite_sword", primary, 4 + 8 * fraction);
        }
    }

    @Test
    void disablingSqueezingPreservesOriginalDamage() {
        WeaponGenerator.Result result = generator(Map.of("sword", assembly(1.25)), Map.of(), false, 0)
                .generate("example", List.of(item("wood_sword", 4), item("iron_sword", 9)), null);
        assertDamage(result, "iron_sword", primary, 10.25);
        assertDamage(result, "iron_sword", secondary, 8.25);
    }

    @Test
    void differentAssembliesAndNamespacesDoNotShareSqueezeMinima() {
        WeaponGenerator.Result result = generator(Map.of("sword", assembly(0), "axe", assembly(0)), Map.of(), true, 0.5)
                .generate("example", List.of(item("wood_sword", 4), item("iron_sword", 8), item("wood_axe", 10),
                        item("iron_axe", 14), input("other:tiny_sword", "wood", false, true, 0, List.of())), null);
        assertDamage(result, "iron_sword", primary, 6);
        assertDamage(result, "iron_axe", primary, 12);
        assertFalse(result.weapons().containsKey("tiny_sword"));
    }

    @Test
    void presetsOverrideGenerationAndDoNotInfluencePeerSqueezing() {
        WeaponInfo preset = new WeaponInfo(Map.of(primary, AttackTypeInfo.FIST), spread(3), WeaponStatApplicability.PROJECTILE);
        WeaponGenerator generator = generator(Map.of("sword", assembly(0)), Map.of("wood_sword", preset), true, 0.5);
        List<WeaponGenerator.ItemInput> items = List.of(item("wood_sword", 0), item("iron_sword", 8), item("diamond_sword", 10));
        WeaponGenerator.Result all = generator.generate("example", items, null);
        assertSame(preset, all.weapons().get("wood_sword"));
        assertDamage(all, "iron_sword", primary, 8);
        assertDamage(all, "diamond_sword", primary, 9);
        assertSame(preset, generator.generate("example", items, "wood_sword").weapons().get("wood_sword"));
    }

    @Test
    void presetsNeedNeitherProceduralEligibilityNorAssemblyAndReturnWithoutGeneratingPeers() {
        WeaponInfo preset = new WeaponInfo(Map.of(primary, AttackTypeInfo.FIST), spread(1), WeaponStatApplicability.BOTH);
        WeaponGenerator generator = generator(Map.of(), Map.of("special", preset), true, 0.5);
        WeaponGenerator.ItemInput ordinary = input("example:special", null, false, false, 0, List.of());
        assertSame(preset, generator.generate("example", List.of(ordinary), null).weapons().get("special"));
        // Invalid procedural damage would fail rounding if peers were unnecessarily generated.
        WeaponGenerator withAssembly = generator(Map.of("sword", assembly(0)), Map.of("special", preset), true, 0.5);
        assertSame(preset, withAssembly.generate("example", List.of(item("broken_sword", Double.NaN)), "special")
                .weapons().get("special"));
    }

    @Test
    void projectileWeaponsWithoutHandAttributesQualifyForBothRequests() {
        WeaponGenerator generator = generator(Map.of("bow", assembly(0)), Map.of(), true, 0.5);
        WeaponGenerator.ItemInput bow = input("example:bow", null, true, false, 5, List.of());
        WeaponGenerator.Result all = generator.generate("example", List.of(bow), null);
        WeaponGenerator.Result single = generator.generate("example", List.of(bow), "bow");
        assertEquals(WeaponStatApplicability.PROJECTILE, all.weapons().get("bow").applicability());
        assertEquals(all.weapons().get("bow").getAttackTypes(), single.weapons().get("bow").getAttackTypes());
        assertDamage(all, "bow", primary, 5);
    }

    @Test
    void nonTieredWeaponsNeitherUndergoNorInfluenceSqueezing() {
        WeaponGenerator.Result result = generator(Map.of("sword", assembly(0)), Map.of(), true, 0.5)
                .generate("example", List.of(input("example:odd_sword", null, false, true, 0, List.of()),
                        item("iron_sword", 8), item("diamond_sword", 10)), null);
        assertDamage(result, "odd_sword", primary, 0);
        assertDamage(result, "iron_sword", primary, 8);
        assertDamage(result, "diamond_sword", primary, 9);
    }

    @Test
    void generationTagWinsOverNameAndMatchesNormalizedSimpleAssemblyNames() {
        WeaponGenerator generator = generator(Map.of("sword", assembly(0), "example.heavy_axe", assembly(10)), Map.of(), false, 1);
        WeaponGenerator.ItemInput tagged = input("example:iron_sword", "iron", false, true, 8,
                List.of(id("skada:generator/weapon_assembly/heavy-axe")));
        assertDamage(generator.generate("example", List.of(tagged), null), "iron_sword", primary, 18);
    }

    @Test
    void invalidTagFallsBackToNameAndNameMatchingRetainsRegexAndFuzzyPasses() {
        WeaponGenerator generator = generator(Map.of("sword", assembly(0), "long_sword", assembly(10)), Map.of(), false, 1);
        WeaponGenerator.ItemInput regex = input("example:iron_long_fancy_sword", "iron", false, true, 8,
                List.of(id("skada:generator/weapon_assembly/missing")));
        assertDamage(generator.generate("example", List.of(regex, item("sord", 8)), null), "iron_long_fancy_sword", primary, 18);
        assertDamage(generator.generate("example", List.of(item("sord", 8)), null), "sord", primary, 8);
    }

    @Test
    void materialLookupPrefersItemNamespaceThenFallsBackAcrossNamespaces() {
        MaterialInfo local = material(2);
        MaterialInfo foreign = material(3);
        Map<String, MaterialInfo> materials = new LinkedHashMap<>();
        materials.put("other.iron", foreign);
        materials.put("example.iron", local);
        WeaponGenerator.ItemInput item = item("iron_sword", 8);
        WeaponGenerator localGenerator = new WeaponGenerator(materials, Map.of("sword", assembly(0)), Map.of(), false, 1);
        assertSame(local.spread(), localGenerator.generate("example", List.of(item), null).weapons().get("iron_sword").getSpread());
        WeaponGenerator fallback = new WeaponGenerator(Map.of("other.iron", foreign), Map.of("sword", assembly(0)), Map.of(), false, 1);
        assertSame(foreign.spread(), fallback.generate("example", List.of(item), null).weapons().get("iron_sword").getSpread());
    }

    @Test
    void unknownMaterialUsesDefaultAndReturnsDiagnostic() {
        WeaponGenerator generator = new WeaponGenerator(Map.of(), Map.of("sword", assembly(0)), Map.of(), false, 1);
        WeaponGenerator.Result result = generator.generate("example", List.of(item("unknown_sword", 8)), null);
        assertDamage(result, "unknown_sword", primary, 8);
        assertEquals(1.0, result.weapons().get("unknown_sword").getSpread().getPowerBudget(), EPS);
        assertTrue(result.diagnostics().get(id("example:unknown_sword")).contains("default material"));
    }

    @Test
    void missingAssemblySkipsItemsAndIndividualDiagnosticsExcludeUnrelatedPeers() {
        WeaponGenerator generator = generator(Map.of(), Map.of(), true, 0.5);
        List<WeaponGenerator.ItemInput> items = List.of(item("iron_sword", 8), item("wood_sword", 4));
        assertTrue(generator.generate("example", items, null).weapons().isEmpty());
        assertEquals(2, generator.generate("example", items, null).diagnostics().size());
        WeaponGenerator.Result selected = generator.generate("example", items, "iron_sword");
        assertEquals(Set.of(id("example:iron_sword")), selected.diagnostics().keySet());
    }

    @Test
    void ordinaryItemsAreSkippedWithoutSpuriousNamespaceWarnings() {
        WeaponGenerator generator = generator(Map.of("sword", assembly(0)), Map.of(), true, 0.5);
        WeaponGenerator.ItemInput item = input("example:stick", null, false, false, 0, List.of());
        WeaponGenerator.Result all = generator.generate("example", List.of(item), null);
        assertTrue(all.weapons().isEmpty());
        assertTrue(all.diagnostics().isEmpty());
        assertEquals("Item is not a weapon", generator.generate("example", List.of(item), "stick").diagnostics().get(item.id()));
    }

    @Test
    void generatedSpeedKeepsSoftCapsWhileExplicitAttackSpeedRemainsExact() {
        WeaponGenerator generator = generator(Map.of("sword", assembly(0)), Map.of(), false, 1);
        WeaponGenerator.ItemInput slow = new WeaponGenerator.ItemInput(id("example:slow_sword"), List.of(),
                "iron", false, true, 0.6, 4);
        WeaponGenerator.ItemInput fast = new WeaponGenerator.ItemInput(id("example:fast_sword"), List.of(),
                "iron", false, true, 4, 4);
        WeaponGenerator.Result result = generator.generate("example", List.of(slow, fast), null);
        double slowSpeed = result.weapons().get("slow_sword").getAttackTypes().get(primary).attackSpeed();
        double fastSpeed = result.weapons().get("fast_sword").getAttackTypes().get(primary).attackSpeed();
        assertTrue(slowSpeed >= 0.46 && slowSpeed < 0.5);
        assertTrue(fastSpeed > 2.5 && fastSpeed <= 2.7);
        for (WeaponInfo info : result.weapons().values()) {
            assertEquals(1.876, info.getAttackTypes().get(secondary).attackSpeed(), EPS);
        }
    }

    @Test
    void generatedStatisticsStillRoundTripThroughExportCodecAndStackNbt() {
        WeaponGenerator.Result result = generator(Map.of("sword", assembly(1.25)), Map.of(), true, 0.5)
                .generate("example", List.of(item("wood_sword", 4), item("iron_sword", 8)), null);
        var json = WeaponInfo.STRING_MAP_CODEC.encodeStart(JsonOps.INSTANCE, result.weapons()).result().orElseThrow();
        Map<String, WeaponInfo> decoded = WeaponInfo.STRING_MAP_CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
        for (Map.Entry<String, WeaponInfo> entry : result.weapons().entrySet()) {
            WeaponInfo fromJson = decoded.get(entry.getKey());
            WeaponInfo fromNbt = WeaponInfo.fromCompoundTag(entry.getValue().toCompoundTag());
            assertEquals(entry.getValue().getAttackTypes(), fromJson.getAttackTypes());
            // Existing NBT omits empty reticle lists and reads them as null; stored data must stay identical.
            assertEquals(entry.getValue().toCompoundTag(), fromNbt.toCompoundTag());
            for (WeaponInfo restored : List.of(fromJson, fromNbt)) {
                assertEquals(entry.getValue().getSpread().getRatios(), restored.getSpread().getRatios());
                assertEquals(entry.getValue().applicability(), restored.applicability());
            }
        }
    }

    @Test
    void realSwordAssemblyGeneratesFiniteStatsForBothAttacksAndKeepsIndividualParity() throws IOException {
        Path path = Path.of("src/main/resources/data/skada/generator_data/weapon/weapon_assembly/sword.json");
        WeaponAssembly sword = WeaponAssembly.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(path)))
                .result().orElseThrow();
        WeaponGenerator generator = generator(Map.of("sword", sword), Map.of(), true, 0.5);
        List<WeaponGenerator.ItemInput> items = List.of(item("iron_sword", 8), item("diamond_sword", 10));
        WeaponGenerator.Result all = generator.generate("example", items, null);
        WeaponGenerator.Result single = generator.generate("example", items, "diamond_sword");
        assertEquals(Set.of(slash, thrust), all.weapons().get("diamond_sword").getAttackTypes().keySet());
        for (AttackType type : List.of(slash, thrust)) {
            AttackTypeInfo info = all.weapons().get("diamond_sword").getAttackTypes().get(type);
            assertTrue(Double.isFinite(info.lethality()) && info.lethality() > 0);
            assertTrue(Double.isFinite(info.precision()) && info.precision() > 0);
            assertTrue(Double.isFinite(info.attackSpeed()) && info.attackSpeed() > 0);
            assertTrue(Double.isFinite(info.failChance()));
            assertEquals(info, single.weapons().get("diamond_sword").getAttackTypes().get(type));
            assertEquals(9, info.damage(), EPS);
        }
    }

    @Test
    void generationApplicabilityTagsOverrideLauncherInferenceAndEnableHybridAttributes() {
        WeaponGenerator generator = generator(Map.of("sword", assembly(0)), Map.of(), true, 0.5);
        for (String mode : List.of("melee", "projectile", "both")) {
            WeaponGenerator.ItemInput item = input("example:iron_sword", "iron", true, false, 8,
                    List.of(id("skada:generator/weapon_applicability/" + mode)));
            WeaponInfo info = generator.generate("example", List.of(item), null).weapons().get("iron_sword");
            assertEquals(WeaponStatApplicability.valueOf(mode.toUpperCase(Locale.ROOT)), info.applicability());
        }
        WeaponGenerator.ItemInput mergedTags = input("example:iron_sword", "iron", true, false, 8, List.of(
                id("skada:generator/weapon_applicability/melee"), id("skada:generator/weapon_applicability/projectile")));
        assertEquals(WeaponStatApplicability.BOTH,
                generator.generate("example", List.of(mergedTags), null).weapons().get("iron_sword").applicability());
    }

    @Test
    void generationInfersOrdinaryMeleeLaunchersAndThrownHybrids() {
        WeaponGenerator generator = generator(Map.of("sword", assembly(0)), Map.of(), false, 1);
        assertEquals(WeaponStatApplicability.MELEE,
                generator.generate("example", List.of(item("sword", 8)), null).weapons().get("sword").applicability());
        WeaponGenerator.ItemInput launcher = input("example:bow_sword", null, true, false, 0, List.of());
        assertEquals(WeaponStatApplicability.PROJECTILE,
                generator.generate("example", List.of(launcher), null).weapons().get("bow_sword").applicability());
        WeaponGenerator.ItemInput hybrid = new WeaponGenerator.ItemInput(id("example:trident_sword"), List.of(), null,
                true, true, 1.6, 8, true);
        WeaponInfo generated = generator.generate("example", List.of(hybrid), null).weapons().get("trident_sword");
        assertEquals(WeaponStatApplicability.BOTH, generated.applicability());
    }

    @Test
    void manualApplicabilityWinsOverGenerationTags() {
        WeaponInfo preset = new WeaponInfo(Map.of(primary, AttackTypeInfo.ITEM), spread(1), WeaponStatApplicability.BOTH);
        WeaponGenerator generator = generator(Map.of("sword", assembly(0)), Map.of("sword", preset), true, 0.5);
        WeaponGenerator.ItemInput item = input("example:sword", "iron", true, true, 8,
                List.of(id("skada:generator/weapon_applicability/melee")));
        assertSame(preset, generator.generate("example", List.of(item), null).weapons().get("sword"));
        assertSame(preset, generator.generate("example", List.of(item), "sword").weapons().get("sword"));
    }

    @Test
    void configuredGeneratorWinsForBuiltinAndCustomCapabilitiesInFinalWeaponStats() {
        List<Class<? extends AttackCapable>> capabilities = List.of(SlashCapable.class, ThrustCapable.class,
                StrikeCapable.class, NoneCapable.class);
        for (Class<? extends AttackCapable> capability : capabilities) {
            AtomicInteger invocations = new AtomicInteger();
            AttackTypeGeneratorConfiguration custom = new AttackTypeGeneratorConfiguration(assembly -> {
                invocations.incrementAndGet();
                return new AttackTypeGeneratorConfiguration.GeneratedStats(13.125, 29.375, 0.125, -0.25);
            });
            AttackType type = new AttackType("custom", null, custom, null, capability);
            WeaponAssembly assembly = new WeaponAssembly(List.of(), Map.of(type,
                    new AttackTypeJsonInfo(0, 3, Double.NaN, 1, 1, 0, 1, List.of())));
            WeaponInfo weapon = generator(Map.of("sword", assembly), Map.of(), false, 1)
                    .generate("example", List.of(item("iron_sword", 8)), null).weapons().get("iron_sword");
            AttackTypeInfo attack = weapon.getAttackTypes().get(type);
            assertEquals(1, invocations.get(), "All stats must come from one configured invocation");
            assertEquals(13.13, attack.precision(), EPS, capability.getSimpleName());
            assertEquals(29.38, attack.lethality(), EPS, capability.getSimpleName());
            assertEquals(0.125, attack.failChance(), EPS, capability.getSimpleName());
            assertEquals(1.35, attack.attackSpeed(), EPS, capability.getSimpleName());
            assertEquals(0.0, attack.knockback(), EPS, "An empty assembly has zero physical source impulse");
        }
    }

    @Test
    void manualKnockbackOverridesIncludingZeroAreRetained() {
        WeaponAssembly configured = new WeaponAssembly(List.of(), Map.of(
                primary, new AttackTypeJsonInfo(0.25, 2.75, Double.NaN, 1, 1, 0, 1, 0.75, List.of()),
                secondary, new AttackTypeJsonInfo(0.25, 2.75, Double.NaN, 1, 1, 0, 1, 0.0, List.of())));

        WeaponInfo result = generator(Map.of("sword", configured), Map.of(), false, 1)
                .generate("example", List.of(item("iron_sword", 8)), null).weapons().get("iron_sword");

        assertEquals(0.75, result.getAttackTypes().get(primary).knockback(), EPS);
        assertEquals(0.0, result.getAttackTypes().get(secondary).knockback(), EPS);
    }

    @Test
    void builtinGeneratorsPreserveExistingFormulasAndBuildOneSharedContext() throws IOException {
        Map<String, JsonObject> parts = new HashMap<>();
        try (var files = Files.list(Path.of("src/main/resources/data/skada/generator_data/weapon/part"))) {
            for (Path file : files.filter(file -> file.toString().endsWith(".json")).toList()) {
                String id = "skada:" + file.getFileName().toString().replace(".json", "");
                parts.put(id, GenJsonLoader.normalizeWeaponPartDefinitionJson(JsonParser.parseString(Files.readString(file)).getAsJsonObject()));
            }
        }
        for (String name : List.of("sword", "mace", "pickaxe", "trident")) {
            Path path = Path.of("src/main/resources/data/skada/generator_data/weapon/weapon_assembly/" + name + ".json");
            JsonObject resolved = GenJsonLoader.resolveWeaponAssemblyPartReferences(
                    JsonParser.parseString(Files.readString(path)).getAsJsonObject(), "skada", parts);
            var decoded = WeaponAssembly.CODEC.parse(JsonOps.INSTANCE, resolved);
            WeaponAssembly template = decoded.result().orElseThrow(() -> new AssertionError(name + ": " + decoded.error()));
            for (MaterialInfo material : List.of(material(1), new MaterialInfo(0.71, 1.5, 4, 6, spread(1)),
                    new MaterialInfo(12, 9, 3, 1, spread(2)))) {
                WeaponAssembly assembly = template.withMaterialWoodenHandle(material);
                for (AttackType type : assembly.getAttackTypes().keySet()) {
                    AttackTypeGeneratorConfiguration.GeneratedStats expected = switch (type.name()) {
                        case "slash" -> new AttackTypeGeneratorConfiguration.GeneratedStats(
                                PrecisionGenerationUtil.slash(assembly), LethalityGenerationUtil.slash(assembly),
                                CriticalFailGenerationUtil.slash(assembly), AttackSpeedGenerationUtil.slash(assembly));
                        case "thrust" -> new AttackTypeGeneratorConfiguration.GeneratedStats(
                                PrecisionGenerationUtil.thrust(assembly), LethalityGenerationUtil.thrust(assembly),
                                CriticalFailGenerationUtil.thrust(assembly), AttackSpeedGenerationUtil.thrust(assembly));
                        case "strike" -> new AttackTypeGeneratorConfiguration.GeneratedStats(
                                PrecisionGenerationUtil.strike(assembly), LethalityGenerationUtil.strike(assembly),
                                CriticalFailGenerationUtil.strike(assembly), AttackSpeedGenerationUtil.strike(assembly));
                        default -> throw new AssertionError("Unexpected built-in attack: " + type.name());
                    };
                    try (MockedStatic<AttackGenerationContextFactory> contexts = mockStatic(AttackGenerationContextFactory.class, CALLS_REAL_METHODS)) {
                        assertEquals(expected, type.tierStatFunction().generateAll(assembly), name + "/" + type.name());
                        KnockbackGenerationUtil.Estimate estimate = KnockbackGenerationUtil.estimate(assembly, type);
                        assertTrue(Double.isFinite(estimate.effectiveMassKg()), name + "/" + type.name());
                        assertTrue(Double.isFinite(estimate.contactSpeedMps()), name + "/" + type.name());
                        assertTrue(estimate.transferredImpulseNs() >= 0.0 && estimate.transferredImpulseNs() <= KnockbackGenerationUtil.MAX_ESTIMATED_IMPULSE_NS);
                        assertTrue(estimate.attackKnockback() >= 0.0 && estimate.attackKnockback() <= KnockbackGenerationUtil.MAX_ATTACK_KNOCKBACK);
                        assertTrue(estimate.attackKnockback() > 0.0, name + "/" + type.name());
                        WeaponInfo generated = new WeaponGenerator(Map.of("example.iron", material), Map.of(name, template),
                                Map.of(), false, 1.0)
                                .generate("example", List.of(item("iron_" + name, 8.0)), null)
                                .weapons().get("iron_" + name);
                        assertEquals(estimate.attackKnockback(), generated.getAttackTypes().get(type).knockback(), EPS,
                                name + "/" + type.name() + " preview uses the same source estimate");
                        switch (type.name()) {
                            case "slash" -> contexts.verify(() -> AttackGenerationContextFactory.buildSlashContext(assembly), times(1));
                            case "thrust" -> contexts.verify(() -> AttackGenerationContextFactory.buildThrustContext(assembly), times(1));
                            case "strike" -> contexts.verify(() -> AttackGenerationContextFactory.buildStrikeContext(assembly), times(1));
                        }
                    }
                }
            }
        }
    }

    /** Builds an assembly whose distinct attack offsets and modifiers expose policy regressions. */
    private static WeaponAssembly assembly(double primaryDamageOffset) {
        return new WeaponAssembly(List.of(), Map.of(
                primary, new AttackTypeJsonInfo(0.25, 2.75, Double.NaN, 0.5, 2, primaryDamageOffset, 2, List.of("test_reticle")),
                secondary, new AttackTypeJsonInfo(0.5, 3, 1.8755, 1, 1, -0.75, 1, List.of())));
    }

    /** Supplies an iron definition while allowing other tiers to exercise the default fallback. */
    private static WeaponGenerator generator(Map<String, WeaponAssembly> assemblies, Map<String, WeaponInfo> presets,
                                             boolean squeeze, double fraction) {
        return new WeaponGenerator(Map.of("example.iron", material(2)), assemblies, presets, squeeze, fraction);
    }

    /** Builds material with an observable element budget without requiring registered game attributes. */
    private static MaterialInfo material(double budget) {
        return new MaterialInfo(7.78, 5, 7, 4.5, spread(budget));
    }

    /** Creates a physical element spread using the suite's registry data. */
    private static ElementSpread spread(double budget) {
        return new ElementSpread(budget, new HashMap<>(Map.of(physical, budget)));
    }

    /** Creates a tiered melee weapon input. */
    private static WeaponGenerator.ItemInput item(String path, double damage) {
        return input("example:" + path, "iron", false, true, damage, List.of());
    }

    /** Supplies item facts directly, leaving registry reading to the production item reader. */
    private static WeaponGenerator.ItemInput input(String id, String tier, boolean projectile,
                                                    boolean handAttributes, double damage, List<ResourceLocation> tags) {
        return new WeaponGenerator.ItemInput(id(id), tags, tier, projectile, handAttributes, 1.6, damage);
    }

    /** Creates identifiers used by the test registry and generation inputs. */
    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }

    /** Asserts final, publicly observable damage for an attack. */
    private static void assertDamage(WeaponGenerator.Result result, String path, AttackType attack, double expected) {
        assertEquals(expected, result.weapons().get(path).getAttackTypes().get(attack).damage(), EPS);
    }
}
