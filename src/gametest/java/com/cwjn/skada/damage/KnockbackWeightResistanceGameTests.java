package com.cwjn.skada.damage;

import com.cwjn.skada.Skada;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.data.damage.WeaponInfo;
import com.cwjn.skada.data.damage.WeaponStatApplicability;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.generation.attack.ElementSpread;
import com.cwjn.skada.util.EquipmentWeightUtil;
import com.cwjn.skada.util.KnockbackWeightResistanceUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

/** Exercises automatic weight resistance through spawn, equipment changes, and growth. */
@GameTestHolder(Skada.MODID)
@PrefixGameTestTemplate(false)
public final class KnockbackWeightResistanceGameTests {
    private static final String TEMPLATE = "damage_handler_5x4x5";

    /** Prevents instantiation of this GameTest collection. */
    private KnockbackWeightResistanceGameTests() {}

    /** Confirms body and held mass update one stable resistance modifier as the entity changes. */
    @GameTest(templateNamespace = Skada.MODID, template = TEMPLATE)
    public static void spawnedWeightResistanceTracksEquipmentAndGrowthWithoutDuplicates(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 1, 2, 1);
        double adultResistance = derivedResistance(cow);
        helper.assertTrue(adultResistance > 0.0,
                "A spawned living entity should receive resistance from its resolved body weight");

        cow.setBaby(true);
        cow.tick();
        double babyResistance = derivedResistance(cow);
        helper.assertTrue(babyResistance < adultResistance,
                "Normal-pose body volume should reduce the resistance modifier while the cow is a baby");

        ItemStack weightedWeapon = weapon(25.0);
        cow.setItemSlot(EquipmentSlot.MAINHAND, weightedWeapon);
        cow.tick();
        helper.assertTrue(derivedResistance(cow) > babyResistance,
                "Changing held equipment should add its physical weight to the derived defence");
        helper.assertTrue(EquipmentWeightUtil.getTotalEquippedWeightKg(cow) == 25.0,
                "Only the equipped weapon mass should contribute to total equipment weight");
        helper.assertTrue(weightModifierCount(cow) == 1,
                "Equipment changes must update one stable weight modifier rather than stack duplicates");

        cow.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        cow.tick();
        helper.assertTrue(Math.abs(derivedResistance(cow) - babyResistance) < 1.0e-9,
                "Removing weighted equipment should restore the body-only modifier");

        cow.setBaby(false);
        cow.tick();
        helper.assertTrue(Math.abs(derivedResistance(cow) - adultResistance) < 1.0e-9,
                "Growth should replace the body modifier with the adult body-volume value");
        helper.assertTrue(weightModifierCount(cow) == 1,
                "Growth and equipment updates must continue to maintain a single modifier");
        helper.succeed();
    }

    /** Reads the separately identified weight contribution from the cow's resistance attribute. */
    private static double derivedResistance(Cow cow) {
        AttributeModifier modifier = cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE)
                .getModifier(KnockbackWeightResistanceUtil.MODIFIER_ID);
        return modifier == null ? 0.0 : modifier.getAmount();
    }

    /** Counts active copies of the derived modifier to detect accidental stacking. */
    private static long weightModifierCount(Cow cow) {
        return cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getModifiers().stream()
                .filter(modifier -> modifier.getId().equals(KnockbackWeightResistanceUtil.MODIFIER_ID))
                .count();
    }

    /** Creates a configured weapon stack with the requested physical mass. */
    private static ItemStack weapon(double weightKg) {
        AttackTypeInfo profile = new AttackTypeInfo(8, 1000, 0, 4, 1.6, 2, 0, 0, List.of());
        WeaponInfo info = new WeaponInfo(Map.of(AttackType.strike(), profile), new ElementSpread(),
                WeaponStatApplicability.MELEE, weightKg);
        ItemStack stack = new ItemStack(Items.STICK);
        stack.getOrCreateTag().put(SkadaData.WEAPON_INFO_TAG_KEY, info.toCompoundTag());
        return stack;
    }
}
