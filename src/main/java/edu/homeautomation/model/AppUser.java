package edu.homeautomation.model;

public record AppUser(String name, String email, String role) {
    @Override public String toString() { return name + " · " + role; }
}
