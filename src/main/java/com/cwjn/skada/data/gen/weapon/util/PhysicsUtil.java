package com.cwjn.skada.data.gen.weapon.util;

import com.cwjn.skada.data.gen.weapon.util.GeometryUtil.Vec3;

public class PhysicsUtil {
  
  public static double toKgM2(double gramCentimeterSquared) {
    return gramCentimeterSquared * 1.0e-7;
  }

  public static double toKg(double gram) {
    return gram * 1.0e-3;
  }

  public record MassProperties(double volumeCm3, double massG, Vec3 centerOfMass) {}

}
