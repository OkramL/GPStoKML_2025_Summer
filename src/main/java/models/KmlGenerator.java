package models;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates KML (Keyhole Markup Language) files based on the provided model and settings.
 * <p>
 * The generated KML includes styled lines and icons representing movement, speed, and
 * additional metadata such as parking locations and directional markers.
 * <p>
 * This class supports various KML configurations depending on user-selected map types
 * (e.g., road map, speed map, disrupted paths).
 */
public class KmlGenerator {
    private final String LINE_STYLE_SPEED = "lineStyleSpeed";
    private final String LINE_STYLE_DISRUPTED = "lineStyleDisrupted";
    private final String ICON_STYLE_START = "iconStyleStart";
    private final String ICON_STYLE_END = "iconStyleEnd";
    private final String ICON_STYLE_PARKING = "iconStyleParking";
    private final String ICON_STYLE_SPEED_START = "iconStyleSpeedStart";
    private final String ICON_STYLE_SPEED_END = "iconStyleSpeedEnd";

    private final Settings settings;
    private final Model model;

    /**
     * Constructs a new {@code KmlGenerator} with the specified settings and data model.
     *
     * @param settings the settings object containing configuration for the KML generation
     * @param model    the model object containing parsed data points and file metadata
     */
    public KmlGenerator(Settings settings, Model model) {
        this.settings = settings;
        this.model = model;
    }

    /**
     * Builds the entire KML document from the loaded data and writes it to a file.
     * Groups data by month and day, adds visual styles, and creates structured KML output.
     */
    public void build() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Create KML root element
            Element kml = doc.createElement("kml");
            kml.setAttribute("xmlns", "http://www.opengis.net/kml/2.2");
            doc.appendChild(kml);

            // Let's create a Document element
            Element documentElement = doc.createElement("Document");
            kml.appendChild(documentElement);

            // Let's create and add a name element
            Element elementName = doc.createElement("name");
            elementName.setTextContent("GPS to KML Map (June 2025)");
            documentElement.appendChild(elementName);

            // Let's create and add a description element
            Element descriptionElement = doc.createElement("description");
            descriptionElement.setTextContent("Draws lines on the map. File type " + settings.getTypeFile());
            documentElement.appendChild(descriptionElement);

            // Create line and icon styles
            String iconFolder = (settings.isKmzFile() ? "../" : "") + "files/"; // files/image.png or ../files/image.png
            String LINE_STYLE_ROAD = "lineStyleRoad";
            addLineStyle(doc, documentElement, LINE_STYLE_ROAD, settings.rgbToKmlHex(settings.getColorRoad(), 100), settings.getLineWidth());
            addLineStyle(doc, documentElement, LINE_STYLE_DISRUPTED, settings.rgbToKmlHex(settings.getColorDisrupted(), 100), settings.getLineWidth());
            addIconStyle(doc, documentElement, ICON_STYLE_START, iconFolder + model.getIconStart(), settings.rgbToKmlHex(settings.getColorStart(), 100), 1.5);
            addIconStyle(doc, documentElement, ICON_STYLE_END, iconFolder + model.getIconEnd(), settings.rgbToKmlHex(settings.getColorEnd(), 100), 1.5);
            addIconStyle(doc, documentElement, ICON_STYLE_PARKING, iconFolder + model.getIconParking(), settings.rgbToKmlHex(settings.getColorParking(), 100), 1.5);
            String ICON_STYLE_DIRECTION = "iconStyleDirection";
            addIconStyle(doc, documentElement, ICON_STYLE_DIRECTION, iconFolder + model.getIconDirection(), settings.rgbToKmlHex(settings.getColorSpeedDirection(), 100), settings.getDirectionScale());

            // This is where the line-related stuff starts, including making folders.
            Set<String> uniqueMonths = getUniqueMonths(model.getDataPoints()); // get unique YYYY-MM from dataPoints
            for(String month : uniqueMonths) {
                // Create a folder for each month
                Element folder = doc.createElement("Folder");
                documentElement.appendChild(folder);

                // Create a name of YYYY-MM
                Element name = doc.createElement("name");
                name.setTextContent(month);
                folder.appendChild(name);

                // Group DataPoints by day and optionally by explanation if settings.isFileMerge() is false
                Map<String, List<DataPoint>> groupedByDay = groupDataPointsByDay(month, model.getDataPoints());
                for(Map.Entry<String, List<DataPoint>> entry : groupedByDay.entrySet()) {
                    String description = entry.getKey();
                    List<DataPoint> value = entry.getValue();

                    // Create folder for each day
                    Element dayFolder = doc.createElement("Folder");
                    folder.appendChild(dayFolder);

                    Element dayName = doc.createElement("name");
                    if(settings.isFileMerge()) {
                        dayName.setTextContent(description); // Mixed together
                    } else {
                        dayName.setTextContent(value.getFirst().getDayName()); // Original file name (dayName)
                    }
                    dayFolder.appendChild(dayName);
                    drawDayLines(doc, dayFolder, entry.getValue());
                }
            }

