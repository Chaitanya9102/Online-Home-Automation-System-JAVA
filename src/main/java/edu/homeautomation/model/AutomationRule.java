package edu.homeautomation.model;

public final class AutomationRule {
    private final String name;
    private final String condition;
    private final String action;
    private boolean enabled;

    public AutomationRule(String name, String condition, String action, boolean enabled) {
        this.name = required(name, "Routine name", 80);
        this.condition = required(condition, "Routine condition", 160);
        this.action = required(action, "Routine action", 160);
        this.enabled = enabled;
    }

    private static String required(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        String clean = value.trim();
        if (clean.length() > maxLength) throw new IllegalArgumentException(label + " must be " + maxLength + " characters or fewer.");
        return clean;
    }

    public String getName() { return name; }
    public String getCondition() { return condition; }
    public String getAction() { return action; }
    public synchronized boolean isEnabled() { return enabled; }
    public synchronized void setEnabled(boolean enabled) { this.enabled = enabled; }
}
