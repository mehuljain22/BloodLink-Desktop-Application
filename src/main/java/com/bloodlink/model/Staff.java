package com.bloodlink.model;
public final class Staff extends User {
    public Staff(int id, String name, String email) { super(id, name, email, Role.STAFF); }
    @Override public String dashboardTitle() { return "Blood Bank Operations"; }
}
