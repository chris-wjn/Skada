package com.cwjn.skada.data.damage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/** The uses allowed to consume a weapon's shared configured attack profile. */
public enum WeaponStatApplicability {
    MELEE, PROJECTILE, BOTH;

    public static final Codec<WeaponStatApplicability> CODEC = Codec.STRING.comapFlatMap(value -> {
        try {
            return DataResult.success(valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown weapon stat applicability: " + value);
        }
    }, WeaponStatApplicability::serializedName);

    /** Returns the stable JSON/NBT spelling. */
    public String serializedName() { return name().toLowerCase(Locale.ROOT); }

    /** Whether configured statistics apply to direct attacks. */
    public boolean melee() { return this == MELEE || this == BOTH; }

    /** Whether configured statistics apply to launched attacks. */
    public boolean projectile() { return this == PROJECTILE || this == BOTH; }

    /** Preserves the meaning of data authored before applicability was explicit. */
    public static WeaponStatApplicability legacy(boolean ignoreAttributes) {
        return ignoreAttributes ? PROJECTILE : BOTH;
    }
}
