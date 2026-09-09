package com.bloodlink.repository.memory;

import com.bloodlink.model.*;
import com.bloodlink.repository.DonorRepository;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class MemoryDonorRepository implements DonorRepository {
    private final List<Donor> donors = new ArrayList<>();
    private final AtomicInteger ids = new AtomicInteger(1006);
    public MemoryDonorRepository() {
        donors.add(new Donor(1001,"Aarav Sharma","aarav@example.com",BloodGroup.O_POS,28,"9876543210","Pune",LocalDate.now().minusMonths(5),true,6));
        donors.add(new Donor(1002,"Diya Mehta","diya@example.com",BloodGroup.A_POS,24,"9822011122","Mumbai",LocalDate.now().minusMonths(4),true,3));
        donors.add(new Donor(1003,"Kabir Joshi","kabir@example.com",BloodGroup.B_NEG,31,"9890012345","Nashik",LocalDate.now().minusMonths(2),false,8));
        donors.add(new Donor(1004,"Ananya Rao","ananya@example.com",BloodGroup.AB_POS,27,"9765432190","Pune",LocalDate.now().minusMonths(6),true,5));
        donors.add(new Donor(1005,"Rohan Patil","rohan@example.com",BloodGroup.O_NEG,35,"9000012345","Kolhapur",LocalDate.now().minusMonths(7),true,11));
    }
    @Override public synchronized List<Donor> findAll(){ return new ArrayList<>(donors); }
    @Override public synchronized List<Donor> search(String q){
        String s=q==null?"":q.toLowerCase();
        return donors.stream().filter(d -> d.getName().toLowerCase().contains(s) || d.getEmail().toLowerCase().contains(s)
                || d.getCity().toLowerCase().contains(s) || d.getBloodGroup().toString().toLowerCase().contains(s))
                .collect(Collectors.toList());
    }
    @Override public synchronized Donor save(Donor donor){
        if(donor.getId()==0){ donor.setId(ids.getAndIncrement()); donors.add(donor); }
        else { delete(donor.getId()); donors.add(donor); }
        return donor;
    }
    @Override public synchronized void delete(int id){ donors.removeIf(d -> d.getId()==id); }
}
