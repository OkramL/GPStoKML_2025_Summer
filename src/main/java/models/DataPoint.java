package models;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Represents a single data point containing geographical coordinates, timestamp, speed,
 * and additional metadata such as date, description, and context tags.
 * <p>
 * This class is immutable, except for the {@code description} field which can be modified if needed.
 */
public class DataPoint {
    private final double latitude;
    private final double longitude;
    private final ZonedDateTime pointTime;
    private final String date;
    private final double speed;
    private final String explanation;
    private String description;
    private final String dayName;

    /**
     * Constructs a new {@code DataPoint} instance with the given properties.
     *
     * @param latitude     the latitude of the point (in decimal degrees)
     * @param longitude    the longitude of the point (in decimal degrees)
     * @param pointTime    the timestamp associated with this point
     * @param date         the date string (typically derived from the filename)
     * @param speed        the speed value associated with this point
     * @param explanation  an optional explanation or tag describing the data context
     * @param description  a human-readable description for the point
     * @param dayName      the grouping name representing the day and optional description
     */
    public DataPoint(double latitude, double longitude, ZonedDateTime pointTime, String date, double speed,
                     String explanation, String description, String dayName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.pointTime = pointTime;
        this.date = date;
        this.speed = speed;
        this.explanation = explanation;
        this.description = description;
        this.dayName = dayName;
    }

    @Override
    public String toString() {
        return "DataPoint{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", pointTime=" + pointTime +
                ", date='" + date + '\'' +
                ", speed=" + speed +
                ", explanation='" + explanation + '\'' +
                ", description='" + description + '\'' +
                ", dayName='" + dayName + '\'' +
                '}';
    }

    /**
     * Calculates the view parameters from a list of DataPoint objects.
     * @param dataPoints List of DataPoint objects
     * @return ViewParameters object containing average latitude, longitude, altitude, and range.
     */
    public static ViewParameters calculateViewParameters(List<DataPoint> dataPoints) {
        if (dataPoints == null || dataPoints.isEmpty()) {
            throw new IllegalArgumentException("DataPoints list cannot be null or empty");
        }

        // 1. Arvuta keskmine punkt
        DataPoint averagePoint = calculateAveragePoint(dataPoints);
        double averageLatitude = averagePoint.getLatitude();
        double averageLongitude = averagePoint.getLongitude();

        // 2. Arvuta kaamera kõrgus ja kaugus
        double[] altitudeAndRange = calculateAltitudeAndRange(dataPoints);
        double altitude = altitudeAndRange[0];
        double range = altitudeAndRange[1];

        // 3. Tagasta ViewParameters objekt
        return new ViewParameters(averageLatitude, averageLongitude, altitude, range);
    }

    /**
     * Calculates the average latitude and longitude from a list of DataPoint objects.
     * @param dataPoints List of DataPoint objects
     * @return A new DataPoint representing the average location
     * ChatGPT
     */
    public static DataPoint calculateAveragePoint(List<DataPoint> dataPoints) {
        if (dataPoints == null || dataPoints.isEmpty()) {
            throw new IllegalArgumentException("DataPoints list cannot be null or empty");
        }

        double totalLatitude = 0.0;
        double totalLongitude = 0.0;

        for (DataPoint point : dataPoints) {
            totalLatitude += point.getLatitude();
            totalLongitude += point.getLongitude();
        }

        double averageLatitude = totalLatitude / dataPoints.size();
        double averageLongitude = totalLongitude / dataPoints.size();

        // Loome uue DataPoint objekti keskmiste koordinaatidega (muud väljad võib täita vastavalt vajadusele)
        return new DataPoint(
                averageLatitude,
                averageLongitude,
                null, // pointTime, kui soovite, võite selle seada kohalikule ajale
                null, // filenameDate
                0.0, // speed
                "Keskmine punkt", // description
                "Arvutatud keskmine punkt", // explanation
                null // dayName
        );
    }

    /**
     * Calculates the altitude and range needed to view all DataPoints.
     * @param dataPoints List of DataPoint objects
     * @return A double array where index 0 is altitude and index 1 is range.
     * ChatGPT
     */
    public static double[] calculateAltitudeAndRange(List<DataPoint> dataPoints) {
        if (dataPoints == null || dataPoints.isEmpty()) {
            throw new IllegalArgumentException("DataPoints list cannot be null or empty");
        }

        double minLatitude = Double.MAX_VALUE;
        double maxLatitude = Double.MIN_VALUE;
        double minLongitude = Double.MAX_VALUE;
        double maxLongitude = Double.MIN_VALUE;

        for (DataPoint point : dataPoints) {
            minLatitude = Math.min(minLatitude, point.getLatitude());
            maxLatitude = Math.max(maxLatitude, point.getLatitude());
            minLongitude = Math.min(minLongitude, point.getLongitude());
            maxLongitude = Math.max(maxLongitude, point.getLongitude());
        }

        double latitudeSpan = maxLatitude - minLatitude;
        double longitudeSpan = maxLongitude - minLongitude;
        double maxSpan = Math.max(latitudeSpan, longitudeSpan);

        double altitude = (maxSpan / 360) * 40075 * 1000; // Maailma ümbermõõt ja teisendamine meetriteks

        return new double[]{altitude, altitude};
    }

    // GETTERS

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public ZonedDateTime getPointTime() {
        return pointTime;
    }

    public String getDate() {
        return date;
    }

    public double getSpeed() {
        return speed;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getDescription() {
        return description;
    }

    public String getDayName() {
        return dayName;
    }

    // SETTER

    public void setDescription(String description) {
        this.description = description;
    }

}
