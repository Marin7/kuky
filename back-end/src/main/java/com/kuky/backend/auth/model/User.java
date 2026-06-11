package com.kuky.backend.auth.model;

import java.time.Instant;
import java.util.UUID;

public class User {

    private UUID id;
    private String email;
    private String passwordHash;
    private String status = "ACTIVE";
    private String role = "STUDENT";
    private boolean gdprConsent;
    private String firstName;
    private String lastName;
    private String username;
    private UUID avatarImageId;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isGdprConsent() { return gdprConsent; }
    public void setGdprConsent(boolean gdprConsent) { this.gdprConsent = gdprConsent; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public UUID getAvatarImageId() { return avatarImageId; }
    public void setAvatarImageId(UUID avatarImageId) { this.avatarImageId = avatarImageId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
