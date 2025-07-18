package models;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for generating a fully documented example INI configuration file.
 * <p>
 * This class is typically used to create a sample {@code settings.ini} file
 * containing all available configuration keys, default values, and inline documentation.
 * The output can serve as a reference for users to rename and edit as {@code settings.ini}.
 */
public class SettingsExample {
    private static final String EXAMPLE_INI = """
            [File]
            # Default folder where files are searched.
            default_folder=data_files
            # The file name of the map with the extension. The file is created in the same location as the application.
            file_map=CameraMap.kml
            # The speed file name with extension. The file is created in the same location as the application.
            file_speed=CameraSpeed.kml
            # KMZ file name with extension. The file is created in the same location as the application.
            file_kmz=CameraKmz.kmz
            [Fixed]
            # Allowed file extensions are: .txt, .0805, .csv, .hero8, .canyon, .gpx, .kml (.log)
            type_file=.gpx
            # What type of file is being created. map, speed, both or settings? The settings setting creates a commented sample
            # settings file. Can be used if renamed settings.ini
            type_map=both
            [Number]
            # Line thickness on the map
            line_width=3
            # The length of the pause in minutes before starting a new line. This means parking.
            stop_minutes=1
            # The lowest speed that goes on the speed card.
            speed_map=18.0
            # Kilometer posts every x kilometers (default 10 km). Can also be in meters 0.2 (200 meters)
            km_steps=2
            # Signal lost distance in km.
            max_distance=2.0
            # Speed when actually moving (type_map map). Too small a number can draw meaningless lines in one place on the map.
            # Too big can lose some of the ride. This is a GPS for smoothing lines. Depending on the file type, it can be one or
            # the other number. In addition, it also depends on the GPS's own logging data. This is speed from source.
            # Range 0.0 - 10.0 default 5.0. 0.0 means that all points from the file to the map are even standing points. Higher
            # speed fewer points, lower speed more points. Use .csv file parse.
            speed_gps=1.0
            # Size of KML speed file icons. Allowed values are 0.1 to 5.0. Default is 1.0.
            direction_scale=0.4
            [Boolean]
            # The new camera is 0805 false and the old camera is 0803 true. This is important if the file type is .txt. The GPS
            # data from the new camera is strange.
            old_camera=true
            # If there are multiple files on a single day, then whether to convert these files into a single day entry (true)
            # or not (false). Default false (all files are visible separately on the map. Repeating dates.) If true, the merged
            # trip name will be used. If false, each day will contain its own filename description.
            file_merge=false
            # Calculate the locations of the kilometer posts. Default false (do not calculate. There is no Kilometer post folder
            # inside day folder). Also for speed marker to.
            km_sign=true
            # Are the kilometer posts visible on the map or do they need to be clicked separately? (by default requires
            # clicking - false). Also for speed marker to.
            km_sign_visibility=false
            # Create a KMZ file or not. If not, a files folder will be created with the necessary icons, so the kml file must be\s
            # shared with this folder so that the icons are visible on the map.
            kmz_file=false
            [Color]
            # Make a color on the map. RGB color. Numbers 0-255 including 0,0,255 => blue
            color_road=0,0,255
            # Speed map path color. RGB color. Numbers 0-255 incl. 255,0,0 => red
            color_speed=255,0,0
            # Icon color at the beginning of the line. RGB color. Numbers 0-255 incl. 0,255,75 => green
            color_start=0,255,75
            # Icon color at the end of the line. RGB color. Numbers 0-255 incl. 255,165,0 => orange
            color_end=255,165,0
            # If the GPS connection is lost, the line is drawn in red, here you can set the RGB line color. (Numbers range
            # from 0 to 255 inclusive). The speed_gps setting can change these lines or draw them in a meaningful or strange way.
            color_disrupted=255,0,0
            # Parking icon color. Default RGB number 255,255,255 => original icon color. (Numbers range from 0 to 255 inclusive)
            color_parking=255,255,255
            # Speed start point color. Default forestgreen 34,139,34.
            color_speed_start=0,255,75
            # Speed end point color. Default crimson 220,20,60.
            color_speed_end=255,165,0
            # Free color for the direction icon for the speed KML file. Default is white 255,255,255
            color_speed_direction=255,255,255
            [Other]
            # What time zone to use on the map. Default is UTC. Examples: Europe/Tallinn, Europe/Stockholm, Europe/Paris, UTC.
            # Only one!
            time_zone=Europe/Tallinn
            """;

    /**
     * Generates a commented example INI settings file using predefined default content.
     * <p>
     * This method writes a structured and documented configuration template to the specified file,
     * using UTF-8 encoding. It can be used to help users create or replace their own {@code settings.ini}.
     *
     * @param filename the name of the file to write (e.g., "settings.ini")
     * @throws IOException if an I/O error occurs during writing
     */
    public void generateSettings(String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename, StandardCharsets.UTF_8)) {
            writer.write(EXAMPLE_INI);
        }
    }
}
