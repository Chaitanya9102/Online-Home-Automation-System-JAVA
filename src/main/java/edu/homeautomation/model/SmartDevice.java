package edu.homeautomation.model;

import java.util.Objects;

public abstract class SmartDevice implements Switchable {
    private final String id;
    private final String name;
    private final String room;
    private final String kind;
    private boolean on;
    private boolean compatible = true;

    protected SmartDevice(String id, String name, String room, String kind, boolean on) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.room = Objects.requireNonNull(room);
        this.kind = Objects.requireNonNull(kind);
        this.on = on;
    }

    public abstract String statusText();

    @Override public synchronized void turnOn() { on = true; }
    @Override public synchronized void turnOff() { on = false; }
    @Override public synchronized boolean isOn() { return on; }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRoom() { return room; }
    public String getKind() { return kind; }
    public synchronized void setCompatible(boolean compatible) { this.compatible = compatible; }
    public synchronized boolean isCompatible() { return compatible; }
}
