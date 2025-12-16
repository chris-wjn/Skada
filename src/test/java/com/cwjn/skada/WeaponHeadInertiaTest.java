package com.cwjn.skada;

import com.cwjn.skada.data.gen.weapon.parts.*;
import com.cwjn.skada.data.gen.weapon.WeaponProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class WeaponHeadInertiaTest {

    private static final double EPSILON = 1e-3;

    @Test
    public void testBladeInertia() {
        double length = 100.0;
        double width = 10.0;
        double thickness = 2.0;
        double density = 7.85; // g/cm^3 (approx density of steel)

        Blade.Bevel rectBevel = new Blade.Bevel(0.0, 2.0);

        Blade blade = new Blade(
                false,
                width, width, Map.of(),
                thickness, thickness, Map.of(),
                length,
                rectBevel,
                Blade.EdgeBevel.noBevel(),
                Blade.TipSpecifications.noTip(),
                Blade.Fuller.noFuller()
        );

        double dist = 50.0; // Pivot 50mm away from base

        double mass = length * width * thickness * density;
        // I_cm for rectangular prism about axis parallel to thickness (edge swing)
        // I_cm = 1/12 * M * (L^2 + W^2)
        double I_cm = (1.0/12.0) * mass * (length*length + width*width);

        // Parallel axis theorem: I = I_cm + M * d^2
        // d is distance from pivot to centroid (dist + L/2)
        double d_cm = dist + length/2.0;
        double expected_I = I_cm + mass * d_cm * d_cm;

        double actual_I = blade.getMomentOfInertia(dist, density, WeaponProfile.HeadOrientation.PARALLEL);

        System.out.println("Expected I: " + expected_I);
        System.out.println("Actual I: " + actual_I);

        Assertions.assertEquals(expected_I, actual_I, expected_I * 0.05, "Blade inertia should match theoretical value within 5%");
    }

    @Test
    public void testAxeHeadInertia() {
        double eyeLen = 50;
        double eyeHeight = 50;
        double eyeThick = 50;
        double cheekLen = 50;
        double cheekHeight = 50;
        double density = 7.85; // g/cm^3

        AxeHead axe = new AxeHead(
            eyeLen, eyeHeight,
            cheekLen, cheekHeight,
            0, 0, // Beard
            0, 0, // Toe
            eyeThick,
            0, 0, // Hole
            null, null
        );

        double dist = 100.0;

        // Eye Calculation
        double mEye = eyeLen * eyeHeight * eyeThick * density;
        double IcmEye = (1.0/12.0) * mEye * (eyeLen*eyeLen + eyeHeight*eyeHeight);
        double dEye = dist; // Eye center is at distance 'dist' from pivot
        double IEye = IcmEye + mEye * dEye * dEye;

        // Cheek Calculation
        double mCheek = cheekLen * cheekHeight * eyeThick * density;
        double IcmCheek = (1.0/12.0) * mCheek * (cheekLen*cheekLen + cheekHeight*cheekHeight);
        // Cheek center X offset from eye center = eyeLen/2 + cheekLen/2 = 50
        // Cheek center Y offset from pivot = dist
        double dCheekSq = 50*50 + dist*dist;
        double ICheek = IcmCheek + mCheek * dCheekSq;

        double expected = IEye + ICheek;

        double actual = axe.getMomentOfInertia(dist, density, WeaponProfile.HeadOrientation.PERPENDICULAR);
        Assertions.assertEquals(expected, actual, expected * 0.05, "Axe inertia should match theoretical value within 5%");
    }

    @Test
    public void testPickHeadInertia() {
        double eyeLen = 50;
        double eyeHeight = 50;
        double eyeThick = 50;
        double spikeLen = 100;
        double spikeBase = 20;
        double density = 7.85; // g/cm^3

        PickHead pick = new PickHead(
            eyeLen, eyeHeight, eyeThick,
            0, 0,
            spikeLen, spikeBase, spikeBase,
            0, 0, 0
        );

        double dist = 100.0;

        // Eye
        double mEye = eyeLen * eyeHeight * eyeThick * density;
        double IcmEye = (1.0/12.0) * mEye * (eyeLen*eyeLen + eyeHeight*eyeHeight);
        double IEye = IcmEye + mEye * dist * dist;

        // Spike (Pyramid)
        double volSpike = (spikeBase * spikeBase * spikeLen) / 3.0;
        double mSpike = volSpike * density;
        // I_cm for pyramid about Z axis (perpendicular to length L and width W)
        // I_cm = M * (3/80 * L^2 + 1/20 * W^2)
        double IcmSpike = mSpike * (3.0/80.0 * spikeLen*spikeLen + 1.0/20.0 * spikeBase*spikeBase);

        // Centroid X from eye center = eyeLen/2 + spikeLen/4 = 25 + 25 = 50
        // Centroid Y from pivot = dist = 100
        double dSpikeSq = 50*50 + 100*100;
        double ISpike = IcmSpike + mSpike * dSpikeSq;

        double expected = IEye + ISpike;

        double actual = pick.getMomentOfInertia(dist, density, WeaponProfile.HeadOrientation.PERPENDICULAR);
        Assertions.assertEquals(expected, actual, expected * 0.05, "Pick inertia should match theoretical value within 5%");
    }

    @Test
    public void testShovelHeadInertia() {
        double socketLen = 50;
        double socketR = 10;
        double bladeLen = 100;
        double bladeWidth = 50;
        double bladeThick = 2;
        double density = 7.85; // g/cm^3

        ShovelHead shovel = new ShovelHead(
            bladeLen, bladeWidth, bladeThick,
            socketLen, socketR, 0,
            0
        );

        double dist = 100.0;

        // Socket (Cylinder)
        double mSocket = Math.PI * socketR * socketR * socketLen * density;
        double IcmSocket = (1.0/12.0) * mSocket * (3*socketR*socketR + socketLen*socketLen);
        double dSocket = dist + socketLen/2.0;
        double ISocket = IcmSocket + mSocket * dSocket * dSocket;

        // Blade (Rectangular Plate)
        double mBlade = bladeLen * bladeWidth * bladeThick * density;
        // I_cm about transverse axis (parallel to width)
        // I = 1/12 * M * (L^2 + T^2)
        double IcmBlade = (1.0/12.0) * mBlade * (bladeLen*bladeLen + bladeThick*bladeThick);
        double dBlade = dist + socketLen + bladeLen/2.0;
        double IBlade = IcmBlade + mBlade * dBlade * dBlade;

        double expected = ISocket + IBlade;

        double actual = shovel.getMomentOfInertia(dist, density, WeaponProfile.HeadOrientation.PERPENDICULAR);
        Assertions.assertEquals(expected, actual, expected * 0.05, "Shovel inertia should match theoretical value within 5%");
    }

    @Test
    public void testMaceHeadInertia() {
        double socketLen = 50;
        double socketR = 10;
        double headLen = 50;
        double headR = 20;
        double density = 7.85; // g/cm^3

        MaceHead mace = new MaceHead(
            MaceHead.HeadShape.CYLINDER,
            headLen, headR,
            0, 0, 0,
            socketLen, socketR, 0
        );

        double dist = 100.0;

        // Socket
        double mSocket = Math.PI * socketR * socketR * socketLen * density;
        double IcmSocket = (1.0/12.0) * mSocket * (3*socketR*socketR + socketLen*socketLen);
        double dSocket = dist + socketLen/2.0;
        double ISocket = IcmSocket + mSocket * dSocket * dSocket;

        // Head (Cylinder)
        double mHead = Math.PI * headR * headR * headLen * density;
        double IcmHead = (1.0/12.0) * mHead * (3*headR*headR + headLen*headLen);
        double dHead = dist + socketLen + headLen/2.0;
        double IHead = IcmHead + mHead * dHead * dHead;

        double expected = ISocket + IHead;

        double actual = mace.getMomentOfInertia(dist, density, WeaponProfile.HeadOrientation.PARALLEL);
        Assertions.assertEquals(expected, actual, expected * 0.05, "Mace inertia should match theoretical value within 5%");
    }

    @Test
    public void testSickleHeadInertia() {
        SickleHead sickle = new SickleHead(
            110, 100, 20,
            110, 100, 20,
            10, 10,
            5,
            null, null
        );

        double dist = 100.0;
        double density = 7.85; // g/cm^3

        double actual = sickle.getMomentOfInertia(dist, density, WeaponProfile.HeadOrientation.PERPENDICULAR);
        Assertions.assertTrue(actual > 0);
    }
}

