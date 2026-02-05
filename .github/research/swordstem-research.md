# SwordSTEM.com Research Document
## Comprehensive Weapon Physics Resource Guide

*This document is generated research for the Skada Minecraft mod to assist with realistic weapon physics implementation.*

---

## Overview

**SwordSTEM.com** is a HEMA (Historical European Martial Arts) focused website that applies scientific principles to sword fighting, with emphasis on physics, material science, and biomechanics. It is authored primarily by **Sean Franklin**, who has a Bachelor's Degree in Mechatronic Systems Engineering and works as a Controls Engineer.

The site provides evidence-based analysis of sword mechanics, making it invaluable for implementing realistic weapon physics in games.

---

## Primary Categories

### 1. Physics
Core physics articles covering weapon mechanics and combat dynamics.

### 2. Cutting
Analysis of cutting mechanics, blade geometry, and cutting phases.

### 3. Impact
Studies on impact forces, momentum transfer, and injury mechanics.

### 4. Material Science
Properties of steel, material fatigue, and gear durability.

### 5. Sport Science / Physiology
Biomechanics of sword fighting and human performance factors.

---

## Key Physics Concepts for Game Implementation

### Rotational vs Translational Motion
**Source**: [Rotational (Spinny) & Translational (Straight-ey) Motion & Swords](https://swordstem.com/2018/05/10/rotational-spinny-translational-straight-ey-motion-swords/)

**Key Points**:
- Swords achieve damage through **rotational motion** creating speed multiplication at the tip
- **Hilt velocity ≠ Tip velocity** - rotation around the body creates much higher tip speed
- Moving the Center of Rotation to the center of the body produces optimal tip speed
- Poor cutting technique often comes from increasing hilt speed instead of improving rotation
- A skilled fighter can cut powerfully while appearing to move slowly due to good rotation mechanics

**Formulas**:
- Linear kinetic energy: `KE = ½mv²`
- Rotational kinetic energy: `KE_rot = ½Iω²`
- Doubling mass doubles kinetic energy
- Doubling speed **quadruples** kinetic energy

**Implementation Notes**:
- Tip speed should scale with rotation quality, not just "swing speed"
- Attack speed attributes should affect tip velocity through rotation mechanics

---

### Moment of Inertia (MoI)
**Sources**: 
- [Center of Percussion? Vibration Node? Balance Point?](https://swordstem.com/2018/04/19/center-of-percussion-vibration-node-balance-point-what-does-it-all-mean/)
- [An Alternative Method for MoI Determination](https://swordstem.com/2021/03/30/an-alternative-method-for-moi-determination/)

**Key Points**:
- MoI measures how difficult it is to spin something (resistance to rotational acceleration)
- Two ways to increase MoI:
  1. Increase total mass
  2. Move mass further from the rotation point
- **Balance point alone is insufficient** - two swords with identical weight and balance can have very different MoI
- MoI determines effective "weight" in rotation
- Higher MoI = harder for target to stop the blade

**Relationship to Torque**:
```
τ = α × I
Where:
  τ = Torque (moment)
  α = Rotational acceleration
  I = Moment of Inertia
```

**Parallel to Linear Motion**:
- Force = mass × acceleration (`F = ma`)
- Torque = MoI × angular acceleration (`τ = Iα`)

**Implementation Notes**:
- Weapons with mass distributed toward the tip (higher MoI) hit harder but are slower
- Weapons with mass concentrated near the hilt (lower MoI) are more agile but deliver less force
- This is the physics basis for the "blade-heavy vs pommel-heavy" sword tradeoff

---

### Balance Point (Center of Mass)
**Source**: [Center of Percussion? Vibration Node? Balance Point?](https://swordstem.com/2018/04/19/center-of-percussion-vibration-node-balance-point-what-does-it-all-mean/)

**Key Points**:
- The point where the sword balances
- Where torque of the sword's mass equalizes
- Heavy pommel close to balance point can compensate for lighter blade far from balance point
- **You can NEVER make a sword rotate faster by adding weight** - only redistribute existing mass
- Balance point alone tells you relatively little about handling

**Implementation Notes**:
- Balance point affects sword feel but is not the primary determinant of damage or speed
- Use in combination with MoI for accurate physics

---

### Center of Percussion (CoP)
**Source**: [Center of Percussion? Vibration Node? Balance Point?](https://swordstem.com/2018/04/19/center-of-percussion-vibration-node-balance-point-what-does-it-all-mean/)

**Key Points**:
- **NOT the same as vibration node** (common misconception)
- The point where impact causes no reaction force at the hand
- If you hit at CoP, the sword tip stopping transmits no shock to your hand
- Varies based on grip position (center of rotation)
- Connected to pivot point geometry through mathematical relationship

**Implementation Notes**:
- CoP determines the "sweet spot" for maximum damage with minimum feedback
- Hitting at CoP provides the most efficient energy transfer to target

---

### Vibrational Node
**Source**: [Center of Percussion? Vibration Node? Balance Point?](https://swordstem.com/2018/04/19/center-of-percussion-vibration-node-balance-point-what-does-it-all-mean/)

**Key Points**:
- Two points where the sword doesn't vibrate when struck
- One roughly ⅔ toward the tip
- Well-designed swords have the other node at the hand position
- Changes based on boundary conditions (free vs gripped vs embedded in target)

---

### Impact, Impulse, and Momentum
**Source**: [Impact, Impulse, and Momentum](https://swordstem.com/2018/06/20/impact-impulse-and-momentum/)

**Key Concepts**:

**Momentum** (`p = mv`):
- Speed × weight
- Doubling speed has same momentum effect as doubling weight
- Rotational equivalent: `L = Iω` (angular momentum)

**Impulse** (`J = FΔt`):
- Force acting over time
- Changes momentum
- Same impulse spread over longer time = lower peak force (why padding works)

**Peak Force**:
- What actually causes damage
- Hard impacts (short time) = high peak force
- Soft impacts (long time) = lower peak force
- Striking hard body parts (elbow) produces higher peak force than soft parts (bicep)

**Implementation Notes**:
- Armor should extend impact time, reducing peak force
- Damage should be based on peak force, not raw momentum
- Body structure/resistance affects how quickly a blow decelerates

---

### What Does "Hitting Hard" Mean?
**Source**: [What Does Hitting Hard Mean?](https://swordstem.com/2018/09/05/what-does-hitting-hard-mean/)

**Key Factors**:

1. **Speed** - Quadratic effect on kinetic energy
2. **Body Structure** - Locked vs relaxed joints at impact
3. **Momentum Transfer** - Weight behind the blow
4. **Sharp vs Blunt Mechanics**:
   - Sharp: Separates material, wants to keep speed through target
   - Blunt: Transfers energy into target, disrupts internal structure

**Key Insight**:
> "Throwing more weight into a cut does wonders for blunt damage, but doesn't add much to cutting potential"

**Implementation Notes**:
- Sharp weapons: damage scales primarily with tip speed
- Blunt weapons: damage scales with both speed and mass behind blow
- This explains why maces are heavy but swords need to be fast

---

### Two Phases of Cutting
**Source**: [Two Phases Of a Cut – Surface vs Steady State](https://swordstem.com/2021/01/06/two-phases-of-a-cut-surface-vs-steady-state/)

**Phase 1: Surface Penetration**
- Blade pushes surface inward
- Force increases continuously
- Sharp vs dull blade has minimal difference
- Must exceed shear stress of material to proceed

**Phase 2: Steady State (Internal Cutting)**
- Once penetrated, blade acts as wedge
- Force levels off to constant value
- Force is equal to or less than peak surface penetration force
- Blade sharpness matters more in surface phase

**Slicing Effect**:
- Adding lateral movement dramatically reduces force needed for surface penetration
- Less effect on internal cutting phase
- Allows cuts at force levels that wouldn't otherwise penetrate

**Implementation Notes**:
- Initial hit determines if target is penetrated
- Once penetrated, resistance is more predictable
- Slashing attacks (with lateral motion) have lower penetration threshold

---

### Punches vs Sword Cuts
**Source**: [Punches Hit Harder Than Cuts?](https://swordstem.com/2022/03/07/punches-hit-harder-than-cuts/)

**Counterintuitive Finding**:
- Punches transfer more momentum to target than sword cuts
- Swords are designed to cut through, not knock things over
- Sword cuts have high tip speed but low momentum transfer

**Implementation Notes**:
- Don't equate "damage" with "knockback"
- Sharp weapons should deal damage without significant knockback
- Blunt weapons should provide more knockback

---

### Wrist Acceleration in Cuts
**Source**: [Accelerating the Wrist in a Cut – It Doesn't Work!](https://swordstem.com/2019/01/09/accelerating-the-wrist-in-a-cut-it-doesnt-work/)

**Key Finding**:
> "You CAN NOT have acceleration at the wrist AND a locked structure at the moment of contact!"

**Why**:
- Wrist acceleration can increase tip speed during the swing
- But you must lock structure before impact for good cut
- Locking structure means stopping wrist acceleration
- Tip speed returns to what it would have been without wrist accel

**Implementation Notes**:
- "Snapping" attacks shouldn't add damage
- Damage comes from proper rotation mechanics, not wrist/elbow acceleration

---

## Material Science

### Steel Properties
**Sources**: 
- [Steel: Why?](https://swordstem.com/2018/11/14/steel-why/)
- [Steel: What?](https://swordstem.com/2018/11/21/586/)
- [Introduction to Material Properties](https://swordstem.com/2018/08/08/introduction-to-material-properties/)

**Why Steel for Swords**:
- Optimal balance of hardness, toughness, and resilience
- Can be hardened and tempered for edge retention
- Flexible enough to absorb shock without shattering

**Key Material Properties**:
- **Hardness**: Resistance to deformation
- **Toughness**: Resistance to fracture
- **Yield Strength**: Stress at which permanent deformation begins
- **Ultimate Strength**: Maximum stress before failure

### Gear Fatigue
**Sources**:
- [Why Does my Gear Crack in Half?](https://swordstem.com/2018/12/19/why-does-my-gear-crack-in-half/)
- [Do Swords Really Wear Out?](https://swordstem.com/2019/10/30/do-swords-really-wear-out/)

**Key Points**:
- Cracks start as microfractures
- Repeated stress below yield strength can still cause failure (fatigue)
- Steel can take "unlimited" cycles below certain stress threshold
- Impacts create stress concentrations at existing flaws

### Brittle vs Ductile
**Source**: [Brittle vs Ductile and Swords in Freezers](https://swordstem.com/2020/01/08/brittle-vs-ductile-and-swords-in-freezers/)

**Key Points**:
- Cold increases brittleness
- Ductile failure: material deforms before breaking
- Brittle failure: sudden fracture without warning

---

## External Resources (Referenced by SwordSTEM)

### 1. Ensis Sub Caelo
**URL**: http://blog.subcaelo.net/ensis/
**Author**: Vincent Le Chevalier

**Key Resources**:
- [Weapon Dynamics Computer](https://subcaelo.net/ensis/dynamics-computer/) - Tool for calculating sword dynamics
- [Measuring Swords as Pendulums](http://blog.subcaelo.net/ensis/measuring-swords-pendulums/) - Precise MoI measurement technique
- [Documenting the Dynamics of Swords](http://blog.subcaelo.net/ensis/documenting-dynamics-of-swords/) - Complete measurement protocol

**Key Concepts from Ensis**:
- **Waggle Test**: Light grip, quick side-to-side motion to find pivot point
- **Pendulum Test**: More accurate MoI measurement using timed oscillations
- **Effective Mass Curves**: Shows how much mass contributes to impact at different blade positions

**Formula from Pendulum Testing**:
```
T = 2π√(I/mgd)
Where:
  T = Period of oscillation
  I = Moment of inertia about pivot
  m = Mass
  g = Gravity
  d = Distance from pivot to center of mass
```

### 2. George Turner's "Sword Motions and Impacts" (ARMA)
**URL**: http://www.thearma.org/spotlight/GTA/motions_and_impacts.htm

**Revolutionary Findings**:
- Authentic historical swords had pivot points carefully placed at or near the tip
- Modern reproductions often have pivot points 10-20 inches past the tip
- This represents lost knowledge of sword design

**Pivot Point Geometry**:
- Impact point and pivot point are always on opposite sides of balance point
- The relationship follows a rectangle with constant area
- For any weapon: `distance_impact × distance_pivot = constant`
- The constant equals `I/m` (MoI divided by mass)

**Pommel Function**:
- Not primarily for "balance" as often stated
- Controls the rotational behavior of the sword
- Determines where the sword pivots during impact
- Too large a pommel moves pivot past tip (bad)
- Too small allows uncontrolled rotation

**Pendulum Equivalence**:
- A sword hanging by the cross and swinging behaves like a simple pendulum
- The pendulum length is the distance from hand to hand's natural pivot point
- This is the sword's "natural length" for rotation purposes

### 3. Science of Sharp
**URL**: https://scienceofsharp.wordpress.com/

**Focus**: Microscopic analysis of edge geometry and sharpening
**Useful For**: Understanding what "sharpness" actually means mechanically

### 4. Sparky's Sword Science
**URL**: https://sparkyswordscience.blogspot.com/
**Author**: David Farrell

**Focus**: Technical concepts explained in layman's terms
- Equipment safety
- Materials science
- Impact mechanics

---

## Key Physics Formulas Summary

### Linear Motion
| Quantity | Formula | Units |
|----------|---------|-------|
| Momentum | p = mv | kg⋅m/s |
| Kinetic Energy | KE = ½mv² | Joules |
| Impulse | J = FΔt | N⋅s |
| Force | F = ma | Newtons |

### Rotational Motion
| Quantity | Formula | Units |
|----------|---------|-------|
| Angular Momentum | L = Iω | kg⋅m²/s |
| Rotational KE | KE = ½Iω² | Joules |
| Torque | τ = Iα | N⋅m |
| Angular Velocity | ω = v/r | rad/s |

### Derived Quantities
| Quantity | Formula | Notes |
|----------|---------|-------|
| Tip Velocity | v_tip = ω × r_tip | r = distance from rotation center |
| Effective Mass | m_eff = I/r² | At distance r from pivot |
| Pendulum Period | T = 2π√(I/mgd) | d = pivot to CoM distance |

---

## Implementation Recommendations for Skada

### Damage Calculation
1. Calculate angular velocity from torque and MoI: `ω = √(2τ/I)`
2. Calculate tip velocity from rotation: `v_tip = ω × length`
3. Calculate kinetic energy: `KE = ½Iω²`
4. Apply attack type modifiers (slash vs thrust vs strike)
5. Account for armor damage absorption (extends impact time)

### Weapon Properties to Model
1. **Mass** - Total weight
2. **Balance Point** - Distance from hilt to center of mass
3. **Moment of Inertia** - Resistance to rotation
4. **Effective Length** - Handle + blade contributing to rotation
5. **Edge Geometry** - For cutting calculations

### Attack Type Differentiation
- **Slash**: High tip velocity, uses rotation, affected by edge alignment
- **Thrust**: Lower velocity, concentrated force over small area (pressure)
- **Strike**: Momentum transfer, less affected by edge geometry

### Armor Interaction
- Armor should extend impact duration, reducing peak force
- Padded armor: significant force reduction
- Rigid armor: force dispersion over larger area
- No armor: short impact time, high peak force

---

## Article Index by Relevance to Skada

### Tier 1: Essential Reading
| Article | URL | Topic |
|---------|-----|-------|
| Center of Percussion | https://swordstem.com/2018/04/19/center-of-percussion-vibration-node-balance-point-what-does-it-all-mean/ | Core physics concepts |
| Rotational Motion | https://swordstem.com/2018/05/10/rotational-spinny-translational-straight-ey-motion-swords/ | Sword rotation mechanics |
| Impact, Impulse, Momentum | https://swordstem.com/2018/06/20/impact-impulse-and-momentum/ | Damage physics |
| What Does Hitting Hard Mean | https://swordstem.com/2018/09/05/what-does-hitting-hard-mean/ | Damage factors |
| Two Phases of a Cut | https://swordstem.com/2021/01/06/two-phases-of-a-cut-surface-vs-steady-state/ | Cutting mechanics |

### Tier 2: Valuable Context
| Article | URL | Topic |
|---------|-----|-------|
| MoI Determination | https://swordstem.com/2021/03/30/an-alternative-method-for-moi-determination/ | Physics measurement |
| Accelerating the Wrist | https://swordstem.com/2019/01/09/accelerating-the-wrist-in-a-cut-it-doesnt-work/ | Cutting mechanics |
| Punches vs Cuts | https://swordstem.com/2022/03/07/punches-hit-harder-than-cuts/ | Momentum transfer |
| Steel Why/What | https://swordstem.com/2018/11/14/steel-why/ | Material properties |
| Material Properties | https://swordstem.com/2018/08/08/introduction-to-material-properties/ | Material science |

### Tier 3: Advanced Topics
| Article | URL | Topic |
|---------|-----|-------|
| Force, Deceleration | https://swordstem.com/2019/06/12/force-deceleration-and-how-to-stab-through-a-tank/ | Thrust mechanics |
| Why Do Polearms Hurt | https://swordstem.com/2019/05/01/why-do-polearms-hurt/ | Lever mechanics |
| Blade Stiffness | https://swordstem.com/2020/12/22/difficulties-with-sca-flex-test-and-buckling-test-for-measuring-blade-stiffness/ | Blade properties |
| Gear Cracking | https://swordstem.com/2018/12/19/why-does-my-gear-crack-in-half/ | Fatigue mechanics |

---

## Glossary

| Term | Definition |
|------|------------|
| **Balance Point** | Center of mass; where sword balances horizontally |
| **Center of Percussion (CoP)** | Impact point that produces no reaction force at pivot |
| **Center of Rotation** | Point around which sword rotates during swing |
| **Effective Mass** | Mass "felt" at a specific point on the blade during impact |
| **HEMA** | Historical European Martial Arts |
| **Impulse** | Force × time; changes momentum |
| **Moment of Inertia (MoI)** | Resistance to rotational acceleration |
| **Momentum** | Mass × velocity |
| **Peak Force** | Maximum instantaneous force during impact |
| **Pivot Point** | Point that doesn't move during impact at corresponding impact point |
| **Radius of Gyration** | √(I/m); characteristic length for rotation |
| **Vibrational Node** | Point where vibrations cancel out |
| **Waggle Test** | Method to find pivot points by oscillating a loosely-held sword |

---

## Last Updated
January 31, 2026

## Source
https://swordstem.com/links-resources/
