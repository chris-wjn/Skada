import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.cwjn.skada.data.gen.weapon.parts.Blade.getSuperEllipseArea;

public class EllipseAreaTests {

  @Test
  void testSuperEllipse1() {
    double a = 3;
    double b = 2;
    double r = 2.3;
    double expectedArea = 19.7922;
    double area = getSuperEllipseArea(a, b, r);
    Assertions.assertEquals(expectedArea, area, 0.01);
  }

  @Test
  void testSuperEllipse2() {
    double a = 10;
    double b = 6;
    double r = 7;
    double expectedArea = 233.41;
    double area = getSuperEllipseArea(a, b, r);
    Assertions.assertEquals(expectedArea, area, 0.01);
  }

  @Test
  void testSuperEllipse3() {
    double a = 40;
    double b = 6;
    double r = 3;
    double expectedArea = 847.987;
    double area = getSuperEllipseArea(a, b, r);
    Assertions.assertEquals(expectedArea, area, 0.01);
  }

  @Test
  void testSuperEllipse4() {
    double a = 40;
    double b = 6;
    double r = 2.0/3.0;
    double expectedArea = 282.743;
    double area = getSuperEllipseArea(a, b, r);
    Assertions.assertEquals(expectedArea, area, 0.01);
  }

}
