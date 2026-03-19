package com.cwjn.skada.data.gen.weapon.mace;

import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.Core;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.CoreStation;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.DerivedStrikeGeometry;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.Flanges;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.ImpactOverride;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.KnobRing;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.Mount;
import com.cwjn.skada.data.gen.weapon.profile.MaceHeadProfile.SpikeRing;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Bounds;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaceHeadProfileTest {

  @Test
  void codecRoundTripPreservesFeatureCollections() {
    MaceHeadProfile original = createFeaturedProfile(null);

    var encoded = MaceHeadProfile.CODEC.encodeStart(JsonOps.INSTANCE, original).result().orElseThrow();
    MaceHeadProfile decoded = MaceHeadProfile.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

    assertEquals(1, decoded.getVersion());
    assertTrue(decoded.getFlanges() != null);
    assertEquals(1, decoded.getKnobRings().size());
    assertEquals(1, decoded.getSpikeRings().size());
    assertEquals(10.5, decoded.getCore().length(), 1.0e-9);
  }

  @Test
  void featuresIncreaseVolumeAndRadialBounds() {
    MaceHeadProfile smooth = createSmoothProfile();
    MaceHeadProfile featured = createFeaturedProfile(null);

    MassProperties smoothMass = smooth.computeMassProperties(1.0, 256);
    MassProperties featuredMass = featured.computeMassProperties(1.0, 256);
    Bounds smoothBounds = smooth.localBounds(128);
    Bounds featuredBounds = featured.localBounds(128);

    assertTrue(featuredMass.volumeCm3() > smoothMass.volumeCm3());
    assertTrue(featuredMass.centerOfMass().x() > smoothMass.centerOfMass().x());
    assertTrue(featuredBounds.maxY() > smoothBounds.maxY());
    assertTrue(featuredBounds.maxX() > smoothBounds.maxX());
  }

  @Test
  void impactOverrideReplacesDerivedStrikeScalars() {
    ImpactOverride override = new ImpactOverride(1.4, 1.7, 1.2);
    MaceHeadProfile profile = createFeaturedProfile(override);

    DerivedStrikeGeometry strike = profile.deriveStrikeGeometry();

    assertEquals("spike", strike.featureType());
    assertEquals(1.4, strike.effectiveContactAreaCm2(), 1.0e-9);
    assertEquals(1.7, strike.focusFactor(), 1.0e-9);
    assertEquals(1.2, strike.rigidity(), 1.0e-9);
    assertTrue(strike.contactPointX() > profile.coreStartX());
  }

  @Test
  void invalidSchemaValuesAreRejected() {
    IllegalArgumentException badStations = assertThrows(IllegalArgumentException.class,
        () -> new Core(8.0, List.of(
            new CoreStation(0.6, 2.0),
            new CoreStation(0.4, 1.8))));
    assertTrue(badStations.getMessage().contains("monotonic"));

    IllegalArgumentException badMount = assertThrows(IllegalArgumentException.class,
        () -> new Mount("socket", 4.0, 1.0, 1.2, 0.0, 0.0, 0.0));
    assertTrue(badMount.getMessage().contains("innerRadius"));

    IllegalArgumentException badFlange = assertThrows(IllegalArgumentException.class,
        () -> new Flanges(6, 0.2, 0.9, 0.8, 0.6, 0.7, 1.6, 0.0));
    assertTrue(badFlange.getMessage().contains("tipWidth"));
  }

  private static MaceHeadProfile createSmoothProfile() {
    return new MaceHeadProfile(
        1,
        new Mount("socket", 5.2, 1.2, 0.8, 0.0, 0.0, 1.2),
        new Core(10.5, List.of(
            new CoreStation(0.0, 1.8),
            new CoreStation(0.18, 2.45),
            new CoreStation(0.55, 2.75),
            new CoreStation(0.88, 2.35),
            new CoreStation(1.0, 1.85))),
        null,
        List.of(),
        List.of(),
        null);
  }

  private static MaceHeadProfile createFeaturedProfile(ImpactOverride impactOverride) {
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
        List.of(new KnobRing(0.98, 8, 0.6, 0.55)),
        List.of(new SpikeRing(0.72, 8, 1.4, 0.18, 0.95, 10.0)),
        impactOverride);
  }
}