package com.cwjn.skada.data.gen.weapon.attack_capability;

import net.minecraft.util.Mth;

public interface StrikeCapable extends AttackCapable {

	/**
	 * Returns the normalized contact point for strike attacks on this part.
	 *
	 * This value is in part-local weapon-axis coordinates where 0 is the rear-most
	 * position on the part and 1 is the forward-most position.
	 *
	 * @param samples sampling resolution hint for geometry-driven implementations
	 * @return strike contact point in [0, 1]
	 */
	default double normalizedStrikeContactPointOnPart(int samples) {
		return 0.5;
	}

	/**
	 * Effective strike contact area in cm^2.
	 *
	 * Smaller values indicate more focused impact patches and therefore
	 * higher impact localization reliability (strike precision).
	 */
	default double effectiveContactAreaCm2() {
		return 4.0;
	}

	/**
	 * Face-geometry focus factor for striking surfaces.
	 *
	 * Values above 1 indicate crowned/peened/flanged faces that localize
	 * impact better; values below 1 indicate broad flat faces.
	 */
	default double strikeFaceGeometryFocus() {
		return 1.0;
	}

	/**
	 * Part-level rigidity contribution for strike precision.
	 *
	 * Higher values mean less local deformation of the striking face during impact.
	 */
	default double strikeHeadRigidity() {
		return 1.0;
	}

	/**
	 * Part/assembly stability contribution for strike precision.
	 *
	 * Higher values mean less wobble/flex and cleaner impulse localization.
	 */
	default double strikeAssemblyStability() {
		return 1.0;
	}

	/**
	 * Structural impact efficiency for strike lethality.
	 *
	 * Values below 1 indicate energy lost to local flex, shell deformation,
	 * or poor support behind the contact patch. Values above 1 are reserved for
	 * unusually solid, compact impactors that preserve peak force well.
	 */
	default double strikeStructuralEfficiency() {
		return 1.0;
	}

	/**
	 * Clean-hit efficiency for strike lethality.
	 *
	 * Values below 1 indicate strike faces that tend to glance, roll, or land
	 * obliquely instead of producing a clean normal impact.
	 */
	default double strikeIncidenceEfficiency() {
		return 1.0;
	}

	/**
	 * Repeatability of landing a clean, combat-useful strike with this face.
	 *
	 * Broad, compliant, glancing-prone faces should land lower than compact,
	 * rigid impactors even if they can deliver similar swing momentum.
	 */
	default double strikeRepeatability() {
		double contactArea = Math.max(0.02, effectiveContactAreaCm2());
		double areaFactor = Mth.clamp(Math.pow(1.6 / contactArea, 0.38), 0.70, 1.02);
		double focus = Mth.clamp(strikeFaceGeometryFocus(), 0.75, 1.25);
		double structural = Mth.clamp(strikeStructuralEfficiency(), 0.72, 1.08);
		double incidence = Mth.clamp(strikeIncidenceEfficiency(), 0.72, 1.05);
		return Mth.clamp(
			0.45 * areaFactor + 0.20 * focus + 0.18 * structural + 0.17 * incidence,
			0.70,
			1.02);
	}

}
