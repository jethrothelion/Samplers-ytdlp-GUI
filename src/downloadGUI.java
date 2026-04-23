import java.awt.*;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.net.URI;
        


public class downloadGUI extends JFrame
{
    private String selectedDirectory;
    private JLabel downloadMessage;
    private JProgressBar progressBar;
    private JTextField urlField;
    private JTextField commandBar;
    private JRadioButton videoBtn;
    private JRadioButton audioBtn;
    private JRadioButton highestButton;
    private JRadioButton mediumButton;
    private JRadioButton lowestButton;
    private double distanceFromLeft;
    private double distanceFromRight;
    private JLabel progressLabel;
    TimelineRangeSelector timelineSelector;

    private int videoDuration = -1; // Duration in seconds, -1 means unknown
    private boolean hasVerifiedExecutables = false; // Flag to prevent duplicate checks

    private String cachedYtdlpPath = null;
    private String cachedFfmpegPath = null;

    public void initialization()
    {
        // Basic window setup
        setTitle("Youtube Downloader");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 500);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;

        // --- TOP ROW: URL Input and Media Type ---
        // URL TextField
        urlField = new JTextField("Enter URL");
        urlField.setBorder(new LineBorder(Color.BLACK, 2));
        urlField.setForeground(Color.GRAY); 
        
