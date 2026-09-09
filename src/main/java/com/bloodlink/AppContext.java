package com.bloodlink;

import com.bloodlink.db.Database;
import com.bloodlink.repository.*;
import com.bloodlink.repository.audit.AuditedDonorRepository;
import com.bloodlink.repository.jdbc.*;
import com.bloodlink.repository.memory.*;
import com.bloodlink.service.*;

/**
 * Composition root. All wiring happens here, so every other class can depend on
 * interfaces and stay unaware of whether it is talking to MySQL or to memory.
 */
public final class AppContext {

    public final AuthService authService;
    public final DonorService donorService;
    public final InventoryService inventoryService;
    public final RequestService requestService;
    public final AppointmentService appointmentService;
    public final ScreeningService screeningService;
    public final AuditService auditService;
    public final boolean databaseMode;

    private AppContext(AuthService authService, DonorService donorService, InventoryService inventoryService,
                       RequestService requestService, AppointmentService appointmentService,
                       ScreeningService screeningService, AuditService auditService, boolean databaseMode) {
        this.authService = authService;
        this.donorService = donorService;
        this.inventoryService = inventoryService;
        this.requestService = requestService;
        this.appointmentService = appointmentService;
        this.screeningService = screeningService;
        this.auditService = auditService;
        this.databaseMode = databaseMode;
    }

    public static AppContext bootstrap() {
        UserRepository users;
        DonorRepository donors;
        BloodBagRepository bags;
        BloodRequestRepository requests;
        AppointmentRepository appointments;
        AuditRepository auditLog;
        boolean db = false;

        if (Database.isConfigured()) {
            try {
                Database.initializeSchema();
                users = new JdbcUserRepository();
                donors = new JdbcDonorRepository();
                bags = new JdbcBloodBagRepository();
                requests = new JdbcBloodRequestRepository();
                appointments = new JdbcAppointmentRepository();
                auditLog = new JdbcAuditRepository();
                ((JdbcUserRepository) users).ensureDefaultAdmin();
                db = true;
            } catch (Exception ex) {
                System.err.println("MySQL unavailable; starting BloodLink in demo mode: " + ex.getMessage());
                users = new MemoryUserRepository();
                donors = new MemoryDonorRepository();
                bags = new MemoryBloodBagRepository();
                requests = new MemoryBloodRequestRepository();
                appointments = new MemoryAppointmentRepository();
                auditLog = new MemoryAuditRepository();
            }
        } else {
            users = new MemoryUserRepository();
            donors = new MemoryDonorRepository();
            bags = new MemoryBloodBagRepository();
            requests = new MemoryBloodRequestRepository();
            appointments = new MemoryAppointmentRepository();
            auditLog = new MemoryAuditRepository();
        }

        AuditService auditService = new AuditService(auditLog);

        // Decorator: donor writes are audited without the storage class or the
        // service knowing anything about auditing.
        DonorRepository auditedDonors = new AuditedDonorRepository(donors, auditService);

        InventoryService inventoryService = new InventoryService(bags, auditService);

        AppContext context = new AppContext(
                new AuthService(users, auditService),
                new DonorService(auditedDonors),
                inventoryService,
                new RequestService(requests, inventoryService, auditService),
                new AppointmentService(appointments, auditService),
                new ScreeningService(bags, donors, auditService),
                auditService,
                db);

        // Anything that expired while the application was closed is caught here.
        context.inventoryService.runExpirySweep();
        return context;
    }
}
