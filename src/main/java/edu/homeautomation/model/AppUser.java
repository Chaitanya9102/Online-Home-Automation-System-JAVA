package edu.homeautomation.model;

import java.util.Locale;

public record AppUser(String name, String email, String role, String passwordHash) {
    public AppUser(String name, String email, String role) {
        this(name, email, role, "");
    }

    public AppUser {
        name = name == null ? "" : name.trim();
        email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        role = role == null ? "HOMEOWNER" : role.trim().toUpperCase();
        passwordHash = passwordHash == null ? "" : passwordHash;
    }

    @Override public String toString() { return name + " · " + role; }
}
