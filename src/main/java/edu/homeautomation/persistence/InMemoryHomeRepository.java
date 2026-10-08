package edu.homeautomation.persistence;

import edu.homeautomation.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import edu.homeautomation.service.PasswordSecurity;

/** Seeded classroom store used when no JDBC URL is configured. */
public final class InMemoryHomeRepository implements HomeRepository {
    private final List<SmartDevice> devices = new ArrayList<>();
    private final List<AutomationRule> rules = new ArrayList<>();
    private final List<AppUser> users = new ArrayList<>();
    private final Map<String, String> settings = new HashMap<>();

    public InMemoryHomeRepository() {
        devices.add(new LightDevice("L-101", "Living room lights", "Living Room", true, 78));
        devices.add(new ThermostatDevice("T-201", "Smart thermostat", "Hallway", true, 22));
        devices.add(new SecurityDevice("S-301", "Front door lock", "Entry", true));
        devices.add(new LightDevice("L-102", "Bedroom lamp", "Bedroom", false, 40));
        devices.add(new SecurityDevice("S-302", "Motion sensor", "Garage", true));
        rules.add(new AutomationRule("Good night", "At 10:30 PM", "Turn off living room lights", true));
        rules.add(new AutomationRule("Welcome home", "When entry sensor detects you", "Turn on hallway lights", true));
        users.add(new AppUser("Alex Morgan", "alex@example.com", "HOMEOWNER", PasswordSecurity.hash("Home123!".toCharArray())));
        users.add(new AppUser("System Administrator", "admin@haven.local", "ADMIN", PasswordSecurity.hash("Admin123!".toCharArray())));
        settings.put("home_name", "Alex's Home");
        settings.put("temperature_limit", "28");
    }

    @Override public synchronized List<SmartDevice> findDevices() { return new ArrayList<>(devices); }
    @Override public synchronized List<AutomationRule> findRules() { return new ArrayList<>(rules); }
    @Override public synchronized List<AppUser> findUsers() { return new ArrayList<>(users); }
    @Override public synchronized AppUser findUserByEmail(String email) {
        return users.stream().filter(user -> user.email().equalsIgnoreCase(email)).findFirst().orElse(null);
    }
    @Override public synchronized void saveDevice(SmartDevice device) {
        devices.removeIf(item -> item.getId().equals(device.getId())); devices.add(device);
    }
    @Override public synchronized void saveRule(AutomationRule rule) { rules.add(rule); }
    @Override public synchronized void updateRule(AutomationRule rule) { /* The rule object is already held by this demo store. */ }
    @Override public synchronized void saveUser(AppUser user) {
        users.removeIf(item -> item.email().equalsIgnoreCase(user.email())); users.add(user);
    }
    @Override public synchronized void deleteUser(String email) { users.removeIf(item -> item.email().equalsIgnoreCase(email)); }
    @Override public synchronized String getSetting(String key, String defaultValue) { return settings.getOrDefault(key, defaultValue); }
    @Override public synchronized void saveSetting(String key, String value) { settings.put(key, value); }
    @Override public boolean isPersistent() { return false; }
}
