package com.cwjn.skada.data.mob;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

public record BestiaryInfo(List<String> description, List<BestiaryTag> tags, BestiaryPreviewSettings preview) {

  public static final Codec<List<String>> DESCRIPTION_CODEC = Codec.either(Codec.STRING, Codec.list(Codec.STRING))
    .xmap(description -> description.map(List::of, List::copyOf),
      description -> description.size() == 1 ? Either.left(description.get(0)) : Either.right(description));

  private static final MapCodec<List<BestiaryTag>> TAGS_CODEC = Codec.list(Codec.STRING)
      .optionalFieldOf("tags", List.of())
      .flatXmap(BestiaryTag::fromSerializedNames, BestiaryTag::toSerializedNames);

  public static final Codec<BestiaryInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    DESCRIPTION_CODEC.fieldOf("description").forGetter(BestiaryInfo::description),
      TAGS_CODEC.forGetter(BestiaryInfo::tags),
      BestiaryPreviewSettings.CODEC.optionalFieldOf("preview", BestiaryPreviewSettings.DEFAULT)
          .forGetter(BestiaryInfo::preview)
  ).apply(instance, BestiaryInfo::new));

  public static final Codec<Map<String, BestiaryInfo>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC);

  public BestiaryInfo {
    description = List.copyOf(description);
    tags = tags == null ? List.of() : List.copyOf(tags);
    preview = preview == null ? BestiaryPreviewSettings.DEFAULT : preview;
  }
}