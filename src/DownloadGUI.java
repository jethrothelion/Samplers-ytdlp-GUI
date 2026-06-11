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
    private double distanceFromLeft;
    private double distanceFromRight;
    private JLabel progressLabel;
    private JButton downloadBtn;
    TimelineRangeSelector timelineSelector;

    private JTextArea logArea;
    private JScrollPane logScrollPane;
    private JButton toggleLogBtn;
    private int savedLogHeight = 100;

    private int videoDuration = -1; // Duration in seconds, -1 means unknown
    private boolean hasVerifiedExecutables = false; // Flag to prevent duplicate checks

    private DependencyLocator locator = new DependencyLocator();
    private DownloadManager downloader;
    
    // prevent spamming yt-dlp processes
    private Timer urlDebounceTimer; 

    public void initialization()
        {
            // Basic window setup
            setTitle("Youtube Downloader");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.BOTH;

            // --- ROW 0: URL Input, Media Type, Folders & Download ---
            
            // CREATION
            urlField = new JTextField("Enter URL");
            
            JButton savePrefBttn = new JButton();
            JPanel saveButtonPanel = new JPanel(new GridBagLayout());
            
            JPanel typePanel = new JPanel(new GridLayout(2, 1)); // Reverted back to GridLayout to stack vertically
            videoBtn = new JRadioButton("Video");
            audioBtn = new JRadioButton("Audio");
            ButtonGroup typeGroup = new ButtonGroup();
            
            JPanel gapPanel = new JPanel();
            JButton folderSelectButton = new JButton("Select Folder");
            downloadBtn = new JButton("DOWNLOAD");

            // POSISTIONING
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 4;
            gbc.weightx = 0.7;
            gbc.ipady = 40; // Makes the text box taller
            add(urlField, gbc);

            gbc.gridx = 4;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.ipady = 0; // Reset padding
            add(typePanel, gbc);
            
            gbc.gridx = 5;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            add(saveButtonPanel, gbc);

            gbc.gridx = 6;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            add(gapPanel, gbc);

            gbc.gridx = 7;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.weighty = 0;
            add(folderSelectButton, gbc);

            gbc.gridx = 8;
            gbc.gridwidth = 1;
            gbc.weightx = 0.2;
            gbc.weighty = 0;    
            add(downloadBtn, gbc);

            // TOP ROW PROPERTIES
            
            // URL input box
            urlField.setBorder(new LineBorder(Color.BLACK, 2));
            urlField.setForeground(Color.GRAY);    
            
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
                public void insertUpdate(DocumentEvent e) { urlDebounceTimer.restart(); }
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

            // Save button
            Icon saveIcon = locator.getIcon("saveIcon.png");
            savePrefBttn.setIcon(saveIcon);
            savePrefBttn.setPreferredSize(new Dimension(20, 30));
            saveButtonPanel.add(savePrefBttn);
            savePrefBttn.addActionListener(e -> saveConfig());
            
            // Gap Panel
            gapPanel.setPreferredSize(new Dimension(80, 1));

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
                        String message = "Selected folder: " + selectedDirectory;
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

            // Download Button
            downloadBtn.setForeground(new Color(0, 0, 0));
            downloadBtn.setBorder(new LineBorder(new Color(0, 153, 51), 3));
            downloadBtn.setFont(new Font("Arial", Font.BOLD, 16));
            downloadBtn.setBackground(Color.GREEN);
            downloadBtn.setPreferredSize(new Dimension(5, 30));
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
            gbc.weightx = 1.0; // Overrides the 0.2 carried over from the new downloadBtn position
            gbc.weighty = 0.0; // Reset just in case
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
            // Removed explicit weighty here; it inherently carries over 0.3 from qualityPanel like the old code.
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
            JPanel logWrapperPanel = new JPanel(new BorderLayout());
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
                boolean isCurrentlyVisible = logScrollPane.isVisible();
                GridBagLayout layout = (GridBagLayout) getContentPane().getLayout();
                GridBagConstraints logGbc = layout.getConstraints(logWrapperPanel);

                if (isCurrentlyVisible) {
                    savedLogHeight = logScrollPane.getHeight();
                    logScrollPane.setVisible(false);
                    toggleLogBtn.setText("Show Console");
                    logGbc.weighty = 0.0;
                    layout.setConstraints(logWrapperPanel, logGbc);
                    setSize(getWidth(), getHeight() - savedLogHeight); 
                } else {
                    logScrollPane.setVisible(true);
                    toggleLogBtn.setText("Hide Console");
                    logGbc.weighty = 0.5;
                    layout.setConstraints(logWrapperPanel, logGbc);
                    setSize(getWidth(), getHeight() + savedLogHeight);
                }
                revalidate();
                repaint();
            });
            
            buttonWrapper.add(toggleLogBtn);
            logWrapperPanel.add(buttonWrapper, BorderLayout.NORTH);
            logWrapperPanel.add(logScrollPane, BorderLayout.CENTER);


            // --- FINAL SETTING RUNNERS & INITIALIZATION ---
            
            constructCommand();
            setupCommandBarListeners();
            readConfig();

            // Run verification
            runStartupChecks();
            
            setLocationRelativeTo(null);
            setVisible(true);
        }
    
        
    String configPath = System.getProperty("user.home") + File.separator + "YoutubeDownloaderConfig.properties";
    public void saveConfig()
    {
        Properties prop = new Properties();

        
        try (FileOutputStream out = new FileOutputStream(configPath)) 
        {
            // Get the propertys from the radio buttons

            //quality
            String quality = "3"; // Defaults to highest
            if (mediumButton.isSelected()) quality = "2";
            if (lowestButton.isSelected()) quality = "1";

            //video or audio
            String type = "audio"; // Defaults to audio
            if (videoBtn.isSelected()) type = "video";
            if (audioBtn.isSelected()) type = "audio";



            //  Add properties to the object
            prop.setProperty("quality", quality);

            if (selectedDirectory != null) 
            {
                prop.setProperty("directory", selectedDirectory);
            }

            prop.setProperty("type", type);

            prop.setProperty("windowWidth", String.valueOf(getWidth()));
            prop.setProperty("windowHeight", String.valueOf(getHeight()));

            // Save the file to disk
            prop.store(out, "User Preferences");
            System.out.println("Settings saved to config.properties");
            logArea.append("Settings saved to config.properties");

        } catch (IOException e) {
            e.printStackTrace();
            logArea.append("Failure to save settings: " + e.toString() + "\n");
            popupMessage("Failed to save settings.");
        }

        
    }
    public void readConfig()
    {

        Properties prop = new Properties();
        // Load the file
        try (FileInputStream in = new FileInputStream(configPath)) {
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
            File savedDir = new File(prop.getProperty("directory", ""));
            if(savedDir.exists() && savedDir.isDirectory())
            {   
                
                selectedDirectory = prop.getProperty("directory");
            }

            //type
            String type = prop.getProperty("type", "audio");
            if (type.equals("audio")) audioBtn.setSelected(true);
            if (type.equals("video")) videoBtn.setSelected(true);

                        
            // Load window dimensions, with a safety fallback to 900x550
            try {
                int width = Integer.parseInt(prop.getProperty("windowWidth", "900"));
                int height = Integer.parseInt(prop.getProperty("windowHeight", "550"));
                setSize(width, height);
            } catch (NumberFormatException ex) {
                setSize(900, 550);
            }

            // Refresh the command bar to reflect loaded settings
            constructCommand();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("No config file found, using defaults.");
            logArea.append("No config file found, using defaults. \n");
            setSize(900,550);

            videoBtn.setSelected(true);
        }


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

        StringBuffer fileName = new StringBuffer("%(title)s.%(ext)s");

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
            command.add(File.separator + selectedDirectory + File.separator + "\"" + fileName.toString() + "\"" + File.separator);
        } else {
            command.add("-o");
            command.add("\"" + fileName.toString() + "\"");
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
                downloader.Download(command, new DownloadListener()
                {
                    @Override
                    public void onProgress(int progress) {
                        publish(progress);
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
                                logArea.append("--- DOWNLOAD COMPLETE ---\n");
                                popupMessage("Download completed successfully!");
                                progressBar.setValue(0);
                            } else {    
                                logArea.append("--- DOWNLOAD FAILED ---\n");
                                popupMessage("Download failed, Check the console for details.");
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
