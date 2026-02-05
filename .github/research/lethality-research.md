# Lethality Research Document: Physics-Based Weapon Damage in Medieval Combat
## Comprehensive Analysis of Factors Affecting Weapon Lethality

*This document provides research on weapon lethality mechanics to enable realistic physics-based damage systems.*

---

## Table of Contents
1. Introduction: Defining Lethality
2. Physics of Damage Delivery
3. Attack Type Mechanics
4. Target Anatomy and Vulnerability
5. Material Science of Lethality
6. Weapon Geometry and Lethality
7. Environmental and Contextual Factors
8. Integration with Existing Systems
9. Mathematical Models for Lethality

---

## 1. Introduction: Defining Lethality

### 1.1 What Is Lethality?

**Lethality** in the context of medieval weapons refers to **the weapon's capacity to cause incapacitating or lethal injury upon successful contact with a target**. Unlike raw "damage" (which often abstractly represents health point reduction), lethality encompasses the complex interaction of:

1. **Energy delivery** - How much kinetic energy reaches the target
2. **Pressure concentration** - Force distributed over contact area
3. **Tissue disruption mechanisms** - Cutting, piercing, crushing
4. **Critical structure damage** - Severing vessels, fracturing bones, organ trauma
5. **Shock and blood loss** - Secondary effects leading to incapacitation

### 1.2 Lethality vs Damage vs Injury

These terms represent different concepts that are often conflated:

| Concept | Definition | Example |
|---------|------------|---------|
| **Damage** | Abstract reduction in combat effectiveness | -10 HP |
| **Injury** | Physical harm to biological tissue | Lacerated muscle |
| **Lethality** | Probability/severity of life-threatening trauma | Severed artery → exsanguination |

**Key Insight**: A highly lethal weapon may cause immediate incapacitation through a single critical strike, while a low-lethality weapon might require multiple hits to achieve the same combat result.

### 1.3 Historical Context

Medieval combat treatises and forensic analysis of battlefield remains reveal:
- **One-hit kills were rare** - Even "lethal" wounds often took minutes to hours to incapacitate
- **Shock and blood loss** were primary incapacitation mechanisms
- **Armor effectiveness** dramatically altered lethality profiles
- **Skill and technique** multiplied inherent weapon lethality

---

## 2. Physics of Damage Delivery

### 2.1 Energy Transfer Fundamentals

The lethality of a weapon strike depends fundamentally on energy delivery to the target.

**Kinetic Energy for Linear Motion**:
```
KE = ½mv²

Where:
  m = mass (kg)
  v = velocity (m/s)
```

**Kinetic Energy for Rotational Motion** (swords, axes):
```
KE_rot = ½Iω²

Where:
  I = moment of inertia (kg·m²)
  ω = angular velocity (rad/s)
```

**Critical Insight from SwordSTEM**: 
> "Doubling the mass doubles kinetic energy, but doubling speed **quadruples** kinetic energy."

This explains why **tip velocity is the dominant factor** in weapon lethality, not raw weapon weight.

### 2.2 Tip Velocity and Angular Momentum

From the attached research (attackspeed-research.md and swordstem-research.md):

**Measured Sword Tip Velocities**:
- Longsword (trained HEMA fighter): **17-20 m/s** (38-45 mph)
- Iaido cutting: **~17 m/s**
- Optimal cutting speed range: **15-22 m/s**

**Angular Momentum**:
```
L = Iω = I × (v_tip / r)

Where:
  L = angular momentum (kg·m²/s)
  I = moment of inertia
  r = distance from rotation center to tip
```

**Lethality Scaling with Angular Momentum**:

Typical values based on weapon physics:
- Dagger (L ≈ 0.05 kg·m²/s): lethality ≈ 5
- Longsword (L ≈ 0.3 kg·m²/s): lethality ≈ 20
- Greatsword (L ≈ 0.8 kg·m²/s): lethality ≈ 30
- Polearm (L ≈ 2.0 kg·m²/s): lethality ≈ 40

**Why Logarithmic Scaling?**
- Represents diminishing returns as weapons get heavier/longer
- Matches empirical observation that heavier weapons don't scale linearly with effectiveness
- Accounts for the fact that excessively heavy weapons are harder to control and achieve optimal impact

### 2.3 Force vs Pressure vs Stress

**Force** is the total push/pull on an object:
```
F = ma (Newtons)
```

**Pressure** is force distributed over area:
```
P = F / A (Pascals = N/m²)
```

**Stress** is the internal force per unit area within a material resisting deformation.

**Critical Insight from Ballistic Trauma Research**:
> "Stress = Force / Area. A blunt sword edge already has a much smaller area than a fist. This means that in addition to producing a higher peak force we also get an even higher peak stress, due to the even smaller contact area of the blunt sword edge. And if you reduce the sword's surface area even more by, say, making it sharp, the stress goes through the roof."

**Lethality Implication**: 
- Sharp weapons concentrate stress to exceed tissue shear strength
- Blunt weapons distribute force, causing crushing trauma
- **The same kinetic energy can produce vastly different injuries** depending on contact area

### 2.4 Penetration Depth Physics

From ballistic and penetrating trauma research:

**Penetration depends on**:
1. **Peak stress** at impact point
2. **Tissue shear strength** (resistance to separation)
3. **Deceleration distance** (how quickly weapon slows)

**Two Phases of Penetration** (from swordstem-research.md):

**Phase 1: Surface Penetration**
- Blade pushes surface inward
- Force increases continuously
- Must exceed **shear stress** of material to proceed
- Sharp vs dull blade has minimal difference at this stage

