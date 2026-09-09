package com.bloodlink.repository;

import com.bloodlink.model.BloodBag;
import java.util.List;
import java.util.Optional;

/**
 * Storage contract for bag level inventory. Replaces the old InventoryRepository,
 * which could only hold one integer per blood group.
 */
public interface BloodBagRepository {
    List<BloodBag> findAll();
    Optional<BloodBag> findById(int id);
    BloodBag save(BloodBag bag);
    void update(BloodBag bag);
}
