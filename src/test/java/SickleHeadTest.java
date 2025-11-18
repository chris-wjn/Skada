import com.cwjn.skada.data.gen.weapon.parts.Blade;
import com.cwjn.skada.data.gen.weapon.parts.SickleHead;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SickleHeadTest {

  private static final double EPS = 1e-6;

  /**
   * Test a simple sickle with equal base and tip distances (uniform width).
   * This should behave like a curved strip of constant width.
   */
  @Test
  void testUniformWidthSickle() {
    // Create a sickle where spine and blade have similar curvature
    // and the base-to-blade distance is the same at both ends
    SickleHead sickle = new SickleHead(
            100.0, // spineArcLength
            95.0,  // spineChordLength
            10.0,  // spineSagittaHeight
            100.0, // bladeArcLength
            95.0,  // bladeChordLength
            10.0,  // bladeSagittaHeight
            20.0,  // spineBaseToBladeBaseDistance
            20.0,  // spineTipToBladeTipDistance (same as base = uniform)
            5.0,   // spineThickness
            new Blade.Bevel(0.0, 1.0), // no bevel effect
            null   // no edge bevel
    );

    double volume = sickle.getVolume();

    // Volume should be positive
    Assertions.assertTrue(volume > 0, "Volume should be positive");

    // For a uniform width sickle, rough estimate: arcLength * width * thickness
    // Expected ~100 * 20 * 5 = 10000 mm³ (rough approximation)
    Assertions.assertTrue(volume > 8000 && volume < 12000,
            "Volume should be approximately 10000 mm³, got: " + volume);
  }

  /**
   * Test a sickle with converging width (wider at base, narrower at tip).
   */
  @Test
  void testConvergingSickle() {
    SickleHead sickle = new SickleHead(
            100.0, // spineArcLength
            90.0,  // spineChordLength
            15.0,  // spineSagittaHeight
            100.0, // bladeArcLength
            90.0,  // bladeChordLength
            15.0,  // bladeSagittaHeight
            30.0,  // spineBaseToBladeBaseDistance (wide at base)
            10.0,  // spineTipToBladeTipDistance (narrow at tip)
            5.0,   // spineThickness
            new Blade.Bevel(0.0, 1.0),
            null
    );

    double volume = sickle.getVolume();

    // Volume should be positive but less than a uniform 30mm width
    Assertions.assertTrue(volume > 0, "Volume should be positive");
    System.out.println(volume);
    Assertions.assertTrue(volume < 100.0 * 30.0 * 5.0,
            "Volume should be less than max possible (arcLength * maxWidth * thickness)");
  }

  /**
   * Test a sickle with diverging width (narrower at base, wider at tip).
   */
  @Test
  void testDivergingSickle() {
    SickleHead sickle = new SickleHead(
            80.0,  // spineArcLength
            75.0,  // spineChordLength
            8.0,   // spineSagittaHeight
            80.0,  // bladeArcLength
            75.0,  // bladeChordLength
            8.0,   // bladeSagittaHeight
            10.0,  // spineBaseToBladeBaseDistance (narrow at base)
            25.0,  // spineTipToBladeTipDistance (wide at tip)
            4.0,   // spineThickness
            new Blade.Bevel(0.0, 1.0),
            null
    );

    double volume = sickle.getVolume();

    Assertions.assertTrue(volume > 0, "Volume should be positive");
    // Average width ~17.5mm, so volume ~ 80 * 17.5 * 4 = 5600
    System.out.println(volume);
    Assertions.assertTrue(volume > 4000 && volume < 8000,
            "Volume should be approximately 5600 mm³, got: " + volume);
  }

  /**
   * Test zero thickness - should return zero volume.
   */
  @Test
  void testZeroThickness() {
    SickleHead sickle = new SickleHead(
            100.0, 90.0, 10.0,
            100.0, 90.0, 10.0,
            20.0, 20.0,
            0.0,   // zero thickness
            new Blade.Bevel(0.0, 1.0),
            null
    );

    Assertions.assertEquals(0.0, sickle.getVolume(), EPS,
            "Volume should be zero when thickness is zero");
  }

  /**
   * Test with primary bevel - should reduce volume.
   */
  @Test
  void testWithPrimaryBevel() {
    // Create two identical sickles, one with bevel and one without
    double spineArc = 100.0, spineChord = 95.0, spineSag = 10.0;
    double bladeArc = 100.0, bladeChord = 95.0, bladeSag = 10.0;
    double baseToBase = 20.0, tipToTip = 20.0, thickness = 5.0;

    SickleHead noBevel = new SickleHead(
            spineArc, spineChord, spineSag,
            bladeArc, bladeChord, bladeSag,
            baseToBase, tipToTip, thickness,
            new Blade.Bevel(0.0, 1.0), // no bevel
            null
    );

    SickleHead withBevel = new SickleHead(
            spineArc, spineChord, spineSag,
            bladeArc, bladeChord, bladeSag,
            baseToBase, tipToTip, thickness,
            new Blade.Bevel(0.5, 1.0), // 50% bevel
            null
    );

    double noBevelVolume = noBevel.getVolume();
    double withBevelVolume = withBevel.getVolume();

    // With bevel should have less volume
    Assertions.assertTrue(withBevelVolume < noBevelVolume,
            "Volume with bevel should be less than without bevel");

    // Calculate expected reduction factor
    double p = 0.5;
    double r = 1.0;
    double superCoeff = Blade.superEllipseCoefficient(r);
    double expectedFactor = (1.0 - p) + p * superCoeff;

    System.out.println(noBevelVolume);
    System.out.println(withBevelVolume);

    Assertions.assertEquals(noBevelVolume * expectedFactor, withBevelVolume, EPS * 100,
            "Bevel should reduce volume by expected factor");
  }

  /**
   * Test point of balance for a uniform sickle - should be near the center.
   */
  @Test
  void testPointOfBalanceUniform() {
    SickleHead sickle = new SickleHead(
            100.0, 90.0, 10.0,
            100.0, 90.0, 10.0,
            20.0, 20.0, // uniform width
            5.0,
            new Blade.Bevel(0.0, 1.0),
            null
    );

    double pob = sickle.getPointOfBalance();
    double chordLength = 90.0;

    // For uniform distribution, PoB should be near the center
    Assertions.assertTrue(pob > chordLength * 0.4 && pob < chordLength * 0.6,
            "Point of balance should be near center for uniform sickle, got: " + pob);
  }

  /**
   * Test point of balance for a front-heavy sickle (wider at tip).
   */
  @Test
  void testPointOfBalanceFrontHeavy() {
    SickleHead sickle = new SickleHead(
            100.0, 90.0, 10.0,
            100.0, 90.0, 10.0,
            10.0,  // narrow at base
            30.0,  // wide at tip (front-heavy)
            5.0,
            new Blade.Bevel(0.0, 1.0),
            null
    );

    double pob = sickle.getPointOfBalance();
    double chordLength = 90.0;

    // Front-heavy should have PoB past the center
    Assertions.assertTrue(pob > chordLength * 0.5,
            "Point of balance should be past center for front-heavy sickle, got: " + pob);
  }

  /**
   * Test point of balance for a rear-heavy sickle (wider at base).
   */
  @Test
  void testPointOfBalanceRearHeavy() {
    SickleHead sickle = new SickleHead(
            100.0, 90.0, 10.0,
            100.0, 90.0, 10.0,
            30.0,  // wide at base (rear-heavy)
            10.0,  // narrow at tip
            5.0,
            new Blade.Bevel(0.0, 1.0),
            null
    );

    double pob = sickle.getPointOfBalance();
    double chordLength = 90.0;

    // Rear-heavy should have PoB before the center
    Assertions.assertTrue(pob < chordLength * 0.5,
            "Point of balance should be before center for rear-heavy sickle, got: " + pob);
  }

  /**
   * Test with different spine and blade curvatures.
   */
  @Test
  void testDifferentCurvatures() {
    SickleHead sickle = new SickleHead(
            120.0, // spineArcLength (more curved)
            90.0,  // spineChordLength
            25.0,  // spineSagittaHeight (deeper curve)
            100.0, // bladeArcLength (less curved)
            90.0,  // bladeChordLength (same chord)
            10.0,  // bladeSagittaHeight (shallower curve)
            15.0,  // spineBaseToBladeBaseDistance
            15.0,  // spineTipToBladeTipDistance
            5.0,   // spineThickness
            new Blade.Bevel(0.0, 1.0),
            null
    );

    double volume = sickle.getVolume();

    // Volume should be positive and reasonable
    Assertions.assertTrue(volume > 0, "Volume should be positive");

    // Different curvatures mean the actual width varies along the blade
    // but volume should still be in a reasonable range
    Assertions.assertTrue(volume > 5000 && volume < 20000,
            "Volume should be in reasonable range for different curvatures, got: " + volume);
  }

  /**
   * Test a very small sickle to verify calculations work at small scales.
   */
  @Test
  void testSmallSickle() {
    SickleHead sickle = new SickleHead(
            10.0,  // spineArcLength
            9.5,   // spineChordLength
            1.0,   // spineSagittaHeight
            10.0,  // bladeArcLength
            9.5,   // bladeChordLength
            1.0,   // bladeSagittaHeight
            2.0,   // spineBaseToBladeBaseDistance
            2.0,   // spineTipToBladeTipDistance
            0.5,   // spineThickness
            new Blade.Bevel(0.0, 1.0),
            null
    );

    double volume = sickle.getVolume();

    Assertions.assertTrue(volume > 0, "Small sickle should have positive volume");
    // Expected ~10 * 2 * 0.5 = 10 mm³
    Assertions.assertTrue(volume > 5 && volume < 20,
            "Small sickle volume should be approximately 10 mm³, got: " + volume);
  }

  /**
   * Test a realistic minecraft hoe sickle blade.
   */
  @Test
  void testRealisticHoeBlade() {
    // Based on typical Minecraft hoe dimensions
    SickleHead hoeBlade = new SickleHead(
            180.0, // spineArcLength (curved blade)
            160.0, // spineChordLength
            30.0,  // spineSagittaHeight (pronounced curve)
            180.0, // bladeArcLength
            160.0, // bladeChordLength
            30.0,  // bladeSagittaHeight
            40.0,  // spineBaseToBladeBaseDistance (decent width)
            20.0,  // spineTipToBladeTipDistance (tapers to point)
            8.0,   // spineThickness (reasonable tool thickness)
            new Blade.Bevel(0.3, 1.0), // some beveling
            new Blade.EdgeBevel(22.5, 180.0, 5.0)
    );

    double volume = hoeBlade.getVolume();
    double pob = hoeBlade.getPointOfBalance();

    System.out.println("Realistic hoe blade volume: " + volume + " mm³");
    System.out.println("Realistic hoe blade point of balance: " + pob + " mm from base");

    Assertions.assertTrue(volume > 0, "Hoe blade should have positive volume");
    Assertions.assertTrue(pob > 0 && pob < 160.0,
            "Point of balance should be within chord length");

    // Hoe is slightly front-heavy due to taper, so PoB should be slightly past center
    Assertions.assertTrue(pob > 70.0 && pob < 90.0,
            "Hoe blade PoB should be near center, slightly forward, got: " + pob);
  }

  /**
   * Test symmetry: two sickles with swapped base/tip distances should have
   * the same volume but different points of balance.
   */
  @Test
  void testSymmetry() {
    SickleHead sickle1 = new SickleHead(
            100.0, 90.0, 10.0,
            100.0, 90.0, 10.0,
            20.0, 30.0, // base narrow, tip wide
            5.0,
            new Blade.Bevel(0.0, 1.0),
            null
    );

    SickleHead sickle2 = new SickleHead(
            100.0, 90.0, 10.0,
            100.0, 90.0, 10.0,
            30.0, 20.0, // base wide, tip narrow (swapped)
            5.0,
            new Blade.Bevel(0.0, 1.0),
            null
    );

    // Volumes should be equal (same total material, just distributed differently)
    Assertions.assertEquals(sickle1.getVolume(), sickle2.getVolume(), EPS * 100,
            "Swapped sickles should have equal volumes");

    // Points of balance should be different and mirrored
    double pob1 = sickle1.getPointOfBalance();
    double pob2 = sickle2.getPointOfBalance();
    double center = 90.0 / 2.0;

    Assertions.assertNotEquals(pob1, pob2, EPS,
            "Points of balance should differ for swapped sickles");

    // They should be roughly equidistant from center (but on opposite sides)
    double dist1 = Math.abs(pob1 - center);
    double dist2 = Math.abs(pob2 - center);
    Assertions.assertEquals(dist1, dist2, 5.0, // allow some tolerance due to curved geometry
            "Distances from center should be similar for swapped sickles");
  }

  /**
   * Test edge case: spine tip to blade tip distance is zero (tip comes to a point).
   */
  @Test
  void testZeroTipDistance() {
    SickleHead sickle = new SickleHead(
            100.0, 90.0, 15.0,
            100.0, 90.0, 15.0,
            25.0,  // wide at base
            0.0,   // point at tip
            5.0,
            new Blade.Bevel(0.0, 1.0),
            null
    );

    double volume = sickle.getVolume();
    double pob = sickle.getPointOfBalance();

    Assertions.assertTrue(volume > 0, "Volume should be positive even with pointed tip");

    // PoB should be toward the base since mass is concentrated there
    Assertions.assertTrue(pob < 90.0 / 2.0,
            "Point of balance should be toward base for pointed tip sickle, got: " + pob);
  }

  /**
   * Test consistency: getVolume should return the same value on multiple calls.
   */
  @Test
  void testVolumeConsistency() {
    SickleHead sickle = new SickleHead(
            100.0, 90.0, 10.0,
            100.0, 90.0, 10.0,
            20.0, 20.0, 5.0,
            new Blade.Bevel(0.25, 1.0),
            null
    );

    double vol1 = sickle.getVolume();
    double vol2 = sickle.getVolume();
    double vol3 = sickle.getVolume();

    Assertions.assertEquals(vol1, vol2, EPS, "Volume should be consistent across calls");
    Assertions.assertEquals(vol2, vol3, EPS, "Volume should be consistent across calls");
  }

  /**
   * Test consistency: getPointOfBalance should return the same value on multiple calls.
   */
  @Test
  void testPointOfBalanceConsistency() {
    SickleHead sickle = new SickleHead(
            100.0, 90.0, 10.0,
            100.0, 90.0, 10.0,
            20.0, 25.0, 5.0,
            new Blade.Bevel(0.25, 1.0),
            null
    );

    double pob1 = sickle.getPointOfBalance();
    double pob2 = sickle.getPointOfBalance();
    double pob3 = sickle.getPointOfBalance();

    Assertions.assertEquals(pob1, pob2, EPS, "Point of balance should be consistent across calls");
    Assertions.assertEquals(pob2, pob3, EPS, "Point of balance should be consistent across calls");
  }
}

