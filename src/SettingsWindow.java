import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class SettingsWindow extends JDialog
{
    private CardLayout cardLayout;
    private JPanel cardPanel;

    private JTextField defaultDirField;
    private JTextField ytdlpPathField;
    private JTextField ffmpegPathField;
    private JTextField customFlagsField;
    private JTextField cookiesFileField;
    private JButton browseDirBtn;
    private JButton browseCookiesBtn;
    private JButton updateYTDLP;

    private JComboBox<String> themeDropdown;

    private JCheckBox autoStartCheckbox;
    private JCheckBox openWhenDoneCheckBox;
    private JCheckBox popUpCheckBox;
    private JCheckBox windowDimensionSaveCheckBox;

    private JButton saveBtn;
    private JButton closeBtn;

    String configPath = System.getProperty("user.home") + File.separator + "YoutubeDownloaderConfig.properties";
    DownloadManager downloader = new DownloadManager();
    DependencyLocator locator = DependencyLocator.getInstance();
    private DownloadListener outputListener;

    public void initialization()
    {
        setTitle("Settings");
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); 
        setModal(true);

        setLayout(new BorderLayout());

        // --- CREATION: Category Sidebar (Left) ---
        JPanel sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        sidebarPanel.setBackground(new Color(230, 230, 230));

        JButton pathsBtn = createCategoryButton("Paths");
        JButton appearanceBtn = createCategoryButton("Appearance");
        JButton behaviorBtn = createCategoryButton("Behavior");

        sidebarPanel.add(pathsBtn);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacing
        sidebarPanel.add(appearanceBtn);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sidebarPanel.add(behaviorBtn);

        // --- CREATION: Card Panel (Right) ---
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Add our modular panels to the card deck
        cardPanel.add(createPathsPanel(), "Paths");
        cardPanel.add(createAppearancePanel(), "Appearance");
        cardPanel.add(createBehaviorPanel(), "Behavior");

        // Action Listeners to flip the cards
        pathsBtn.addActionListener(e -> cardLayout.show(cardPanel, "Paths"));
        appearanceBtn.addActionListener(e -> cardLayout.show(cardPanel, "Appearance"));
        behaviorBtn.addActionListener(e -> cardLayout.show(cardPanel, "Behavior"));

        // --- CREATION: Bottom Action Buttons ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveBtn = new JButton("SAVE");
        closeBtn = new JButton("CLOSE");

        saveBtn.setBackground(Color.GREEN);
        saveBtn.setBorder(new LineBorder(new Color(0, 153, 51), 3));
        saveBtn.addActionListener(e -> saveConfig());

        closeBtn.setBackground(new Color(200, 0, 0));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBorder(new LineBorder(new Color(150, 0, 0), 3));
        closeBtn.addActionListener(e -> dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(closeBtn);

        // --- POSITIONING: Assembly ---
        add(sidebarPanel, BorderLayout.WEST);
        add(cardPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- FINAL SETTING RUNNERS ---
        readConfig();
        setSize(650, 350); // Made slightly wider to accommodate the sidebar
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Helper method to keep styling consistent for sidebar buttons
    private JButton createCategoryButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(120, 40));
        btn.setMaximumSize(new Dimension(120, 40));
        btn.setBackground(Color.WHITE);
        btn.setBorder(new LineBorder(Color.BLACK, 1));
        btn.setFocusPainted(false);
        return btn;
    }

    // ==========================================================
    // RIGHT SIDE MODULE PANELS 
    // ==========================================================

    private JPanel createPathsPanel()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;

        // --- Default Directory ---
        JLabel dirLabel = new JLabel("Default Folder:");
        defaultDirField = new JTextField();
        browseDirBtn = new JButton("Browse");

        defaultDirField.setBorder(new LineBorder(Color.BLACK, 2));
        browseDirBtn.setBorder(new LineBorder(Color.BLACK, 2));
        browseDirBtn.setBackground(Color.WHITE);
        
        browseDirBtn.addActionListener(e -> {
            try {
                LookAndFeel previousLF = UIManager.getLookAndFeel();
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    defaultDirField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
                UIManager.setLookAndFeel(previousLF);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(dirLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(defaultDirField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(browseDirBtn, gbc);

        // --- YT-DLP Path ---
        JLabel ytdlpLabel = new JLabel("YT-DLP Path:");
        ytdlpPathField = new JTextField();
        ytdlpPathField.setBorder(new LineBorder(Color.BLACK, 2));
        ytdlpPathField.setToolTipText("Full path to the yt-dlp executable. Leave as yt-dlp to use your system PATH.");
        
        updateYTDLP = new JButton("Update yt-dlp");
        updateYTDLP.setBorder(new LineBorder(Color.BLACK, 2));
        updateYTDLP.setBackground(Color.WHITE);
        
        updateYTDLP.addActionListener(e -> updateYTDLP());


        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(ytdlpLabel, gbc);
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 1.0;
        panel.add(ytdlpPathField, gbc);
        gbc.gridx = 2; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(updateYTDLP, gbc);

        // --- FFmpeg Path ---
        JLabel ffmpegLabel = new JLabel("FFmpeg Path:");
        ffmpegPathField = new JTextField();
        ffmpegPathField.setBorder(new LineBorder(Color.BLACK, 2));
        ffmpegPathField.setToolTipText("Full path to the ffmpeg executable. Leave as ffmpeg to use your system PATH.");

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(ffmpegLabel, gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(ffmpegPathField, gbc);

        // --- Custom Flags ---
        JLabel customFlags = new JLabel("Custom Flags:");
        customFlagsField = new JTextField();
        customFlagsField.setBorder(new LineBorder(Color.BLACK, 2));
        customFlagsField.setToolTipText("Extra yt-dlp flags added to the end of every command.");

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(customFlags, gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(customFlagsField, gbc);

        // --- Cookies File ---
        JLabel cookiesFile = new JLabel("Cookies File:");
        cookiesFileField = new JTextField();
        browseCookiesBtn = new JButton("Browse");

        cookiesFileField.setBorder(new LineBorder(Color.BLACK, 2));
        cookiesFileField.setToolTipText("Path to a cookies.txt file, for age restricted or members only videos. Can also name browser, chrome, safari or others");
        browseCookiesBtn.setBorder(new LineBorder(Color.BLACK, 2));
        browseCookiesBtn.setBackground(Color.WHITE);

        browseCookiesBtn.addActionListener(e -> {
            try {
                LookAndFeel previousLF = UIManager.getLookAndFeel();
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    cookiesFileField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
                UIManager.setLookAndFeel(previousLF);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(cookiesFile, gbc);
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 1.0;
        panel.add(cookiesFileField, gbc);
        gbc.gridx = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(browseCookiesBtn, gbc);


        // Blank space at bottom to push everything up
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 3; gbc.weighty = 1.0;
        panel.add(new JLabel(""), gbc);

        return panel;
    }

    private JPanel createAppearancePanel()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;

        JLabel themeLabel = new JLabel("Color Theme:");
        String[] themes = {"Light", "Dark", "System Default"};
        themeDropdown = new JComboBox<>(themes);
        themeDropdown.setBorder(new LineBorder(Color.BLACK, 2));
        themeDropdown.setBackground(Color.WHITE);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(themeLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(themeDropdown, gbc);

        // Blank space at bottom
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weighty = 1.0;
        panel.add(new JLabel(""), gbc);

        return panel;
    }

    private JPanel createBehaviorPanel()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL; // Horizontal so checkbox doesn't stretch weirdly

        // Auto start box
        autoStartCheckbox = new JCheckBox("Auto-start download when URL is pasted");
        autoStartCheckbox.setFocusPainted(false);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0;
        panel.add(autoStartCheckbox, gbc);

        // Open folder on download complete 
        openWhenDoneCheckBox = new JCheckBox("Open file in folder explorer on finished download");
        openWhenDoneCheckBox.setFocusPainted(false);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1.0;
        panel.add(openWhenDoneCheckBox, gbc);

        // Pop up notifaction on download complete
        popUpCheckBox = new JCheckBox("When done download have a pop up notifaction");
        popUpCheckBox.setFocusPainted(false);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 1.0;
        panel.add(popUpCheckBox, gbc);

        windowDimensionSaveCheckBox = new JCheckBox("Save the Dimension of the window when you close the app");
        windowDimensionSaveCheckBox.setFocusPainted(false);
        windowDimensionSaveCheckBox.setToolTipText("Remembers the window size and reopens the app at that size.");

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1.0;
        panel.add(windowDimensionSaveCheckBox, gbc);

        // Blank space at bottom
        gbc.gridx = 0; gbc.gridy = 4; gbc.weighty = 1.0;
        panel.add(new JLabel(""), gbc);

        return panel;
    }

    private void updateYTDLP()
    {
        // Stop the button being spammed while an update is already running
        updateYTDLP.setEnabled(false);
        updateYTDLP.setText("Updating...");

        SwingWorker<Boolean, Void> updateWorker = new SwingWorker<Boolean, Void>() {
            private final StringBuilder updateLog = new StringBuilder();
            private boolean succeeded = false;

            @Override
            protected Boolean doInBackground() throws Exception
            {
                // Built as a list so nothing has to be quoted, a path with spaces in it
                // used to get chopped back apart into seperate arguments
                List<String> command = new ArrayList<>();
                command.add(locator.getYtdlpPath()); // Same executable the downloads use
                command.add("-U");

                // Our own listener, the main window log sits behind this dialog and
                // may be hidden completely so nothing ever looked like it happened
                DownloadListener updateListener = new DownloadListener() {
                    @Override
                    public void onOutput(String output)
                    {
                        updateLog.append(output).append("\n");
                        if (outputListener != null) { outputListener.onOutput(output); }
                    }

                    @Override
                    public void onComplete(boolean success) { succeeded = success; }
                };

                downloader.Download(command, updateListener, false, false);
                return succeeded;
            }

            @Override
            protected void done()
            {
                updateYTDLP.setEnabled(true);
                updateYTDLP.setText("Update yt-dlp");

                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(SettingsWindow.this, "yt-dlp is up to date!");
                    } else {
                        JOptionPane.showMessageDialog(SettingsWindow.this, buildFailureMessage(updateLog.toString()),
                            "Update Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(SettingsWindow.this, "Update failed: " + e.getMessage(),
                        "Update Failed", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        };
        updateWorker.execute();
    }

    // Turns the captured yt-dlp output into something that explains what to do about it
    private String buildFailureMessage(String updateLog)
    {
        String lowerLog = updateLog.toLowerCase();

        if (lowerLog.contains("permission denied") || lowerLog.contains("access is denied"))
        {
            if (System.getProperty("os.name").toLowerCase().contains("window")) {
                return "Could not update yt-dlp, it does not have permission to write to itself.\n"
                     + "Close this and run the app as administrator, then try again.";
            }
            return "Could not update yt-dlp, it does not have permission to write to itself.\n"
                 + "Run 'sudo " + locator.getYtdlpPath() + " -U' in a terminal instead.";
        }

        // yt-dlp refuses -U when a package manager owns the install
        if (lowerLog.contains("not a valid executable") || lowerLog.contains("package manager"))
        {
            return "This copy of yt-dlp was installed by a package manager, so it cannot update itself.\n"
                 + "Update it the same way you installed it (pip, apt, brew, and so on).";
        }

        return "Could not update yt-dlp. Check the console for details.";
    }

    // ==========================================================
    // CONFIGURATION SAVING & LOADING
    // ==========================================================
    public void saveConfig()
    {
        ConfigManager config = ConfigManager.getInstance();

        // Checked before the new values overwrite them, so the locator is only sent
        // looking again when a path actually changed instead of on every single save
        boolean pathsChanged = !ytdlpPathField.getText().equals(config.getProperty("ytdlpPath", ""))
                            || !ffmpegPathField.getText().equals(config.getProperty("ffmpegPath", ""));

        // Paths
        config.setProperty("directory", defaultDirField.getText());
        config.setProperty("ytdlpPath", ytdlpPathField.getText());
        config.setProperty("ffmpegPath", ffmpegPathField.getText());
        config.setProperty("customFlags", customFlagsField.getText());
        config.setProperty("cookiesFile", cookiesFileField.getText());

        // Appearance & Behavior
        config.setProperty("theme", themeDropdown.getSelectedItem().toString());
        config.setProperty("autoStart", String.valueOf(autoStartCheckbox.isSelected()));
        config.setProperty("openWhenDone", String.valueOf(openWhenDoneCheckBox.isSelected()));
        config.setProperty("popUp", String.valueOf(popUpCheckBox.isSelected()));
        config.setProperty("windowDimensionSave", String.valueOf(windowDimensionSaveCheckBox.isSelected()));

        config.save(); // Writes everything to the file once

        // Make the locator forget what it found so the new paths take effect right away
        if (pathsChanged) { locator.clearCache(); }

        JOptionPane.showMessageDialog(this, "Settings Saved Successfully!");
        dispose(); 
    }

    public void readConfig()
    {
        ConfigManager config = ConfigManager.getInstance();

        // Paths
        defaultDirField.setText(config.getProperty("directory", ""));
        // Show what actually got resolved, not just what was typed, the locator already
        // prefers the saved path so these only differ when the saved one did not work
        ytdlpPathField.setText(locator.getYtdlpPath());
        ffmpegPathField.setText(locator.getFFmpegPath());
        customFlagsField.setText(config.getProperty("customFlags", ""));
        cookiesFileField.setText(config.getProperty("cookiesFile", ""));

        // Appearance
        themeDropdown.setSelectedItem(config.getProperty("theme", "Light"));

        // Behavior
        autoStartCheckbox.setSelected(Boolean.parseBoolean(config.getProperty("autoStart", "false")));
        openWhenDoneCheckBox.setSelected(Boolean.parseBoolean(config.getProperty("openWhenDone", "false")));
        popUpCheckBox.setSelected(Boolean.parseBoolean(config.getProperty("popUp", "true")));
        windowDimensionSaveCheckBox.setSelected(Boolean.parseBoolean(config.getProperty("windowDimensionSave", "true")));
    }

    public void setOutputListener(DownloadListener listener)
    {
        this.outputListener = listener;
    }
}