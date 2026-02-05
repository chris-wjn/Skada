package com.cwjn.skada.data.gen.weapon;
import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.gen.weapon.old_system.WeaponProfile;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.SlashCapable;
import com.cwjn.skada.data.gen.weapon.parts.attack_types.ThrustCapable;
import com.cwjn.skada.util.Util;

import static com.cwjn.skada.data.SkadaData.BEVEL_ANGLE_DEFAULT;

import static com.cwjn.skada.data.SkadaData.MATERIAL_PROPERTY_SOFT_CAP;

public abstract class LethalityGenerationUtil {

  // Scales the friction-length term (f * bevelLength) into the same numeric
  // range as angular momentum. Typical values: L ≈ 0.001–0.003 kg·m²/s,
  // bevelLength ≈ 0.02–0.05 m, f ∈ [0,1]. Using 0.05 keeps
  // 0.05 * f * 0.03 ≈ 0.00075 (for f≈0.5) comparable to L≈0.0015.
  private static final double BEVEL_RESISTANCE_MOMENTUM_SCALE = 0.05;

  /**
   * Calculates lethality for slashing attacks.
   * @param profile The weapon profile of the weapon
   * @param material The weapon material info
   * @return a double representing lethality
   */
  public static double slash(WeaponProfile profile, ExtraTierInfo material) {
    WeaponProfile.WeaponHeadEntry head = profile.getSlashHead();
    if (head.getMaterial().isPresent()) material = head.getMaterial().get();
    SlashCapable slashHead = (SlashCapable) head.getHead(); //this is a bit dubious but it should be fine

    double pos = head.getHead().pointOfBalance();
    double primaryBevelAngle = slashHead.primaryBevelAngle(pos); //angle of the primary bevel at point of balance
    double normalizedBladeWeight = profile.normalizeBladeWeight(material);

    /*
       Lethality is based primarily on a few things:
       - The angular momentum the weapon can generate: longer, heavier weapons generate more angular momentum when swung,
                                                       increasing cutting power. Point of Balance plays a role in moment of inertia.
       - The wedge size: combination of bevel angle (how wide the wedge is) and bevel width/length (how wide the blade is).
                         A larger wedge mean more lethality, because the cut is more destructive.
    */
    double momentOfInertia = profile.getMomentOfInertia(material);
    double angularVelocity = Util.angularVelocity(momentOfInertia, SkadaData.PLAYER_STRENGTH);
    double angularMomentum = momentOfInertia * angularVelocity;
    double lethalityAngularMomentum = angularMomentumToLethalityBase(angularMomentum);

    System.out.println("Moment of Inertia: " + momentOfInertia + " kg·m², Angular Velocity: " + angularVelocity + " rad/s, Angular Momentum: " + angularMomentum + " kg·m²/s");

     /*
     * Calculate wedge size by combining bevel angle and bevel length. Wedge size
     * will be tied to angular momemntum based the following facts:
     * - A wider bevel is more lethal, since it creates a more destructive cut
     * - However, a wider bevel also requires more force to cut with, since wider
     * wedges are worse at cutting
     * - ergo, a wider wedge provides higher lethality to the extent that there is
     * enough angular momentum to drive it through the target,
     * so if the wedge is too wide compared to the angular momentum, the lethality
     * bonus from the wedge size should be diminished.
     */
    
    //the height of the bevel is roughly half the thickness of the head, which we can calculate using the bevel length (hypotenuse) and bevel angle,
    //so the side we're looking for is the opposite of the angle.
    System.out.println("Bevel Width: " + slashHead.getBevelWidthAt(profile.getCentreOfPercussion(material)) + " cm, Primary Bevel Angle: " + primaryBevelAngle + " degrees");
    double bevelHeight = slashHead.getBevelThicknessAt(profile.getCentreOfPercussion(material));
     //the amount the wedge contributes to cutting resistance is some factor of friction between the wedge and the target
     //we'll call this factor 'f'. We derive f from bevel angle, since we can't use the exact coefficient of friction between the blade and target.
     //then, some function of f and bevel length tells us how much angular momentum is needed to get benefits from the bevel height.
    double f = 1.0 - (primaryBevelAngle / 90.0); //at 0 degrees, f=1 (maximum friction), at 90 degrees, f=0 (no friction)
    double bevelLen = slashHead.absoluteBevelLength(profile.getCentreOfPercussion(material));
    System.out.println("Bevel Length: " + bevelLen + " cm, Friction Factor: " + f + " , Resistance: " + (f * bevelLen));
    double momentumSurplus = angularMomentum - (BEVEL_RESISTANCE_MOMENTUM_SCALE * f * bevelLen);
    //now we can use a 2 variable function to determine how much lethality we'll add. The function will return a value roughly
    //between 0 and 30, where the returned value is dependant on bevel height, but only if there's enough momentum surplus to make use of the bevel height.
    System.out.println("Bevel Height: " + bevelHeight + " cm, Momentum Surplus: " + momentumSurplus + " kg·m²/s");
    double bevelLethalityBonus = getWedgeBonus(bevelHeight, momentumSurplus);
    double lethality = lethalityAngularMomentum + bevelLethalityBonus;
    
    double normalizedHardness = material.hardness()/MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedFlexibility = Math.abs(material.flexibility()-MATERIAL_PROPERTY_SOFT_CAP/2)/(MATERIAL_PROPERTY_SOFT_CAP/2);
    lethality *= 1 + 0.08*normalizedHardness + 0.14*normalizedBladeWeight - 0.1*normalizedFlexibility; //factor material properties
    /*
      increases with hardness (keeps a sharp edge), flexibility has a sweet spot
      at flexibility = 5. Too low flexibility and the blade can't flex at all
      to achieve the correct angle, too high and the blade might bounce off the target
      instead of cutting in.
    */

    return lethality;
  }

