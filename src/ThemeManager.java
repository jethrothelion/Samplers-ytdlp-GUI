import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

public class ThemeManager
// Loads the selected theme from the themes folder and hands out colors by name,
// first line of a theme file is its display name, the rest are name=r,g,b or name=r,g,b,a
{
    // shared instance of this class
    private static ThemeManager instance;
    private Map<String, Color> colors = new HashMap<>();

    private ThemeManager()
    {
        setDefaults();
        readTheme();
    }

    // get shared instance
    public static ThemeManager getInstance()
    {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    // Fallback colors used when a theme is missing or does not set a name
    private void setDefaults()
    {
        colors.put("borderColor", Color.BLACK);
        colors.put("accentColor", Color.BLUE);
        colors.put("placeholderTextColor", Color.GRAY);

        colors.put("secondaryButtonBackground", Color.WHITE);
        colors.put("secondaryButtonText", Color.BLACK);

        colors.put("successBackground", Color.GREEN);
        colors.put("successText", new Color(0, 0, 0));
        colors.put("successBorder", new Color(0, 153, 51));

        colors.put("dangerBackground", new Color(200, 0, 0));
        colors.put("dangerText", Color.WHITE);
        colors.put("dangerBorder", new Color(150, 0, 0));

        colors.put("overlayColor", new Color(0, 0, 0, 200));
        colors.put("overlayTextColor", Color.PINK);

        colors.put("sidebarBackground", new Color(230, 230, 230));

        colors.put("timelineEndBar", Color.GRAY);
        colors.put("timelineTrack", Color.LIGHT_GRAY);
        colors.put("timelineTrackZoomed", new Color(255, 180, 180));
        colors.put("timelineSelection", new Color(100, 150, 255));
        colors.put("timelineHandle", Color.DARK_GRAY);
        colors.put("timelineText", Color.BLACK);

        // backgroundColor, textColor, textBoxBackground, textBoxTextColor, buttonBackground
        // and buttonTextColor have no default so the look and feel keeps its own
    }

    public void readTheme()
    {
        // Start from the defaults each time so a theme that was switched away from leaves nothing behind
        colors.clear();
        setDefaults();

        String themeName = ConfigManager.getInstance().getProperty("theme", "Light");

        File[] themeFiles = DependencyLocator.getInstance().getThemesFolder().listFiles();
        if (themeFiles == null) return; // No themes folder, keep default colors

        for (File themeFile : themeFiles)
        {
            try (BufferedReader reader = new BufferedReader(new FileReader(themeFile)))
            {
                // First line is the display name
                String displayName = reader.readLine();
                if (displayName == null || !displayName.trim().equals(themeName)) continue;

                Properties theme = new Properties();
                theme.load(reader);

                for (String key : theme.stringPropertyNames())
                {
                    colors.put(key, parseColor(theme.getProperty(key), colors.get(key)));
                }
                return;
            } catch (IOException e) {
                System.err.println("Failed to read theme " + themeFile.getName() + ": " + e.getMessage());
            }
        }
        System.out.println("Theme " + themeName + " not found, using default colors.");
    }

    // Gets a color by name, null if neither the theme or defaults set it
    public Color getColor(String key)
    {
        return colors.get(key);
    }

    // Gets a color by name, returning a fallback if it doesn't exist
    public Color getColor(String key, Color defaultValue)
    {
        return colors.getOrDefault(key, defaultValue);
    }

    // Display names of every theme in the themes folder, for the settings dropdown
    public List<String> getThemeNames()
    {
        List<String> themes = new ArrayList<>();
        File[] themeFiles = DependencyLocator.getInstance().getThemesFolder().listFiles();
        if (themeFiles != null)
        {
            for (File themeFile : themeFiles)
            {
                try (BufferedReader reader = new BufferedReader(new FileReader(themeFile)))
                {
                    String displayName = reader.readLine();
                    if (displayName != null && !displayName.trim().isEmpty()) themes.add(displayName.trim());
                } catch (IOException e) {
                    System.err.println("Failed to read theme " + themeFile.getName() + ": " + e.getMessage());
                }
            }
        }
        if (themes.isEmpty()) themes.add("Light");
        return themes;
    }

    // Pushes the background theme colors into the UIManager so every window
    // (including settings and popups) picks them up without setting each component,
    // windows that already exist need SwingUtilities.updateComponentTreeUI after this
    public void applyTheme()
    {
        putThemeColor(getColor("backgroundColor"), "Panel.background", "RootPane.background", "RadioButton.background", "CheckBox.background",
                      "OptionPane.background", "ScrollPane.background", "Viewport.background", "ProgressBar.background");
        putThemeColor(getColor("textColor"), "Label.foreground", "RadioButton.foreground", "CheckBox.foreground", "OptionPane.messageForeground");
        putThemeColor(getColor("textBoxBackground"), "TextField.background", "TextArea.background");
        putThemeColor(getColor("textBoxTextColor"), "TextField.foreground", "TextArea.foreground", "TextField.caretForeground", "TextArea.caretForeground");
        putThemeColor(getColor("buttonBackground"), "Button.background");
        putThemeColor(getColor("buttonTextColor"), "Button.foreground");

        // Metal paints its own light gradient over buttons, swap it for a flat one in the theme color
        // (null hands the key back to the look and feel default gradient)
        Color buttonColor = getColor("buttonBackground");
        UIManager.put("Button.gradient", buttonColor == null ? null : Arrays.asList(0f, 0f, buttonColor, buttonColor, buttonColor));
    }

    // Sets one color for each UIManager key, a null color clears the key back to the look and feel default
    private void putThemeColor(Color color, String... uiKeys)
    {
        // Wrapped as a UIResource so Swing knows it came from the theme and will replace it on the next reload
        ColorUIResource uiColor = color == null ? null : new ColorUIResource(color);
        for (String key : uiKeys)
        {
            UIManager.put(key, uiColor);
        }
    }

    // Turns "r,g,b" or "r,g,b,a" into a Color, returns fallback if missing or bad
    private Color parseColor(String value, Color fallback)
    {
        if (value == null) return fallback;
        try
        {
            String[] parts = value.split(",");
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            if (parts.length > 3) return new Color(r, g, b, Integer.parseInt(parts[3].trim()));
            return new Color(r, g, b);
        } catch (Exception e) {
            System.err.println("Bad theme color: " + value);
            return fallback;
        }
    }
}
