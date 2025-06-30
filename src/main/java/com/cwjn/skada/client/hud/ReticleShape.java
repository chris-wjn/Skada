package com.cwjn.skada.client.hud;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReticleShape {

    private final String name;
    private final List<ReticleCoordinate> shape;
    private final Multimap<Float, Float> filledShape;

    public List<ReticleCoordinate> getOutline() {
        return shape;
    }

    public Multimap<Float, Float> getFilledShape() {
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
            shape = new ArrayList<>(outline);
            shape.add(outline.get(0)); // Close the shape by adding the first vertex at the end
            this.filledShape = getFilledShape(outline, 3f);
        }
    }

    private Multimap<Float, Float> getFilledShape(List<ReticleCoordinate> outline, float spacing) {

         ImmutableMultimap.Builder<Float, Float> points = ImmutableMultimap.builder();

        // Find bounding box
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (ReticleCoordinate coord : outline) {
            minX = Math.min(minX, coord.x());
            minY = Math.min(minY, coord.y());
            maxX = Math.max(maxX, coord.x());
            maxY = Math.max(maxY, coord.y());
        }

         /*
            We don't want to add the entire outline if the shape has a lot of vertices,
         */
        int increment = 1 + (int) Math.floor(outline.size() / 10f);
        int index = 0;
        while (index < outline.size()) {
            ReticleCoordinate coord = outline.get(index);
            points.put(coord.x(), coord.y());
            index += increment;
        }

        // Scan the bounding box with the specified spacing
        for (float x = minX; x <= maxX; x += spacing) {
            for (float y = minY; y <= maxY; y += spacing) {
                if (isPointInPolygon(x, y, outline)) {
                    points.put(x, y);
                }
            }
        }

        return points.build();
    }

    private boolean isPointInPolygon(float px, float py, List<ReticleCoordinate> vertices) {
        boolean inside = false;
        int n = vertices.size();

        // Slightly adjust py to avoid edge cases
        py += 0.0001f;

        for (int i = 0, j = n - 1; i < n; j = i++) {
            float xi = vertices.get(i).x();
            float yi = vertices.get(i).y();
            float xj = vertices.get(j).x();
            float yj = vertices.get(j).y();

            if (((yi > py) != (yj > py)) &&
                    (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }

        return inside;
    }

    public static final Codec<ReticleShape> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ReticleShape::getName),
            Codec.list(ReticleCoordinate.CODEC).fieldOf("shape").forGetter(ReticleShape::getOutline)
    ).apply(instance, ReticleShape::new));

}
