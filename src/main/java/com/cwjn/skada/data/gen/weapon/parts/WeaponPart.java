package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.gen.weapon.util.GeometryUtil;
import com.cwjn.skada.data.gen.weapon.util.PhysicsUtil.MassProperties;
import com.cwjn.skada.data.gen.weapon.util.WeaponAxis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Map;
import java.util.function.Function;

/**
 * Represents a modular component of a weapon that can be assembled together.
 * 
 * WeaponPart defines the interface for various weapon components such as blade parts,
 * handles, guards, and pommels. Each part has physical properties, geometric bounds,
 * and can be transformed during weapon assembly. Important to note that not all parts
 * are 
 * 
 * <h2>Codec System</h2>
 * Parts are serialized and deserialized using a polymorphic codec system. Each concrete
 * implementation must register its codec in {@link #PART_CODECS_BY_TYPE} via its TYPE constant.
 * 
 * <h2>Physical Properties</h2>
 * Parts provide mass and inertia calculations based on a specified density and sample count.
 * These properties are essential for weapon balance and physics simulation.
 * 
 * <h2>Spatial Information</h2>
 * Each part is defined entirely in its own local coordinate space. Assembly-level
 * placement and orientation are carried by {@link WeaponPartEntry} when parts are
 * composed into a weapon.
 * 
 * @see AxePart
 * @see MaceHeadPart
 * @see PickHeadPart
 * @see BladePart
 * @see HandlePart
 * @see GuardPart
 * @see PommelPart
 * @see WeaponPartEntry
 */
public interface WeaponPart {

    Map<String, Codec<? extends WeaponPart>> PART_CODECS_BY_TYPE = Map.of(
        AxePart.TYPE, AxePart.CODEC,
        MaceHeadPart.TYPE, MaceHeadPart.codec(),
        PickHeadPart.TYPE, PickHeadPart.codec(),
        ShovelPart.TYPE, ShovelPart.codec(),
        SpearPart.TYPE, SpearPart.codec(),
        ScythePart.TYPE, ScythePart.CODEC,
        BladePart.TYPE, BladePart.CODEC,
        HandlePart.TYPE, HandlePart.CODEC,
        GuardPart.TYPE, GuardPart.CODEC,
        PommelPart.TYPE, PommelPart.CODEC
    );

    Codec<String> PART_TYPE_CODEC = Codec.STRING.comapFlatMap(
        type -> PART_CODECS_BY_TYPE.containsKey(type)
            ? DataResult.success(type)
            : DataResult.error(() -> "Unknown weapon part type: " + type),
        Function.identity()
    );

    Codec<WeaponPart> CODEC = PART_TYPE_CODEC.dispatch("type", WeaponPart::typeId, PART_CODECS_BY_TYPE::get);

    String typeId();

    MassProperties massProperties(double densityGPerCm3, int samples);

    double momentOfInertiaAboutCenterOfMass(WeaponAxis axis, double densityGPerCm3, int samples);

    GeometryUtil.Bounds localBounds(int samples);

    default double volumeCm3(int samples) {
        return massProperties(1.0, samples).volumeCm3();
    }

}
