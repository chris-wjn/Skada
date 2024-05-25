package com.cwjn.skada.data;

import com.cwjn.skada.data.damage.LethalityFunction;
import com.cwjn.skada.data.gen.AttackTypeGeneratorConfiguration;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.data.registry.Parameter;
import com.cwjn.skada.util.Util;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.UUID;
import java.util.function.Supplier;

import static com.cwjn.skada.data.damage.LethalityFunction.Operation.*;

/*
    * A class to hold global data for Skada.
*/
public abstract class SkadaData {

    public static Supplier<IForgeRegistry<AttackType>> REGISTRY_ATTACK_TYPE;
    public static Supplier<IForgeRegistry<Element>> REGISTRY_ELEMENT;
    public static Supplier<IForgeRegistry<Parameter>> REGISTRY_PARAMETER;
    public static final LethalityFunction NONE = new LethalityFunction((l, a) -> 0, SUM_WITH_DAMAGE);
    public static final LethalityFunction FLAT_DAMAGE = new LethalityFunction((l, a) -> Math.max(l - a, 0), SUM_WITH_DAMAGE);
    public static final LethalityFunction FLAT_REDUC = new LethalityFunction((l, a) -> -Math.max(l - a, 0), SUM_WITH_ARMOUR);
    public static final LethalityFunction PERCENT_REDUC = new LethalityFunction(Util::percentReduc, MULTIPLY_WITH_ARMOUR);
    public static final AttackTypeGeneratorConfiguration SLASH_GENERATOR_CONFIG = new AttackTypeGeneratorConfiguration(Util::slashDamageCalculation, Util::slashLethalityCalculation, Util::slashAimCalculation);
    public static final AttackTypeGeneratorConfiguration STRIKE_GENERATOR_CONFIG = new AttackTypeGeneratorConfiguration(Util::strikeDamageCalculation, Util::strikeLethalityCalculation, Util::strikeAimCalculation);
    public static final AttackTypeGeneratorConfiguration THRUST_GENERATOR_CONFIG = new AttackTypeGeneratorConfiguration(Util::thrustDamageCalculation, Util::thrustLethalityCalculation, Util::thrustAimCalculation);
    public static final AttackTypeGeneratorConfiguration NULL_GENERATOR_CONFIG = new AttackTypeGeneratorConfiguration((a, b, c, d) -> 0, (a, b, c, d) -> 0, (a, b, c, d) -> 0);
    public static final String WEAPON_INFO_TAG_KEY = "skada.weapon_info.tagkey";
    public static final String CURRENT_ATTACK_TYPE_TAG_KEY = "skada.current_attack_type.key";
    public static final String NUM_ATTACK_TYPES_TAG_KEY = "skada.num_attack_types.key";
    public static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    public static final UUID BASE_ATTACK_SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
    public static final UUID ATTACK_TYPE_BASE_MOD_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
    public static final UUID unused = UUID.fromString("e4eaaaf2-d142-11e1-b3e4-080027620cdd");
    public static final UUID unused_1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    public static final UUID unused_2 = UUID.fromString("6fad54f3-44c2-4a4f-a8c0-6d0b8a2e7f30");

}
