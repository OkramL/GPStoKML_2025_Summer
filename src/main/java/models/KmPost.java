package models;

/**
 * Represents a kilometer post on the map, associated with a specific data point.
 * <p>
 * Contains the kilometer marker value, direction of movement, and the corresponding {@link DataPoint}.
 * Useful for visualizing distance markers or intervals along a route in mapping applications.
 */
public record KmPost(DataPoint dataPoint, double kmNumber, double direction) {

    /**
     * Constructs a new {@code KmPost} with the given data point, kilometer number, and direction.
     *
     * @param dataPoint the data point at which the kilometer post is placed
     * @param kmNumber  the cumulative distance (in kilometers) from the start point
     * @param direction the direction of movement at this post (in degrees)
     */
    public KmPost {
    }
}
