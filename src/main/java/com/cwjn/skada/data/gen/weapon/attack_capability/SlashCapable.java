package com.cwjn.skada.data.gen.weapon.attack_capability;

import java.util.Optional;

/**
 * Indicates that a weapon can perform slashing attacks, and provides information needed to calculate
 * properties of slashing attacks.
 */
public interface SlashCapable extends AttackCapable {

  /**
   * Returns the cross-section of the weapon at the given normalized position along the blade.
   * The normalized position is a value between 0 and 1, where 0 corresponds to the base of the weapon part,
   * and 1 corresponds to the tip of the weapon part. Values outside this range should be clamped to the nearest valid value.
   * 
   * IMPORTANT! Since not all weapon parts are orientated the same way when viewed in isolation,
   * implementations of this method cannot assume that the normalized position corresponds to the
   * same axis for all weapon parts. For example, cross-section slices of a sword blade are taken along
   * the length, or the x axis. However, cross-sections of an axe head are taken along the height,
   * or the y axis. Therefore, it is up to each weapon part to interpret how the normalized position
   * correspends to the geometry of the weapon part, and to return the appropriate cross-section for that position.
   * 
   * @param normalizedPosition the normalized position on the weapon part, between 0 and 1, where 0 is the base and 1 is the tip
   * @return the cross-section of the weapon at the given position
   */
	CrossSection crossSectionAt(double normalizedPosition);

  /**
   * Weapon parts could have an edge bevel. If present, we use the given
   * edge bevel angle instead of the angle of the primary bevel.
   * @return the angle of the edge bevel in degrees, if an edge bevel is present, or an empty Optional if no edge bevel is present
   */
	Optional<Double> edgeBevelAngle();

  /**
   * Returns the angle at the edge of the weapon part at a given position.
   * If the weapon part has a defined edgeBevelAngle, then that will be used.
   * Otherwise, we calculate the angle of the entire bevel using the dimensions
   * of the cross-section at normalizedPosition.
   * @param normalizedPosition the normalized position on the weapon part, between 0 and 1, where 0 is the base and 1 is the tip
   * @return the angle at the edge of the weapon part in degrees
   */
	default double edgeAngleDegreesAt(double normalizedPosition) {
		if (edgeBevelAngle().isPresent()) {
      return edgeBevelAngle().get();
    } else {
      CrossSection crossSection = crossSectionAt(normalizedPosition);
      double halfThickness = crossSection.halfThicknessCm();
      double effectiveWidth = crossSection.effectiveWidthCm();
      return Math.toDegrees(Math.atan(halfThickness / effectiveWidth));
    }
	}

  /**
   * The thickness of a cross-section at a given position
   * @param normalizedPosition the normalized position on the weapon part, between 0 and 1, where 0 is the base and 1 is the tip
   * @return the thickness of the cross-section at the given position
   */
	default double wedgeThicknessCmAt(double normalizedPosition) {
		return crossSectionAt(normalizedPosition).halfThicknessCm() * 2;
	}

  /**
   * Returns the edge apex radius in nanometres.
   *
   * Edge radius is the primary geometric factor for slash precision because
   * smaller radii concentrate contact stress more effectively at impact.
   *
   * @return edge radius in nm, must be > 0
   */
  double edgeRadiusNm();

  /**
   * Returns the ideal normalized position of the point of balance for this weapon when used for
   * slash attacks. Default 33% is for sword blades.
   * @return the ideal normalized position of the point of balance for slash attacks, between 0 and 1
   */
  default double idealPointOfBalanceSlash() {
    return 0.33;
  }

  /**
   * Returns the normalized strike point for slashing on this part.
   *
   * This value is in part-local weapon-axis coordinates where 0 is the rear-most
   * position on the part and 1 is the forward-most position.
   *
   * @param samples sampling resolution hint for geometry-driven implementations
   * @return strike point in [0, 1]
   */
  default double normalizedSlashStrikePointOnPart(int samples) {
    return 0.66;
  }

  /**
   * Information about the cross-section of a slashing weapon. Since it is necessary for
   * a weapon to have a defined edge in order to slash effectively, we can be 100% sure
   * that ANY slashing-capable weapon has a bevelled cross-section, and thus we can use this class to
   * represent the cross-section of ANY slashing-capable weapon.
   * 
   * @param effectiveWidthCm the width of the cross-section measured from spine to edge. 
   * Note that the effective width is not necessarily the same as the actual width of the weapon, 
   * since the spine may not be located at the center of the weapon.
   * @param halfThicknessCm the thickness of the cross-section measured from the center to either the top or bottom surface.
   * @param r the superelliptical exponent. 
   * r < 0 is not a real shape,
   * 0 < r < 1 increases concavity of the sides,
   * r < 2/3 is an astroid,
   * r = 1 is a rhombus, 
   * r = 2 is an ellipse,
   * r > 2 tends towards a rectangle,
   * @return a new BevelCrossSection instance with the given parameters
   */
	record CrossSection(double effectiveWidthCm, double halfThicknessCm, double r) {
		public CrossSection {
			if (!Double.isFinite(effectiveWidthCm) || !Double.isFinite(halfThicknessCm)) {
				throw new IllegalArgumentException("bevel cross-section values must be finite");
			}
			if (effectiveWidthCm <= 0.0) {
				throw new IllegalArgumentException("bevel cross-section width must be > 0");
			}
			if (halfThicknessCm < 0.0) {
				throw new IllegalArgumentException("bevel cross-section thickness must be >= 0");
			}
		}
	}

}
