package models;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The KmzGenerator class is responsible for generating a KMZ (zipped KML) file structure.
 * It creates a KML document with camera viewpoints and links to map/speed overlays,
 * then packages the KML and required images into a KMZ archive. This need KmlGenerator.
 */
public class KmzGenerator {
    private final Model model;
    private final Settings settings;
    private final ViewParameters  viewParameters;
    private final String docFile = "doc.kml";

    /**
     * Constructs a KmzGenerator object and initiates creation of the KML and KMZ files.
     *
     * @param settings        configuration object containing file names and options
     * @param model           data model holding image icon paths
     * @param viewParameters  viewpoint settings for camera look-at parameters
     */
    public KmzGenerator(Settings settings, Model model, ViewParameters viewParameters) {
        this.settings = settings;
        this.model = model;
        this.viewParameters = viewParameters;

        createDoc();
        createKmz();
    }

    /**
     * Creates the primary KML document (doc.kml) with camera viewpoint and links
     * to other KML layers (map/speed), depending on the selected type in settings.
     */
    private void createDoc() {
        String header = """
                <?xml version="1.0" encoding="UTF-8"?>
                <kml xmlns="http://www.opengis.net/kml/2.2">
                    <Document>
                        <LookAt>
                          <longitude>%s</longitude> <!-- The center of Estonia 25.0136 -->
                          <latitude>%s</latitude>   <!-- The center of Estonia 58.5953 -->
                          <altitude>%s</altitude>    <!-- Height 150 km => 150000 -->
                          <range>%s</range>          <!-- Distance 150 km => 150000 -->
                          <tilt>0</tilt>                 <!-- Camera vertical angle -->
                          <heading>0</heading>           <!-- Camera direction north (0 degrees) -->
                        </LookAt>
                        <name>%s</name>
                        <description>%s</description>
                """;

        String content = """
                    \t\t<NetworkLink>
                    \t\t\t<name>%s</name>
                    \t\t\t<description>%s</description>
                    \t\t\t<Link>
                    \t\t\t\t<href>kml/%s</href>
                    \t\t\t</Link>
                    \t\t</NetworkLink>
                """;

        String filePath = String.valueOf(Paths.get(settings.getSourcePath(),  docFile));
        String[] names = new String[]{"Map", "Speed"};

        String[] description = new String[]{
                String.format("Draw lines to map. File type: %s", settings.getTypeFile()),
                String.format("Speed %s km/h or more. File type: %s", settings.getSpeedMap(), settings.getTypeFile())
        };

        String[] twoFiles = new String[]{settings.getFileMap(), settings.getFileSpeed()};

        try {
            PrintWriter pw = new PrintWriter(filePath, StandardCharsets.UTF_8);
            pw.printf(header, viewParameters.longitude, viewParameters.latitude,  viewParameters.altitude,
                    viewParameters.range, "GPS to KML (May 2025)", "One or more different contents");
            switch (settings.getTypeMap()) {
                case "both" -> {
                    for(int x = 0; x < names.length; x++) {
                        pw.printf(content, names[x], description[x], twoFiles[x]);
                    }
                }
                case "map" -> pw.printf(content, names[0], description[0], twoFiles[0]);
                case "speed" -> pw.printf(content, names[1], description[1], twoFiles[1]);
            }

            String footer = """
                        </Document>
                    </kml>
                    """;

            pw.println(footer);
            pw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds the KMZ file by including the generated doc.kml, optional map/speed KML files,
     * and icon images. The contents vary based on the selected map type in settings.
     * If required files are missing, the method prints an error and terminates the program.
     */
    private void createKmz() {
        String folderKml = "kml/";      // Here comes one or two files
        String folderFiles = "files/";  // Here comes an images
        Path doc = Paths.get(docFile);
        Path map = Paths.get(settings.getFileMap());
        Path speed = Paths.get(settings.getFileSpeed());

        String[] images = {model.getIconStart(), model.getIconEnd(), model.getIconParking(), model.getIconDirection()};

        // Zip (.kml) file
        Path zipFile = Paths.get(settings.getFileKmz());

        switch(settings.getTypeMap()) {
            case "both" -> {
                if(Files.exists(doc) && Files.exists(map) && Files.exists(speed)) {
                    // Try to write contents
                    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
                        // doc.kml file
                        addToZipFile(doc, zos, doc.getFileName().toString());

                        // add CameraMap.kml and CameraSpeed.kml file
                        addToZipFile(map, zos, folderKml + map.getFileName().toString());
                        addToZipFile(speed, zos, folderKml + speed.getFileName().toString());

                        // Add images
                        for(String image: images) {
                            addToZipFile(image, zos, folderFiles + image);
                        }

                        // Remove no needed files
                        doc.toFile().deleteOnExit();
                        map.toFile().deleteOnExit();
                        speed.toFile().deleteOnExit();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    System.err.println("One or more files are missing. Interrupting...");
                    System.exit(1);
                }
            }
            case "map" -> {
                if(Files.exists(doc) && Files.exists(map)) {
                    // Try to write contents
                    createZipFile(folderKml, folderFiles, doc, map, images, zipFile, map.getFileName());
                } else {
                    System.err.println("File (" + settings.getFileMap() + ") are missing. Interrupting...");
                    System.exit(1);
                }
            }
            case "speed" -> {
                if(Files.exists(doc) && Files.exists(speed)) {
                    // Try to write contents
                    createZipFile(folderKml, folderFiles, doc, speed, images, zipFile, map.getFileName());
                } else {
                    System.err.println("File (" + settings.getFileSpeed() + ") are missing. Interrupting...");
                    System.exit(1);
                }
            }
        }
    }

    /**
     * Helper method to package files into a KMZ (ZIP) archive.
     * Used by the createKmz method to reduce code duplication.
     *
     * @param folderKml     the subfolder inside KMZ where KML files are placed
     * @param folderFiles   the subfolder inside KMZ for icon image files
     * @param doc           the main KML file (doc.kml)
     * @param map           the secondary KML file (map or speed)
     * @param images        array of image paths to include in the KMZ
     * @param zipFile       path to the output KMZ file
     * @param fileName      the name to use for the map/speed KML inside the ZIP
     */
    private void createZipFile(String folderKml, String folderFiles, Path doc, Path map, String[] images, Path zipFile, Path fileName) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toString()))) {
            // doc.kml file
            addToZipFile(doc, zos, doc.getFileName().toString());
            // CameraMap.kml
            addToZipFile(map, zos, folderKml + fileName.toString());
            // Add images
            for(String image: images) {
                addToZipFile(image, zos, folderFiles + image);
            }
            // Remove no needed files
            doc.toFile().deleteOnExit();
            map.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a resource from inside the JAR (like an icon) to the ZipOutputStream.
     *
     * @param resourcePath   the path to the resource (e.g. "/a.png")
     * @param zos            the ZipOutputStream to write into
     * @param zipEntryName   the name of the entry in the ZIP
     */
    private void addToZipFile(String resourcePath, ZipOutputStream zos, String zipEntryName) {

        try (InputStream is = ClassLoader.getSystemResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }

            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a file to the ZipOutputStream with the given entry name.
     *
     * @param file          the source file to be added to the ZIP
     * @param zos           the ZipOutputStream to write into
     * @param zipEntryName  the path/name to use inside the ZIP archive
     */
    private void addToZipFile(Path file, ZipOutputStream zos, String zipEntryName) {
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