  /**
   * Calculates a wedge bonus for lethality based on bevel height and available momentum surplus.
   * The function returns higher values for higher bevel heights, but only if there's enough
   * momentum surplus to make use of the bevel height.
   * 
   * The function is defined as:
   *   bonus = A * (1 - exp(-B * bevelHeight)) * (1 - exp(-C * momentumSurplus))
   * where A, B, C are constants that shape the curve.
   * 
   * Typical values:
   * - bevelHeight: 0.001 m to 0.05 m
   * - momentumSurplus: 0.0 to 0.005 kg·m²/s
   * 
   * @param bevelHeight the height of the bevel in meters
   * @param momentumSurplus the surplus angular momentum in kg·m²/s
   * @return the wedge bonus for lethality
   */
  private static double getWedgeBonus(double bevelHeight, double momentumSurplus) {
    // Constants to shape the curve
    final double A = 30.0; // Maximum bonus
    final double B = 50.0; // Bevel height sensitivity
    final double C = 800.0; // Momentum surplus sensitivity

    double bevelFactor = 1.0 - Math.exp(-B * bevelHeight);
    double momentumFactor = 1.0 - Math.exp(-C * Math.max(0.0, momentumSurplus));

    System.out.println(A * bevelFactor * momentumFactor);
    return A * bevelFactor * momentumFactor;
  }

  /**
   * Converts angular momentum to a base lethality value using a logarithmic scaling function.
   * This provides diminishing returns as angular momentum increases, mapping typical weapon
   * values (0.01 - 3.0 kg·m²/s) to a lethality range of approximately 0 - 50.
   * 
   * The function uses: lethality = k * ln(1 + c * L) where:
   * - L is angular momentum in kg·m²/s
   * - k and c are scaling constants tuned so that:
   *   - A dagger (L ≈ 0.05) gives ~5 lethality
   *   - A longsword (L ≈ 0.3) gives ~20 lethality  
   *   - A greatsword (L ≈ 0.8) gives ~30 lethality
   *   - Exceptional weapons (L ≈ 2.0) approach ~40 lethality
   *   - Very high angular momentum (L ≈ 3.0) approaches ~50 lethality
   * 
   * @param momentOfInertia the moment of inertia in kg·m²
   * @param angularVelocity the angular velocity in rad/s
   * @return the base lethality value, typically in range 0-50
   */
  private static double angularMomentumToLethalityBase(double angularMomentum) {
    // Logarithmic scaling: lethality = k * ln(1 + c * L)
    // Constants k, c, tuned to make function return lethality values roughly between 0 and 50
    // final lethality values will be further modified by other factors later.
    // At L=0.05: ~5, L=0.3: ~20, L=0.8: ~30, L=2.0: ~40, L=3.0 = ~50
    final double k = 16.0;
    final double c = 8.0;
    
    double lethality = k * Math.log(1.0 + c * angularMomentum);
    
    return Math.max(0.0, lethality);
  }

