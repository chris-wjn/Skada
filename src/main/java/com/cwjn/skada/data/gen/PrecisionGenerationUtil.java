package com.cwjn.skada.data.gen;

import static com.cwjn.skada.data.SkadaData.EDGE_ANGLE_DEFAULT;
import static com.cwjn.skada.data.SkadaData.MATERIAL_PROPERTY_SOFT_CAP;

public class PrecisionGenerationUtil {

  public double slashPrecision(WeaponProfile profile, ExtraTierInfo tierInfo) {
    double edgeAngleNormalized = EDGE_ANGLE_DEFAULT / profile.edgeBevel().angle(); //acuter angle means better cutting
    double bladeWeight = profile.estimateBladeVolume() * tierInfo.density(); //in grams

    double precision = edgeRadiusToPrecisionBase(profile.edgeBevel().edgeRadius()); //the base for precision comes from edge radius
    precision += bladeWeightToPrecisionBase2(bladeWeight); //the blade weight can help, even if the edge is blunt. Heavier blades have more momentum.
    precision *= edgeAngleNormalized; //the edge angle modifies precision

    double normalizedHardness = tierInfo.hardness()/MATERIAL_PROPERTY_SOFT_CAP;
    precision *= 1 + 0.05*normalizedHardness;

    return precision;
  }

  /**
   * Provides a precision value based on edge radius for slashing attacks.
   * Average precision for a sword is 20, for edge radius = 5.
   * Precision decreases sharply as edge radius decreases below 5,
   * and increases sharply as edge radius increases above 5. Anything
   * above 10nm edge radius is effectively blunt.
   * @param edgeRadius The edge radius in nanometres.
   * @return A double representing precision.
   */
  private double edgeRadiusToPrecisionBase(double edgeRadius) {
    if (edgeRadius < 7)
      return -Math.pow((edgeRadius - 5), 3) + 20;
    else
      return Math.exp(-edgeRadius/2 + 6);
  }

  /**
   * Provides a precision value based on blade weight for slashing attacks.
   * This is considered less influential than edge radius, so the values will
   * be less from here. The blade weight for our supposed average is 1300g.
   * We'll set 1300g to provide 6 precision, where heavier blades give more
   * precision up to a soft cap of 10 precision, after which there are diminishing returns.
   * And, of course, anything lighter will give less than 6 by a soft curve, down to 1
   * precision at 100g.
   *
   * @param bladeWeight The blade weight in grams.
   * @return A double representing precision.
   */
  private double bladeWeightToPrecisionBase2(double bladeWeight) {
    if (bladeWeight >= 1300) {
      return 6 + 4 * (1 - Math.exp(-0.0005 * (bladeWeight - 1300)));
    } else {
      return 1 + 5 * Math.exp(-0.002 * (1300 - bladeWeight));
    }
  }

}
