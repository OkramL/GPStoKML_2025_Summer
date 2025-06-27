package models;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Model {
    private Settings settings;
    private List<File> files = new ArrayList<>();
    private List<DataPoint> dataPoints = new ArrayList<>();

    // Different datetime formatters
    private final DateTimeFormatter dateWithMinus = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter dateTimeEstonia = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private final DateTimeFormatter dateWithSlash = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SS");

    public Model(Settings settings) {
        this.settings = settings;
        setValidFiles(); // Reads files requested by the user (example *.txt)
        displayInfo(); // Show current information on the console

        if(!files.isEmpty()) {
            readFileContents(); // Read file contents
            if(!dataPoints.isEmpty()) {
                List<Description> uniqueDescriptions = getUniqueDescriptions();
                updateDescriptions(uniqueDescriptions);

                // Create settings for KMZ file. Average latitude, longitude, altitude and range. ChatGPT.
                // Data for Kml file to see all lines
                ViewParameters viewParameters = DataPoint.calculateViewParameters(dataPoints);

                switch (settings.getTypeMap()) {

                }

            }
        } else {
            System.err.println("No files found! Interrupting...");
            System.exit(1);
        }
    }

    /**
     * Scans the default folder for files that match the expected file extension and date-based naming convention.
     * Files are matched using a regex pattern based on the configured file type (e.g., .txt, .csv).
     * Valid files are added to the internal list and sorted by name.
     * <p>
     * If the folder is invalid or inaccessible, the program exits.
     */
    private void setValidFiles() {
        File folder = new File(settings.getDefaultFolder());

        if(!folder.exists() || !folder.isDirectory()) {
            System.err.println("Invalid folder " +  settings.getDefaultFolder() + ". Interrupting...");
            System.exit(1);
        }

        // Define regex pattern for data-prifixed filenames
        // This means file names 2024-01-31.txt or 2024-12-31_Tallinn-Pärnu-Tallinn.txt
        String datePattern = "^\\d{4}-\\d{2}-\\d{2}.*" + Pattern.quote(settings.getTypeFile()) + "$";

        Pattern pattern = Pattern.compile(datePattern);
        File[] allFiles = folder.listFiles();

        if(allFiles != null) {
            Arrays.sort(allFiles, Comparator.comparing(File::getName)); // Sort list ascending by name
            for(File file : allFiles) {
                if(file.isFile()) {
                    Matcher matcher = pattern.matcher(file.getName());
                    if(matcher.matches()) {
                        files.add(file); // All necessary GPS log files
                    }
                }
            }
        }
    }

    /**
     * Displays a summary of the current settings and file statistics in a formatted way.
     * Outputs include file type, full path to the folder, selected map type, and total number of matched files.
     */
    private void displayInfo() {
        System.out.println("Current Settings:");
        System.out.printf("%-20s %s%n", "File Type:", settings.getTypeFile());
        System.out.printf("%-20s %s%n", "Folder (Full Path):", new File(settings.getDefaultFolder()).getAbsolutePath());
        System.out.printf("%-20s %s%n", "Map Type:", settings.getTypeMap());
        System.out.printf("%-20s %s%n", "Total Files:",  files.size());
    }

    /**
     * Reads the contents of each valid file based on the configured file type (.gpx, .kml, .txt, .csv, etc.).
     * <p>
     * - For .gpx and .kml files, specific parsing methods are called.
     * - For all other types, lines are read and parsed line-by-line using format-specific logic.
     * <p>
     * The parsed data is stored as DataPoint objects in the model.
     * If an error occurs while reading, a RuntimeException is thrown.
     */
    private void readFileContents() {
        for(File file : files) {
            Map<String, String> metadata = extractFilenameMetaData(file.getName()); // .getName() is without folder

            String date = metadata.get("date");
            String explanation = metadata.get("explanation");
            String description = metadata.get("description");

            if(settings.getTypeFile().equals(".gpx")) {

            } else if(settings.getTypeFile().equals(".kml")) {

            } else { // All other files
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String line;
                    while((line = reader.readLine()) != null) {
                        String[] parts = line.split(",");
                        switch(settings.getTypeFile()) {
                            case ".txt" -> parseTxtLine(parts, date, explanation, description);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Extracts metadata from a file name using the following pattern:
     * {@code YYYY-MM-DD[_explanation][_description].ext}
     * <p>
     * - The date is always required.
     * - Explanation and description are optional.
     * - The description may contain multiple underscore-separated parts.
     *
     * @param name the file name (without folder path)
     * @return a map containing "date", "explanation", and "description" keys (or "error" if pattern is invalid)
     */
    private Map<String, String> extractFilenameMetaData(String name) {
        Map<String, String> metadata = new HashMap<>();
        String[] parts = name.split("\\.")[0].split("_"); // Split on underscores and remove extension
        // 2025-05-02_explanation_description-continues. Assumed variant. Explanation and/or description may be missing.
        // The date must be
        if(parts.length == 1) {
            metadata.put("date", parts[0]);
            metadata.put("explanation", "");
            metadata.put("description", "");
        } else if(parts.length == 2) {
            metadata.put("date", parts[0]);
            if(name.endsWith("_" + settings.getTypeFile())) { // Special case: explanation exists but description is empty
                metadata.put("explanation", parts[1]);
                metadata.put("description", "");
            } else {
                metadata.put("explanation", "");
                metadata.put("description", parts[1]);
            }
        } else if(parts.length >= 3) {
            metadata.put("date", parts[0]);
            metadata.put("explanation", parts[1]);
            metadata.put("description", String.join("_", Arrays.copyOfRange(parts, 2, parts.length)));
        } else {
            metadata.put("error", "File name does not match expected pattern.");
        }
        return metadata;
    }

    /**
     * The necessary information is read from the row of the TXT file and written as a data point
     * Correct txt file old and new camera. The GPS coordinates of the new camera are weird and cannot be used.
     * A,140222,140602.208,+5836.7156,N,+02430.5152,E,11.20,-02.45,-09.19,-78.40; <-- old
     * A,130623,161726.000,5836.8005,N,2430.4872,E,0,+00.00,+00.00,+00.00; <- new
     * .txt datetime is UTC
     *
     * @param parts       a line of a split file
     * @param date        date from filename
     * @param explanation an explanation of the filename between two _ (2023-05-31_explanation_description-no-spaces)
     * @param description a description of the filename
     *
     */
    private void parseTxtLine(String[] parts, String date, String explanation, String description) {
        if(parts[0].equalsIgnoreCase("A")) {
            int year = 2000 + Integer.parseInt(parts[1].substring(4, 6));
            int month = Integer.parseInt(parts[1].substring(2, 4));
            int day = Integer.parseInt(parts[1].substring(0, 2));

            int hour = Integer.parseInt(parts[2].substring(0, 2));
            int minute = Integer.parseInt(parts[2].substring(2, 4));
            int second = Integer.parseInt(parts[2].substring(4, 6));

            String d = String.format("%04d-%02d-%02d", year, month, day);
            String t = String.format("%02d:%02d:%02d", hour, minute, second);
            String datetime = d + " " + t;

            if(isDateTimeValid(datetime)) {
                ZonedDateTime myTime = ZonedDateTime.parse(datetime, dateWithMinus.withZone(ZoneId.of("UTC")));

                double latitude;
                double longitude;

                if(settings.isOldCamera()) { // Old camera Mini 0803
                    // +5834.7842 => 58 + (34.7842/60)
                    // +02430.7068 => +024 + (30.7068/60)
                    latitude = Double.parseDouble(parts[3].substring(0, 3)) + Double.parseDouble(parts[3]
                            .substring(3)) / 60;
                    longitude = Double.parseDouble(parts[5].substring(0, 4)) + Double.parseDouble(parts[5]
                            .substring(4)) / 60;
                } else { // New camera Mini 0805
                    // 5836.5182 => 58 + (36.5182/60)
                    // 2430.3033 => 24 + (30.3033/60)
                    latitude = Double.parseDouble(parts[3].substring(0, 2)) + Double.parseDouble(parts[3]
                            .substring(2)) / 60;
                    longitude = Double.parseDouble(parts[5].substring(0, 2)) + Double.parseDouble(parts[5]
                            .substring(2)) / 60;
                }

                double speed = Double.parseDouble(parts[7]) * 1.852; // knots to km/h

                dataPoints.add(new DataPoint(latitude, longitude, myTime, date, speed, explanation, description,
                        setDayName(date, description)));
            }
        }
    }



    /**
     * Validates whether a string matches the expected datetime format "yyyy-MM-dd HH:mm:ss".
     *
     * @param datetime the string to validate
     * @return {@code true} if the string is a valid datetime; {@code false} otherwise
     */
    private boolean isDateTimeValid(String datetime) {
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        try {
            DateFormat df = new SimpleDateFormat(dateFormat);
            df.setLenient(false);
            df.parse(datetime);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Generates a day name for a data group using the provided date and optional description.
     *
     * @param date        the date string in format "YYYY-MM-DD"
     * @param description an optional description to append
     * @return the combined day name (e.g., "2025-06-01_description") or just the date if description is empty
     */
    private String setDayName(String date, String description) {
        if(!description.isEmpty()) {
            return String.join("_", date, description);
        } else {
            return String.join("_", date);
        }
    }

    /**
     * Finds unique descriptions from data points, considering both date and description.
     * @return List<Description> containing the date, old and new descriptions
     */
    private List<Description> getUniqueDescriptions() {
        Map<String, List<String>> descriptionsByDate = new LinkedHashMap<>();

        for (DataPoint dp : dataPoints) {
            String date = dp.getDate();
            String description = dp.getDescription();
            descriptionsByDate.putIfAbsent(date, new ArrayList<>());

            if (!descriptionsByDate.get(date).contains(description)) {
                descriptionsByDate.get(date).add(description);
            }
        }

        List<Description> uniqueDescriptions = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : descriptionsByDate.entrySet()) {
            String date = entry.getKey();
            List<String> descriptions = entry.getValue();

            String newDescription;
            if (descriptions.isEmpty()) {
                newDescription = "";
            } else if (descriptions.size() == 1 && descriptions.getFirst().isEmpty()) {
                newDescription = "";
            } else if (descriptions.size() == 1) {
                newDescription = descriptions.getFirst();
            } else {
                List<String> combinedList = combineDescriptions(descriptions);

                // Ensure the first and last match appropriately
                String firstPart = descriptions.getFirst().split("-")[0];
                String lastPart = descriptions.getLast().split("-")[descriptions.getLast().split("-").length - 1];

                if (!combinedList.getFirst().equals(firstPart)) {
                    combinedList.addFirst(firstPart);
                }
                if (!combinedList.getLast().equals(lastPart)) {
                    combinedList.add(lastPart);
                }

                newDescription = String.join("-", combinedList);
            }
            // add date to the beginning of the new description
            if(!newDescription.isEmpty()) {
                newDescription = date + "_" + newDescription;
            } else {
                newDescription = date;
            }
            uniqueDescriptions.add(new Description(date, descriptions.isEmpty() ? "" : descriptions.getFirst(), newDescription));
        }
        return uniqueDescriptions;
    }

    /**
     * Combines a list of descriptions into a single list of unique components. Each description in the input list is
     * split into parts using a delimiter ("-"), and duplicate components are removed while preserving the order of
     * their first occurrence.
     *
     * @param descriptions the list of descriptions to combine. Each description is assumed to be a string containing
     *                     parts separated by -.
     * @return a list of unique components extracted from the input descriptions, with duplicates removed and
     * order preserved.
     *
     */
    private List<String> combineDescriptions(List<String> descriptions) {
        LinkedHashSet<String> seenNames = new LinkedHashSet<>();

        for (String description : descriptions) {
            String[] parts = description.split("-");
            Collections.addAll(seenNames, parts);
        }

        return new ArrayList<>(seenNames);
    }

    /**
     * Updates description
     * @param uniques Unique descriptions
     */
    private void updateDescriptions(List<Description> uniques) {
        for(Description description : uniques) {
            for(DataPoint dataPoint : dataPoints) {
                if(dataPoint.getDate().equals(description.getDate())) {
                    dataPoint.setDescription(description.getNewDescription());
                }
            }
        }
    }

    // GETTERS

    public DateTimeFormatter getDateTimeEstonia() {
        return dateTimeEstonia;
    }
}
