package com.cwjn.skada.data.gen.weapon.mace;

import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.parts.MaceHeadPart;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPart;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.Core;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.CoreStation;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.Flanges;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.ImpactOverride;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.KnobRing;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.Mount;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.SpikeRing;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaceHeadPartTest {

    @Test
    void weaponPartCodecDispatchesMaceHeadType() {
        MaceHeadPart original = new MaceHeadPart(createProfile());

        var encoded = WeaponPart.CODEC.encodeStart(JsonOps.INSTANCE, original).result().orElseThrow();
        WeaponPart decoded = WeaponPart.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

        assertInstanceOf(MaceHeadPart.class, decoded);
        MaceHeadPart head = (MaceHeadPart) decoded;
        assertEquals(MaceHeadPart.TYPE, head.typeId());

        WeaponPartEntry entry = new WeaponPartEntry(head, null, new Vec3(36.0, 0.0, 0.0), null);
        assertEquals(36.0, entry.position().x(), 1.0e-9);
    }

    @Test
    void maceHeadExposesStrikeGeometryAndInertia() {
        MaceHeadPart head = new MaceHeadPart(createProfile());

        assertFalse(ThrustCapable.class.isInstance(head));
        assertTrue(head.normalizedStrikeContactPointOnPart(128) > 0.5);
        assertTrue(head.effectiveContactAreaCm2() > 0.0);
        assertTrue(head.strikeFaceGeometryFocus() > 1.0);
        assertTrue(head.strikeHeadRigidity() > 1.0);
        assertTrue(head.strikeAssemblyStability() >= 1.0);
        assertTrue(head.momentOfInertiaAboutCenterOfMass(WeaponAxis.Z, 1.0, 128) > 0.0);
    }

    private static MaceHeadProfile createProfile() {
        return new MaceHeadProfile(
                1,
                new Mount("socket", 5.2, 1.2, 0.8, 0.0, 0.0, 1.2),
                new Core(10.5, List.of(
                        new CoreStation(0.0, 1.8),
                        new CoreStation(0.18, 2.45),
                        new CoreStation(0.55, 2.75),
                        new CoreStation(0.88, 2.35),
                        new CoreStation(1.0, 1.85))),
                new Flanges(6, 0.08, 0.94, 0.95, 0.9, 0.24, 1.6, 0.0),
                List.of(new KnobRing(0.55, 8, 0.6, 0.55)),
                List.of(new SpikeRing(0.72, 8, 1.4, 0.18, 0.95, 10.0)),
                new ImpactOverride(1.2, 1.8, 1.15));
    }
}