package com.cwjn.skada.client.hud;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReticleShape {

    private final String name;
    private final ArrayList<ReticleCoordinate> shape = new ArrayList<>();
    private final Map<Float, Float> filledShape;

    public ArrayList<ReticleCoordinate> getOutline() {
        return shape;
    }

    public Map<Float, Float> getFilledShape() {
        return filledShape;
    }

    public String getName() {
        return name;
    }

    public ReticleShape(String name, List<ReticleCoordinate> outline) {
        this.name = name;
        if (outline.size()<3) {
            filledShape = null;
            throw new RuntimeException("Reticle shape '" + name + "' has less than 4 vertices, cannot be used.");
        }
        else {
            ReticleCoordinate.sortByPlace(outline);
            this.filledShape = getFilledShape(outline, 5f);
        }
    }

    public List<ReticleCoordinate> getDrawable() {

        shape.add(shape.get(0)); // Add the first vertex to close the shape
        return shape;
    }

    /*
        Generates a filled shape based on the provided vertices and spacing.
        @param vertices sorted list of ReticleCoordinate representing the vertices of the shape.
        @param spacing the distance between points in the filled shape.
        Uses a grid-based approach to fill the shape with points at specified intervals.
        The resulting points are stored in a map with x being the key and y being the value.
     */
    private static Map<Float, Float> getFilledShape(List<ReticleCoordinate> vertices, float spacing) {
        Map<Float, Float> points = new HashMap<>();
        for (ReticleCoordinate vertex : vertices) {
            points.put(vertex.x(), vertex.y());
        }

        // Determine the bounding box of the shape
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
        for (ReticleCoordinate vertex : vertices) {
            minX = Math.min(minX, vertex.x());
            minY = Math.min(minY, vertex.y());
            maxX = Math.max(maxX, vertex.x());
            maxY = Math.max(maxY, vertex.y());
        }

        // Generate grid points within the bounding box
        for (float x = minX; x <= maxX; x += spacing) {
            for (float y = minY; y <= maxY; y += spacing) {
                Pair<Float, Float> point = new Pair<>(x, y);
                if (isPointInPolygon(point, points)) {
                    points.put(x, y);
                }
            }
        }

        return points;
    }

    // Point-in-polygon algorithm (Ray-casting method)
    private static boolean isPointInPolygon(Pair<Float, Float> point, Map<Float, Float> vertices) {
        int intersectCount = 0;
        List<Map.Entry<Float, Float>> entries = new ArrayList<>(vertices.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Pair<Float, Float> v1 = new Pair<>(entries.get(i).getKey(), entries.get(i).getValue());
            Pair<Float, Float> v2 = new Pair<>(
                    entries.get((i + 1) % entries.size()).getKey(),
                    entries.get((i + 1) % entries.size()).getValue()
            );
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

    public static final Codec<ReticleShape> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ReticleShape::getName),
            Codec.list(ReticleCoordinate.CODEC).fieldOf("shape").forGetter(ReticleShape::getOutline)
    ).apply(instance, ReticleShape::new));

}
