package models;

import java.util.List;

/**
 * Represents a named segment of a route consisting of multiple coordinate points.
 * <p>
 * Each segment has a list of {@link CoordinatePoint}s, a name, and an associated style ID
 * for visual representation in KML output (e.g., line color or icon style).
 */
public class Segment {
    private final List<CoordinatePoint> points;
    private final String name;
    private final String styleId;

    /**
     * Constructs a new {@code Segment} with the given coordinate points, name, and style identifier.
     *
     * @param points   the list of coordinate points that form the segment path
     * @param name     the name or label for the segment (e.g., "MainRoute", "DisruptedSection")
     * @param styleId  the KML style ID used for rendering this segment visually
     */
    public Segment(List<CoordinatePoint> points, String name, String styleId) {
        this.points = points;
        this.name = name;
        this.styleId = styleId;
    }

    /**
     * Constructs a new {@code Segment} with the given coordinate points and name,
     * using the default style ID {@code "lineStyleRoad"}.
     * <p>
     * This constructor is intended for backward compatibility where older code
     * does not explicitly provide a style ID.
     *
     * @param points the list of coordinate points that form the segment
     * @param name   the name or label for the segment
     */
    public Segment(List<CoordinatePoint> points, String name) {
        this(points, name, "lineStyleRoad"); // default style
    }

    public List<CoordinatePoint> getPoints() {
        return points;
    }

    public String getName() {
        return name;
    }

    public String getStyleId() {
        return styleId;
    }
}

