package org.example.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class User {
    private int id;
    private String email;
    private String password;
    private String type; // "admin" or "client"
    private String nom; // User's name
    private String avatarConfig; // Avatar JSON configuration
    private Boolean blocked; // Is user blocked?
    private LocalDateTime createdAt; // Creation date

    public User() {}

    public User(int id, String email, String password, String type) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.type = type;
        this.blocked = false;
        this.createdAt = LocalDateTime.now();
    }

    public User(int id, String email, String password, String type, String nom) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.type = type;
        this.nom = nom;
        this.blocked = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getType() { return type; }
    public String getNom() { return nom; }
    public Boolean isBlocked() { return blocked != null ? blocked : false; }
    public String getCreatedAt() { 
        if (createdAt == null) return "N/A";
        return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    public String getAvatarConfig() { return avatarConfig; }
    public void setAvatarConfig(String avatarConfig) { this.avatarConfig = avatarConfig; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setType(String type) { this.type = type; }
    public void setNom(String nom) { this.nom = nom; }
    public void setBlocked(Boolean blocked) { this.blocked = blocked; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

