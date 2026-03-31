package com.cwjn.skada;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.LethalityFunction;
import com.cwjn.skada.data.gen.attack.AttackTypeGeneratorConfiguration;
import com.cwjn.skada.data.gen.weapon.attack_capability.AttackCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.NoneCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.util.UtilColour;
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
    public static final DeferredRegister<Element> ELEMENTS = DeferredRegister.create(Util.rl("element"), MODID);

    public static final RegistryObject<Element> HEAT = element("heat", UtilColour.HEAT);
    public static final RegistryObject<Element> COLD = element("cold", UtilColour.COLD);
    public static final RegistryObject<Element> LIGHTNING = element("lightning", UtilColour.LIGHTNING);
    public static final RegistryObject<Element> ENDER = element("ender", UtilColour.ENDER);
    public static final RegistryObject<Element> WITHER = element("wither", UtilColour.WITHER);
    public static final RegistryObject<Element> AETHER = element("aether", UtilColour.AETHER);
    public static final RegistryObject<Element> PHYSICAL = element("basic", UtilColour.BASIC);

    public static final RegistryObject<AttackType> SLASH = attackType("slash", SkadaData.PERCENT_DAMAGE_BONUS, SkadaData.SLASH_GENERATOR_CONFIG, SlashCapable.class);
    public static final RegistryObject<AttackType> THRUST = attackType("thrust", SkadaData.PERCENT_HEALTH_DAMAGE, SkadaData.THRUST_GENERATOR_CONFIG, ThrustCapable.class);
    public static final RegistryObject<AttackType> STRIKE = attackType("strike", SkadaData.PERCENT_REDUC, SkadaData.STRIKE_GENERATOR_CONFIG, StrikeCapable.class);
    public static final RegistryObject<AttackType> NONE = attackType("none", SkadaData.NONE, SkadaData.NULL_GENERATOR_CONFIG, NoneCapable.class);

    private static RegistryObject<AttackType> attackType(String name, LethalityFunction type, AttackTypeGeneratorConfiguration tierStatFunction, Class<? extends AttackCapable> capableInterface) {
        Attribute r = new RangedAttribute("attribute.skada." + name + "_resist", 0.0D, -1024.0D, 1024.0D).setSyncable(true);
        ForgeRegistries.ATTRIBUTES.register("damage_class." + name + "_resist", r);
        return ATTACK_TYPES.register(name,
                () -> new AttackType(
                        name,
                        type,
                        tierStatFunction,
                        r,
                        capableInterface
                ));
    }

    private static RegistryObject<Element> element(String name, int colour) {
        Attribute a = new RangedAttribute("attribute.skada." + name + "_affinity", 0.0D, -10.0D, 1024.0D).setSyncable(true);
        Attribute r = new RangedAttribute("attribute.skada." + name + "_resist", 0.0D, -1024.0D, 1024.0D).setSyncable(true);
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

}
