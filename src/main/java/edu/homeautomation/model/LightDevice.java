package edu.homeautomation.model;

public final class LightDevice extends SmartDevice {
    private int brightness;

    public LightDevice(String id, String name, String room, boolean on, int brightness) {
        super(id, name, room, "Light", on);
        setBrightness(brightness);
    }

    public synchronized void setBrightness(int brightness) {
        if (brightness < 0 || brightness > 100) throw new IllegalArgumentException("Brightness must be from 0 to 100.");
        this.brightness = brightness;
    }

    public synchronized int getBrightness() { return brightness; }
    @Override public synchronized String statusText() { return isOn() ? "On · " + brightness + "%" : "Off"; }
}
