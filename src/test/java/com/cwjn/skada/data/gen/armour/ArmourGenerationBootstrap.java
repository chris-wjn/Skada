package com.cwjn.skada.data.gen.armour;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.LethalityFunction;
import com.cwjn.skada.data.gen.attack.AttackTypeGeneratorConfiguration;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.cwjn.skada.data.damage.LethalityFunction.Operation.SUM_WITH_DAMAGE;

final class ArmourGenerationBootstrap {

  private ArmourGenerationBootstrap() {
  }

  static void bootstrapRegistries() {
    LethalityFunction none = new LethalityFunction((lethality, armour, healthContext) -> 0.0,
        SUM_WITH_DAMAGE);
    AttackTypeGeneratorConfiguration nullConfig = new AttackTypeGeneratorConfiguration((a) -> 0, (a) -> 0,
        (a) -> 0, (a) -> 0);

    Element basic = new Element("basic", null, null, null, 0, new ResourceLocation("skada", "basic"), null);
    Element heat = new Element("heat", null, null, null, 0, new ResourceLocation("skada", "heat"), null);
    Element cold = new Element("cold", null, null, null, 0, new ResourceLocation("skada", "cold"), null);
    Element ender = new Element("ender", null, null, null, 0, new ResourceLocation("skada", "ender"), null);
    Element wither = new Element("wither", null, null, null, 0, new ResourceLocation("skada", "wither"), null);
    SkadaData.REGISTRY_ELEMENT = registrySupplier(Map.of(
        new ResourceLocation("skada", "basic"), basic,
        new ResourceLocation("skada", "heat"), heat,
        new ResourceLocation("skada", "cold"), cold,
        new ResourceLocation("skada", "ender"), ender,
        new ResourceLocation("skada", "wither"), wither));

    AttackType slash = new AttackType("slash", none, nullConfig, null, null);
    AttackType thrust = new AttackType("thrust", none, nullConfig, null, null);
    AttackType strike = new AttackType("strike", none, nullConfig, null, null);
    SkadaData.REGISTRY_ATTACK_TYPE = registrySupplier(Map.of(
        new ResourceLocation("skada", "slash"), slash,
        new ResourceLocation("skada", "thrust"), thrust,
        new ResourceLocation("skada", "strike"), strike));
  }

  @SuppressWarnings("unchecked")
  private static <T> Supplier<IForgeRegistry<T>> registrySupplier(Map<ResourceLocation, T> valuesById) {
    Map<T, ResourceLocation> idsByValue = new IdentityHashMap<>();
    valuesById.forEach((id, value) -> idsByValue.put(value, id));

    IForgeRegistry<T> registry = (IForgeRegistry<T>) Proxy.newProxyInstance(
        ArmourGenerationBootstrap.class.getClassLoader(),
        new Class<?>[] { IForgeRegistry.class },
        (proxy, method, args) -> switch (method.getName()) {
          case "getValue" -> valuesById.get(args[0]);
          case "getKey" -> idsByValue.get(args[0]);
          case "containsKey" -> valuesById.containsKey(args[0]);
          case "getValues" -> valuesById.values();
          case "toString" -> "TestForgeRegistry";
          default -> method.getReturnType().isPrimitive() ? primitiveDefault(method.getReturnType()) : null;
        });
    return () -> registry;
  }

  private static Object primitiveDefault(Class<?> type) {
    if (type == boolean.class) {
      return false;
    }
    if (type == char.class) {
      return '\0';
    }
    return 0;
  }
}