package org.example.model;

public class User {
    private int id;
    private String email;
    private String password;
    private String type; // "admin" or "client"
    private String nom; // User's name

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

    public void setId(int id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setType(String type) { this.type = type; }
    public void setNom(String nom) { this.nom = nom; }
}

