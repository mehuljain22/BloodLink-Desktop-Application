package com.bloodlink.model;

/**
 * Transfusion transmissible infection markers. Every donated unit must be tested
 * for all of these before it can be released from quarantine.
 */
public enum TtiMarker {
    HIV("HIV 1 and 2"),
    HBV("Hepatitis B"),
    HCV("Hepatitis C"),
    SYPHILIS("Syphilis"),
    MALARIA("Malaria");

    private final String label;
    TtiMarker(String label) { this.label = label; }
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
}