  public static double thrust(WeaponProfile profile, ExtraTierInfo tierInfo) {
    WeaponProfile.WeaponHeadEntry thrustHead = profile.getThrustHead();
    if (thrustHead.getMaterial().isPresent()) tierInfo = thrustHead.getMaterial().get();
    ThrustCapable head = (ThrustCapable) thrustHead.getHead();
    //weapon profile values and normalizations
    double primaryAngle = head.tipSpecs().tipBevelAngle();
    double primaryAngleNormalized = Util.normalizeBevelAngle(primaryAngle); //acuter angle means more lethality cause better piercing
    double lethality = primaryAngleNormalized * bladeDimensionsToLethalityBase(head);

    double shoulderAngle = head.tipSpecs().tipBevelShoulderAngle();
    double distFromOptimalShoulder = Math.abs(180 - shoulderAngle);
    if (shoulderAngle > 180) distFromOptimalShoulder*=2;
    lethality -= distFromOptimalShoulder * (1-head.tipSpecs().tipShoulderRoundedness());

    if (head instanceof SlashCapable slashCapable && slashCapable.isSingleEdged()) {
      lethality *= 0.5;
    }

    double normalizedHardness = tierInfo.hardness()/MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedFlexibility = tierInfo.flexibility()/MATERIAL_PROPERTY_SOFT_CAP;
    lethality *= 1.0 + 0.1 * normalizedHardness - 0.06 * normalizedFlexibility;

    return lethality;
  }

  /**
   * Calculates a base lethality value for thrust from the dimensions of a ThrustCapable head.
   * The head could be any shape, blade, spike, cone, etc, so we need the head to provide its
   * own taper value.
   * @param head the ThrustCapable head of the weapon
   * @return the base lethality value, somewhere between 0 and ~80.
   */
  public static double bladeDimensionsToLethalityBase(ThrustCapable head) {
    double taperValue = head.getTaperValue();
    if (taperValue <= 91.3) {
      return 0.82 * taperValue;
    } else {
      return 75.0 + 5*Math.log(1 + 0.1*(taperValue-91.3));
    }
  }

  public static double strike(WeaponProfile profile, ExtraTierInfo tierInfo) {
    //Some calculations about the balance point of the weapon, using material density and profile dimensions
    double pointOfBalanceNormalized = profile.pointOfBalance(tierInfo) / profile.getTotalLength(); //percentage from 0-1, where 1 is furthest from pommel

    double weight = profile.getVolume() * tierInfo.density(); //in grams
    double lethality = weightToLethalityBase(weight);

    lethality *= 0.5 + 0.75*pointOfBalanceNormalized;

    double normalizedHardness = tierInfo.hardness()/MATERIAL_PROPERTY_SOFT_CAP;
    double normalizedFlexibility = tierInfo.flexibility()/MATERIAL_PROPERTY_SOFT_CAP;
    lethality *= 1.0 + 0.15 * normalizedHardness - 0.15 * normalizedFlexibility;

    return lethality;
  }

  /**
   * Converts weapon weight to a lethality linearly, since for strike,
   * you're literally just smacking them with a piece of metal or whatever.
   * The weight of whatever it is matters most.
   * @param weight the weight of the weapon in grams
   * @return the base lethality value for strike attacks
   */
  public static double weightToLethalityBase(double weight) {
    if (weight <= 3000) return 0.0167*weight + 30; //3000g = 80 leth
    else {
      return 80.0 + 4*Math.log(1 + 0.01*(weight-3000)); //diminishing returns after 3000g
    }
  }

}


