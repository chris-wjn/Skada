package com.cwjn.skada.data.gen.weapon.parts;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BladeTaperTest {

    @Test
    public void testMedianWidthSimpleLinear() {
        // Blade with constant width -> median should equal that width
        Blade b = new Blade(false, 50.0, 50.0, Map.of(), 5.0, 5.0, Map.of(), 200.0, Blade.Bevel.defaultBevel(), Blade.EdgeBevel.noBevel(), Blade.TipSpecifications.noTip(), Blade.Fuller.noFuller());
        double median = b.getMedianWidth();
        assertEquals(50.0, median, 1e-6, "Median width for constant width blade should equal width");
    }

    @Test
    public void testMedianWidthVarying() {
        // Width tapers from 100 at base to 20 at tip; median should be around middle of sampled widths
        Blade b = new Blade(false, 100.0, 20.0, Map.of(), 6.0, 2.0, Map.of(), 300.0, new Blade.Bevel(0.2,1.0), Blade.EdgeBevel.noBevel(), Blade.TipSpecifications.noTip(), Blade.Fuller.noFuller());
        double median = b.getMedianWidth();
        // median should be between tip and base
        assertTrue(median > 20.0 && median < 100.0, "Median should be strictly between tip and base widths");
    }

    @Test
    public void testTaperValueSimple() {
        // If median width equals base width and no taper, taper value should be small
        Blade b = new Blade(false, 40.0, 40.0, Map.of(), 4.0, 4.0, Map.of(), 150.0, Blade.Bevel.defaultBevel(), Blade.EdgeBevel.noBevel(), Blade.TipSpecifications.noTip(), Blade.Fuller.noFuller());
        double taper = b.getTaperValue();
        assertTrue(taper >= 0.0, "Taper value should be non-negative");
    }

    @Test
    public void testTaperValueTaperedBlade() {
        // Tapered blade from 120 to 10 over 240mm should have substantial taper value
        Blade b = new Blade(false, 120.0, 10.0, Map.of(50.0,60.0), 6.0, 2.0, Map.of(), 240.0, new Blade.Bevel(0.25,1.2), Blade.EdgeBevel.noBevel(), Blade.TipSpecifications.noTip(), Blade.Fuller.noFuller());
        double taper = b.getTaperValue();
        assertTrue(taper > 1.0, "Taper value for strongly tapered blade should be > 1.0");
    }

}

