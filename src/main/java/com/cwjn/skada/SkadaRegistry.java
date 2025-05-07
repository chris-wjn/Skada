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
    public static final DeferredRegister<AttackType> ATTACK_TYPES = DeferredRegister.create(Util.rl("damage_class"), MODID);
    public static final DeferredRegister<Parameter> PARAMETERS = DeferredRegister.create(Util.rl("parameter"), MODID);

    public static final DeferredRegister<Element> ELEMENTS = DeferredRegister.create(Util.rl("element"), MODID);

    public static final RegistryObject<Element> HEAT = element("heat", ColourLibrary.HEAT);
    public static final RegistryObject<Element> COLD = element("cold", ColourLibrary.COLD);
    public static final RegistryObject<Element> LIGHTNING = element("lightning", ColourLibrary.LIGHTNING);
    public static final RegistryObject<Element> ENDER = element("ender", ColourLibrary.ENDER);
    public static final RegistryObject<Element> WITHER = element("wither", ColourLibrary.WITHER);
    public static final RegistryObject<Element> AETHER = element("aether", ColourLibrary.AETHER);
    public static final RegistryObject<Element> BASIC = element("basic", ColourLibrary.BASIC);

    public static final RegistryObject<AttackType> SLASH = attackType("slash", SkadaData.PERCENT_DAMAGE_BONUS, SkadaData.SLASH_GENERATOR_CONFIG);
    public static final RegistryObject<AttackType> THRUST = attackType("thrust", SkadaData.PERCENT_HEALTH_DAMAGE, SkadaData.THRUST_GENERATOR_CONFIG);
    public static final RegistryObject<AttackType> STRIKE = attackType("strike", SkadaData.PERCENT_REDUC, SkadaData.STRIKE_GENERATOR_CONFIG);
    public static final RegistryObject<AttackType> MAGIC = attackType("magic", SkadaData.NONE, SkadaData.NULL_GENERATOR_CONFIG);
    public static final RegistryObject<AttackType> NONE = attackType("none", SkadaData.NONE, SkadaData.NULL_GENERATOR_CONFIG);

    private static RegistryObject<AttackType> attackType(String name, LethalityFunction type, AttackTypeGeneratorConfiguration tierStatFunction) {
        Attribute r = new RangedAttribute("attribute.skada." + name + "_resist", 0.0D, -1024.0D, 10.0D).setSyncable(true);
        ForgeRegistries.ATTRIBUTES.register("damage_class." + name + "_resist", r);
        return ATTACK_TYPES.register(name,
                () -> new AttackType(
                        name,
                        type,
                        tierStatFunction,
                        r
                ));
    }

    private static RegistryObject<Element> element(String name, int colour) {
        Attribute a = new RangedAttribute("attribute.skada." + name + "_affinity", 0.0D, -10.0D, 1024.0D).setSyncable(true);
        Attribute r = new RangedAttribute("attribute.skada." + name + "_resist", 0.0D, -1024.0D, 10.0D).setSyncable(true);
        Attribute b = new RangedAttribute("attribute.skada." + name + "_base_damage", 0.0D, 0.0D, 8192.0D).setSyncable(true);
        ForgeRegistries.ATTRIBUTES.register("element." + name + "_affinity", a);
        ForgeRegistries.ATTRIBUTES.register("element." + name + "_resist", r);
        ForgeRegistries.ATTRIBUTES.register("element." + name + "_base_damage", b);
        return ELEMENTS.register(name,
                () -> new Element(
                        name,
                        b, a, r, colour, Util.rl("textures/element/" + name + ".png"),
                        TagKey.create(Registries.DAMAGE_TYPE, Util.rl("convert_" + name))
                )
        );
    }

    private static RegistryObject<Attribute> combatAttribute(String name) {
            return ATTRIBUTES.register("combat_attribute." + name,
                    () -> new RangedAttribute("attribute.skada.combat_attribute." + name, 0.0D, 0.0D, 20.0D).setSyncable(true));
    }

}
