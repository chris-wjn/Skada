package com.cwjn.skada.data.gen.weapon.generation_algo;

import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationBalanceTuningTest {

  @Test
  void failChanceCalibrationStaysInBasisPointToLowPercentRange() {
    assertEquals(0.0, CriticalFailGenerationUtil.clampAndRoundFailChance(0.00004, 0.0, 0.05), 1.0e-9);
    assertEquals(0.004, CriticalFailGenerationUtil.clampAndRoundFailChance(0.0042, 0.0001, 0.05), 1.0e-9);
    assertEquals(0.05, CriticalFailGenerationUtil.clampAndRoundFailChance(0.08, 0.0001, 0.05), 1.0e-9);
  }

  @Test
  void strikeLethalityCurveIsMonotonicAndTempered() {
    double lowMomentum = LethalityGenerationUtil.strikeLethalityFromMomentum(0.05, 1.0);
    double midMomentum = LethalityGenerationUtil.strikeLethalityFromMomentum(0.10, 1.0);
    double highMomentum = LethalityGenerationUtil.strikeLethalityFromMomentum(0.20, 1.0);

    assertTrue(lowMomentum < midMomentum);
    assertTrue(midMomentum < highMomentum);
    assertTrue(midMomentum < 45.0);
    assertTrue(highMomentum < 56.0);
  }

  @Test
  void slashBaseLethalityCurveIsMonotonicAndMeaningful() {
    double lowEnergy = LethalityGenerationUtil.slashBaseLethalityFromEnergy(0.08);
    double midEnergy = LethalityGenerationUtil.slashBaseLethalityFromEnergy(0.16);
    double highEnergy = LethalityGenerationUtil.slashBaseLethalityFromEnergy(0.32);

    assertTrue(lowEnergy < midEnergy);
    assertTrue(midEnergy < highEnergy);
    assertTrue(lowEnergy > 0.0);
    assertTrue(midEnergy > 10.0);
    assertTrue(highEnergy < 60.0);
  }

  @Test
  void thrustBaseLethalityCurveIsMonotonicAndTempered() {
    double lowEnergy = LethalityGenerationUtil.thrustBaseLethalityFromEnergy(6.0);
    double midEnergy = LethalityGenerationUtil.thrustBaseLethalityFromEnergy(12.0);
    double highEnergy = LethalityGenerationUtil.thrustBaseLethalityFromEnergy(24.0);

    assertTrue(lowEnergy < midEnergy);
    assertTrue(midEnergy < highEnergy);
    assertTrue(lowEnergy > 5.0);
    assertTrue(midEnergy < 20.0);
    assertTrue(highEnergy < 30.0);
  }

  @Test
  void strikePrecisionRewardsFocusedFacesWithoutRunningAway() {
    double broadFace = PrecisionGenerationUtil.strikeAreaDrivenPrecision(4.0, 1.0);
    double focusedFace = PrecisionGenerationUtil.strikeAreaDrivenPrecision(1.0, 1.0);
    double focusedAndCrowned = PrecisionGenerationUtil.strikeAreaDrivenPrecision(1.0, 1.2);

    assertTrue(broadFace < focusedFace);
    assertTrue(focusedFace < focusedAndCrowned);
    assertTrue(focusedFace < 20.0);
  }

  @Test
  void strikeLocalizationRewardsFocusedImpactFacesWithoutRunningAway() {
    double broadFace = LethalityGenerationUtil.strikeLocalizationFactor(4.0, 1.0);
    double focusedFace = LethalityGenerationUtil.strikeLocalizationFactor(1.0, 1.0);
    double focusedAndCrowned = LethalityGenerationUtil.strikeLocalizationFactor(1.0, 1.2);

    assertTrue(broadFace < focusedFace);
    assertTrue(focusedFace < focusedAndCrowned);
    assertTrue(broadFace >= 0.55);
    assertTrue(focusedAndCrowned <= 1.25);
  }

  @Test
  void strikeLocalizationMeaningfullyPenalizesBroadImpactFaces() {
    double shovelLike = LethalityGenerationUtil.strikeLocalizationFactor(4.2, 0.92);
    double maceLike = LethalityGenerationUtil.strikeLocalizationFactor(1.2, 1.15);

    assertTrue(shovelLike < 0.9);
    assertTrue(maceLike > shovelLike);
  }

  @Test
  void strikeComplianceFactorPreservesBoundedStructuralLoss() {
    double compliant = LethalityGenerationUtil.strikeComplianceFactor(0.78);
    double neutral = LethalityGenerationUtil.strikeComplianceFactor(1.0);
    double veryRigid = LethalityGenerationUtil.strikeComplianceFactor(1.2);

    assertTrue(compliant < neutral);
    assertTrue(neutral < veryRigid);
    assertEquals(1.08, veryRigid, 1.0e-9);
  }

  @Test
  void strikeIncidenceFactorPenalizesGlancingProneFaces() {
    double glancingProne = LethalityGenerationUtil.strikeIncidenceFactor(0.82);
    double neutral = LethalityGenerationUtil.strikeIncidenceFactor(1.0);
    double veryClean = LethalityGenerationUtil.strikeIncidenceFactor(1.2);

    assertTrue(glancingProne < neutral);
    assertTrue(neutral < veryClean);
    assertEquals(1.05, veryClean, 1.0e-9);
  }

  @Test
  void strikeContactQualityFactorPenalizesCompliantGlancingFaces() {
    double shovelLike = PrecisionGenerationUtil.strikeContactQualityFactor(0.82, 0.84);
    double neutral = PrecisionGenerationUtil.strikeContactQualityFactor(1.0, 1.0);
    double compactBlunt = PrecisionGenerationUtil.strikeContactQualityFactor(1.05, 1.03);

    assertTrue(shovelLike < neutral);
    assertTrue(neutral < compactBlunt);
    assertTrue(shovelLike >= 0.78);
    assertTrue(compactBlunt <= 1.05);
  }

  @Test
  void strikeRepeatabilityPenalizesBroadImpactTools() {
    StrikeCapable shovelLike = new StrikeCapable() {
      @Override
      public double effectiveContactAreaCm2() {
        return 4.2;
      }

      @Override
      public double strikeFaceGeometryFocus() {
        return 0.92;
      }

      @Override
      public double strikeStructuralEfficiency() {
        return 0.82;
      }

      @Override
      public double strikeIncidenceEfficiency() {
        return 0.84;
      }
    };

    StrikeCapable maceLike = new StrikeCapable() {
      @Override
      public double effectiveContactAreaCm2() {
        return 1.2;
      }

      @Override
      public double strikeFaceGeometryFocus() {
        return 1.15;
      }

      @Override
      public double strikeStructuralEfficiency() {
        return 1.03;
      }

      @Override
      public double strikeIncidenceEfficiency() {
        return 1.02;
      }
    };

    double shovelRepeatability = shovelLike.strikeRepeatability();
    double maceRepeatability = maceLike.strikeRepeatability();

    assertTrue(shovelRepeatability < maceRepeatability);
    assertTrue(PrecisionGenerationUtil.strikeRepeatabilityFactor(shovelRepeatability) < 1.0);
    assertTrue(CriticalFailGenerationUtil.strikeRepeatabilityRisk(shovelRepeatability) > 1.0);
  }

  @Test
  void thrustAlignmentEfficiencyPenalizesRotationalToolPoints() {
    ThrustCapable pickLike = new ThrustCapable() {
      @Override
      public double pointTaper() {
        return 0.82;
      }

      @Override
      public double tipLengthCm() {
        return 6.5;
      }

      @Override
      public double widthAtPointBase() {
        return 1.25;
      }

      @Override
      public double thicknessAtPointBase() {
        return 0.75;
      }

      @Override
      public double tipRadiusNm() {
        return 8.0;
      }

      @Override
      public ThrustMotionMode thrustMotionMode() {
        return ThrustMotionMode.ROTATIONAL;
      }
    };

    ThrustCapable spearLike = new ThrustCapable() {
      @Override
      public double pointTaper() {
        return 0.94;
      }

      @Override
      public double tipLengthCm() {
        return 14.0;
      }

      @Override
      public double widthAtPointBase() {
        return 0.75;
      }

      @Override
      public double thicknessAtPointBase() {
        return 0.35;
      }

      @Override
      public double tipRadiusNm() {
        return 4.0;
      }
    };

    double pickAlignment = pickLike.thrustAlignmentEfficiency();
    double spearAlignment = spearLike.thrustAlignmentEfficiency();

    assertTrue(pickAlignment < spearAlignment);
    assertTrue(CriticalFailGenerationUtil.thrustAlignmentRisk(pickAlignment) > 1.0);
    assertTrue(CriticalFailGenerationUtil.thrustAlignmentRisk(spearAlignment) <= 1.0);
  }

  @Test
  void thrustPenetrationEfficiencyPenalizesBroadBlockyPoints() {
    ThrustCapable blockySwordLike = new ThrustCapable() {
      @Override
      public double pointTaper() {
        return 0.55;
      }

      @Override
      public double tipLengthCm() {
        return 7.0;
      }

      @Override
      public double widthAtPointBase() {
        return 6.0;
      }

      @Override
      public double thicknessAtPointBase() {
        return 0.72;
      }

      @Override
      public double tipRadiusNm() {
        return 18.0;
      }
    };

    ThrustCapable spearLike = new ThrustCapable() {
      @Override
      public double pointTaper() {
        return 0.94;
      }

      @Override
      public double tipLengthCm() {
        return 14.0;
      }

      @Override
      public double widthAtPointBase() {
        return 0.75;
      }

      @Override
      public double thicknessAtPointBase() {
        return 0.35;
      }

      @Override
      public double tipRadiusNm() {
        return 4.0;
      }
    };

    double swordLikePenetration = blockySwordLike.thrustPenetrationEfficiency();
    double spearLikePenetration = spearLike.thrustPenetrationEfficiency();

    assertTrue(swordLikePenetration < spearLikePenetration);
    assertTrue(LethalityGenerationUtil.thrustPenetrationFactor(swordLikePenetration) < 0.7);
    assertTrue(PrecisionGenerationUtil.thrustPenetrationFactor(swordLikePenetration) < 0.7);
  }
}