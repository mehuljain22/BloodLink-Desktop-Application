package com.bloodlink.repository;
import com.bloodlink.model.BloodRequest;
import java.util.List;
public interface BloodRequestRepository {
    List<BloodRequest> findAll();
    BloodRequest save(BloodRequest request);
    void update(BloodRequest request);
}
