package com.cwjn.skada.data.damage;

import com.cwjn.skada.client.hud.ReticleShape;
import com.cwjn.skada.data.SkadaData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

import static com.cwjn.skada.data.SkadaData.RETICLES;

public record AttackTypeInfo(
  double lethality,
    double precision,
    double minReach,
    double maxReach,
    double attackSpeed,
    double damage,
    double failChance,
    List<String> reticleShapes) {

  public static Codec<AttackTypeInfo> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
          Codec.DOUBLE.fieldOf("lethality").forGetter(AttackTypeInfo::lethality),
          Codec.DOUBLE.fieldOf("precision").forGetter(AttackTypeInfo::precision),
          Codec.DOUBLE.fieldOf("minReach").forGetter(AttackTypeInfo::minReach),
          Codec.DOUBLE.fieldOf("maxReach").forGetter(AttackTypeInfo::maxReach),
          Codec.DOUBLE.fieldOf("attackSpeed").forGetter(AttackTypeInfo::attackSpeed),
          Codec.DOUBLE.fieldOf("damage").forGetter(AttackTypeInfo::damage),
          Codec.DOUBLE.fieldOf("failChance").forGetter(AttackTypeInfo::failChance),
          Codec.list(Codec.STRING).optionalFieldOf("reticleShapes", new ArrayList<>())
              .forGetter(AttackTypeInfo::reticleShapes))
          .apply(instance, AttackTypeInfo::new));

  // Default AttackTypeInfo that represents an unarmed attack.
  public static final AttackTypeInfo FIST = new AttackTypeInfo(0.0, 8.0, 0.0, 3.0, 4.0, 1.0, 0.0, List.of("punch"));
  public static final AttackTypeInfo ITEM = new AttackTypeInfo(0.0, 4.0, 0.0, 0.0, 3.0, 1.0, 0.0, List.of("strike_default"));

  /**
   * Creates an attack info instance for tests and simple call sites without
   * having to repeat the record constructor name.
   *
   * @param lethality the attack lethality value
   * @param precision the attack precision value
   * @param minReach the minimum reach for the attack
   * @param maxReach the maximum reach for the attack
   * @param attackSpeed the exact attacks-per-second value
   * @param damage the damage bonus carried by the attack
   * @param failChance the critical fail chance
   * @param reticleShapes the reticle shapes used by this attack
   * @return a new {@link AttackTypeInfo} with the supplied values
   */
  public static AttackTypeInfo of(double lethality, double precision, double minReach, double maxReach,
      double attackSpeed, double damage, double failChance, List<String> reticleShapes) {
    return new AttackTypeInfo(lethality, precision, minReach, maxReach, attackSpeed, damage, failChance,
        reticleShapes);
  }

  public CompoundTag toCompoundTag() {
    CompoundTag tag = new CompoundTag();
    tag.putDouble("lethality", lethality);
    tag.putDouble("precision", precision);
    tag.putDouble("minReach", minReach);
    tag.putDouble("maxReach", maxReach);
    tag.putDouble("attackSpeed", attackSpeed);
    tag.putDouble("damage", damage);
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
        tag.contains("attackSpeed") ? tag.getDouble("attackSpeed")
            : tag.contains("attackSpeedOffset") ? tag.getDouble("attackSpeedOffset") : 0.0,
        tag.contains("damage") ? tag.getDouble("damage") : tag.getDouble("damageBonus"),
        tag.getDouble("failChance"),
        reticleShapes);
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
      } else {
        throw new IllegalArgumentException("Reticle shape '" + shapeName + "' not found in registry.");
      }
    }
    return shapes;
  }

}
