// java

import com.cwjn.skada.data.gen.weapon.parts.Blade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BladeTest {

  private static final Blade testBlade1 = new Blade(
    10, 10, null,
          2, 2, null,
          1000,
          new Blade.Bevel(0.66, 1),
          null,
          null,
          null
  );

  // tolerance in mm\(/mm\^3\)
  private static final double EPS = 1e-6;

  @Test
  void testUniformPrism() {
    double length = 1000.0; // mm
    double w0 = 10.0; // mm
    double w1 = 10.0; // mm
    double t0 = 2.0;  // mm
    double t1 = 2.0;  // mm

    double expectedVolume = w0 * t0 * length; // rectangular prism
    double expectedPoB = length / 2.0;

    Assertions.assertEquals(expectedVolume, testBlade1.getVolume(), EPS);
    Assertions.assertEquals(expectedPoB, testBlade1.getPointOfBalance(), EPS);
  }

}
