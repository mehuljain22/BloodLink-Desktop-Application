package com.bloodlink.repository.memory;

import com.bloodlink.model.*;
import com.bloodlink.repository.BloodRequestRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class MemoryBloodRequestRepository implements BloodRequestRepository {
    private final List<BloodRequest> data=new ArrayList<>();
    private final AtomicInteger ids=new AtomicInteger(504);
    public MemoryBloodRequestRepository(){
        data.add(new BloodRequest(501,"Ishaan Verma",BloodGroup.O_POS,2,"CityCare Hospital","Dr. Shah",Priority.EMERGENCY,LocalDateTime.now().minusHours(2),RequestStatus.PENDING));
        data.add(new BloodRequest(502,"Mira Kulkarni",BloodGroup.A_POS,1,"Sunrise Hospital","Dr. Rao",Priority.URGENT,LocalDateTime.now().minusDays(1),RequestStatus.APPROVED));
        data.add(new BloodRequest(503,"Vivaan Desai",BloodGroup.B_POS,2,"Lotus Medical","Dr. Mehta",Priority.ROUTINE,LocalDateTime.now().minusDays(2),RequestStatus.COMPLETED));
    }
    @Override public synchronized List<BloodRequest> findAll(){ return new ArrayList<>(data); }
    @Override public synchronized BloodRequest save(BloodRequest r){ if(r.getId()==0){r.setId(ids.getAndIncrement());data.add(r);} return r; }
    @Override public synchronized void update(BloodRequest r){ for(int i=0;i<data.size();i++) if(data.get(i).getId()==r.getId()){data.set(i,r);return;} }
}