**Phase 2: Steady State (Internal Cutting)**
- Once penetrated, blade acts as wedge
- Force levels off to constant value
- Blade sharpness matters more in Phase 1

**Force Required**:
```
F_penetration ≥ σ_shear × A_contact

Where:
  σ_shear = material shear strength (Pa)
  A_contact = contact area (m²)
```

For human soft tissue:
- Skin shear strength: ~2-20 MPa (depends on hydration, location)
- Muscle: ~0.3-3 MPa
- Fat: ~0.1-0.5 MPa

**Practical Example**:
- Sword tip (1mm² contact): 4N exceeds skin shear stress
- Arrow point (0.5mm²): 2N exceeds skin shear stress
- Needle (0.01mm²): 0.04N exceeds skin shear stress

This explains the myth-busting claim that "only 4 ounces of force" can penetrate skin - it's about **stress concentration**, not raw force.

### 2.5 Temporary vs Permanent Cavitation

From ballistic trauma research:

**Permanent Cavity**:
- The actual hole left by the weapon
- Size approximates weapon cross-section (for low-velocity)
- Direct tissue destruction

**Temporary Cavity**:
- Radial expansion of tissue from pressure wave
- Only significant at high velocities (>300 m/s)
- **Not relevant for medieval weapons** (sword tip: 20 m/s, even thrown spears: <30 m/s)

**Key Finding**: Medieval weapons cause damage almost entirely through **permanent cavitation** (direct cutting/crushing), not temporary cavity formation.

### 2.6 Impact Duration and Peak Force

From thrust mechanics research (swordstem-research.md):

**Impulse-Momentum Relationship**:
```
J = FΔt = Δp

Where:
  J = impulse (N·s)
  F = average force (N)
  Δt = impact duration (s)
  Δp = momentum change (kg·m/s)
```

**Peak Force depends on impact duration**:
```
F_peak = Δp / Δt

Shorter impact time → Higher peak force
```

**Armor's Effect on Lethality**:
- Padding extends impact duration (Δt ↑)
- Same momentum change over longer time
- Peak force reduced (F_peak ↓)
- Lower peak force → lower tissue damage

**Example**:
- Sword strike: Δp = 3 kg·m/s
- Unarmored (Δt = 0.002s): F_peak = 1500 N
- Padded gambeson (Δt = 0.010s): F_peak = 300 N
- **5x force reduction from armor!**

---

## 3. Attack Type Mechanics

### 3.1 The Three Primary Attack Types

Medieval weapons deliver damage through three fundamental mechanisms:

| Attack Type | Mechanism | Primary Factors | Typical Weapons |
|-------------|-----------|----------------|-----------------|
| **Slash** | Material separation via wedge action | Edge geometry, tip velocity, blade weight | Swords, axes, falchions |
| **Thrust** | Concentrated pressure penetration | Point geometry, linear velocity, rigidity | Spears, rapiers, estocs |
| **Strike** | Blunt force trauma | Momentum, impact area, structural coupling | Maces, hammers, polearms |

### 3.2 Slash Lethality

**Cutting Mechanics**:

Slashing cuts work through **wedge action** - the blade separates tissue by forcing it apart as it moves through.

**Key Factors** (from swordstem-research.md):

1. **Angular Momentum** (L = Iω):
   - Primary determinant of cutting power
   - Higher L → deeper cuts
   - Logarithmic scaling with lethality

2. **Edge Geometry**:
   - **Bevel angle**: Acuter angle = less resistance but requires more momentum
   - **Bevel width/height**: Larger wedge = more tissue disruption
   - **Edge sharpness**: Reduces surface penetration force

3. **Blade Weight Distribution**:
   - Mass toward tip increases impact energy
   - But increases moment of inertia (slower swing)
   - Optimal balance point varies by weapon type

4. **Slicing Motion**:
   - Lateral movement lowers penetration threshold
   - Allows cuts at force levels that wouldn't otherwise penetrate
   - All real sword cuts have some lateral component

**Wedge Physics**:

The wedge effect in cutting can be understood through:
- Friction factor from bevel angle (steeper angle = less friction)
- Resistance from wedge action increases with bevel size
- Surplus momentum after overcoming resistance determines cut depth
- Wedge bonus requires sufficient momentum to drive through tissue

**Interpretation**:
- A wider wedge (larger bevel) creates more destructive cuts
- But requires more angular momentum to drive through tissue
- If momentum insufficient, wider wedge actually reduces lethality
- This explains why thin, sharp blades can cut effectively despite lower mass

**Material Properties Effect**:

Based on materials research:

- **Hardness**: Maintains sharp edge, resists dulling
- **Blade weight**: Increases momentum and cut depth
- **Flexibility**: Moderate flexibility optimal (sweet spot typically at mid-range)
  - Too rigid: Can't flex to maintain edge alignment
  - Too flexible: Bounces off target instead of cutting

### 3.3 Thrust Lethality

**Penetration Mechanics**:

Thrusts work through **concentrated pressure** - concentrating force over minimal area to exceed tissue yield strength.

**Key Factors**:

1. **Tip Geometry**:
   - **Point angle**: Acuter angle = higher stress concentration
   - **Cross-sectional area**: Smaller = less resistance after initial penetration
   - **Taper**: Gradual taper allows deeper penetration before resistance rises

2. **Linear Velocity**:
   - Unlike slashing, thrusts benefit from pure linear speed
   - No need for rotational motion
   - Momentum from body weight critical (see below)

