package edu.homeautomation.ui;

import edu.homeautomation.model.*;
import edu.homeautomation.persistence.HomeRepository;
import edu.homeautomation.service.DeviceService;
import edu.homeautomation.service.EnvironmentService;
import edu.homeautomation.service.PasswordSecurity;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/** Role-aware dashboards for homeowners and administrators. */
public final class HomeDashboardFrame extends JFrame {
    private static final Color BG = new Color(245, 248, 247);
    private static final Color NAVY = new Color(20, 39, 60);
    private static final Color GREEN = new Color(47, 131, 107);
    private static final String[] DEVICE_COLUMNS = {"Device", "Room", "Type", "Compatibility", "Action"};
    private static final String[] USER_COLUMNS = {"Name", "Email", "Role"};

    private final HomeRepository repository;
    private final DeviceService deviceService;
    private final EnvironmentService environment;
    private final boolean admin;
    private AppUser activeUser;
    private final CardLayout pages = new CardLayout();
    private final JPanel content = new JPanel(pages);
    private final JPanel deviceGrid = new JPanel(new GridLayout(0, 2, 14, 14));
    private final JPanel ruleList = new JPanel();
    private JPanel body;
    private JPanel topbarPanel;
    private final JComboBox<String> roomFilter = new JComboBox<>(new String[]{"All rooms", "Living Room", "Hallway", "Bedroom", "Entry", "Garage"});
    private JLabel temperatureValue;
    private JLabel securityValue;
    private JLabel updatedValue;
    private JLabel alertValue;
    private JLabel pageHeading;
    private JLabel pageSubheading;
    private JTable deviceTable;
    private JTable userTable;
    private JTextField profileName;
    private JTextField profileEmail;
    private JTextField homeNameField;
    private JTextField temperatureLimitField;
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("h:mm a");
    private final Consumer<EnvironmentReading> environmentListener;

