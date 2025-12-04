package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.SkadaData;
import com.mojang.serialization.Codec;

public abstract class WeaponHead {

  /**
   * Get the length of the weapon head in its primary dimension (e.g., blade length, axe head width).
   * @return Length in millimeters.
   */
  public double getLength() {
    return 0;
  }

  /**
   * Get the width of the weapon head in its secondary dimension (e.g., blade width, axe head height).
   * @return Width in millimeters.
   */
  public double getWidth() {
    return 0;
  }

  /**
   * Get the volume of the weapon head.
   * @return Volume in cubic millimeters.
   */
  public double getVolume() {
    return 0;
  }

  /**
   * Get the point of balance for the weapon head.
   * @return Point of balance in millimeters from the base.
   */
  public double getPointOfBalance() {
    return 0;
  }

  public abstract Codec<? extends WeaponHead> type();

  public abstract String typeKey();

  public static final Codec<WeaponHead> DISPATCH_CODEC = Codec.STRING.dispatch(
          "typeKey",
          WeaponHead::typeKey,
          SkadaData.WEAPON_HEAD_CODECS::get
  );

  // Use the dispatch codec directly for WeaponHead encoding/decoding to avoid custom decode edge cases
  public static final Codec<WeaponHead> CODEC = DISPATCH_CODEC;

}
