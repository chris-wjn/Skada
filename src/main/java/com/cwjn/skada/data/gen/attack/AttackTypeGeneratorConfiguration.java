package com.cwjn.skada.data.gen.attack;

import com.cwjn.skada.data.gen.weapon.WeaponAssembly;
import com.cwjn.skada.data.gen.weapon.attack_capability.AttackCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.SlashCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.StrikeCapable;
import com.cwjn.skada.data.gen.weapon.attack_capability.ThrustCapable;
import com.cwjn.skada.data.gen.weapon.generation_algo.AttackSpeedGenerationUtil;
import com.cwjn.skada.data.gen.weapon.generation_algo.CriticalFailGenerationUtil;
import com.cwjn.skada.data.gen.weapon.generation_algo.LethalityGenerationUtil;
import com.cwjn.skada.data.gen.weapon.generation_algo.PrecisionGenerationUtil;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextFactory;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextSlash;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextStrike;
import com.cwjn.skada.data.gen.weapon.generation_algo.context.AttackGenerationContextThrust;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.TierStatFunctionInterface;

public record AttackTypeGeneratorConfiguration(TierStatFunctionInterface precision, TierStatFunctionInterface lethality, TierStatFunctionInterface critFail, TierStatFunctionInterface attackSpeed) {

  public record GeneratedStats(double precision, double lethality, double criticalFail, double attackSpeed) {
  }

  public double precision(WeaponAssembly profile) {
    return precision.apply(profile);
  }

  public double lethality(WeaponAssembly profile) {
    return lethality.apply(profile);
  }

  public double criticalFail(WeaponAssembly profile) {
    return critFail.apply(profile);
  }

  public double attackSpeed(WeaponAssembly profile) {
    return attackSpeed.apply(profile);
  }

  public GeneratedStats generateAll(WeaponAssembly profile, AttackType attackType) {
    Class<? extends AttackCapable> capableInterface = attackType.capableInterface();
    if (capableInterface != null) {
      if (SlashCapable.class.isAssignableFrom(capableInterface)) {
        return generateSlash(profile);
      }
      if (ThrustCapable.class.isAssignableFrom(capableInterface)) {
        return generateThrust(profile);
      }
      if (StrikeCapable.class.isAssignableFrom(capableInterface)) {
        return generateStrike(profile);
      }
    }
    return new GeneratedStats(precision(profile), lethality(profile), criticalFail(profile), attackSpeed(profile));
  }

  private static GeneratedStats generateSlash(WeaponAssembly profile) {
    AttackGenerationContextSlash context = AttackGenerationContextFactory.buildSlashContext(profile);
    return new GeneratedStats(
      PrecisionGenerationUtil.slash(context),
      LethalityGenerationUtil.slash(context),
      CriticalFailGenerationUtil.slash(context),
      AttackSpeedGenerationUtil.slash(context));
  }

  private static GeneratedStats generateThrust(WeaponAssembly profile) {
    AttackGenerationContextThrust context = AttackGenerationContextFactory.buildThrustContext(profile);
    return new GeneratedStats(
      PrecisionGenerationUtil.thrust(context),
      LethalityGenerationUtil.thrust(context),
      CriticalFailGenerationUtil.thrust(context),
      AttackSpeedGenerationUtil.thrust(context));
  }

  private static GeneratedStats generateStrike(WeaponAssembly profile) {
    AttackGenerationContextStrike context = AttackGenerationContextFactory.buildStrikeContext(profile);
    return new GeneratedStats(
      PrecisionGenerationUtil.strike(context),
      LethalityGenerationUtil.strike(context),
      CriticalFailGenerationUtil.strike(context),
      AttackSpeedGenerationUtil.strike(context));
  }

}
