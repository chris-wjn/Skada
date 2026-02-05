package com.cwjn.skada.data.gen.weapon.new_system.geometry;

import com.cwjn.skada.data.gen.weapon.new_system.geometry.GeometryUtil.Vec3;

public class PhysicsUtil {
  
    public record MassProperties(double volumeCm3, double massG, Vec3 centerOfMass) {}

}