            // Write a document to a file with special settings
            String outputFilePath = settings.getFileMap(); // Specifies the location and name of the file to be saved.
            TransformerFactory transformerFactory = TransformerFactory.newInstance(); // This is the first step in converting an XML object from the DOM to real output.
            Transformer transformer = transformerFactory.newTransformer(); // XML generation rules are configured and the conversion process is initiated.
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // Ensures that the output XML is indented.
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // Here 4 spaces are used for one level.

            DOMSource source = new DOMSource(doc); // Gives the Transformer input with the DOM structure to be written.
            StreamResult result = new StreamResult(new File(outputFilePath)); // Specifies that the XML is converted and saved to a physical file.
            transformer.transform(source, result); // Runs a transformation from DOM (source) to file (result).

            // System.out.println("KML file written to " + outputFilePath); // The user is given feedback on where the file was saved.

        } catch (ParserConfigurationException | TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a KML file that highlights segments where the speed meets or exceeds the defined threshold.
     * <p>
     * For each day, segments with sufficient speed are grouped and drawn as "Speed Lines".
     * If enabled in settings, the start, end, and direction icons are also added to a separate
     * "Speed Markers" folder, which is appended at the end of each day’s folder.
     * <p>
     * Icon visibility is controlled via user settings:
     * <ul>
     *   <li>{@code isKmSign()} determines whether icons are created.</li>
     *   <li>{@code isKmSignVisibility()} controls whether icons are shown by default.</li>
     * </ul>
     *
     * Icon appearance (color and scale) is also configured via user settings.
     * The output file path is defined by {@code settings.getFileSpeed()}.
     */
    public void speed() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // KML root
            Element kml = doc.createElement("kml");
            kml.setAttribute("xmlns", "http://www.opengis.net/kml/2.2");
            doc.appendChild(kml);

            // Document block
            Element documentElement = doc.createElement("Document");
            kml.appendChild(documentElement);

            // Name and description
            Element nameElement = doc.createElement("name");
            nameElement.setTextContent("GPS to KML Speed (May 2025)");
            documentElement.appendChild(nameElement);

            Element descriptionElement = doc.createElement("description");
            descriptionElement.setTextContent("This KML shows only the segments where speed is at least " + settings.getSpeedMap() + " km/h.");
            documentElement.appendChild(descriptionElement);

            // Add all style definitions
            String iconFolder = (settings.isKmzFile() ? "../" : "") + "files/"; // files/image.png or ../files/image.png
            addLineStyle(doc, documentElement, LINE_STYLE_SPEED, settings.rgbToKmlHex(settings.getColorSpeed(), 100), settings.getLineWidth());
            addIconStyle(doc, documentElement, ICON_STYLE_START, iconFolder + model.getIconStart(), settings.rgbToKmlHex(settings.getColorStart(), 100), 1.5);
            addIconStyle(doc, documentElement, ICON_STYLE_END, iconFolder + model.getIconEnd(), settings.rgbToKmlHex(settings.getColorEnd(), 100), 1.5);
            addIconStyle(doc, documentElement, ICON_STYLE_SPEED_START, iconFolder + model.getIconStart(), settings.rgbToKmlHex(settings.getColorSpeedStart(), 100), settings.getDirectionScale());
            addIconStyle(doc, documentElement, ICON_STYLE_SPEED_END,iconFolder + model.getIconEnd(), settings.rgbToKmlHex(settings.getColorSpeedEnd(), 100), settings.getDirectionScale());
            String ICON_STYLE_SPEED_DIRECTION = "iconStyleSpeedDirection";
            addIconStyle(doc, documentElement, ICON_STYLE_SPEED_DIRECTION, iconFolder + model.getIconDirection(), settings.rgbToKmlHex(settings.getColorSpeedDirection(), 100), settings.getDirectionScale());

            // Group and process data by month and day
            Set<String> uniqueMonths = getUniqueMonths(model.getDataPoints());
            for (String month : uniqueMonths) {
                Element monthFolder = doc.createElement("Folder");
                documentElement.appendChild(monthFolder);

                Element monthName = doc.createElement("name");
                monthName.setTextContent(month);
                monthFolder.appendChild(monthName);

                Map<String, List<DataPoint>> groupedByDay = groupDataPointsByDay(month, model.getDataPoints());
                for (Map.Entry<String, List<DataPoint>> entry : groupedByDay.entrySet()) {
                    Element dayFolder = doc.createElement("Folder");
                    monthFolder.appendChild(dayFolder);

                    Element dayName = doc.createElement("name");
                    if (settings.isFileMerge()) {
                        dayName.setTextContent(entry.getKey());
                    } else {
                        dayName.setTextContent(entry.getValue().getFirst().getDayName());
                    }
                    dayFolder.appendChild(dayName);

                    drawSpeedLinesByDay(doc, dayFolder, entry.getValue());
                }
            }

            // Save KML to file
            String outputFilePath = settings.getFileSpeed();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(outputFilePath));
            transformer.transform(source, result);

            //System.out.println("Speed KML file written to " + outputFilePath);
        } catch (TransformerException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a line style to the KML document.
     *
     * @param doc    The KML document to which the style will be added.
     * @param parent The element to which the style belongs.
     * @param id     The unique ID of the style.
     * @param color  The line color (in KML format AABBGGRR).
     * @param width  The line width in pixels.
     */
    public void addLineStyle(Document doc, Element parent, String id, String color, double width) {
        // Create a Style element
        Element style = doc.createElement("Style");
        style.setAttribute("id", id);
        parent.appendChild(style);

        // Create a LineStyle element
        Element lineStyle = doc.createElement("LineStyle");
        style.appendChild(lineStyle);

        // Set the color of the line
        Element colorElement = doc.createElement("color");
        colorElement.setTextContent(color); // KML format: AABBGGRR (alpha, blue, green, red)
        lineStyle.appendChild(colorElement);

        // Set the width of the line
        Element widthElement = doc.createElement("width");
        widthElement.setTextContent(String.valueOf(width));
        lineStyle.appendChild(widthElement);
    }

    /**
     * Adds an icon style to the KML document.
     *
     * @param doc      The KML document to which the style will be added.
     * @param parent   The element to which the style belongs.
     * @param id       The unique ID of the style.
     * @param iconHref The icon's URL or path.
     * @param color    The icon color (in KML format AABBGGRR).
     * @param scale    The scaling factor for the icon (enlargement or reduction).
     */
    public void addIconStyle(Document doc, Element parent, String id, String iconHref, String color, double scale) {
        // Create a Style element
        Element style = doc.createElement("Style");
        style.setAttribute("id", id);
        parent.appendChild(style);

        // Create an IconStyle element
        Element iconStyle = doc.createElement("IconStyle");
        style.appendChild(iconStyle);

        // Set the color of the icon
        Element colorElement = doc.createElement("color");
        colorElement.setTextContent(color); // KML format: AABBGGRR
        iconStyle.appendChild(colorElement);

        // Set the scale of the icon
        Element scaleElement = doc.createElement("scale");
        scaleElement.setTextContent(String.valueOf(scale));
        iconStyle.appendChild(scaleElement);

        // Set the icon href (link to the icon image)
        Element icon = doc.createElement("Icon");
        iconStyle.appendChild(icon);

        Element href = doc.createElement("href");
        href.setTextContent(iconHref); // URL or local path to the icon image
        icon.appendChild(href);
    }

    /**
     * Adds an icon element to the KML document for a data point.
     *
     * @param doc     The KML document to which the icon will be added.
     * @param parent  The element to which the icon belongs.
     * @param point   The data point corresponding to the icon.
     * @param styleId The ID of the style to be used.
     * @param name    The name of the icon.
     */
    private void addIconPlaceMark(Document doc, Element parent, DataPoint point, String styleId, String name) {
        Element placemark = doc.createElement("Placemark");
        parent.appendChild(placemark);

        Element placemarkName = doc.createElement("name");
        placemarkName.setTextContent(name);
        placemark.appendChild(placemarkName);

        Element style = doc.createElement("styleUrl");
        style.setTextContent("#" + styleId);
        placemark.appendChild(style);

        Element description = doc.createElement("description");
        description.setTextContent(name + " " + convertUtcToLocal(point.getPointTime()));
        placemark.appendChild(description);

        Element pointElement = doc.createElement("Point");
        placemark.appendChild(pointElement);

        Element coordinates = doc.createElement("coordinates");
        coordinates.setTextContent(point.getLongitude() + "," + point.getLatitude());
        pointElement.appendChild(coordinates);
    }

    /**
     * Creates a placemark element representing a segment line on the map.
     * Includes metadata such as start/end times, coordinates, and total distance.
     *
     * @param doc       The KML document being constructed.
     * @param parent    The parent element to which the placemark will be added.
     * @param nameText  The name of the placemark.
     * @param segment   The segment containing coordinate points and metadata.
     */
    private void createLinePlaceMark(Document doc, Element parent, String nameText, Segment segment) {
        Element placeMark = doc.createElement("Placemark");
        parent.appendChild(placeMark);

        Element name = doc.createElement("name");
        name.setTextContent(nameText);
        placeMark.appendChild(name);

        Element description = doc.createElement("description");

        // Calculate segment details
        CoordinatePoint startPoint = segment.getPoints().getFirst();
        CoordinatePoint endPoint = segment.getPoints().getLast();

        String startTime = convertUtcToLocal(startPoint.pointTime());
        String endTime = convertUtcToLocal(endPoint.pointTime());
        String formattedTime = calculateTime(startPoint.pointTime(), endPoint.pointTime());

        double totalDistance = 0.0;
        for (int i = 1; i < segment.getPoints().size(); i++) {
            CoordinatePoint prev = segment.getPoints().get(i - 1);
            CoordinatePoint current = segment.getPoints().get(i);
            totalDistance += calculateDistance(prev.latitude(), prev.longitude(), current.latitude(), current.longitude());
        }

        description.appendChild(doc.createCDATASection(String.format("<b>Start:</b> %s<br>" +
                        "<b>End:</b> %s<br>" +
                        "<b>Length:</b> %s<br>" +
                        "<b>Start Coordinates:</b> %f, %f<br>" +
                        "<b>End Coordinates:</b> %f, %f<br>" +
                        "<b>Distance:</b> %.2f km",
                startTime, endTime, formattedTime,
                startPoint.latitude(), startPoint.longitude(),
                endPoint.latitude(), endPoint.longitude(),
                totalDistance
        )));
        placeMark.appendChild(description);

        Element styleUrl = doc.createElement("styleUrl");
        styleUrl.setTextContent("#" + segment.getStyleId());
        placeMark.appendChild(styleUrl);

        Element lineString = doc.createElement("LineString");
        placeMark.appendChild(lineString);

        Element coordinates = doc.createElement("coordinates");
        StringBuilder coordBuilder = new StringBuilder();
        for (CoordinatePoint point : segment.getPoints()) {
            coordBuilder.append(point.longitude()).append(",")
                    .append(point.latitude()).append(",0 ");
        }
        coordinates.setTextContent(coordBuilder.toString().trim());
        lineString.appendChild(coordinates);
    }

    /**
     * Creates a KML Placemark element with an icon and style based on the provided parameters.
     *
     * @param doc      The KML document.
     * @param lat      Latitude coordinate.
     * @param lon      Longitude coordinate.
     * @param nameText The name of the placemark.
     * @param styleId  The style ID to use for the icon.
     * @return The created Placemark element.
     */
    private Element createIconPlacemark(Document doc, double lat, double lon, String nameText, String styleId) {
        Element placemark = doc.createElement("Placemark");

        Element name = doc.createElement("name");
        name.setTextContent(nameText);
        placemark.appendChild(name);

        applyVisibility(placemark);

        Element styleUrl = doc.createElement("styleUrl");
        styleUrl.setTextContent("#" + styleId);
        placemark.appendChild(styleUrl);

        Element point = doc.createElement("Point");
        placemark.appendChild(point);

        Element coordinates = doc.createElement("coordinates");
        coordinates.setTextContent(lon + "," + lat);
        point.appendChild(coordinates);

        return placemark;
    }

    /**
     * Creates a KML Placemark with a directional heading arrow at the specified point.
     *
     * @param doc     The KML document.
     * @param point   The coordinate point where the arrow is placed.
     * @param heading The compass heading angle in degrees.
     * @return The created Placemark element.
     */
    private Element createDirectionMarker(Document doc, CoordinatePoint point, double heading) {
        Element placemark = doc.createElement("Placemark");

        // Name
        Element name = doc.createElement("name");
        name.setTextContent("Speed Direction");
        placemark.appendChild(name);

        applyVisibility(placemark);

        // Inline Style (we are using heading, so we cannot use styleUrl)
        Element style = doc.createElement("Style");
        placemark.appendChild(style);

        Element iconStyle = doc.createElement("IconStyle");
        style.appendChild(iconStyle);

        // Heading
        Element headingElement = doc.createElement("heading");
        headingElement.setTextContent(String.format(Locale.US, "%.1f", heading));
        iconStyle.appendChild(headingElement);

        // Ikooni asukoht
        Element icon = doc.createElement("Icon");
        iconStyle.appendChild(icon);

        Element href = doc.createElement("href");
        String iconFolder = (settings.isKmzFile() ? "../" : "") + "files/";
        href.setTextContent(iconFolder + model.getIconDirection());
        icon.appendChild(href);

        // (Add scale and hotspot if desired)
        Element scale = doc.createElement("scale");
        scale.setTextContent(String.valueOf(settings.getDirectionScale()));
        iconStyle.appendChild(scale);

        Element hotSpot = doc.createElement("hotSpot");
        hotSpot.setAttribute("x", "0.5");
        hotSpot.setAttribute("y", "0.5");
        hotSpot.setAttribute("xunits", "fraction");
        hotSpot.setAttribute("yunits", "fraction");
        iconStyle.appendChild(hotSpot);

        // Coordinates
        Element pointElement = doc.createElement("Point");
        placemark.appendChild(pointElement);

        Element coordinates = doc.createElement("coordinates");
        coordinates.setTextContent(point.longitude() + "," + point.latitude());
        pointElement.appendChild(coordinates);

        return placemark;
    }

    /**
     * Adds a placemark representing a disrupted segment (e.g., GPS signal lost or unrealistic jump).
     *
     * @param doc        The KML document being constructed.
     * @param parent     The parent element to which the disrupted line will be added.
     * @param startPoint The starting point of the disruption.
     * @param endPoint   The ending point of the disruption.
     */
    private void addDisruptedLine(Document doc, Element parent, DataPoint startPoint, DataPoint endPoint) {
        Element placeMark = doc.createElement("Placemark");
        parent.appendChild(placeMark);

        Element name = doc.createElement("name");
        name.setTextContent("Disruption");
        placeMark.appendChild(name);

        Element description = doc.createElement("description");
        String startTime = convertUtcToLocal(startPoint.getPointTime());
        String endTime = convertUtcToLocal(endPoint.getPointTime());
        description.appendChild(doc.createCDATASection(String.format(
                "<b>Start:</b> %s<br><b>End:</b> %s<br><b>Start Coordinates:</b> %f, %f<br><b>End Coordinates:</b> %f, %f",
                startTime, endTime,
                startPoint.getLatitude(), startPoint.getLongitude(),
                endPoint.getLatitude(), endPoint.getLongitude()
        )));
        placeMark.appendChild(description);

        Element styleUrl = doc.createElement("styleUrl");
        styleUrl.setTextContent("#" + LINE_STYLE_DISRUPTED);
        placeMark.appendChild(styleUrl);

        Element lineString = doc.createElement("LineString");
        placeMark.appendChild(lineString);

        Element coordinates = doc.createElement("coordinates");
        coordinates.setTextContent(String.format("%s,%s %s,%s",
                startPoint.getLongitude(), startPoint.getLatitude(),
                endPoint.getLongitude(), endPoint.getLatitude()));
        lineString.appendChild(coordinates);
    }

    /**
     * Adds a placemark representing a parking event (stop longer than the configured time).
     *
     * @param doc        The KML document being constructed.
     * @param parent     The parent element to which the placemark will be added.
     * @param startTime  The time when the stop started.
     * @param endTime    The time when the stop ended.
     * @param point      The location where the stop occurred.
     */
    private void addParkingPlaceMark(Document doc, Element parent, ZonedDateTime startTime, ZonedDateTime endTime, DataPoint point) {
        Element placeMark = doc.createElement("Placemark");
        parent.appendChild(placeMark);

        // Name
        Element nameElement = doc.createElement("name");
        nameElement.setTextContent("Parking");
        placeMark.appendChild(nameElement);

        // Description
        String formattedDuration = calculateTime(startTime, endTime);

        String start = convertUtcToLocal(startTime);
        String end = convertUtcToLocal(endTime);

        Element descriptionElement = doc.createElement("description");
        descriptionElement.appendChild(doc.createCDATASection(String.format("Start: <b>%s</b><br>End: <b>%s</b><br>Length: <b>%s</b>",
                start,
                end,
                formattedDuration)));
        placeMark.appendChild(descriptionElement);

        // Style
        Element styleUrl = doc.createElement("styleUrl");
        styleUrl.setTextContent("#" + ICON_STYLE_PARKING);
        placeMark.appendChild(styleUrl);

        // Coordinates
        Element pointElement = doc.createElement("Point");
        placeMark.appendChild(pointElement);

        Element coordinatesElement = doc.createElement("coordinates");
        coordinatesElement.setTextContent(point.getLongitude() + "," + point.getLatitude());
        pointElement.appendChild(coordinatesElement);
    }

    /**
     * Adds a detailed kilometer post to the KML, including icon, direction, and local time.
     *
     * @param doc The KML document to which the post is added.
     * @param parent The folder to which the kilometer post belongs.
     * @param post The data associated with the kilometer post.
     */
    private void addKmPlaceMark(Document doc, Element parent, KmPost post) {
        Element placeMark = doc.createElement("Placemark");
        parent.appendChild(placeMark);

        // Name
        Element name = doc.createElement("name");
        name.setTextContent(String.format("%.2f", post.kmNumber()) + " km."); // Ümarda kaks kohta peale koma.
        placeMark.appendChild(name);

        // Visibility
        Element visibility = doc.createElement("visibility");
        visibility.setTextContent(settings.isKmSignVisibility() ? "1" : "0");
        placeMark.appendChild(visibility);

        // Description
        ZoneId timeZoneId = settings.getTimeZone().toZoneId();
        ZonedDateTime localTime = post.dataPoint().getPointTime().withZoneSameInstant(timeZoneId);
        DateTimeFormatter formatter = model.getDateTimeEstonia();

        Element description = doc.createElement("description");
        description.appendChild(doc.createCDATASection(String.format(
                "The direction on the <b>%.2f</b> kilometer post is <b>%.3f</b> degrees. Local time: <b>%s</b>",
                post.kmNumber(),
                post.direction(),
                localTime.format(formatter)
        )));
        placeMark.appendChild(description);

        // Point
        Element point = doc.createElement("Point");
        placeMark.appendChild(point);

        Element coordinates = doc.createElement("coordinates");
        coordinates.setTextContent(post.dataPoint().getLongitude() + "," + post.dataPoint().getLatitude());
        point.appendChild(coordinates);

        // Style
        Element style = doc.createElement("Style");
        placeMark.appendChild(style);

        Element iconStyle = doc.createElement("IconStyle");
        style.appendChild(iconStyle);

        Element icon = doc.createElement("Icon");
        iconStyle.appendChild(icon);

        Element href = doc.createElement("href");
        String iconFolder = (settings.isKmzFile() ? "../" : "") + "files/"; // files/image.png or ../files/image.png
        href.setTextContent(iconFolder + model.getIconDirection());
        icon.appendChild(href);

        Element scale = doc.createElement("scale");
        scale.setTextContent("1.0");
        iconStyle.appendChild(scale);

        Element color = doc.createElement("color");
        color.setTextContent("FFFFFFFF");
        iconStyle.appendChild(color);

        Element heading = doc.createElement("heading");
        heading.setTextContent(String.valueOf(post.direction()));
        iconStyle.appendChild(heading);
    }

    /**
     * Extracts unique months (YYYY-MM) from a list of data points.
     *
     * @return Unique months, preserving the original order.
     */
    public Set<String> getUniqueMonths(List<DataPoint> dataPoints) {
        return dataPoints.stream()
                .map(dataPoint -> dataPoint.getDate().substring(0, 7)) // Extract YYYY-MM
                .collect(Collectors.toCollection(LinkedHashSet::new)); // Maintain order
    }

    /**
     * Groups data points by day, optionally adding explanations.
     *
     * @param month The month (in YYYY-MM format) for which the data points are grouped.
     * @return Data points grouped by day.
     */
    private Map<String, List<DataPoint>> groupDataPointsByDay(String month, List<DataPoint> dataPoints) {
        return dataPoints.stream()
                .filter(dataPoint -> dataPoint.getDate().startsWith(month))
                .collect(Collectors.groupingBy(
                        dataPoint -> {
                            String baseDescription = dataPoint.getDescription();
                            if (!settings.isFileMerge() && !dataPoint.getExplanation().isEmpty()) {
                                return baseDescription + "_" + dataPoint.getExplanation();
                            }
                            return baseDescription;
                        },
                        LinkedHashMap::new, // Preserve order
                        Collectors.toList()
                ));
    }

    /**
     * Processes all data points for a specific day, grouping them into segments.
     * Handles normal movement, disruptions, and parking events, and creates
     * corresponding placemarks in the KML structure.
     *
     * @param doc         The KML document being constructed.
     * @param dayFolder   The folder element where all day-related elements are added.
     * @param dataPoints  List of data points recorded on a single day.
     */
    private void drawDayLines(Document doc, Element dayFolder, List<DataPoint> dataPoints) {
        if(dataPoints == null || dataPoints.isEmpty()) return;

        // Add start of day icon
        addIconPlaceMark(doc, dayFolder, dataPoints.getFirst(), ICON_STYLE_START, "Start");

        List<Segment> segments = new ArrayList<>(); // One type of lines (normal running, interruption, running after a stop)
        List<CoordinatePoint> currentSegmentPoints = new ArrayList<>();

        DataPoint previousPoint = null;
        double totalDistance = 0.0;
        double kmCounter = settings.getKmSteps();
        List<KmPost> kmPosts = new ArrayList<>();

        for(int i = 0; i < dataPoints.size(); i++) {
            DataPoint currentPoint = dataPoints.get(i);

            if(previousPoint != null) {
                double distance = calculateDistance(currentPoint.getLatitude(), currentPoint.getLongitude(),
                        previousPoint.getLatitude(), previousPoint.getLongitude());
                Duration timeDifference = Duration.between(previousPoint.getPointTime(), currentPoint.getPointTime());

                boolean disruptionOccurred = false;
                boolean stopOccurred = false;

                // Handle disruptions
                if(distance > settings.getMaxDistance()) {
                    if(!currentSegmentPoints.isEmpty()) {
                        segments.add(new Segment(new ArrayList<>(currentSegmentPoints), "Normal Line"));
                        createLinePlaceMark(doc, dayFolder, "Normal Line", segments.removeLast());
                    }
                    currentSegmentPoints.clear();

                    // This block is only executed once per disruption.
                    // 'disruptionOccurred' is set to true after the first call.
                    if(!disruptionOccurred) {
                        addDisruptedLine(doc, dayFolder, previousPoint, currentPoint);
                        disruptionOccurred = true;
                    }
                }

                // Handle stops
                if (timeDifference.toMinutes() >= settings.getStopMinutes()) {
                    if (!currentSegmentPoints.isEmpty()) {
                        segments.add(new Segment(new ArrayList<>(currentSegmentPoints), "Normal Line"));
                        createLinePlaceMark(doc, dayFolder, "Normal Line", segments.removeLast());
                    }
                    currentSegmentPoints.clear();

                    if (!stopOccurred) {
                        addParkingPlaceMark(doc, dayFolder, previousPoint.getPointTime(), currentPoint.getPointTime(), previousPoint);
                        stopOccurred = true;
                    }
                }
                // Handle normal movement
                if (distance <= settings.getMaxDistance() && timeDifference.toMinutes() < settings.getStopMinutes()) {
                    totalDistance += distance;

                    // Handle kilometer posts
                    if (settings.isKmSign() && totalDistance >= kmCounter) {
                        double direction = bearingInDegrees(previousPoint, currentPoint);
                        kmPosts.add(new KmPost(currentPoint, totalDistance, direction));
                        kmCounter += settings.getKmSteps();
                    }

                    currentSegmentPoints.add(new CoordinatePoint(currentPoint.getLongitude(), currentPoint.getLatitude(), currentPoint.getPointTime()));
                }
            } else {
                // Start the first segment
                currentSegmentPoints.add(new CoordinatePoint(currentPoint.getLongitude(), currentPoint.getLatitude(), currentPoint.getPointTime()));
            }
            previousPoint = currentPoint;
        }

        // Finalize the last segment immediately
        if (!currentSegmentPoints.isEmpty()) {
            createLinePlaceMark(doc, dayFolder, "Normal Line", new Segment(currentSegmentPoints, "Normal Line"));
        }

        // Add end of day icon
        addIconPlaceMark(doc, dayFolder, dataPoints.getLast(), ICON_STYLE_END, "End");

        // Add kilometer posts
        if (!kmPosts.isEmpty()) {
            Element kmFolder = doc.createElement("Folder");
            dayFolder.appendChild(kmFolder);

            Element kmFolderName = doc.createElement("name");
            kmFolderName.setTextContent("Kilometer posts");
            kmFolder.appendChild(kmFolderName);

            Element visibility = doc.createElement("visibility");
            visibility.setTextContent(settings.isKmSignVisibility() ? "1" : "0");
            kmFolder.appendChild(visibility);

            for (KmPost post : kmPosts) {
                addKmPlaceMark(doc, kmFolder, post);
            }
        }
    }

    /**
     * Draws speed lines for a single day based on the provided data points.
     * Adds optional start, end, and direction icons for each speed segment if enabled in settings.
     *
     * @param doc        The KML document.
     * @param dayFolder  The parent folder element representing the day.
     * @param dataPoints The list of data points for the day.
     */
    private void drawSpeedLinesByDay(Document doc, Element dayFolder, List<DataPoint> dataPoints) {
        if (dataPoints == null || dataPoints.isEmpty()) return;

        List<CoordinatePoint> currentSegmentPoints = new ArrayList<>();
        List<Segment> segments = new ArrayList<>();

        for (int i = 1; i < dataPoints.size(); i++) {
            DataPoint prev = dataPoints.get(i - 1);
            DataPoint curr = dataPoints.get(i);

            if (prev.getSpeed() >= settings.getSpeedMap() && curr.getSpeed() >= settings.getSpeedMap()) {
                currentSegmentPoints.add(new CoordinatePoint(prev.getLongitude(), prev.getLatitude(), prev.getPointTime()));
                if (i == dataPoints.size() - 1) {
                    currentSegmentPoints.add(new CoordinatePoint(curr.getLongitude(), curr.getLatitude(), curr.getPointTime()));
                    segments.add(new Segment(new ArrayList<>(currentSegmentPoints), "Speed Line", LINE_STYLE_SPEED));
                }
            } else {
                if (!currentSegmentPoints.isEmpty()) {
                    currentSegmentPoints.add(new CoordinatePoint(prev.getLongitude(), prev.getLatitude(), prev.getPointTime()));
                    segments.add(new Segment(new ArrayList<>(currentSegmentPoints), "Speed Line", LINE_STYLE_SPEED));
                    currentSegmentPoints.clear();
                }
            }
        }

        // We prepare a folder of markers if necessary
        Element speedMarkerFolder = null;
        if (settings.isKmSign()) {
            speedMarkerFolder = doc.createElement("Folder");

            Element folderName = doc.createElement("name");
            folderName.setTextContent("Speed Markers");
            speedMarkerFolder.appendChild(folderName);

            applyVisibility(speedMarkerFolder);
        }

        // Add lines to the daily folder
        for (Segment segment : segments) {
            List<CoordinatePoint> points = segment.getPoints();
            if (points.size() < 2) continue;

            double segmentLength = 0.0;
            for (int j = 1; j < points.size(); j++) {
                CoordinatePoint p1 = points.get(j - 1);
                CoordinatePoint p2 = points.get(j);
                segmentLength += calculateDistance(p1.latitude(), p1.longitude(), p2.latitude(), p2.longitude());
            }

            createLinePlaceMark(doc, dayFolder, "Speed Line", segment);

            // Add icons to a separate folder only if long enough
            if (settings.isKmSign() && segmentLength >= 0.01 && speedMarkerFolder != null) {
                applyVisibilityToSpeedSegmentIcons(doc, speedMarkerFolder, points);
            }
        }

        // Add icon folder to the very end of the day folder
        if (speedMarkerFolder != null && speedMarkerFolder.hasChildNodes()) {
            dayFolder.appendChild(speedMarkerFolder);
        }
    }

    /**
     * Adds a visibility tag to a KML element based on user settings.
     *
     * @param target The KML element (Placemark or Folder) to which visibility is added.
     */
    private void applyVisibility(Element target) {
        Document doc = target.getOwnerDocument();
        Element visibility = doc.createElement("visibility");
        visibility.setTextContent(settings.isKmSignVisibility() ? "1" : "0");
        target.appendChild(visibility);
    }

    /**
     * Adds start, end, and direction icons to the given folder for a speed segment.
     * Visibility is applied according to user settings.
     *
     * @param doc          The KML document.
     * @param parentFolder The folder to which the placemarks will be added.
     * @param points       The list of coordinate points representing the segment.
     */
    private void applyVisibilityToSpeedSegmentIcons(Document doc, Element parentFolder, List<CoordinatePoint> points) {
        if (!settings.isKmSign()) return;
        if (points.size() < 2) return;

        CoordinatePoint start = points.getFirst();
        CoordinatePoint end = points.getLast();

        // Start icon
        Element startPlacemark = createIconPlacemark(doc, start.latitude(), start.longitude(), "Speed Start", ICON_STYLE_SPEED_START);
        applyVisibility(startPlacemark);
        parentFolder.appendChild(startPlacemark);

        // End icon
        Element endPlacemark = createIconPlacemark(doc, end.latitude(), end.longitude(), "Speed End", ICON_STYLE_SPEED_END);
        applyVisibility(endPlacemark);
        parentFolder.appendChild(endPlacemark);

        // Direction icon
        double heading = bearingInDegrees(start, points.get(1));
        Element directionPlacemark = createDirectionMarker(doc, start, heading);
        applyVisibility(directionPlacemark);
        parentFolder.appendChild(directionPlacemark);
    }

    /**
     * Converts a given time (ZonedDateTime) to the local time zone with the correct format
     * @param pointTime ZonedDateTime in UTC
     * @return String "dd.MM.yyyy HH:mm:ss"
     */
    private String convertUtcToLocal(ZonedDateTime pointTime) {
        ZonedDateTime localTime = pointTime.withZoneSameInstant(settings.getTimeZone().toZoneId());
        return localTime.format(model.getDateTimeEstonia());
    }

    /**
     * To calculate the path length between two coordinates
     * <a href="https://www.geodatasource.com/developers/java">Source</a>
     *
     * @param lat1 current latitude
     * @param lon1 current longitude
     * @param lat2 previous latitude
     * @param lon2 previous longitude
     * @return length in double
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = (dist * 60 * 1.1515) * 1.609344;

        if (dist >= 0) {
            return (dist);
        } else {
            return 0;
        }
    }

    /**
     * This function converts decimal degrees to radians
     *
     * @param deg degree
     * @return radians
     */
    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /**
     * This function converts radians to decimal degrees
     *
     * @param rad radians
     * @return decimal
     */
    private double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    /**
     * Calculates the duration between two time points and formats it as HH:mm:ss.
     *
     * @param start The start time.
     * @param end   The end time.
     * @return A formatted duration string.
     */
    private String calculateTime(ZonedDateTime start, ZonedDateTime end) {
        Duration duration = Duration.between(start, end);
        return String.format("%02d:%02d:%02d",
                duration.toHoursPart(),
                duration.toMinutesPart(),
                duration.toSecondsPart());
    }

    /**
     * Calculates the bearing angle in radians between two geographic points.
     *
     * @param previous The previous data point.
     * @param current  The current data point.
     * @return The bearing angle in radians.
     */
    private double bearingAngle(DataPoint previous, DataPoint current) {
        double srcLat = Math.toRadians(previous.getLatitude());
        double dstLat = Math.toRadians(current.getLatitude());
        double dLng = Math.toRadians(current.getLongitude() - previous.getLongitude());

        return Math.atan2(Math.sin(dLng) * Math.cos(dstLat),
                Math.cos(srcLat) * Math.sin(dstLat) -
                        Math.sin(srcLat) * Math.cos(dstLat) * Math.cos(dLng));
    }

    /**
     * Calculates the bearing angle in degrees between two geographic points.
     *
     * @param previous The previous data point.
     * @param current  The current data point.
     * @return The bearing angle in degrees (0–360°).
     */
    private double bearingInDegrees(DataPoint previous, DataPoint current) {
        return Math.toDegrees((bearingAngle(previous, current)));
    }

    /**
     * Calculates the bearing (direction angle) in degrees between two coordinates.
     *
     * @param from Starting coordinate.
     * @param to   Ending coordinate.
     * @return Bearing angle in degrees (0–360°).
     */
    private double bearingInDegrees(CoordinatePoint from, CoordinatePoint to) {
        double lat1 = Math.toRadians(from.latitude());
        double lon1 = Math.toRadians(from.longitude());
        double lat2 = Math.toRadians(to.latitude());
        double lon2 = Math.toRadians(to.longitude());

        double deltaLon = lon2 - lon1;

        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);

        return Math.toDegrees(Math.atan2(y, x));
    }
}
