package models;

import java.time.ZonedDateTime;

/**
 * Represents a geographical coordinate point with longitude, latitude,
 * and an associated timestamp.
 * <p>
 * This class is immutable and thread-safe.
 */
public record CoordinatePoint(double longitude, double latitude, ZonedDateTime pointTime) {
    /**
     * Constructs a new {@code CoordinatePoint} with the specified longitude, latitude,
     * and timestamp.
     *
     * @param longitude the longitude of the point (in decimal degrees)
     * @param latitude  the latitude of the point (in decimal degrees)
     * @param pointTime the time associated with this point (as a {@link ZonedDateTime})
     */
    public CoordinatePoint {
    }
}
