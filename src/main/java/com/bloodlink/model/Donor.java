package com.bloodlink.model;

import java.time.LocalDate;

public final class Donor extends User {
    private BloodGroup bloodGroup;
    private int age;
    private String phone;
    private String city;
    private LocalDate lastDonationDate;
    private boolean available;
    private int totalDonations;

    public Donor(int id, String name, String email, BloodGroup bloodGroup, int age,
                 String phone, String city, LocalDate lastDonationDate, boolean available, int totalDonations) {
        super(id, name, email, Role.DONOR);
        this.bloodGroup = bloodGroup; this.age = age; this.phone = phone; this.city = city;
        this.lastDonationDate = lastDonationDate; this.available = available; this.totalDonations = totalDonations;
    }
    @Override public String dashboardTitle() { return "Donor Dashboard"; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public LocalDate getLastDonationDate() { return lastDonationDate; }
    public void setLastDonationDate(LocalDate lastDonationDate) { this.lastDonationDate = lastDonationDate; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public int getTotalDonations() { return totalDonations; }
    public void setTotalDonations(int totalDonations) { this.totalDonations = totalDonations; }
}
