package com.bloodlink.service;

import com.bloodlink.model.*;
import com.bloodlink.repository.BloodBagRepository;
import com.bloodlink.repository.DonorRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transfusion transmissible infection (TTI) screening.
 *
 * Nothing donated is issuable until it has been tested. A newly collected bag
 * sits in QUARANTINED, and this service is the only route out of that state:
 *
 *   all five markers non-reactive  -> AVAILABLE  (released to the shelf)
 *   any marker reactive            -> DISCARDED  (destroyed, donor deferred)
 *
 * The rule is deliberately one way: once a bag is discarded for a reactive
 * result it can never be released, no matter what is recorded afterwards.
 */
public final class ScreeningService {

    private final BloodBagRepository bags;
    private final DonorRepository donors;
    private final AuditService audit;

    public ScreeningService(BloodBagRepository bags, DonorRepository donors, AuditService audit) {
        this.bags = bags;
        this.donors = donors;
        this.audit = audit;
    }

    /** The screening worklist: everything currently sitting in quarantine, oldest first. */
    public List<BloodBag> pending() {
        return bags.findAll().stream()
                .filter(b -> b.getStatus() == BagStatus.QUARANTINED)
                .sorted(Comparator.comparing(BloodBag::getCollectionDate))
                .collect(Collectors.toList());
    }

    public long pendingCount() { return pending().size(); }

    /**
     * Records one marker result and, if that completes the panel, resolves the
     * bag. Returns the bag so the caller can report the outcome.
     */
    public BloodBag record(BloodBag bag, TtiMarker marker, TestResult result) {
        if (bag.getStatus() != BagStatus.QUARANTINED)
            throw new IllegalStateException("Only quarantined bags can be screened. This bag is " + bag.getStatus() + ".");
        if (result == TestResult.PENDING)
            throw new IllegalArgumentException("Record a definite result, either non-reactive or reactive.");

        bag.setResult(marker, result);
        bags.update(bag);
        audit.record(AuditAction.SCREENING_RECORDED, "BloodBag", bag.getBagCode(),
                marker.getLabel() + " recorded as " + result + " for " + bag.describe());

        if (result == TestResult.REACTIVE) discard(bag, marker);
        else if (bag.allTestsComplete()) release(bag);
        return bag;
    }

    /** Convenience for the common case: the whole panel comes back clean in one go. */
    public BloodBag recordFullPanelNonReactive(BloodBag bag) {
        for (TtiMarker marker : TtiMarker.values()) {
            if (bag.getStatus() != BagStatus.QUARANTINED) break;
            record(bag, marker, TestResult.NON_REACTIVE);
        }
        return bag;
    }

    private void release(BloodBag bag) {
        bag.setStatus(BagStatus.AVAILABLE);
        bags.update(bag);
        audit.record(AuditAction.BAG_RELEASED, "BloodBag", bag.getBagCode(),
                bag.describe() + " cleared all five TTI markers and is now issuable. Expires " + bag.getExpiryDate());
    }

    private void discard(BloodBag bag, TtiMarker marker) {
        bag.setStatus(BagStatus.DISCARDED);
        bag.setReservedForRequestId(null);
        bags.update(bag);
        audit.record(AuditAction.BAG_DISCARDED, "BloodBag", bag.getBagCode(),
                bag.describe() + " discarded after a reactive " + marker.getLabel() + " result.");
        deferDonor(bag, marker);
    }

    /**
     * A reactive result means the donor must not donate again until counselled
     * and confirmed. BloodLink flags them unavailable and records why.
     */
    private void deferDonor(BloodBag bag, TtiMarker marker) {
        donors.findAll().stream()
                .filter(d -> d.getId() == bag.getDonorId())
                .findFirst()
                .ifPresent(donor -> {
                    donor.setAvailable(false);
                    donors.save(donor);
                    audit.record(AuditAction.DONOR_DEFERRED, "Donor", String.valueOf(donor.getId()),
                            donor.getName() + " deferred following a reactive " + marker.getLabel()
                                    + " result on bag " + bag.getBagCode() + ". Requires counselling and confirmatory testing.");
                });
    }
}
