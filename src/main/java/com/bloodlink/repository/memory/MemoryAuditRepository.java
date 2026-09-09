package com.bloodlink.repository.memory;

import com.bloodlink.model.AuditEvent;
import com.bloodlink.repository.AuditRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class MemoryAuditRepository implements AuditRepository {

    private final List<AuditEvent> events = new ArrayList<>();
    private final AtomicInteger ids = new AtomicInteger(1);

    @Override public synchronized AuditEvent append(AuditEvent event) {
        event.setId(ids.getAndIncrement());
        events.add(event);
        return event;
    }

    @Override public synchronized List<AuditEvent> findRecent(int limit) {
        List<AuditEvent> copy = new ArrayList<>(events);
        Collections.reverse(copy);
        return copy.size() <= limit ? copy : new ArrayList<>(copy.subList(0, limit));
    }

    @Override public synchronized List<AuditEvent> findAll() {
        List<AuditEvent> copy = new ArrayList<>(events);
        Collections.reverse(copy);
        return copy;
    }
}
