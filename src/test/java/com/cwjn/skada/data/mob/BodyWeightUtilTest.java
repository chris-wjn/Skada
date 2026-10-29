package com.cwjn.skada.data.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BodyWeightUtilTest {

  @Test
  void estimatesMissingReferenceWeightFromNormalBodyDimensions() {
    assertEquals(58.32, BodyWeightUtil.estimateReferenceWeightKg(0.6, 1.8), 1.0e-9);
  }

  @Test
  void scalesReferenceMassByRelativeBodyVolume() {
    assertEquals(7.29, BodyWeightUtil.scaleByBodyVolume(58.32, 0.6, 1.8, 0.3, 0.9), 1.0e-9);
  }

  @Test
  void explicitZeroRemainsZeroWhenResolvingWeight() {
    assertEquals(0.0, BodyWeightUtil.resolveWeightKg(0.0, 0.6, 1.8, 0.3, 0.9));
    assertEquals(0.0, BodyWeightUtil.normalizeReferenceWeight(0.0));
    assertNull(BodyWeightUtil.normalizeReferenceWeight(-1.0));
    assertNull(BodyWeightUtil.normalizeReferenceWeight(Double.NaN));
    assertNull(BodyWeightUtil.normalizeReferenceWeight(Double.POSITIVE_INFINITY));
  }

  @Test
  void missingAndInvalidWeightsUseSizeEstimate() {
    assertEquals(58.32, BodyWeightUtil.resolveWeightKg(null, 0.6, 1.8, 0.6, 1.8), 1.0e-9);
    assertEquals(58.32, BodyWeightUtil.resolveWeightKg(-4.0, 0.6, 1.8, 0.6, 1.8), 1.0e-9);
    assertEquals(58.32, BodyWeightUtil.resolveWeightKg(Double.NaN, 0.6, 1.8, 0.6, 1.8), 1.0e-9);
  }

  @Test
  void childReferenceWeightOverridesParentAndMissingWeightUsesFirstConfiguredParent() {
    assertEquals(12.0, BodyWeightUtil.inheritReferenceWeight(12.0, java.util.List.of(30.0, 40.0)));
    assertEquals(30.0, BodyWeightUtil.inheritReferenceWeight(null, java.util.Arrays.asList(null, 30.0, 40.0)));
    assertEquals(0.0, BodyWeightUtil.inheritReferenceWeight(null, java.util.List.of(0.0, 30.0)));
    assertNull(BodyWeightUtil.inheritReferenceWeight(null, java.util.List.of(-1.0, Double.NaN)));
  }

  @Test
  void invalidDimensionsReturnFiniteZero() {
    assertEquals(0.0, BodyWeightUtil.estimateReferenceWeightKg(0.0, 1.8));
    assertEquals(0.0, BodyWeightUtil.scaleByBodyVolume(58.32, 0.6, 1.8, Double.NaN, 0.9));
    assertEquals(0.0, BodyWeightUtil.estimateReferenceWeightKg(Double.MAX_VALUE, Double.MAX_VALUE));
    assertEquals(0.0, BodyWeightUtil.scaleByBodyVolume(58.32, 0.6, 1.8, Double.MAX_VALUE, Double.MAX_VALUE));
  }
}
