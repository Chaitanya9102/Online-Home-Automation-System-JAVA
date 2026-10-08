package edu.homeautomation.service;

import edu.homeautomation.model.*;
import edu.homeautomation.persistence.HomeRepository;
import java.util.List;

/** Coordinates device commands, compatibility checks, and persistence. */
public final class DeviceService {
    private final HomeRepository repository;
    public DeviceService(HomeRepository repository) { this.repository = repository; }

    public List<SmartDevice> devices() { return repository.findDevices(); }

    public synchronized String toggle(SmartDevice device) throws HomeValidationException {
        if (!device.isCompatible()) throw new HomeValidationException("This device is not approved for platform use.");
        if (device.isOn()) device.turnOff(); else device.turnOn();
        repository.saveDevice(device);
        return device.getName() + " is now " + (device.isOn() ? "on" : "off") + ".";
    }

    public synchronized void setCompatibility(SmartDevice device, boolean compatible) {
        device.setCompatible(compatible);
        repository.saveDevice(device);
    }
}
