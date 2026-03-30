package com.cwjn.skada.data.gen.armour;

import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

public record ArmourItemMapping(
    String material,
    String construction,
    String piece,
    ArmourPieceInfo overrides) {

  public static final ArmourItemMapping DEFAULT = new ArmourItemMapping("", "", "", ArmourPieceInfo.DEFAULT);

  @SuppressWarnings("null")
  public static final Codec<ArmourItemMapping> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.fieldOf("material").forGetter(ArmourItemMapping::material),
      Codec.STRING.fieldOf("construction").forGetter(ArmourItemMapping::construction),
      Codec.STRING.fieldOf("piece").forGetter(ArmourItemMapping::piece),
      ArmourPieceInfo.CODEC.optionalFieldOf("overrides", ArmourPieceInfo.DEFAULT).forGetter(ArmourItemMapping::overrides))
      .apply(instance, ArmourItemMapping::new));

  public static final Codec<Map<String, ArmourItemMapping>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC);

  public ArmourPieceInfo resolvePiece(Map<String, ArmourPieceInfo> pieceMap) {
    ArmourPieceInfo basePiece = pieceMap.getOrDefault(piece, ArmourPieceInfo.DEFAULT);
    return basePiece.merge(overrides);
  }

  public String resolveMaterialId(String namespace) {
    return material.contains(".") ? material : namespace + "." + material;
  }

  public String resolveConstructionId(String namespace) {
    return construction.contains(".") ? construction : namespace + "." + construction;
  }

  public record Resolved(Map<Element, Double> elementalResists,
                         Map<AttackType, Double> attackResists,
                         double armourBonus,
                         double armourToughnessBonus,
                         double burden,
                         MaterialInfo material,
                         ArmourConstructionInfo construction,
                         ArmourPieceInfo pieceInfo) {
  }
}