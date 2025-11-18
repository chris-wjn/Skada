// java

import com.cwjn.skada.data.gen.weapon.parts.Blade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BladeTest {

  private static final double EPS = 1e-6;

  private static final Blade testBlade1 = new Blade(false,
    80, 50, null,
          8, 6, null,
          1000,
          new Blade.Bevel(0.66, 1),
          null,
          null,
          null
  );

  @Test
  void testUniformPrism() {
    double length = 1000.0; // mm
    double w0 = 10.0; // mm
    double w1 = 10.0; // mm
    double t0 = 2.0;  // mm
    double t1 = 2.0;  // mm

    double expectedVolume = w0 * t0 * length; // rectangular prism
    double expectedPoB = length / 2.0;

    System.out.println(testBlade1.getVolume());
    System.out.println(testBlade1.getPointOfBalance());
  }

  @Test
  void testSuperEllipseAreaEllipseCase() {
    // r = 2 should reduce to a normal ellipse: area = pi * a * b
    double a = 2.0;
    double b = 3.0;
    double r = 2.0;
    double expected = Math.PI * a * b;
    double actual = Blade.getSuperEllipseArea(a, b, r);
    Assertions.assertEquals(expected, actual, EPS);
  }

  @Test
  void testSuperEllipseAreaInvalidArguments() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> Blade.getSuperEllipseArea(0.0, 1.0, 1.0));
    Assertions.assertThrows(IllegalArgumentException.class, () -> Blade.getSuperEllipseArea(1.0, 0.0, 1.0));
    Assertions.assertThrows(IllegalArgumentException.class, () -> Blade.getSuperEllipseArea(1.0, 1.0, 0.0));
    Assertions.assertThrows(IllegalArgumentException.class, () -> Blade.getSuperEllipseArea(-1.0, 1.0, 1.0));
  }

  @Test
  void testUniformBladeVolumeAndPoB() {
    double length = 1000.0;
    double w = 10.0;
    double t = 2.0;
    Blade.Bevel bevel = new Blade.Bevel(0.66, 1.0);

    Blade blade = new Blade(false,
            w, w, null,
            t, t, null,
            length,
            bevel,
            null,
            null,
            null
    );

    // expected cross-sectional area = spine rectangle + superellipse bevel
    double wBevel = w * bevel.percentageOfBladeWidth();
    double wSpine = w * (1.0 - bevel.percentageOfBladeWidth());
    double areaSpine = t * wSpine;
    double areaBevel = Blade.getSuperEllipseArea(wBevel * 0.5, t * 0.5, bevel.curveFactor());
    double expectedArea = areaSpine + areaBevel;
    double expectedVolume = expectedArea * length;
    double expectedPoB = length / 2.0;

    Assertions.assertEquals(expectedVolume, blade.getVolume(), EPS);
    Assertions.assertEquals(expectedPoB, blade.getPointOfBalance(), EPS);
  }

  @Test
  void testConicalFrustum() {
    double length = 50;
    double widthBase = 20;
    double widthTop = 15;
    Blade blade = new Blade(
            false,
            //widthBase, widthTop, new HashMap<>(Map.of(0.5, 17.5, 0.25, 18.75, 0.75, 16.25, 0.1, 19.5, 0.9, 15.5)),
            //widthBase, widthTop, new HashMap<>(Map.of(0.5, 17.5, 0.25, 18.75, 0.75, 16.25, 0.1, 19.5, 0.9, 15.5)),
            widthBase, widthTop, null,
            widthBase, widthTop, null,
            length,
            new Blade.Bevel(1.0, 2.0),
            null,
            null,
            null
    );

    double expectedVolume = 12108.2216857; // calculated separately
    System.out.println("Expected: " + expectedVolume);
    System.out.println("Actual: " + blade.getVolume());
    System.out.println("Actual by integration: " + blade.getVolume());

    Assertions.assertEquals(expectedVolume, blade.getVolume(), 10);
  }

  @Test
  void testBlade() {
    System.out.println("Volume by trapezoidal rule: " + testBlade1.getVolume());
    System.out.println("Volume by integration: " + testBlade1.getVolume());
  }

  @Test
  void testCircleSegment() {
    double expectedArea = 199.4;
    double actualArea = Blade.getCircleSegmentArea(20, 12);
    Assertions.assertEquals(expectedArea, actualArea, 0.1);
  }

}
