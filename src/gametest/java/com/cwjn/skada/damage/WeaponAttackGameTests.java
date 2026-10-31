package com.cwjn.skada.damage;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.*;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.mob.MobData;
import com.cwjn.skada.util.UtilData;
import com.google.common.collect.ArrayListMultimap;
import com.cwjn.skada.generation.attack.ElementSpread;
import com.cwjn.skada.network.client_to_server.C2SSelectAttackType;
import com.cwjn.skada.network.client_to_server.C2SSelectAttackTypeFromMenu;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.*;

/** Exercises selection, item attributes, wire data and actual vanilla launch adapters with mixins active. */
@GameTestHolder(Skada.MODID)
@PrefixGameTestTemplate(false)
public final class WeaponAttackGameTests {
    private static final String TEMPLATE = "damage_handler_5x4x5";
    private WeaponAttackGameTests() {}

    /** Blocks share the fist footprint while retaining generic-item combat stats and override precedence. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void heldBlocksUseSquareFallbackWithoutChangingTheirStats(GameTestHelper helper) {
        AttackSelection fist = AttackSelection.melee(ItemStack.EMPTY);
        ItemStack block = new ItemStack(Items.COBBLESTONE);
        AttackSelection selected = AttackSelection.melee(block);
        AttackTypeInfo generic = AttackTypeInfo.ITEM;
        helper.assertTrue(!selected.configured() && !block.hasTag(), "A block fallback must not create authored stack data");
        helper.assertTrue(selected.stats().reticleShapes().equals(fist.stats().reticleShapes()), "Block rendering and targeting must resolve the fist's square geometry");
        helper.assertTrue(selected.weapon().getAttackTypes().get(AttackType.strike()).equals(selected.stats()), "Fallback profile and selected stats must agree");
        helper.assertTrue(selected.stats().lethality() == generic.lethality()
                && selected.stats().precision() == generic.precision()
                && selected.stats().minReach() == generic.minReach()
                && selected.stats().maxReach() == generic.maxReach()
                && selected.stats().attackSpeed() == generic.attackSpeed()
                && selected.stats().damage() == generic.damage()
                && selected.stats().failChance() == generic.failChance(), "A geometry change must preserve every generic-item combat stat");
        helper.assertTrue(AttackSelection.melee(new ItemStack(Items.APPLE)).stats().reticleShapes().equals(generic.reticleShapes()), "Miscellaneous items must remain circular");
        WeaponInfo authored = new WeaponInfo(Map.of(AttackType.strike(), generic), new ElementSpread(), WeaponStatApplicability.MELEE);
        block.getOrCreateTag().put(SkadaData.WEAPON_INFO_TAG_KEY, authored.toCompoundTag());
        AttackSelection overridden = AttackSelection.melee(block);
        helper.assertTrue(overridden.configured() && overridden.stats().reticleShapes().equals(generic.reticleShapes()), "An explicitly authored block profile must keep its own shape");
        ItemStack projectileOnly = weapon(Items.DIRT, WeaponStatApplicability.PROJECTILE);
        helper.assertTrue(AttackSelection.melee(projectileOnly).stats().reticleShapes().equals(fist.stats().reticleShapes()), "A projectile-only block must still use the square direct-attack fallback");
        helper.succeed();
    }

    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void selectionRepairsOrderAndMigratesLegacyIndex(GameTestHelper helper) {
        ItemStack stack = weapon(Items.STICK, WeaponStatApplicability.BOTH);
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(SkadaData.ATTACK_TYPES_ARRAY_TAG_KEY, order("skada:thrust", "bad id", "missing:attack", "skada:thrust"));
        tag.putInt(SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY, 99);
        AttackSelection selected = AttackSelection.resolve(stack);
        helper.assertTrue(selected.order().equals(List.of(AttackType.thrust(), AttackType.slash())), "Repair must retain valid order and append missing types");
        helper.assertTrue(selected.type() == AttackType.slash() && selected.index() == 1, "Legacy index must clamp to the repaired order");
        helper.assertTrue(!tag.contains(SkadaData.CURRENT_ATTACK_TYPE_TAG_KEY) && !tag.contains(SkadaData.NUM_ATTACK_TYPES_TAG_KEY), "Index and count must be derived after migration");
        tag.put(SkadaData.ATTACK_TYPES_ARRAY_TAG_KEY, order("skada:slash", "skada:thrust"));
        selected = AttackSelection.resolve(stack);
        helper.assertTrue(selected.type() == AttackType.slash() && selected.index() == 0, "A valid ID must survive reordered attacks");
        CompoundTag stable = tag.copy();
        AttackSelection.resolve(stack);
        helper.assertTrue(stable.equals(tag), "Repeated resolution must leave canonical NBT unchanged");
        ResourceLocation next = selected.cycle(stack);
        helper.assertTrue(next.equals(AttackType.thrust().rl()) && AttackSelection.resolve(stack).type() == AttackType.thrust(), "Cycling must select the next ordered ID");
        AttackSelection.resolve(stack).cycle(stack);
        helper.assertTrue(AttackSelection.resolve(stack).type() == AttackType.slash(), "Cycling must wrap");
        AttackSelection.resolve(stack).cycle(stack, -1);
        helper.assertTrue(AttackSelection.resolve(stack).type() == AttackType.thrust(), "Reverse cycling must wrap to the final attack");
        AttackSelection.resolve(stack).cycle(stack, -1);
        helper.assertTrue(AttackSelection.resolve(stack).type() == AttackType.slash(), "Reverse cycling must follow the saved order");
        helper.assertTrue(!AttackSelection.select(stack, AttackType.strike().rl()), "Unsupported client selections must be rejected");
        helper.succeed();
    }

    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void lazyDefaultsPreserveStackOverrides(GameTestHelper helper) {
        AccessWeaponInfo item = (AccessWeaponInfo) Items.STICK;
        WeaponInfo previous = item.skada$getWeaponInfo();
        try {
            item.skada$setWeaponInfo(profile(WeaponStatApplicability.PROJECTILE));
            ItemStack stack = new ItemStack(Items.STICK);
            AttackSelection selected = AttackSelection.resolve(stack);
            helper.assertTrue(selected.configured() && stack.getTagElement(SkadaData.WEAPON_INFO_TAG_KEY) != null, "First access must copy item defaults");
            item.skada$setWeaponInfo(profile(WeaponStatApplicability.MELEE));
            helper.assertTrue(AttackSelection.resolve(stack).weapon().applicability() == WeaponStatApplicability.PROJECTILE, "Existing stack data must retain precedence after default changes");
            CompoundTag copied = stack.copy().getTag();
            helper.assertTrue(copied.equals(stack.getTag()), "Stack copies must retain canonical selection");
        } finally {
            item.skada$setWeaponInfo(previous);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void applicabilityControlsMeleeAttributesAndMetadata(GameTestHelper helper) {
        ItemStack launcher = weapon(Items.STICK, WeaponStatApplicability.PROJECTILE);
        helper.assertTrue(AttackSelection.melee(launcher).weapon() == WeaponInfo.RANDOM_ITEM, "Projectile-only profiles must not leak into direct attacks");
        helper.assertTrue(launcher.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).isEmpty(), "Projectile-only damage must not become a melee attribute");
        helper.assertTrue(ProjectileLaunch.resolve(launcher, AttackType.strike(), 0, ProjectileLaunch.Flight.CROSSBOW).stats() != null, "Launcher profile must still apply to projectiles");
        ItemStack hybrid = weapon(Items.STICK, WeaponStatApplicability.BOTH);
        helper.assertTrue(AttackSelection.melee(hybrid).configured(), "Hybrid profiles must supply melee metadata");
        helper.assertTrue(hybrid.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().anyMatch(modifier -> modifier.getAmount() == 2.0), "Hybrid profiles must apply their configured melee damage attribute");
        ItemStack melee = weapon(Items.STICK, WeaponStatApplicability.MELEE);
        helper.assertTrue(AttackSelection.melee(melee).configured(), "Melee profiles must apply to direct attacks");
        helper.assertTrue(ProjectileLaunch.resolve(melee, AttackType.strike(), 7, ProjectileLaunch.Flight.BOW).stats() == null, "Melee-only profiles must use projectile fallback");
        Zombie mob = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 1);
        DamageInfo intrinsic = new DamageInfo(3, 5, false, AttackType.strike(), new ElementSpread().instance());
        MobData mobData = new MobData(null, intrinsic, ArrayListMultimap.create());
        mob.setItemInHand(InteractionHand.MAIN_HAND, launcher);
        helper.assertTrue(UtilData.getMobDirectAttackDamageInfo(mob, mobData) == intrinsic, "A mob holding a launcher must use its intrinsic melee fallback");
        mob.setItemInHand(InteractionHand.MAIN_HAND, hybrid);
        helper.assertTrue(UtilData.getMobDirectAttackDamageInfo(mob, mobData).attackType() == AttackType.slash(), "Hybrid melee metadata must take precedence for mobs");
        helper.succeed();
    }

    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void legacyAndExplicitProfilesRoundTrip(GameTestHelper helper) {
        for (boolean ignore : List.of(false, true)) {
            WeaponInfo original = profile(WeaponStatApplicability.MELEE);
            CompoundTag legacy = original.toCompoundTag();
            helper.assertTrue(!legacy.contains("ignore_attributes"), "New NBT must omit the retired setting");
            legacy.remove("applicability");
            legacy.putBoolean("ignore_attributes", ignore);
            helper.assertTrue(WeaponInfo.fromCompoundTag(legacy).applicability() == WeaponStatApplicability.legacy(ignore), "Legacy NBT must preserve its former scope");
            var json = WeaponInfo.CODEC.encodeStart(JsonOps.INSTANCE, original).result().orElseThrow().getAsJsonObject();
            helper.assertTrue(!json.has("ignore_attributes"), "New JSON must omit the retired setting");
            json.remove("applicability");
            json.addProperty("ignore_attributes", ignore);
            helper.assertTrue(WeaponInfo.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow().applicability() == WeaponStatApplicability.legacy(ignore), "Legacy JSON must preserve its former scope");
            legacy.putString("applicability", "both");
            WeaponInfo explicit = WeaponInfo.fromCompoundTag(legacy);
            helper.assertTrue(explicit.applicability() == WeaponStatApplicability.BOTH && !explicit.toCompoundTag().contains("ignore_attributes"), "Explicit NBT scope must win and re-encoding must discard the retired flag");
            json.addProperty("applicability", "both");
            helper.assertTrue(WeaponInfo.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow().applicability() == WeaponStatApplicability.BOTH, "Explicit scope must override legacy inference");
            json.addProperty("applicability", "invalid");
            helper.assertTrue(WeaponInfo.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(), "Invalid authored applicability must be rejected");
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (InteractionHand hand : InteractionHand.values()) {
                C2SSelectAttackType expected = new C2SSelectAttackType(AttackType.thrust().rl(), hand, 7);
                C2SSelectAttackType.encode(expected, buffer);
                helper.assertTrue(expected.equals(C2SSelectAttackType.decode(buffer)), "Held packet must retain the attack ID, hand and hotbar slot");
            }
            C2SSelectAttackTypeFromMenu menu = new C2SSelectAttackTypeFromMenu(AttackType.slash().rl(), 12, 4);
            C2SSelectAttackTypeFromMenu.encode(menu, buffer);
            helper.assertTrue(menu.equals(C2SSelectAttackTypeFromMenu.decode(buffer)), "Menu packet must retain ID and menu context");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void projectileSnapshotPersistsAndFlightAppliesOnlyOnce(GameTestHelper helper) {
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 1);
        ItemStack source = weapon(Items.BOW, WeaponStatApplicability.PROJECTILE);
        Arrow arrow = new Arrow(helper.getLevel(), shooter);
        ProjectileLaunch launch = ProjectileLaunch.resolve(source, AttackType.thrust(), 0, ProjectileLaunch.Flight.BOW);
        launch.attach(arrow, source, shooter);
        helper.assertTrue(Math.abs(launch.velocity(1.5f) - 2.5f) < 0.001, "Half charge must scale base and bonus velocity together");
        AttackSelection.select(source, AttackType.thrust().rl());
        arrow.shoot(0, 0, 1, 1.5f, 0);
        AccessProjectileData data = (AccessProjectileData) arrow;
        helper.assertTrue(data.getPendingLaunch() == null, "First flight must consume transient launch adjustments");
        helper.assertTrue(data.getDamageInfo().attackType() == AttackType.slash(), "Weapon changes must not alter an already captured attack");
        arrow.shoot(0, 0, 1, 1, 0);
        helper.assertTrue(Math.abs(arrow.getDeltaMovement().length() - 1) < 0.001, "Later shoots must not reapply damage bonuses");
        CompoundTag saved = arrow.saveWithoutId(new CompoundTag());
        Arrow loaded = new Arrow(EntityType.ARROW, helper.getLevel());
        loaded.load(saved);
        helper.assertTrue(((AccessProjectileData) loaded).getDamageInfo().toCompoundTag().equals(data.getDamageInfo().toCompoundTag()), "Persistent snapshot must include configured projectile damage");
        helper.assertTrue(((AccessProjectileData) loaded).getPendingLaunch() == null, "Loading must not reapply flight adjustments");
        ProjectileLaunch.resolve(source, AttackType.thrust(), 0, ProjectileLaunch.Flight.BOW).attach(loaded, source, shooter);
        helper.assertTrue(((AccessProjectileData) loaded).getDamageInfo().attackType() == AttackType.slash(), "Adapters must preserve an existing snapshot");
        Arrow unknown = new Arrow(helper.getLevel(), shooter);
        unknown.shoot(0, 0, 1, 1, 0);
        helper.assertTrue(!((AccessProjectileData) unknown).hasDamageInfo(), "Unknown projectiles must retain their existing damage fallback");
        ItemStack meleeOnly = weapon(Items.TRIDENT, WeaponStatApplicability.MELEE);
        ThrownTrident fallbackTrident = new ThrownTrident(helper.getLevel(), shooter, meleeOnly);
        ProjectileLaunch.resolve(meleeOnly, AttackType.thrust(), 0, ProjectileLaunch.Flight.UNCHANGED)
                .attach(fallbackTrident, meleeOnly, shooter);
        helper.assertTrue(Objects.equals(((AccessProjectileData) fallbackTrident).getDamageInfo().projectileDamage(), 8.0),
                "New fallback tridents must capture vanilla damage rather than consult a later item profile");
        helper.succeed();
    }

    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void bowCapturesOffhandSourceInsteadOfMainhand(GameTestHelper helper) {
        Player shooter = mockPlayer(helper);
        ItemStack bow = weapon(Items.BOW, WeaponStatApplicability.PROJECTILE);
        shooter.setItemInHand(InteractionHand.OFF_HAND, bow);
        shooter.setItemInHand(InteractionHand.MAIN_HAND, weapon(Items.STICK, WeaponStatApplicability.MELEE));
        shooter.getInventory().add(new ItemStack(Items.ARROW, 8));
        ((BowItem) Items.BOW).releaseUsing(bow, helper.getLevel(), shooter, bow.getUseDuration() - 20);
        assertLaunches(helper, shooter, 1);
        helper.succeed();
    }

    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void playerCrossbowMultishotAndRocketsShareLaunchProfile(GameTestHelper helper) {
        Player shooter = mockPlayer(helper);
        ItemStack crossbow = weapon(Items.CROSSBOW, WeaponStatApplicability.PROJECTILE);
        ListTag ammo = new ListTag();
        for (Item item : List.of(Items.ARROW, Items.ARROW, Items.FIREWORK_ROCKET)) ammo.add(new ItemStack(item).save(new CompoundTag()));
        crossbow.getOrCreateTag().put("ChargedProjectiles", ammo);
        shooter.setItemInHand(InteractionHand.OFF_HAND, crossbow);
        CrossbowItem.performShooting(helper.getLevel(), shooter, InteractionHand.OFF_HAND, crossbow, 3.15f, 0);
        assertLaunches(helper, shooter, 3);
        helper.succeed();
    }

    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void mobCrossbowUsesSharedFlightWithoutLosingAiming(GameTestHelper helper) {
        Pillager shooter = helper.spawnWithNoFreeWill(EntityType.PILLAGER, 1, 1, 1);
        shooter.setTarget(helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 3, 1, 3));
        ItemStack crossbow = weapon(Items.CROSSBOW, WeaponStatApplicability.PROJECTILE);
        ListTag ammo = new ListTag();
        ammo.add(new ItemStack(Items.ARROW).save(new CompoundTag()));
        crossbow.getOrCreateTag().put("ChargedProjectiles", ammo);
        shooter.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
        CrossbowItem.performShooting(helper.getLevel(), shooter, InteractionHand.MAIN_HAND, crossbow, 1.6f, 0);
        List<Projectile> projectiles = assertLaunches(helper, shooter, 1);
        helper.assertTrue(projectiles.get(0).getDeltaMovement().length() > 3.2, "Mob crossbow must receive the damage velocity bonus too");
        helper.succeed();
    }

    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void thrownTridentAndDrownedCaptureTheirActualWeapon(GameTestHelper helper) {
        Player player = mockPlayer(helper);
        ItemStack trident = weapon(Items.TRIDENT, WeaponStatApplicability.BOTH);
        player.setItemInHand(InteractionHand.MAIN_HAND, trident);
        ((TridentItem) Items.TRIDENT).releaseUsing(trident, helper.getLevel(), player, trident.getUseDuration() - 20);
        assertLaunches(helper, player, 1);
        Drowned drowned = helper.spawnWithNoFreeWill(EntityType.DROWNED, 1, 1, 1);
        drowned.setItemInHand(InteractionHand.MAIN_HAND, weapon(Items.TRIDENT, WeaponStatApplicability.BOTH));
        drowned.performRangedAttack(helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 3, 1, 3), 1);
        assertLaunches(helper, drowned, 1);
        helper.succeed();
    }

    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void mobBowCapturesLauncherInsteadOfAmmo(GameTestHelper helper) {
        Skeleton shooter = helper.spawnWithNoFreeWill(EntityType.SKELETON, 1, 1, 1);
        shooter.setItemInHand(InteractionHand.MAIN_HAND, weapon(Items.BOW, WeaponStatApplicability.PROJECTILE));
        shooter.performRangedAttack(helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 3, 1, 3), 1);
        assertLaunches(helper, shooter, 1);
        helper.succeed();
    }

    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void itemlessLlamaRetainsExplicitFallback(GameTestHelper helper) {
        Llama shooter = helper.spawnWithNoFreeWill(EntityType.LLAMA, 1, 1, 1);
        shooter.performRangedAttack(helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 3, 1, 3), 1);
        List<Projectile> projectiles = helper.getLevel().getEntitiesOfClass(Projectile.class,
                new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(30), projectile -> projectile.getOwner() == shooter);
        helper.assertTrue(projectiles.size() == 1, "Expected one llama spit projectile");
        AccessProjectileData data = (AccessProjectileData) projectiles.get(0);
        helper.assertTrue(data.hasDamageInfo() && data.getDamageInfo().attackType() == AttackType.thrust()
                && data.getDamageInfo().projectileDamage() == null, "Llama spit must retain the itemless fallback");
        helper.assertTrue(data.getPendingLaunch() == null, "Llama flight must consume its pending context");
        helper.succeed();
    }

    /** Supplies two intentionally distinct attack profiles so mixing selections is observable. */
    private static WeaponInfo profile(WeaponStatApplicability applicability) {
        return new WeaponInfo(Map.of(
                AttackType.slash(), new AttackTypeInfo(12, 1000, 0, 4, 1.6, 2, 0, List.of()),
                AttackType.thrust(), new AttackTypeInfo(24, 2000, 0, 5, 1.2, 4, 0, List.of())),
                new ElementSpread(), applicability);
    }

