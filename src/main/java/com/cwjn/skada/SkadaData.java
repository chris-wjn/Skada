package com.cwjn.skada;

import com.cwjn.skada.damage.DamageClass;

import java.util.HashSet;
import java.util.Set;

/*
    * A class to hold global data for Skada.
*/
public abstract class SkadaData {

    private static final Set<DamageClass> DAMAGE_CLASSES = new HashSet<>();
    public static void registerDamageClass(DamageClass damageClass) {
        DAMAGE_CLASSES.add(damageClass);
    }
    public static DamageClass getDamageClassByName(String name) {
        for (DamageClass damageClass : DAMAGE_CLASSES) {
            if (damageClass.name().equals(name)) {
                return damageClass;
            }
        }
        return null;
    }

}
