package com.cwjn.skada;

import org.junit.jupiter.api.Test;

import com.cwjn.skada.util.Util;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkadaCommandGenerationLookupTest {

  @Test
  void profileFallbackPrefersLongestMatchingSuffix() {
    String resolved = Util.findClosestMatch(
        List.of("sword", "longsword", "axe"),
      "steel_longsword");

    assertEquals("longsword", resolved);
  }

  @Test
  void armourFallbackMapsMaterialToConstruction() {
    assertEquals("mail_basic", SkadaCommand.resolveDefaultArmourConstruction("chain"));
    assertEquals("plate_refined", SkadaCommand.resolveDefaultArmourConstruction("minecraft.diamond"));
    assertEquals("plate_basic", SkadaCommand.resolveDefaultArmourConstruction("modded_bronze"));
  }
}