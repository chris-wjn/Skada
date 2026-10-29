package com.cwjn.skada.generation.armour;

import com.cwjn.skada.generation.weapon.MaterialInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmourMassUtilTest {
    @Test
    void convertsDensityThicknessLayersAndCoveredAreaToKilograms() {
        // 7.8 g/cm^3 * 0.3 cm (two 1 mm layers plus 1 mm padding) * 1000 cm^2 = 2340 g.
        assertEquals(2.34, ArmourMassUtil.estimateKg(7.8, 1.0, 2, 1.0, 1000.0), 1.0e-9);
    }

    @Test
    void invalidOrUnconfiguredPhysicalInputsContributeNoMass() {
        assertEquals(0.0, ArmourMassUtil.estimateKg(Double.NaN, 1.0, 1, 0.0, 1000.0));
        assertEquals(0.0, ArmourMassUtil.estimateKg(7.8, 1.0, 1, 0.0, 0.0));
    }

    @Test
    void authoredMassOverridesEstimateAndExplicitZeroIsPreserved() {
        MaterialInfo material = new MaterialInfo(8.0, 5.0, 5.0, 5.0, null);
        ArmourConstructionInfo construction = new ArmourConstructionInfo(3.0, 2, 1.0,
                0.5, 0.5, 0.5, 0.5, 0.0, 1.0);
        assertEquals(0.0, new ArmourPieceInfo(1, 1, 1, 1, 1, 1000, 0)
                .estimateWeightKg(material, construction));
        assertEquals(5.6, new ArmourPieceInfo(1, 1, 1, 1, 1, 1000, -1)
                .estimateWeightKg(material, construction), 1.0e-9);
    }
}
