# Overview

This design doc will detail the stats on weapons and armour that relate to physics and geometry. It does not cover elemental damage or attack types directly, but may mention them as they relate.

# Weapon Stats

There are two phases of cutting and piercing, the initial surface penetration, and the steady state. `.github/research/swordstem-research.md`.

## Initial Surface Penetration (Precision)

This is the part of the cut or stab where the weapon has just made contact with the target. The ability to successfully initiate penetration through a target's surface barrier will be known as **Precision**.

**Physical Process**:

1. Weapon contacts target surface
2. Surface deforms inward (elastic then plastic deformation)
3. Local stress concentrates at contact point
4. When stress exceeds material's shear strength → penetration begins
5. Transition to steady state phase.

**Governing Equation**:

$σ = F / A$, $σ$ = contact stress ($P_a$)

Where:
  
  $F$ = Applied force ($N$)
  $A$ = Contact area ($m²$)
  
The weapon is able to successfully penetrate when contact stress exceeds target's shear strength.

**Gameplay Effects**:

In real life, if an attack is not able to successfully penetrate the target, damage would be negligible. But, this would not be fun in a video game context, especially considering that real life combat is more "armour-sided".

Instead, what we'll do is use contact stress as the weapon's offensive **precision** and the target's shear strength as a defensive **toughness**. We'll use a function of precision and toughness as the variance of the damage of the weapon on a bell curve. So, when precision is much greater than toughness, the damage done on the attack is what is advertised on the weapon. When precision is lower than toughness, there is more variability in damage dealt.

**Note**: Minecraft has an attribute called Armor Toughness. We will rename this to toughness and use it as our toughness stat.

For example, a diamond axe has 9 damage. If the precision of a diamond axe slash attack is much greater than the toughness of the target, there is a near 100% chance to deal 9 damage (before other damage calculation modifiers). However if the precision of a diamond axe slash attack is lower than the toughness target, the chance of dealing 9 damage (before other damage calculation modifiers) is much lower than 100%.

To summarize, we use the attack's damage as the mean of a normal distribution, and a function of precision and toughness as variance. A higher ratio of precision to toughness means lower variance, and vice versa. Whatever number we get from this normal distribution will be the damage used in the rest of damage calculation.

**What governs precision?** We must look at attack type separately, since they behave in different ways.

### Slash

For slash, we're interested primarily in the sharpness of the blade. This is defined primarily by the edge radius of the blade, which is measured in nanometres. Any weapon that is capable of slash attacks must have an edge radius field.

Secondary to edge radius is edge angle. We measure bevel angles from the centreline of the cross-section, not the entire internal angle. Usually, a slashing weapon will only have one bevel angle, which is formed by the primary bevel. This angle can be found using the width and height of a cross-section. Sometimes, a slashing weapon can have a field named edgeBevelAngle, denoting a very small edge bevel that has a different angle from the primary bevel. This field is in degrees, and also measured from the centreline of the cross-section.

Double vs single-edged makes no difference for slash, since we're only interested in the edge radius and angle.

In the future, we may look into edge alignment as a skill for potential RPG mods we make. For Skada, though, we assume the player always has perfect edge alignment so we do not consider it.

### Thrust

Since stress is naturally more concentrated on a point than along an edge, thrust attacks will generally have higher precision values than slash attacks.

The primary factor of thrust precision is the point sharpness, which is defined by the 