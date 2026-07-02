import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.net.URI;
        

public class DownloadGUI extends JFrame
// Too long of a class that makes the GUI and handles some logic as well such as config and
// construct command and get video length
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
    private JLabel progressLabel;
    private JButton downloadBtn;
    TimelineRangeSelector timelineSelector;

    private JTextArea logArea;
    private JScrollPane logScrollPane;
    private JPanel logWrapperPanel;
    private JButton toggleLogBtn;
    boolean logCurrentlyVisible = true;
    private int savedLogHeight = 100;

    private int videoDuration = -1; // Duration in seconds, -1 means unknown
    private boolean hasVerifiedExecutables = false; // Flag to prevent duplicate checks

    private DependencyLocator locator = new DependencyLocator();
    private DownloadManager downloader;
    
    // prevent spamming yt-dlp processes
    private Timer urlDebounceTimer; 

    // settings variables
    boolean autoStart = false;
    boolean openWhenDone = false;
    boolean popUp = true;

    public void initialization()
        {
            
            // Basic window setup
            setTitle("Youtube Downloader");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.BOTH;
            // window sizing load
            try 
            {
                int width = Integer.parseInt(ConfigManager.getInstance().getProperty("windowWidth", "900"));
                int height = Integer.parseInt(ConfigManager.getInstance().getProperty("windowHeight", "550"));
             setSize(width, height);
            } catch (NumberFormatException ex) {
                setSize(900, 550);
            }


            // --- ROW 0: URL Input, Media Type, Folders & Download ---
            
            // CREATION
            JButton settingsBttn = new JButton();
            JButton savePrefBttn = new JButton();

            JPanel settingsButtonPanel = new JPanel(new GridLayout(2, 1));

            urlField = new JTextField("Enter URL");
            
            JPanel typePanel = new JPanel(new GridLayout(2, 1)); 
            videoBtn = new JRadioButton("Video");
            audioBtn = new JRadioButton("Audio");
            ButtonGroup typeGroup = new ButtonGroup();
            
            JPanel gapPanel = new JPanel();

            JPanel folderWrapperPanel = new JPanel(new BorderLayout());
            JButton folderSelectButton = new JButton("Select Folder");
            JLabel openFolderLabel = new JLabel("<html><a href=''>Open folder</a></html>");
            JPanel folderLinkPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

            downloadBtn = new JButton("DOWNLOAD");

            // POSISTIONING
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.ipady = 0; 
            add(settingsButtonPanel, gbc);

            gbc.gridx = 1;
            gbc.gridwidth = 4;
            gbc.weightx = 0.1; 
            gbc.ipady = 40; // Makes the text box taller
            add(urlField, gbc);

            gbc.gridx = 5;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.ipady = 0; // Reset padding
            add(typePanel, gbc);
            
            gbc.gridx = 6;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            add(gapPanel, gbc);

            gbc.gridx = 7;
            gbc.gridwidth = 1;
            gbc.weightx = 0; 
            gbc.weighty = 0;
            add(folderWrapperPanel, gbc);

            gbc.gridx = 8;
            gbc.gridwidth = 1;
            gbc.weightx = 0.2;
            gbc.weighty = 0;    
            add(downloadBtn, gbc);

            // TOP ROW PROPERTIES

            // Settings button
            ImageIcon originalSettingsIcon = (ImageIcon) locator.getIcon("settingsIcon.png");
            Image scaledSettingsImg = originalSettingsIcon.getImage().getScaledInstance(30, 25, Image.SCALE_SMOOTH);
            Icon settingsIcon = new ImageIcon(scaledSettingsImg);
            settingsBttn.setIcon(settingsIcon);
            settingsBttn.setMargin(new Insets(0, 0, 0, 0));
            settingsButtonPanel.add(settingsBttn);
            settingsBttn.addActionListener(e -> openSettings());

            // Save button
            ImageIcon originalSaveIcon = (ImageIcon) locator.getIcon("saveIcon.png");
            Image scaledSaveImg = originalSaveIcon.getImage().getScaledInstance(30, 25, Image.SCALE_SMOOTH);
            Icon saveIcon = new ImageIcon(scaledSaveImg);
            savePrefBttn.setIcon(saveIcon);
            savePrefBttn.setMargin(new Insets(0, 0, 0, 0)); // CHANGED: Removes default inner button padding to shrink width/height
            savePrefBttn.setPreferredSize(new Dimension(30, 25));
            settingsButtonPanel.add(savePrefBttn);
            savePrefBttn.addActionListener(e -> saveConfig());

            // URL input box
            urlField.setBorder(new LineBorder(Color.BLACK, 2));
            urlField.setForeground(Color.GRAY);    
            urlField.setPreferredSize(new Dimension(315, 60));

            urlDebounceTimer = new Timer(1000, e -> {
                if (urlField.getText().contains("youtube.com"))
                {
                    fetchVideoDuration(urlField.getText()); 
                }
            });
            urlDebounceTimer.setRepeats(false);
            
            urlField.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) { urlDebounceTimer.restart(); }
                public void removeUpdate(DocumentEvent e) { urlDebounceTimer.restart(); }
                //AUTO start setting starts download on inputed url
                public void insertUpdate(DocumentEvent e) { 
                    urlDebounceTimer.restart();
                    if(autoStart && urlField.getText().contains("https")) startDownload(commandBar.getText());}
            });
            
            urlField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    if (urlField.getText().equals("Enter URL")) {
                        urlField.setText("");
                        urlField.setForeground(Color.BLACK);
                    }
                }
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    if (urlField.getText().isEmpty()) {
                        urlField.setForeground(Color.GRAY);
                        urlField.setText("Enter URL");
                    }
                }
            });
            
            // Type Panel
            typeGroup.add(videoBtn);
            typeGroup.add(audioBtn);
            typePanel.add(videoBtn);
            typePanel.add(audioBtn);

            // Gap Panel
            gapPanel.setMinimumSize(new Dimension(100, 1));

            // Folder Select Button
            Icon folderIcon = locator.getIcon("folderIcon.png");
            folderSelectButton.setIcon(folderIcon);
            folderSelectButton.setBorder(new LineBorder(Color.BLACK, 2));
            folderSelectButton.setFont(new Font("Arial", Font.PLAIN, 12));
            folderSelectButton.setBackground(Color.WHITE);  
            folderSelectButton.setForeground(Color.BLACK);
            folderSelectButton.setPreferredSize(new Dimension(110, 50));
            folderSelectButton.setMinimumSize(new Dimension(110, 50));
            folderSelectButton.addActionListener(e -> {
                // Try to swap look and feel for just file explorer
                try {
                    LookAndFeel previousLF = UIManager.getLookAndFeel();
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    JFileChooser chooser = new JFileChooser();
                    chooser.setCurrentDirectory(new java.io.File(""));
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int result = chooser.showOpenDialog(folderSelectButton);
                    UIManager.setLookAndFeel(previousLF);
                    
                    if (result == JFileChooser.APPROVE_OPTION) {
                        selectedDirectory = chooser.getSelectedFile().getAbsolutePath();
                        String message = "Selected folder: " + selectedDirectory + "\n";
                        System.out.println(message);
                        logArea.append(message);
                    } else {
                        System.out.println("No folder selected");
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to swap Style, Look and Feel for file chooser.");
                    logArea.append("Failed to swap Style, Look and Feel for file chooser.");
                    ex.printStackTrace();
                }
                constructCommand();
            });
            //Hyper link open selected folder in explorer
            openFolderLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            openFolderLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            openFolderLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    try {
                        if (selectedDirectory != null && !selectedDirectory.isEmpty()) {
                            File dirToOpen = new File(selectedDirectory);
                            if (dirToOpen.exists()) {
                                Desktop.getDesktop().open(dirToOpen); // Opens the folder in OS file explorer
                            } else {
                                popupMessage("The selected folder no longer exists.");
                            }
                        } else {
                            popupMessage("Please select a folder first.");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        logArea.append(ex.toString() +"\n");
                        popupMessage("Could not open the folder, sorry: " + ex); 
                    }
                }
            });

            folderLinkPanel.add(openFolderLabel);
            folderWrapperPanel.add(folderSelectButton, BorderLayout.CENTER);
            folderWrapperPanel.add(folderLinkPanel, BorderLayout.SOUTH);


            // Download Button
            downloadBtn.setForeground(new Color(0, 0, 0));
            downloadBtn.setBorder(new LineBorder(new Color(0, 153, 51), 3));
            downloadBtn.setFont(new Font("Arial", Font.BOLD, 16));
            downloadBtn.setBackground(Color.GREEN);
            downloadBtn.setPreferredSize(new Dimension(36, 30));
            downloadBtn.addActionListener(e -> startDownload(commandBar.getText()));


            // --- ROW 1: Command Bar Execution Display ---
            
            // CREATION
            commandBar = new JTextField();
            JScrollPane commandScrollPane = new JScrollPane(commandBar, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            JPanel commandWrapperPanel = new JPanel(new BorderLayout());
            JLabel hyperlinkLabel = new JLabel("<html><a href=''>More parameters</a></html>");
            JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

            // POSISTIONING
            gbc.gridx = 0;
            gbc.gridy = 1; 
            gbc.gridwidth = 9;
            gbc.gridheight = 1;
            gbc.weightx = 1.0; 
            gbc.weighty = 0.0; 
            gbc.ipady = 15;
            add(commandWrapperPanel, gbc);

            // PROPERTIES
            commandBar.setBorder(new LineBorder(Color.BLUE, 2));
            commandBar.setFont(new Font("Monospaced", Font.PLAIN, 14));
            
            commandScrollPane.setPreferredSize(new Dimension(400, 50));
            commandScrollPane.setMinimumSize(new Dimension(100, 50));
            
            hyperlinkLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            hyperlinkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            hyperlinkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    try {
                        Desktop.getDesktop().browse(new URI("https://github.com/yt-dlp/yt-dlp"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        logArea.append(ex.toString() +"\n");
                        popupMessage("Could not open the link, sorry: " + ex); 
                    }
                }
            });

            linkPanel.add(hyperlinkLabel);
            commandWrapperPanel.add(commandScrollPane, BorderLayout.CENTER);
            commandWrapperPanel.add(linkPanel, BorderLayout.SOUTH);


            // --- ROW 2: Quality Panel & Timeline Range Selector ---

            // CREATION
            JPanel qualityPanel = new JPanel();
            highestButton = new JRadioButton("Highest quality");
            mediumButton = new JRadioButton("Medium quality");
            lowestButton = new JRadioButton("Lowest quality");
            ButtonGroup qualityGroup = new ButtonGroup();
            
            timelineSelector = new TimelineRangeSelector();

            // POSISTIONING
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 4;
            gbc.weightx = 0.4;
            gbc.weighty = 0.3;
            gbc.ipady = 0; // Ensure reset
            add(qualityPanel, gbc);

            gbc.gridx = 4;
            gbc.gridy = 2;
            gbc.gridwidth = 5;
            gbc.weightx = 0.6;
            // Removed explicit weighty here; it inherently carries over 0.3 from qualityPanel
            add(timelineSelector, gbc);

            // PROPERTIES
            qualityPanel.setBorder(new LineBorder(Color.BLACK, 2));
            qualityPanel.setLayout(new BoxLayout(qualityPanel, BoxLayout.Y_AXIS));
            
            highestButton.setActionCommand("3");
            highestButton.setSelected(true); 
            mediumButton.setActionCommand("2");
            lowestButton.setActionCommand("1");

            qualityGroup.add(lowestButton); 
            qualityGroup.add(mediumButton); 
            qualityGroup.add(highestButton);

            qualityPanel.add(highestButton); 
            qualityPanel.add(mediumButton); 
            qualityPanel.add(lowestButton);


            // --- ROW 3: Status and Progress Bar ---

            // CREATION
            JPanel statusContainer = new JPanel(new BorderLayout(5, 5));
            JPanel statusBox = new JPanel(new BorderLayout());
            JLabel statusLabel = new JLabel("download status", SwingConstants.LEFT);
            downloadMessage = new JLabel("inactive", SwingConstants.CENTER);  
            
            JPanel progressBox = new JPanel(new BorderLayout());
            progressLabel = new JLabel("progress bar", SwingConstants.LEFT);
            progressBar = new JProgressBar();

            // POSISTIONING
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 9;
            gbc.weightx = 0.4; // Explicitly set to 0.4 to perfectly mimic the carry-over from qualityPanel in your original code
            gbc.weighty = 0.1;
            add(statusContainer, gbc);

            // PROPERTIES
            downloadMessage.setBorder(new LineBorder(Color.BLACK, 2));
            downloadMessage.setPreferredSize(new Dimension(100, 50));
            statusBox.add(statusLabel, BorderLayout.NORTH);
            statusBox.add(downloadMessage, BorderLayout.CENTER);

            progressBar.setBorder(new LineBorder(Color.BLACK, 2));
            progressBar.setPreferredSize(new Dimension(400, 30));
            progressBox.add(progressLabel, BorderLayout.NORTH);
            progressBox.add(progressBar, BorderLayout.CENTER);

            statusContainer.add(statusBox, BorderLayout.WEST);
            statusContainer.add(progressBox, BorderLayout.CENTER);


            // --- ROW 4: Output / Log Console Area ---

            // CREATION
            logWrapperPanel = new JPanel(new BorderLayout());
            logArea = new JTextArea();
            logScrollPane = new JScrollPane(logArea);
            
            JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            toggleLogBtn = new JButton("Hide Console");

            // POSISTIONING
            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.gridwidth = 9;
            // weightx naturally carries over as 0.4 from Row 3, matching old behavior
            gbc.weighty = 0.5; // Allows console expansion
            add(logWrapperPanel, gbc);

            // PROPERTIES
            logArea.setEditable(false);
            logArea.setLineWrap(true);
            logArea.setWrapStyleWord(true);
            logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            logArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            logScrollPane.setPreferredSize(new Dimension(800, 100));
            
            toggleLogBtn.setFont(new Font("Arial", Font.PLAIN, 10));
            toggleLogBtn.setFocusPainted(false);
            toggleLogBtn.setMargin(new Insets(2, 5, 2, 5));
            
            toggleLogBtn.addActionListener(e -> {
                setLogVisibility(!logCurrentlyVisible);
            });
            
            buttonWrapper.add(toggleLogBtn);
            logWrapperPanel.add(buttonWrapper, BorderLayout.NORTH);
            logWrapperPanel.add(logScrollPane, BorderLayout.CENTER);


            // --- FINAL SETTING RUNNERS & INITIALIZATION ---
            
            constructCommand();
            setupCommandBarListeners();
            readConfig();

            runStartupChecks();
            
            setLocationRelativeTo(null);
            setVisible(true);
        }
    
    public void openSettings()
    {
    // Dimming layer
    JPanel dimmingPanel = new JPanel() 
    {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            g.setColor(new Color(0, 0, 0, 200)); 
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    };
    dimmingPanel.setOpaque(false);
    setGlassPane(dimmingPanel);
    dimmingPanel.setLayout(new java.awt.GridBagLayout());
    dimmingPanel.setVisible(true);
    
    // Notify in settings text
    JLabel youInSettings = new JLabel("You are in the settings btw");
    youInSettings.setSize(300, 200);
    youInSettings.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
    youInSettings.setForeground(Color.PINK);
    dimmingPanel.add(youInSettings);
    
   // run settings window
    SettingsWindow settings = new SettingsWindow();
    settings.setOutputListener(createLogListener("YTDLP Update"));
    settings.initialization();

    // Clear Dimming layer
    dimmingPanel.setVisible(false);

    readConfig();
    constructCommand();
    }
        
    public void saveConfig()
    {
        ConfigManager config = ConfigManager.getInstance();

        // Quality
        String quality = "3"; 
        if (mediumButton.isSelected()) quality = "2";
        if (lowestButton.isSelected()) quality = "1";
        config.setProperty("quality", quality);

        // Directory
        if (selectedDirectory != null) {
            config.setProperty("directory", selectedDirectory);
        }

        // Type
        String type = "audio"; 
        if (videoBtn.isSelected()) type = "video";
        config.setProperty("type", type);

        // Window Size
        config.setProperty("windowWidth", String.valueOf(getWidth()));
        config.setProperty("windowHeight", String.valueOf(getHeight()));

        if(logCurrentlyVisible) config.setProperty("logVisibility", "true");
        if(!logCurrentlyVisible) config.setProperty("logVisibility", "false");

        config.save();
        logArea.append("Settings saved to config.properties\n");
    }

    public void readConfig()
    {
        ConfigManager config = ConfigManager.getInstance();

        // Quality
        String quality = config.getProperty("quality", "3"); 
        if (quality.equals("1")) lowestButton.setSelected(true);
        else if (quality.equals("2")) mediumButton.setSelected(true);
        else highestButton.setSelected(true);

        // Directory
        File savedDir = new File(config.getProperty("directory", ""));
        if(savedDir != null && savedDir.exists() && savedDir.isDirectory()) {   
            selectedDirectory = config.getProperty("directory", "");
        }

        // Type
        String type = config.getProperty("type", "audio");
        if (type.equals("audio")) audioBtn.setSelected(true);
        if (type.equals("video")) videoBtn.setSelected(true);

        // Log area visibility
        String logVisibilityCheck = config.getProperty("logVisibility", "True");
        if(logVisibilityCheck.equals("true")) logCurrentlyVisible = true;
        if(logVisibilityCheck.equals("false")) logCurrentlyVisible = false;
        
        // Auto start download on input into URLfield
        String stringAutoStart = config.getProperty("autoStart", "false");
        if(stringAutoStart != null) 
        {   
            autoStart = Boolean.parseBoolean(stringAutoStart);
        }

        // Open folder when done 
        String stringOpenWhenDone = config.getProperty("openWhenDone", "false");
        if(stringOpenWhenDone != null)
        {
            openWhenDone = Boolean.parseBoolean(stringOpenWhenDone);
        }

        String stringPopUp = config.getProperty("popUp", "true");
        if(stringPopUp != null)
        {
            popUp = Boolean.parseBoolean(stringPopUp);
        }

        constructCommand();
    }

    private void setLogVisibility(boolean makeVisible) {
        if (makeVisible == logCurrentlyVisible) return; //Initial check if already has been set by config
        GridBagLayout layout = (GridBagLayout) getContentPane().getLayout();
        GridBagConstraints logGbc = layout.getConstraints(logWrapperPanel);


        if (makeVisible) 
        {
            logScrollPane.setVisible(true);
            logArea.setVisible(true); 
            toggleLogBtn.setText("Hide Console");
            logGbc.weighty = 0.5;
            
            // Only adjust window size if the window is currently being displayed on the screen
            if (isVisible()) {
                setSize(getWidth(), getHeight() + savedLogHeight);
            }
            logCurrentlyVisible = true;
        }
        if (!makeVisible)
        {
            // Only save height if it actually exists to prevent the squish 
            if (logScrollPane.getHeight() > 0) {
                savedLogHeight = logScrollPane.getHeight(); 
            }
            logScrollPane.setVisible(false);
            logArea.setVisible(false);
            toggleLogBtn.setText("Show Console");
            logGbc.weighty = 0.0;
            
            // Only adjust window size if the window is currently being displayed on the screen
            if (isVisible()) {
                setSize(getWidth(), getHeight() - savedLogHeight); 
            }
            logCurrentlyVisible = false;
        }
        
        layout.setConstraints(logWrapperPanel, logGbc);
        revalidate();
        repaint();
    }

    // Dedicated method to ensure checks only run once
    private void runStartupChecks() {
        if (hasVerifiedExecutables) {
            return; // Exit immediately if already checked
        }
        
        System.out.println("Running initial dependency checks...");
        verifyExecutable(locator.getYtdlpPath(), "--version", "yt-dlp");
        verifyExecutable(locator.getFFmpegPath(), "-version", "ffmpeg");
        
        hasVerifiedExecutables = true; // Mark as done so it never runs again
    }

    public boolean verifyExecutable(String path, String versionFlag, String appName)
    {
        // Check using the safe buffer-clearing method
        if (locator.checkProcessSilently(path, versionFlag)) {
            return true;
        }
        String message = "Warning: Could not verify " + appName + ".\nIt may not be in your system PATH or application folder. If downloads fail, please ensure it is installed.\n";
        popupMessage(message);
        logArea.append(message);
        return false;
    }

    private void fetchVideoDuration(String url) {
        SwingWorker<Integer, Void> durationWorker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception 
            {
                try {
                    ProcessBuilder pb = new ProcessBuilder(locator.getYtdlpPath(), url, "--print", "%(duration)s");
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
                    logArea.append("Error in duration fetch: " + e.getMessage() + "\n");
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

        StringBuffer fileName = new StringBuffer("%(title), %(author)");

        command.add(locator.getYtdlpPath()); // Add the executable path first

        String ffmpegLocation = locator.getFFmpegPath();
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

        
        if (audioBtn.isSelected())
        {
            command.add("-x");
            command.add("--audio-format");
            command.add("mp3");
        }
        
        //if not full range selected
        if(!timelineSelector.isFullRangeSelected())
        {
            String length = (int)(timelineSelector.getStartTime()) + "-" + (int)(timelineSelector.getEndTime());

            command.add("--force-keyframes-at-cuts");
            command.add("--download-sections");
            command.add("\"*" + length + "\"");

            fileName.append(length);

            
        }

        //quality options
        if (highestButton.isSelected()) {
            command.add("-f");
            command.add(audioBtn.isSelected() ? "bestaudio" : "bestvideo+bestaudio");
        } else if (mediumButton.isSelected()) {
            command.add("-f");
            command.add(audioBtn.isSelected() ? "bestaudio.2" : "bestvideo.2+bestaudio.2");
        } else if (lowestButton.isSelected()) {
            command.add("-f");
            command.add(audioBtn.isSelected() ? "worstaudio" : "worstvideo+worstaudio");
        }

        if(selectedDirectory != null && !selectedDirectory.isEmpty())
        {
            command.add("-o");
            command.add(File.separator + selectedDirectory + File.separator + "\"" + fileName.toString() + "\"");
        } else {
            command.add("-o");
            command.add("\"" + fileName.toString() + "\"");
        }
        
        commandBar.setText(String.join(" ", command).trim());  
    }
    
    private void setupCommandBarListeners()
    {
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

        timelineSelector.addPropertyChangeListener("rangeChanged", evt -> constructCommand());
    }

    public void popupMessage(String message)
    {
        JOptionPane.showMessageDialog(this, message);
    }

    private void changeDownloadButton(boolean state)
    {
        // Loop through and remove all old click events so they don't stack up
        for (java.awt.event.ActionListener al : downloadBtn.getActionListeners()) {
            downloadBtn.removeActionListener(al);
        }
        if(state == false)
        {

            downloadBtn.setText("ABORT");
            downloadBtn.setForeground(Color.white);
            downloadBtn.setBackground(new Color(200, 0, 0));
            downloadBtn.setBorder(new LineBorder(new Color(150, 0, 0), 3));
            downloadBtn.addActionListener(e ->
                {downloader.abortDownload();
                progressLabel.setText("Aborting or aborted");
            });
                
        }

        if(state == true)
        {
            downloadBtn.setText("DOWNLOAD");
            downloadBtn.setForeground(new Color(0, 0, 0));
            downloadBtn.setBackground(Color.GREEN);
            downloadBtn.setBorder(new LineBorder(new Color(0, 153, 51), 3));
            downloadBtn.addActionListener(e -> {startDownload(commandBar.getText());});
        };

    }
    
    public DownloadListener createLogListener(String taskString)
    {
        return new DownloadListener() {
            @Override
            public void onProgress(int progress) {
                SwingUtilities.invokeLater(() -> updateProgress(progress));
            }

            @Override
            public void onError(String errorMessage) {
                logArea.append("ERROR: " + errorMessage + "\n");

                logArea.setCaretPosition(logArea.getDocument().getLength());
            }

            @Override
            public void onComplete(boolean success) {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        logArea.append("--- " + taskString + " COMPLETE ---\n");
                        if(popUp){popupMessage(taskString + " completed successfully!");}

                        progressBar.setValue(0);
                    } else {    
                        logArea.append("--- " + taskString + " FAILED ---\n");
                        if(popUp){popupMessage( taskString + " failed, Check the console for details.");}
                        
                        progressBar.setValue(0);
                    }
                    changeDownloadButton(true);    
                });
            }

            @Override
            public void onOutput(String output) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(output + "\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }

            // Message is progress state from ran application
            @Override
            public void onMessage(String message) {
                SwingUtilities.invokeLater(() -> downloadMessage.setText(message));
            }
        };
    }
    

    public void startDownload(String command)
    {
        String commandMsg = "Starting download with command: " + command;
        System.out.println(commandMsg);
        logArea.append(commandMsg);
        
        changeDownloadButton(false);

        SwingWorker<Void, Integer> downloadWorker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception 
            {
                downloader = new DownloadManager();
                downloader.Download(command, createLogListener("Video Download"), true, true);

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
