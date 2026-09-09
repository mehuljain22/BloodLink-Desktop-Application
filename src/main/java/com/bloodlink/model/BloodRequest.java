package com.bloodlink.model;

import java.time.LocalDateTime;

public final class BloodRequest {
    private int id;
    private String patientName;
    private BloodGroup bloodGroup;
    private int units;
    private String hospital;
    private String doctor;
    private Priority priority;
    private LocalDateTime createdAt;
    private RequestStatus status;

    public BloodRequest(int id, String patientName, BloodGroup bloodGroup, int units, String hospital,
                        String doctor, Priority priority, LocalDateTime createdAt, RequestStatus status) {
        this.id=id; this.patientName=patientName; this.bloodGroup=bloodGroup; this.units=units;
        this.hospital=hospital; this.doctor=doctor; this.priority=priority; this.createdAt=createdAt; this.status=status;
    }
    public int getId(){ return id; } public void setId(int id){ this.id=id; }
    public String getPatientName(){ return patientName; }
    public BloodGroup getBloodGroup(){ return bloodGroup; }
    public int getUnits(){ return units; }
    public String getHospital(){ return hospital; }
    public String getDoctor(){ return doctor; }
    public Priority getPriority(){ return priority; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public RequestStatus getStatus(){ return status; }
    public void setStatus(RequestStatus status){ this.status=status; }
}
