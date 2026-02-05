package com.cwjn.skada.data.damage;

import com.cwjn.skada.client.hud.ReticleShape;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

import static com.cwjn.skada.data.SkadaData.RETICLES;

public record AttackTypeInfo(double lethality,
                             double precision,
                             double minReach,
                             double maxReach,
                             double attackSpeedMod,
                             double damageBonus,
                             double failChance,
                             List<String> reticleShapes) {

    public static Codec<AttackTypeInfo> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
            Codec.DOUBLE.fieldOf("lethality").forGetter(AttackTypeInfo::lethality),
            Codec.DOUBLE.fieldOf("precision").forGetter(AttackTypeInfo::precision),
            Codec.DOUBLE.fieldOf("minReach").forGetter(AttackTypeInfo::minReach),
            Codec.DOUBLE.fieldOf("maxReach").forGetter(AttackTypeInfo::maxReach),
            Codec.DOUBLE.fieldOf("attackSpeedModifier").forGetter(AttackTypeInfo::attackSpeedMod),
            Codec.DOUBLE.fieldOf("damageBonus").forGetter(AttackTypeInfo::damageBonus),
            Codec.DOUBLE.fieldOf("failChance").forGetter(AttackTypeInfo::failChance),
            Codec.list(Codec.STRING).optionalFieldOf("reticleShapes", new ArrayList<>()).forGetter(AttackTypeInfo::reticleShapes)
    ).apply(instance, AttackTypeInfo::new));

    public static final AttackTypeInfo DEFAULT = new AttackTypeInfo(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, List.of());

    public CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("lethality", lethality);
        tag.putDouble("precision", precision);
        tag.putDouble("minReach", minReach);
        tag.putDouble("maxReach", maxReach);
        tag.putDouble("attackSpeedModifier", attackSpeedMod);
        tag.putDouble("damageBonus", damageBonus);
        tag.putDouble("failChance", failChance);
        if (hasReticleShapes()) {
            tag.putInt("numReticleShapes", reticleShapes.size());
            for (int i = 0; i < reticleShapes.size(); i++) {
                tag.putString("reticleShape" + i, reticleShapes.get(i));
            }
        }
        return tag;
    }

    public static AttackTypeInfo fromCompoundTag(CompoundTag tag) {
        List<String> reticleShapes = null;
        if (tag.contains("numReticleShapes")) {
            int numShapes = tag.getInt("numReticleShapes");
            reticleShapes = new ArrayList<>(numShapes);
            for (int i = 0; i < numShapes; i++) {
                reticleShapes.add(tag.getString("reticleShape" + i));
            }
        }
        return new AttackTypeInfo(
                tag.getDouble("lethality"),
                tag.getDouble("precision"),
                tag.getDouble("minReach"),
                tag.getDouble("maxReach"),
                tag.getDouble("attackSpeedModifier"),
                tag.getDouble("damageBonus"),
                tag.getDouble("failChance"),
                reticleShapes
        );
    }

    public boolean hasReticleShapes() {
        return reticleShapes != null && !reticleShapes.isEmpty();
    }

    public List<ReticleShape> getReticleShapes() {
        if (reticleShapes == null || reticleShapes.isEmpty()) {
            throw new IllegalStateException("Reticle shapes are not defined for this AttackTypeInfo.");
        }
        List<ReticleShape> shapes = new ArrayList<>(reticleShapes.size());
        for (String shapeName : reticleShapes) {
            ReticleShape shape = RETICLES.get(shapeName);
            if (shape != null) {
                shapes.add(shape);
            }
            else {
                throw new IllegalArgumentException("Reticle shape '" + shapeName + "' not found in registry.");
            }
        }
        return shapes;
    }

}
