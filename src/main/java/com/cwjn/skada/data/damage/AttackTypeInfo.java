package com.cwjn.skada.data.damage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

public record AttackTypeInfo(double lethality,
                             double accuracy,
                             double minReach,
                             double maxReach,
                             double attackSpeedMod,
                             double damageBonus,
                             double failChance) {

    public static Codec<AttackTypeInfo> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
            Codec.DOUBLE.fieldOf("lethality").forGetter(AttackTypeInfo::lethality),
            Codec.DOUBLE.fieldOf("accuracy").forGetter(AttackTypeInfo::accuracy),
            Codec.DOUBLE.fieldOf("minReach").forGetter(AttackTypeInfo::minReach),
            Codec.DOUBLE.fieldOf("maxReach").forGetter(AttackTypeInfo::maxReach),
            Codec.DOUBLE.fieldOf("attackSpeedMod").forGetter(AttackTypeInfo::attackSpeedMod),
            Codec.DOUBLE.fieldOf("damageBonus").forGetter(AttackTypeInfo::damageBonus),
            Codec.DOUBLE.fieldOf("failChance").forGetter(AttackTypeInfo::failChance)
    ).apply(instance, AttackTypeInfo::new));

    public static final AttackTypeInfo DEFAULT = new AttackTypeInfo(0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0);

    public CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("lethality", lethality);
        tag.putDouble("accuracy", accuracy);
        tag.putDouble("minReach", minReach);
        tag.putDouble("maxReach", maxReach);
        tag.putDouble("attackSpeedMod", attackSpeedMod);
        tag.putDouble("damageBonus", damageBonus);
        tag.putDouble("failChance", failChance);
        return tag;
    }

    public static AttackTypeInfo fromCompoundTag(CompoundTag tag) {
        return new AttackTypeInfo(
                tag.getDouble("lethality"),
                tag.getDouble("accuracy"),
                tag.getDouble("minReach"),
                tag.getDouble("maxReach"),
                tag.getDouble("attackSpeedMod"),
                tag.getDouble("damageBonus"),
                tag.getDouble("failChance")
        );
    }

}