3. **Rigidity**:
   - Flexible blades absorb energy, reducing penetration
   - Rigid construction transmits force to tip
   - But excessive rigidity makes weapon fragile

4. **Body Structure**:
   - Locked joints at impact = maximum momentum transfer
   - "Throwing your weight" behind thrust literally multiplies force

**Thrust Momentum Transfer**:

From SwordSTEM research on thrusts:
```
Effective mass in thrust can be:
- Sword alone: m = 1-2 kg
- Sword + arms: m = 5-7 kg  
- Sword + arms + body: m = 70+ kg

Momentum = m × v
```

**Critical Insight**: "When a thrust lands it can have the momentum of the sword, the momentum of the sword + arms, or the momentum of the sword + arms + body."

A thrust with body commitment can deliver **35x more momentum** than a sword-only thrust!

**Blade Profile and Taper**:

The shoulder angle (transition from tip to blade) determines how smoothly the blade widens:
- 180° (flat shoulder) is optimal for maintaining penetration momentum
- Acute shoulders (<180°) create snag points
- Obtuse shoulders (>180°) catch on wound edges

**Material Property Effects**:
- **Hardness**: Tip resists deformation on bone contact
- **Flexibility**: Energy lost to blade flex reduces penetration

### 3.4 Strike Lethality

**Blunt Trauma Mechanics**:

Strikes work through **momentum transfer** causing internal damage without necessarily penetrating.

**Injury Mechanisms**:
1. **Bone fractures**: Direct crushing or bending failure
2. **Organ contusion**: Compression and shearing of internal organs
3. **Concussion**: Hydraulic pressure waves through brain
4. **Internal bleeding**: Vessel rupture from tissue compression

**Key Factors**:

1. **Weapon Mass**:
   - Unlike cutting, mass directly translates to damage
   - Heavier = more momentum = more trauma
   - Near-linear relationship (diminishing returns at extreme weights)

2. **Impact Area**:
   - Larger area = force distribution over more tissue
   - But also = more total energy transfer
   - Optimal depends on target (armor vs flesh)

3. **Balance Point**:
   - Further from hand = longer lever = higher tip velocity
   - But also = harder to control
   - Strike damage scales with normalized balance point

**Strike Lethality Formula**:

Based on physics principles:
- Base lethality scales with weapon weight
  - Linear relationship up to ~3kg
  - Logarithmic diminishing returns for heavier weapons
- Balance point multiplier (forward balance increases effectiveness)
- Material properties:
  - Hardness: Prevents head deformation, transmits force (approximately +15% per normalized point)
  - Flexibility: Absorbs energy, reduces force transfer (approximately -15% per normalized point)

**Interpretation**:
- Light mace (1.5kg, balanced forward): ~65 lethality
- War hammer (2.5kg, head-heavy): ~78 lethality
- Maul (5kg, head-heavy): ~88 lethality

**Material Effects**:
- **Hardness** (+15% per point): Prevents head deformation, transmits force
- **Flexibility** (-15% per point): Absorbs energy, reduces force transfer

### 3.5 Attack Type vs Armor Interaction

**Armor Effectiveness by Attack Type**:

| Attack Type | vs No Armor | vs Padded | vs Chain | vs Plate |
|-------------|------------|-----------|----------|----------|
| Slash | 100% | 60% | 40% | 10% |
| Thrust | 100% | 75% | 30% | 15% |
| Strike | 100% | 70% | 85% | 45% |

(Percentages represent relative effectiveness)

**Why Differences Exist**:

