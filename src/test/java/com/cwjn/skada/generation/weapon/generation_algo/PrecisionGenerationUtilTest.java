package com.cwjn.skada.generation.weapon.generation_algo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.cwjn.skada.generation.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.generation.weapon.generation_algo.context.AssemblyPhysicsSnapshot;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackDeliverySnapshot;
import com.cwjn.skada.generation.weapon.generation_algo.context.ContactSnapshotStrike;
import com.cwjn.skada.generation.weapon.generation_algo.context.MaterialResponseSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrecisionGenerationUtilTest {

  private static final double EPS = 1.0e-6;

  @Test
  void slashGeometryAnchorsStayResponsiveAcrossRealisticEdges() {
    assertEquals(46.0, PrecisionGenerationUtil.edgeRadiusToPrecisionBase(2.5), EPS);
    assertEquals(46.0 * Math.exp(-2.5 / 4.0), PrecisionGenerationUtil.edgeRadiusToPrecisionBase(5.0), EPS);
    assertEquals(46.0 * Math.exp(-4.5 / 4.0), PrecisionGenerationUtil.edgeRadiusToPrecisionBase(7.0), EPS);
    assertEquals(46.0 * Math.exp(-7.5 / 4.0), PrecisionGenerationUtil.edgeRadiusToPrecisionBase(10.0), EPS);

    assertEquals(1.15, PrecisionGenerationUtil.slashBevelAngleFactor(8.0), EPS);
    assertEquals(1.0, PrecisionGenerationUtil.slashBevelAngleFactor(14.0), EPS);
    assertEquals(0.85, PrecisionGenerationUtil.slashBevelAngleFactor(20.0), EPS);
  }

  @Test
  void thrustGeometryAnchorsUseSharedScaleAndSoftPowerCurves() {
    assertEquals(7.5, PrecisionGenerationUtil.pointTaperToPrecisionBase(0.0), EPS);
    assertEquals(50.0, PrecisionGenerationUtil.pointTaperToPrecisionBase(1.0), EPS);
    assertTrue(PrecisionGenerationUtil.pointTaperToPrecisionBase(0.92) > 44.0);

    assertEquals(1.0, PrecisionGenerationUtil.thrustWidthFactor(2.5), EPS);
    assertEquals(Math.sqrt(0.35 / 0.22), PrecisionGenerationUtil.thrustThicknessFactor(0.22), EPS);
    assertTrue(PrecisionGenerationUtil.thrustWidthFactor(6.0) > 0.55);
    assertTrue(PrecisionGenerationUtil.thrustTipAspectFactor(23.2 / 0.9) < 1.55);
  }

  @Test
  void strikeFocusProducesAFlushProbabilityAgainstDeliveryError() {
    assertEquals(1.0, PrecisionGenerationUtil.strikeThetaMaxDegrees(0.82), EPS);
    assertEquals(4.0, PrecisionGenerationUtil.strikeThetaMaxDegrees(1.0), 0.2);
    assertEquals(7.0, PrecisionGenerationUtil.strikeThetaMaxDegrees(1.3), 0.2);
    assertEquals(11.0, PrecisionGenerationUtil.strikeThetaMaxDegrees(1.9), EPS);

    double flatFace = PrecisionGenerationUtil.strikeFlushProbability(1.0, 9.0);
    double flangedFace = PrecisionGenerationUtil.strikeFlushProbability(8.0, 9.0);
    double spikeFace = PrecisionGenerationUtil.strikeFlushProbability(11.0, 8.5);
    assertTrue(flatFace > 0.08 && flatFace < 0.10);
    assertTrue(flangedFace > 0.55 && flangedFace < 0.70);
    assertTrue(spikeFace > 0.80 && spikeFace < 0.90);
    assertEquals(8.5, PrecisionGenerationUtil.strikeAngularErrorSigmaDegrees(0.0, 0.0, 0.0, 0.0), EPS);
    assertEquals(15.0, PrecisionGenerationUtil.strikeAngularErrorSigmaDegrees(1.0, 1.0, 1.0, 1.0), EPS);
    assertTrue(PrecisionGenerationUtil.strikeAreaInteractionFactor(12.9, 0.82)
      < PrecisionGenerationUtil.strikeAreaInteractionFactor(12.9, 1.56));
  }

  @Test
  void strikeSnapshotPreservesSpikeFocusForPrecision() {
    StrikeCapable spike = mock(StrikeCapable.class);
    when(spike.strikeFaceGeometryFocus()).thenReturn(1.9);
    when(spike.effectiveContactAreaCm2()).thenReturn(0.1);

    assertEquals(1.9, ContactSnapshotStrike.fromPart(spike).strikeFaceGeometryFocus(), EPS);
  }

  @Test
  void distributionFixtureLoadsTheShippedAssemblyAndTridentPartJsons() throws Exception {
    for (String name : List.of("axe", "hoe", "longsword", "mace", "pickaxe", "shovel", "spear", "sword", "trident")) {
      String path = "/data/skada/generator_data/weapon/weapon_assembly/" + name + ".json";
      try (var stream = getClass().getResourceAsStream(path)) {
        assertNotNull(stream, "Missing shipped assembly fixture " + path);
        JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray parts = json.getAsJsonArray("parts");
        assertNotNull(parts, "Assembly fixture must contain parts: " + path);
        assertTrue(!parts.isEmpty(), "Assembly fixture must not be empty: " + path);
      }
    }

    for (String name : List.of("trident_main_spike", "trident_side_spike")) {
      String path = "/data/skada/generator_data/weapon/part/" + name + ".json";
      try (var stream = getClass().getResourceAsStream(path)) {
        assertNotNull(stream, "Missing shipped part fixture " + path);
        assertTrue(JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).isJsonObject());
      }
    }
  }

  @Test
  void deterministicShippedProfilePopulationIsBoundedMidCenteredAndEmergent() {
    List<Double> allScores = new ArrayList<>();
    List<Double> slashScores = new ArrayList<>();
    List<Double> thrustScores = new ArrayList<>();
    List<Double> strikeScores = new ArrayList<>();

    for (Profile profile : SHIPPED_PROFILE_ANCHORS) {
      for (int materialIndex = 0; materialIndex < 5; materialIndex++) {
        double hardness = 0.30 + 0.16 * materialIndex;
        double toughness = 0.35 + 0.14 * materialIndex;
        double flexibility = 0.18 + 0.10 * materialIndex;
        for (int seed = 0; seed < 32; seed++) {
          double deliveryVariation = triangular(seed, 32);
          double balanceMismatch = 0.03 + 0.22 * deliveryVariation;
          double overcommitment = 0.02 + 0.18 * deliveryVariation;
          double inertia = 0.04 + 0.16 * deliveryVariation;
          double effectiveMass = 0.18 + 0.40 * deliveryVariation;
          double assemblyInertia = 0.04 + 0.20 * deliveryVariation;
          MaterialResponseSnapshot material = material(hardness, toughness, flexibility);
          AssemblyPhysicsSnapshot assembly = assembly(assemblyInertia);
          AttackDeliverySnapshot delivery = delivery(
            balanceMismatch, overcommitment, effectiveMass, profile.rotationalThrust);

          if (profile.hasSlash()) {
            double control = PrecisionGenerationUtil.controlFactor(delivery, assembly);
            double geometry = PrecisionGenerationUtil.edgeRadiusToPrecisionBase(profile.edgeRadiusNm)
              * PrecisionGenerationUtil.slashBevelAngleFactor(profile.bevelAngleDeg);
            double score = PrecisionGenerationUtil.slashScore(
              geometry, PrecisionGenerationUtil.slashMaterialModifier(material), control);
            slashScores.add(score);
            allScores.add(score);
          }
          if (profile.hasThrust()) {
            double thrustEffectiveMass = profile.rotationalThrust ? effectiveMass : 1.0;
            AttackDeliverySnapshot thrustDelivery = delivery(
              balanceMismatch, overcommitment, thrustEffectiveMass, profile.rotationalThrust);
            double taperBase = PrecisionGenerationUtil.pointTaperToPrecisionBase(profile.pointTaper);
            double widthFactor = PrecisionGenerationUtil.thrustWidthFactor(profile.widthAtPointBaseCm);
            double thicknessFactor = PrecisionGenerationUtil.thrustThicknessFactor(profile.thicknessAtPointBaseCm);
            double geometryFactor = 0.45 * widthFactor + 0.55 * thicknessFactor;
            double tipAspect = profile.tipLengthCm
              / Math.max(profile.widthAtPointBaseCm, profile.thicknessAtPointBaseCm);
            double geometry = (0.70 * taperBase + 0.30 * taperBase * geometryFactor)
              * PrecisionGenerationUtil.thrustTipAspectFactor(tipAspect);
            double motion = profile.rotationalThrust
              ? Math.max(0.26, Math.min(0.72,
                (0.72 - 0.55 * balanceMismatch - 0.40 * overcommitment - 0.22 * thrustEffectiveMass)
                  * profile.alignmentEfficiency))
              : 1.0;
            double score = PrecisionGenerationUtil.thrustScore(
              geometry,
              PrecisionGenerationUtil.thrustMaterialModifier(material),
              PrecisionGenerationUtil.controlFactor(thrustDelivery, assembly),
              motion);
            thrustScores.add(score);
            allScores.add(score);
          }
          if (profile.hasStrike()) {
            double theta = PrecisionGenerationUtil.strikeThetaMaxDegrees(profile.strikeFocus);
            double sigma = PrecisionGenerationUtil.strikeAngularErrorSigmaDegrees(
              balanceMismatch, overcommitment, assemblyInertia, effectiveMass);
            double secondary = 0.56 * profile.strikeRigidity + 0.44 * profile.strikeStability;
            double score = PrecisionGenerationUtil.strikeScore(
              PrecisionGenerationUtil.strikeFlushProbability(theta, sigma),
              PrecisionGenerationUtil.strikeMaterialModifier(material),
              PrecisionGenerationUtil.strikeFaceIntegrityFactor(secondary),
              PrecisionGenerationUtil.strikeContactQualityFactor(
                profile.strikeStructuralEfficiency, profile.strikeIncidenceEfficiency),
              PrecisionGenerationUtil.strikeRepeatabilityFactor(profile.strikeRepeatability),
              PrecisionGenerationUtil.strikeAreaInteractionFactor(profile.strikeAreaCm2, profile.strikeFocus));
            strikeScores.add(score);
            allScores.add(score);
          }
        }
      }
    }

    assertTrue(slashScores.size() > 100);
    assertTrue(thrustScores.size() > 100);
    assertTrue(strikeScores.size() > 100);
    assertTrue(allScores.stream().allMatch(score -> score >= 1.0 && score <= 50.0));
    assertTrue(allScores.stream().filter(score -> score >= 49.99 || score <= 1.01).count()
      < allScores.size() * 0.10, "Too many scores accumulate at a shared scale endpoint");

    double overallMedian = median(allScores);
    assertTrue(overallMedian >= 15.0 && overallMedian <= 30.0,
      "Population medians: overall=" + overallMedian + ", slash=" + median(slashScores)
        + ", thrust=" + median(thrustScores) + ", strike=" + median(strikeScores));
    assertTrue(median(thrustScores) > median(slashScores), "Thrust should emerge above slash without a type multiplier");
    assertTrue(median(thrustScores) > median(strikeScores),
      "Thrust median " + median(thrustScores) + " should emerge above strike median " + median(strikeScores));

    int[] buckets = new int[10];
    for (double score : allScores) {
      buckets[Math.min(9, Math.max(0, (int) ((score - 1.0) / 4.9)))]++;
    }
    int medianBucket = buckets.length - 1;
    int cumulative = 0;
    for (int i = 0; i < buckets.length; i++) {
      cumulative += buckets[i];
      if (cumulative >= allScores.size() / 2) {
        medianBucket = i;
        break;
      }
    }
    assertTrue(medianBucket >= 2 && medianBucket <= 5,
      "Median histogram bucket should cover the middle scores, got " + medianBucket);
    int largestCentralBucket = Math.max(Math.max(buckets[2], buckets[3]), Math.max(buckets[4], buckets[5]));
    assertTrue(buckets[0] < largestCentralBucket && buckets[9] < largestCentralBucket,
      "Both histogram tails should be smaller than the central mass: " + java.util.Arrays.toString(buckets));
  }

  private static double triangular(int index, int count) {
    double unit = (index + 0.5) / count;
    return 1.0 - Math.abs(2.0 * unit - 1.0);
  }

  private static double median(List<Double> values) {
    List<Double> sorted = values.stream().sorted().toList();
    int middle = sorted.size() / 2;
    return sorted.size() % 2 == 0
      ? (sorted.get(middle - 1) + sorted.get(middle)) / 2.0
      : sorted.get(middle);
  }

  private static MaterialResponseSnapshot material(double hardness, double toughness, double flexibility) {
    MaterialResponseSnapshot material = mock(MaterialResponseSnapshot.class);
    when(material.hardnessNorm()).thenReturn(hardness);
    when(material.toughnessNorm()).thenReturn(toughness);
    when(material.flexibilityNorm()).thenReturn(flexibility);
    return material;
  }

  private static AssemblyPhysicsSnapshot assembly(double inertia) {
    AssemblyPhysicsSnapshot assembly = mock(AssemblyPhysicsSnapshot.class);
    when(assembly.normalizedInertiaCoefficient()).thenReturn(inertia);
    return assembly;
  }

  private static AttackDeliverySnapshot delivery(
      double balanceMismatch,
      double overcommitment,
      double effectiveMass,
      boolean rotationalThrust) {
    AttackDeliverySnapshot delivery = mock(AttackDeliverySnapshot.class);
    when(delivery.balanceMismatch()).thenReturn(balanceMismatch);
    when(delivery.forwardOvercommitment()).thenReturn(overcommitment);
    when(delivery.effectiveMassRatio()).thenReturn(effectiveMass);
    when(delivery.rotationalThrust()).thenReturn(rotationalThrust);
    return delivery;
  }

  /**
   * Geometry anchors are sampled from the shipped assembly/profile set and the
   * observed input ranges documented in unified_precision_scale_plan.md. Seeds
   * and material grades vary deterministically in the test population above.
   */
  private static final List<Profile> SHIPPED_PROFILE_ANCHORS = List.of(
    new Profile("sword", 5.5, 12.0, 0.55, 6.0, 0.72, 9.0,
      Double.NaN, Double.NaN, 1.0, 1.0, 1.0, 1.0, 0.90, 0.98, false),
    new Profile("longsword", 5.0, 11.0, 0.62, 3.2, 0.48, 12.0,
      Double.NaN, Double.NaN, 1.0, 1.0, 1.0, 1.0, 0.92, 0.98, false),
    new Profile("spear", 7.0, 20.0, 0.92, 2.4, 0.22, 16.0,
      Double.NaN, Double.NaN, 1.0, 1.0, 1.0, 1.0, 0.96, 1.0, false),
    new Profile("trident spikes", 7.0, 20.0, 1.0, 1.6, 0.30, 15.0,
      Double.NaN, Double.NaN, 1.0, 1.0, 1.0, 1.0, 0.94, 1.0, false),
    new Profile("pickaxe", Double.NaN, Double.NaN, 0.0, 0.9, 0.18, 23.2,
      0.01, 1.9, 1.15, 1.10, 1.0, 1.0, 0.90, 0.72, true),
    new Profile("hoe", Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
      12.9, 0.82, 0.95, 0.95, 0.90, 0.85, 0.75, 1.0, false),
    new Profile("mace", Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
      1.2, 1.56, 1.15, 1.10, 1.0, 1.0, 1.0, 1.0, false),
    new Profile("shovel", 6.0, 15.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
      6.0, 0.90, 0.98, 0.96, 0.72, 0.90, 0.78, 1.0, false),
    new Profile("pommel", Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
      0.05, 1.30, 1.05, 1.02, 1.0, 1.0, 0.98, 1.0, false),
    new Profile("scythe", 6.0, 16.0, 0.50, 2.4, 0.42, 8.0,
      Double.NaN, Double.NaN, 1.0, 1.0, 1.0, 1.0, 0.88, 0.94, true),
    new Profile("axe", 6.5, 16.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
      2.5, 1.0, 1.05, 1.0, 1.0, 1.0, 0.90, 1.0, false));

  private record Profile(
      String name,
      double edgeRadiusNm,
      double bevelAngleDeg,
      double pointTaper,
      double widthAtPointBaseCm,
      double thicknessAtPointBaseCm,
      double tipLengthCm,
      double strikeAreaCm2,
      double strikeFocus,
      double strikeRigidity,
      double strikeStability,
      double strikeStructuralEfficiency,
      double strikeIncidenceEfficiency,
      double strikeRepeatability,
      double alignmentEfficiency,
      boolean rotationalThrust) {
    boolean hasSlash() { return Double.isFinite(edgeRadiusNm); }
    boolean hasThrust() { return Double.isFinite(pointTaper); }
    boolean hasStrike() { return Double.isFinite(strikeAreaCm2); }
  }
}
