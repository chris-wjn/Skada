// java

import com.cwjn.skada.data.gen.weapon.parts.Blade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BladeTest {

  // tolerance in mm\(/mm\^3\)
  private static final double EPS = 1e-6;

//  @Test
//  void testUniformPrism() {
//    double length = 1000.0; // mm
//    double w0 = 10.0; // mm
//    double w1 = 10.0; // mm
//    double t0 = 2.0;  // mm
//    double t1 = 2.0;  // mm
//
//    Blade blade = new Blade(w0, w1, null, t0, t1, null, length);
//
//    double expectedVolume = w0 * t0 * length; // rectangular prism
//    double expectedPoB = length / 2.0;
//
//    Assertions.assertEquals(expectedVolume, blade.getVolume(), EPS);
//    Assertions.assertEquals(expectedPoB, blade.getPointOfBalance(), EPS);
//  }
//
//  @Test
//  void testLinearTaperAnalytic() {
//    double L = 1000.0; // mm
//    double w0 = 20.0;  // width at base mm
//    double w1 = 10.0;  // width at tip mm
//    double t0 = 4.0;   // thickness at base mm
//    double t1 = 2.0;   // thickness at tip mm
//
//    Blade blade = new Blade(w0, w1, null, t0, t1, null, L);
//
//    // analytic integration for w(x)=w0 + alpha*x, t(x)=t0 + beta*x where x in [0,L]
//    double alpha = (w1 - w0) / L;
//    double beta = (t1 - t0) / L;
//
//    // Volume = ∫_0^L (w0 + alpha x)(t0 + beta x) dx
//    // = w0 t0 L + 0.5 (w0 beta + t0 alpha) L^2 + (1.0/3.0) alpha beta L^3
//    double expectedVolume = w0 * t0 * L
//            + 0.5 * (w0 * beta + t0 * alpha) * L * L
//            + (1.0 / 3.0) * alpha * beta * L * L * L;
//
//    // First moment = ∫_0^L x * A(x) dx
//    // = w0 t0 L^2/2 + (w0 beta + t0 alpha) L^3/3 + alpha beta L^4/4
//    double firstMoment = w0 * t0 * (L * L / 2.0)
//            + (w0 * beta + t0 * alpha) * (L * L * L / 3.0)
//            + alpha * beta * (L * L * L * L / 4.0);
//
//    double expectedPoB = firstMoment / expectedVolume;
//
//    Assertions.assertEquals(expectedVolume, blade.getVolume(), 1e-4 * Math.max(1.0, expectedVolume));
//    Assertions.assertEquals(expectedPoB, blade.getPointOfBalance(), 1e-6);
//  }

}
