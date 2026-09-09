package com.bloodlink.repository.memory;
import com.bloodlink.model.*;
import com.bloodlink.repository.AppointmentRepository;
import java.time.*; import java.util.*; import java.util.concurrent.atomic.AtomicInteger;
public final class MemoryAppointmentRepository implements AppointmentRepository {
    private final List<Appointment> data=new ArrayList<>(); private final AtomicInteger ids=new AtomicInteger(203);
    public MemoryAppointmentRepository(){
        data.add(new Appointment(201,"Aarav Sharma",BloodGroup.O_POS,LocalDate.now().plusDays(2),LocalTime.of(10,30),"Pune Central","CONFIRMED"));
        data.add(new Appointment(202,"Ananya Rao",BloodGroup.AB_POS,LocalDate.now().plusDays(4),LocalTime.of(12,0),"Pune Central","CONFIRMED"));
    }
    public synchronized List<Appointment> findAll(){return new ArrayList<>(data);} 
    public synchronized Appointment save(Appointment a){if(a.getId()==0){a.setId(ids.getAndIncrement());data.add(a);}return a;}
    public synchronized void update(Appointment a){for(int i=0;i<data.size();i++)if(data.get(i).getId()==a.getId()){data.set(i,a);return;}}
}
