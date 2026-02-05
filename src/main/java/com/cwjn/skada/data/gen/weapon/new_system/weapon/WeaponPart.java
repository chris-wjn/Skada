package com.cwjn.skada.data.gen.weapon.new_system.weapon;

import com.cwjn.skada.data.gen.weapon.new_system.geometry.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.new_system.geometry.PhysicsUtil.MassProperties;


/**
 * A weapon part that can contribute volume, mass properties, and inertia.
 *
 * Units: centimeters for length, grams for mass, g/cm^3 for density.
 */
public interface WeaponPart {

    GeometryUtil.Vec3 position();

    /**
     * Optional coordinate transform applied when this part is assembled on a weapon.
     * Default is identity (no transform).
     */
    default WeaponPartTransform transform() {
        return WeaponPartTransform.identity();
    }

    MassProperties massProperties(double densityGPerCm3, int samples);

    double momentOfInertiaAboutWeaponBase(WeaponAxis axis, double densityGPerCm3, int samples);

    GeometryUtil.Bounds localBounds(int samples);

    default double volumeCm3(int samples) {
        return massProperties(1.0, samples).volumeCm3();
    }

}
