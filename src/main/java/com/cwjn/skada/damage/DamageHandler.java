package com.cwjn.skada.damage;

import com.cwjn.skada.CommonConfig;
import com.cwjn.skada.Skada;
import com.cwjn.skada.data.damage.AccessProjectileData;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.ElementSpreadInstance;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.event.custom.PostMitigationEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.cwjn.skada.util.ConsoleColour.*;

@Mod.EventBusSubscriber
public class DamageHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void doDamageCalculation(LivingHurtEvent event) {
        SkadaDamageSource source;
        LivingEntity target = event.getEntity();
        boolean isProjectile = false;
        Skada.LOGGER.debug(UNDERLINE + "Damage event for entity: {}, initial damage: {}, source: {}" + RESET, target.getName().getString(), event.getAmount(), event.getSource().getMsgId());
        double amount = event.getAmount();
        if (event.getSource() instanceof SkadaDamageSource) {
            Skada.LOGGER.debug("Damage source is a SkadaDamageSource.");
            source = (SkadaDamageSource) event.getSource();
        }
        else if (event.getSource().getDirectEntity() instanceof Projectile projectile) {
            AccessProjectileData proj = (AccessProjectileData) projectile;
            source = new SkadaDamageSource(event.getSource(), proj.getDamageInfo());
            isProjectile = true;
            Skada.LOGGER.debug("Damage source is a projectile with damage info: {}", proj.getDamageInfo());
        }
        else {
            Skada.LOGGER.debug("Damage source is not a SkadaDamageSource, creating environmental source.");
            source = SkadaDamageSource.environmental(event.getSource());
        }
        DamageInfo info = source.getInfo();
        ElementSpreadInstance spread = info.elementSpreadInstance();
        double armour = target.getAttributeValue(Attributes.ARMOR);
        Skada.LOGGER.debug("Damage info: {}, Element spread: {}, Armour: {}", info, spread, armour);

        if (source.is(SkadaDamageTypeTags.CANCELLED_BY_ARMOUR)) {
            if (armour != 0) {
                Skada.LOGGER.debug("Damage is cancelled by armour, cancelling event.");
                event.setCanceled(true);
                return;
            }
        }

        if (!info.isEnvironmental()) {

            if (CommonConfig.ENABLE_ACCURACY.get() && CommonConfig.ENABLE_ACCURACY_FOR_MELEE.get() && !isProjectile) {
                Skada.LOGGER.debug("Pre-accuracy damage: {}", amount);
                amount = getDamageFromAccuracyNormalDistribution(info.accuracy(), amount, event.getEntity().getRandom());
                Skada.LOGGER.debug("Post-accuracy damage: {}", amount);
            }

            if (CommonConfig.ENABLE_LETHALITY.get()) {
                double lethality = info.lethality();
                double toughness = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

                Skada.LOGGER.debug("Lethality: {}, Toughness: {}", lethality, toughness);

                Skada.LOGGER.debug("Pre-lethality damage: {}", amount);
                switch (info.attackType().type().getOperation()) {
                    case SUM_WITH_DAMAGE -> amount += info.attackType().type().apply(lethality, toughness, target.getMaxHealth());
                    case SUM_WITH_ARMOUR -> armour += info.attackType().type().apply(lethality, toughness, target.getMaxHealth());
                    case MULTIPLY_WITH_DAMAGE -> amount *= info.attackType().type().apply(lethality, toughness, target.getMaxHealth());
                    case MULTIPLY_WITH_ARMOUR -> armour *= info.attackType().type().apply(lethality, toughness, target.getMaxHealth());
                }
                Skada.LOGGER.debug("Post-lethality damage: {}", amount);
            }

            //ATTACK TYPE WEAKNESSES
            AttackType attackType = info.attackType();
            Skada.LOGGER.debug("Pre-attack type damage: {}", amount);
            amount -= (amount*resistanceReductionFormula(target.getAttributeValue(attackType.resistAttribute())));
            Skada.LOGGER.debug("Post-attack type damage: {}", amount);

        }

        //ARMOUR FORMULA
        Skada.LOGGER.debug("Pre-armour damage: {}", amount);
        amount = armourReductionFormula(amount, armour);
        Skada.LOGGER.debug("Post-armour damage: {}", amount);

