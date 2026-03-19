
package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.MaterialInfo;
import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents an entry combining a weapon part with optional material properties,
 * placement, and orientation.
 * 
 * This record associates a specific {@link WeaponPart} with material characteristics
 * defined by {@link MaterialInfo} plus optional assembly-space placement and
 * orientation data. Material can be absent during assembly template loading and
 * supplied later at generation time.
 * 
 * @param part the weapon part being defined, must not be null
 * @param material the material information including density, hardness, flexibility,
 *                 and toughness; may be null until generation time. When present,
 *                 all properties must be greater than 0
 * @param position optional weapon-space translation in cm for placing the part in an assembly;
 *                 defaults to the origin when absent
 * @param transform optional orientation data for aligning the weapon part to weapon space;
 *                  defaults to the identity transform when absent
 * 
 * @throws NullPointerException if {@code part} is null
 * @throws IllegalArgumentException if any material property (density, hardness,
 *                                  flexibility, or toughness) is less than or equal to 0 when material is present
 */
public record WeaponPartEntry(WeaponPart part, MaterialInfo material, GeometryUtil.Vec3 position, WeaponPartTransform transform) {
    
    public WeaponPartEntry {
        Objects.requireNonNull(part, "part");
        if (material != null) {
            if (material.density() <= 0.0) {
                throw new IllegalArgumentException("material density must be > 0");
            }
            if (material.hardness() <= 0.0) {
                throw new IllegalArgumentException("material hardness must be > 0");
            }
            if (material.flexibility() <= 0.0) {
                throw new IllegalArgumentException("material flexibility must be > 0");
            }
            if (material.toughness() <= 0.0) {
                throw new IllegalArgumentException("material toughness must be > 0");
            }
        }
        if (position == null) position = new GeometryUtil.Vec3(0.0, 0.0, 0.0);
        if (transform == null) transform = WeaponPartTransform.identity();
    }

    public WeaponPartEntry withMaterial(MaterialInfo newMaterial) {
        return new WeaponPartEntry(part, newMaterial, position, transform);
    }

    public static final Codec<WeaponPartEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            WeaponPart.CODEC.fieldOf("part").forGetter(WeaponPartEntry::part),
            MaterialInfo.CODEC.optionalFieldOf("material").forGetter(entry -> Optional.ofNullable(entry.material())),
            GeometryUtil.Vec3.CODEC.optionalFieldOf("position").forGetter(entry -> Optional.ofNullable(entry.position())),
            WeaponPartTransform.CODEC.optionalFieldOf("transform").forGetter(entry -> Optional.ofNullable(entry.transform()))
    ).apply(instance, (part, material, position, transform) -> new WeaponPartEntry(
            part,
            material.orElse(null),
            position.orElse(null),
            transform.orElse(null))));

}
