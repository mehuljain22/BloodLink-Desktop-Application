package com.bloodlink.service;

import com.bloodlink.model.*;
import com.bloodlink.repository.BloodRequestRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request workflow.
 *
 * The important change from the earlier version: approving a request now
 * physically reserves bags. Completion issues exactly those reserved bags, and
 * rejecting an approved request puts them back on the shelf. Two approved
 * requests can therefore never claim the same unit.
 */
public final class RequestService {

    private final BloodRequestRepository repo;
    private final InventoryService inventory;
    private final AuditService audit;

    public RequestService(BloodRequestRepository repo, InventoryService inventory, AuditService audit) {
        this.repo = repo;
        this.inventory = inventory;
        this.audit = audit;
    }

    public List<BloodRequest> all() { return repo.findAll(); }

    public BloodRequest create(String patient, BloodGroup group, int units, String hospital,
                               String doctor, Priority priority) {
        if (patient.isBlank() || hospital.isBlank())
            throw new IllegalArgumentException("Patient and hospital are required.");
        if (units <= 0)
            throw new IllegalArgumentException("Units must be greater than zero.");

        BloodRequest saved = repo.save(new BloodRequest(0, patient, group, units, hospital, doctor,
                priority, LocalDateTime.now(), RequestStatus.PENDING));
        audit.record(AuditAction.REQUEST_CREATED, "BloodRequest", String.valueOf(saved.getId()),
                units + " unit(s) of " + group + " for " + patient + " at " + hospital + ", priority " + priority);
        return saved;
    }

    /** Approving holds real bags, chosen by nearest expiry. */
    public void approve(BloodRequest r) {
        if (r.getStatus() != RequestStatus.PENDING)
            throw new IllegalStateException("Only pending requests can be approved.");

        List<BloodBag> held = inventory.reserve(r.getBloodGroup(), r.getUnits(), r.getId());
        r.setStatus(RequestStatus.APPROVED);
        repo.update(r);
        audit.record(AuditAction.REQUEST_APPROVED, "BloodRequest", String.valueOf(r.getId()),
                "Reserved " + held.size() + " bag(s): " + held.stream().map(BloodBag::getBagCode).toList());
    }

    public void reject(BloodRequest r) {
        if (r.getStatus() == RequestStatus.COMPLETED)
            throw new IllegalStateException("A completed request cannot be rejected.");

        if (r.getStatus() == RequestStatus.APPROVED) inventory.releaseReservation(r.getId());
        r.setStatus(RequestStatus.REJECTED);
        repo.update(r);
        audit.record(AuditAction.REQUEST_REJECTED, "BloodRequest", String.valueOf(r.getId()),
                "Request for " + r.getPatientName() + " rejected; any held bags returned to the shelf.");
    }

    /** Completion issues exactly the bags that were reserved at approval. */
    public void complete(BloodRequest r) {
        if (r.getStatus() != RequestStatus.APPROVED)
            throw new IllegalStateException("Approve the request before completion.");

        List<BloodBag> issued = inventory.issueReserved(r.getId());
        r.setStatus(RequestStatus.COMPLETED);
        repo.update(r);
        audit.record(AuditAction.REQUEST_COMPLETED, "BloodRequest", String.valueOf(r.getId()),
                "Issued " + issued.size() + " bag(s) to " + r.getHospital() + ": "
                        + issued.stream().map(BloodBag::getBagCode).toList());
    }

    public long pending() {
        return all().stream().filter(r -> r.getStatus() == RequestStatus.PENDING).count();
    }

    public long emergencies() {
        return all().stream()
                .filter(r -> r.getPriority() == Priority.EMERGENCY)
                .filter(r -> r.getStatus() != RequestStatus.COMPLETED && r.getStatus() != RequestStatus.REJECTED)
                .count();
    }
}
