package edu.homeautomation.service;

import edu.homeautomation.model.*;
import edu.homeautomation.persistence.HomeRepository;
import java.util.List;

/** Coordinates device commands, compatibility checks, and persistence. */
public final class DeviceService {
    private final HomeRepository repository;
    private final ActivityHistory activity;
    public DeviceService(HomeRepository repository) { this(repository, new ActivityHistory()); }
    public DeviceService(HomeRepository repository, ActivityHistory activity) {
        this.repository = repository;
        this.activity = activity;
    }

    public ActivityHistory activityHistory() { return activity; }

    public List<SmartDevice> devices() { return repository.findDevices(); }

    public synchronized String toggle(SmartDevice device) throws HomeValidationException {
        if (!device.isCompatible()) throw new HomeValidationException("This device is not approved for platform use.");
        if (device.isOn()) device.turnOff(); else device.turnOn();
        repository.saveDevice(device);
        String message = device.getName() + " is now " + (device.isOn() ? "on" : "off") + ".";
        activity.record("Homeowner", "Device control", message);
        return message;
    }

    public synchronized void adjust(SmartDevice device, int value) {
        if (!device.isCompatible()) throw new IllegalArgumentException("This device is not approved for platform use.");
        String detail;
        if (device instanceof LightDevice light) {
            light.setBrightness(value);
            detail = light.getName() + " brightness set to " + value + "%.";
        } else if (device instanceof ThermostatDevice thermostat) {
            thermostat.setTargetTemperature(value);
            detail = thermostat.getName() + " set to " + value + "°C.";
        } else throw new IllegalArgumentException("This device does not have adjustable settings.");
        repository.saveDevice(device);
        activity.record("Homeowner", "Device adjustment", detail);
    }

    /** Runs the supported on/off automation action against a compatible device. */
    public synchronized String runAutomation(String action) throws HomeValidationException {
        java.util.regex.Matcher match = actionMatcher(action);
        if (!match.matches()) throw new IllegalArgumentException("Use ‘Turn on [device name]’ or ‘Turn off [device name]’.");
        boolean turnOn = match.group(1).equalsIgnoreCase("on");
        SmartDevice target = findAutomationDevice(match.group(2));
        if (!target.isCompatible()) throw new HomeValidationException("This device is not approved for platform use.");
        if (turnOn) target.turnOn(); else target.turnOff();
        repository.saveDevice(target);
        String message = target.getName() + " turned " + match.group(1).toLowerCase(java.util.Locale.ROOT) + " by automation.";
        activity.record("Homeowner", "Automation run", message);
        return message;
    }

    /** Checks a routine action before it is saved, without changing device state. */
    public synchronized void validateAutomationAction(String action) {
        java.util.regex.Matcher match = actionMatcher(action);
        if (!match.matches()) throw new IllegalArgumentException("Use ‘Turn on [device name]’ or ‘Turn off [device name]’.");
        SmartDevice target = findAutomationDevice(match.group(2));
        if (!target.isCompatible()) throw new IllegalArgumentException("Approve this device before using it in an automation.");
    }

    private java.util.regex.Matcher actionMatcher(String action) {
        return java.util.regex.Pattern.compile("(?i)^\\s*turn\\s+(on|off)\\s+(.+?)\\s*$")
                .matcher(action == null ? "" : action);
    }

    private SmartDevice findAutomationDevice(String name) {
        return repository.findDevices().stream()
                .filter(device -> device.getName().equalsIgnoreCase(name.trim()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No device matches ‘" + name.trim() + "’."));
    }

    public synchronized void setCompatibility(SmartDevice device, boolean compatible) {
        device.setCompatible(compatible);
        repository.saveDevice(device);
        activity.record("Administrator", "Compatibility review", device.getName() + (compatible ? " approved." : " marked not compatible."));
    }
}
