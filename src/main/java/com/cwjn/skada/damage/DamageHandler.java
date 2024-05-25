package com.cwjn.skada.damage;

import com.cwjn.skada.SkadaRegistry;
import com.cwjn.skada.data.damage.DamageInfo;
import com.cwjn.skada.data.damage.ElementSpread;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.data.registry.Element;
import com.cwjn.skada.event.custom.PostMitigationEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.cwjn.skada.data.SkadaData.*;
import static com.cwjn.skada.data.damage.LethalityFunction.*;

@Mod.EventBusSubscriber
public class DamageHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void doDamageCalculation(LivingHurtEvent event) {
        SkadaDamageSource source;
        LivingEntity target = event.getEntity();
        //Skada.LOGGER.debug("Damage event for " + target.getName().getString() + " with amount " + event.getAmount() + " from " + event.getSource().getMsgId());
        double amount = event.getAmount();
        if (event.getSource() instanceof SkadaDamageSource) {
            //Skada.LOGGER.debug("Damage source is a SkadaDamageSource, using it.");
            source = (SkadaDamageSource) event.getSource();
        } else {
            //Skada.LOGGER.debug("Damage source is not a SkadaDamageSource, creating environmental source.");
            source = SkadaDamageSource.environmental(event.getSource());
        }
        DamageInfo info = source.getInfo();
        ElementSpread spread = info.elementSpread();
        double armour = target.getAttributeValue(Attributes.ARMOR);
        if (source.getInfo().isEnvironmental() && source.is(SkadaDamageTypeTags.BLOCKED_BY_ARMOUR)) {
            if (armour != 0) {
                //Skada.LOGGER.debug("Damage is environmental and blocked by armour, cancelling event.");
                event.setCanceled(true);
                return;
            }
        }

        if (!info.isEnvironmental()) {
            //Check attack aim vs defender evasion to determine hit chance
            double aim = info.aim();
            double evasion = target.getAttributeValue(SkadaRegistry.EVASIVENESS.get());
            double hitChance = aimEvasionFormula(aim, evasion);
            //Skada.LOGGER.debug("Hit chance: " + hitChance);
            if (hitChance < target.getRandom().nextDouble()) {
                event.setCanceled(true);
                return;
            }

            //Check attack lethality vs defender armour toughness to apply lethality function.
            double lethality = info.lethality();
            double toughness = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            //Skada.LOGGER.debug("Pre-lethality damage: " + amount);
            switch (info.attackType().type().getOperation()) {
                case SUM_WITH_DAMAGE -> amount += info.attackType().type().apply(lethality, toughness);
                case SUM_WITH_ARMOUR -> armour += info.attackType().type().apply(lethality, toughness);
                case MULTIPLY_WITH_DAMAGE -> amount *= info.attackType().type().apply(lethality, toughness);
                case MULTIPLY_WITH_ARMOUR -> armour *= info.attackType().type().apply(lethality, toughness);
            }
            //Skada.LOGGER.debug("Post-lethality damage: " + amount);

            //ATTACK TYPE WEAKNESSES
            AttackType attackType = info.attackType();
            //Skada.LOGGER.debug("Pre-attack type damage: " + amount);
            amount = amount * (1 / target.getAttributeValue(attackType.resistAttribute()));
            //Skada.LOGGER.debug("Post-attack type damage: " + amount);

            //ARMOUR FORMULA
            //Skada.LOGGER.debug("Pre-armour damage: " + amount);
            amount = armourReductionFormula(amount, armour);
            //Skada.LOGGER.debug("Pre-attack type damage: " + amount);
        }

        //ELEMENTAL WEAKNESSES
        spread.transform(amount);
        for (Element element : spread.getElements().keySet()) {
            if (source.getEntity() instanceof LivingEntity le) {
                spread.applyFunctionToElement(element, x -> x + le.getAttributeValue(element.baseDamage()));
                spread.applyFunctionToElement(element, x -> x * le.getAttributeValue(element.affinityAttribute()));
            }
            spread.applyFunctionToElement(element, x -> x * (1 / target.getAttributeValue(element.resistAttribute())));
        }
        PostMitigationEvent evt = new PostMitigationEvent(target, spread.getElements());
        MinecraftForge.EVENT_BUS.post(evt);

        event.setAmount((float) spread.sum());
    }

    private static double aimEvasionFormula(double aim, double evasion) {
        if (aim >= evasion) return 1;
        else {
            int difference = (int) (evasion - aim);
            return 0.05 + 0.05*difference;
        }
    }

    /*
     * Base armour damage reduction formula. Returns the damage after reduction.
     */
    private static double armourReductionFormula(double damage, double armour) {
        return damage / Math.pow(2, armour/damage);
    }

    /*
    * This method increases the element spread based on the attacker's finesse and the defender's mobility. Uses
    * a roll system, with 4 tiers.
     */
    private static int finesseMobilityFormula(ElementSpread spread, double finesse, double mobility, double secondaryStat, double agility, RandomSource random) {
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
