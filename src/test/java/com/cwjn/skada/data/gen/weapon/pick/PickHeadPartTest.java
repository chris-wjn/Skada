package com.cwjn.skada.data.gen.weapon.pick;

import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.parts.PickHeadPart;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPart;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Bore;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Eye;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Rear;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Section;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Spike;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Station;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickHeadPartTest {

    @Test
    void weaponPartCodecDispatchesPickHeadType() {
        PickHeadPart original = new PickHeadPart(createHammerProfile());

        var encoded = WeaponPart.CODEC.encodeStart(JsonOps.INSTANCE, original).result().orElseThrow();
        WeaponPart decoded = WeaponPart.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

        assertInstanceOf(PickHeadPart.class, decoded);
        PickHeadPart head = (PickHeadPart) decoded;
        assertEquals(PickHeadPart.TYPE, head.typeId());

        WeaponPartEntry entry = new WeaponPartEntry(head, null, new Vec3(3.0, 0.0, 0.0), null);
        assertEquals(3.0, entry.position().x(), 1.0e-9);
    }

    @Test
    void pickHeadProvidesRotationalThrustAndRearStrikeGeometry() {
        PickHeadPart hammerHead = new PickHeadPart(createHammerProfile());
        PickHeadPart spikeHead = new PickHeadPart(createRearSpikeProfile());

        assertEquals(ThrustCapable.ThrustMotionMode.ROTATIONAL, hammerHead.thrustMotionMode());
        assertTrue(hammerHead.tipLengthCm() > 0.0);
        assertTrue(hammerHead.widthAtPointBase() > 0.0);
        assertTrue(hammerHead.thicknessAtPointBase() > 0.0);
        assertTrue(hammerHead.tipRadiusNm() > 0.0);
        assertEquals(1.0, hammerHead.normalizedThrustContactPointOnPart(128), 1.0e-9);

        assertEquals(0.0, hammerHead.normalizedStrikeContactPointOnPart(128), 1.0e-9);
        assertTrue(hammerHead.effectiveContactAreaCm2() > 0.02);
        assertTrue(hammerHead.strikeFaceGeometryFocus() >= 0.95);

        assertEquals(0.0, spikeHead.normalizedStrikeContactPointOnPart(128), 1.0e-9);
        assertTrue(spikeHead.effectiveContactAreaCm2() >= 0.02);
        assertTrue(spikeHead.strikeFaceGeometryFocus() > hammerHead.strikeFaceGeometryFocus());
    }

    private static PickProfile createHammerProfile() {
        return new PickProfile(
                1,
                new Eye(4.5, 4.0, 2.4, 0.0, new Bore(2.0, 1.5, "ellipse")),
                createFrontSpike(),
                new Rear("hammer", 4.0, 2.1, 1.7, 6.5, 1.5, 0.25, null)
        );
    }

    private static PickProfile createRearSpikeProfile() {
        return new PickProfile(
                1,
                new Eye(4.5, 4.0, 2.4, 0.0, new Bore(2.0, 1.5, "ellipse")),
                createFrontSpike(),
                new Rear("spike", null, null, null, null, null, null, createRearSpike())
        );
    }

    private static Spike createFrontSpike() {
        return new Spike(
                List.of(
                        new Vec3(2.25, 0.0, 0.0),
                        new Vec3(9.0, 0.0, 0.0),
                        new Vec3(14.5, -0.4, 0.0)),
                0.97,
                7.0,
                List.of(
                        new Station(0.0, 1.8, 1.6, new Section("diamond", 1.0)),
                        new Station(0.45, 1.0, 0.9, new Section("diamond", 1.0)),
                        new Station(0.82, 0.36, 0.3, new Section("diamond", 1.0))));
    }

    private static Spike createRearSpike() {
        return new Spike(
                List.of(
                        new Vec3(-2.25, 0.0, 0.0),
                        new Vec3(-7.0, 0.2, 0.0),
                        new Vec3(-12.0, 0.0, 0.0)),
                0.92,
                8.0,
                List.of(
                        new Station(0.0, 1.4, 1.2, new Section("diamond", 1.0)),
                        new Station(0.5, 0.85, 0.7, new Section("diamond", 1.0)),
                        new Station(0.82, 0.3, 0.24, new Section("diamond", 1.0))));
    }
}