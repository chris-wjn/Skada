package com.cwjn.skada.generation.attack;

import com.cwjn.skada.generation.weapon.WeaponAssembly;
import com.cwjn.skada.generation.weapon.generation_algo.AttackSpeedGenerationUtil;
import com.cwjn.skada.generation.weapon.generation_algo.CriticalFailGenerationUtil;
import com.cwjn.skada.generation.weapon.generation_algo.LethalityGenerationUtil;
import com.cwjn.skada.generation.weapon.generation_algo.PrecisionGenerationUtil;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextFactory;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextSlash;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextStrike;
import com.cwjn.skada.generation.weapon.generation_algo.context.AttackGenerationContextThrust;
import java.util.Objects;
import java.util.function.Function;

/**
 * Configures the authoritative generator for an attack type. The generator calculates all four
 * statistics together, allowing physics and contact calculations to be shared within one invocation.
 * Attack capabilities select eligible parts; they never replace this configured calculation.
 */
public record AttackTypeGeneratorConfiguration(Function<WeaponAssembly, GeneratedStats> generator) {

  /** Raw generated statistics before attack metadata modifiers and attack-speed soft caps. */
  public record GeneratedStats(double precision, double lethality, double criticalFail, double attackSpeed) {
  }

  /** Requires an explicit generator rather than inferring one from attack capabilities. */
  public AttackTypeGeneratorConfiguration {
    Objects.requireNonNull(generator, "generator");
  }

  /** Invokes the configured generator once for this materialized assembly. */
  public GeneratedStats generateAll(WeaponAssembly assembly) {
    return generator.apply(assembly);
  }

  /** Generates the existing slash statistics from one shared slash context. */
  public static GeneratedStats generateSlash(WeaponAssembly profile) {
    AttackGenerationContextSlash context = AttackGenerationContextFactory.buildSlashContext(profile);
    return new GeneratedStats(
      PrecisionGenerationUtil.slash(context),
      LethalityGenerationUtil.slash(context),
      CriticalFailGenerationUtil.slash(context),
      AttackSpeedGenerationUtil.slash(context));
  }

  /** Generates the existing thrust statistics from one shared thrust context. */
  public static GeneratedStats generateThrust(WeaponAssembly profile) {
    AttackGenerationContextThrust context = AttackGenerationContextFactory.buildThrustContext(profile);
    return new GeneratedStats(
      PrecisionGenerationUtil.thrust(context),
      LethalityGenerationUtil.thrust(context),
      CriticalFailGenerationUtil.thrust(context),
      AttackSpeedGenerationUtil.thrust(context));
  }

  /** Generates the existing strike statistics from one shared strike context. */
  public static GeneratedStats generateStrike(WeaponAssembly profile) {
    AttackGenerationContextStrike context = AttackGenerationContextFactory.buildStrikeContext(profile);
    return new GeneratedStats(
      PrecisionGenerationUtil.strike(context),
      LethalityGenerationUtil.strike(context),
      CriticalFailGenerationUtil.strike(context),
      AttackSpeedGenerationUtil.strike(context));
  }

}
