package com.bloodlink.service;

import com.bloodlink.model.*;
import com.bloodlink.repository.BloodBagRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bag level inventory.
 *
 * Stock is never stored as a number. Every figure this class reports is derived
 * by counting bags in a given status, which means the reported count and the
 * physical shelf cannot drift apart.
 *
 * Issuing follows FEFO (first expired, first out): the bag closest to its expiry
 * date is always chosen first, which is what actually reduces wastage.
 */
public final class InventoryService {

    public static final int LOW_STOCK_THRESHOLD = 10;
    public static final int EXPIRY_WARNING_DAYS = 7;

    private final BloodBagRepository repo;
    private final AuditService audit;

    public InventoryService(BloodBagRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    // ---------------------------------------------------------------- queries

    public List<BloodBag> allBags() { return repo.findAll(); }

    public List<BloodBag> bagsWithStatus(BagStatus status) {
        return repo.findAll().stream().filter(b -> b.getStatus() == status).collect(Collectors.toList());
    }

    /** Issuable stock per blood group. Quarantined and reserved bags are excluded on purpose. */
    public Map<BloodGroup, Integer> stock() {
        EnumMap<BloodGroup, Integer> map = new EnumMap<>(BloodGroup.class);
        for (BloodGroup g : BloodGroup.values()) map.put(g, 0);
        for (BloodBag b : repo.findAll())
            if (b.getStatus() == BagStatus.AVAILABLE) map.merge(b.getBloodGroup(), 1, Integer::sum);
        return map;
    }

    public int units(BloodGroup group) { return stock().getOrDefault(group, 0); }

    public int total() { return stock().values().stream().mapToInt(Integer::intValue).sum(); }

    public boolean lowStock(BloodGroup group) { return units(group) < LOW_STOCK_THRESHOLD; }

    public long quarantinedCount() { return bagsWithStatus(BagStatus.QUARANTINED).size(); }

    public long reservedCount() { return bagsWithStatus(BagStatus.RESERVED).size(); }

    /** Available bags due to expire within the warning window. These should be pushed out first. */
    public List<BloodBag> expiringSoon() {
        LocalDate today = LocalDate.now();
        return repo.findAll().stream()
                .filter(b -> b.getStatus() == BagStatus.AVAILABLE)
                .filter(b -> b.daysToExpiry(today) <= EXPIRY_WARNING_DAYS)
                .sorted(Comparator.comparing(BloodBag::getExpiryDate))
                .collect(Collectors.toList());
    }

    public long expiredCount() { return bagsWithStatus(BagStatus.EXPIRED).size(); }

    public long discardedCount() { return bagsWithStatus(BagStatus.DISCARDED).size(); }

    public long issuedCount() { return bagsWithStatus(BagStatus.ISSUED).size(); }

    /**
     * Wastage rate: bags lost to expiry or a reactive screening result, as a
     * percentage of every bag that has left the shelf one way or another. This
     * is the headline quality metric for a blood bank.
     */
    public double wastagePercent() {
        long wasted = expiredCount() + discardedCount();
        long resolved = wasted + issuedCount();
        return resolved == 0 ? 0.0 : (wasted * 100.0) / resolved;
    }

    // ---------------------------------------------------------------- collection

    /** Records a new collection. The bag enters quarantine and is not issuable yet. */
    public BloodBag collect(BloodGroup group, BloodComponent component, int donorId,
                            String donorName, LocalDate collectionDate) {
        if (donorName == null || donorName.isBlank())
            throw new IllegalArgumentException("Donor name is required for traceability.");
        if (collectionDate.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Collection date cannot be in the future.");

        BloodBag bag = BloodBag.collected(generateCode(collectionDate), group, component, donorId, donorName, collectionDate);
        repo.save(bag);
        audit.record(AuditAction.BAG_COLLECTED, "BloodBag", bag.getBagCode(),
                bag.describe() + " from " + donorName + ", expires " + bag.getExpiryDate());
        return bag;
    }

    private static final java.util.concurrent.atomic.AtomicInteger CODE_SEQUENCE =
            new java.util.concurrent.atomic.AtomicInteger(5000);

    /** Bag codes are unique and human readable: BL-<collection date>-<sequence>. */
    private String generateCode(LocalDate date) {
        return String.format("BL-%s-%04d",
                date.toString().replace("-", "").substring(2), CODE_SEQUENCE.incrementAndGet());
    }

    // ---------------------------------------------------------------- expiry

    /**
     * Moves every on shelf bag past its expiry date into EXPIRED. Run at startup
     * and on demand. Returns the bags that were swept.
     */
    public List<BloodBag> runExpirySweep() {
        LocalDate today = LocalDate.now();
        List<BloodBag> swept = new ArrayList<>();
        for (BloodBag bag : repo.findAll()) {
            if (bag.getStatus().isOnShelf() && bag.isExpiredOn(today)) {
                bag.setStatus(BagStatus.EXPIRED);
                bag.setReservedForRequestId(null);
                repo.update(bag);
                audit.record(AuditAction.BAG_EXPIRED, "BloodBag", bag.getBagCode(),
                        bag.describe() + " expired on " + bag.getExpiryDate());
                swept.add(bag);
            }
        }
        return swept;
    }

    // ---------------------------------------------------------------- reserve, issue, release

    /**
     * Holds the required number of bags for an approved request, nearest expiry
     * first. Reserving at approval time is what stops two approved requests from
     * competing for the same physical bag.
     */
    public List<BloodBag> reserve(BloodGroup group, int units, int requestId) {
        List<BloodBag> candidates = fefoCandidates(group);
        if (candidates.size() < units)
            throw new IllegalStateException("Only " + candidates.size() + " issuable unit(s) of " + group + " available.");

        List<BloodBag> held = new ArrayList<>();
        for (int i = 0; i < units; i++) {
            BloodBag bag = candidates.get(i);
            bag.setStatus(BagStatus.RESERVED);
            bag.setReservedForRequestId(requestId);
            repo.update(bag);
            audit.record(AuditAction.BAG_RESERVED, "BloodBag", bag.getBagCode(),
                    bag.describe() + " reserved for request #" + requestId);
            held.add(bag);
        }
        return held;
    }

    /** Releases every bag held for a request back onto the shelf. */
    public void releaseReservation(int requestId) {
        for (BloodBag bag : repo.findAll()) {
            if (isHeldFor(bag, requestId)) {
                bag.setStatus(bag.isExpiredOn(LocalDate.now()) ? BagStatus.EXPIRED : BagStatus.AVAILABLE);
                bag.setReservedForRequestId(null);
                repo.update(bag);
                audit.record(AuditAction.BAG_UNRESERVED, "BloodBag", bag.getBagCode(),
                        bag.describe() + " returned to shelf from request #" + requestId);
            }
        }
    }

    /** Marks the bags held for a request as physically issued. */
    public List<BloodBag> issueReserved(int requestId) {
        List<BloodBag> issued = new ArrayList<>();
        for (BloodBag bag : repo.findAll()) {
            if (isHeldFor(bag, requestId)) {
                bag.setStatus(BagStatus.ISSUED);
                repo.update(bag);
                audit.record(AuditAction.BAG_ISSUED, "BloodBag", bag.getBagCode(),
                        bag.describe() + " issued against request #" + requestId);
                issued.add(bag);
            }
        }
        if (issued.isEmpty()) throw new IllegalStateException("No bags are reserved for request #" + requestId + ".");
        return issued;
    }

    private boolean isHeldFor(BloodBag bag, int requestId) {
        return bag.getStatus() == BagStatus.RESERVED
                && bag.getReservedForRequestId() != null
                && bag.getReservedForRequestId() == requestId;
    }

    /** Direct over the counter issue, still FEFO ordered. */
    public List<BloodBag> issueDirect(BloodGroup group, int units, String reason) {
        List<BloodBag> candidates = fefoCandidates(group);
        if (candidates.size() < units)
            throw new IllegalStateException("Only " + candidates.size() + " issuable unit(s) of " + group + " available.");

        List<BloodBag> issued = new ArrayList<>();
        for (int i = 0; i < units; i++) {
            BloodBag bag = candidates.get(i);
            bag.setStatus(BagStatus.ISSUED);
            repo.update(bag);
            audit.record(AuditAction.BAG_ISSUED, "BloodBag", bag.getBagCode(), bag.describe() + " issued. " + reason);
            issued.add(bag);
        }
        return issued;
    }

    /** Manual discard, for breakage, cold chain failure or a failed visual inspection. */
    public void discard(BloodBag bag, String reason) {
        if (!bag.getStatus().isOnShelf())
            throw new IllegalStateException("Only bags still on the shelf can be discarded.");
        bag.setStatus(BagStatus.DISCARDED);
        bag.setReservedForRequestId(null);
        repo.update(bag);
        audit.record(AuditAction.BAG_DISCARDED, "BloodBag", bag.getBagCode(), bag.describe() + " discarded. " + reason);
    }

    /** Available, unexpired bags of a group sorted by nearest expiry. The heart of FEFO. */
    private List<BloodBag> fefoCandidates(BloodGroup group) {
        LocalDate today = LocalDate.now();
        return repo.findAll().stream()
                .filter(b -> b.getBloodGroup() == group)
                .filter(b -> b.getStatus() == BagStatus.AVAILABLE)
                .filter(b -> !b.isExpiredOn(today))
                .sorted(Comparator.comparing(BloodBag::getExpiryDate).thenComparing(BloodBag::getId))
                .collect(Collectors.toList());
    }
}
