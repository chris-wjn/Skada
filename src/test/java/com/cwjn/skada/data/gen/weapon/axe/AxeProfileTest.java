package com.cwjn.skada.data.gen.weapon.axe;

import com.cwjn.skada.data.gen.weapon.profile.AxeProfile;
import com.cwjn.skada.data.gen.weapon.profile.AxeProfile.AxeCrossSection;
import com.cwjn.skada.data.gen.weapon.profile.AxeProfile.AxeEdge;
import com.cwjn.skada.data.gen.weapon.profile.AxeProfile.AxeEye;
import com.cwjn.skada.data.gen.weapon.profile.AxeProfile.AxeEyeBore;
import com.cwjn.skada.data.gen.weapon.profile.AxeProfile.AxeLobe;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AxeProfileTest {

    private static final int AXE_PROFILE_VERSION = 1;

    @Test
    void edgeStartAndBoundsUseEyeOffset() {
        AxeProfile profile = createProfile(1.25);

        assertEquals(-1.75, AxeProfile.eyeMinX(profile.eye()), 1.0e-9);
        assertEquals(4.25, AxeProfile.eyeMaxX(profile.eye()), 1.0e-9);
        assertEquals(4.25, AxeProfile.edgeStart(profile), 1.0e-9);
        assertEquals(-1.75, AxeProfile.localBounds(profile, 64).minX(), 1.0e-9);
        assertEquals(10.25, AxeProfile.localBounds(profile, 64).maxX(), 1.0e-9);
    }

    @Test
    void shiftedEyeMovesPositiveMassWhileBoreStaysPinnedAtOrigin() {
        AxeProfile baseProfile = createProfile(0.0);
        AxeProfile shiftedProfile = createProfile(1.5);

        MassProperties base = AxeProfile.computeMassProperties(baseProfile, 1.0, 256);
        MassProperties shifted = AxeProfile.computeMassProperties(shiftedProfile, 1.0, 256);

        AxeEyeBore baseBore = AxeProfile.getClampedEyeBore(baseProfile.eye());
        AxeEyeBore clampedBore = AxeProfile.getClampedEyeBore(shiftedProfile.eye());
        assertNotNull(baseBore);
        assertNotNull(clampedBore);

        double baseBoreVolume = AxeProfile.boreArea(baseBore) * baseProfile.eye().height();
        double shiftedBoreVolume = AxeProfile.boreArea(clampedBore) * shiftedProfile.eye().height();
        double positiveVolume = base.volumeCm3() + baseBoreVolume;
        double expectedShiftedVolume = positiveVolume - shiftedBoreVolume;
        double expectedShiftedCenterX = ((base.centerOfMass().x() * base.volumeCm3()) + (1.5 * positiveVolume)) / expectedShiftedVolume;

        assertEquals(expectedShiftedVolume, shifted.volumeCm3(), 1.0e-9);
        assertEquals(expectedShiftedCenterX, shifted.centerOfMass().x(), 3.0e-2);
    }

    @Test
    void boreWidthIsClampedToOriginCenteredOpeningWithinShiftedEye() {
        AxeEye eye = new AxeEye(6.0, 5.0, 4.0, 1.5, new AxeEyeBore(8.0, 3.0, "rect"));

        AxeEyeBore clamped = AxeProfile.getClampedEyeBore(eye);

        assertNotNull(clamped);
        assertEquals(2.999, clamped.width(), 1.0e-9);
        assertEquals(3.0, clamped.thickness(), 1.0e-9);
    }

    @Test
    void crossSectionLengthChangesHalfThicknessSampling() {
        AxeCrossSection shortSection = new AxeCrossSection(3.0, 4.0, 1.0);
        AxeCrossSection longSection = new AxeCrossSection(9.0, 4.0, 1.0);

        double shortHalfThickness = AxeProfile.halfThicknessAt(shortSection, 4.0, 6.0);
        double longHalfThickness = AxeProfile.halfThicknessAt(longSection, 4.0, 6.0);

        assertEquals(0.0, shortHalfThickness, 1.0e-9);
        assertTrue(longHalfThickness > 1.0);
    }

    @Test
    void crossSectionLengthChangesAxeMassProperties() {
        AxeProfile shortProfile = createProfile(0.0, 3.5, 3.0);
        AxeProfile longProfile = createProfile(0.0, 9.0, 8.5);

        MassProperties shortMass = AxeProfile.computeMassProperties(shortProfile, 1.0, 256);
        MassProperties longMass = AxeProfile.computeMassProperties(longProfile, 1.0, 256);

        assertTrue(longMass.volumeCm3() > shortMass.volumeCm3());
        assertTrue(longMass.massG() > shortMass.massG());
    }

    private static AxeProfile createProfile(double eyeOffset) {
        return createProfile(eyeOffset, 8.0, 7.5);
    }

    private static AxeProfile createProfile(double eyeOffset, double coreSectionLength, double sideSectionLength) {
        AxeEye eye = new AxeEye(6.0, 5.0, 4.0, eyeOffset, new AxeEyeBore(3.0, 3.0, "circle"));
        AxeCrossSection coreSection = new AxeCrossSection(coreSectionLength, 3.0, 1.3);
        AxeCrossSection sideSection = new AxeCrossSection(sideSectionLength, 2.0, 1.2);
        AxeLobe core = new AxeLobe(5.0, null, coreSection);
        AxeLobe top = new AxeLobe(2.0, null, sideSection);
        AxeLobe bottom = new AxeLobe(2.0, null, sideSection);
        AxeEdge edge = new AxeEdge(6.0, core, top, bottom);
        return new AxeProfile(AXE_PROFILE_VERSION, eye, edge);
    }
}
