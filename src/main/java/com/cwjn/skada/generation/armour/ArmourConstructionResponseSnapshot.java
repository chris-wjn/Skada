package com.cwjn.skada.generation.armour;

public record ArmourConstructionResponseSnapshot(
    double effectiveThickness,
    double paddingStrength,
    double continuityQuality,
    double rigidityQuality,
    double seamWeakness,
    double gapExposure,
    double deflectionQuality,
    double burdenFactor) {
}