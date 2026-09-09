package com.bloodlink.repository.audit;

import com.bloodlink.model.AuditAction;
import com.bloodlink.model.Donor;
import com.bloodlink.repository.DonorRepository;
import com.bloodlink.service.AuditService;

import java.util.List;

/**
 * Decorator pattern. This class implements DonorRepository, wraps another
 * DonorRepository, and adds audit logging around the write operations without
 * changing either the storage implementation or the calling service.
 *
 * Because the delegate is injected, the same decorator works over the in-memory
 * repository and the JDBC repository.
 */
public final class AuditedDonorRepository implements DonorRepository {

    private final DonorRepository delegate;
    private final AuditService audit;

    public AuditedDonorRepository(DonorRepository delegate, AuditService audit) {
        this.delegate = delegate;
        this.audit = audit;
    }

    @Override public List<Donor> findAll() { return delegate.findAll(); }

    @Override public List<Donor> search(String query) { return delegate.search(query); }

    @Override public Donor save(Donor donor) {
        boolean creating = donor.getId() == 0;
        Donor saved = delegate.save(donor);
        audit.record(creating ? AuditAction.DONOR_CREATED : AuditAction.DONOR_UPDATED,
                "Donor", String.valueOf(saved.getId()),
                saved.getName() + ", " + saved.getBloodGroup() + ", " + saved.getCity());
        return saved;
    }

    @Override public void delete(int id) {
        String name = delegate.findAll().stream()
                .filter(d -> d.getId() == id).map(Donor::getName).findFirst().orElse("unknown");
        delegate.delete(id);
        audit.record(AuditAction.DONOR_DELETED, "Donor", String.valueOf(id), "Removed donor record for " + name);
    }
}
