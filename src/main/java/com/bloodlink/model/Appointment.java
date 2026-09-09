package com.bloodlink.model;

import java.time.LocalDate;
import java.time.LocalTime;

public final class Appointment {
    private int id;
    private String donorName;
    private BloodGroup bloodGroup;
    private LocalDate date;
    private LocalTime time;
    private String branch;
    private String status;

    public Appointment(int id, String donorName, BloodGroup bloodGroup, LocalDate date, LocalTime time, String branch, String status) {
        this.id=id; this.donorName=donorName; this.bloodGroup=bloodGroup; this.date=date; this.time=time; this.branch=branch; this.status=status;
    }
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getDonorName(){return donorName;} public BloodGroup getBloodGroup(){return bloodGroup;}
    public LocalDate getDate(){return date;} public LocalTime getTime(){return time;} public String getBranch(){return branch;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
}
