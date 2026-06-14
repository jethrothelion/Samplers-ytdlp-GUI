import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private JButton browseDirBtn;
    private JButton updateYTDLP;

    private JComboBox<String> themeDropdown;

    private JCheckBox autoStartCheckbox;
    private JCheckBox openWhenDone;


    private JButton saveBtn;
    private JButton closeBtn;

    String configPath = System.getProperty("user.home") + File.separator + "YoutubeDownloaderConfig.properties";
    DownloadManager downloader = new DownloadManager();

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
        
        JButton updateYTDLP = new JButton("Update yt-dlp");
        updateYTDLP.setBorder(new LineBorder(Color.BLACK, 2));
        updateYTDLP.setBackground(Color.WHITE);
        
        updateYTDLP.addActionListener(e -> {

        });


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

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(ffmpegLabel, gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(ffmpegPathField, gbc);

        // Blank space at bottom to push everything up
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3; gbc.weighty = 1.0;
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
        openWhenDone = new JCheckBox("Open file in folder explorer on finished download");
        openWhenDone.setFocusPainted(false);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1.0;
        panel.add(openWhenDone, gbc);

        // Blank space at bottom
        gbc.gridx = 0; gbc.gridy = 2; gbc.weighty = 1.0;
        panel.add(new JLabel(""), gbc);

        return panel;
    }

    private void updateYTDLP()
    {

        downloader.Download("", new DownloadListener()
        {
            
        });

    }

    // ==========================================================
    // CONFIGURATION SAVING & LOADING
    // ==========================================================

    public void saveConfig()
    {
        ConfigManager config = ConfigManager.getInstance();

        // Paths
        config.setProperty("directory", defaultDirField.getText());
        config.setProperty("ytdlpPath", ytdlpPathField.getText());
        config.setProperty("ffmpegPath", ffmpegPathField.getText());

        // Appearance & Behavior
        config.setProperty("theme", themeDropdown.getSelectedItem().toString());
        config.setProperty("autoStart", String.valueOf(autoStartCheckbox.isSelected()));
        config.setProperty("openWhenDone", String.valueOf(openWhenDone.isSelected()));

        config.save(); // Writes everything to the file once
        
        JOptionPane.showMessageDialog(this, "Settings Saved Successfully!");
        dispose(); 
    }

    public void readConfig()
    {
        ConfigManager config = ConfigManager.getInstance();

        // Paths
        defaultDirField.setText(config.getProperty("directory", ""));
        ytdlpPathField.setText(config.getProperty("ytdlpPath", "yt-dlp"));
        ffmpegPathField.setText(config.getProperty("ffmpegPath", "ffmpeg"));

        // Appearance
        themeDropdown.setSelectedItem(config.getProperty("theme", "Light"));

        // Behavior
        autoStartCheckbox.setSelected(Boolean.parseBoolean(config.getProperty("autoStart", "false")));
        openWhenDone.setSelected(Boolean.parseBoolean(config.getProperty("openWhenDone", "false")));
    }
}