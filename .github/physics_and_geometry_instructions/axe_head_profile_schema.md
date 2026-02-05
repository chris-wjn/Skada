# Axe Head Profile (V1) — Schema + Runtime Guide

This document defines the **axe-head** JSON schema used by the mod’s physics model. The viewer consumes the same schema only to visualize the model. It is designed for:
- High model accuracy (diagram-aligned)
- Accurate viewer display
- Runtime derivation of **volume**, **center of mass**, and **inertia**
- Easy JSON authoring

> **Note:** Axe head JSON files **must not** include density. Density is supplied at runtime.

---

## 1) Geometry Model (Viewer‑Aligned)

The axe head is decomposed into **four volumes**:

1. **Eye / haft block** — a rectangular prism
2. **Edge core lobe** — a bevelled prism with constant height
3. **Top lobe** — a bevelled prism with a linear height profile in $x$
4. **Bottom lobe** — a bevelled prism with a linear height profile in $x$

The three lobes (core/top/bottom) form the **edge assembly** and each has its own **cross‑section in the $x$–$z$ plane**. The top and bottom lobes also taper in $z$ (thickness) using the same curve exponent.

### Coordinate System
- $x$ = length (from eye toward edge) (left/right)
- $y$ = height (up/down)
- $z$ = thickness (backwards/forwards)

Right‑handed orientation is assumed. The **edge points toward +$x$**.

### Origin Convention
Set the origin at the **center of the eye block**:
- $x=0$ at eye center
- $y=0$ at vertical midline of the head
- $z=0$ at thickness midline

This makes mounting and assembly predictable. All positions below are **relative to this origin**.

---

## 2) Cross‑Section Model (for core/top/bottom lobes)

Each lobe uses the **same modified superellipse** as the blade cross‑section, but **one‑sided** in the $x$–$z$ plane. The profile is the **positive‑$x$ half** of the superellipse, with a flat back cap at $x=0$.

### Parameters
- `length` — cross‑section length scale along $x$ (one‑sided)
- `thickness` — full thickness at the back (at $x=0$)
- `curve` — exponent controlling the bevel curvature (smaller = more concave/sharper taper, larger = flatter/boxier)

### One‑Sided Modified Superellipse
Use the blade formula in the $x$–$z$ plane:

$$|x/a|^r + |z/b| = 1$$

Where:
- $a = \text{length}$
- $b = \tfrac{\text{thickness}}{2}$
- $r = \text{curve}$

**One‑sided rule:** only $x \in [0, a]$ is used. The back is capped by the plane at $x=0$.

### 2D outline (top half)

$$z(x) = b \cdot \left(1 - \left(\frac{x}{a}\right)^r\right), \quad x \in [0,a]$$

Mirror for the bottom half ($z \to -z$). The **tip** is at $x=a$.

### Cross‑Section Sampling Rule (Viewer)
The viewer maps the edge‑local coordinate $x_{local} \in [0, L]$ to the cross‑section coordinate:

$$x = a \cdot \frac{x_{local}}{L}$$

Then computes half‑thickness:

$$z_{half}(x_{local}) = b \cdot \left(1 - \left(\frac{x}{a}\right)^r\right)$$

If $a \ne L$, the bevel curve is stretched ($a>L$) or compressed ($a<L$) along the edge length.

---

## 3) Side Profiles (in $x$–$y$ plane)

Let:
- $x_0 = \tfrac{\text{eye.length}}{2}$ (edge base plane)
- $L = \text{edge.length}$
- $x \in [x_0, x_0 + L]$

### Core (constant height)
$$h_{core}(x) = H_{core}$$

### Top/Bottom (linear with optional tip)
Each lobe may define an absolute **tip position** `tipX`. If omitted, `tipX = x_0` (a single ramp down to the edge). Heights are defined as a piecewise linear “tent”:

Let $x_t = \text{tipX}$ and $H = \text{lobe.height}$.

**Case A — tip at/behind base:** $x_t \le x_0$

$$h(x) = H \cdot \left(1 - \frac{x - x_0}{L}\right)$$

**Case B — tip beyond edge:** $x_t \ge x_0 + L$

$$h(x) = H \cdot \frac{x - x_0}{L}$$

**Case C — tip within the edge:** $x_0 < x_t < x_0 + L$

$$
h(x) =
\begin{cases}
H \cdot \frac{x - x_0}{x_t - x_0} & x \le x_t \\
H \cdot \frac{x_0 + L - x}{x_0 + L - x_t} & x \ge x_t
\end{cases}
$$

For the **top lobe**, the volume spans $y \in [\tfrac{H_{core}}{2}, \tfrac{H_{core}}{2} + h(x)]$.

For the **bottom lobe**, the volume spans $y \in [-\tfrac{H_{core}}{2} - h(x), -\tfrac{H_{core}}{2}]$.

### Thickness‑wise taper (top/bottom only)
For top/bottom lobes, height is also reduced toward the outer faces in $z$ using the same curve exponent:

$$h_z(x, z) = h(x) \cdot \left(1 - \left(\frac{|z|}{z_{half}(x)}\right)^r\right)$$

This produces a smooth bevel toward the thickness edges in the viewer.

---

## 4) JSON Schema Overview (V1)

### Required fields
- **version**: integer (currently 1)
- **units**: must be `"cm"`
- **eye**: dimensions of the eye/haft block (plus optional bore)
- **edge**: edge assembly containing the three lobes

### Schema Layout (conceptual)

