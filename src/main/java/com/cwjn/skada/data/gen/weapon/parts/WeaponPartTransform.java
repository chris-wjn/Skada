package com.cwjn.skada.data.gen.weapon.parts;

import java.util.EnumSet;
import java.util.Objects;

import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Axis-aligned coordinate transform for weapon parts.
 *
 * This transform supports rotations that are permutations of axes with optional sign flips.
 * It is intended for aligning part-local coordinate systems (e.g., axe heads) to the
 * weapon assembly coordinate system while keeping the part definition unchanged for
 * isolated viewing.
 */
public final class WeaponPartTransform {

    public record AxisMap(WeaponAxis localAxis, int sign) {
        public AxisMap {
            Objects.requireNonNull(localAxis, "localAxis");
            if (sign != 1 && sign != -1) {
                throw new IllegalArgumentException("sign must be +1 or -1");
            }
        }
        public static final Codec<AxisMap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("localAxis").forGetter(axisMap -> axisMap.localAxis().name()),
                Codec.INT.fieldOf("sign").forGetter(AxisMap::sign)
            ).apply(instance, (localAxis, sign) -> new AxisMap(WeaponAxis.valueOf(localAxis), sign)));
    }

    public static final Codec<WeaponPartTransform> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AxisMap.CODEC.fieldOf("xMap").forGetter(WeaponPartTransform::xMap),
            AxisMap.CODEC.fieldOf("yMap").forGetter(WeaponPartTransform::yMap),
            AxisMap.CODEC.fieldOf("zMap").forGetter(WeaponPartTransform::zMap)
        ).apply(instance, WeaponPartTransform::new));

    private static final WeaponPartTransform IDENTITY = new WeaponPartTransform(
            new AxisMap(WeaponAxis.X, 1),
            new AxisMap(WeaponAxis.Y, 1),
            new AxisMap(WeaponAxis.Z, 1)
    );

    private final AxisMap xMap;
    private final AxisMap yMap;
    private final AxisMap zMap;

    private WeaponPartTransform(AxisMap xMap, AxisMap yMap, AxisMap zMap) {
        this.xMap = Objects.requireNonNull(xMap, "xMap");
        this.yMap = Objects.requireNonNull(yMap, "yMap");
        this.zMap = Objects.requireNonNull(zMap, "zMap");
        validatePermutation(xMap, yMap, zMap);
    }

    public static WeaponPartTransform identity() {
        return IDENTITY;
    }

    /**
     * Create an axis-aligned transform where each weapon axis is mapped to a local axis.
     * Example: weapon X = +local Y, weapon Y = -local X, weapon Z = +local Z.
     */
    public static WeaponPartTransform axisAligned(AxisMap xMap, AxisMap yMap, AxisMap zMap) {
        return new WeaponPartTransform(xMap, yMap, zMap);
    }

    /**
     * Rotate 90 degrees clockwise around +Z (weapon Z), in local coordinates.
     * Local X -> weapon Y, local Y -> weapon X.
     */
    public static WeaponPartTransform rotateAroundZClockwise90() {
        return axisAligned(
                new AxisMap(WeaponAxis.Y, 1),
                new AxisMap(WeaponAxis.X, -1),
                new AxisMap(WeaponAxis.Z, 1)
        );
    }

    public AxisMap xMap() {
        return xMap;
    }

    public AxisMap yMap() {
        return yMap;
    }

    public AxisMap zMap() {
        return zMap;
    }

    public GeometryUtil.Vec3 apply(GeometryUtil.Vec3 local) {
        Objects.requireNonNull(local, "local");
        double[] v = { local.x(), local.y(), local.z() };
        double x = xMap.sign() * v[indexOf(xMap.localAxis())];
        double y = yMap.sign() * v[indexOf(yMap.localAxis())];
        double z = zMap.sign() * v[indexOf(zMap.localAxis())];
        return new GeometryUtil.Vec3(x, y, z);
    }

    public WeaponAxis localAxisForWeaponAxis(WeaponAxis weaponAxis) {
        return switch (weaponAxis) {
            case X -> xMap.localAxis();
            case Y -> yMap.localAxis();
            case Z -> zMap.localAxis();
        };
    }

    public int signForWeaponAxis(WeaponAxis weaponAxis) {
        return switch (weaponAxis) {
            case X -> xMap.sign();
            case Y -> yMap.sign();
            case Z -> zMap.sign();
        };
    }

    public boolean isIdentity() {
        return xMap.localAxis() == WeaponAxis.X && xMap.sign() == 1
                && yMap.localAxis() == WeaponAxis.Y && yMap.sign() == 1
                && zMap.localAxis() == WeaponAxis.Z && zMap.sign() == 1;
    }

    private static int indexOf(WeaponAxis axis) {
        return switch (axis) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
    }

    private static void validatePermutation(AxisMap xMap, AxisMap yMap, AxisMap zMap) {
        EnumSet<WeaponAxis> axes = EnumSet.noneOf(WeaponAxis.class);
        axes.add(xMap.localAxis());
        axes.add(yMap.localAxis());
        axes.add(zMap.localAxis());
        if (axes.size() != 3) {
            throw new IllegalArgumentException("Axis mapping must be a permutation of X/Y/Z");
        }
    }
}
