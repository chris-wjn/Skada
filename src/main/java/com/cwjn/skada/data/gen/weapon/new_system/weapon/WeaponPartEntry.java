package com.cwjn.skada.data.gen.weapon.new_system.weapon;

import com.cwjn.skada.data.gen.weapon.ExtraTierInfo;

import java.util.Objects;

public record WeaponPartEntry(WeaponPart part, ExtraTierInfo material) {
    public WeaponPartEntry {
        Objects.requireNonNull(part, "part");
        Objects.requireNonNull(material, "material");
        if (material.density() <= 0.0) {
            throw new IllegalArgumentException("material density must be > 0");
        }
    }
}
