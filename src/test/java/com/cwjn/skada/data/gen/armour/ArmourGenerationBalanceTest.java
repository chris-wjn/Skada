package com.cwjn.skada.data.gen.armour;

import com.cwjn.skada.data.armour.ArmourInfo;
import com.cwjn.skada.data.gen.attack.ElementSpread;
import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import com.cwjn.skada.data.registry.AttackType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmourGenerationBalanceTest {

  private static final ArmourPieceInfo HELMET = new ArmourPieceInfo(0.2, 0.2, 0.2, 0.2, 0.15);
  private static final ArmourPieceInfo CHEST = new ArmourPieceInfo(0.35, 0.35, 0.35, 0.35, 0.4);
  private static final ArmourPieceInfo LEGS = new ArmourPieceInfo(0.29, 0.29, 0.29, 0.29, 0.3);
  private static final ArmourPieceInfo BOOTS = new ArmourPieceInfo(0.16, 0.16, 0.16, 0.16, 0.15);
  private static final List<ArmourPieceInfo> FULL_SET = List.of(HELMET, CHEST, LEGS, BOOTS);

  @BeforeAll
  static void bootstrap() {
    ArmourGenerationBootstrap.bootstrapRegistries();
  }

  @Test
  void denserConstructionProducesMoreBurdenThanLighterProtection() {
    MaterialInfo dense = new MaterialInfo(8.0, 4.0, 7.0, 2.0, new ElementSpread());
    MaterialInfo light = new MaterialInfo(1.0, 1.5, 5.0, 8.0, new ElementSpread());
    ArmourConstructionInfo construction = new ArmourConstructionInfo(4.5, 2, 2.5, 0.6, 0.8, 0.2, 0.6, 0.15, 1.0);

    ArmourInfo denseChest = ArmourInfo.generate(CHEST, dense, construction);
    ArmourInfo lightChest = ArmourInfo.generate(CHEST, light, construction);

    assertTrue(denseChest.burden() > lightChest.burden());
  }

  @Test
  void harderMaterialsImproveSlashResistanceWhenConstructionMatches() {
    MaterialInfo hard = new MaterialInfo(6.0, 8.5, 5.0, 2.0, new ElementSpread());
    MaterialInfo soft = new MaterialInfo(6.0, 1.5, 5.0, 2.0, new ElementSpread());
    ArmourConstructionInfo construction = new ArmourConstructionInfo(5.0, 1, 2.0, 0.8, 0.85, 0.15, 0.75, 0.25, 1.0);

    double hardSlash = resist(ArmourInfo.generate(CHEST, hard, construction), "slash");
    double softSlash = resist(ArmourInfo.generate(CHEST, soft, construction), "slash");

    assertTrue(hardSlash > softSlash);
  }

  @Test
  void tougherMaterialsImproveThrustResistanceWhenThicknessMatches() {
    MaterialInfo tough = new MaterialInfo(6.0, 4.0, 8.0, 2.0, new ElementSpread());
    MaterialInfo brittle = new MaterialInfo(6.0, 4.0, 1.0, 2.0, new ElementSpread());
    ArmourConstructionInfo construction = new ArmourConstructionInfo(5.0, 1, 2.0, 0.8, 0.85, 0.15, 0.75, 0.25, 1.0);

    double toughThrust = resist(ArmourInfo.generate(CHEST, tough, construction), "thrust");
    double brittleThrust = resist(ArmourInfo.generate(CHEST, brittle, construction), "thrust");

    assertTrue(toughThrust > brittleThrust);
  }

  @Test
  void paddingImprovesStrikeHandlingMoreThanSlashHandling() {
    MaterialInfo material = new MaterialInfo(6.0, 3.0, 6.0, 4.0, new ElementSpread());
    ArmourConstructionInfo unpadded = new ArmourConstructionInfo(3.5, 1, 0.0, 0.45, 0.75, 0.2, 0.35, 0.1, 0.9);
    ArmourConstructionInfo padded = new ArmourConstructionInfo(3.5, 1, 6.0, 0.45, 0.75, 0.2, 0.35, 0.1, 0.9);

    ArmourInfo unpaddedInfo = ArmourInfo.generate(CHEST, material, unpadded);
    ArmourInfo paddedInfo = ArmourInfo.generate(CHEST, material, padded);

    double strikeGain = resist(paddedInfo, "strike") - resist(unpaddedInfo, "strike");
    double slashGain = resist(paddedInfo, "slash") - resist(unpaddedInfo, "slash");

    assertTrue(strikeGain > slashGain);
  }

  @Test
  void poorSeamsAndHighGapExposurePunishThrustMoreThanSlash() {
    MaterialInfo material = new MaterialInfo(7.0, 4.0, 7.0, 2.0, new ElementSpread());
    ArmourConstructionInfo stable = new ArmourConstructionInfo(5.0, 1, 2.0, 0.8, 0.92, 0.08, 0.7, 0.2, 1.0);
    ArmourConstructionInfo exposed = new ArmourConstructionInfo(5.0, 1, 2.0, 0.8, 0.45, 0.45, 0.7, 0.2, 1.0);

    ArmourInfo stableInfo = ArmourInfo.generate(CHEST, material, stable);
    ArmourInfo exposedInfo = ArmourInfo.generate(CHEST, material, exposed);

    double thrustLoss = resist(stableInfo, "thrust") - resist(exposedInfo, "thrust");
    double slashLoss = resist(stableInfo, "slash") - resist(exposedInfo, "slash");

    assertTrue(thrustLoss > slashLoss);
  }

  @Test
  void vanillaProfilesPreserveIntendedSetIdentities() {
    ArmourConstructionInfo leather = new ArmourConstructionInfo(3.4, 2, 4.0, 0.28, 0.58, 0.24, 0.3, 0.08, 0.75);
    ArmourConstructionInfo mail = new ArmourConstructionInfo(3.2, 2, 3.5, 0.45, 0.72, 0.3, 0.38, 0.18, 0.95);
    ArmourConstructionInfo iron = new ArmourConstructionInfo(5.5, 1, 2.0, 0.85, 0.82, 0.18, 0.72, 0.35, 1.15);
    ArmourConstructionInfo gilded = new ArmourConstructionInfo(4.5, 1, 2.0, 0.65, 0.78, 0.2, 0.9, 0.18, 0.9);
    ArmourConstructionInfo refined = new ArmourConstructionInfo(4.4, 2, 3.0, 0.92, 0.94, 0.08, 0.9, 0.28, 1.0);
    ArmourConstructionInfo heavy = new ArmourConstructionInfo(6.0, 1, 3.0, 0.95, 0.92, 0.1, 0.82, 0.45, 1.25);

    MaterialInfo leatherMaterial = new MaterialInfo(0.95, 1.2, 5.2, 8.5, new ElementSpread());
    MaterialInfo chainMaterial = new MaterialInfo(7.1, 4.3, 7.2, 6.2, new ElementSpread());
    MaterialInfo ironMaterial = new MaterialInfo(7.874, 4.0, 8.0, 2.0, new ElementSpread());
    MaterialInfo goldMaterial = new MaterialInfo(19.3, 2.8, 5.0, 7.5, new ElementSpread());
    MaterialInfo diamondMaterial = new MaterialInfo(3.53, 10.0, 2.0, 0.2, new ElementSpread());
    MaterialInfo netheriteMaterial = new MaterialInfo(9.0, 8.5, 9.0, 1.0, new ElementSpread());

    Totals leatherSet = totalSet(leatherMaterial, leather);
    Totals chainSet = totalSet(chainMaterial, mail);
    Totals ironSet = totalSet(ironMaterial, iron);
    Totals goldSet = totalSet(goldMaterial, gilded);
    Totals diamondSet = totalSet(diamondMaterial, refined);
    Totals netheriteSet = totalSet(netheriteMaterial, heavy);

    assertTrue(leatherSet.burden < ironSet.burden);
    assertTrue(chainSet.slashResist > leatherSet.slashResist);
    assertTrue(ironSet.armourBonus > leatherSet.armourBonus);
    assertTrue(diamondSet.armourBonus > ironSet.armourBonus);
    assertTrue(netheriteSet.armourBonus > diamondSet.armourBonus);
    assertTrue(netheriteSet.burden > diamondSet.burden);
    assertTrue(goldSet.burden < ironSet.burden);
  }

  private static Totals totalSet(MaterialInfo material, ArmourConstructionInfo construction) {
    double armourBonus = 0.0;
    double toughnessBonus = 0.0;
    double burden = 0.0;
    double slashResist = 0.0;
    for (ArmourPieceInfo piece : FULL_SET) {
      ArmourInfo info = ArmourInfo.generate(piece, material, construction);
      armourBonus += info.armourBonus();
      toughnessBonus += info.armourToughnessBonus();
      burden += info.burden();
      slashResist += resist(info, "slash");
    }
    return new Totals(armourBonus, toughnessBonus, burden, slashResist);
  }

  private static double resist(ArmourInfo info, String name) {
    return info.attackResists().entrySet().stream()
        .filter(entry -> name.equals(entry.getKey().name()))
        .mapToDouble(Map.Entry::getValue)
        .findFirst()
        .orElse(0.0);
  }

  private record Totals(double armourBonus, double toughnessBonus, double burden, double slashResist) {
  }
}