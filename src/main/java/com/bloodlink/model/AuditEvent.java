package com.bloodlink.model;

import java.time.LocalDateTime;

/**
 * One immutable entry in the audit trail. Audit rows are only ever inserted,
 * never updated or deleted, which is why every field here is final.
 */
public final class AuditEvent {

    private int id;
    private final LocalDateTime timestamp;
    private final String actor;
    private final Role actorRole;
    private final AuditAction action;
    private final String entityType;
    private final String entityId;
    private final String details;

    public AuditEvent(int id, LocalDateTime timestamp, String actor, Role actorRole,
                      AuditAction action, String entityType, String entityId, String details) {
        this.id = id;
        this.timestamp = timestamp;
        this.actor = actor;
        this.actorRole = actorRole;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getActor() { return actor; }
    public Role getActorRole() { return actorRole; }
    public AuditAction getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getDetails() { return details; }
}
