package com.cwjn.skada.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ReticleShapes {

    public static ArrayList<Pair<Float, Float>> getDrawable(Pair<Float, Float> ...shape) {
        ArrayList<Pair<Float, Float>> ret = new ArrayList<>();
        for (int i = 0; i < shape.length-1; i++) {
            ret.add(shape[i]);
            ret.add(shape[i+1]);
        }
        ret.add(shape[shape.length-1]);
        ret.add(shape[0]);
        return ret;
    }

    public static Pair<Float, Float>[] getFilledShape(Pair<Float, Float>[] vertices, float spacing) {
        List<Pair<Float, Float>> points = new ArrayList<>(List.of(vertices));

        // Determine the bounding box of the shape
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
        for (Pair<Float, Float> vertex : vertices) {
            minX = Math.min(minX, vertex.getA());
            minY = Math.min(minY, vertex.getB());
            maxX = Math.max(maxX, vertex.getA());
            maxY = Math.max(maxY, vertex.getB());
        }

        // Generate grid points within the bounding box
        for (float x = minX; x <= maxX; x += spacing) {
            for (float y = minY; y <= maxY; y += spacing) {
                Pair<Float, Float> point = new Pair<>(x, y);
                if (isPointInPolygon(point, vertices)) {
                    points.add(point);
                }
            }
        }

        @SuppressWarnings("unchecked")
        Pair<Float, Float>[] pointsArray = points.toArray(new Pair[0]);
        return pointsArray;
    }

    // Point-in-polygon algorithm (Ray-casting method)
    private static boolean isPointInPolygon(Pair<Float, Float> point, Pair<Float, Float>[] vertices) {
        int intersectCount = 0;
        for (int i = 0; i < vertices.length; i++) {
            Pair<Float, Float> v1 = vertices[i];
            Pair<Float, Float> v2 = vertices[(i + 1) % vertices.length];
            if (rayIntersectsSegment(point, v1, v2)) {
                intersectCount++;
            }
        }
        return (intersectCount % 2) == 1;
    }

    private static boolean rayIntersectsSegment(Pair<Float, Float> point, Pair<Float, Float> v1, Pair<Float, Float> v2) {
        float px = point.getA();
        float py = point.getB();
        float v1x = v1.getA();
        float v1y = v1.getB();
        float v2x = v2.getA();
        float v2y = v2.getB();

        if (v1y > v2y) {
            float tempX = v1x, tempY = v1y;
            v1x = v2x;
            v1y = v2y;
            v2x = tempX;
            v2y = tempY;
        }

        if (py == v1y || py == v2y) py += 0.0001;

        if (py > v2y || py < v1y || px > Math.max(v1x, v2x)) return false;

        if (px < Math.min(v1x, v2x)) return true;

        float m = (v2y - v1y) / (v2x - v1x);
        float xIntersect = v1x + (py - v1y) / m;

        return px < xIntersect;
    }

    public static Pair<Float, Float>[] SlashDefault = new Pair[]{
            new Pair(50f, -35f),
            new Pair(2f, 2f),
            new Pair(-50f, 35f),
            new Pair(-2f, -2f)
    };

    public static final Pair<Float, Float>[] filledSlashDefault = getFilledShape(SlashDefault, 5f);

    public static Pair<Float, Float>[] CirclePerfectCrosshair = new Pair[]{
            new Pair<>(4.5f, 0f),
            new Pair<>(3.18f, 3.18f),
            new Pair<>(0f, 4.5f),
            new Pair<>(-3.18f, 3.18f),
            new Pair<>(-4.5f, 0f),
            new Pair<>(-3.18f, -3.18f),
            new Pair<>(0f, -4.5f),
            new Pair<>(3.18f, -3.18f),
    };

    public static final Pair<Float, Float>[] filledCirclePerfectCrosshair = getFilledShape(CirclePerfectCrosshair, 5f);

    public static Pair<Float, Float>[] CircleRad5 = new Pair[]{
            new Pair<>(5f, 0f),
            new Pair<>(3.54f, 3.54f),
            new Pair<>(0f, 5f),
            new Pair<>(-3.54f, 3.54f),
            new Pair<>(-5f, 0f),
            new Pair<>(-3.54f, -3.54f),
            new Pair<>(0f, -5f),
            new Pair<>(3.54f, -3.54f),
    };

    public static Pair<Float, Float>[] CircleRad10 = new Pair[]{
            new Pair<>(10f, 0f),
            new Pair<>(7.07f, 7.07f),
            new Pair<>(0f, 10f),
            new Pair<>(-7.07f, 7.07f),
            new Pair<>(-10f, 0f),
            new Pair<>(-7.07f, -7.07f),
            new Pair<>(0f, -10f),
            new Pair<>(7.07f, -7.07f),
    };

    public static Pair<Float, Float>[] CircleRad15 = new Pair[]{
            new Pair<>(15f, 0f),
            new Pair<>(10.6f, 10.6f),
            new Pair<>(0f, 15f),
            new Pair<>(-10.6f, 10.6f),
            new Pair<>(-15f, 0f),
            new Pair<>(-10.6f, -10.6f),
            new Pair<>(0f, -15f),
            new Pair<>(10.6f, -10.6f),
    };

    public static final Pair<Float, Float>[] filledCircleRad15 = getFilledShape(CircleRad15, 5f);

    public static Pair<Float, Float>[] CircleRad30 = new Pair[]{
            new Pair<>(30f, 0f),
            new Pair<>(21.2f, 21.2f),
            new Pair<>(0f, 30f),
            new Pair<>(-21.2f, 21.2f),
            new Pair<>(-30f, 0f),
            new Pair<>(-21.2f, -21.2f),
            new Pair<>(0f, -30f),
            new Pair<>(21.2f, -21.2f),
    };

    public static Pair<Float, Float>[] CircleRad45 = new Pair[]{
            new Pair<>(45f, 0f),
            new Pair<>(31.82f, 31.82f),
            new Pair<>(0f, 45f),
            new Pair<>(-31.82f, 31.82f),
            new Pair<>(-45f, 0f),
            new Pair<>(-31.82f, -31.82f),
            new Pair<>(0f, -45f),
            new Pair<>(31.82f, -31.82f),
    };

}
