package models;

import java.time.format.DateTimeFormatter;

public class Model {
    private Settings settings;

    // Different datetime formatters
    private final DateTimeFormatter dateWithMinus = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter dateTimeEstonia = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private final DateTimeFormatter dateWithSlash = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SS");

    public Model(Settings settings) {
        this.settings = settings;

    }
}
