package com.bloodlink.repository;
import com.bloodlink.model.Appointment;
import java.util.List;
public interface AppointmentRepository {
    List<Appointment> findAll();
    Appointment save(Appointment appointment);
    void update(Appointment appointment);
}
