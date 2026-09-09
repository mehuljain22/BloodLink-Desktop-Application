package com.bloodlink.service;

import com.bloodlink.model.*;
import com.bloodlink.repository.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class AppointmentService {

    private final AppointmentRepository repo;
    private final AuditService audit;

    public AppointmentService(AppointmentRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    public List<Appointment> all() { return repo.findAll(); }

    public Appointment book(String donor, BloodGroup group, LocalDate date, LocalTime time, String branch) {
        if (donor.isBlank() || branch.isBlank())
            throw new IllegalArgumentException("Donor and branch are required.");
        if (date.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Appointment date cannot be in the past.");

        Appointment saved = repo.save(new Appointment(0, donor, group, date, time, branch, "CONFIRMED"));
        audit.record(AuditAction.APPOINTMENT_BOOKED, "Appointment", String.valueOf(saved.getId()),
                donor + " (" + group + ") on " + date + " at " + time + ", " + branch);
        return saved;
    }

    public void cancel(Appointment a) {
        a.setStatus("CANCELLED");
        repo.update(a);
        audit.record(AuditAction.APPOINTMENT_CANCELLED, "Appointment", String.valueOf(a.getId()),
                "Appointment for " + a.getDonorName() + " on " + a.getDate() + " cancelled.");
    }
}