    /** Creates explicit stack data so tests do not depend on installed weapon resources. */
    private static ItemStack weapon(Item item, WeaponStatApplicability applicability) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().put(SkadaData.WEAPON_INFO_TAG_KEY, profile(applicability).toCompoundTag());
        stack.getOrCreateTag().putString(SkadaData.CURRENT_ATTACK_TYPE_ID_TAG_KEY, AttackType.slash().rl().toString());
        return stack;
    }

    /** Places a mock player inside this test's search area. */
    private static Player mockPlayer(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        return player;
    }

    /** Checks real spawned entities, not just the shared snapshot constructor. */
    private static List<Projectile> assertLaunches(GameTestHelper helper, LivingEntity shooter, int count) {
        List<Projectile> projectiles = helper.getLevel().getEntitiesOfClass(Projectile.class,
                new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(30), projectile -> projectile.getOwner() == shooter);
        helper.assertTrue(projectiles.size() == count, "Expected " + count + " projectiles, got " + projectiles.size());
        for (Projectile projectile : projectiles) {
            AccessProjectileData data = (AccessProjectileData) projectile;
            helper.assertTrue(data.hasDamageInfo() && data.getDamageInfo().attackType() == AttackType.slash(), "Adapter must capture the selected source attack");
            helper.assertTrue(Objects.equals(data.getDamageInfo().projectileDamage(), 2.0), "Adapter must capture configured projectile damage");
            helper.assertTrue(data.getPendingLaunch() == null, "Shared flight adjustment must run on the actual launch");
        }
        return projectiles;
    }

    /** Creates stored order data, including deliberate malformed entries. */
    private static ListTag order(String... ids) {
        ListTag result = new ListTag();
        for (String id : ids) result.add(StringTag.valueOf(id));
        return result;
    }
}
