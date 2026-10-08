package edu.homeautomation.ui;

import edu.homeautomation.model.AppUser;
import edu.homeautomation.persistence.HomeRepository;
import edu.homeautomation.service.DeviceService;
import edu.homeautomation.service.EnvironmentService;
import edu.homeautomation.service.PasswordSecurity;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

/** Sign-in screen and homeowner account registration. */
public final class LoginFrame extends JFrame {
    private static final Color NAVY = new Color(20, 39, 60);
    private static final Color SURFACE = new Color(35, 59, 79);
    private static final Color GREEN = new Color(47, 131, 107);
    private final HomeRepository repository;
    private final DeviceService devices;
    private final EnvironmentService environment;
    private JTextField emailField;
    private JPasswordField passwordField;

    public LoginFrame(HomeRepository repository, DeviceService devices, EnvironmentService environment) {
        super("Haven · Home Control");
        this.repository = repository;
        this.devices = devices;
        this.environment = environment;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(940, 620);
        setLocationRelativeTo(null);
        setResizable(false);
        build();
    }

    private void build() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        JPanel hero = new JPanel();
        hero.setBackground(NAVY);
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBorder(new EmptyBorder(58, 55, 45, 45));
        hero.add(label("⌂   HAVEN", 19, Color.WHITE, Font.BOLD));
        hero.add(Box.createVerticalStrut(58));
        hero.add(label("◉     ◌     ◉", 34, new Color(128, 196, 174), Font.PLAIN));
        hero.add(Box.createVerticalStrut(22));
        hero.add(label("A calmer way\nto run your home.", 34, Color.WHITE, Font.BOLD));
        hero.add(Box.createVerticalStrut(18));
        hero.add(label("Control your spaces, keep an eye on\nyour environment, and let routines do\nthe little things for you.", 17, Color.WHITE, Font.BOLD));

        JPanel form = new JPanel();
        form.setBackground(SURFACE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(58, 54, 42, 54));
        form.add(label("WELCOME HOME", 14, Color.WHITE, Font.BOLD));
        form.add(Box.createVerticalStrut(10));
        form.add(label("Sign in to Haven", 28, Color.WHITE, Font.BOLD));
        form.add(Box.createVerticalStrut(7));
        form.add(label("Use your account or create a homeowner profile.", 15, Color.WHITE, Font.BOLD));
        form.add(Box.createVerticalStrut(8));
        form.add(label(repository.isPersistent() ? "SQLite database connected · changes are saved" : "Demo mode · changes reset when the app closes",
                13, Color.WHITE, Font.BOLD));
        form.add(Box.createVerticalStrut(24));
        form.add(label("Email address", 15, Color.WHITE, Font.BOLD));
        emailField = new JTextField("alex@example.com");
        styleField(emailField);
        form.add(Box.createVerticalStrut(6)); form.add(emailField);
        form.add(Box.createVerticalStrut(14));
        form.add(label("Password", 15, Color.WHITE, Font.BOLD));
        passwordField = new JPasswordField();
        styleField(passwordField);
        form.add(Box.createVerticalStrut(6)); form.add(passwordField);
        form.add(Box.createVerticalStrut(16));

        JButton signIn = primaryButton("Sign in   →");
        signIn.setMaximumSize(new Dimension(340, 44));
        signIn.addActionListener(event -> signIn());
        form.add(signIn);

        JButton create = new JButton("Create a homeowner account");
        create.setFont(new Font("Segoe UI", Font.BOLD, 15));
        create.setForeground(Color.WHITE);
        create.setContentAreaFilled(false);
        create.setBorderPainted(false);
        create.setAlignmentX(Component.LEFT_ALIGNMENT);
        create.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        create.addActionListener(event -> createAccount());
        form.add(Box.createVerticalStrut(9)); form.add(create);
        form.add(Box.createVerticalStrut(13));
        form.add(label("Demo homeowner: alex@example.com  /  Home123!", 13, Color.WHITE, Font.BOLD));
        form.add(Box.createVerticalStrut(4));
        form.add(label("Demo administrator: admin@haven.local  /  Admin123!", 13, Color.WHITE, Font.BOLD));

        root.add(hero);
        root.add(form);
        setContentPane(root);
        getRootPane().setDefaultButton(signIn);
    }

    private void signIn() {
        String email = emailField.getText().trim();
        char[] password = passwordField.getPassword();
        try {
            AppUser user = repository.findUserByEmail(email);
            if (user == null || !PasswordSecurity.verify(password, user.passwordHash())) {
                JOptionPane.showMessageDialog(this, "We couldn't match that email and password. Please try again.", "Sign-in unsuccessful", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new HomeDashboardFrame(repository, devices, environment, user).setVisible(true);
            dispose();
        } catch (RuntimeException exception) {
            showError("We couldn't sign in right now.", exception);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void createAccount() {
        JTextField name = new JTextField(22);
        JTextField email = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        JPasswordField confirm = new JPasswordField(22);
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Your name")); form.add(name);
        form.add(new JLabel("Email address")); form.add(email);
        form.add(new JLabel("Password (8+ characters)")); form.add(password);
        form.add(new JLabel("Confirm password")); form.add(confirm);

        int choice = JOptionPane.showConfirmDialog(this, form, "Create homeowner account", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        char[] first = password.getPassword();
        char[] second = confirm.getPassword();
        try {
            String cleanName = name.getText().trim();
            String cleanEmail = email.getText().trim().toLowerCase();
            if (cleanName.isBlank()) throw new IllegalArgumentException("Enter your name.");
            if (!cleanEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new IllegalArgumentException("Enter a valid email address.");
            if (first.length < 8) throw new IllegalArgumentException("Choose a password with at least 8 characters.");
            if (!Arrays.equals(first, second)) throw new IllegalArgumentException("The passwords do not match.");
            if (repository.findUserByEmail(cleanEmail) != null) throw new IllegalArgumentException("An account already exists for that email.");

            AppUser user = new AppUser(cleanName, cleanEmail, "HOMEOWNER", PasswordSecurity.hash(first));
            repository.saveUser(user);
            emailField.setText(cleanEmail);
            passwordField.setText("");
            JOptionPane.showMessageDialog(this, "Your account has been created. Sign in with your new details.", "Account created", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Check your details", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException exception) {
            showError("We couldn't create your account.", exception);
        } finally {
            Arrays.fill(first, '\0');
            Arrays.fill(second, '\0');
        }
    }

    private void showError(String message, RuntimeException exception) {
        JOptionPane.showMessageDialog(this, message + "\n" + exception.getMessage(), "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }

    private void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(NAVY);
        field.setBackground(Color.WHITE);
        field.setCaretColor(NAVY);
        field.setMaximumSize(new Dimension(340, 40));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(GREEN);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    static JLabel label(String text, int size, Color color, int style) {
        JLabel label = new JLabel("<html>" + text.replace("\n", "<br>") + "</html>");
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
