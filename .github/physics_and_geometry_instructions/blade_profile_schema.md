# Blade Profile (V1) — Schema + Runtime Guide

This document defines the **blade-only** JSON schema for a parametric loft model. It is designed for:

- High model accuracy
- Easy JSON authoring
- Runtime derivation of **volume** and **point of balance**
- Future expansion (fullers, flamboyance, curvature, etc.)

> **Note:** Blade JSON files **must not** include density. Density is provided at runtime.

---

## 1) Geometry Model

A blade is defined by:

1. **Spine curve** — the blade’s centerline in 3D (polyline)
2. **Stations** — cross-sections at normalized positions $s \in [0,1]$ along the spine
3. **Optional features** — fullers and flamboyance
4. **Point Taper** — double $p \in [0,1]$ defining the level of distal and profile taper from the last station to the end of the blade. $p = 1$ means the blade converges in a straight line to the tip, whereas $p = 0$ means the blad

### Coordinate System
Uses the project’s standard coordinates:

- $x$ = length (along blade)
- $y$ = height
- $z$ = thickness

---

## 2) Cross‑Section Definition (Modified Superellipse)

Each station cross‑section uses your **modified superellipse**:

$$|x/a|^r + |y/b| = 1$$

Where:

- $a = \tfrac{\text{width}}{2}$
- $b = \tfrac{\text{thickness}}{2}$
- $r$ is the **curve factor** (primary bevel curve)

### Single‑Edged Blades

Single‑edged blades (katana, scimitar) are supported by **offsetting the section** toward the spine.

Add `edgeOffset` to `section`, in the range $[-1,1]$:

$$y_{offset} = \text{edgeOffset} \cdot \frac{\text{thickness}}{2}$$

The outline becomes:

$$y = b \cdot \left(1 - |x/a|^r\right) + y_{offset}$$

Positive `edgeOffset` shifts mass toward the spine; negative shifts toward the edge. This changes the **visual cross‑section** but keeps **area** unchanged (volume integration is unaffected).

### Area (for volume integration)

$$A = 4ab \cdot \frac{r}{r+1}$$

> This matches the definition in [geometry.md](geometry.md).

### 2D cross‑section outline (top half)

$$y = b \cdot \left(1 - |x/a|^r\right), \quad x \in [-a, a]$$

Mirror for the bottom half.

---

## 3) JSON Schema Overview

- **version**: integer (currently 1)
- **units**: "cm" only
- **spine**: polyline points (list of [x,y,z])
- **stations**: array of section stations
- **fullers**: optional fuller definitions
- **flamboyance**: optional width modulation
- **pointTaper**: optional taper factor for the blade tip

See the formal schema in [blade_profile.schema.json](blade_profile.schema.json).

---

## 4) Example JSON

```json
{
  "version": 1,
  "units": "cm",
  "spine": {
    "type": "polyline",
    "points": [[0,0,0],[35,0,0],[70,1.5,0],[100,4,0]]
  },
  "stations": [
    { "s": 0.0, "width": 4.0, "thickness": 0.7, "section": { "r": 2.2, "edgeOffset": 0.2 } },
    { "s": 0.4, "width": 3.6, "thickness": 0.6, "section": { "r": 2.4, "edgeOffset": 0.2 } },
    { "s": 0.8, "width": 2.4, "thickness": 0.45, "section": { "r": 2.6, "edgeOffset": 0.15 } }
  ],
  "point": { "taper": 0.9 },
  "modifiers": {
    "fullers": [
      { "sRange": [0.15, 0.7], "width": 0.7, "depth": 0.08, "count": 1, "profile": "u" }
    ],
    "flamboyance": { "amplitude": 0.2, "frequency": 5, "phase": 0 }
  }
}
```

---

## 5) Runtime Computation (Volume & Balance)

Sample $N$ slices along the spine. For each sample $i$:

1. Interpolate station width/thickness/$r$ at $s_i$
2. Apply modifiers (fuller area subtraction, flamboyance width offset)
3. If `pointTaper` is defined, taper width/thickness from the **last station** to the blade end using a slight **convex** easing for $\text{taper} < 1$
4. Compute cross‑section area $A_i$
5. Accumulate volume and center of mass:

$$V \approx \sum_i A_i\,\Delta l_i$$

$$\vec{c} = \frac{\sum_i \vec{p}_i \cdot A_i \cdot \Delta l_i}{\sum_i A_i\,\Delta l_i}$$

Density is supplied at runtime:

$$m = \rho \cdot V$$

---

## 6) Reference Implementation

A reference implementation is provided here:

- [BladeProfile.java](../../src/main/java/com/cwjn/skada/data/gen/weapon/new_system/blade/BladeProfile.java)

It includes:

- Polyline spine interpolation
- Station interpolation
- Modified superellipse area
- Fuller subtraction (approx)
- Flamboyance width offset
- Cross‑section outline sampling
