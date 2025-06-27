package models;


import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Settings {
    // Work folder
    private String sourcePath = System.getProperty("user.dir");
    // Settings file name
    private String SETTINGS_FILE = "settings.ini";

    // Accepted file extensions and map types
    private Set<String> ALLOWED_FILE_TYPES = new HashSet<>(Arrays.asList(".txt", ".csv", ".0805", ".hero8", ".canyon", ".gpx", ".kml", ".log")); // .log not supported
    private Set<String> ALLOWED_MAP_TYPES = new HashSet<>(Arrays.asList("map", "speed", "both", "settings"));

    // String. Default settings if there is something wrong in the settings.ini file
    private String DEFAULT_FOLDER = "data_files"; // Default location of GPS files
    private String DEFAULT_TYPE_FILE = ".txt";  // Default file type
    private String DEFAULT_TYPE_MAP = "map";    // Default type of generated KML file
    private String DEFAULT_FILE_MAP = "CameraMap.kml";  // Default map file name
    private String DEFAULT_FILE_SPEED = "CameraSpeed.kml"; // Default speed file name
    private String DEFAULT_FILE_KMZ = "CameraKmz.kmz"; // Default .kmz file name

    // int
    private int DEFAULT_LINE_WIDTH = 3; // Default line width
    private int DEFAULT_STOP_MINUTES = 5; // Default parking time in minutes

    // double
    private double DEFAULT_SPEED_MAP = 90.0; // Default speed or minimum value for speed map
    private double DEFAULT_KM_STEPS = 10.0; // Default kilometer marker interval. 0.2 is 200 meters
    private double DEFAULT_SPEED_GPS = 5.0; // Default GPS speed
    private double DEFAULT_MAX_DISTANCE = 2.0; // Default maximum distance for invalid (disrupted) drive
    private double DEFAULT_DIRECTION_SCALE = 1.0; // Default direction icon size

    // Color
    private Color DEFAULT_COLOR_ROAD = new Color(0,0,255);
    private Color DEFAULT_COLOR_SPEED =  new Color(255,0,0);
    private Color DEFAULT_COLOR_START = new Color(0,255,75);
    private Color DEFAULT_COLOR_END = new Color(255,165,0);
    private Color DEFAULT_COLOR_DISRUPTED = new Color(255,0,0);
    private Color DEFAULT_COLOR_PARKING = new Color(255,255,255);
    private Color DEFAULT_COLOR_SPEED_START = new Color(35,139,35);
    private Color DEFAULT_COLOR_SPEED_END = new Color(220,20,60);
    private Color DEFAULT_COLOR_SPEED_DIRECTION = new Color(255,255,255);

    // boolean
    private boolean DEFAULT_OLD_CAMERA = true;
    private boolean DEFAULT_FILE_MERGE = false; // Files with the same date are merged into a single file
    private boolean DEFAULT_KM_SIGN = false;
    private boolean DEFAULT_KM_SIGN_VISIBILITY = false;
    private boolean DEFAULT_KMZ_FILE = false;

    // Other
    private TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("UTC");

    // SETTINGS
    private String defaultFolder;
    private String typeFile;
    private String typeMap;
    private String fileMap;
    private String fileSpeed;
    private String fileKmz;

    private int lineWidth;
    private int stopMinutes;

    private double speedMap;
    private double kmSteps;
    private double maxDistance;
    private double speedGps;
    private double directionScale;

    private Color colorRoad;
    private Color colorSpeed;
    private Color colorStart;
    private Color colorEnd;
    private Color colorDisrupted;
    private Color colorParking;
    private Color colorSpeedStart;
    private Color colorSpeedEnd;
    private Color colorSpeedDirection;

    private boolean oldCamera;
    private boolean fileMerge;
    private boolean kmSign;
    private boolean kmSignVisibility;
    private boolean kmzFile;

    private TimeZone timeZone;

    public Settings() throws IOException {
        if(checkSettingsFileExists()) {
            loadSettings();
        } else {
            System.err.println("No settings file (settings.ini) found. Using default settings.");
            new SettingsExample().generateSettings("settings.ini");
            System.out.println("Sample settings file settings.ini has been created.");
            loadDefaultSettings();
        }
        validateSettings();
    }

    /**
     * Checks if the settings file (settings.ini) exists in the working directory and is a valid file.
     *
     * @return {@code true} if the settings file exists and is a regular file;
     *         {@code false} otherwise
     */
    private boolean checkSettingsFileExists() {
        File file = new File(SETTINGS_FILE);
        return file.exists() && file.isFile();
    }

    /**
     * Loads configuration values from the settings.ini file using Apache Commons Configuration.
     * Values are loaded per section and parsed into appropriate data types.
     * If an error occurs during parsing or loading, default values are used instead.
     */
    private void loadSettings() {
        // Section name must be correct!
        try {
            INIConfiguration config = new INIConfiguration();
            FileHandler fileHandler = new FileHandler(config);
            fileHandler.load(new File(SETTINGS_FILE));

            // Laeme seaded failist
            this.defaultFolder = config.getSection("File").getString("default_folder", DEFAULT_FOLDER);
            this.fileMap = config.getSection("File").getString("file_map", DEFAULT_FILE_MAP);
            this.fileSpeed = config.getSection("File").getString("file_speed", DEFAULT_FILE_SPEED);
            this.fileKmz = config.getSection("File").getString("file_kmz", DEFAULT_FILE_KMZ);

            this.typeFile = config.getSection("Fixed").getString("type_file", DEFAULT_TYPE_FILE);
            this.typeMap = config.getSection("Fixed").getString("type_map", DEFAULT_TYPE_MAP);

            this.lineWidth = parseInteger(config.getSection("Number").getString("line_width"), DEFAULT_LINE_WIDTH, "line_width");
            this.stopMinutes = parseInteger(config.getSection("Number").getString("stop_minutes"), DEFAULT_STOP_MINUTES, "stop_minutes");

            this.speedMap = parseDouble(config.getSection("Number").getString("speed_map"), DEFAULT_SPEED_MAP, "speed_map");
            this.kmSteps = parseDouble(config.getSection("Number").getString("km_steps"), DEFAULT_KM_STEPS, "km_steps");
            this.maxDistance = parseDouble(config.getSection("Number").getString("max_distance"), DEFAULT_MAX_DISTANCE, "max_distance");
            this.speedGps = parseDouble(config.getSection("Number").getString("speed_gps"), DEFAULT_SPEED_GPS, "speed_gps");
            this.directionScale = parseDouble(config.getSection("Number").getString("direction_scale"), DEFAULT_DIRECTION_SCALE, "direction_scale");

            this.oldCamera = parseBoolean(config.getSection("Boolean").getString("old_camera"), DEFAULT_OLD_CAMERA, "old_camera");
            this.fileMerge = parseBoolean(config.getSection("Boolean").getString("file_merge"), DEFAULT_FILE_MERGE, "file_merge");
            this.kmSign = parseBoolean(config.getSection("Boolean").getString("km_sign"), DEFAULT_KM_SIGN, "km_sign");
            this.kmSignVisibility = parseBoolean(config.getSection("Boolean").getString("km_sign_visibility"), DEFAULT_KM_SIGN_VISIBILITY, "km_sign_visibility");
            this.kmzFile = parseBoolean(config.getSection("Boolean").getString("kmz_file"), DEFAULT_KMZ_FILE, "kmz_file");

            this.colorRoad = parseColor(config.getSection("Color").getString("color_road"), DEFAULT_COLOR_ROAD, "color_road");
            this.colorSpeed = parseColor(config.getSection("Color").getString("color_speed"), DEFAULT_COLOR_SPEED, "color_speed");
            this.colorStart = parseColor(config.getSection("Color").getString("color_start"), DEFAULT_COLOR_START, "color_start");
            this.colorEnd = parseColor(config.getSection("Color").getString("color_end"), DEFAULT_COLOR_END, "color_end");
            this.colorDisrupted = parseColor(config.getSection("Color").getString("color_disrupted"), DEFAULT_COLOR_DISRUPTED, "color_disrupted");
            this.colorParking = parseColor(config.getSection("Color").getString("color_parking"), DEFAULT_COLOR_PARKING, "color_parking");
            this.colorSpeedStart = parseColor(config.getSection("Color").getString("color_speed_start"), DEFAULT_COLOR_SPEED_START, "color_speed_start");
            this.colorSpeedEnd = parseColor(config.getSection("Color").getString("color_speed_end"), DEFAULT_COLOR_SPEED_END, "color_speed_end");
            this.colorSpeedDirection = parseColor(config.getSection("Color").getString("color_speed_direction"), DEFAULT_COLOR_SPEED_DIRECTION, "color_speed_direction");

            this.timeZone = parseTimeZone(config.getSection("Other").getString("time_zone"), DEFAULT_TIME_ZONE);

        } catch (ConfigurationException e) {
            System.err.println("Error loading settings file (" + SETTINGS_FILE + "). Using default settings.");
            loadDefaultSettings();
        }
    }

    /**
     * Loads default values for all settings.
     * This method is called when the settings.ini file does not exist
     * or fails to load due to configuration errors.
     */
    private void loadDefaultSettings() {
        this.defaultFolder = DEFAULT_FOLDER;
        this.typeFile = DEFAULT_TYPE_FILE;
        this.typeMap = DEFAULT_TYPE_MAP;
        this.fileMap = DEFAULT_FILE_MAP;
        this.fileSpeed = DEFAULT_FILE_SPEED;
        this.fileKmz = DEFAULT_FILE_KMZ;

        this.lineWidth = DEFAULT_LINE_WIDTH;
        this.stopMinutes = DEFAULT_STOP_MINUTES;
        this.speedMap = DEFAULT_SPEED_MAP;
        this.kmSteps = DEFAULT_KM_STEPS;
        this.maxDistance = DEFAULT_MAX_DISTANCE;
        this.speedGps = DEFAULT_SPEED_GPS;
        this.directionScale = DEFAULT_DIRECTION_SCALE;

        this.oldCamera = DEFAULT_OLD_CAMERA;
        this.fileMerge = DEFAULT_FILE_MERGE;
        this.kmSign = DEFAULT_KM_SIGN;
        this.kmSignVisibility = DEFAULT_KM_SIGN_VISIBILITY;

        this.colorRoad = DEFAULT_COLOR_ROAD;
        this.colorSpeed = DEFAULT_COLOR_SPEED;
        this.colorStart = DEFAULT_COLOR_START;
        this.colorEnd = DEFAULT_COLOR_END;
        this.colorDisrupted = DEFAULT_COLOR_DISRUPTED;
        this.colorParking = DEFAULT_COLOR_PARKING;
        this.colorSpeedStart = DEFAULT_COLOR_SPEED_START;
        this.colorSpeedEnd = DEFAULT_COLOR_SPEED_END;
        this.colorSpeedDirection = DEFAULT_COLOR_SPEED_DIRECTION;

        this.timeZone = DEFAULT_TIME_ZONE;

    }

    /**
     * Validates loaded settings to ensure values are within acceptable ranges and formats.
     * If any setting is invalid or inconsistent (e.g., missing folder, invalid file type),
     * it prints an error message and exits the application.
     */
    private void validateSettings() {
        // Is folder exists
        File dir = new File(defaultFolder);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Folder does not exist: " + defaultFolder + ". Interrupting...");
            System.exit(1);
        }

        // Is folder contains files
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            System.err.println("Folder " + defaultFolder + " is empty. No files of type " + typeFile + " found. Interrupting...");
            System.exit(1);
        }

        // Check filetypes
        if (!ALLOWED_FILE_TYPES.contains(typeFile)) {
            System.err.println("Invalid file type in settings: " + typeFile);
            System.exit(1);
        }

        // Check map types: map, speed, both
        if(!ALLOWED_MAP_TYPES.contains(typeMap)) {
            System.err.println("Invalid map type in settings: " + typeMap);
            System.exit(1);
        }

        // Check line width. Accepted 1-10
        if(lineWidth < 1 || lineWidth > 10) {
            System.err.println("Invalid line width: " + lineWidth + " Using default width: " + DEFAULT_LINE_WIDTH);
            lineWidth = DEFAULT_LINE_WIDTH;
        }

        // Check parking time. Accepted 1-10
        if(stopMinutes < 1 || stopMinutes > 10) {
            System.err.println("Invalid parking time: " + stopMinutes + " Using default parking time: " + DEFAULT_STOP_MINUTES);
            stopMinutes = DEFAULT_STOP_MINUTES;
        }

        // Check speed map speed. Accepted 1-200 km/h
        if(speedMap < 0 || speedMap > 200) {
            System.err.println("Invalid stop minutes: " + stopMinutes + " Using default stop time minutes: " + DEFAULT_STOP_MINUTES);
            speedMap = DEFAULT_SPEED_MAP;
        }

        // Check Speed GPS
        if(speedGps < 0 || speedGps > 10.0) {
            System.err.println("Invalid speed in settings: " + speedGps + ". Using default speed " + DEFAULT_SPEED_GPS);
            speedGps = DEFAULT_SPEED_GPS;
        }

        // Check Speed direction scale size
        if(directionScale <= 0.1 || directionScale > 5.0) {
            System.err.println("Invalid speed direction scale in settings: " + directionScale + ". Using default scale size " + DEFAULT_DIRECTION_SCALE);
            directionScale = DEFAULT_DIRECTION_SCALE;
        }

        // Check map file name (CameraMap.kml)
        if(fileMap == null || fileMap.isEmpty()) {
            System.err.println("Invalid map file name: " + fileMap + " Using default name " + DEFAULT_FILE_MAP);
            fileMap = DEFAULT_FILE_MAP;
        }

        // Check speed map file name (CameraSpeed.kml)
        if(fileSpeed == null || fileSpeed.isEmpty()) {
            System.err.println("Invalid speed map file name: " + fileSpeed + " Using default name: " + DEFAULT_FILE_SPEED);
            fileSpeed = DEFAULT_FILE_SPEED;
        }

        // Check kmz file name (CameraKmz.kmz)
        if(fileKmz == null || fileKmz.isEmpty()) {
            System.err.println("Invalid kmz file name: " + fileKmz + " Using default name: " + DEFAULT_FILE_KMZ);
            fileKmz = DEFAULT_FILE_KMZ;
        }

        // Check km steps
        if(kmSteps < 0 || kmSteps > 1000) {
            System.err.println("Invalid km steps: " + kmSteps + ". Using default km steps " + DEFAULT_KM_STEPS);
            kmSteps = DEFAULT_KM_STEPS;
        }
    }

    /**
     * Parses a string value into an integer.
     * If parsing fails or the value is null, returns a default value and logs a warning.
     *
     * @param value        the string value to parse
     * @param defaultValue fallback value in case of invalid input
     * @param settingName  the name of the setting (for logging purposes)
     * @return parsed integer or default
     */
    private int parseInteger(String value, int defaultValue, String settingName) {
        if (value == null) {
            System.out.println(settingName + " not set. Using default: " + defaultValue);
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid " + settingName + " value: '" + value + "'. Using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Parses a string value into a double.
     * If parsing fails or the value is null, returns a default value and logs a warning.
     *
     * @param value        the string value to parse
     * @param defaultValue fallback value in case of invalid input
     * @param settingName  the name of the setting (for logging purposes)
     * @return parsed double or default
     */
    private double parseDouble(String value, double defaultValue, String settingName) {
        if (value == null) {
            System.out.println(settingName + " not set. Using default: " + defaultValue);
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid " + settingName + " value: '" + value + "'. Using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Parses a string value into a boolean.
     * If parsing fails or the value is null, returns a default value and logs a warning.
     *
     * @param value        the string value to parse
     * @param defaultValue fallback value in case of invalid input
     * @param settingName  the name of the setting (for logging purposes)
     * @return parsed boolean or default
     */
    private boolean parseBoolean(String value, boolean defaultValue, String settingName) {
        if (value == null) {
            System.out.println(settingName + " not set. Using default: " + defaultValue);
            return defaultValue;
        }
        try {
            return Boolean.parseBoolean(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid " + settingName + " value: '" + value + "'. Using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Parses a comma-separated RGB color string (e.g., "255,0,0") into a Color object.
     * If parsing fails or the value is invalid, returns the default color and logs a warning.
     *
     * @param colorString  the RGB string value to parse
     * @param defaultColor fallback color in case of invalid input
     * @param settingName  the name of the setting (for logging purposes)
     * @return parsed Color or default
     */
    private Color parseColor(String colorString, Color defaultColor, String settingName) {
        if (colorString == null) {
            System.out.println(settingName + " not set. Using default: " + defaultColor);
            return defaultColor;
        }
        try {
            String[] rgb = colorString.split(",");
            int r = Integer.parseInt(rgb[0].trim());
            int g = Integer.parseInt(rgb[1].trim());
            int b = Integer.parseInt(rgb[2].trim());
            if (isValidColorValue(r) && isValidColorValue(g) && isValidColorValue(b)) {
                return new Color(r, g, b);
            } else {
                throw new IllegalArgumentException("Invalid RGB range");
            }
        } catch (Exception e) {
            System.err.println("Invalid color value: '" + colorString + "'. Using default: " + defaultColor);
            return defaultColor;
        }
    }

    /**
     * Validates that an integer color component is within the RGB range (0–255).
     *
     * @param value the RGB component value to check
     * @return {@code true} if value is between 0 and 255 (inclusive); {@code false} otherwise
     */
    private boolean isValidColorValue(int value) {
        return value >= 0 && value <= 255;
    }

    /**
     * Parses a time zone ID string and returns the corresponding {@link TimeZone} object.
     * If the ID is invalid or not recognized, the default time zone is returned.
     *
     * @param value            the string representing the time zone ID (e.g., "Europe/Tallinn")
     * @param defaultTimeZone  the fallback time zone if parsing fails
     * @return parsed {@link TimeZone} or default
     */
    private TimeZone parseTimeZone(String value, TimeZone defaultTimeZone) {
        if (value == null) {
            System.out.println("time_zone" + " not set. Using default: " + defaultTimeZone.getID());
            return defaultTimeZone;
        }
        if (TimeZone.getAvailableIDs().length > 0 && Arrays.asList(TimeZone.getAvailableIDs()).contains(value)) {
            return TimeZone.getTimeZone(value);
        } else {
            System.err.println("Invalid " + "time_zone" + " value: '" + value + "'. Using default: " + defaultTimeZone.getID());
            return defaultTimeZone;
        }
    }

    /**
     * Converts a Color object to a KML-specific HEX color code (AABBGGRR format).
     * @param color The color to convert.
     * @param opacity The opacity (alpha) value as a percentage (0-100).
     * @return The KML HEX color string.
     */
    public String rgbToKmlHex(Color color, int opacity) {
        if (color == null) {
            throw new IllegalArgumentException("Color cannot be null");
        }
        if (opacity < 0 || opacity > 100) {
            throw new IllegalArgumentException("Opacity must be between 0 and 100");
        }

        // Calculate the alpha value based on opacity percentage
        int alpha = (int) (opacity / 100.0 * 255);

        // Convert to KML HEX format (AABBGGRR)
        return String.format("%02x%02x%02x%02x",
                alpha,                    // Alpha
                color.getBlue(),          // Blue
                color.getGreen(),         // Green
                color.getRed());          // Red
    }

    // GETTERS

    public String getSourcePath() {
        return sourcePath;
    }

    public String getDefaultFolder() {
        return defaultFolder;
    }

    public String getTypeFile() {
        return typeFile;
    }

    public String getTypeMap() {
        return typeMap;
    }

    public String getFileMap() {
        return fileMap;
    }

    public String getFileSpeed() {
        return fileSpeed;
    }

    public String getFileKmz() {
        return fileKmz;
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public int getStopMinutes() {
        return stopMinutes;
    }

    public double getSpeedMap() {
        return speedMap;
    }

    public double getKmSteps() {
        return kmSteps;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public double getSpeedGps() {
        return speedGps;
    }

    public double getDirectionScale() {
        return directionScale;
    }

    public Color getColorRoad() {
        return colorRoad;
    }

    public Color getColorSpeed() {
        return colorSpeed;
    }

    public Color getColorStart() {
        return colorStart;
    }

    public Color getColorEnd() {
        return colorEnd;
    }

    public Color getColorDisrupted() {
        return colorDisrupted;
    }

    public Color getColorParking() {
        return colorParking;
    }

    public Color getColorSpeedStart() {
        return colorSpeedStart;
    }

    public Color getColorSpeedEnd() {
        return colorSpeedEnd;
    }

    public Color getColorSpeedDirection() {
        return colorSpeedDirection;
    }

    public boolean isOldCamera() {
        return oldCamera;
    }

    public boolean isFileMerge() {
        return fileMerge;
    }

    public boolean isKmSign() {
        return kmSign;
    }

    public boolean isKmSignVisibility() {
        return kmSignVisibility;
    }

    public boolean isKmzFile() {
        return kmzFile;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }
}
