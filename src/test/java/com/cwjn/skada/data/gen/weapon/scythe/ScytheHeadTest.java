package com.cwjn.skada.data.gen.weapon.scythe;

import com.cwjn.skada.data.gen.weapon.parts.ScythePart;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPart;
import com.cwjn.skada.data.gen.weapon.parts.WeaponPartEntry;
import com.cwjn.skada.data.gen.weapon.profile.ScytheProfile;
import com.cwjn.skada.data.gen.weapon.profile.ScytheProfile.Mount;
import com.cwjn.skada.data.gen.weapon.profile.ScytheProfile.Neck;
import com.cwjn.skada.data.gen.weapon.profile.ScytheProfile.SharpenedRange;
import com.cwjn.skada.data.gen.weapon.profile.ScytheProfile.Station;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScytheHeadTest {

    @Test
    void weaponPartCodecDispatchesScytheType() {
        ScythePart original = new ScythePart(createProfile());

        var encoded = WeaponPart.CODEC.encodeStart(JsonOps.INSTANCE, original).result().orElseThrow();
        WeaponPart decoded = WeaponPart.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

        assertInstanceOf(ScythePart.class, decoded);
        ScythePart head = (ScythePart) decoded;
        assertEquals(ScythePart.TYPE, head.typeId());

        WeaponPartEntry entry = new WeaponPartEntry(head, null, new Vec3(2.0, 0.0, 0.0), null);
        assertEquals(2.0, entry.position().x(), 1.0e-9);
    }

    @Test
    void scytheHeadProvidesSlashAndThrustGeometry() {
        ScythePart head = new ScythePart(createProfile());

        assertTrue(head.edgeRadiusNm() > 0.0);
        assertTrue(head.tipRadiusNm() > 0.0);
        assertTrue(head.tipLengthCm() > 0.0);
        assertTrue(head.widthAtPointBase() > 0.0);
        assertTrue(head.thicknessAtPointBase() > 0.0);

        var cross = head.crossSectionAt(0.7);
        assertNotNull(cross);
        assertTrue(cross.effectiveWidthCm() > 0.0);
        assertTrue(cross.halfThicknessCm() >= 0.0);

        double strikeNorm = head.normalizedSlashStrikePointOnPart(128);
        assertTrue(strikeNorm >= 0.0 && strikeNorm <= 1.0);
    }

    private static ScytheProfile createProfile() {
        Mount mount = new Mount("socket", 10.0, 3.2, 2.2, 2.4, 1.6, 0.0, 0.0, "oval", 68.0, 2.0);
        Neck neck = new Neck(4.5, 1.2, 0.6, 0.75);

        List<Station> stations = List.of(
                new Station(0.0, 4.2, 0.6, new Station.Section(2.1, 0.38, 0.35)),
                new Station(0.25, 4.8, 0.52, new Station.Section(2.2, 0.36, 0.3)),
                new Station(0.55, 4.4, 0.4, new Station.Section(2.4, 0.3, 0.2)),
                new Station(0.82, 2.6, 0.24, new Station.Section(2.7, 0.18, 0.08))
        );

        return new ScytheProfile(
                1,
                mount,
                neck,
                List.of(
                        new Vec3(0, 0, 0),
                        new Vec3(14, 1.8, 0),
                        new Vec3(31, 7.2, 0),
                        new Vec3(48, 15.6, 0),
                        new Vec3(61, 23.8, 0)
                ),
                0.82,
                6.0,
                18.0,
                8.0,
                new SharpenedRange(0.1, 1.0),
                0.74,
                stations
        );
    }
}
