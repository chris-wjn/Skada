import com.cwjn.skada.data.gen.weapon.parts.AxeHead;
import com.cwjn.skada.data.gen.weapon.parts.Blade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AxeHeadTest {

  private static final double EPS = 1e-6;

  @Test
  void testSimpleRectangleNoBevelNoHole() {
    AxeHead a = new AxeHead(
            10.0, 10.0, // eyeLength, eyeHeight
            20.0, 20.0, // cheekLength, cheekHeight
            0.0, 0.0, // beardHeight, beardTipDistance
            0.0, 0.0, // toeHeight, toeTipDistance
            5.0, // eyeThickness
            0.0, 0.0, // eyeHole semi axes
            new Blade.Bevel(0.0, 1.0), // primary bevel
            null // edge bevel
    );

    double expectedPlanArea = 10.0 * 10.0 + 20.0 * 20.0; // 100 + 400 = 500
    double expectedVolume = expectedPlanArea * 5.0; // 2500
    Assertions.assertEquals(expectedVolume, a.getVolume(), EPS);
  }

  @Test
  void testBeardWithPrimaryBevel() {
    AxeHead a = new AxeHead(
            10.0, 10.0,
            20.0, 20.0,
            10.0, 5.0,
            0.0, 0.0,
            5.0,
            0.0, 0.0,
            new Blade.Bevel(0.25, 1.0),
            null
    );

    double eyeArea = 10.0 * 10.0; // 100
    double cheekArea = 20.0 * 20.0; // 400
    double beardArea = 0.5 * 5.0 * 10.0 + 0.5 * (20.0 - 5.0) * 10.0; // uses tip distance
    double nonEyeArea = cheekArea + beardArea; // 500
    double coeff = Blade.getSuperEllipseArea(0.5, 0.5, 1.0); // coefficient for a=b=0.5
    double K = (1.0 - 0.25) + 0.25 * coeff; // 0.875
    double expected = eyeArea * 5.0 + nonEyeArea * 5.0 * K;
    Assertions.assertEquals(expected, a.getVolume(), EPS);
  }

  @Test
  void testBeardBevelAndEyeHole() {
    AxeHead a = new AxeHead(
            10.0, 10.0,
            20.0, 20.0,
            10.0, 5.0,
            0.0, 0.0,
            5.0,
            3.0, 2.0,
            new Blade.Bevel(0.25, 1.0),
            null
    );

    double eyeArea = 10.0 * 10.0; // 100
    double cheekArea = 20.0 * 20.0; // 400
    double beardArea = 0.5 * 5.0 * 10.0 + 0.5 * (20.0 - 5.0) * 10.0; // uses tip distance
    double nonEyeArea = cheekArea + beardArea; // 500
    double coeff = Blade.getSuperEllipseArea(0.5, 0.5, 1.0);
    double K = (1.0 - 0.25) + 0.25 * coeff; // 0.875
    double gross = eyeArea * 5.0 + nonEyeArea * 5.0 * K; // beard+cheek beveled, eye not beveled
    double hole = Math.PI * 3.0 * 2.0 * 5.0; // elliptical cylinder
    double expected = gross - hole;
    System.out.println(a.getVolume());
    Assertions.assertEquals(expected, a.getVolume(), 1e-5);
  }

  @Test
  void testZeroThickness() {
    AxeHead a = new AxeHead(
            10.0, 10.0,
            20.0, 20.0,
            5.0, 0.0,
            5.0, 0.0,
            0.0,
            3.0, 2.0,
            new Blade.Bevel(0.5, 1.0),
            null
    );

    Assertions.assertEquals(0.0, a.getVolume(), EPS);
  }

  @Test
  void testNegativeTipDistancesNoClipping() {
    AxeHead a = new AxeHead(
            10.0, 10.0,
            20.0, 20.0,
            10.0, -5.0, // negative tip distance
            8.0, -12.0, // negative toe tip distance
            5.0,
            0.0, 0.0,
            new Blade.Bevel(0.25, 1.0),
            null
    );

    double cLen = 20.0;
    double eyeArea = 10.0 * 10.0; // 100
    double cheekArea = cLen * 20.0; // 400
    double beardArea = 0.5 * cLen * 10.0; // full base, no clipping = 100
    double toeArea = 0.5 * cLen * 8.0; // 80

    double coeff = Blade.getSuperEllipseArea(0.5, 0.5, 1.0);
    double K = (1.0 - 0.25) + 0.25 * coeff; // 0.875

    double expected = eyeArea * 5.0 + (cheekArea + beardArea + toeArea) * 5.0 * K;
    Assertions.assertEquals(expected, a.getVolume(), 1e-6);
  }

  @Test
  void testPointOfBalanceSimple() {
    // Simple two-rectangle case: eye 10x10, cheek 20x10, thickness 5, no bevel, no hole
    AxeHead a = new AxeHead(
            10.0, 10.0,
            20.0, 10.0,
            0.0, 0.0,
            0.0, 0.0,
            5.0,
            0.0, 0.0,
            new Blade.Bevel(0.0, 1.0),
            null
    );

    // manual expected PoB: eyeVol=100*5 at x=5, cheekVol=200*5 at x=20 => PoB = (100*5*5 + 200*5*20)/(1500) = 15.0
    double expected = 15.0;
    Assertions.assertEquals(expected, a.getPointOfBalance(), 1e-9);
  }

  @Test
  void testPointOfBalanceWithTriangles() {
    // Eye 10x10, cheek 20x10, beard 10 height, toe 5 height, thickness 5, no bevel
    AxeHead a = new AxeHead(
            10.0, 10.0,
            20.0, 10.0,
            10.0, 10.0,
            5.0, 10.0,
            5.0,
            0.0, 0.0,
            new Blade.Bevel(0.0, 1.0),
            null
    );

    double eLen = 10.0; double cLen = 20.0; double t = 5.0;
    double eyeArea = eLen * 10.0; // 100
    double cheekArea = cLen * 10.0; // 200
    double beardArea = 0.5 * cLen * 10.0; // 100
    double toeArea = 0.5 * cLen * 5.0; // 50

    double eyeVol = eyeArea * t; double cheekVol = cheekArea * t; double beardVol = beardArea * t; double toeVol = toeArea * t;
    double eyeCentroid = eLen / 2.0; //5
    double cheekCentroid = eLen + cLen / 2.0; //20
    double beardApexX = eLen + 10.0; double toeApexX = eLen + 10.0; // apex at center of cheek
    double beardCentroid = (eLen + (eLen + cLen) + beardApexX) / 3.0;
    double toeCentroid = (eLen + (eLen + cLen) + toeApexX) / 3.0;

    double first = eyeVol * eyeCentroid + cheekVol * cheekCentroid + beardVol * beardCentroid + toeVol * toeCentroid;
    double totalVol = eyeVol + cheekVol + beardVol + toeVol;
    double expected = first / totalVol;
    Assertions.assertEquals(expected, a.getPointOfBalance(), 1e-9);
  }

  @Test
  void testPointOfBalanceWithEyeHole() {
    // Use the simple two-rectangle case but subtract an eye hole
    AxeHead a = new AxeHead(
            10.0, 10.0,
            20.0, 10.0,
            0.0, 0.0,
            0.0, 0.0,
            5.0,
            2.0, 1.0,
            new Blade.Bevel(0.0, 1.0),
            null
    );

    double eLen = 10.0; double cLen = 20.0; double t = 5.0;
    double eyeArea = eLen * 10.0; //100
    double cheekArea = cLen * 10.0; //200
    double eyeVol = eyeArea * t; double cheekVol = cheekArea * t;
    double eyeCentroid = eLen / 2.0; //5
    double cheekCentroid = eLen + cLen / 2.0; //20

    double holeVol = Math.PI * 2.0 * 1.0 * t; // pi * a * b * thickness
    double holeCentroid = eyeCentroid;

    double first = eyeVol * eyeCentroid + cheekVol * cheekCentroid - holeVol * holeCentroid;
    double totalVol = eyeVol + cheekVol - holeVol;
    double expected = first / totalVol;
    Assertions.assertEquals(expected, a.getPointOfBalance(), 1e-9);
  }

}
