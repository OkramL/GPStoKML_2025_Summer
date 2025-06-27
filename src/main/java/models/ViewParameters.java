package models;

/**
 * Holds camera view parameters for a KML visualization.
 * <p>
 * Defines the default viewpoint (LookAt) used when opening the KML/KMZ file in map viewers
 * such as Google Earth, including center position, altitude, and viewing range.
 */
public class ViewParameters {
    public double latitude;
    public double longitude;
    public double altitude;
    public double range;

    /**
     * Constructs a new {@code ViewParameters} instance with the specified latitude, longitude,
     * altitude, and range.
     *
     * @param latitude  the center latitude of the viewpoint
     * @param longitude the center longitude of the viewpoint
     * @param altitude  the altitude of the camera in meters
     * @param range     the viewing range (distance from camera to focus point) in meters
     */
    public ViewParameters(double latitude, double longitude, double altitude, double range) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.range = range;
    }

    @Override
    public String toString() {
        return "ViewParameters{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", altitude=" + altitude +
                ", range=" + range +
                '}';
    }
}
