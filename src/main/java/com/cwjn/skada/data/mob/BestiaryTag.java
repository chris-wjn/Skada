package com.cwjn.skada.data.mob;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Valid authored tags that can be attached to bestiary entries.
 */
public enum BestiaryTag {
  OVERWORLD("overworld"),
  NETHER("nether"),
  END("end"),
  AQUATIC("aquatic"),
  PASSIVE("passive"),
  NEUTRAL("neutral"),
  HOSTILE("hostile");

  public static final Codec<BestiaryTag> CODEC = Codec.STRING.comapFlatMap(BestiaryTag::fromSerializedName,
      BestiaryTag::serializedName);

  private final String serializedName;

  BestiaryTag(String serializedName) {
    this.serializedName = serializedName;
  }

  /**
   * Returns the stable lowercase name used in authored JSON data.
   */
  public String serializedName() {
    return serializedName;
  }

  /**
   * Decodes an authored list of lowercase tag names into validated enum values.
   */
  public static DataResult<List<BestiaryTag>> fromSerializedNames(List<String> names) {
    List<BestiaryTag> tags = new ArrayList<>(names.size());
    for (String name : names) {
      DataResult<BestiaryTag> result = fromSerializedName(name);
      if (result.error().isPresent()) {
        return DataResult.error(() -> "Unknown bestiary tag: " + name);
      }
      tags.add(result.result().orElseThrow());
    }
    return DataResult.success(List.copyOf(tags));
  }

  /**
   * Encodes bestiary tags back to their stable lowercase authored names.
   */
  public static DataResult<List<String>> toSerializedNames(List<BestiaryTag> tags) {
    return DataResult.success(tags.stream().map(BestiaryTag::serializedName).toList());
  }

  private static DataResult<BestiaryTag> fromSerializedName(String name) {
    for (BestiaryTag value : values()) {
      if (value.serializedName.equals(name)) {
        return DataResult.success(value);
      }
    }
    return DataResult.error(() -> "Unknown bestiary tag: " + name);
  }
}