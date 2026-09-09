package com.bloodlink.model;
public final class Admin extends User {
    public Admin(int id, String name, String email) { super(id, name, email, Role.ADMIN); }
    @Override public String dashboardTitle() { return "Administration Dashboard"; }
}
