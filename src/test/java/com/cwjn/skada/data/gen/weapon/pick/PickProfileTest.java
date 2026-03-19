package com.cwjn.skada.data.gen.weapon.pick;

import com.cwjn.skada.data.gen.weapon.profile.PickProfile;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Bore;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Eye;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Rear;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Section;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Spike;
import com.cwjn.skada.data.gen.weapon.profile.PickProfile.Station;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Bounds;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickProfileTest {

  @Test
  void shiftedEyeClampsOriginCenteredEllipseBore() {
    Eye eye = new Eye(6.0, 4.0, 3.0, 1.5, new Bore(8.0, 4.0, "ellipse"));

    Bore clamped = PickProfile.getClampedBore(eye);

    assertNotNull(clamped);
    assertEquals(2.999, clamped.width(), 1.0e-9);
    assertEquals(2.999, clamped.thickness(), 1.0e-9);
    assertEquals("ellipse", clamped.shape());
  }

  @Test
  void rearHammerAddsMassAndExtendsBoundsBehindEye() {
    PickProfile withoutRear = createProfile(new Rear("none", null, null, null, null, null, null, null));
    PickProfile withHammer = createProfile(new Rear("hammer", 4.0, 2.4, 1.8, 6.5, 1.5, 0.25, null));

    MassProperties baseMass = PickProfile.computeMassProperties(withoutRear, 1.0, 256);
    MassProperties hammerMass = PickProfile.computeMassProperties(withHammer, 1.0, 256);
    Bounds bounds = PickProfile.localBounds(withHammer, 128);

    assertTrue(hammerMass.volumeCm3() > baseMass.volumeCm3());
    assertTrue(hammerMass.centerOfMass().x() < baseMass.centerOfMass().x());
    assertTrue(bounds.minX() < PickProfile.eyeMinX(withHammer.getEye()));
  }

  @Test
  void rearSpikeReusesSpikeSamplingAndExtendsNegativeXBounds() {
    Spike rearSpike = new Spike(
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
    PickProfile profile = createProfile(new Rear("spike", null, null, null, null, null, null, rearSpike));

    PickProfile.SpikeSlice slice = profile.sampleRearSpikeSliceAt(0.45).orElseThrow();
    Bounds bounds = profile.localBounds(128);

    assertTrue(slice.width() > 0.0);
    assertTrue(slice.thickness() > 0.0);
    assertTrue(slice.area() > 0.0);
    assertTrue(bounds.minX() < PickProfile.eyeMinX(profile.getEye()));
  }

  @Test
  void invalidVersionAndRearFieldMixesAreRejected() {
    IllegalArgumentException badVersion = assertThrows(IllegalArgumentException.class,
      () -> new PickProfile(2, createEye(), createFrontSpike(), new Rear("none", null, null, null, null, null, null, null)));
    assertTrue(badVersion.getMessage().contains("version"));

    IllegalArgumentException badRear = assertThrows(IllegalArgumentException.class,
        () -> new Rear("spike", 3.0, null, null, null, null, null, createRearSpike()));
    assertTrue(badRear.getMessage().contains("hammer-only"));
  }

  private static PickProfile createProfile(Rear rear) {
    return new PickProfile(1, createEye(), createFrontSpike(), rear);
  }

  private static Eye createEye() {
    return new Eye(4.5, 4.0, 2.4, 0.0, new Bore(2.0, 1.5, "ellipse"));
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