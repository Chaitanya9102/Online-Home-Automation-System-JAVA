package edu.homeautomation.model;

public final class SecurityDevice extends SmartDevice {
    public SecurityDevice(String id, String name, String room, boolean on) {
        super(id, name, room, "Security", on);
    }

    @Override public synchronized String statusText() { return isOn() ? "Armed" : "Disarmed"; }
}
