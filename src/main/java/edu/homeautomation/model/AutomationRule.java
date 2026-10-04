package edu.homeautomation.model;

public final class AutomationRule {
    private final String name;
    private final String condition;
    private final String action;
    private boolean enabled;

    public AutomationRule(String name, String condition, String action, boolean enabled) {
        this.name = name;
        this.condition = condition;
        this.action = action;
        this.enabled = enabled;
    }

    public String getName() { return name; }
    public String getCondition() { return condition; }
    public String getAction() { return action; }
    public synchronized boolean isEnabled() { return enabled; }
    public synchronized void setEnabled(boolean enabled) { this.enabled = enabled; }
}
