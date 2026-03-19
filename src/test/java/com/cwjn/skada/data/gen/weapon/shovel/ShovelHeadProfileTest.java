package com.cwjn.skada.data.gen.weapon.shovel;

import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.Blade;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.CenterRib;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.DerivedStrikeGeometry;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.Edge;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.ImpactOverride;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.Mount;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.Reinforcement;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.Shoulder;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.SideFlanges;
import com.cwjn.skada.data.gen.weapon.profile.ShovelHeadProfile.Station;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Bounds;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShovelHeadProfileTest {

  @Test
  void codecRoundTripPreservesReinforcements() {
    ShovelHeadProfile original = createProfile(null);

    var encoded = ShovelHeadProfile.CODEC.encodeStart(JsonOps.INSTANCE, original).result().orElseThrow();
    ShovelHeadProfile decoded = ShovelHeadProfile.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

    assertEquals(1, decoded.getVersion());
    assertTrue(decoded.getReinforcement() != null);
    assertTrue(decoded.getReinforcement().centerRib() != null);
    assertTrue(decoded.getReinforcement().sideFlanges() != null);
    assertEquals("rounded", decoded.getBlade().edge().profile());
  }

  @Test
  void reinforcementsIncreaseVolumeAndLateralBounds() {
    ShovelHeadProfile reinforced = createProfile(null);
    ShovelHeadProfile plain = new ShovelHeadProfile(
        1,
        reinforced.getMount(),
        reinforced.getShoulder(),
        reinforced.getBlade(),
        null,
        null);

    MassProperties reinforcedMass = reinforced.computeMassProperties(1.0, 256);
    MassProperties plainMass = plain.computeMassProperties(1.0, 256);
    Bounds reinforcedBounds = reinforced.localBounds(128);
    Bounds plainBounds = plain.localBounds(128);

    assertTrue(reinforcedMass.volumeCm3() > plainMass.volumeCm3());
    assertTrue(reinforcedBounds.maxY() > plainBounds.maxY());
  }

  @Test
  void reinforcementsImproveStrikeCompliance() {
    ShovelHeadProfile reinforced = createProfile(null);
    ShovelHeadProfile plain = new ShovelHeadProfile(
        1,
        reinforced.getMount(),
        reinforced.getShoulder(),
        reinforced.getBlade(),
        null,
        null);

    assertTrue(reinforced.deriveStrikeComplianceFactor() > plain.deriveStrikeComplianceFactor());
    assertTrue(plain.deriveStrikeComplianceFactor() < 0.9);
  }

  @Test
  void broadRoundedShovelFaceIsGlancingProne() {
    ShovelHeadProfile profile = createProfile(null);

    assertTrue(profile.deriveStrikeGlancingFactor() < 0.95);
    assertTrue(profile.deriveStrikeGlancingFactor() >= 0.72);
  }

  @Test
  void impactOverrideReplacesDerivedStrikeScalars() {
    ImpactOverride override = new ImpactOverride("heel", 4.2, 0.92, 1.08, 1.1);
    ShovelHeadProfile profile = createProfile(override);

    DerivedStrikeGeometry strike = profile.deriveStrikeGeometry();

    assertEquals("heel", strike.contactRegion());
    assertEquals(4.2, strike.effectiveContactAreaCm2(), 1.0e-9);
    assertEquals(0.92, strike.focusFactor(), 1.0e-9);
    assertEquals(1.08, strike.rigidity(), 1.0e-9);
    assertEquals(1.1, strike.stability(), 1.0e-9);
    assertTrue(strike.contactPointX() > profile.bladeStartX());
  }

  @Test
  void invalidSchemaValuesAreRejected() {
    IllegalArgumentException badStations = assertThrows(IllegalArgumentException.class,
        () -> new Blade(18.0, List.of(
            new Station(0.6, 5.0, 0.3, 0.4, 0.0),
            new Station(0.2, 6.0, 0.2, 0.6, 0.1)), new Edge("rounded", 1.2, 1.4)));
    assertTrue(badStations.getMessage().contains("monotonic"));

    IllegalArgumentException badMount = assertThrows(IllegalArgumentException.class,
        () -> new Mount("socket", 6.0, 2.8, 2.2, 3.2, 1.9, 3.3, 1.4, 0.22, 1.6, null, null, null));
    assertTrue(badMount.getMessage().contains("innerWidth"));

    IllegalArgumentException badImpact = assertThrows(IllegalArgumentException.class,
        () -> new ImpactOverride("slash", 1.0, 1.0, 1.0, 1.0));
    assertTrue(badImpact.getMessage().contains("contactRegion"));
  }

  private static ShovelHeadProfile createProfile(ImpactOverride impactOverride) {
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
        new Reinforcement(
            new CenterRib(0.08, 0.78, 0.35, 1.4),
            new SideFlanges(0.18, 0.9, 0.2, 0.65),
            null),
        impactOverride);
  }
}