package com.bloodlink.model;

/**
 * Screening outcome for one marker on one bag. Blood banking deliberately uses
 * "reactive" and "non-reactive" rather than positive and negative, because a
 * screening assay is not a clinical diagnosis.
 */
public enum TestResult {
    PENDING, NON_REACTIVE, REACTIVE;

    public boolean isFinal() { return this != PENDING; }
}
