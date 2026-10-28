package com.cwjn.skada.generation;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.Skada;
import com.cwjn.skada.SkadaCommand;
import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.damage.ManualWeaponInfos;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.generation.armour.ArmourConstructionInfo;
import com.cwjn.skada.generation.armour.ArmourItemMapping;
import com.cwjn.skada.generation.armour.ArmourPieceInfo;
import com.cwjn.skada.generation.weapon.MaterialInfo;
import com.cwjn.skada.generation.weapon.WeaponAssembly;
import com.cwjn.skada.generation.weapon.WeaponGenerator;
import com.cwjn.skada.generation.weapon.parts.HandlePart;
import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** Opt-in evidence capture using real loaded resources and registries, without exporting runtime data. */
@GameTestHolder(Skada.MODID)
@PrefixGameTestTemplate(false)
public final class GenerationBaselineGameTests {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private GenerationBaselineGameTests() {}

    /** Writes a complete current baseline only when an explicit evidence-output directory is provided. */
    @GameTest(templateNamespace = Skada.MODID, template = "damage_handler_5x4x5", timeoutTicks = 1200)
    public static void captureVanillaEquipmentBaseline(GameTestHelper helper) throws Exception {
        String destination = System.getProperty("skada.baseline.output");
        if (destination == null || destination.isBlank()) {
            helper.succeed();
            return;
        }
        Path output = Path.of(destination).toAbsolutePath().normalize();
        Path project = output.resolve("../../../..").normalize();
        if (!output.equals(project.resolve(".scratch/initial-release/evidence/generation"))
                || !Files.isRegularFile(project.resolve("build.gradle"))) {
            throw new IllegalArgumentException("Baseline output must be the project's .scratch/initial-release/evidence/generation directory");
        }
        ResourceManager resources = helper.getLevel().getServer().getResourceManager();
        Map<String, MaterialInfo> materials = GenJsonLoader.loadMaterialInfo(resources, null);
        Map<String, WeaponAssembly> assemblies = GenJsonLoader.loadWeaponAssemblies(resources, null);
        Map<String, ArmourPieceInfo> pieces = GenJsonLoader.loadArmourPieceInfo(resources, null);
        Map<String, ArmourConstructionInfo> constructions = GenJsonLoader.loadArmourConstructionInfo(resources, null);
        Map<String, WeaponInfo> presets = ManualWeaponInfos.byNamespace("minecraft");
        boolean squeeze = CommonConfig.SQUEEZE_DAMAGE_VALUES.get();
        double retained = CommonConfig.DAMAGE_DIFFERENCE_AMOUNT.get();
        List<Item> items = ForgeRegistries.ITEMS.getValues().stream()
                .filter(item -> Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)).getNamespace().equals("minecraft"))
                .sorted(Comparator.comparing(item -> ForgeRegistries.ITEMS.getKey(item).toString())).toList();
        List<WeaponGenerator.ItemInput> inputs = items.stream().map(WeaponGenerator.ItemInput::fromItem).toList();
        WeaponGenerator generator = new WeaponGenerator(materials, assemblies, presets, squeeze, retained);
        WeaponGenerator.Result result = generator.generate("minecraft", inputs, null);
        JsonElement generatedWeapons = encode(WeaponInfo.STRING_MAP_CODEC, result.weapons());
        List<WeaponGenerator.ItemInput> reversed = new ArrayList<>(inputs);
        Collections.reverse(reversed);

        JsonObject root = new JsonObject();
        root.addProperty("capturedUtc", Instant.now().toString());
        root.addProperty("javaVersion", System.getProperty("java.version"));
        root.addProperty("workingDirectory", Path.of("").toAbsolutePath().toString());
        root.addProperty("squeezeDamageValues", squeeze);
        root.addProperty("retainedDamageDifference", retained);
        root.addProperty("lengthUnits", "geometry cm; reach blocks/metres");
        root.addProperty("massUnits", "grams");
        root.addProperty("densityUnits", "g/cm^3");
        root.addProperty("attackSpeedUnits", "attacks/second");
        root.addProperty("failChanceUnits", "fraction");
        root.add("materials", encodeMap(MaterialInfo.CODEC, materials));
        root.add("assemblies", encodeMap(WeaponAssembly.CODEC, assemblies));
        root.add("armourPieces", encodeMap(ArmourPieceInfo.CODEC, pieces));
        root.add("armourConstructions", encodeMap(ArmourConstructionInfo.CODEC, constructions));
        root.add("generatedWeapons", generatedWeapons);
        root.add("shippedWeaponsRaw", readResource(resources, "weapon_info/minecraft.json"));
        root.add("shippedWeaponsDecoded", encode(WeaponInfo.STRING_MAP_CODEC,
                decode(WeaponInfo.STRING_MAP_CODEC, root.get("shippedWeaponsRaw"))));
        root.add("manualPresets", encode(WeaponInfo.STRING_MAP_CODEC, presets));
        root.addProperty("repeatWeaponGenerationEqual", generatedWeapons.equals(encode(WeaponInfo.STRING_MAP_CODEC,
                generator.generate("minecraft", inputs, null).weapons())));
        root.addProperty("reversedItemOrderEqual", generatedWeapons.equals(encode(WeaponInfo.STRING_MAP_CODEC,
                generator.generate("minecraft", reversed, null).weapons())));
        JsonArray individualMismatches = new JsonArray();
        JsonObject provenance = new JsonObject();
        for (WeaponGenerator.ItemInput input : inputs) {
            String name = input.id().getPath();
            if (!result.weapons().containsKey(name)) continue;
            JsonElement single = encode(WeaponInfo.STRING_MAP_CODEC, generator.generate("minecraft", inputs, name).weapons());
            if (!single.getAsJsonObject().get(name).equals(generatedWeapons.getAsJsonObject().get(name))) individualMismatches.add(name);
            JsonObject row = JSON.toJsonTree(input).getAsJsonObject();
            boolean manual = presets.containsKey(name);
            row.addProperty("manualPreset", manual);
            WeaponAssembly assembly = manual ? null : generator.resolveAssembly(input);
            MaterialInfo material = manual ? null : generator.resolveMaterial(input);
            row.addProperty("resolvedAssembly", keyForIdentity(assemblies, assembly));
            row.addProperty("resolvedMaterial", keyForIdentity(materials, material));
            row.addProperty("materialFallback", material == null && !manual);
            if (!manual) row.add("usedMaterial", encode(MaterialInfo.CODEC, material == null ? MaterialInfo.getDefault() : material));
            if (result.diagnostics().containsKey(input.id())) row.addProperty("diagnostic", result.diagnostics().get(input.id()));
            provenance.add(name, row);
        }
        root.add("weaponProvenance", provenance);
        root.add("individualWeaponMismatches", individualMismatches);
        root.add("weaponDiagnostics", JSON.toJsonTree(result.diagnostics()));

        SkadaCommand commands = new SkadaCommand(new CommandDispatcher<CommandSourceStack>(),
                CommandBuildContext.simple(helper.getLevel().registryAccess(), FeatureFlags.DEFAULT_FLAGS));
        JsonObject armour = captureArmour(commands, items, pieces, constructions, materials);
        root.add("generatedArmour", armour.get("outputs"));
        root.add("armourProvenance", armour.get("provenance"));
        root.add("armourSkipped", armour.get("skipped"));
        root.addProperty("repeatArmourGenerationEqual", armour.equals(captureArmour(commands, items, pieces, constructions, materials)));
        List<Item> reversedItems = new ArrayList<>(items);
        Collections.reverse(reversedItems);
        root.addProperty("reversedArmourItemOrderEqual", armour.equals(captureArmour(commands, reversedItems, pieces, constructions, materials)));
        JsonArray individualArmourMismatches = new JsonArray();
        for (Item item : items) {
            String name = ForgeRegistries.ITEMS.getKey(item).getPath();
            if (!armour.getAsJsonObject("outputs").has(name)) continue;
            JsonObject single = captureArmour(commands, List.of(item), pieces, constructions, materials);
            if (!single.getAsJsonObject("outputs").get(name).equals(armour.getAsJsonObject("outputs").get(name)))
                individualArmourMismatches.add(name);
        }
        root.add("individualArmourMismatches", individualArmourMismatches);
        root.add("shippedArmourRaw", readResource(resources, "armour_info/minecraft.json"));
        root.add("shippedArmourDecoded", encode(ArmourInfo.STRING_MAP_CODEC,
                decode(ArmourInfo.STRING_MAP_CODEC, root.get("shippedArmourRaw"))));
        root.add("ambiguityProbes", captureAmbiguities(materials, assemblies, inputs));
        JsonArray handles = new JsonArray();
        for (var entry : assemblies.get("sword").withMaterialWoodenHandle(materials.get("minecraft.iron")).parts()) {
            if (entry.part() instanceof HandlePart) handles.add(encode(MaterialInfo.CODEC, entry.material()));
        }
        root.add("actualSwordHandleMaterials", handles);
        root.add("sourceSha256", sourceHashes(project));
        assertFinite(root);
        Files.createDirectories(output);
        Files.writeString(output.resolve("capture.json"), JSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
        helper.assertTrue(root.get("repeatWeaponGenerationEqual").getAsBoolean(), "Repeated weapon capture differs");
        helper.assertTrue(root.get("reversedItemOrderEqual").getAsBoolean(), "Reversed weapon inputs differ");
        helper.assertTrue(individualMismatches.isEmpty(), "Individual weapon output differs from namespace output");
        helper.assertTrue(root.get("repeatArmourGenerationEqual").getAsBoolean(), "Repeated armour capture differs");
        helper.assertTrue(root.get("reversedArmourItemOrderEqual").getAsBoolean(), "Reversed armour inputs differ");
        helper.assertTrue(individualArmourMismatches.isEmpty(), "Individual armour calculation differs from namespace output");
        helper.succeed();
    }

    /** Mirrors command enumeration while calling the command's actual private mapping/resolution methods. */
    private static JsonObject captureArmour(SkadaCommand commands, List<Item> items, Map<String, ArmourPieceInfo> pieces,
            Map<String, ArmourConstructionInfo> constructions, Map<String, MaterialInfo> materials) throws Exception {
        JsonObject outputs = new JsonObject();
        JsonObject provenance = new JsonObject();
        JsonObject skipped = new JsonObject();
        Method mappingMethod = method("resolveArmourItemMapping", Item.class, Map.class, Map.class, Map.class);
        Method materialMethod = method("resolveSharedMaterial", Map.class, String.class, String.class, Item.class);
        Method constructionMethod = method("resolveArmourConstruction", Map.class, String.class, String.class);
        for (Item item : items) {
            if (List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET).stream()
                    .allMatch(slot -> item.getDefaultInstance().getAttributeModifiers(slot).isEmpty())) continue;
            String name = ForgeRegistries.ITEMS.getKey(item).getPath();
            ArmourItemMapping mapping = (ArmourItemMapping) mappingMethod.invoke(commands, item, pieces, constructions, materials);
            if (mapping == null) { skipped.addProperty(name, "no mapping"); continue; }
            MaterialInfo material = (MaterialInfo) materialMethod.invoke(commands, materials, "minecraft", mapping.material(), item);
            ArmourConstructionInfo construction = (ArmourConstructionInfo) constructionMethod.invoke(commands, constructions, "minecraft", mapping.construction());
            if (material == null || construction == null) { skipped.addProperty(name, "unresolved material/construction"); continue; }
            double baseArmour = baseAttribute(item, Attributes.ARMOR);
            double baseToughness = baseAttribute(item, Attributes.ARMOR_TOUGHNESS);
            ArmourPieceInfo piece = mapping.resolvePiece(pieces);
            outputs.add(name, encode(ArmourInfo.CODEC, ArmourInfo.generate(piece, material, construction, baseArmour, baseToughness)));
            JsonObject row = new JsonObject();
            row.add("mapping", encode(ArmourItemMapping.CODEC, mapping));
            row.addProperty("materialKey", keyForIdentity(materials, material));
            row.addProperty("constructionKey", keyForIdentity(constructions, construction));
            row.addProperty("baseArmour", baseArmour);
            row.addProperty("baseToughness", baseToughness);
            row.add("resolvedPiece", encode(ArmourPieceInfo.CODEC, piece));
            provenance.add(name, row);
        }
        JsonObject result = new JsonObject();
        result.add("outputs", outputs);
        result.add("provenance", provenance);
        result.add("skipped", skipped);
        return result;
    }

    /** Records deliberately ambiguous synthetic input orders, without choosing a new resolution policy. */
    private static JsonObject captureAmbiguities(Map<String, MaterialInfo> materials, Map<String, WeaponAssembly> assemblies,
            List<WeaponGenerator.ItemInput> inputs) {
        JsonObject probes = new JsonObject();
        MaterialInfo iron = materials.get("minecraft.iron"), diamond = materials.get("minecraft.diamond");
        Map<String, MaterialInfo> forward = new LinkedHashMap<>();
        forward.put("a.iron", iron); forward.put("b.iron", diamond);
        Map<String, MaterialInfo> reverse = new LinkedHashMap<>();
        reverse.put("b.iron", diamond); reverse.put("a.iron", iron);
        WeaponGenerator.ItemInput input = new WeaponGenerator.ItemInput(new ResourceLocation("example", "sword"),
                List.of(), "iron", false, true, 1.6, 5);
        probes.add("suffixForward", encode(MaterialInfo.CODEC, new WeaponGenerator(forward, assemblies, Map.of(), false, 1).resolveMaterial(input)));
        probes.add("suffixReverse", encode(MaterialInfo.CODEC, new WeaponGenerator(reverse, assemblies, Map.of(), false, 1).resolveMaterial(input)));
        Map<String, WeaponAssembly> ambiguous = new LinkedHashMap<>();
        ambiguous.put("one", assemblies.get("sword")); ambiguous.put("two", assemblies.get("axe"));
        WeaponGenerator tags = new WeaponGenerator(materials, ambiguous, Map.of(), false, 1);
        List<ResourceLocation> tagOrder = List.of(new ResourceLocation("skada", "generator/weapon_assembly/one"),
                new ResourceLocation("skada", "generator/weapon_assembly/two"));
        WeaponGenerator.ItemInput first = new WeaponGenerator.ItemInput(input.id(), tagOrder, "iron", false, true, 1.6, 5);
        WeaponGenerator.ItemInput second = new WeaponGenerator.ItemInput(input.id(), List.of(tagOrder.get(1), tagOrder.get(0)), "iron", false, true, 1.6, 5);
        probes.addProperty("equalLengthTagsForward", keyForIdentity(ambiguous, tags.resolveAssembly(first)));
        probes.addProperty("equalLengthTagsReverse", keyForIdentity(ambiguous, tags.resolveAssembly(second)));
        probes.addProperty("fixtureScope", "Synthetic ambiguity probes; ordinary vanilla input reversal is checked separately");
        return probes;
    }

    /** Resolves a command method for evidence capture without modifying production visibility. */
    private static Method method(String name, Class<?>... parameters) throws NoSuchMethodException {
        Method method = SkadaCommand.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    /** Reads the same base armour attributes used by the current generation commands. */
    private static double baseAttribute(Item item, Attribute attribute) {
        return item.getDefaultAttributeModifiers(LivingEntity.getEquipmentSlotForItem(item.getDefaultInstance())).get(attribute)
                .stream().filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                .mapToDouble(AttributeModifier::getAmount).sum();
    }

    /** Finds the loaded source key for a resolved object, preserving identity rather than guessing from equal values. */
    private static <T> String keyForIdentity(Map<String, T> map, T value) {
        return map.entrySet().stream().filter(entry -> entry.getValue() == value).map(Map.Entry::getKey).sorted().findFirst().orElse(null);
    }

    /** Encodes evidence through the same codec used for exports. */
    private static <T> JsonElement encode(Codec<T> codec, T value) {
        return codec.encodeStart(JsonOps.INSTANCE, value).result().orElseThrow(() -> new IllegalStateException("Evidence encoding failed"));
    }

    /** Decodes shipped data to distinguish semantic differences from legacy field spellings. */
    private static <T> T decode(Codec<T> codec, JsonElement value) {
        return codec.parse(JsonOps.INSTANCE, value).result().orElseThrow(() -> new IllegalStateException("Shipped evidence decoding failed"));
    }

    /** Orders named input definitions for stable evidence serialization. */
    private static <T> JsonObject encodeMap(Codec<T> codec, Map<String, T> map) {
        JsonObject json = new JsonObject();
        new TreeMap<>(map).forEach((name, value) -> json.add(name, encode(codec, value)));
        return json;
    }

    /** Reads the actual resource-manager winner for a bundled runtime definition. */
    private static JsonElement readResource(ResourceManager resources, String path) throws IOException {
        try (var reader = resources.getResource(new ResourceLocation("skada", path)).orElseThrow().openAsReader()) {
            return JsonParser.parseReader(reader);
        }
    }

    /** Hashes all production sources/resources and this harness so the baseline identifies its exact inputs. */
    private static JsonObject sourceHashes(Path project) throws Exception {
        JsonObject hashes = new JsonObject();
        for (String directory : List.of("src/main/java", "src/main/resources", "src/generated/resources")) {
            Path root = project.resolve(directory);
            if (!Files.isDirectory(root)) continue;
            try (var files = Files.walk(root)) {
                for (Path path : files.filter(Files::isRegularFile).sorted().toList()) {
                    hashes.addProperty(project.relativize(path).toString().replace('\\', '/'),
                            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
                }
            }
        }
        for (String relative : List.of("build.gradle", "gradle.properties", "src/gametest/java/com/cwjn/skada/generation/GenerationBaselineGameTests.java")) {
            hashes.addProperty(relative, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(project.resolve(relative)))));
        }
        return hashes;
    }

    /** Rejects nonfinite numeric evidence before writing a success result. */
    private static void assertFinite(JsonElement value) {
        if (value.isJsonObject()) value.getAsJsonObject().entrySet().forEach(entry -> assertFinite(entry.getValue()));
        else if (value.isJsonArray()) value.getAsJsonArray().forEach(GenerationBaselineGameTests::assertFinite);
        else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() && !Double.isFinite(value.getAsDouble()))
            throw new IllegalStateException("Nonfinite generation output: " + value);
    }
}
