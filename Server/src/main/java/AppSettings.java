import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class AppSettings {

    public int getPortFromSettingsFile() throws IOException {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            throw new IllegalStateException("user.home==null");
        }
        Properties properties = new Properties();
        final String settingsFileName = userHome + File.separator + "server.properties";
        int serverPort;
        File settings = new File(settingsFileName);
        if (settings.exists() && settings.isFile()) {
            try (FileInputStream inputStream = new FileInputStream(settingsFileName)) {
                properties.load(inputStream);
                serverPort = Integer.parseInt(properties.getProperty("port"));
            }
        } else {
            settings.createNewFile();
            try (FileOutputStream out = new FileOutputStream(settingsFileName)) {
                properties.setProperty("port", "8080");
                properties.store(out, "configuring the server port");
                serverPort = Integer.parseInt(properties.getProperty("port"));
            }
        }
        return serverPort;
    }
}
