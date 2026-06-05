import java.io.IOException;

import models.Model;
import models.Settings;
import version.BuildInfo;

public class MainApp {
    public static void main(String[] args) throws IOException {
        System.out.println("Build Version: " + BuildInfo.VERSION);
        Settings settings = new Settings();
        new Model(settings);
    }
}
