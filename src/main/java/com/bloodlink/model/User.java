package com.bloodlink.model;

public abstract class User {
    private int id;
    private String name;
    private String email;
    private Role role;

    protected User(int id, String name, String email, Role role) {
        this.id = id; this.name = name; this.email = email; this.role = role;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    protected void setRole(Role role) { this.role = role; }
    public abstract String dashboardTitle();
}
