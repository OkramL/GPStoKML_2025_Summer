package models;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Model {
    private Settings settings;
    private List<File> files = new ArrayList<>();

    // Different datetime formatters
    private final DateTimeFormatter dateWithMinus = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter dateTimeEstonia = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private final DateTimeFormatter dateWithSlash = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SS");

    public Model(Settings settings) {
        this.settings = settings;
        setValidFiles(); // Reads files requested by the user (example *.txt)
        displayInfo();
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

    // GETTERS

    public DateTimeFormatter getDateTimeEstonia() {
        return dateTimeEstonia;
    }
}
