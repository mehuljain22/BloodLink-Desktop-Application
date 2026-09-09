package com.bloodlink.model;

/**
 * Lifecycle of a single blood bag.
 *
 * QUARANTINED -> AVAILABLE   all TTI screening tests came back non-reactive
 * QUARANTINED -> DISCARDED   at least one screening test was reactive
 * AVAILABLE   -> RESERVED    held for an approved blood request
 * RESERVED    -> ISSUED      physically handed over to the hospital
 * RESERVED    -> AVAILABLE   the request was rejected, so the hold is released
 * AVAILABLE   -> EXPIRED     shelf life ended before the bag was used
 */
public enum BagStatus {
    QUARANTINED, AVAILABLE, RESERVED, ISSUED, EXPIRED, DISCARDED;

    /** Only bags in these states occupy usable shelf space. */
    public boolean isOnShelf() { return this == QUARANTINED || this == AVAILABLE || this == RESERVED; }

    /** Bags that were lost rather than used. This is the wastage numerator. */
    public boolean isWasted() { return this == EXPIRED || this == DISCARDED; }
}
