package com.cwjn.skada.data.gen.attack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record AttackTypeJsonInfo(double minReach,
                                 double maxReach,
                                 double attackSpeedModifier,
                                 double lethalityModifier,
                                 double precisionModifier,
                                 double damageBonus,
                                 double critFailModifier,
                                 List<String> reticleShapes) {

  public static AttackTypeJsonInfo getDefault() {
    return new AttackTypeJsonInfo(0.0, 3.0, 1.0, 1.0, 1.0, 0.0, 1.0, List.of());
  }

  public static Codec<AttackTypeJsonInfo> CODEC = RecordCodecBuilder.create(
          instance -> instance.group(
                  Codec.DOUBLE.fieldOf("minReach").forGetter(AttackTypeJsonInfo::minReach),
                  Codec.DOUBLE.fieldOf("maxReach").forGetter(AttackTypeJsonInfo::maxReach),
                  Codec.DOUBLE.optionalFieldOf("attackSpeedModifier", 1.0).forGetter(AttackTypeJsonInfo::attackSpeedModifier),
                  Codec.DOUBLE.optionalFieldOf("lethalityModifier", 1.0).forGetter(AttackTypeJsonInfo::lethalityModifier),
                  Codec.DOUBLE.optionalFieldOf("precisionModifier", 1.0).forGetter(AttackTypeJsonInfo::precisionModifier),
                  Codec.DOUBLE.optionalFieldOf("damageBonus", 0.0).forGetter(AttackTypeJsonInfo::damageBonus),
                  Codec.DOUBLE.optionalFieldOf("critFailModifier", 1.0).forGetter(AttackTypeJsonInfo::critFailModifier),
                  Codec.list(Codec.STRING).optionalFieldOf("reticleShapes", List.of()).forGetter(AttackTypeJsonInfo::reticleShapes)
          ).apply(instance, AttackTypeJsonInfo::new));

}
