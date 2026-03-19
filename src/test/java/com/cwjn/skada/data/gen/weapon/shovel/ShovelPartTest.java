package com.cwjn.skada.data.gen.weapon.shovel;

import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.parts.ShovelPart;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPart;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.Blade;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.CenterRib;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.Edge;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.Mount;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.Reinforcement;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.Shoulder;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.Station;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShovelPartTest {

    @Test
    void weaponPartCodecDispatchesShovelType() {
        ShovelPart original = new ShovelPart(createProfile());

        var encoded = WeaponPart.CODEC.encodeStart(JsonOps.INSTANCE, original).result().orElseThrow();
        WeaponPart decoded = WeaponPart.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

        assertInstanceOf(ShovelPart.class, decoded);
        ShovelPart head = (ShovelPart) decoded;
        assertEquals(ShovelPart.TYPE, head.typeId());

        WeaponPartEntry entry = new WeaponPartEntry(head, null, new Vec3(35.0, 0.0, 0.0), null);
        assertEquals(35.0, entry.position().x(), 1.0e-9);
    }

    @Test
    void shovelRemainsStrikeOnlyAndExposesStrikeGeometry() {
        ShovelPart head = new ShovelPart(createProfile());

        assertFalse(ThrustCapable.class.isInstance(head));
        assertTrue(head.normalizedStrikeContactPointOnPart(128) > 0.6);
        assertTrue(head.effectiveContactAreaCm2() > 0.0);
        assertTrue(head.strikeFaceGeometryFocus() > 0.8);
        assertTrue(head.strikeHeadRigidity() > 0.9);
        assertTrue(head.strikeAssemblyStability() > 0.9);
        assertTrue(head.momentOfInertiaAboutCenterOfMass(WeaponAxis.Z, 1.0, 128) > 0.0);
    }

    private static ShovelHeadProfile createProfile() {
        return new ShovelHeadProfile(
                1,
                new Mount("socket", 6.5, 2.8, 2.2, 3.2, 1.9, 1.9, 1.45, 0.22, 1.6, null, null, null),
                new Shoulder(3.2, 1.6, 5.4, 0.5, 0.28, 0.45),
                new Blade(18.0, List.of(
                        new Station(0.0, 5.4, 0.28, 0.45, 0.0),
                        new Station(0.35, 7.0, 0.24, 0.7, 0.15),
                        new Station(0.72, 7.5, 0.2, 0.55, 0.08),
                        new Station(1.0, 6.3, 0.14, 0.1, -0.05)),
                        new Edge("rounded", 1.2, 1.6)),
                new Reinforcement(new CenterRib(0.08, 0.78, 0.35, 1.4), null, null),
                null);
    }
}