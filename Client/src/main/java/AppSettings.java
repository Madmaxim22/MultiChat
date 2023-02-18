import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class AppSettings {

    private String addressServer;
    private int portServer;

    public String getAddressServer() {
        return addressServer;
    }

    public int getPortServer() {
        return portServer;
    }

    public void getSettingsFile() throws IOException {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            throw new IllegalStateException("user.home==null");
        }
        Properties properties = new Properties();
        final String settingsFilename = userHome + File.separator + "client.properties";
        File settings = new File(settingsFilename);
        if (settings.exists() && settings.isFile()) {
            try (FileInputStream inputStream = new FileInputStream(settingsFilename)) {
                properties.load(inputStream);
                addressServer = properties.getProperty("address");
                portServer = Integer.parseInt(properties.getProperty("port"));
            }
        } else if (settings.createNewFile()) {
            try (FileOutputStream out = new FileOutputStream(settingsFilename)) {
                properties.setProperty("address", "127.0.0.1");
                properties.setProperty("port", "8080");
                properties.store(out, "configuring the  ip address and port server");
                addressServer = properties.getProperty("address");
                portServer = Integer.parseInt(properties.getProperty("port"));
            }
        }
    }
}
