package com.cwjn.skada.data.damage;

public interface AccessProjectileData {

    DamageInfo getDamageInfo();
    void setDamageInfo(DamageInfo data);
    boolean hasDamageInfo();

    /** Transient flight context; consumed by the first shoot call and never reconstructed from the owner. */
    ProjectileLaunch getPendingLaunch();
    void setPendingLaunch(ProjectileLaunch launch);

}
