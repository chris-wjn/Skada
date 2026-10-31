package com.cwjn.skada.client.gui;

import com.cwjn.skada.util.UtilDefenseDisplay;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Shared localized stat explanations for journal hovers and interactive tutorial examples. */
public final class StatHelp {
    private StatHelp() {}

    /** Classifies selected-attack base push using the author's observed bands, with inclusive lower bounds. */
    public static String knockbackCategoryKey(double knockback) {
        String category;
        if (!Double.isFinite(knockback) || knockback <= 0.0) category = "none";
        else if (knockback < 0.375) category = "negligible";
        else if (knockback < 0.5) category = "weak";
        else if (knockback < 1.0) category = "normal";
        else if (knockback < 1.375) category = "strong";
        else if (knockback < 1.875) category = "extreme";
        else category = "massive";
        return "skada.knockback.category." + category;
    }

    /**
     * Shows the category and optional raw value. The mutable return type selects UtilText's config-aware
     * formatter; its Component varargs overload forces the pixel font and discards legacy colours.
     */
    public static MutableComponent knockbackTooltip(double knockback, boolean detailed) {
        Component category = Component.translatable(knockbackCategoryKey(knockback));
        return detailed
                ? Component.translatable("skada.tooltip.info.knockback_detailed", category, Double.toString(knockback))
                : Component.translatable("skada.tooltip.info.knockback", category);
    }

    /** Builds a summary, with an optional signed calculated defence result. */
    public static List<Component> lines(String stat, Double rating) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("skada.help." + stat));
        if (rating != null && Double.isFinite(rating)) {
            Double percent = switch (stat) {
                case "armour", "physical_resist" -> UtilDefenseDisplay.armourMitigationPercent(rating);
                case "element_resist" -> UtilDefenseDisplay.elementalMitigationPercent(rating);
                case "slash_resist", "thrust_resist", "strike_resist", "attack_resist" -> UtilDefenseDisplay.attackTypeMitigationPercent(rating);
                default -> null;
            };
            if (percent != null && Double.isFinite(percent)) {
                String key = percent < 0 ? "increasing" : "reducing";
                if (stat.equals("armour") || stat.equals("physical_resist")) key = percent < 0 ? "base_increasing" : "base_reducing";
                lines.add(Component.translatable("skada.help.result." + key, String.format(Locale.ROOT, "%.1f", Math.abs(percent))));
            }
        }
        return lines;
    }

    /** Resolves an attribute or compact-label key to a stable teaching identity. */
    public static String statForLabel(String key) {
        if (key.endsWith(".compact")) key = key.substring(0, key.length() - 8);
        String name = key.substring(key.lastIndexOf('.') + 1);
        return switch (name) {
            case "max_health", "health" -> "health";
            case "armor", "armour" -> "armour";
            case "armor_toughness", "toughness" -> "toughness";
            case "movement_speed", "move_speed" -> "move_speed";
            case "slash", "thrust", "strike" -> name + "_resist";
            case "slash_resist", "thrust_resist", "strike_resist", "knockback_resistance" -> name;
            case "physical_resist" -> "physical_resist";
            case "body_size_scale" -> "body_size_scale";
            case "body_weight_reference", "body_weight_resolved", "weight" -> "weight";
            default -> name.endsWith("_resist") ? "element_resist" : "configured_stat";
        };
    }
}
