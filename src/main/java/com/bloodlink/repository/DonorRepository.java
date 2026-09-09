package com.bloodlink.repository;
import com.bloodlink.model.Donor;
import java.util.List;
public interface DonorRepository {
    List<Donor> findAll();
    List<Donor> search(String query);
    Donor save(Donor donor);
    void delete(int id);
}
