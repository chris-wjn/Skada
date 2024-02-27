package com.cwjn.skada.damage;

import com.cwjn.skada.Config;
import com.cwjn.skada.SkadaData;
import com.cwjn.skada.SkadaRegistry;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class DamageHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void doDamageCalculation(LivingHurtEvent event) {
        SkadaDamageSource source;
        LivingEntity target = event.getEntity();
        double amount = event.getAmount();
        if (event.getSource() instanceof SkadaDamageSource) {
            source = (SkadaDamageSource) event.getSource();
        } else {
            source = SkadaDamageSource.convert(event.getSource());
        }

        DamageInfo info = source.getInfo();
        ElementSpread spread = info.elementSpread();
        int extrahits = 0;
        double primaryStat = info.primaryStat();
        double secondaryStat = info.secondaryStat();
        double vitality = target.getAttributeValue(SkadaRegistry.VITALITY.get());
        double agility = target.getAttributeValue(SkadaRegistry.AGILITY.get());
        double armour = target.getAttributeValue(Attributes.ARMOR);

        //IMPACT VS GRIT
        if (Config.isArmourPenEnabled() && !info.isEnvironmental()) {
            double impact = info.impact();
            double grit = target.getAttributeValue(SkadaRegistry.GRIT.get());
            armour = impactGritFormula(armour, primaryStat, vitality, impact, grit);
        }

        //SUBTRACT ARMOUR FROM DAMAGE AND THEN TRANSFORM INTO ELEMENTAL DAMAGE
        amount = amount - armour;
        spread.transform(amount);

        //FINESSE VS MOBILITY
        if (Config.isGlancingBlowEnabled() && !info.isEnvironmental()) {
            double finesse = info.finesse();
            double mobility = target.getAttributeValue(SkadaRegistry.MOBILITY.get());
            extrahits = finesseMobilityFormula(spread, finesse, mobility, secondaryStat, agility, target.getRandom());
        }

        //DEFTNESS VS RESILIENCE
        if (Config.isCritDamageEnabled() && !info.isEnvironmental() && info.isCrit()) {
            double deftness = info.deftness();
            double resilience = target.getAttributeValue(SkadaRegistry.RESILIENCE.get());
            spread.applyFunctionToAll((x) -> {
                double ratio1 = ((primaryStat+secondaryStat)/20)*deftness;
                double ratio2 = ((vitality+agility)/20)*resilience;
                return x*(ratio1/ratio2);
            });
        }

        //DAMAGE CLASS WEAKNESSES
        DamageClass clazz = info.damageClass();
        if (SkadaData.DAMAGE_CLASSES.contains(clazz)) {
            spread.applyFunctionToAll((x) -> x * (1 / target.getAttributeValue(clazz.resistAttribute())));
        }

        //ELEMENTAL WEAKNESSES
        spread.applyFunctionToFire((x) -> x * (1 / target.getAttributeValue(SkadaRegistry.FIRE_RESIST.get())));
        spread.applyFunctionToCold((x) -> x * (1 / target.getAttributeValue(SkadaRegistry.COLD_RESIST.get())));
        spread.applyFunctionToLightning((x) -> x * (1 / target.getAttributeValue(SkadaRegistry.LIGHTNING_RESIST.get())));
        spread.applyFunctionToWater((x) -> x * (1 / target.getAttributeValue(SkadaRegistry.WATER_RESIST.get())));
        spread.applyFunctionToWind((x) -> x * (1 / target.getAttributeValue(SkadaRegistry.WIND_RESIST.get())));
        spread.applyFunctionToEarth((x) -> x * (1 / target.getAttributeValue(SkadaRegistry.EARTH_RESIST.get())));
        spread.applyFunctionToLight((x) -> x * (1 / target.getAttributeValue(SkadaRegistry.LIGHT_RESIST.get())));
        spread.applyFunctionToDark((x) -> x * (1 / target.getAttributeValue(SkadaRegistry.DARK_RESIST.get())));

        event.setAmount((float) spread.sum());

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

    /*
    * This method determines a multiplier for defender armor depending on a ratio of the attacker's impact to the defender's grit.
    * The ratio must be between 0.5 and 2, so we use Minecraft's clamp util.
     */
    private static double impactGritFormula(double armour, double strength, double vitality, double impact, double grit) {
        double ratio = Mth.clamp(
                ((strength*impact)/(vitality*grit)),
                0.5,
                2);
        if (ratio < 1) {
            return armour * (2*ratio - 1);
        }
        else if (ratio > 1) {
            return armour * ratio;
        }
        else return armour;
    }

}
