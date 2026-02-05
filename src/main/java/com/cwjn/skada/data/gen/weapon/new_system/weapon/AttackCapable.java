package com.cwjn.skada.data.gen.weapon.new_system.weapon;

import com.cwjn.skada.data.registry.AttackType;
import java.util.EnumSet;

public interface AttackCapable {
  
    EnumSet<AttackCapability> attackCapabilities();

    default boolean supports(AttackCapability capability) {
        return attackCapabilities().contains(capability);
    }

    /**
     * The ideal point of balance in cm along the length of the weapon for this part and attack type.
     * The ideal point of balance is defined as the ideal position for the point of balance to be
     * located for the optimal balance of handling and heft.
     * @param attackType attack type context
     * @return the ideal PoB in cm along the length of the weapon
     */
    double idealPointOfBalance(AttackType attackType);

    public enum AttackCapability {
      SLASH,
      THRUST,
      STRIKE
    }

}
