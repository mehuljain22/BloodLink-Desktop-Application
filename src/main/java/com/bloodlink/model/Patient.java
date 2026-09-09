package com.bloodlink.model;
public final class Patient extends User {
    private BloodGroup bloodGroup;
    private String hospital;
    public Patient(int id, String name, String email, BloodGroup bloodGroup, String hospital) {
        super(id, name, email, Role.PATIENT); this.bloodGroup = bloodGroup; this.hospital = hospital;
    }
    @Override public String dashboardTitle() { return "Patient Dashboard"; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public String getHospital() { return hospital; }
}
