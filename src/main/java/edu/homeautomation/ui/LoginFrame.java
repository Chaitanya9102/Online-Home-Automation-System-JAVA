package edu.homeautomation.ui;

import edu.homeautomation.persistence.HomeRepository;
import edu.homeautomation.service.DeviceService;
import edu.homeautomation.service.EnvironmentService;

import javax.swing.*;
import java.awt.*;

public final class LoginFrame extends JFrame {
    private static final Color NAVY = new Color(20, 39, 60);
    private static final Color GREEN = new Color(47, 131, 107);
    private final HomeRepository repository;
    private final DeviceService devices;
    private final EnvironmentService environment;
    private final JComboBox<String> role = new JComboBox<>(new String[]{"Homeowner", "Administrator"});

    public LoginFrame(HomeRepository repository, DeviceService devices, EnvironmentService environment) {
        super("Haven · Home Control");
        this.repository = repository;
        this.devices = devices;
        this.environment = environment;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 590);
        setLocationRelativeTo(null);
        setResizable(false);
        build();
    }

    private void build() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        JPanel hero = new JPanel(); hero.setBackground(NAVY); hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBorder(BorderFactory.createEmptyBorder(58, 55, 45, 45));
        JLabel brand = label("⌂   HAVEN", 19, Color.WHITE, Font.BOLD);
        JLabel headline = label("A calmer way\nto run your home.", 34, Color.WHITE, Font.BOLD);
        JLabel copy = label("Control your spaces, keep an eye on\nyour environment, and let routines do\nthe little things for you.", 15, new Color(189, 207, 218), Font.PLAIN);
        JLabel icon = label("◉     ◌     ◉", 34, new Color(128, 196, 174), Font.PLAIN);
        hero.add(brand); hero.add(Box.createVerticalStrut(65)); hero.add(icon); hero.add(Box.createVerticalStrut(24)); hero.add(headline); hero.add(Box.createVerticalStrut(18)); hero.add(copy);

        JPanel form = new JPanel(); form.setBackground(new Color(248, 250, 249)); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(96, 54, 65, 54));
        JLabel eyebrow = label("WELCOME HOME", 12, GREEN, Font.BOLD);
        JLabel title = label("Sign in to Haven", 27, NAVY, Font.BOLD);
        JLabel hint = label("Select a workspace to continue.", 14, new Color(101, 116, 127), Font.PLAIN);
        form.add(eyebrow); form.add(Box.createVerticalStrut(12)); form.add(title); form.add(Box.createVerticalStrut(8)); form.add(hint);
        form.add(Box.createVerticalStrut(35)); form.add(label("Workspace", 13, NAVY, Font.BOLD));
        role.setMaximumSize(new Dimension(330, 44)); role.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        form.add(Box.createVerticalStrut(9)); form.add(role); form.add(Box.createVerticalStrut(18));
        JButton enter = new JButton("Open dashboard   →");
        enter.setFont(new Font("Segoe UI", Font.BOLD, 14)); enter.setForeground(Color.WHITE); enter.setBackground(GREEN); enter.setFocusPainted(false); enter.setBorderPainted(false); enter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        enter.setMaximumSize(new Dimension(330, 46)); enter.setAlignmentX(Component.LEFT_ALIGNMENT);
        enter.addActionListener(event -> openDashboard());
        form.add(enter); form.add(Box.createVerticalStrut(22));
        JLabel demo = label("Classroom demo · no password required", 12, new Color(122, 135, 143), Font.PLAIN); form.add(demo);
        root.add(hero); root.add(form); setContentPane(root);
        getRootPane().setDefaultButton(enter);
    }

    private void openDashboard() {
        String selected = (String) role.getSelectedItem();
        boolean admin = "Administrator".equals(selected);
        new HomeDashboardFrame(repository, devices, environment, admin).setVisible(true);
        dispose();
    }

    static JLabel label(String text, int size, Color color, int style) {
        JLabel label = new JLabel("<html>" + text.replace("\n", "<br>") + "</html>");
        label.setFont(new Font("Segoe UI", style, size)); label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
