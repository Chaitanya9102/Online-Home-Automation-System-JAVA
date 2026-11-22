package edu.homeautomation.model;

import java.time.LocalDateTime;

/** One simulated platform action shown in the activity history. */
public record ActivityEvent(LocalDateTime occurredAt, String actor, String action, String details) { }
