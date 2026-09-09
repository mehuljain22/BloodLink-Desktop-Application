package com.bloodlink.service;

import com.bloodlink.model.*;
import com.bloodlink.repository.AuditRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Central recording point for the audit trail.
 *
 * Every service that changes state calls record(...) so that the log answers the
 * four questions a blood bank inspector asks: who did it, what did they do, to
 * which record, and when.
 */
public final class AuditService {

    private final AuditRepository repo;
    private User actor;

    public AuditService(AuditRepository repo) { this.repo = repo; }

    /** Set once at sign in, so services do not have to thread the user through every call. */
    public void setActor(User actor) { this.actor = actor; }

    public User getActor() { return actor; }

    public void record(AuditAction action, String entityType, String entityId, String details) {
        String name = actor == null ? "system" : actor.getName();
        Role role = actor == null ? Role.ADMIN : actor.getRole();
        repo.append(new AuditEvent(0, LocalDateTime.now(), name, role, action, entityType, entityId, details));
    }

    /** Used for sign in events, where the actor is not established yet. */
    public void recordAs(String actorName, Role role, AuditAction action, String details) {
        repo.append(new AuditEvent(0, LocalDateTime.now(), actorName, role, action, "Session", actorName, details));
    }

    public List<AuditEvent> all() { return repo.findAll(); }

    public List<AuditEvent> recent(int limit) { return repo.findRecent(limit); }

    public long countToday() {
        return all().stream().filter(e -> e.getTimestamp().toLocalDate().equals(java.time.LocalDate.now())).count();
    }
}
