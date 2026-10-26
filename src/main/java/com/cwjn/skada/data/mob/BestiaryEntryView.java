package com.cwjn.skada.data.mob;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record BestiaryEntryView(ResourceLocation entityId,
                                String displayNameKey,
                                String attackTypeKey,
                                List<StatRow> statRows,
                                List<ResistanceRow> resistanceRows,
                                List<String> description,
                List<BestiaryTag> tags,
                                BestiaryPreviewSettings preview) {

  private static final MapCodec<List<BestiaryTag>> TAGS_CODEC = Codec.list(Codec.STRING)
    .optionalFieldOf("tags", List.of())
                      .flatXmap(BestiaryTag::fromSerializedNames, BestiaryTag::toSerializedNames);

  public static final Codec<BestiaryEntryView> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      ResourceLocation.CODEC.fieldOf("entity_id").forGetter(BestiaryEntryView::entityId),
      Codec.STRING.fieldOf("display_name_key").forGetter(BestiaryEntryView::displayNameKey),
      Codec.STRING.fieldOf("attack_type_key").forGetter(BestiaryEntryView::attackTypeKey),
      Codec.list(StatRow.CODEC).fieldOf("stat_rows").forGetter(BestiaryEntryView::statRows),
      Codec.list(ResistanceRow.CODEC).fieldOf("resistance_rows").forGetter(BestiaryEntryView::resistanceRows),
    BestiaryInfo.DESCRIPTION_CODEC.fieldOf("description").forGetter(BestiaryEntryView::description),
    TAGS_CODEC.forGetter(BestiaryEntryView::tags),
      BestiaryPreviewSettings.CODEC.optionalFieldOf("preview", BestiaryPreviewSettings.DEFAULT)
          .forGetter(BestiaryEntryView::preview)
  ).apply(instance, BestiaryEntryView::new));

  public BestiaryEntryView {
    statRows = List.copyOf(statRows);
    resistanceRows = List.copyOf(resistanceRows);
    description = List.copyOf(description);
    tags = tags == null ? List.of() : List.copyOf(tags);
    preview = preview == null ? BestiaryPreviewSettings.DEFAULT : preview;
  }

  public record StatRow(String labelKey, double value, String formattedValue) {
    public static final Codec<StatRow> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("label_key").forGetter(StatRow::labelKey),
        Codec.DOUBLE.fieldOf("value").forGetter(StatRow::value),
        Codec.STRING.fieldOf("formatted_value").forGetter(StatRow::formattedValue)
    ).apply(instance, StatRow::new));
  }

  public record ResistanceRow(String labelKey, double value, String formattedValue) {
    public static final Codec<ResistanceRow> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("label_key").forGetter(ResistanceRow::labelKey),
        Codec.DOUBLE.fieldOf("value").forGetter(ResistanceRow::value),
        Codec.STRING.fieldOf("formatted_value").forGetter(ResistanceRow::formattedValue)
    ).apply(instance, ResistanceRow::new));
  }
}