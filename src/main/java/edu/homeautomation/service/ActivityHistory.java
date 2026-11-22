package edu.homeautomation.service;

import edu.homeautomation.model.ActivityEvent;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;

/** Keeps the latest actions for the current application session. */
public final class ActivityHistory {
    private static final int MAX_EVENTS = 100;
    private final ArrayDeque<ActivityEvent> events = new ArrayDeque<>();

    public synchronized void record(String actor, String action, String details) {
        events.addFirst(new ActivityEvent(LocalDateTime.now(), clean(actor), clean(action), clean(details)));
        while (events.size() > MAX_EVENTS) events.removeLast();
    }

    public synchronized List<ActivityEvent> latest(int count) {
        if (count < 0) throw new IllegalArgumentException("Count cannot be negative.");
        return events.stream().limit(count).toList();
    }

    private String clean(String value) { return value == null ? "System" : value.trim(); }
}
