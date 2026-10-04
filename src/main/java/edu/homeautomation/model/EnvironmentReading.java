package edu.homeautomation.model;

import java.time.LocalDateTime;

public record EnvironmentReading(double temperature, String securityStatus, LocalDateTime recordedAt) { }
