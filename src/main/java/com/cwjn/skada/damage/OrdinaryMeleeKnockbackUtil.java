package com.cwjn.skada.damage;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Shared bounded ordinary melee impulse and scoped acceptance state for real mob attack methods. */
public final class OrdinaryMeleeKnockbackUtil {
    public static final double MAX_COMBINED_KNOCKBACK = 4.0;
    private static final ThreadLocal<Deque<AttackContext>> ATTACKS = ThreadLocal.withInitial(ArrayDeque::new);

    /** Prevents construction of this stateless attack utility. */
    private OrdinaryMeleeKnockbackUtil() {}

    /** Keeps the ordinary scalar finite, non-negative, and within the calibrated post-bonus cap. */
    public static double boundedKnockback(double knockback) {
        if (Double.isNaN(knockback) || knockback <= 0.0) return 0.0;
        if (Double.isInfinite(knockback)) return knockback > 0.0 ? MAX_COMBINED_KNOCKBACK : 0.0;
        return Math.min(MAX_COMBINED_KNOCKBACK, knockback);
    }

    /** Begins a source-scoped attack frame using the entity's authored/held attack and enchantment values. */
    public static void beginAttack(LivingEntity attacker, Entity target) {
        double knockback = attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        if (target instanceof LivingEntity) knockback += EnchantmentHelper.getKnockbackBonus(attacker);
        ATTACKS.get().push(new AttackContext(attacker, target, boundedKnockback(knockback)));
    }

    /** Records ordinary damage acceptance, including an actual shield block that prevents hurt(). */
    public static boolean recordResult(DamageSource source, Entity target, boolean accepted) {
        AttackContext context = current();
        if (context == null || context.attacker != source.getEntity() || context.target != target) return accepted;
        context.accepted |= accepted;
        return accepted;
    }

    /** Marks only a real vanilla shield-block callback as accepted physical contact. */
    public static void recordShieldContact(LivingEntity attacker, LivingEntity target) {
        AttackContext context = current();
        if (context != null && context.attacker == attacker && context.target == target) {
            context.accepted = true;
        }
    }

    /** True only for the exact attacker/target pair while one of this module's ordinary attack methods is active. */
    public static boolean isCurrentAttack(LivingEntity attacker, Entity target) {
        AttackContext context = current();
        return context != null && context.attacker == attacker && context.target == target;
    }

    /** Suppresses only LivingEntity.hurt's overlapping ordinary shove during a scoped attack method. */
    public static boolean shouldSuppressHurtKnockback(DamageSource source, LivingEntity target) {
        return source.getEntity() instanceof LivingEntity attacker && isCurrentAttack(attacker, target);
    }

    /** Ends the current attack frame and applies the single approved ordinary impulse when the attack was accepted. */
    public static void finishAttack(LivingEntity attacker, Entity target) {
        Deque<AttackContext> stack = ATTACKS.get();
        AttackContext context = stack.peek();
        if (context == null || context.attacker != attacker || context.target != target) return;
        stack.pop();
        if (stack.isEmpty()) ATTACKS.remove();
        if (context.accepted && target instanceof LivingEntity living && context.knockback > 0.0) {
            apply(attacker, living, context.knockback);
        }
    }

    /** Applies the bounded scalar through vanilla LivingEntity.knockback, which consumes resistance once. */
    public static void apply(LivingEntity attacker, LivingEntity target, double knockback) {
        double bounded = boundedKnockback(knockback);
        if (bounded <= 0.0) return;
        target.knockback(bounded * 0.5, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
    }

    /** Returns the active nested attack frame, clearing the thread local when no frame exists. */
    private static AttackContext current() {
        Deque<AttackContext> stack = ATTACKS.get();
        if (stack.isEmpty()) {
            ATTACKS.remove();
            return null;
        }
        return stack.peek();
    }

    /** Captures one attack's source, target, push amount, and accepted-contact result. */
    private static final class AttackContext {
        private final LivingEntity attacker;
        private final Entity target;
        private final double knockback;
        private boolean accepted;

        /** Creates a scoped frame for one attack invocation. */
        private AttackContext(LivingEntity attacker, Entity target, double knockback) {
            this.attacker = attacker;
            this.target = target;
            this.knockback = knockback;
        }
    }
}
