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

    private File cachedJarDir = null;

    private File getJarDirectory()
    {
        if (cachedJarDir != null) return cachedJarDir;

        try
        {
            String classPath = DependencyLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(classPath);
            cachedJarDir = jarFile.getParentFile();
            return cachedJarDir;            
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

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
            
            boolean finished = process.waitFor(8, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                System.err.println(executableCmd + " check timed out after 8s.");
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized String getYtdlpPath() 
    {
        //returns imediately if already been found
        if (cachedYtdlpPath != null) {
            return cachedYtdlpPath;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String executableName = os.contains("windows") ? "yt-dlp.exe" : "yt-dlp";
        
        // 1. Try PATH First
        if (checkProcessSilently(executableName, "--version")) {
            System.out.println("yt-dlp found in system PATH.");
            cachedYtdlpPath = executableName;
            return cachedYtdlpPath;
        }

        // 2. Try Folder if not found in PATH
        File jarDir = getJarDirectory();
        if (jarDir != null)
        {
            try
            {
                File executableFile = new File(jarDir, executableName);
                
                if (executableFile.exists()) 
                {
                    if (!executableFile.canExecute()) {
                        executableFile.setExecutable(true);
                    }
                    System.out.println("YT-DLP found in application folder.");
                    cachedYtdlpPath = executableFile.getAbsolutePath();
                    return cachedYtdlpPath;
                } 
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.err.println("yt-dlp not found in folder or PATH.");
        cachedYtdlpPath = executableName;
        return cachedYtdlpPath; 
    }

    public synchronized String getFFmpegPath() 
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
        File jarDir = getJarDirectory();
        if (jarDir != null)
        {
            try
            {
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
        }

        System.err.println("ffmpeg not found in PATH or folder.");
        cachedFfmpegPath = executableName;
        return cachedFfmpegPath; 
    }

    //Takes path to icon returns Icon object
    public Icon getIcon(String name)
    {
        File jarDir = getJarDirectory();
        if (jarDir != null)
        {
            // 1. Try to load from the executable/jar folder
            try
            {
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