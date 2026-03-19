package com.cwjn.skada.data.gen.weapon.spear;

import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.parts.SpearPart;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPart;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile;
import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile.Mount;
import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile.Section;
import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile.SharpenedRange;
import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile.Station;
import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile.Wings;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpearPartTest {

    @Test
    void weaponPartCodecDispatchesSpearType() {
        SpearPart original = new SpearPart(createProfile());

        var encoded = WeaponPart.CODEC.encodeStart(JsonOps.INSTANCE, original).result().orElseThrow();
        WeaponPart decoded = WeaponPart.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

        assertInstanceOf(SpearPart.class, decoded);
        SpearPart spear = (SpearPart) decoded;
        assertEquals(SpearPart.TYPE, spear.typeId());

        WeaponPartEntry entry = new WeaponPartEntry(spear, null, new Vec3(2.0, 0.0, 0.0), null);
        assertEquals(2.0, entry.position().x(), 1.0e-9);
    }

    @Test
    void spearPartProvidesLinearThrustAndSlashGeometry() {
        SpearPart spear = new SpearPart(createProfile());

        assertEquals(ThrustCapable.ThrustMotionMode.LINEAR, spear.thrustMotionMode());
        assertTrue(spear.tipLengthCm() > 0.0);
        assertTrue(spear.widthAtPointBase() > 0.0);
        assertTrue(spear.thicknessAtPointBase() > 0.0);
        assertTrue(spear.tipRadiusNm() > 0.0);
        assertEquals(1.0, spear.normalizedThrustContactPointOnPart(128), 1.0e-9);

        var cross = spear.crossSectionAt(0.45);
        assertNotNull(cross);
        assertTrue(cross.effectiveWidthCm() > 0.0);
        assertTrue(cross.halfThicknessCm() >= 0.0);
        assertEquals(20.0, spear.edgeAngleDegreesAt(0.45), 1.0e-9);

        double slashNorm = spear.normalizedSlashStrikePointOnPart(128);
        assertTrue(slashNorm >= 0.0 && slashNorm <= 1.0);
        assertTrue(spear.momentOfInertiaAboutCenterOfMass(WeaponAxis.Z, 1.0, 128) > 0.0);
        assertTrue(spear.localBounds(128).minX() < 0.0);
    }

    private static SpearHeadProfile createProfile() {
        return new SpearHeadProfile(
                1,
                new Mount("socket", 11.0, 2.8, 2.2, 2.1, 1.6, null, null, null, null, "oval", 2.0),
                List.of(
                        new Vec3(0.0, 0.0, 0.0),
                        new Vec3(12.0, 0.0, 0.0),
                        new Vec3(26.0, 0.0, 0.0)
                ),
                0.92,
                6.0,
                7.0,
                20.0,
                new SharpenedRange(0.12, 1.0),
                new Wings(true, 0.18, 2.0, 3.6, 0.42, 1.2, "triangular"),
                List.of(
                        new Station(0.0, 5.6, 1.0, new Section(1.5, 0.18)),
                        new Station(0.25, 5.0, 0.88, new Section(1.55, 0.16)),
                        new Station(0.55, 3.1, 0.56, new Section(1.65, 0.10)),
                        new Station(0.82, 1.4, 0.22, new Section(1.8, 0.0))
                )
        );
    }
}