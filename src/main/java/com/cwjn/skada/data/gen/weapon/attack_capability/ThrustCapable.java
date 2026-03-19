package com.cwjn.skada.data.gen.weapon.attack_capability;

import net.minecraft.util.Mth;

public interface ThrustCapable extends AttackCapable {

	enum ThrustMotionMode {
		LINEAR,
		ROTATIONAL
	}

	/**
	 * Value from 0-1 defining the level of distal and profile taper of the tip section of the weapon.
	 * This describes the <em>shape quality</em> of the taper: how aggressively the tip converges
	 * to a point. A value of 1.0 means maximum taper (straight-line convergence); 0.0 means no taper.
	 *
	 * <p>What constitutes the "tip section" varies from part to part:
	 * <ul>
	 *   <li><b>Blade</b>: the region from the last station to the spine end.</li>
	 *   <li><b>Spike</b>: the entire spike.</li>
	 *   <li><b>Pick head</b>: the front spike projection.</li>
	 *   <li><b>Sickle</b>: the arc near the terminal point.</li>
	 * </ul>
	 *
	 * <p>This value alone does not capture how much of the weapon is dedicated to the tip — see
	 * {@link #tipLengthCm()} for that.
	 * 
	 * @return the point taper value, between 0 and 1
	 */
	double pointTaper();

	/**
	 * The absolute length of the tip section in centimetres. Each weapon part defines its own
	 * tip region and reports the length of that region:
	 * <ul>
	 *   <li><b>Blade</b>: the arc length along the spine from the last station to the blade end.</li>
	 *   <li><b>Spike</b>: the full length of the spike.</li>
	 *   <li><b>Pick head</b>: the length of the front spike.</li>
	 *   <li><b>Sickle</b>: the arc length of the curved tip region.</li>
	 * </ul>
	 *
	 * <p>Longer tip sections are beneficial for thrusting because:
	 * <ol>
	 *   <li><b>Lethality</b>: a longer, more gradual "wedge" profile requires less force to
	 *       advance through the target, yielding deeper wound channels for the same linear momentum.</li>
	 * </ol>
	 *
	 * @return the tip length in cm, must be &gt; 0
	 */
	double tipLengthCm();

	/**
	 * The width of the weapon at the base of the tip section, in centimeters. This is effectively the
	 * "wedge thickness" of the tip. A smaller width generally allows for deeper penetration with less force,
	 * but also results in less overall damage, or in our case, lethality.
	 *
	 * @return the width at the base of the tip in cm, must be &gt; 0
	 */
	double widthAtPointBase();


	/**
	 * Returns the ideal normalized position of the point of balance for this weapon when used for
	 * thrust attacks. Default 0% means the ideal PoB is at the weapon base, which is appropriate for short thrusting spikes and dagger-like weapons.
	 * @return
	 */
	default double idealPointOfBalanceThrust() {
		return 0.0;
	}

	/**
	 * The thickness of the weapon at the base of the tip section, in centimeters.
	 * Together with {@link #widthAtPointBase()}, this characterizes how focused the
	 * point base geometry is for penetration initiation.
	 *
	 * @return the thickness at the base of the tip in cm, must be &gt; 0
	 */
	double thicknessAtPointBase();

	/**
	 * The radius of the tip section in nanometers. This is a very fine measurement used for
	 * detailed modeling of the tip geometry.
	 *
	 * @return the tip radius in nm, must be &gt; 0
	 */
	double tipRadiusNm();

	/**
	 * Returns the normalized contact point for thrust on this part.
	 *
	 * This value is in part-local weapon-axis coordinates where 0 is the rear-most
	 * position on the part and 1 is the forward-most position.
	 *
	 * @param samples sampling resolution hint for geometry-driven implementations
	 * @return thrust contact point in [0, 1]
	 */
	default double normalizedThrustContactPointOnPart(int samples) {
		return 1.0;
	}

	default ThrustMotionMode thrustMotionMode() {
		return ThrustMotionMode.LINEAR;
	}

	/**
	 * Alignment efficiency for keeping the point on line during a combat thrust.
	 *
	 * This captures how forgiving the point geometry is under dynamic use.
	 * Rotational thrust tools are penalized more heavily because awkward geometry
	 * is harder to keep aligned once the point is being driven by a swing arc.
	 */
	default double thrustAlignmentEfficiency() {
		double taper = Mth.clamp(pointTaper(), 0.0, 1.0);
		double width = Math.max(0.05, widthAtPointBase());
		double thickness = Math.max(0.05, thicknessAtPointBase());
		double tipLength = Math.max(0.10, tipLengthCm());
		double tipSlenderness = tipLength / Math.max(width, thickness);
		double slendernessFactor = Mth.clamp(tipSlenderness / 7.0, 0.35, 1.10);
		double widthFactor = Mth.clamp(1.15 / width, 0.35, 1.10);
		double thicknessFactor = Mth.clamp(0.40 / thickness, 0.35, 1.15);
		double alignment = 0.28 * taper + 0.27 * slendernessFactor + 0.20 * widthFactor + 0.25 * thicknessFactor;
		if (thrustMotionMode() == ThrustMotionMode.ROTATIONAL) {
			return Mth.clamp(alignment, 0.48, 1.0);
		}
		return Mth.clamp(0.92 + 0.08 * alignment, 0.92, 1.02);
	}

	/**
	 * Penetration efficiency of the point geometry itself.
	 *
	 * Broad, slab-like point bases should lose substantial thrust lethality and
	 * precision even when the whole weapon can be driven with good delivery.
	 */
	default double thrustPenetrationEfficiency() {
		double taper = Mth.clamp(pointTaper(), 0.0, 1.0);
		double width = Math.max(0.05, widthAtPointBase());
		double thickness = Math.max(0.05, thicknessAtPointBase());
		double tipLength = Math.max(0.10, tipLengthCm());
		double pointBaseArea = width * thickness;
		double areaFactor = Mth.clamp(Math.pow(0.55 / pointBaseArea, 0.48), 0.22, 1.10);
		double widthFactor = Mth.clamp(Math.pow(1.10 / width, 0.42), 0.25, 1.10);
		double tipLengthFactor = Mth.clamp(Math.pow(tipLength / 8.0, 0.30), 0.55, 1.08);
		double taperFactor = Mth.clamp(0.35 + 0.65 * taper, 0.35, 1.00);
		return Mth.clamp(
			0.40 * areaFactor + 0.25 * widthFactor + 0.20 * tipLengthFactor + 0.15 * taperFactor,
			0.28,
			1.05);
	}

}
