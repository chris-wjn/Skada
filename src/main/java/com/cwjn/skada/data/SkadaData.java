package com.cwjn.skada.data;

import com.cwjn.skada.client.hud.ReticleShape;
import com.cwjn.skada.data.damage.LethalityFunction;
import com.cwjn.skada.data.gen.AttackTypeGeneratorConfiguration;
import com.cwjn.skada.data.mob.MobData;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.*;
import java.util.function.Supplier;

import static com.cwjn.skada.data.damage.LethalityFunction.Operation.*;

/*
 * A class to hold global data for Skada.
 */
public abstract class SkadaData {

  public static boolean DEBUG_ENABLED = false;
  public static final double DEFAULT_ACCURACY = 0.75;
  public static Supplier<IForgeRegistry<AttackType>> REGISTRY_ATTACK_TYPE;
  public static Supplier<IForgeRegistry<Element>> REGISTRY_ELEMENT;
  public static final HashMap<EntityType<?>, MobData> MOB_DATA = new HashMap<>();
  public static Map<String, ReticleShape> RETICLES = new HashMap<>();
  public static final LethalityFunction NONE = new LethalityFunction((lethality, armourToughness, targetHP) -> 0, SUM_WITH_DAMAGE);
  public static final LethalityFunction PERCENT_DAMAGE_BONUS = new LethalityFunction(Util::percentBonusDamage, MULTIPLY_WITH_DAMAGE);
  public static final LethalityFunction PERCENT_HEALTH_DAMAGE = new LethalityFunction(Util::percentHealthDamage, SUM_WITH_DAMAGE);
  public static final LethalityFunction PERCENT_REDUC = new LethalityFunction(Util::percentReduc, MULTIPLY_WITH_ARMOUR);
  public static final AttackTypeGeneratorConfiguration SLASH_GENERATOR_CONFIG = new AttackTypeGeneratorConfiguration(Util::slashAccuracyCalculation, Util::slashLethalityCalculation, Util::slashCriticalFailCalculation);
  public static final AttackTypeGeneratorConfiguration STRIKE_GENERATOR_CONFIG = new AttackTypeGeneratorConfiguration(Util::strikeAccuracyCalculation, Util::strikeLethalityCalculation, Util::strikeCriticalFailCalculation);
  public static final AttackTypeGeneratorConfiguration THRUST_GENERATOR_CONFIG = new AttackTypeGeneratorConfiguration(Util::thrustAccuracyCalculation, Util::thrustLethalityCalculation, Util::thrustCriticalFailCalculation);
  public static final AttackTypeGeneratorConfiguration NULL_GENERATOR_CONFIG = new AttackTypeGeneratorConfiguration((a, b, c, d, e, f) -> 0, (a, b, c, d, e, f) -> 0, (a, b, c, d, e, f) -> 0);
  public static final String WEAPON_INFO_TAG_KEY = "skada.weapon_info.tagkey";
  public static final String ARMOUR_INFO_TAG_KEY = "skada.armour_info.tagkey";
  public static final String CURRENT_ATTACK_TYPE_TAG_KEY = "skada.current_attack_type.tagKey";
  public static final String NUM_ATTACK_TYPES_TAG_KEY = "skada.num_attack_types.tagKey";
  public static final String ATTACK_TYPES_ARRAY_TAG_KEY = "skada.attack_types_array.tagKey";
  public static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
  public static final UUID BASE_ATTACK_SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
  public static final UUID SKADA_ATTACK_TYPE_BASE_MOD_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
  public static final UUID SKADA_ATTACK_TYPE_DAMAGE_UUID = UUID.fromString("7b8e9f2a-3c4d-5e6f-7890-1a2b3c4d5e6f");
  public static final UUID SKADA_ATTACK_TYPE_REACH_UUID = UUID.fromString("8c9f0a3b-4d5e-6f70-8901-2b3c4d5e6f70");
  public static final UUID SKADA_ATTACK_TYPE_SPEED_UUID = UUID.fromString("9d0a1b4c-5e6f-7081-9012-3c4d5e6f7081");
  public static final UUID[] SKADA_ARMOUR_BASE_MOD_UUID = {
          UUID.fromString("a7b3c4d5-e6f7-8901-2345-6789abcdef01"),
          UUID.fromString("b8c4d5e6-f7a8-9012-3456-789abcdef012"),
          UUID.fromString("c9d5e6f7-a8b9-0123-4567-89abcdef0123"),
          UUID.fromString("dae6f7a8-b9c0-1234-5678-9abcdef01234")
  };
  public static final UUID SKADA_MOB_MODIFIER_OPERATION_0 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
  public static final UUID SKADA_MOB_MODIFIER_OPERATION_1 = UUID.fromString("5520c08a-21d8-4b25-bfb4-a7b69fb96040");
  public static final UUID SKADA_MOB_MODIFIER_OPERATION_2 = UUID.fromString("ea5398cc-9f9d-43ac-bf3f-1ee0ab6c5dc4");
  public static final UUID unused_2 = UUID.fromString("6fad54f3-44c2-4a4f-a8c0-6d0b8a2e7f30");
  public static final double WEAPON_WEIGHT_MINIMUM = 0.01;
  public static final double LETHALITY_MINIMUM = 1;
  public static final double LETHALITY_SOFT_CAP = 20;
  public static final double MATERIAL_PROPERTY_MINIMUM = 1.0;
  public static final double MATERIAL_PROPERTY_SOFT_CAP = 10.0;

}
