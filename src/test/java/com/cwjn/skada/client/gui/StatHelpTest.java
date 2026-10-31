package com.cwjn.skada.client.gui;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Guards the player-facing distinction between ratings, reductions and contextual statistics. */
class StatHelpTest {
    /** Every exact threshold starts the next band, while values immediately below retain the previous band. */
    @Test void knockbackCategoriesRespectObservedBoundaries() {
        double[] thresholds = {0.375, 0.5, 1.0, 1.375, 1.875};
        String[] bands = {"negligible", "weak", "normal", "strong", "extreme", "massive"};
        for (int i = 0; i < thresholds.length; i++) {
            assertEquals("skada.knockback.category." + bands[i],
                    StatHelp.knockbackCategoryKey(Math.nextDown(thresholds[i])));
            assertEquals("skada.knockback.category." + bands[i + 1],
                    StatHelp.knockbackCategoryKey(thresholds[i]));
        }
        assertEquals("skada.knockback.category.negligible", StatHelp.knockbackCategoryKey(Math.nextUp(0.0)));
        assertEquals("skada.knockback.category.massive", StatHelp.knockbackCategoryKey(4.0));
        for (double value : new double[]{0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertEquals("skada.knockback.category.none", StatHelp.knockbackCategoryKey(value));
        }
    }

    /** Exact values remain available on Shift without crowding the default category display. */
    @Test void knockbackTooltipRevealsTheUnroundedValueOnlyInDetailedMode() {
        double value = 1.23456789;
        var compact = (TranslatableContents) StatHelp.knockbackTooltip(value, false).getContents();
        var detailed = (TranslatableContents) StatHelp.knockbackTooltip(value, true).getContents();
        assertEquals("skada.tooltip.info.knockback", compact.getKey());
        assertEquals(1, compact.getArgs().length);
        assertEquals("skada.knockback.category.strong", ((TranslatableContents)
                ((net.minecraft.network.chat.Component) compact.getArgs()[0]).getContents()).getKey());
        assertEquals("skada.tooltip.info.knockback_detailed", detailed.getKey());
        assertEquals("1.23456789", detailed.getArgs()[1]);
    }

    /** Signed attack resistance results must never present vulnerability as damage reduction. */
    @Test void resistanceResultsDistinguishReductionFromVulnerability() {
        var positive = StatHelp.lines("strike_resist", 100.0);
        var reduction = (TranslatableContents) positive.get(1).getContents();
        assertEquals("skada.help.result.reducing", reduction.getKey());
        assertEquals("50.0", reduction.getArgs()[0]);
        var negative = StatHelp.lines("strike_resist", -50.0);
        var vulnerability = (TranslatableContents) negative.get(1).getContents();
        assertEquals("skada.help.result.increasing", vulnerability.getKey());
        assertEquals("100.0", vulnerability.getArgs()[0]);
    }

    /** Armour must disclose that its standalone percentage is a baseline. */
    @Test void physicalReductionIsAlwaysLabelledAsBaseline() {
        for (String stat : new String[]{"armour", "physical_resist"}) {
            assertEquals("skada.help.result.base_reducing", ((TranslatableContents) StatHelp.lines(stat, 12.0).get(1).getContents()).getKey());
            assertEquals("skada.help.result.base_increasing", ((TranslatableContents) StatHelp.lines(stat, -12.0).get(1).getContents()).getKey());
        }
    }

    /** Opponent-dependent stats and nonfinite values cannot acquire an invented result percentage. */
    @Test void contextualAndInvalidValuesHaveOnlyTheSummary() {
        assertEquals(1, StatHelp.lines("toughness", 100.0).size());
        assertEquals(1, StatHelp.lines("element_damage", 100.0).size());
        assertEquals(1, StatHelp.lines("strike_resist", Double.NaN).size());
    }

    /** Player compact labels and synced bestiary attribute keys share the same explanations. */
    @Test void journalLabelsResolveToTheCorrectStatMeaning() {
        assertEquals("strike_resist", StatHelp.statForLabel("skada.journal.player.core.strike.compact"));
        assertEquals("strike_resist", StatHelp.statForLabel("attribute.skada.strike_resist"));
        assertEquals("armour", StatHelp.statForLabel("attribute.name.generic.armor"));
        assertEquals("toughness", StatHelp.statForLabel("attribute.name.generic.armor_toughness"));
        assertEquals("element_resist", StatHelp.statForLabel("attribute.skada.heat_resist"));
        assertEquals("weight", StatHelp.statForLabel("skada.journal.bestiary.body_weight_reference"));
    }
}