    public HomeDashboardFrame(HomeRepository repository, DeviceService deviceService,
                              EnvironmentService environment, AppUser activeUser) {
        super("Haven · Home Control");
        this.repository = repository;
        this.deviceService = deviceService;
        this.environment = environment;
        this.activeUser = activeUser;
        this.admin = "ADMIN".equals(activeUser.role());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1240, 790);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        environmentListener = reading -> SwingUtilities.invokeLater(() -> updateEnvironment(reading));
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent event) {
                environment.removeListener(environmentListener);
            }
        });
        build();
        environment.addListener(environmentListener);
        updateSecurityFromDevices();
        updateEnvironment(environment.latest());
    }

    private void build() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(BG);
        shell.add(sidebar(), BorderLayout.WEST);

        body = new JPanel(new BorderLayout());
        body.setBackground(BG);
        topbarPanel = topbar();
        body.add(topbarPanel, BorderLayout.NORTH);

        content.setBackground(BG);
        content.add(homePage(), "home");
        content.add(automationPage(), "automation");
        content.add(profilePage(), "profile");
        content.add(adminPage(), "admin");
        body.add(content, BorderLayout.CENTER);
        shell.add(body, BorderLayout.CENTER);
        setContentPane(shell);
        refreshDevices();
        pages.show(content, "home");
    }

    private JPanel sidebar() {
        JPanel panel = new JPanel();
        panel.setBackground(NAVY);
        panel.setPreferredSize(new Dimension(226, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(25, 18, 20, 18));
        JLabel logo = LoginFrame.label("⌂   HAVEN", 19, Color.WHITE, Font.BOLD);
        logo.setBorder(new EmptyBorder(3, 8, 34, 0));
        panel.add(logo);
        panel.add(sectionLabel("YOUR HOME"));
        navButton(panel, "⌂   Overview", "home");
        navButton(panel, "◷   Automations", "automation");
        navButton(panel, "♙   My profile", "profile");
        if (admin) {
            panel.add(Box.createVerticalStrut(22));
            panel.add(sectionLabel("MANAGEMENT"));
            navButton(panel, "⚙   Admin console", "admin");
        }
        panel.add(Box.createVerticalGlue());

        JPanel online = new JPanel(new BorderLayout());
        online.setBackground(new Color(31, 57, 77));
        online.setBorder(new EmptyBorder(13, 12, 13, 12));
        online.add(LoginFrame.label("●", 12, new Color(111, 205, 159), Font.BOLD), BorderLayout.WEST);
        online.add(LoginFrame.label("  Home hub online<br>  Simulated device network", 12,
                new Color(213, 226, 232), Font.PLAIN), BorderLayout.CENTER);
        panel.add(online);
        panel.add(Box.createVerticalStrut(18));

        JButton signOut = new JButton("←  Sign out");
        signOut.setForeground(new Color(210, 221, 227));
        signOut.setBackground(NAVY);
        signOut.setBorderPainted(false);
        signOut.setFocusPainted(false);
        signOut.setAlignmentX(Component.LEFT_ALIGNMENT);
        signOut.addActionListener(event -> {
            new LoginFrame(repository, deviceService, environment).setVisible(true);
            dispose();
        });
        panel.add(signOut);
        return panel;
    }

    private JLabel sectionLabel(String value) {
        JLabel label = LoginFrame.label(value, 10, new Color(136, 162, 177), Font.BOLD);
        label.setBorder(new EmptyBorder(0, 8, 12, 0));
        return label;
    }

    private void navButton(JPanel parent, String text, String page) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(new Color(222, 233, 237));
        button.setBackground(NAVY);
        button.setBorder(new EmptyBorder(12, 12, 12, 10));
        button.setFocusPainted(false);
        button.setMaximumSize(new Dimension(190, 43));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> {
            if ("admin".equals(page)) refreshAdminTables();
            if ("profile".equals(page)) loadProfileFields();
            if ("automation".equals(page)) refreshRules();
            updateHeading(page);
            pages.show(content, page);
        });
        parent.add(button);
    }

    private JPanel topbar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(17, 30, 17, 30));
        String title = admin ? "Administration" : "Good morning, " + activeUser.name() + "  ☀";
        pageHeading = LoginFrame.label(title, 22, NAVY, Font.BOLD);
        pageSubheading = LoginFrame.label(admin ? "Manage accounts, devices and platform settings."
                : "Here’s what’s happening around " + repository.getSetting("home_name", "your home") + ".",
                12, new Color(113, 129, 138), Font.PLAIN);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(pageHeading);
        heading.add(Box.createVerticalStrut(4));
        heading.add(pageSubheading);
        top.add(heading, BorderLayout.WEST);

        JPanel profile = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 3));
        profile.setOpaque(false);
        JLabel avatar = new JLabel(initials(activeUser.name()), SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(new Color(226, 240, 235));
        avatar.setForeground(GREEN);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        avatar.setPreferredSize(new Dimension(37, 37));
        profile.add(avatar);
        profile.add(LoginFrame.label(activeUser.name(), 13, NAVY, Font.BOLD));
        top.add(profile, BorderLayout.EAST);
        return top;
    }

    private String initials(String name) {
        String[] words = name.trim().split("\\s+");
        if (words.length == 1) return words[0].substring(0, Math.min(2, words[0].length())).toUpperCase();
        return (words[0].charAt(0) + "" + words[words.length - 1].charAt(0)).toUpperCase();
    }

    private void updateHeading(String page) {
        if (pageHeading == null || admin) return;
        switch (page) {
            case "automation" -> { pageHeading.setText("Your automations"); pageSubheading.setText("Small routines that make home feel effortless."); }
            case "profile" -> { pageHeading.setText("Your profile"); pageSubheading.setText("Keep your account details up to date."); }
            default -> { pageHeading.setText("Good morning, " + activeUser.name() + "  ☀"); pageSubheading.setText("Here’s what’s happening around " + repository.getSetting("home_name", "your home") + "."); }
        }
    }

    private JPanel pageBase() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(24, 30, 26, 30));
        return page;
    }

    private JPanel homePage() {
        JPanel page = pageBase();
        JPanel metrics = new JPanel(new GridLayout(1, 3, 14, 0));
        metrics.setOpaque(false);
        temperatureValue = LoginFrame.label("22.4° C", 25, NAVY, Font.BOLD);
        securityValue = LoginFrame.label("All secure", 20, NAVY, Font.BOLD);
        updatedValue = LoginFrame.label("Updated just now", 11, new Color(119, 135, 144), Font.PLAIN);
        alertValue = LoginFrame.label("No active alerts", 11, GREEN, Font.BOLD);
        metrics.add(metricCard("INDOOR TEMPERATURE", temperatureValue,
                "Comfortable range · live simulation", "◉", new Color(232, 242, 250)));
        metrics.add(metricCard("HOME SECURITY", securityValue,
                "Security device status", "⌑", new Color(232, 245, 238)));
        metrics.add(metricCard("CONNECTED DEVICES", LoginFrame.label(deviceService.devices().size() + " devices", 25, NAVY, Font.BOLD),
                "Your home hub is responding", "⌘", new Color(244, 239, 250)));

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(LoginFrame.label("Your devices", 18, NAVY, Font.BOLD), BorderLayout.WEST);
        roomFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        roomFilter.addActionListener(event -> refreshDevices());
        head.add(roomFilter, BorderLayout.EAST);
        deviceGrid.setOpaque(false);
        JScrollPane scroll = new JScrollPane(deviceGrid);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        center.add(head, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        page.add(metrics, BorderLayout.NORTH);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private JPanel metricCard(String title, JLabel value, String detail, String symbol, Color tint) {
        JPanel card = new JPanel(new BorderLayout(10, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(230, 236, 234)),
                new EmptyBorder(17, 18, 17, 18)));
        JPanel labelRow = new JPanel(new BorderLayout());
        labelRow.setOpaque(false);
        labelRow.add(LoginFrame.label(title, 10, new Color(107, 126, 133), Font.BOLD), BorderLayout.WEST);
        JLabel icon = new JLabel(symbol, SwingConstants.CENTER);
        icon.setOpaque(true);
        icon.setBackground(tint);
        icon.setForeground(GREEN);
        icon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        icon.setPreferredSize(new Dimension(36, 36));
        labelRow.add(icon, BorderLayout.EAST);
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(value);
        bottom.add(Box.createVerticalStrut(4));
        bottom.add(LoginFrame.label(detail, 11, new Color(112, 129, 136), Font.PLAIN));
        if ("INDOOR TEMPERATURE".equals(title)) {
            bottom.add(Box.createVerticalStrut(4));
            bottom.add(updatedValue);
            bottom.add(alertValue);
        }
        card.add(labelRow, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);
        return card;
    }

    private void refreshDevices() {
        deviceGrid.removeAll();
        String selected = (String) roomFilter.getSelectedItem();
        List<SmartDevice> devices = deviceService.devices();
        for (SmartDevice device : devices) {
            if (selected != null && !selected.equals("All rooms") && !selected.equals(device.getRoom())) continue;
            deviceGrid.add(deviceCard(device));
        }
        if (deviceGrid.getComponentCount() == 0) {
            deviceGrid.add(LoginFrame.label("No devices in this room yet.", 14, new Color(115, 130, 137), Font.PLAIN));
        }
        deviceGrid.revalidate();
        deviceGrid.repaint();
    }

    private JPanel deviceCard(SmartDevice device) {
        JPanel card = new JPanel(new BorderLayout(12, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(229, 235, 233)),
                new EmptyBorder(16, 16, 15, 16)));
        JLabel badge = new JLabel(device.getKind().equals("Security") ? "⌑"
                : device.getKind().equals("Thermostat") ? "♨" : "☼", SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(new Color(232, 242, 238));
        badge.setForeground(GREEN);
        badge.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 20));
        badge.setPreferredSize(new Dimension(43, 43));
        card.add(badge, BorderLayout.WEST);

        JPanel middle = new JPanel();
        middle.setOpaque(false);
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        middle.add(LoginFrame.label(device.getName(), 14, NAVY, Font.BOLD));
        middle.add(Box.createVerticalStrut(4));
        middle.add(LoginFrame.label(device.getRoom() + "  ·  " + device.statusText(), 11,
                new Color(112, 129, 136), Font.PLAIN));
        card.add(middle, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        JToggleButton toggle = new JToggleButton(device.isOn() ? "On" : "Off", device.isOn());
        toggle.setFocusPainted(false);
        toggle.setBackground(device.isOn() ? new Color(225, 242, 235) : new Color(242, 245, 244));
        toggle.setForeground(device.isOn() ? GREEN : new Color(108, 121, 127));
        toggle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        toggle.setBorderPainted(false);
        toggle.setAlignmentX(Component.CENTER_ALIGNMENT);
        toggle.setEnabled(device.isCompatible());
        toggle.addActionListener(event -> {
            try {
                JOptionPane.showMessageDialog(this, deviceService.toggle(device));
                updateSecurityFromDevices();
            } catch (HomeValidationException exception) {
                JOptionPane.showMessageDialog(this, exception.getMessage(), "Device unavailable", JOptionPane.WARNING_MESSAGE);
            } catch (RuntimeException exception) {
                showError("The device command could not be saved.", exception);
            }
            refreshDevices();
        });
        actions.add(toggle);
        if (device instanceof LightDevice || device instanceof ThermostatDevice) {
            JButton adjust = new JButton("Adjust");
            adjust.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            adjust.setBorderPainted(false);
            adjust.setContentAreaFilled(false);
            adjust.setForeground(GREEN);
            adjust.setAlignmentX(Component.CENTER_ALIGNMENT);
            adjust.setEnabled(device.isCompatible());
            adjust.addActionListener(event -> adjustDevice(device));
            actions.add(adjust);
        }
        card.add(actions, BorderLayout.EAST);
        if (!device.isCompatible()) card.setToolTipText("This device needs administrator approval.");
        return card;
    }

    private void adjustDevice(SmartDevice device) {
        if (device instanceof LightDevice light) {
            JSlider brightness = new JSlider(0, 100, light.getBrightness());
            brightness.setMajorTickSpacing(25);
            brightness.setPaintTicks(true);
            brightness.setPaintLabels(true);
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.add(new JLabel("Brightness: " + light.getBrightness() + "%"), BorderLayout.NORTH);
            panel.add(brightness, BorderLayout.CENTER);
            brightness.addChangeListener(event -> ((JLabel) panel.getComponent(0)).setText("Brightness: " + brightness.getValue() + "%"));
            if (JOptionPane.showConfirmDialog(this, panel, "Adjust " + light.getName(), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                light.setBrightness(brightness.getValue());
                saveDeviceAndRefresh(light);
            }
        } else if (device instanceof ThermostatDevice thermostat) {
            JSpinner target = new JSpinner(new SpinnerNumberModel(thermostat.getTargetTemperature(), 10, 32, 1));
            JPanel panel = new JPanel(new GridLayout(1, 2, 8, 8));
            panel.add(new JLabel("Target temperature (°C)"));
            panel.add(target);
            if (JOptionPane.showConfirmDialog(this, panel, "Adjust " + thermostat.getName(), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                thermostat.setTargetTemperature((Integer) target.getValue());
                saveDeviceAndRefresh(thermostat);
            }
        }
    }

    private void saveDeviceAndRefresh(SmartDevice device) {
        try {
            repository.saveDevice(device);
            refreshDevices();
            JOptionPane.showMessageDialog(this, "Your device settings have been updated.", "Settings saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError("The device settings could not be saved.", exception);
        }
    }

    private JPanel automationPage() {
        JPanel page = pageBase();
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(LoginFrame.label("Your routines", 20, NAVY, Font.BOLD), BorderLayout.WEST);
        JButton add = primaryButton("＋  New routine");
        add.addActionListener(event -> addRule());
        top.add(add, BorderLayout.EAST);
        ruleList.setBackground(BG);
        ruleList.setLayout(new BoxLayout(ruleList, BoxLayout.Y_AXIS));
        page.add(top, BorderLayout.NORTH);
        page.add(new JScrollPane(ruleList), BorderLayout.CENTER);
        refreshRules();
        return page;
    }

    private void refreshRules() {
        ruleList.removeAll();
        List<AutomationRule> rules = repository.findRules();
        if (rules.isEmpty()) {
            ruleList.add(LoginFrame.label("No routines yet. Create one to get started.", 14,
                    new Color(115, 130, 137), Font.PLAIN));
        }
        for (AutomationRule rule : rules) {
            JPanel card = new JPanel(new BorderLayout(15, 0));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(229, 235, 233)),
                    new EmptyBorder(18, 18, 18, 18)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
            JLabel icon = new JLabel("◷", SwingConstants.CENTER);
            icon.setOpaque(true);
            icon.setBackground(new Color(232, 242, 238));
            icon.setForeground(GREEN);
            icon.setFont(new Font("Segoe UI", Font.PLAIN, 20));
            icon.setPreferredSize(new Dimension(44, 44));
            card.add(icon, BorderLayout.WEST);
            JPanel info = new JPanel();
            info.setOpaque(false);
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.add(LoginFrame.label(rule.getName(), 15, NAVY, Font.BOLD));
            info.add(Box.createVerticalStrut(5));
            info.add(LoginFrame.label(rule.getCondition() + "   →   " + rule.getAction(), 12,
                    new Color(108, 125, 133), Font.PLAIN));
            card.add(info, BorderLayout.CENTER);
            JCheckBox enabled = new JCheckBox("Enabled", rule.isEnabled());
            enabled.setOpaque(false);
            enabled.setForeground(rule.isEnabled() ? GREEN : new Color(119, 130, 136));
            enabled.addActionListener(event -> {
                rule.setEnabled(enabled.isSelected());
                try { repository.updateRule(rule); }
                catch (RuntimeException exception) { showError("The routine status could not be saved.", exception); }
            });
            card.add(enabled, BorderLayout.EAST);
            ruleList.add(card);
            ruleList.add(Box.createVerticalStrut(12));
        }
        ruleList.revalidate();
        ruleList.repaint();
    }

    private void addRule() {
        JTextField name = new JTextField(22);
        JTextField condition = new JTextField(22);
        JTextField action = new JTextField(22);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 9));
        form.add(new JLabel("Routine name")); form.add(name);
        form.add(new JLabel("When / condition")); form.add(condition);
        form.add(new JLabel("Then / action")); form.add(action);
        if (JOptionPane.showConfirmDialog(this, form, "Create automation", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String cleanName = name.getText().trim();
        String cleanCondition = condition.getText().trim();
        String cleanAction = action.getText().trim();
        if (cleanName.isEmpty() || cleanCondition.isEmpty() || cleanAction.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please complete all three fields.", "Check your routine", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            repository.saveRule(new AutomationRule(cleanName, cleanCondition, cleanAction, true));
            refreshRules();
            JOptionPane.showMessageDialog(this, "Your automation is ready.", "Routine created", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError("The routine could not be saved.", exception);
        }
    }

    private JPanel profilePage() {
        JPanel page = pageBase();
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(24, 26, 24, 26));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(LoginFrame.label("Personal details", 18, NAVY, Font.BOLD));
        card.add(Box.createVerticalStrut(20));
        card.add(LoginFrame.label("Name", 13, NAVY, Font.BOLD));
        profileName = new JTextField(); styleTextField(profileName);
        card.add(Box.createVerticalStrut(6)); card.add(profileName);
        card.add(Box.createVerticalStrut(16));
        card.add(LoginFrame.label("Email address", 13, NAVY, Font.BOLD));
        profileEmail = new JTextField(); styleTextField(profileEmail);
        card.add(Box.createVerticalStrut(6)); card.add(profileEmail);
        card.add(Box.createVerticalStrut(20));
        JButton save = primaryButton("Save profile");
        save.addActionListener(event -> saveProfile());
        card.add(save);
        page.add(card, BorderLayout.NORTH);
        loadProfileFields();
        return page;
    }

    private void loadProfileFields() {
        if (profileName != null) profileName.setText(activeUser.name());
        if (profileEmail != null) profileEmail.setText(activeUser.email());
    }

    private void saveProfile() {
        String name = profileName.getText().trim();
        String email = profileEmail.getText().trim().toLowerCase();
        if (name.isEmpty() || !validEmail(email)) {
            JOptionPane.showMessageDialog(this, "Enter your name and a valid email address.", "Check your details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AppUser existing = repository.findUserByEmail(email);
        if (existing != null && !email.equalsIgnoreCase(activeUser.email())) {
            JOptionPane.showMessageDialog(this, "That email address is already in use.", "Check your details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            AppUser updated = new AppUser(name, email, activeUser.role(), activeUser.passwordHash());
            if (!email.equalsIgnoreCase(activeUser.email())) repository.deleteUser(activeUser.email());
            repository.saveUser(updated);
            activeUser = updated;
            refreshTopbar();
            JOptionPane.showMessageDialog(this, "Your profile has been saved.", "Profile updated", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError("Your profile could not be saved.", exception);
        }
    }

    private JPanel adminPage() {
        JPanel page = pageBase();
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(LoginFrame.label("Platform management", 20, NAVY, Font.BOLD), BorderLayout.WEST);
        header.add(LoginFrame.label(repository.isPersistent() ? "SQLite database connected"
                        : "Demo data · changes reset when app closes", 12, new Color(106, 124, 131), Font.PLAIN),
                BorderLayout.EAST);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabs.addTab("Devices", deviceTab());
        tabs.addTab("Users", userTab());
        tabs.addTab("System", systemTab());
        page.add(header, BorderLayout.NORTH);
        page.add(tabs, BorderLayout.CENTER);
        return page;
    }

    private JPanel deviceTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setBackground(BG);
        tab.add(LoginFrame.label("Review and approve compatible home devices", 14, NAVY, Font.BOLD), BorderLayout.NORTH);
        deviceTable = createTable(DEVICE_COLUMNS, adminDeviceRows());
        tab.add(new JScrollPane(deviceTable), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        JButton review = primaryButton("Approve / reject selected");
        review.addActionListener(event -> updateCompatibility(deviceTable.getSelectedRow()));
        actions.add(review);
        tab.add(actions, BorderLayout.SOUTH);
        return tab;
    }

    private Object[][] adminDeviceRows() {
        List<SmartDevice> devices = deviceService.devices();
        Object[][] rows = new Object[devices.size()][DEVICE_COLUMNS.length];
        for (int index = 0; index < devices.size(); index++) {
            SmartDevice device = devices.get(index);
            rows[index] = new Object[]{device.getName(), device.getRoom(), device.getKind(),
                    device.isCompatible() ? "Approved" : "Pending", "Select row to review"};
        }
        return rows;
    }

    private void updateCompatibility(int selectedRow) {
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a device first.");
            return;
        }
        List<SmartDevice> devices = deviceService.devices();
        if (selectedRow >= devices.size()) return;
        SmartDevice device = devices.get(selectedRow);
        boolean approve = !device.isCompatible();
        try {
            deviceService.setCompatibility(device, approve);
            refreshAdminTables();
            refreshDevices();
            JOptionPane.showMessageDialog(this, device.getName() + " is now "
                    + (approve ? "approved" : "pending review") + ".");
        } catch (RuntimeException exception) {
            showError("The compatibility decision could not be saved.", exception);
        }
    }

    private JPanel userTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setBackground(BG);
        userTable = createTable(USER_COLUMNS, userRows());
        tab.add(new JScrollPane(userTable), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        JButton remove = new JButton("Remove selected");
        remove.addActionListener(event -> removeSelectedUser());
        JButton edit = new JButton("Edit selected");
        edit.addActionListener(event -> editSelectedUser());
        JButton add = primaryButton("＋ Add user");
        add.addActionListener(event -> editUser(null));
        buttons.add(remove); buttons.add(edit); buttons.add(add);
        tab.add(buttons, BorderLayout.SOUTH);
        return tab;
    }

    private Object[][] userRows() {
        List<AppUser> users = repository.findUsers();
        Object[][] rows = new Object[users.size()][USER_COLUMNS.length];
        for (int index = 0; index < users.size(); index++) {
            AppUser user = users.get(index);
            rows[index] = new Object[]{user.name(), user.email(), user.role()};
        }
        return rows;
    }

    private void editSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a user first."); return; }
        editUser(repository.findUserByEmail((String) userTable.getValueAt(row, 1)));
    }

    private void editUser(AppUser existing) {
        JTextField name = new JTextField(existing == null ? "" : existing.name());
        JTextField email = new JTextField(existing == null ? "" : existing.email());
        JComboBox<String> role = new JComboBox<>(new String[]{"HOMEOWNER", "ADMIN"});
        if (existing != null) role.setSelectedItem(existing.role());
        JPasswordField password = new JPasswordField();
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 9));
        form.add(new JLabel("Name")); form.add(name);
        form.add(new JLabel("Email")); form.add(email);
        form.add(new JLabel("Role")); form.add(role);
        form.add(new JLabel(existing == null ? "Password (8+ characters)" : "New password (optional)")); form.add(password);
        int choice = JOptionPane.showConfirmDialog(this, form, existing == null ? "Add platform user" : "Edit platform user",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        char[] newPassword = password.getPassword();
        try {
            String cleanName = name.getText().trim();
            String cleanEmail = email.getText().trim().toLowerCase();
            String cleanRole = (String) role.getSelectedItem();
            if (cleanName.isBlank() || !validEmail(cleanEmail)) {
                throw new IllegalArgumentException("Enter a name and valid email address.");
            }
            if (existing == null && newPassword.length < 8) {
                throw new IllegalArgumentException("Choose a password with at least 8 characters.");
            }
            AppUser emailOwner = repository.findUserByEmail(cleanEmail);
            if (emailOwner != null && (existing == null || !emailOwner.email().equalsIgnoreCase(existing.email()))) {
                throw new IllegalArgumentException("That email address is already in use.");
            }
            if (existing != null && "ADMIN".equals(existing.role()) && !"ADMIN".equals(cleanRole)
                    && countAdmins() <= 1) {
                throw new IllegalArgumentException("The platform must keep at least one administrator.");
            }
            String hash = newPassword.length == 0 && existing != null
                    ? existing.passwordHash() : PasswordSecurity.hash(newPassword);
            AppUser updated = new AppUser(cleanName, cleanEmail, cleanRole, hash);
            if (existing != null && !cleanEmail.equalsIgnoreCase(existing.email())) repository.deleteUser(existing.email());
            repository.saveUser(updated);
            if (existing != null && existing.email().equalsIgnoreCase(activeUser.email())) {
                activeUser = updated;
                refreshTopbar();
            }
            refreshAdminTables();
            JOptionPane.showMessageDialog(this, existing == null ? "The user account has been created." : "The user account has been updated.",
                    "User saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Check user details", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException exception) {
            showError("The user account could not be saved.", exception);
        } finally {
            Arrays.fill(newPassword, '\0');
        }
    }

    private void removeSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a user first."); return; }
        String email = (String) userTable.getValueAt(row, 1);
        AppUser selected = repository.findUserByEmail(email);
        if (selected == null) return;
        if (email.equalsIgnoreCase(activeUser.email())) {
            JOptionPane.showMessageDialog(this, "You can't remove the account you're currently using.", "Account in use", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("ADMIN".equals(selected.role()) && countAdmins() <= 1) {
            JOptionPane.showMessageDialog(this, "The platform must keep at least one administrator.", "Action not allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Remove " + selected.name() + " from the platform?", "Remove user",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            repository.deleteUser(email);
            refreshAdminTables();
        } catch (RuntimeException exception) {
            showError("The user account could not be removed.", exception);
        }
    }

    private long countAdmins() {
        return repository.findUsers().stream().filter(user -> "ADMIN".equals(user.role())).count();
    }

    private JPanel systemTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 22, 20, 22));
        panel.setBackground(BG);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 6, 7, 12);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0; c.gridy = 0;
        panel.add(LoginFrame.label("System settings", 17, NAVY, Font.BOLD), c);
        c.gridy++;
        panel.add(new JLabel("Home display name"), c);
        c.gridx = 1; homeNameField = new JTextField(repository.getSetting("home_name", "My Home"), 20); panel.add(homeNameField, c);
        c.gridx = 0; c.gridy++;
        panel.add(new JLabel("Temperature alert limit (°C)"), c);
        c.gridx = 1; temperatureLimitField = new JTextField(repository.getSetting("temperature_limit", "28"), 20); panel.add(temperatureLimitField, c);

        JButton save = primaryButton("Save system settings");
        save.addActionListener(event -> saveSystemSettings());
        c.gridx = 1; c.gridy++; panel.add(save, c);

        c.gridx = 0; c.gridy++; c.gridwidth = 2;
        panel.add(LoginFrame.label("System monitoring", 16, NAVY, Font.BOLD), c);
        c.gridy++;
        panel.add(LoginFrame.label("Home hub: Online · environment monitor refreshes every 5 seconds", 13, GREEN, Font.PLAIN), c);
        c.gridy++;
        panel.add(LoginFrame.label("Storage: " + (repository.isPersistent() ? "SQLite database" : "In-memory demo"), 13, NAVY, Font.PLAIN), c);
        c.gridy++;
        panel.add(LoginFrame.label("Compatible devices: " + deviceService.devices().stream().filter(SmartDevice::isCompatible).count()
                + " of " + deviceService.devices().size(), 13, NAVY, Font.PLAIN), c);
        c.gridy++;
        panel.add(LoginFrame.label("Alerts are shown on the Homeowner overview when readings exceed this limit.", 12,
                new Color(106, 124, 131), Font.PLAIN), c);
        return panel;
    }

    private void saveSystemSettings() {
        String homeName = homeNameField.getText().trim();
        if (homeName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Enter a name for the home.", "Check settings", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            double limit = Double.parseDouble(temperatureLimitField.getText().trim());
            if (limit < 10 || limit > 40) throw new NumberFormatException();
            repository.saveSetting("home_name", homeName);
            repository.saveSetting("temperature_limit", Double.toString(limit));
            environment.setTemperatureLimit(limit);
            updateEnvironment(environment.latest());
            JOptionPane.showMessageDialog(this, "System settings have been updated.", "Settings saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException exception) {
            JOptionPane.showMessageDialog(this, "Enter a temperature limit between 10 and 40 °C.", "Check settings", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException exception) {
            showError("System settings could not be saved.", exception);
        }
    }

    private JTable createTable(String[] columns, Object[][] rows) {
        DefaultTableModel model = new DefaultTableModel(rows, columns) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setFillsViewportHeight(true);
        return table;
    }

    private void refreshAdminTables() {
        if (deviceTable != null) replaceRows(deviceTable, adminDeviceRows());
        if (userTable != null) replaceRows(userTable, userRows());
    }

    private void replaceRows(JTable table, Object[][] rows) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (Object[] row : rows) model.addRow(row);
    }

    private void updateSecurityFromDevices() {
        boolean allSecure = deviceService.devices().stream()
                .filter(device -> "Security".equals(device.getKind()))
                .allMatch(SmartDevice::isOn);
        environment.setSecurityStatus(allSecure ? "All secure" : "Check security devices");
        updateEnvironment(environment.latest());
    }

    private void updateEnvironment(EnvironmentReading reading) {
        if (temperatureValue == null || reading == null) return;
        temperatureValue.setText(String.format("%.1f° C", reading.temperature()));
        securityValue.setText(reading.securityStatus());
        updatedValue.setText("Updated " + reading.recordedAt().format(timeFormat));
        boolean alert = reading.temperature() >= environment.getTemperatureLimit();
        alertValue.setText(alert ? "Temperature alert · check settings" : "No active alerts");
        alertValue.setForeground(alert ? new Color(174, 90, 44) : GREEN);
    }

    private void refreshTopbar() {
        body.remove(topbarPanel);
        topbarPanel = topbar();
        body.add(topbarPanel, BorderLayout.NORTH);
        body.revalidate();
        body.repaint();
    }

    private boolean validEmail(String email) {
        return email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setMaximumSize(new Dimension(380, 40));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(GREEN);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void showError(String message, RuntimeException exception) {
        JOptionPane.showMessageDialog(this, message + "\n" + exception.getMessage(), "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }
}