```json
{
  "version": 1,
  "units": "cm",
  "eye": {
    "length": 4.0,
    "height": 4.5,
    "thickness": 2.5,
    "bore": {
      "width": 2.0,
      "thickness": 1.8,
      "shape": "rect"
    }
  },
  "edge": {
    "length": 6.0,
    "core": {
      "height": 3.0,
      "crossSection": {
        "length": 6.0,
        "thickness": 1.6,
        "curve": 2.4
      }
    },
    "top": {
      "height": 1.4,
      "tipX": 5.0,
      "crossSection": {
        "length": 6.0,
        "thickness": 1.0,
        "curve": 2.2
      }
    },
    "bottom": {
      "height": 1.1,
      "tipX": 5.0,
      "crossSection": {
        "length": 6.0,
        "thickness": 1.0,
        "curve": 2.2
      }
    }
  }
}
```

### Field Definitions

#### `eye`
- `length` — size along $x$
- `height` — size along $y$
- `thickness` — size along $z$
- `bore` — optional eye hole specification

The eye block is centered at the origin. Its front face is at $x = \tfrac{\text{length}}{2}$.

##### `eye.bore`
- `width` — hole width along $x$
- `thickness` — hole thickness along $z$
- `shape` — `"rect"` (default) or `"circle"`

The bore is centered in the eye block and extruded along $y$ for the full eye height. For circular bores, the diameter is clamped to the minimum of `width` and `thickness`.

#### `edge`
- `length` — edge assembly length along $x$

The edge assembly **always** starts at the eye front face:

$$x_0 = \tfrac{\text{eye.length}}{2}$$

So it occupies $x \in [x_0, x_0 + \text{edge.length}]$.

#### `edge.core`
- `height` — full height of the core lobe (centered on $y=0$)
- `crossSection` — parameters for the core lobe’s $x$–$z$ profile

The core extends from $y = -\tfrac{height}{2}$ to $y = +\tfrac{height}{2}$.

#### `edge.top`
- `height` — maximum height above the core top at the base plane
- `tipX` — absolute $x$ position for the top apex (optional)
- `crossSection` — parameters for the top lobe’s $x$–$z$ profile

The top lobe occupies:
- $y \in [\tfrac{core.height}{2}, \tfrac{core.height}{2} + h(x)]$
- Height varies per the **tip‑controlled linear profile** in Section 3

#### `edge.bottom`
- `height` — maximum height below the core bottom at the base plane
- `tipX` — absolute $x$ position for the bottom apex (optional)
- `crossSection` — parameters for the bottom lobe’s $x$–$z$ profile

The bottom lobe occupies:
- $y \in [-\tfrac{core.height}{2} - h(x), -\tfrac{core.height}{2}]$
- Height varies per the **tip‑controlled linear profile** in Section 3

---

## 5) Runtime Computation (Volume, COM, Inertia)

### 5.1 Sampling Strategy
Compute each component’s contribution separately, then sum:

1. **Eye block** — exact box formulas, subtracting the bore (rect or circle)
2. **Edge lobes** — numeric integration over $x$ using the height profile and cross‑section thickness

For each lobe, sample $N$ slices along $x_{local} \in [0, L]$:

1. Compute lobe height $h(x)$ from the tip‑controlled linear profile
2. Compute half‑thickness $z_{half}(x)$ from the cross‑section profile
3. Slice area in $y$–$z$ at this $x$:

$$A(x) = h(x) \cdot 2z_{half}(x)$$

4. Accumulate volume and center of mass using the slice centroid at $\big(x, y_{mid}, 0\big)$:

$$V \approx \sum_i A(x_i)\,\Delta x$$

For the top/bottom lobes, the **$y$ centroid** is offset from the core midline:

- Top: $y_{mid} = \tfrac{core.height}{2} + \tfrac{h(x)}{2}$
- Bottom: $y_{mid} = -\tfrac{core.height}{2} - \tfrac{h(x)}{2}$

Combine all component volumes to compute total center of mass:

$$\vec{c} = \frac{\sum_i \vec{p}_i V_i}{\sum_i V_i}$$

### 5.2 Mass and Inertia
Density $\rho$ is supplied at runtime:

$$m = \rho \cdot V$$

Use standard numerical inertia integration by summing per‑slice rectangular prism inertia about the local centroid, then apply the parallel‑axis theorem to the global COM. The eye block uses exact box inertia, with the bore subtracted.

---

## 6) Viewer Notes

- Render the eye block as a box, subtracting the optional bore.
- Render the three lobes as **lofted meshes** using the side profile in $x$–$y$, the one‑sided cross‑section in $x$–$z$, and the $z$‑taper for top/bottom lobes.
- Keep axes aligned: the edge points toward $+x$ and the head is symmetric about $z=0$.

---

## 7) Validation Rules (V1)

- `units` must be `"cm"`
- All lengths must be **positive**
- `edge.length` must be strictly positive
- `crossSection.curve` must be $> 0$
- `eye.bore` (if present) must fit inside the eye block
- All cross‑sections must be symmetric about $z=0$

---

## 8) Summary

This schema mirrors the viewer’s geometry model:
- Eye block (with optional bore)
- Core lobe with constant height
- Top/bottom lobes with tip‑controlled linear height profiles
- One‑sided superellipse cross‑sections with optional length scaling

Each lobe’s **side profile** and **cross‑section** are defined explicitly so the viewer can display the axe head accurately and the physics system can derive mass properties consistently.