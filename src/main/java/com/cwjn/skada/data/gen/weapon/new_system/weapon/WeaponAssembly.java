package com.cwjn.skada.data.gen.weapon.new_system.weapon;

import com.cwjn.skada.data.gen.weapon.ExtraTierInfo;
import com.cwjn.skada.data.gen.weapon.new_system.geometry.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.new_system.geometry.GeometryUtil.Vec3;
import com.cwjn.skada.data.gen.weapon.new_system.geometry.PhysicsUtil.MassProperties;
import com.cwjn.skada.data.gen.weapon.new_system.weapon.AttackCapable.AttackCapability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.cwjn.skada.data.registry.AttackType;

/**
 * A weapon assembled from multiple parts (blade, handle, guard, pommel, etc.).
 *
 * Units: centimeters for length, grams for mass, g/cm^3 for density.
 */
public final class WeaponAssembly {
    private final List<WeaponPartEntry> parts;
    public static final int SMALL_SAMPLE_SIZE = 64;
    public static final int LARGE_SAMPLE_SIZE = 1024;
    private static final int DEFAULT_MASS = 1300; // grams. TODO: set this automatically based on all weapon entries?

    public WeaponAssembly(List<WeaponPartEntry> parts) {
        Objects.requireNonNull(parts, "parts");
        this.parts = Collections.unmodifiableList(new ArrayList<>(parts));
    }

    public List<WeaponPartEntry> parts() {
        return parts;
    }

    public double volume(int samples) {
        return parts.stream()
                .mapToDouble(entry -> entry.part().volumeCm3(samples))
                .sum();
    }

    public double mass(int samples) {
        return parts.stream()
                .mapToDouble(entry -> entry.part().massProperties(entry.material().density(), samples).massG())
                .sum();
    }

    public GeometryUtil.Vec3 centerOfMass(int samples) {
        double totalMass = 0.0;
        GeometryUtil.Vec3 weightedSum = new GeometryUtil.Vec3(0, 0, 0);

        for (WeaponPartEntry entry : parts) {
            MassProperties props = entry.part().massProperties(entry.material().density(), samples);
            Vec3 localCom = props.centerOfMass();
            Vec3 worldCom = entry.part().transform().apply(localCom).add(entry.part().position());
            double mass = props.massG();
            totalMass += mass;
            weightedSum = weightedSum.add(worldCom.mul(mass));
        }

        if (totalMass <= 0.0) {
            return new GeometryUtil.Vec3(0, 0, 0);
        }
        return weightedSum.mul(1.0 / totalMass);
    }

    public double pointOfBalance(int samples) {
        return centerOfMass(samples).x();
    }

    public double momentOfInertiaAboutBase(WeaponAxis axis, int samples) {
        return parts.stream()
                .mapToDouble(entry -> entry.part().momentOfInertiaAboutWeaponBase(axis, entry.material().density(), samples))
                .sum();
    }

    /**
     * Returns the total weapon length along the handle axis (weapon X), in cm.
     */
    public double length() {
        if (parts.isEmpty()) {
            return 0.0;
        }

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;

        for (WeaponPartEntry entry : parts) {
            WeaponPart part = entry.part();
            GeometryUtil.Bounds bounds = part.localBounds(SMALL_SAMPLE_SIZE);
            WeaponPartTransform transform = part.transform();
            WeaponAxis localAxis = transform.localAxisForWeaponAxis(WeaponAxis.X);
            int sign = transform.signForWeaponAxis(WeaponAxis.X);

            double localMin = switch (localAxis) {
                case X -> bounds.minX();
                case Y -> bounds.minY();
                case Z -> bounds.minZ();
            };
            double localMax = switch (localAxis) {
                case X -> bounds.maxX();
                case Y -> bounds.maxY();
                case Z -> bounds.maxZ();
            };
            double originX = part.position().x();

            double partMinX;
            double partMaxX;
            if (sign >= 0) {
                partMinX = originX + localMin;
                partMaxX = originX + localMax;
            } else {
                partMinX = originX - localMax;
                partMaxX = originX - localMin;
            }

            minX = Math.min(minX, partMinX);
            maxX = Math.max(maxX, partMaxX);
        }

        return Math.max(0.0, maxX - minX);
    }

    /**
     * Calculates the ideal point of balance (PoB) in cm for this weapon for a specific attack type
     * @return the ideal PoB in cm along the length of the weapon
     */
    public double idealPointOfBalanceForAttackType(AttackType attackType) {
        WeaponPartEntry attackHead = primaryPartForAttackType(attackType, SMALL_SAMPLE_SIZE).get();
        WeaponPart part = attackHead.part();
        if (part instanceof AttackCapable capable) {
            return capable.idealPointOfBalance(attackType);
        }
        return part.position().x();
    }

    /**
     * Normalize the weapon assembly mass to a standard value for use in calculations.
     * We don't want to just divide a default value by the mass here, because
     * that would make very light or very heavy weapons have extreme values.
     * Instead, we use a logarithmic scale to keep values within a reasonable range,
     * with
     * an average value of 1.0 for a weapon assembly mass of 1300 grams (the default).
     *
     * @return a double representing the normalized weapon assembly mass.
     */
    public double normalizedMass(ExtraTierInfo material) { // in grams, assuming iron density of 7.85 g/cm³
        double mass = this.mass(LARGE_SAMPLE_SIZE); // in grams
        return Math.atan(
                (mass / (DEFAULT_MASS + 200)) - (DEFAULT_MASS / (DEFAULT_MASS + 200))) / 2
                + 1;
    }

    public List<WeaponPartEntry> partsForAttack(AttackCapability capability) {
        return parts.stream()
                .filter(entry -> entry.part() instanceof AttackCapable capable && capable.supports(capability))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<WeaponPartEntry> partsForAttackType(AttackType attackType) {
        return mapAttackType(attackType)
                .map(this::partsForAttack)
                .orElse(List.of());
    }

    public Optional<WeaponPartEntry> primaryPartForAttack(AttackCapability capability, int samples) {
        return partsForAttack(capability).stream()
                .max((a, b) -> Double.compare(a.part().volumeCm3(samples), b.part().volumeCm3(samples)));
    }

    public Optional<WeaponPartEntry> primaryPartForAttackType(AttackType attackType, int samples) {
        return mapAttackType(attackType)
                .flatMap(capability -> primaryPartForAttack(capability, samples));
    }

    private static Optional<AttackCapability> mapAttackType(AttackType attackType) {
        if (attackType == null) {
            return Optional.empty();
        }
        if (attackType.equals(AttackType.slash())) {
            return Optional.of(AttackCapability.SLASH);
        }
        if (attackType.equals(AttackType.thrust())) {
            return Optional.of(AttackCapability.THRUST);
        }
        if (attackType.equals(AttackType.strike())) {
            return Optional.of(AttackCapability.STRIKE);
        }
        return Optional.empty();
    }

    
}
