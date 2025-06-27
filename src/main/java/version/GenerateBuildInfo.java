package version;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class GenerateBuildInfo {
    public static void main(String[] args) throws IOException {
        Path propsPath = Paths.get("src/main/resources/buildnumber.properties");
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(propsPath)) {
            props.load(in);
        }

        int major = Integer.parseInt(props.getProperty("MAJOR", "1"));
        int minor = Integer.parseInt(props.getProperty("MINOR", "0"));
        int build = Integer.parseInt(props.getProperty("BUILD", "0")) + 1;
        props.setProperty("BUILD", String.valueOf(build));

        try (OutputStream out = Files.newOutputStream(propsPath)) {
            props.store(out, "Updated by GeneratedBuildInfo");
        }

        String version = String.format("%d.%d.%d", major, minor, build);
        /*String content = "package version;\n\n"
                + "public class BuildInfo {\n"
                + "    public static final String VERSION = \"" + version + "\";\n"
                + "}\n";*/
        String content = "package version;\n\n"
                + "public class BuildInfo {\n"
                + "    public static final String VERSION = \"" + version + "\";\n"
                + "}\n";

        Path outputDir = Paths.get("src/main/java/version");
        Files.createDirectories(outputDir);
        System.out.println("Writing BuildInfo.java to: " + outputDir);
        if (!Files.exists(outputDir)) {
            System.out.println("Output directory does not exist, creating it...");
            Files.createDirectories(outputDir);
        }
        Files.writeString(outputDir.resolve("BuildInfo.java"), content);

        System.out.println("Generated BuildInfo.java with version: " + version);
    }
}
