package org.example.model;

public class User {
    private int id;
    private String email;
    private String password;
    private String type; // "admin" or "client"
    private String nom; // User's name
    private String telephone;
    private boolean blocked;
    private boolean totpEnabled;
    private String totpSecret;
    private String avatarConfig;
    private String createdAt;

    public User() {}

    public User(int id, String email, String password, String type) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.type = type;
    }

    public User(int id, String email, String password, String type, String nom) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.type = type;
        this.nom = nom;
    }

    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getType() { return type; }
    public String getNom() { return nom; }
    public String getTelephone() { return telephone; }
    public boolean isBlocked() { return blocked; }
    public boolean isTotpEnabled() { return totpEnabled; }
    public String getTotpSecret() { return totpSecret; }
    public String getAvatarConfig() { return avatarConfig; }
    public String getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setType(String type) { this.type = type; }
    public void setNom(String nom) { this.nom = nom; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public void setTotpEnabled(boolean totpEnabled) { this.totpEnabled = totpEnabled; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
    public void setAvatarConfig(String avatarConfig) { this.avatarConfig = avatarConfig; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

