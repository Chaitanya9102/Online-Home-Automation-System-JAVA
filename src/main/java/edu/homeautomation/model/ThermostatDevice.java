package edu.homeautomation.model;

public final class ThermostatDevice extends SmartDevice {
    private int targetTemperature;

    public ThermostatDevice(String id, String name, String room, boolean on, int targetTemperature) {
        super(id, name, room, "Thermostat", on);
        setTargetTemperature(targetTemperature);
    }

    public synchronized void setTargetTemperature(int value) {
        if (value < 10 || value > 32) throw new IllegalArgumentException("Target temperature must be 10–32 °C.");
        targetTemperature = value;
    }

    public synchronized int getTargetTemperature() { return targetTemperature; }
    @Override public synchronized String statusText() { return isOn() ? "Heating · " + targetTemperature + "°C" : "Standby"; }
}
