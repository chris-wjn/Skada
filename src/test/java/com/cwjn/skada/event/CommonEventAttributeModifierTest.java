package com.cwjn.skada.event;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AttackTypeInfo;
import com.cwjn.skada.util.Util;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonEventAttributeModifierTest {

  @BeforeAll
  static void bootstrapMinecraft() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  @Test
  void attackSpeedModifierUsesSkadaUuidAndFinalApsOffset() {
    AttackTypeInfo attackInfo = AttackTypeInfo.of(10.0, 7.0, 2.4, 3.0, 0.821, 6.5, 0.08, List.of());

    AttributeModifier modifier = CommonEvent.ForgeBusEvent.attackSpeedModifier(attackInfo);

    assertEquals(SkadaData.SKADA_ATTACK_TYPE_SPEED_UUID, modifier.getId());
    assertEquals(attackInfo.attackSpeed() - Attributes.ATTACK_SPEED.getDefaultValue(), modifier.getAmount(), 1.0e-9);
  }

  @Test
  void attackDamageModifierUsesSkadaUuidAndGeneratedDamage() {
    AttackTypeInfo attackInfo = AttackTypeInfo.of(10.0, 7.0, 2.4, 3.0, 0.821, 6.5, 0.08, List.of());

    AttributeModifier modifier = CommonEvent.ForgeBusEvent.attackDamageModifier(attackInfo);

    assertEquals(SkadaData.SKADA_ATTACK_TYPE_DAMAGE_UUID, modifier.getId());
    assertEquals(attackInfo.damage(), modifier.getAmount(), 1.0e-9);
  }

  @Test
  void generationBaseLookupUsesVanillaDefaultModifiers() {
    assertEquals(8.0, Util.getDamageModifierForItem(Items.DIAMOND_AXE), 1.0e-9);
    assertEquals(6.0, Util.getDamageModifierForItem(Items.WOODEN_AXE), 1.0e-9);
    assertEquals(1.0, Util.getAttackSpeedForItem(Items.DIAMOND_AXE), 1.0e-9);
    assertEquals(4.0, Util.getAttackSpeedForItem(Items.DIAMOND_HOE), 1.0e-9);
  }
}