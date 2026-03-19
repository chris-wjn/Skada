package com.cwjn.skada.data.gen.weapon.spear;

import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile;
import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile.Mount;
import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile.Section;
import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile.SharpenedRange;
import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile.Station;
import com.cwjn.skada.data.gen.weapon.profile.SpearHeadProfile.Wings;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Bounds;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpearHeadProfileTest {

  @Test
  void codecRoundTripPreservesSocketAndWingGeometry() {
    SpearHeadProfile original = createSocketProfile();

    var encoded = SpearHeadProfile.CODEC.encodeStart(JsonOps.INSTANCE, original).result().orElseThrow();
    Object decoded = SpearHeadProfile.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

    assertInstanceOf(SpearHeadProfile.class, decoded);
    SpearHeadProfile profile = (SpearHeadProfile) decoded;
    assertEquals("socket", profile.getMount().type());
    assertEquals(4, profile.getStations().size());
    assertTrue(profile.getWings() != null && profile.getWings().enabled());
  }

  @Test
  void socketMountAndWingsContributeToMassAndBounds() {
    SpearHeadProfile socket = createSocketProfile();
    SpearHeadProfile tang = createTangProfile();

    MassProperties socketMass = socket.computeMassProperties(1.0, 256);
    MassProperties tangMass = tang.computeMassProperties(1.0, 256);
    Bounds bounds = socket.localBounds(256);

    assertTrue(socketMass.volumeCm3() > tangMass.volumeCm3());
    assertTrue(socketMass.centerOfMass().x() < socket.sampleSliceAt(0.55).position().x());
    assertTrue(bounds.minX() < socket.getCenterline().get(0).x());
    assertTrue(bounds.maxY() > socket.sampleSliceAt(0.0).width() * 0.5);
  }

  @Test
  void tipSamplingAndSharpenedRangeFollowSchemaRules() {
    SpearHeadProfile profile = createTangProfile();

    SpearHeadProfile.SpearSlice base = profile.sampleSliceAt(0.82);
    SpearHeadProfile.SpearSlice tip = profile.sampleSliceAt(0.96);

    assertTrue(profile.tipLengthCm() > 0.0);
    assertTrue(tip.width() < base.width());
    assertTrue(tip.thickness() < base.thickness());
    assertTrue(profile.isSharpenedAt(0.5));
    assertTrue(!profile.isSharpenedAt(0.05));
  }

  @Test
  void invalidMountAndWingConfigurationsAreRejected() {
    IllegalArgumentException badSocket = assertThrows(IllegalArgumentException.class,
        () -> new Mount("socket", 10.0, 2.0, 2.0, 2.0, 1.8, null, null, null, null, "oval", 2.0));
    assertTrue(badSocket.getMessage().contains("inner dimensions"));

    IllegalArgumentException badWings = assertThrows(IllegalArgumentException.class,
        () -> new SpearHeadProfile(
            1,
            new Mount("socket", 11.0, 2.8, 2.2, 2.1, 1.6, null, null, null, null, "oval", 2.0),
            List.of(new Vec3(0.0, 0.0, 0.0), new Vec3(12.0, 0.0, 0.0), new Vec3(26.0, 0.0, 0.0)),
            0.92,
            6.0,
            7.0,
            20.0,
            new SharpenedRange(0.12, 1.0),
            new Wings(true, 0.02, 2.0, 4.5, 0.4, 1.2, "triangular"),
            defaultStations()));
    assertTrue(badWings.getMessage().contains("behind the shoulder"));
  }

  private static SpearHeadProfile createSocketProfile() {
    return new SpearHeadProfile(
        1,
        new Mount("socket", 11.0, 2.8, 2.2, 2.1, 1.6, null, null, null, null, "oval", 2.0),
        List.of(
            new Vec3(0.0, 0.0, 0.0),
            new Vec3(12.0, 0.0, 0.0),
            new Vec3(26.0, 0.0, 0.0)),
        0.92,
        6.0,
        7.0,
        20.0,
        new SharpenedRange(0.12, 1.0),
        new Wings(true, 0.18, 2.0, 3.6, 0.42, 1.2, "triangular"),
        defaultStations());
  }

  private static SpearHeadProfile createTangProfile() {
    return new SpearHeadProfile(
        1,
        new Mount("tang", 9.0, null, null, null, null, 2.2, 0.7, 0.8, 0.25, "oval", 1.5),
        List.of(
            new Vec3(0.0, 0.0, 0.0),
            new Vec3(10.0, 0.1, 0.0),
            new Vec3(24.0, 0.0, 0.0)),
        0.88,
        null,
        7.0,
        18.0,
        new SharpenedRange(0.1, 1.0),
        null,
        defaultStations());
  }

  private static List<Station> defaultStations() {
    return List.of(
        new Station(0.0, 5.6, 1.0, new Section(1.5, 0.18)),
        new Station(0.25, 5.0, 0.88, new Section(1.55, 0.16)),
        new Station(0.55, 3.1, 0.56, new Section(1.65, 0.10)),
        new Station(0.82, 1.4, 0.22, new Section(1.8, 0.0)));
  }
}