**Slash vs Armor**:
- Edge caught by chain links (can't cut steel)
- Dispersed over large area by plate
- Padding extends impact time, reduces peak force

**Thrust vs Armor**:
- Can slide between chain rings
- Can exploit plate gaps (armpits, visors)
- Rigid tip penetrates padding better than slash

**Strike vs Armor**:
- Force transmitted through armor to body
- Chain provides minimal blunt protection
- Plate distributes force but doesn't eliminate it
- Why maces/hammers were anti-armor weapons

---

## 4. Target Anatomy and Vulnerability

### 4.1 Human Anatomy as a Damage System

The human body is not a uniform damage-receiving surface. Lethality varies dramatically based on **anatomical target zone**.

**Zones of Vulnerability**:

| Zone | Critical Structures | Time to Incapacitation | Lethality Multiplier |
|------|---------------------|----------------------|---------------------|
| **Head** | Brain, brainstem, spinal cord | Instant to seconds | 3.0x |
| **Neck** | Carotid arteries, jugular veins, trachea, spine | 5-30 seconds | 2.5x |
| **Chest** | Heart, lungs, major vessels (aorta) | 30 seconds to 5 minutes | 2.0x |
| **Abdomen** | Liver, spleen, intestines, major vessels | 5-30 minutes | 1.5x |
| **Limbs** | Major arteries (femoral, brachial) | 2-10 minutes | 1.0x |

### 4.2 Mechanism of Incapacitation

From ballistic trauma research:

**Primary Causes of Combat Death**:
1. **Exsanguination** (blood loss) - most common
2. **Hypoxia** (oxygen deprivation) - from airway/lung damage
3. **Central nervous system disruption** - brain/spinal trauma

**Blood Loss Thresholds**:
```
Normal blood volume: ~5 liters
Class I hemorrhage (15% loss): Minimal symptoms
Class II (15-30% loss): Increased heart rate, anxiety
Class III (30-40% loss): Shock, confusion
Class IV (>40% loss): Unconsciousness, death without intervention
```

**Critical Insight**: 
- Losing 2 liters (40%) is life-threatening
- Major artery severance can achieve this in **2-5 minutes**
- This explains historical accounts of warriors fighting for minutes after mortal wounds

### 4.3 Tissue Type and Damage Resistance

**Tissue Shear Strength** (resistance to cutting):

| Tissue Type | Shear Strength (MPa) | Slashing Vulnerability | Thrusting Vulnerability |
|-------------|---------------------|----------------------|------------------------|
| Skin | 2-20 | Moderate | High (once penetrated) |
| Fat | 0.1-0.5 | Low | Low |
| Muscle | 0.3-3 | Moderate | Moderate |
| Tendon | 10-50 | High | Low (slides aside) |
| Bone | 50-150 | Very High | Moderate (can be cracked) |
| Cartilage | 5-20 | High | Low |

**Elastic vs Inelastic Tissue**:

From penetrating trauma research:
> "Flexible elastic soft tissues, such as muscle, intestine, skin, and blood vessels, are good energy absorbers and are resistant to tissue stretch. If enough energy is transferred, the liver may disintegrate."

**Organs with Low Tensile Strength** (from gunshot wound research):
- Liver
- Spleen  
- Kidney
- Brain

These organs **split or shatter** from temporary cavitation pressure that muscle/skin would survive. However, at medieval weapon velocities (~20 m/s vs bullets at 300+ m/s), this is less relevant - direct mechanical disruption dominates.

### 4.4 The "Golden Hour" and Damage Over Time

Medieval combat lethality must account for **time to incapacitation** vs **time to death**:

**Instant Incapacitation**:
- Severed spinal cord
- Massive brain trauma
- Heart destruction

**Rapid Incapacitation** (seconds to minutes):
- Major arterial bleeding (carotid, aorta)
- Both lungs punctured
- Severe blunt head trauma

**Delayed Incapacitation** (minutes to hours):
- Single lung puncture
- Liver/spleen laceration
- Intestinal perforation (leads to sepsis if untreated)

**Game Design Implication**: 
A "lethal" wound may not immediately remove a combatant from action. Historical accounts describe warriors fighting effectively for several minutes after receiving wounds that would prove fatal.

---

## 5. Material Science of Lethality

### 5.1 The Hardness-Toughness Tradeoff

From critical-failure-research.md:

**Hardness**: Resistance to plastic deformation
- Maintains sharp edge
- Resists rolling/bending
- Enables armor penetration

**Toughness**: Resistance to fracture propagation
- Absorbs impact energy
- Resists chipping/shattering
- Enables repeated use

**The Fundamental Conflict**:
```
↑ Hardness = Better edge retention BUT ↑ Brittleness
↑ Toughness = Better impact resistance BUT ↓ Edge retention
```

**Optimal Material Properties for Lethality**:

| Weapon Type | Hardness Priority | Toughness Priority | Typical Hardness (HRC) |
|-------------|------------------|-------------------|----------------------|
| Slashing blade | High | Medium | 50-58 |
| Thrusting blade | Very High | Medium-Low | 52-60 |
| Striking head | Medium | High | 45-52 |
| Armor-piercing point | Very High | Low-Medium | 58-62 |

### 5.2 Edge Retention and Lethality Degradation

**Sharpness vs Damage**:

A dull blade requires **significantly more force** to achieve surface penetration (Phase 1 of cutting, see Section 2.4), but once penetrated, internal cutting (Phase 2) is less affected.

**Mohs Hardness Scale** (resistance to scratching):
- Steel: 4-4.5
- Hardened steel: 5-8.5
- Bone: 5
- Tooth enamel: 5

**Key Insight**: Striking bone with a blade causes **mutual wear** - the blade dulls, the bone may chip. Harder blades maintain edge longer but are more prone to chipping.

**Lethality Impact of Dulling**:
```
Sharp blade (100% lethality):
  Surface penetration force: F_min
  
Dull blade (70% lethality):
  Surface penetration force: 3-5 × F_min
  Internal cutting: ~0.9 × F_min
```

The primary lethality loss from dulling is **failure to penetrate**, not reduced cutting once inside tissue.

### 5.3 Flexibility, Rigidity, and Energy Transfer

**Blade Flex Physics**:

When a flexible blade impacts, some kinetic energy is stored as **elastic potential energy** in blade deformation:

```
E_total = E_damage + E_flex + E_vibration

E_flex = ½kx²
Where:
  k = blade stiffness (N/m)
  x = deflection (m)
```

**Lethality Implications**:

**For Slashing**:
- Moderate flexibility (sweet spot): Blade flexes to maintain edge alignment
- Too stiff: Blade bounces off at poor angle
- Too flexible: Excessive energy loss to flex

**For Thrusting**:
- High stiffness required: Minimal energy loss
- Flexible blades "bend around" the target
- Historical solution: estocs, rapiers optimized for rigidity

**For Striking**:
- High stiffness required: Transmit force, don't absorb it
- Flexible striking weapons are ineffective (whips excepted)

**Material Property: Yield Strength**

From critical-failure-research.md:
```
Stress below yield: Elastic (recovers shape)
Stress above yield: Plastic (permanent deformation)
```

A blade that yields under striking force will **roll its edge** or **bend permanently**, dramatically reducing lethality.

### 5.4 Temperature Effects on Lethality

**Ductile-Brittle Transition**:

Steel behavior changes dramatically with temperature:

| Temperature | Behavior | Lethality Impact |
|------------|----------|-----------------|
| Hot (>200°C) | Very ductile | Edge rolls easily, low lethality |
| Room temp | Balanced | Optimal performance |
| Cold (0 to -20°C) | Increasingly brittle | Increased chip/shatter risk |
| Very cold (<-20°C) | Brittle | High catastrophic failure risk |

**Historical Context**: 
Winter campaigns saw increased weapon failures. Steel swords could shatter on hard impacts in extreme cold - a lethality reduction through weapon loss rather than direct performance degradation.

---

## 6. Weapon Geometry and Lethality

### 6.1 Edge Geometry: The Fundamental Trade-off

**Edge Angle vs Cutting Performance**:

| Edge Angle | Sharpness | Durability | Best Use |
|------------|-----------|------------|----------|
| 10-15° | Razor sharp | Poor (rolls/chips) | Surgical scalpels |
| 15-25° | Very sharp | Moderate | Kitchen knives, swords |
| 25-35° | Sharp | Good | Utility knives, axes |
| 35-45° | Durable | Excellent | Machetes, cleavers |
| 45°+ | Blunt | Maximum | Wedges, some axes |

**Optimal Sword Edge Angle**: 20-30° (total included angle)
- Sharp enough for effective cutting
- Durable enough for combat longevity
- Balances lethality with weapon survival

### 6.2 Cross-Sectional Geometry

**Blade Profile Impact on Lethality**:

**Lens/Lenticular** (convex on both sides):
- Excellent cutting geometry
- Thin profile = low resistance
- Moderate strength
- Used: Viking swords, early medieval blades

**Diamond** (flat faces tapering to edge):
- Strong spine for thrusting
- Good cutting
- Heavier than lens
- Used: Longswords, arming swords

**Hollow Ground** (concave faces):
- Extremely thin, sharp edge
- Weak - prone to bending
- Excellent cutting while sharp
- Used: Razors, rarely combat swords

**Flat with Bevels**:
- Maximum strength
- Thicker = more durable
- Higher cutting resistance
- Used: Cleavers, some axes

**Lethality Impact**:
```
Cutting resistance ∝ blade thickness × blade width
Structural strength ∝ thickness² × width

Thinner blade = Higher lethality (less resistance)
But: Lower durability (easier to damage)
```

### 6.3 Distal Taper and Point of Balance

**Distal Taper**: The reduction in blade thickness from hilt to tip.

**Effects on Lethality**:

1. **Mass Distribution**:
   - More taper → more mass near hilt
   - Lowers moment of inertia
   - **Faster** swing speed
   - But **less** impact mass at tip

2. **Blade Flexibility**:
   - Thinner tip flexes more
   - Better for thrusting (penetration geometry)
   - Worse for cutting rigidity

3. **Point of Balance**:
   - Aggressive taper pulls balance toward hilt
   - Easier to maneuver
   - Less momentum in strikes

**Historical Examples**:

| Sword Type | Distal Taper | Balance Point | Combat Role |
|------------|-------------|---------------|-------------|
| Viking sword | Minimal | Far forward | Heavy cutting |
| Longsword | Moderate | Mid-blade | Balanced cutting/thrust |
| Rapier | Extreme | Near hilt | Thrusting specialist |
| Falchion | Reverse (thicker toward tip) | Very forward | Chopping |

**Lethality Interaction**:

Forward balance increases strike lethality (more mass at impact point) but increases moment of inertia (slower swing, potentially lower angular momentum for cuts).

### 6.4 Weapon Length and Leverage

**Leverage Physics**:

For rotational weapons (swords, axes, polearms):
```
Tip velocity = ω × r

Where:
  ω = angular velocity (rad/s)
  r = distance from rotation center to tip (m)
```

**Longer weapon = higher tip velocity for same angular velocity**

But moment of inertia increases with length:
```
I ∝ L³ (for constant cross-section)
```

**The Length-Lethality Tradeoff**:

**Benefits of Length**:
- Higher tip velocity
- Greater reach (tactical, not physical lethality)
- More leverage for cuts

**Costs of Length**:
- Dramatically higher moment of inertia
- Slower acceleration
- Harder to control
- More vulnerable to damage

**Optimal Length by Weapon Type**:

| Weapon | Length | Primary Lethality Factor |
|--------|--------|-------------------------|
| Dagger | 20-40 cm | Thrust pressure |
| Arming sword | 75-90 cm | Balanced slash/thrust |
| Longsword | 100-120 cm | Angular momentum |
| Greatsword | 120-180 cm | Extreme angular momentum |
| Polearm | 180-250 cm | Leverage multiplier |

From swordstem-research.md on polearms:
> "The oak staff is heavier than the steel sword, but not by an enormous margin. Have a slightly thinner staff made out of a lighter material and you can level the playing field. But weight isn't the most important quantity at play here. **Moment of Inertia** is."

A 3m oak staff has **~40x the moment of inertia** of a longsword, producing correspondingly massive angular momentum and lethality for strikes.

### 6.5 Point Geometry for Thrusting

**Tip Angle and Penetration**:

Thrust lethality increases with acuter tip angles (smaller angle = better penetration).

**Optimal Thrust Point Angles**:

| Weapon Type | Tip Angle | Penetration | Strength |
|-------------|-----------|-------------|----------|
| Needle rapier | 5-10° | Excellent | Very poor |
| Estoc | 15-20° | Very good | Poor |
| Longsword | 20-30° | Good | Moderate |
| War spear | 25-35° | Moderate | Good |
| Boar spear | 35-45° | Poor | Excellent |

**Shoulder Geometry**:

The "shoulder" is where the tip widens to the blade/shaft cross-section.

Optimal shoulder angle is approximately 180° (perpendicular to blade axis):

**Why 180° is Optimal**:
- Smooth transition minimizes snag points
- Wound channel widens gradually
- Blade continues penetrating with minimal resistance increase
- Acute shoulders (<180°) create snag that stops penetration
- Obtuse shoulders (>180°) catch on wound edges (worse than acute)

---

## 7. Environmental and Contextual Factors

### 7.1 Target State: Static vs Dynamic

**Impact Velocity and Relative Motion**:

Lethality depends on **relative velocity** between weapon and target:

```
v_relative = v_weapon + v_target
```

**Moving Target Considerations**:

1. **Target moving toward weapon**:
   - Increased relative velocity
   - Higher impact energy
   - Greater penetration depth
   - **Why "running into" a thrust is devastating**

2. **Target moving away**:
   - Decreased relative velocity
   - "Glancing blow" effect
   - Reduced lethality

3. **Perpendicular motion**:
   - Affects impact angle
   - Can convert perpendicular into oblique hit
   - Reduces effective penetration

### 7.2 Impact Angle and Obliquity

**Perpendicular Impact** (90° to surface):
- Maximum force transmission
- Full penetration potential
- Optimal lethality

**Oblique Impact** (angled):
```
F_perpendicular = F_total × cos(θ)

Where θ = angle from perpendicular
```

**Lethality Reduction from Obliquity**:

| Angle | Perpendicular Force | Effective Lethality |
|-------|-------------------|-------------------|
| 0° (perpendicular) | 100% | 100% |
| 30° | 87% | ~75% |
| 45° | 71% | ~50% |
| 60° | 50% | ~25% |
| 75° | 26% | ~5% (glancing) |

**Why Armor is Sloped**:
- Forces oblique impacts
- Dramatically reduces effective lethality
- Same principle as tank armor

### 7.3 Environmental Effects on Weapon Performance

**Moisture and Corrosion**:
- Rust reduces edge sharpness
- Pitting creates stress concentrators
- Lethality degradation over time

**Temperature** (covered in Section 5.4):
- Cold increases brittleness
- Heat increases ductility
- Both reduce optimal lethality

**Blood and Tissue on Blade**:
- Increases friction
- Reduces cutting efficiency
- Why warriors cleaned blades between engagements

### 7.4 Fatigue and Successive Impacts

From attackspeed-research.md:

> "Sustaining a high strike frequency is physiologically taxing. A 2024 study of medieval combat sports measured **a clear degradation in impact velocity as a fight progressed**."

**Fatigue Effects on Lethality**:
1. **Reduced swing velocity**:
   - Lower kinetic energy
   - Reduced penetration
   - Diminished cutting depth

2. **Degraded technique**:
   - Poor edge alignment
   - Suboptimal impact angles
   - Loss of body structure

3. **Accumulated damage**:
   - Blade edge rolls/chips
   - Handle loosens
   - Grip deteriorates

**Practical Implication**: 
Early strikes in combat are most lethal. As combatants tire and weapons degrade, lethality decreases even if technique remains "perfect."

---

## 8. Mathematical Models for Lethality

### 8.1 Unified Lethality Model

Based on the research, we can propose a **unified lethality model**:

```
L_total = L_base × M_geometry × M_material × M_technique × M_target

Where:
  L_base = Base lethality from physics (angular momentum or thrust force)
  M_geometry = Geometric efficiency multiplier
  M_material = Material property multiplier
  M_technique = Skill/execution multiplier
  M_target = Target vulnerability multiplier
```

### 8.2 Attack Type Specific Models

**Slash Lethality**:
```
L_slash = f(I, ω, bevel, material)

Where:
  I = moment of inertia (kg·m²)
  ω = angular velocity (rad/s)
  bevel = {angle, width, height}
  material = {hardness, weight, flexibility}

Expanded:
  L_angular = 16 × ln(1 + 8 × I × ω)
  L_wedge = A × (1 - e^(-B×h)) × (1 - e^(-C×surplus))
  L_material = 1 + 0.08H + 0.14W - 0.10F
  
  L_slash = (L_angular + L_wedge) × L_material
```

**Thrust Lethality**:
```
L_thrust = f(v, area, angle, material)

Where:
  v = linear velocity (m/s)
  area = tip cross-section (m²)
  angle = tip bevel angle (degrees)
  material = {hardness, flexibility}

Expanded:
  pressure = (m × v²) / area
  angle_factor = 1 - (angle / 90)
  material_factor = 1 + 0.10H - 0.06F
  
  L_thrust = k × pressure × angle_factor × material_factor
```

**Strike Lethality**:
```
L_strike = f(m, v, balance, material)

Where:
  m = weapon mass (kg)
  v = impact velocity (m/s)
  balance = normalized balance point [0,1]
  material = {hardness, flexibility}

Expanded:
  momentum = m × v
  balance_factor = 0.5 + 0.75 × balance
  material_factor = 1 + 0.15H - 0.15F
  
  L_strike = weight_to_base(m) × balance_factor × material_factor
```

### 8.3 Armor Interaction Model

```
Damage_final = L_weapon × (1 - A_reduction) × F_lethality(L, T)

Where:
  L_weapon = weapon base lethality
  A_reduction = armor damage reduction [0,1]
  F_lethality = lethality function (operation type dependent)
  L = lethality value
  T = armor toughness
```

**Armor Reduction by Type**:
```
A_reduction = f(armor_type, attack_type)

Examples:
  Slash vs Plate: 0.90 (90% reduction)
  Thrust vs Chain: 0.70 (70% reduction)
  Strike vs Plate: 0.55 (55% reduction)
```

### 8.4 Probabilistic Critical Hit Model

Lethality can be extended to model critical hits (striking vital zones):

```
P_critical = base_crit_chance × (L / T)

Damage_critical = Damage_normal × (1 + crit_multiplier)

Where:
  base_crit_chance = 0.05 (5% baseline)
  L/T ratio scales probability
  crit_multiplier = 2.0-4.0 depending on zone
```

This represents the increased likelihood of hitting vitals when weapon lethality exceeds target's protective attributes.

---

## Conclusion

### 10.1 Enhancements to Current System

The Skada mod's existing lethality system is already sophisticated. Potential refinements:

**1. Positional Damage Multipliers**:

Implement hit location tracking with lethality multipliers:
```java
enum HitZone {
    HEAD(3.0),      // Brain, instant incapacitation risk
    NECK(2.5),      // Major vessels
    CHEST(2.0),     // Heart, lungs
    ABDOMEN(1.5),   // Liver, intestines
    LIMBS(1.0);     // Baseline
    
    public final double lethalityMultiplier;
}

// In damage calculation
double effectiveLethality = baseLethality * hitZone.lethalityMultiplier;
```

**2. Weapon Degradation System**:

Track edge condition and apply lethality penalties:
```java
class WeaponCondition {
    double sharpness;  // 1.0 = perfect, 0.0 = destroyed
    double integrity;  // 1.0 = perfect, 0.0 = shattered
    
    double getLethalityMultiplier() {
        // Sharpness primarily affects surface penetration
        double sharpnessFactor = 0.7 + 0.3 * sharpness;
        // Integrity affects overall structural soundness
        double integrityFactor = 0.5 + 0.5 * integrity;
        return sharpnessFactor * integrityFactor;
    }
}
```

**3. Velocity-Based Lethality Scaling**:

Currently Skada uses fixed angular velocity. Could implement dynamic velocity based on attack type:

```java
// Power attack: slower windup, higher velocity
double powerAttackMultiplier = 1.3;
double angularVelocity = baseOmega * powerAttackMultiplier;

// Quick attack: faster execution, lower velocity  
double quickAttackMultiplier = 0.8;
double angularVelocity = baseOmega * quickAttackMultiplier;
```

**4. Cumulative Injury System**:

Track damage over time, with penalties for successive hits:
```java
class EntityInjuryState {
    double bloodLoss;  // 0.0 to 1.0 (40% = critical)
    List<InjuryInstance> wounds;
    
    double getDefenseMultiplier() {
        // Blood loss reduces combat effectiveness
        return 1.0 - (0.5 * bloodLoss);
    }
    
    void applyBleedingDamage(double damageThisTick) {
        bloodLoss += damageThisTick / maxHealth * 0.01;
    }
}
```

### 10.2 Configuration and Balance

**Lethality Scaling Factor**:

Add global config to tune lethality impact:
```java
// CommonConfig.java
public static final ForgeConfigSpec.DoubleValue LETHALITY_SCALE_FACTOR;

// DamageHandler.java
double scaledLethality = lethality * CommonConfig.LETHALITY_SCALE_FACTOR.get();
```

Allows server admins to adjust difficulty without rebalancing every weapon.

**Material Property Soft Caps**:

Current system uses soft cap of 10 for material properties. Could make configurable:
```java
public static final ForgeConfigSpec.IntValue MATERIAL_SOFTCAP;

double normalizedHardness = hardness / MATERIAL_SOFTCAP.get();
```

**Attack Type Resistance Scaling**:

Currently attack type resistances are attributes. Could add configurable base values:
```java
public static final ForgeConfigSpec.DoubleValue SLASH_RESIST_EFFECTIVENESS;

double damageReduction = slashResist * SLASH_RESIST_EFFECTIVENESS.get();
```

### 10.3 Visual Feedback for Lethality

**Damage Number Colors**:

Color-code damage numbers by lethality ratio:
```java
if (lethality / toughness > 3.0) {
    return ChatFormatting.DARK_RED;  // Overwhelming lethality
} else if (lethality / toughness > 1.5) {
    return ChatFormatting.RED;  // High lethality
} else if (lethality / toughness > 0.75) {
    return ChatFormatting.YELLOW;  // Moderate lethality
} else {
    return ChatFormatting.GRAY;  // Low lethality
}
```

**Hit Effect Particles**:

Spawn different particles based on attack type and lethality:
- **High lethality slash**: Blood spray particles
- **High lethality thrust**: Single blood gush direction
- **High lethality strike**: Impact shockwave effect
- **Low lethality**: Reduced or no effects

**Sound Effects**:

Play distinct sounds for different lethality levels:
- **Critical lethality** (L > 3T): Bone crunch + scream
- **High lethality** (L > T): Flesh impact + grunt
- **Moderate lethality** (L ≈ T): Standard hit sound
- **Low lethality** (L < 0.5T): Deflection clang

### 10.4 Advanced: Biomechanical Damage Model

For maximum realism, implement tissue-specific damage:

```java
enum TissueType {
    SKIN(2000000, 0.02),     // Shear strength (Pa), thickness (m)
    FAT(200000, 0.01),
    MUSCLE(1000000, 0.05),
    BONE(100000000, 0.02);
    
    public final double shearStrength;
    public final double thickness;
}

class PenetrationCalculator {
    double calculateDepth(double force, double area, TissueType[] layers) {
        double depth = 0.0;
        double remainingForce = force;
        
        for (TissueType layer : layers) {
            double stress = remainingForce / area;
            if (stress < layer.shearStrength) {
                break;  // Stopped by this layer
            }
            depth += layer.thickness;
            remainingForce -= layer.shearStrength * area * layer.thickness;
        }
        
        return depth;
    }
}
```

This would allow calculating penetration depth through skin → fat → muscle → bone, with lethality determined by which structures were breached.

### 10.5 Machine Learning Calibration (Advanced)

For perfect balance, train neural network on combat data:

```
Input Features:
- Weapon lethality
- Target toughness  
- Armor value
- Attack type
- Hit location
- Player skill level

Output:
- Expected damage
- Combat effectiveness rating

Training Data:
- Player combat logs
- Simulated battles
- Historical combat accounts (if digitized)

Goal:
- Predict "feels right" damage values
- Auto-tune lethality formulas
- Adapt to player behavior
```

---

## Conclusion

Weapon lethality in medieval combat is the product of complex, interacting physical systems:

1. **Energy Delivery**: Kinetic energy, angular momentum, and force transmission
2. **Weapon Geometry**: Edge angles, cross-sections, tapers, and leverage
3. **Material Properties**: Hardness, toughness, flexibility, and their trade-offs
4. **Attack Mechanics**: Slash wedge action, thrust pressure concentration, strike momentum transfer
5. **Target Anatomy**: Tissue vulnerability, vital structures, and damage mechanisms
6. **Environmental Factors**: Armor, temperature, fatigue, and successive impacts

This research document provides the scientific foundation for understanding weapon lethality in combat systems.

**Key Takeaways**:

- **Lethality is not linear** - Use logarithmic/hyperbolic scaling functions
- **Attack types behave fundamentally differently** - Require separate damage models
- **Material properties create tradeoffs** - No universally "best" configuration
- **Context matters** - Armor, hit location, and target state dramatically alter lethality
- **Physics is the foundation** - Angular momentum, stress concentration, and energy transfer determine real-world weapon effectiveness

By grounding combat mechanics in real physics, systems can create combat where weapon choice, technique, and tactical decisions mirror historical martial reality - making for compelling, intuitive, and strategically deep gameplay.

---

## References

1. **SwordSTEM.com** - Sean Franklin's evidence-based HEMA analysis
   - Rotational vs Translational Motion
   - Moment of Inertia and Center of Percussion
   - Impact, Impulse, and Momentum
   - Two Phases of Cutting
   - Force and Thrust Mechanics

2. **Attack Speed Research** - Historical and biomechanical analysis
   - Measured sword velocities from motion capture
   - Fatigue effects on combat performance
   - Strike frequency vs swing velocity tradeoffs

3. **Critical Failure Research** - Material science and weapon durability
   - Hardness-toughness tradeoff
   - Fracture mechanics
   - Heat treatment effects
   - Ductile vs brittle failure modes

4. **Penetrating Trauma Research** (Medical Literature)
   - Wound ballistics and cavity formation
   - Tissue damage mechanisms
   - Critical injury thresholds
   - Time to incapacitation data

---

## Appendix: Quick Reference Tables

### Typical Lethality Values

| Weapon | Angular Momentum (kg·m²/s) | Base Lethality | Notes |
|--------|---------------------------|----------------|-------|
| Dagger | 0.03-0.08 | 3-7 | Low leverage but fast |
| Arming Sword | 0.15-0.25 | 15-20 | Balanced cutting weapon |
| Longsword | 0.25-0.35 | 20-25 | Optimal two-hand leverage |
| Greatsword | 0.50-0.90 | 28-33 | Extreme angular momentum |
| War Axe | 0.30-0.50 | 23-28 | Head-heavy for strikes |
| Poleaxe | 0.80-1.50 | 32-38 | Leverage multiplier |
| Maul | 1.00-2.00 | 35-40 | Maximum strike force |

### Material Property Impact

| Property | Slash Effect | Thrust Effect | Strike Effect |
|----------|-------------|---------------|---------------|
| Hardness | +8% per point | +10% per point | +15% per point |
| Weight | +14% per point | N/A | Linear base increase |
| Flexibility | -10% per point (sweet spot at 5) | -6% per point | -15% per point |

### Armor Effectiveness vs Attack Types

| Armor | vs Slash | vs Thrust | vs Strike |
|-------|----------|-----------|-----------|
| None | 0% reduction | 0% reduction | 0% reduction |
| Padded (gambeson) | 30-40% | 20-25% | 25-30% |
| Leather | 20-30% | 15-20% | 10-15% |
| Chain mail | 50-70% | 65-75% | 10-20% |
| Plate armor | 85-95% | 80-90% | 45-60% |

### Hit Zone Multipliers

| Zone | Lethality Multiplier | Critical Structures |
|------|-------------------|-------------------|
| Head | 3.0x | Brain, cranial nerves |
| Neck | 2.5x | Carotids, jugulars, trachea, spine |
| Chest | 2.0x | Heart, lungs, aorta |
| Abdomen | 1.5x | Liver, spleen, intestines |
| Limbs | 1.0x | Femoral/brachial arteries |

---

*Document Version: 1.0*
*Last Updated: February 1, 2026*
*For: Skada Minecraft Mod (1.20.1 Forge)*
