# Coordinate Transform System (V1) — Schema + Runtime Guide

This document defines the **axis‑aligned transform schema** used by the mod’s weapon assembly system. The viewer consumes the same schema only to visualize the model. It is designed for:
- Predictable part orientation without arbitrary matrices
- Consistent inertia and center‑of‑mass mapping
- Easy JSON authoring

---

## 1) Coordinate System

The viewer uses the standard right‑handed coordinates:
- $x$ = length (forward)
- $y$ = height (up)
- $z$ = thickness (out of the page)

Transforms are **axis‑aligned permutations** with optional sign flips. No scaling or shear is supported.

---

## 2) Transform Schema (Axis‑Aligned)

A transform remaps each **weapon axis** (`x`, `y`, `z`) to a **local axis** with a sign:

```json
{
  "type": "axisAligned",
  "x": { "axis": "x", "sign": 1 },
  "y": { "axis": "y", "sign": 1 },
  "z": { "axis": "z", "sign": 1 }
}
```

### Fields
- `type`: must be `"axisAligned"`
- `x`, `y`, `z`: axis maps
  - `axis`: one of `"x" | "y" | "z"`
  - `sign`: `1` or `-1`

### Identity Transform

The identity transform is:

```json
{
  "type": "axisAligned",
  "x": { "axis": "x", "sign": 1 },
  "y": { "axis": "y", "sign": 1 },
  "z": { "axis": "z", "sign": 1 }
}
```

---

## 3) Runtime Mapping

For a local vector $\vec{v} = (v_x, v_y, v_z)$, the transformed vector is:

$$
\vec{v}' = \big( s_x \cdot v_{a_x},\ s_y \cdot v_{a_y},\ s_z \cdot v_{a_z} \big)
$$

Where $a_x$, $a_y$, $a_z$ are the selected axes and $s_x$, $s_y$, $s_z \in \{-1, 1\}$ are signs.

This is equivalent to multiplying by a **signed permutation matrix**.

---

## 4) Constraints & Best Practices

- Each of `x`, `y`, `z` **should map to a unique axis** to keep an orthonormal basis.
- Use a **right‑handed** mapping to avoid mirrored geometry.
- The transform is applied **before** the part’s `position` offset.

---

## 5) Examples

### A) Rotate 90° around $z$ (swap $x$ and $y$)

```json
{
  "type": "axisAligned",
  "x": { "axis": "y", "sign": 1 },
  "y": { "axis": "x", "sign": -1 },
  "z": { "axis": "z", "sign": 1 }
}
```

### B) Flip the part along $x$ (mirror)

```json
{
  "type": "axisAligned",
  "x": { "axis": "x", "sign": -1 },
  "y": { "axis": "y", "sign": 1 },
  "z": { "axis": "z", "sign": 1 }
}
```

---

## 6) Usage in Weapon Assembly

Any part in a weapon assembly may define a `transform`. The transform is applied to:
- **Geometry** for rendering
- **Center of mass** calculations
- **Inertia tensor** mapping

The part’s `position` is applied **after** the transform.
