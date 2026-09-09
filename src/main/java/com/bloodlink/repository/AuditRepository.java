package com.bloodlink.repository;

import com.bloodlink.model.AuditEvent;
import java.util.List;

/**
 * Append only storage for the audit trail. There is deliberately no update or
 * delete method: an audit log that can be edited is not an audit log.
 */
public interface AuditRepository {
    AuditEvent append(AuditEvent event);
    List<AuditEvent> findRecent(int limit);
    List<AuditEvent> findAll();
}
