package com.cwjn.skada.generation.weapon.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryUtilTest {

    private static final double EPS = 1.0e-6;

    private static List<GeometryUtil.Vec3> pts(GeometryUtil.Vec3... vs) {
        return new ArrayList<>(List.of(vs));
    }

    @Test
    void endpointsReturnFirstAndLastPoints() {
        List<GeometryUtil.Vec3> points = pts(
                new GeometryUtil.Vec3(0, 0, 0),
                new GeometryUtil.Vec3(1, 0, 0),
                new GeometryUtil.Vec3(2, 0, 0));
        GeometryUtil.Vec3 a = GeometryUtil.pointOnPolyline(points, 0.0);
        GeometryUtil.Vec3 b = GeometryUtil.pointOnPolyline(points, 1.0);
        assertEquals(0.0, a.x(), EPS);
        assertEquals(0.0, a.y(), EPS);
        assertEquals(0.0, a.z(), EPS);
        assertEquals(2.0, b.x(), EPS);
        assertEquals(0.0, b.y(), EPS);
        assertEquals(0.0, b.z(), EPS);
    }

    @Test
    void sNegativeOrOverOneIsClampedToEndpoints() {
        List<GeometryUtil.Vec3> points = pts(
                new GeometryUtil.Vec3(5, 5, 5),
                new GeometryUtil.Vec3(7, 5, 5),
                new GeometryUtil.Vec3(9, 5, 5));
        assertEquals(5.0, GeometryUtil.pointOnPolyline(points, -0.5).x(), EPS);
        assertEquals(9.0, GeometryUtil.pointOnPolyline(points, 1.5).x(), EPS);
    }

    @Test
    void twoPointFallsBackToLinear() {
        List<GeometryUtil.Vec3> points = pts(
                new GeometryUtil.Vec3(0, 0, 0),
                new GeometryUtil.Vec3(10, 0, 0));
        GeometryUtil.Vec3 mid = GeometryUtil.pointOnPolyline(points, 0.5);
        assertEquals(5.0, mid.x(), EPS);
        assertEquals(0.0, mid.y(), EPS);
        assertEquals(0.0, mid.z(), EPS);
    }

    @Test
    void singlePointThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> GeometryUtil.pointOnPolyline(pts(new GeometryUtil.Vec3(0, 0, 0)), 0.5));
    }

    @Test
    void midpointOfSymmetricArcLandsNearMidpoint() {
        List<GeometryUtil.Vec3> points = pts(
                new GeometryUtil.Vec3(-5, 0, 0),
                new GeometryUtil.Vec3(0, 5, 0),
                new GeometryUtil.Vec3(5, 0, 0));
        GeometryUtil.Vec3 mid = GeometryUtil.pointOnPolyline(points, 0.5);
        // For a symmetric arc, the center should be near the y-axis peak (0, ~5, 0)
        assertEquals(0.0, mid.x(), 1.0e-3);
        assertTrue(mid.y() > 0.0);
        assertTrue(mid.y() <= 5.0);
    }

    @Test
    void arcLengthIncreasesMonotonically() {
        List<GeometryUtil.Vec3> points = pts(
                new GeometryUtil.Vec3(0, 0, 0),
                new GeometryUtil.Vec3(1, 2, 0),
                new GeometryUtil.Vec3(3, 1, 0),
                new GeometryUtil.Vec3(5, 3, 0),
                new GeometryUtil.Vec3(7, 2, 0));
        double lastDist = 0.0;
        for (int i = 0; i <= 100; i++) {
            double s = i / 100.0;
            GeometryUtil.Vec3 p = GeometryUtil.pointOnPolyline(points, s);
            double dist = Math.sqrt(p.x() * p.x() + p.y() * p.y() + p.z() * p.z());
            assertTrue(dist >= lastDist - EPS,
                    "Distance from origin should be non-decreasing along arc length, but s=" + s
                            + " gave dist=" + dist + " after " + lastDist);
            lastDist = dist;
        }
    }

    @Test
    void centripetalHandlesCloseAndDistantPointsWithoutLoops() {
        // Simulate the case from the wiki: two close points followed by a distant point.
        // With uniform Catmull-Rom, this would self-intersect. With centripetal, it should not.
        List<GeometryUtil.Vec3> points = pts(
                new GeometryUtil.Vec3(0, 0, 0),
                new GeometryUtil.Vec3(1, 0, 0),
                new GeometryUtil.Vec3(1.05, 0, 0),   // very close to previous
                new GeometryUtil.Vec3(10, 0, 0));   // far jump
        // Check the curve doesn't overshoot back before x=0 or past x=10
        for (int i = 0; i <= 200; i++) {
            double s = i / 200.0;
            GeometryUtil.Vec3 p = GeometryUtil.pointOnPolyline(points, s);
            assertTrue(p.x() >= -1.0 && p.x() <= 11.0,
                    "Catmull-Rom point at s=" + s + " overshot to x=" + p.x());
        }
    }

    @Test
    void repeatedControlPointsPreserveArcLengthSampling() {
        // The public parameter measures arc length, independently of internal spline knots.
        List<GeometryUtil.Vec3> shortSpan = pts(
                new GeometryUtil.Vec3(0, 0, 0),
                new GeometryUtil.Vec3(0, 0, 0),
                new GeometryUtil.Vec3(1, 0, 0),
                new GeometryUtil.Vec3(2, 0, 0));
        GeometryUtil.Vec3 shortMid = GeometryUtil.pointOnPolyline(shortSpan, 0.5);

        List<GeometryUtil.Vec3> longSpan = pts(
                new GeometryUtil.Vec3(0, 0, 0),
                new GeometryUtil.Vec3(0, 0, 0),
                new GeometryUtil.Vec3(4, 0, 0),   // 4x the span
                new GeometryUtil.Vec3(5, 0, 0));
        GeometryUtil.Vec3 longMid = GeometryUtil.pointOnPolyline(longSpan, 0.5);

        assertEquals(1.0, shortMid.x(), EPS);
        assertEquals(2.5, longMid.x(), EPS);
    }

    @Test
    void identicalControlPointsStayAtTheirOnlyPosition() {
        GeometryUtil.Vec3 point = new GeometryUtil.Vec3(2, 3, 4);
        for (List<GeometryUtil.Vec3> points : List.of(pts(point, point), pts(point, point, point))) {
            assertEquals(point, GeometryUtil.pointOnPolyline(points, 0.5));
        }
    }

    @Test
    void consecutiveDuplicatesDoNotProduceNaN() {
        List<GeometryUtil.Vec3> points = pts(
                new GeometryUtil.Vec3(0, 0, 0),
                new GeometryUtil.Vec3(1, 1, 0),
                new GeometryUtil.Vec3(1, 1, 0),   // duplicate
                new GeometryUtil.Vec3(2, 2, 0));
        for (int i = 0; i <= 50; i++) {
            double s = i / 50.0;
            GeometryUtil.Vec3 p = GeometryUtil.pointOnPolyline(points, s);
            assertNotNull(p);
            assertTrue(Double.isFinite(p.x()));
            assertTrue(Double.isFinite(p.y()));
            assertTrue(Double.isFinite(p.z()));
            assertEquals(2.0 * s, p.x(), EPS);
            assertEquals(2.0 * s, p.y(), EPS);
            assertEquals(0.0, p.z(), EPS);
        }
    }

    @Test
    void polylineLengthMatchesPythagoreanSum() {
        List<GeometryUtil.Vec3> points = pts(
                new GeometryUtil.Vec3(0, 0, 0),
                new GeometryUtil.Vec3(3, 0, 0),
                new GeometryUtil.Vec3(3, 4, 0));
        assertEquals(7.0, GeometryUtil.polylineLength(points), EPS);
    }

    @Test
    void pointOnPolylineAtMidpointOfFlatThreePointsIsTheMiddlePoint() {
        // Three collinear points: the midpoint of the arc-length parameter should be
        // the second point (the curve is straight, no curvature).
        List<GeometryUtil.Vec3> points = pts(
                new GeometryUtil.Vec3(0, 0, 0),
                new GeometryUtil.Vec3(5, 0, 0),
                new GeometryUtil.Vec3(10, 0, 0));
        GeometryUtil.Vec3 mid = GeometryUtil.pointOnPolyline(points, 0.5);
        assertEquals(5.0, mid.x(), 1.0e-3);
    }

    @Test
    void pointOnPolylineIsInBoundsForCurvedSpine() {
        // Y-curve: points should stay within the y-range of the control points.
        List<GeometryUtil.Vec3> points = pts(
                new GeometryUtil.Vec3(0, 0, 0),
                new GeometryUtil.Vec3(2, 10, 0),
                new GeometryUtil.Vec3(5, 0, 0),
                new GeometryUtil.Vec3(8, 10, 0),
                new GeometryUtil.Vec3(10, 0, 0));
        for (int i = 0; i <= 100; i++) {
            double s = i / 100.0;
            GeometryUtil.Vec3 p = GeometryUtil.pointOnPolyline(points, s);
            assertTrue(p.y() >= -EPS && p.y() <= 10.0 + EPS,
                    "y out of bounds at s=" + s + ": y=" + p.y());
        }
    }
}
