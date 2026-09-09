package com.bloodlink.model;

/**
 * The closed set of auditable actions. Using an enum rather than free text means
 * the audit log can be filtered and reported on reliably.
 */
public enum AuditAction {
    LOGIN_SUCCESS("Sign in"),
    LOGIN_FAILED("Failed sign in"),
    DONOR_CREATED("Donor created"),
    DONOR_UPDATED("Donor updated"),
    DONOR_DELETED("Donor deleted"),
    DONOR_DEFERRED("Donor deferred"),
    BAG_COLLECTED("Bag collected"),
    BAG_RELEASED("Bag released from quarantine"),
    BAG_RESERVED("Bag reserved"),
    BAG_UNRESERVED("Reservation released"),
    BAG_ISSUED("Bag issued"),
    BAG_EXPIRED("Bag expired"),
    BAG_DISCARDED("Bag discarded"),
    SCREENING_RECORDED("Screening result recorded"),
    REQUEST_CREATED("Request created"),
    REQUEST_APPROVED("Request approved"),
    REQUEST_REJECTED("Request rejected"),
    REQUEST_COMPLETED("Request completed"),
    APPOINTMENT_BOOKED("Appointment booked"),
    APPOINTMENT_CANCELLED("Appointment cancelled");

    private final String label;
    AuditAction(String label) { this.label = label; }
    public String getLabel() { return label; }
    @Override public String toString() { return label; }

    public boolean isSecurityEvent() { return this == LOGIN_SUCCESS || this == LOGIN_FAILED; }
}
