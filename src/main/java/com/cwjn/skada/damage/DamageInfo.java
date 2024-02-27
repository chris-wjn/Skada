package com.cwjn.skada.damage;

public record DamageInfo(double impact, double finesse, double deftness, double strength, double primaryStat, double secondaryStat,
                         boolean isCrit, boolean isEnvironmental, DamageClass damageClass, ElementSpread elementSpread) {

}
