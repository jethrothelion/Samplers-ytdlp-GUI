import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.awt.Image;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class DependencyLocator 
// has getDependancyPath(base command, --specific version flag) for every dependancy nessicary,
// methods returns path to dependancy or a image object in the case of icons
{
    private String cachedYtdlpPath = null;
    private String cachedFfmpegPath = null;

    // silent checker method that reads output to prevent process freezing
    public boolean checkProcessSilently(String executableCmd, String versionFlag) 
    {
        try {
            ProcessBuilder pb = new ProcessBuilder(executableCmd, versionFlag);
            pb.redirectErrorStream(true); // Combine stderr and stdout
            Process process = pb.start();
            
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (br.readLine() != null) {
                    // Do nothing just empty the stream
                }
            }
            
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String getYtdlpPath() 
    {
        //returns imediately if already been found
        if (cachedYtdlpPath != null) {
            return cachedYtdlpPath;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String executableName = os.contains("windows") ? "yt-dlp.exe" : "yt-dlp";

        try
        {
            String classPath = DependencyLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(classPath);
            File jarDir = jarFile.getParentFile();
            File executableFile = new File(jarDir, executableName);
            
            // 1. Try Folder First (Standard for yt-dlp)
            if (executableFile.exists()) 
            {
                if (!executableFile.canExecute()) {
                    executableFile.setExecutable(true);
                }
                cachedYtdlpPath = executableFile.getAbsolutePath();
                return cachedYtdlpPath;
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Try PATH if not found in folder
        if (checkProcessSilently(executableName, "--version")) {
            System.out.println("yt-dlp found in system PATH.");
            cachedYtdlpPath = executableName;
            return cachedYtdlpPath;
        }

        System.err.println("yt-dlp not found in folder or PATH.");
        cachedYtdlpPath = executableName;
        return cachedYtdlpPath; 
    }

    public String getFFmpegPath() 
    {
        // Return immediately if we already found it before
        if (cachedFfmpegPath != null) {
            return cachedFfmpegPath;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String executableName = os.contains("windows") ? "ffmpeg.exe" : "ffmpeg";

        // 1. Try PATH First 
        if (checkProcessSilently(executableName, "-version")) {
            System.out.println("ffmpeg found in system PATH.");
            cachedFfmpegPath = executableName;
            return cachedFfmpegPath;
        }

        // 2. Try Folder if not found in PATH
        try
        {
            String classPath = DependencyLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(classPath);
            File jarDir = jarFile.getParentFile();
            File executableFile = new File(jarDir, executableName);
            
            if (executableFile.exists()) 
            {
                if (!executableFile.canExecute()) {
                    executableFile.setExecutable(true);
                }
                System.out.println("ffmpeg found in application folder.");
                cachedFfmpegPath = executableFile.getAbsolutePath();
                return cachedFfmpegPath;
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.err.println("ffmpeg not found in PATH or folder.");
        cachedFfmpegPath = executableName;
        return cachedFfmpegPath; 
    }

    public Icon getIcon(String name)
    {
        //Takes path to icon returns Icon object

        // 1. Try to load from the executable/jar folder (same logic as yt-dlp)
        try
        {
            // CHANGED: DownloadGUI.class to DependencyLocator.class
            String classPath = DependencyLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(classPath);
            File jarDir = jarFile.getParentFile();
            File iconFile = new File(jarDir, name);
            
            if (iconFile.exists()) 
            {
                ImageIcon icon = new ImageIcon(iconFile.getAbsolutePath());
                Image scaledImage = icon.getImage().getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Fallback to the internal resource method
        java.net.URL iconURL = getClass().getResource(name);

        if(iconURL != null)
        {
            ImageIcon icon = new ImageIcon(iconURL);

            Image scaledImage = icon.getImage().getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);

            return new ImageIcon(scaledImage);
        }
        else
        {
            System.err.println("Warning: " + name + " not found in resources. Falling back to default.");
            
            // CHANGED: Replaced popupMessage() with JOptionPane passing null for the parent component
            JOptionPane.showMessageDialog(null, "cant find " + name + " icon defaulting to os default save icon may not by there");
            
            if (name.equals("folderIcon.png"))
            {
                return UIManager.getIcon("FileView.directoryIcon");
            }
            else if(name.equals("saveIcon.png"))
            {
                return UIManager.getIcon("FileView.floppyDriveIcon");
            }

            return null;
        }
    }
}