        if (!source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            if (target.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                int i = (target.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
                int j = 25 - i;
                double f = amount * (float) j;
                double f1 = amount;
                amount = Math.max(f / 25.0F, 0.0F);
                double f2 = f1 - amount;
                if (f2 > 0.0F && f2 < 3.4028235E37F) {
                    if (target instanceof ServerPlayer) {
                        ((ServerPlayer) target).awardStat(Stats.DAMAGE_RESISTED, (int) Math.round(f2 * 10.0F));
                    } else if (source.getEntity() instanceof ServerPlayer) {
                        ((ServerPlayer) source.getEntity()).awardStat(Stats.DAMAGE_RESISTED, (int) Math.round(f2 * 10.0F));
                    }
                }
                Skada.LOGGER.debug("Post-resistance damage: {}", amount);
            }
            if (!source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
                int k = EnchantmentHelper.getDamageProtection(target.getArmorSlots(), source);
                if (k > 0) {
                    amount = CombatRules.getDamageAfterMagicAbsorb((float) amount, (float) k);
                }
                Skada.LOGGER.debug("Post-enchantment damage: {}", amount);
            }
        }

        //ELEMENTAL WEAKNESSES AND AFFINITIES
        spread.transform(amount);
        for (Element element : spread.getElements().keySet()) {
            if (source.getEntity() instanceof LivingEntity le) {
                Skada.LOGGER.debug("Element: {}, Base damage: {}, Affinity: {}, Pre-resistance damage: {}, Target Resistance: {}",
                        element.name(),
                        le.getAttributeValue(element.baseDamage()),
                        (1 + le.getAttributeValue(element.affinityAttribute())),
                        spread.getElements().get(element),
                        target.getAttributeValue(element.resistAttribute()));
                spread.applyFunctionToElement(element, x -> x + le.getAttributeValue(element.baseDamage()));
                spread.applyFunctionToElement(element, x -> x * (1 + le.getAttributeValue(element.affinityAttribute())));
            }
            spread.applyFunctionToElement(element, x -> x - (x * resistanceReductionFormula(target.getAttributeValue(element.resistAttribute()))));
            Skada.LOGGER.debug("Element: {}, Final damage: {}", element.name(), spread.getElements().get(element));
        }
        PostMitigationEvent evt = new PostMitigationEvent(target, spread.getElements());
        MinecraftForge.EVENT_BUS.post(evt);
        event.setAmount(evt.getTotalDamage());
    }

    /*
     * Base armour damage reduction formula. Returns the damage after reduction.
     */
    private static double armourReductionFormula(double damage, double armour) {
        //If the target has no armour, the attack does full damage.
        if (armour == 0) return damage;
        //Convert armour points to a percentage resistance. If armour is negative, the formula is inverted.
        double resistance = armour < 0 ? -100 / (1 + Math.exp((armour / 10) + 2)) : 100 / (1 + Math.exp((-armour / 10) + 2));
        //If the resistance is >= 100% (somehow), do no damage, otherwise, return the damage after reduction.
        return resistance >= 100 ? 0 : damage * (1 - resistance / 100);
    }

    /*
        * Return a number from representing the percentage of resistance reduction, where 1.0 is 100% reduction.
     */
    private static double resistanceReductionFormula(double resistance) {
        //each point of resistance is 10% reduction, this can also be negative.
        return resistance * 0.1;
    }

    /*
        * Get a percentage of damage total based on a normal distribution, where the damage total is the
        * mean and the accuracy is the standard deviation. Cannot go above the original damage value.
     */
    private static double getDamageFromAccuracyNormalDistribution(double accuracy, double damage, RandomSource random) {
        // Higher accuracy means lower standard deviation (more consistent damage)
        double standardDeviation = (1.0 - accuracy)*damage;

        // Generate a random number from a normal distribution
        double z = random.nextGaussian();

        // Since the mean is the original damage value, any damage value beyond the mean should
        // be treated as a negative number. We'll take the absolute value of z to ensure
        // we're always reducing the damage.
        double normalDistributionModifier = Math.abs(z) * standardDeviation;

        // Calculate final damage using the normal distribution
        // Clamp the result between 0.5x and the original damage value
        return Math.max(damage*0.5, damage - normalDistributionModifier);
    }

    /*
    * This method increases the element spread based on the attacker's finesse and the defender's mobility. Uses
    * a roll system, with 4 tiers.
     */
    private static int finesseMobilityFormula(ElementSpreadInstance spread, double finesse, double mobility, double secondaryStat, double agility, RandomSource random) {
        int difference = Math.min((int) ((finesse*secondaryStat*0.1)-(mobility*agility*0.1)), 300);
        if (difference == 0) return 0;
        if (difference > 0) {
            int extraHits = 0;
            while (true) {
                double rate = 0.5 - extraHits*0.25;
                if (extraHits == 1) rate = 0.33;
                if (difference < 100) {
                    if (rate*difference*0.01 > random.nextDouble()) {
                        extraHits++;
                    }
                    break;
                }
                else {
                    difference-=100;
                    if (rate > random.nextDouble()) {
                        extraHits++;
                    }
                    else {
                        break;
                    }
                }
            }
            return extraHits;
        }
        else {
            difference*=-1;
            int tier = 0;
            double damageReduc = 0.0;
            while (true) {
                double rate = 0.5 - tier * 0.25;
                if (tier == 1) rate = 0.33;
                if (difference < 100) {
                    if (rate * difference * 0.01 > random.nextDouble()) {
                        damageReduc += 16.5;
                    }
                    break;
                } else {
                    difference -= 100;
                    if (rate > random.nextDouble()) {
                        damageReduc += 16.5;
                        tier++;
                    } else {
                        break;
                    }
                }
            }
            double finalDamageReduc = damageReduc;
            spread.applyFunctionToAll(d -> d * (1 - finalDamageReduc * 0.01));
        }
        return 0;
    }

}
