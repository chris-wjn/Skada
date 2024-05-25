package com.cwjn.skada;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.LethalityFunction;
import com.cwjn.skada.data.gen.AttackTypeGeneratorConfiguration;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.data.registry.Parameter;
import com.cwjn.skada.util.ColourLibrary;
import com.cwjn.skada.util.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.cwjn.skada.Skada.MODID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SkadaRegistry {

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, MODID);
    public static final DeferredRegister<AttackType> DAMAGE_CLASSES = DeferredRegister.create(Util.rl("damage_class"), MODID);
    public static final DeferredRegister<Element> ELEMENTS = DeferredRegister.create(Util.rl("element"), MODID);
    public static final DeferredRegister<Parameter> PARAMETERS = DeferredRegister.create(Util.rl("parameter"), MODID);

    public static final RegistryObject<Element> FIRE = element("fire", ColourLibrary.FIRE);
    public static final RegistryObject<Element> ICE = element("ice", ColourLibrary.ICE);
    public static final RegistryObject<Element> LIGHTNING = element("lightning", ColourLibrary.LIGHTNING);
    public static final RegistryObject<Element> EARTH = element("earth", ColourLibrary.EARTH);
    public static final RegistryObject<Element> DARK = element("dark", ColourLibrary.DARK);
    public static final RegistryObject<Element> LIGHT = element("light", ColourLibrary.LIGHT);
    public static final RegistryObject<Element> BASIC = element("basic", ColourLibrary.BASIC);

    public static final RegistryObject<AttackType> SLASH = attackType("slash", SkadaData.FLAT_DAMAGE, SkadaData.SLASH_GENERATOR_CONFIG);
    public static final RegistryObject<AttackType> THRUST = attackType("thrust", SkadaData.FLAT_REDUC, SkadaData.THRUST_GENERATOR_CONFIG);
    public static final RegistryObject<AttackType> STRIKE = attackType("strike", SkadaData.PERCENT_REDUC, SkadaData.STRIKE_GENERATOR_CONFIG);
    public static final RegistryObject<AttackType> MAGIC = attackType("magic", SkadaData.NONE, SkadaData.NULL_GENERATOR_CONFIG);
    public static final RegistryObject<AttackType> NONE = attackType("none", SkadaData.NONE, SkadaData.NULL_GENERATOR_CONFIG);

    public static final RegistryObject<Parameter> VITALITY = parameter("vitality");
    public static final RegistryObject<Parameter> STRENGTH = parameter("strength");
    public static final RegistryObject<Parameter> DEXTERITY = parameter("dexterity");
    public static final RegistryObject<Parameter> INTELLIGENCE = parameter("intelligence");
    public static final RegistryObject<Parameter> WISDOM = parameter("wisdom");
    public static final RegistryObject<Parameter> FAITH = parameter("faith");
    public static final RegistryObject<Parameter> AGILITY = parameter("agility");

    public static final RegistryObject<Attribute> LETHALITY = combatAttribute("lethality");
    public static final RegistryObject<Attribute> AIM = combatAttribute("aim");
    public static final RegistryObject<Attribute> EVASIVENESS = combatAttribute("evasion");

    private static RegistryObject<AttackType> attackType(String name, LethalityFunction type, AttackTypeGeneratorConfiguration tierStatFunction) {
        Attribute r = new RangedAttribute("attribute.skada." + name + "_resist", 1.0D, 0.0D, 10.0D).setSyncable(true);
        ForgeRegistries.ATTRIBUTES.register("damage_class." + name + "_resist", r);
        return DAMAGE_CLASSES.register(name,
                () -> new AttackType(
                        name,
                        type,
                        tierStatFunction,
                        r
                ));
    }

    private static RegistryObject<Element> element(String name, int colour) {
        Attribute a = new RangedAttribute("attribute.skada." + name + "_affinity", 1.0D, 0.0D, 10.D).setSyncable(true);
        Attribute r = new RangedAttribute("attribute.skada." + name + "_resist", 1.0D, 0.0D, 10.0D).setSyncable(true);
        Attribute b = new RangedAttribute("attribute.skada." + name + "_base_damage", 0.0D, 0.0D, 8192.0D).setSyncable(true);
        ForgeRegistries.ATTRIBUTES.register("element." + name + "_affinity", a);
        ForgeRegistries.ATTRIBUTES.register("element." + name + "_resist", r);
        ForgeRegistries.ATTRIBUTES.register("element." + name + "_base_damage", b);
        return ELEMENTS.register(name,
                () -> new Element(
                        name,
                        b, a, r, colour, Util.rl("textures/element/" + name + ".png"),
                        TagKey.create(Registries.DAMAGE_TYPE, Util.rl("convert_" + name))
                ));
    }

    private static RegistryObject<Parameter> parameter(String name) {
        Attribute p = new RangedAttribute("attribute.skada.parameter." + name, 0.0D, 0.0D, 99.0D).setSyncable(true);
        ForgeRegistries.ATTRIBUTES.register("parameter." + name, p);
        return PARAMETERS.register(name,
                () -> new Parameter(
                        name,
                        p
                ));
    }

    private static RegistryObject<Attribute> combatAttribute(String name) {
            return ATTRIBUTES.register("combat_attribute." + name,
                    () -> new RangedAttribute("attribute.skada.combat_attribute." + name, 0.0D, 0.0D, 20.0D).setSyncable(true));
    }

}