        // Listen for typing in the URL field to update the timeline selector's max range 
        urlField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { 
                if (urlField.getText().contains("youtube.com"))
                {
                    fetchVideoDuration(urlField.getText()); 
                
                }
            }
            public void removeUpdate(DocumentEvent e) { 
                if (urlField.getText().contains("youtube.com"))
                {
                    fetchVideoDuration(urlField.getText()); 
                    
                }
            }
            public void insertUpdate(DocumentEvent e) { 
                if (urlField.getText().contains("youtube.com"))
                {
                    fetchVideoDuration(urlField.getText()); 
                }
            }
        });

        // Focus listener to handle placeholder text behavior
        urlField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                // If they click in and it says "Enter URL", clear it
                if (urlField.getText().equals("Enter URL")) {
                    urlField.setText("");
                    urlField.setForeground(Color.BLACK); // Switch to normal typing color
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                // If they click away and left it blank, put the placeholder back
                if (urlField.getText().isEmpty()) {
                    urlField.setForeground(Color.GRAY); // Switch back to placeholder color
                    urlField.setText("Enter URL");
                }
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;  // was 2
        gbc.weightx = 0.7;
        gbc.ipady = 40; // Makes the text box taller
        add(urlField, gbc);


        // Video/Audio Radio Buttons
        JPanel typePanel = new JPanel(new GridLayout(2, 1));
        videoBtn = new JRadioButton("Video");
        audioBtn = new JRadioButton("Audio");
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(videoBtn);
        typeGroup.add(audioBtn);
        typePanel.add(videoBtn);
        typePanel.add(audioBtn);
        
        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.ipady = 0; 
        add(typePanel, gbc);

        // New button
        JButton savePrefBttn = new JButton();

        Icon saveIcon = getIcon("saveIcon.png");
        savePrefBttn.setIcon(saveIcon);
        savePrefBttn.setPreferredSize(new Dimension(20, 30));
        JPanel newButtonPanel = new JPanel(new GridBagLayout());
        newButtonPanel.add(savePrefBttn);
        gbc.gridx = 5;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        savePrefBttn.addActionListener(e -> {
            setDefaults();

        });

        add(newButtonPanel, gbc);

        // Empty gap
        JPanel gapPanel = new JPanel();
        gapPanel.setPreferredSize(new Dimension(80, 1));
        gbc.gridx = 6;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        add(gapPanel, gbc);

        // Directory choosing button
        JButton folderSelectButton = new JButton("Select Folder");
        folderSelectButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();

            chooser.setCurrentDirectory(new java.io.File(""));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            
            if (chooser.showOpenDialog(folderSelectButton) == JFileChooser.APPROVE_OPTION) {
                selectedDirectory = chooser.getSelectedFile().getAbsolutePath();
                System.out.println("Selected folder: " + selectedDirectory);
            } else {
                System.out.println("No folder selected");
            }
        });

        Icon folderIcon = getIcon("folderIcon.png");
        
        folderSelectButton.setIcon(folderIcon);
        folderSelectButton.setBorder(new LineBorder(Color.BLACK, 2));
        folderSelectButton.setFont(new Font("Arial", Font.PLAIN, 12));
        folderSelectButton.setBackground(Color.WHITE);  
        folderSelectButton.setForeground(Color.BLACK);
        folderSelectButton.setPreferredSize(new Dimension(110, 50));
        folderSelectButton.setMinimumSize(new Dimension(110, 50));
        gbc.gridx = 7;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        add(folderSelectButton, gbc);

    
        // Command bar
        commandBar = new JTextField();
        commandBar.setBorder(new LineBorder(Color.BLUE, 2)); // Blue border to stand out
        commandBar.setFont(new Font("Monospaced", Font.PLAIN, 14)); // Code font
        
        JScrollPane scrollPane = new JScrollPane(commandBar, 
            JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(400, 50));
        scrollPane.setMinimumSize(new Dimension(100, 50));

        // Wrapper panel to hold the command bar and the hyperlink without shifting the UI
        JPanel commandWrapperPanel = new JPanel(new BorderLayout());
        commandWrapperPanel.add(scrollPane, BorderLayout.CENTER);

        // Small hyperlink label
        JLabel hyperlinkLabel = new JLabel("<html><a href=''>More parameters</a></html>");
        hyperlinkLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        hyperlinkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // MouseListener to make the label act like a real link
        hyperlinkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                try {
                    // Put your desired URL inside the quotes below
                    Desktop.getDesktop().browse(new URI("https://github.com/yt-dlp/yt-dlp"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    popupMessage("Could not open the link."); // Optional error popup
                }
            }
        });

        // FlowLayout to align the link to the right side
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        linkPanel.add(hyperlinkLabel);

        // Add the link underneath the command bar inside the wrapper
        commandWrapperPanel.add(linkPanel, BorderLayout.SOUTH);
        gbc.gridx = 0;
        gbc.gridy = 1; 
        gbc.gridwidth = 9;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.ipady = 15;
        
        add(commandWrapperPanel, gbc);


        // --- MIDDLE ROW: Timeline Range Selector ---
        timelineSelector = new TimelineRangeSelector();

        gbc.gridx = 4;
        gbc.gridy = 2;
        gbc.gridwidth = 5;
        gbc.weightx = 0.6;
        gbc.ipady = 0;
        add(timelineSelector, gbc);
        

        // --- MIDDLE ROW: Quality and More Options ---
        // Quality Panel
        JPanel qualityPanel = new JPanel();
        qualityPanel.setBorder(new LineBorder(Color.BLACK, 2));
        qualityPanel.setLayout(new BoxLayout(qualityPanel, BoxLayout.Y_AXIS));
        
        highestButton = new JRadioButton("Highest quality");
        highestButton.setActionCommand("3");
        highestButton.setSelected(rootPaneCheckingEnabled);

        mediumButton = new JRadioButton("Medium quality");
        mediumButton.setActionCommand("2");

        lowestButton = new JRadioButton("Lowest quality");
        lowestButton.setActionCommand("1");

        //button group blocks mulitple selections at once
        ButtonGroup qualityGroup = new ButtonGroup();
        qualityGroup.add(lowestButton); qualityGroup.add(mediumButton); qualityGroup.add(highestButton);

        qualityPanel.add(highestButton); qualityPanel.add(mediumButton); qualityPanel.add(lowestButton);
         
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.weightx = 0.4;
        gbc.weighty = 0.3;
        add(qualityPanel, gbc);


        // --- BOTTOM ROW: Status and Progress ---
        // Status Container
        JPanel statusContainer = new JPanel(new BorderLayout(5, 5));
        
        // Status box (left)
        JPanel statusBox = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel("download status", SwingConstants.LEFT);
        downloadMessage = new JLabel("inactive", SwingConstants.CENTER);  
        downloadMessage.setBorder(new LineBorder(Color.BLACK, 2));
        downloadMessage.setPreferredSize(new Dimension(100, 50));
        statusBox.add(statusLabel, BorderLayout.NORTH);
        statusBox.add(downloadMessage, BorderLayout.CENTER);

        // Progress bar box (right)
        JPanel progressBox = new JPanel(new BorderLayout());
        progressLabel = new JLabel("progress bar", SwingConstants.LEFT);
        progressBar = new JProgressBar();
        progressBar.setBorder(new LineBorder(Color.BLACK, 2));
        progressBar.setPreferredSize(new Dimension(400, 30));
        progressBox.add(progressLabel, BorderLayout.NORTH);
        progressBox.add(progressBar, BorderLayout.CENTER);

        statusContainer.add(statusBox, BorderLayout.WEST);
        statusContainer.add(progressBox, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 9;
        gbc.weighty = 0.1;
        add(statusContainer, gbc);

        // Download Button
        JButton downloadBtn = new JButton("DOWNLOAD");
        downloadBtn.setForeground(new Color(0, 0, 0));
        downloadBtn.setBorder(new LineBorder(new Color(0, 153, 51), 3));
        downloadBtn.setFont(new Font("Arial", Font.BOLD, 16));
        downloadBtn.setBackground(Color.GREEN);
        downloadBtn.setPreferredSize(new Dimension(5, 30));
        downloadBtn.addActionListener(e -> {
            startDownload(commandBar.getText());
        });
        gbc.gridx = 8;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weighty = 0;    
        gbc.weightx = 0.2;
        add(downloadBtn, gbc);


        constructCommand();
        setupCommandBarListeners();

        readConfig();

        // Run verification (Guaranteed to only run once)
        runStartupChecks();

        setLocationRelativeTo(null);
        setVisible(true);
        
    }

    public Icon getIcon(String name)
    {
        //Takes path to icon returns Icon object

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
            popupMessage("cant find " + name + " icon defaulting to os default save icon may not by there");
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

    public void setDefaults()
    {
        Properties prop = new Properties();

        // Use FileOutputStream to create/overwrite the config file
        try (FileOutputStream out = new FileOutputStream("config.properties")) 
        {
            // 1. Get the propertys from the radio buttons

            //quality
            String quality = "3"; // Defaults to highest
            if (mediumButton.isSelected()) quality = "2";
            if (lowestButton.isSelected()) quality = "1";

            //video or audio
            String type = "audio"; // Defaults to audio
            if (videoBtn.isSelected()) type = "video";
            if (audioBtn.isSelected()) type = "audio";



            // 2. Add properties to the object
            prop.setProperty("quality", quality);

            if (selectedDirectory != null) 
            {
                prop.setProperty("directory", selectedDirectory);
            }

            prop.setProperty("type", type);

            // 3. Save the file to disk
            prop.store(out, "User Preferences");
            popupMessage("Settings saved!");
            System.out.println("Settings saved to config.properties");

        } catch (IOException e) {
            e.printStackTrace();
            popupMessage("Failed to save settings.");
        }

        
    }
    public void readConfig()
    {

        Properties prop = new Properties();
        // Load the file
        try (FileInputStream in = new FileInputStream("config.properties")) {
            prop.load(in);

            //quality
            String quality = prop.getProperty("quality", "3"); // "3" is the default if key isn't found
            if (quality.equals("1")) {
                lowestButton.setSelected(true);
            } else if (quality.equals("2")) {
                mediumButton.setSelected(true);
            } else {
                highestButton.setSelected(true);
            }

            //directory
            File savedDir = new File(prop.getProperty("directory"));
            if(savedDir.exists() && savedDir.isDirectory())
            {   
                
                selectedDirectory = prop.getProperty("directory");
            }

            //type
            String type = prop.getProperty("type");
            if (type.equals("audio")) audioBtn.setSelected(true);
            if (type.equals("video")) videoBtn.setSelected(true);


            // Refresh the command bar to reflect loaded settings
            constructCommand();

        } catch (IOException e) {
            System.out.println("No config file found, using defaults.");

            videoBtn.setSelected(true);
        }


    }
    
    // Dedicated method to ensure checks only run once
    private void runStartupChecks() {
        if (hasVerifiedExecutables) {
            return; // Exit immediately if already checked
        }
        
        System.out.println("Running initial dependency checks...");
        verifyExecutable(getYtdlpPath(), "--version", "yt-dlp");
        verifyExecutable(getFFmpegPath(), "-version", "ffmpeg");
        
        hasVerifiedExecutables = true; // Mark as done so it never runs again
    }

    // New silent checker method that reads output to prevent process freezing
    private boolean checkProcessSilently(String executableCmd, String versionFlag) {
        try {
            ProcessBuilder pb = new ProcessBuilder(executableCmd, versionFlag);
            pb.redirectErrorStream(true); // Combine stderr and stdout
            Process process = pb.start();
            
            // CRUCIAL: Read and discard output. FFMPEG dumps a lot of text. 
            // If we don't clear the buffer, Java blocks and the process hangs/fails.
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (br.readLine() != null) {
                    // Do nothing, just empty the stream
                }
            }
            
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean verifyExecutable(String path, String versionFlag, String appName)
    {
        // Check using the safe buffer-clearing method
        if (checkProcessSilently(path, versionFlag)) {
            return true;
        }
        
        // Changed to a Warning instead of Error. yt-dlp might still find it even if Java struggles to verify it.
        popupMessage("Warning: Could not verify " + appName + ".\nIt may not be in your system PATH or application folder. If downloads fail, please ensure it is installed.");
        return false;
    }

    public String getYtdlpPath() {
        //returns imediately if already been found
        if (cachedYtdlpPath != null) {
            return cachedYtdlpPath;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String executableName = os.contains("windows") ? "yt-dlp.exe" : "yt-dlp";

        try
        {
            String classPath = downloadGUI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
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

    public String getFFmpegPath() {
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
            String classPath = downloadGUI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
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

    private void fetchVideoDuration(String url) {
        SwingWorker<Integer, Void> durationWorker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception 
            {
                try {
                    ProcessBuilder pb = new ProcessBuilder(getYtdlpPath(), url, "--print", "%(duration)s");
                    Process process = pb.start();
                    BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String line = stdout.readLine();

                    StringBuilder errorMessage = new StringBuilder();
                    String errLine;
                    while ((errLine = stderr.readLine()) != null) {
                        errorMessage.append(errLine).append("\n");
                    }

                    int exitCode = process.waitFor();
                    
                    if (exitCode != 0 )
                    {
                        throw new Exception("YT-DLP failed with exit code " + exitCode + ".\nError details: " + errorMessage.toString().trim());
                    }
                    if (line != null && !line.isEmpty())
                    {
                        return (int) Double.parseDouble(line.trim());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return -1;
            }

            @Override   
            protected void done() 
            {
                try {
                    // get() retrieves the return value from doInBackground()
                    int fetchedDuration = get();
                    if (fetchedDuration != -1) {
                        videoDuration = fetchedDuration;
                        timelineSelector.setVideoDuration(videoDuration);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        
        durationWorker.execute();
    }

    private void constructCommand() 
    {
        List<String> command = new ArrayList<>();

        command.add(getYtdlpPath()); // Add the executable path first

        // Point yt-dlp to the specific bundled ffmpeg location
        String ffmpegLocation = getFFmpegPath();
        // if contains file separator we know its not the path variable
        if (ffmpegLocation.contains(File.separator))
        {
            command.add("--ffmpeg-location");
            command.add("\"" + ffmpegLocation + "\"");
        }

        String url = urlField.getText();
        if (!url.isEmpty() && !url.equals("Enter URL")) {
            command.add("\"" + url + "\"");
        }
        else
        {
            command.add("\"url\"");
        }

        if(selectedDirectory != null && !selectedDirectory.isEmpty())
        {
            command.add("-o");
            command.add(selectedDirectory + File.separator+ "%(title)s.%(ext)s");
        } else {
            command.add("-o");
            command.add("%(title)s.%(ext)s");
        }
        if (audioBtn.isSelected())
        {
            command.add("-x");
            command.add("--audio-format");
            command.add("mp3");
        }
        
        //if not full range selected
        if(!timelineSelector.isFullRangeSelected())
        {
            command.add("--download-sections");
            command.add("\"* " + (timelineSelector.getStartTime()) + "-" + (timelineSelector.getEndTime()) + "\"");
        }

        //quality options
        if (highestButton.isSelected()) {
            command.add(audioBtn.isSelected() ? "-f bestaudio " : "-f bestvideo+bestaudio ");
        } else if (mediumButton.isSelected()) {
            command.add(audioBtn.isSelected() ? "-f bestaudio.2 " : "-f bestvideo.2+bestaudio.2 ");
        } else if (lowestButton.isSelected()) {
            command.add(audioBtn.isSelected() ? "-f worstaudio " : "-f worstvideo+worstaudio ");
        }




        commandBar.setText(String.join(" ", command).trim());  
    
    }

    private void setupCommandBarListeners() {
        // Listen for typing in the URL field
        urlField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { constructCommand(); }
            public void removeUpdate(DocumentEvent e) { constructCommand(); }
            public void insertUpdate(DocumentEvent e) { constructCommand(); }
        });

        // Listen for radio button clicks
        videoBtn.addActionListener(e -> constructCommand());
        audioBtn.addActionListener(e -> constructCommand());
        highestButton.addActionListener(e -> constructCommand());
        mediumButton.addActionListener(e -> constructCommand());
        lowestButton.addActionListener(e -> constructCommand());
    }
    
    public void popupMessage(String message)
    {
        JOptionPane.showMessageDialog(this, message);
    }

    public void startDownload(String command)
    {
        System.out.println("Starting download with command: " + command);
        
        SwingWorker<Void, Integer> downloadWorker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception 
            {
                downloadManager downloader = new downloadManager();
                downloader.Download(command, new downloadListener()
                {
                    @Override
                    public void onProgress(int progress) {
                        publish(progress);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        SwingUtilities.invokeLater(() -> popupMessage("Error: " + errorMessage));
                    }

                    @Override
                    public void onComplete(boolean success) {
                        SwingUtilities.invokeLater(() -> {
                            if (success) {
                                popupMessage("Download completed successfully!");
                                progressBar.setValue(0);
                            } else {    
                                popupMessage("Download failed.");
                                progressBar.setValue(0);
                            }   
                        });
                    }   
                    @Override
                    public void onMessage(String message) {
                        SwingUtilities.invokeLater(() -> downloadMessage.setText(message));
                    }
           });
                
                return null;
            }

            @Override 
            protected void process(java.util.List<Integer> chunks) {
                int latestProgress = chunks.get(chunks.size() - 1);
                if (latestProgress >= 0) 
                {
                    updateProgress(latestProgress);
                }
            }
        };
        downloadWorker.execute();
    }

    public void updateProgress(int progress)
    {
        progressBar.setValue(progress);
        if (progress >= 100) {
            progressLabel.setText("Download complete!");
        } else {
            progressLabel.setText("Downloading...");
        }
    }
}