package com.cwjn.skada.client.hud;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

public class ReticleShape {

    private static final float AXIS_INTERSECTION_EPSILON = 0.0001F;

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

    /**
     * Returns the nearest outline distance from the origin.
     *
     * This lets the HUD size the vanilla crosshair against the actual authored reticle footprint
     * instead of only the horizontal or vertical axis intersections.
     *
     * @return the nearest distance from the origin to the outline, or
     * {@link Float#POSITIVE_INFINITY} when the outline is empty
     */
    public float getNearestOutlineDistance() {
        float nearestDistance = Float.POSITIVE_INFINITY;
        for (int index = 0; index < shape.size() - 1; index++) {
            ReticleCoordinate start = shape.get(index);
            ReticleCoordinate end = shape.get(index + 1);
            nearestDistance = Math.min(nearestDistance, getDistanceToSegment(start.x(), start.y(), end.x(), end.y()));
        }
        return nearestDistance;
    }

    /**
     * Returns the nearest outline distance from the origin along either the horizontal or vertical axis.
     *
     * This is the limiting extent for the vanilla plus-shaped crosshair, whose visible arms extend
     * on the cardinal axes rather than radially toward the reticle outline.
     *
     * @return the nearest axis-aligned outline distance, or {@link Float#POSITIVE_INFINITY} when
     * the outline never intersects either axis
     */
    public float getNearestAxisExtent() {
        float nearestExtent = Float.POSITIVE_INFINITY;
        for (int index = 0; index < shape.size() - 1; index++) {
            ReticleCoordinate start = shape.get(index);
            ReticleCoordinate end = shape.get(index + 1);
            nearestExtent = Math.min(nearestExtent, getAxisIntersectionDistance(start.x(), start.y(), end.x(), end.y()));
            nearestExtent = Math.min(nearestExtent, getAxisIntersectionDistance(start.y(), start.x(), end.y(), end.x()));
        }
        return nearestExtent;
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
            this.filledShape = getFilledShape(outline, 8f);
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
        int index = 0;
        while (index < outline.size()) {
            ReticleCoordinate coord = outline.get(index);
            points.put(coord.x(), coord.y());
            index += 1;
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

    private float getDistanceToSegment(float startX, float startY, float endX, float endY) {
        float deltaX = endX - startX;
        float deltaY = endY - startY;
        float segmentLengthSquared = deltaX * deltaX + deltaY * deltaY;
        if (segmentLengthSquared <= AXIS_INTERSECTION_EPSILON) {
            return (float) Math.sqrt(startX * startX + startY * startY);
        }

        float projection = -(startX * deltaX + startY * deltaY) / segmentLengthSquared;
        projection = Math.max(0.0F, Math.min(1.0F, projection));
        float closestX = startX + deltaX * projection;
        float closestY = startY + deltaY * projection;
        return (float) Math.sqrt(closestX * closestX + closestY * closestY);
    }

    private float getAxisIntersectionDistance(float startAxis,
                                              float startPerpendicular,
                                              float endAxis,
                                              float endPerpendicular) {
        float perpendicularDelta = endPerpendicular - startPerpendicular;
        if (Math.abs(perpendicularDelta) <= AXIS_INTERSECTION_EPSILON) {
            if (Math.abs(startPerpendicular) > AXIS_INTERSECTION_EPSILON) {
                return Float.POSITIVE_INFINITY;
            }
            return Math.min(Math.abs(startAxis), Math.abs(endAxis));
        }

        float interpolation = -startPerpendicular / perpendicularDelta;
        if (interpolation < 0.0F || interpolation > 1.0F) {
            return Float.POSITIVE_INFINITY;
        }
        return Math.abs(startAxis + (endAxis - startAxis) * interpolation);
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
