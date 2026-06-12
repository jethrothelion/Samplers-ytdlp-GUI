import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager 
{
    // shared instance of this class
    private static ConfigManager instance;
    private Properties properties;
    // config file path
    private final String configPath = System.getProperty("user.home") + File.separator + "YoutubeDownloaderConfig.properties";

    private ConfigManager() 
    {
        properties = new Properties();
        load();
    }

    // get shared instance 
    public static ConfigManager getInstance() 
    {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private void load() 
    {
        try (FileInputStream in = new FileInputStream(configPath)) {
            properties.load(in);
        } catch (IOException e) {
            System.out.println("No config file found, starting with empty properties.");
        }
    }

    // Gets a setting, returning a fallback if it doesn't exist
    public String getProperty(String key, String defaultValue) 
    {
        return properties.getProperty(key, defaultValue);
    }

    // Updates a setting in memory (ignores nulls to prevent crashes)
    public void setProperty(String key, String value) 
    {
        if (value != null) {
            properties.setProperty(key, value);
        }
    }

    // Writes the memory state back to the hard drive
    public void save() 
    {
        try (FileOutputStream out = new FileOutputStream(configPath)) {
            properties.store(out, "User Preferences");
            System.out.println("Settings saved to config.properties");
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
}