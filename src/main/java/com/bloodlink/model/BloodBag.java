package com.bloodlink.model;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * One physical bag of blood. This is the unit of inventory in BloodLink.
 *
 * The bag carries its own identity (barcode), provenance (which donor, which
 * date), shelf life (component driven expiry), screening results and lifecycle
 * status. Stock levels are derived by counting bags, never stored as a number.
 */
public final class BloodBag {

    private int id;
    private String bagCode;
    private BloodGroup bloodGroup;
    private BloodComponent component;
    private int donorId;
    private String donorName;
    private LocalDate collectionDate;
    private LocalDate expiryDate;
    private BagStatus status;
    private Integer reservedForRequestId;
    private final Map<TtiMarker, TestResult> screening = new EnumMap<>(TtiMarker.class);

    public BloodBag(int id, String bagCode, BloodGroup bloodGroup, BloodComponent component,
                    int donorId, String donorName, LocalDate collectionDate,
                    LocalDate expiryDate, BagStatus status, Integer reservedForRequestId) {
        this.id = id;
        this.bagCode = bagCode;
        this.bloodGroup = bloodGroup;
        this.component = component;
        this.donorId = donorId;
        this.donorName = donorName;
        this.collectionDate = collectionDate;
        this.expiryDate = expiryDate;
        this.status = status;
        this.reservedForRequestId = reservedForRequestId;
        for (TtiMarker m : TtiMarker.values()) screening.put(m, TestResult.PENDING);
    }

    /** Factory for a freshly collected bag: expiry is derived, status starts in quarantine. */
    public static BloodBag collected(String bagCode, BloodGroup group, BloodComponent component,
                                     int donorId, String donorName, LocalDate collectionDate) {
        return new BloodBag(0, bagCode, group, component, donorId, donorName, collectionDate,
                collectionDate.plusDays(component.getShelfLifeDays()), BagStatus.QUARANTINED, null);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getBagCode() { return bagCode; }
    public void setBagCode(String bagCode) { this.bagCode = bagCode; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public BloodComponent getComponent() { return component; }
    public int getDonorId() { return donorId; }
    public String getDonorName() { return donorName; }
    public LocalDate getCollectionDate() { return collectionDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public BagStatus getStatus() { return status; }
    public void setStatus(BagStatus status) { this.status = status; }
    public Integer getReservedForRequestId() { return reservedForRequestId; }
    public void setReservedForRequestId(Integer reservedForRequestId) { this.reservedForRequestId = reservedForRequestId; }

    public Map<TtiMarker, TestResult> getScreening() { return new EnumMap<>(screening); }
    public TestResult resultFor(TtiMarker marker) { return screening.getOrDefault(marker, TestResult.PENDING); }
    public void setResult(TtiMarker marker, TestResult result) { screening.put(marker, result); }

    public boolean allTestsComplete() {
        return screening.values().stream().allMatch(TestResult::isFinal);
    }

    public boolean anyReactive() {
        return screening.containsValue(TestResult.REACTIVE);
    }

    public boolean isExpiredOn(LocalDate date) {
        return !date.isBefore(expiryDate);
    }

    public long daysToExpiry(LocalDate from) {
        return java.time.temporal.ChronoUnit.DAYS.between(from, expiryDate);
    }

    /** Short human summary used in tables and audit trails. */
    public String describe() {
        return bagCode + " (" + bloodGroup + " " + component.getLabel() + ")";
    }

    @Override public String toString() { return describe(); }
}
